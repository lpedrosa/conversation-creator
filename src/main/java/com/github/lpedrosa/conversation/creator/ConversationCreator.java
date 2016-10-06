package com.github.lpedrosa.conversation.creator;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.japi.Procedure;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.lpedrosa.conversation.creator.message.ConversationCreatorMessage;
import com.github.lpedrosa.conversation.creator.message.CreateConversation;
import com.github.lpedrosa.conversation.creator.pool.message.CreatorWorkerPoolMessage;
import com.github.lpedrosa.conversation.creator.pool.message.WorkerAvailable;
import com.github.lpedrosa.conversation.creator.pool.message.Workers;
import com.google.common.base.Preconditions;

/**
 * TODO:
 *
 * - queueing conversation creator is too complicated, should try a simple implementation
 */
public class ConversationCreator extends UntypedActor {

    private final BlockingDeque<CreateConversation> queuedRequests;
    private final Deque<ActorRef> availableWorkers;

    private final ActorRef workerPool;

    public static Props props(int queueSize, ActorRef workerPool) {
        return Props.create(ConversationCreator.class,
                            () -> new ConversationCreator(queueSize, workerPool));
    }

    private ConversationCreator(int queueSize, ActorRef workerPool) {
        Preconditions.checkArgument(queueSize >= 0);
        // FIXME this blows up when size == 0
        this.queuedRequests = new LinkedBlockingDeque<>(queueSize);
        this.availableWorkers = new ArrayDeque<>();
        this.workerPool = Objects.requireNonNull(workerPool);
    }

    @Override
    public void preStart() throws Exception {
        // tell the pool to send us the available workers
        this.workerPool.tell(CreatorWorkerPoolMessage.GetWorkers, getSelf());

        // watch the worker pool
        getContext().watch(this.workerPool);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof Workers) {
            storeAndWatchWorkers((Workers)message);
            getContext().become(this.active);
        } else {
            getSender().tell(ConversationCreatorMessage.CreationFailed, getSelf());
        }
    }

    private void storeAndWatchWorkers(Workers message) {
        Set<ActorRef> workers = message.getWorkersAvailable();

        for (ActorRef worker : workers) {
            getContext().watch(worker);
            this.availableWorkers.add(worker);
        }
    }

    private Procedure<Object> active = message -> {
        if (message instanceof CreateConversation) {
            createConversation((CreateConversation) message);
        } else if (message instanceof WorkerAvailable) {
            handleWorkerAvailable((WorkerAvailable) message);
        } else if (message instanceof Terminated) {
            handleTerminated((Terminated) message);
        } else {
            unhandled(message);
        }
    };

    private void createConversation(CreateConversation message) {
        ActorRef worker = this.availableWorkers.pollFirst();

        // if there a worker available, push it to the queue
        if (worker != null) {
            worker.tell(message, getSelf());
            return;
        }

        // no available workers
        boolean queuedRequest = this.queuedRequests.offer(message);
        if (!queuedRequest) {
            getSender().tell(ConversationCreatorMessage.CreatorOverloaded, getSelf());
        }
    }

    private void handleWorkerAvailable(WorkerAvailable message) {
        ActorRef worker = message.getWorkerRef();
        this.availableWorkers.add(worker);

        tryDispatchQueuedWork(this.queuedRequests, this.availableWorkers, getSelf());
    }

    private void handleTerminated(Terminated message) {

    }

    private static void tryDispatchQueuedWork(Deque<CreateConversation> workQueue,
                                              Deque<ActorRef> workers,
                                              ActorRef self) {
        CreateConversation queuedWork = workQueue.peek();

        if (queuedWork != null) {
            // give a worker some queued work
            // if there is one available
            ActorRef nextAvailable = workers.poll();
            if (nextAvailable != null) {
                nextAvailable.tell(queuedWork, self);
                workQueue.remove();
            }
        }
    }

}
