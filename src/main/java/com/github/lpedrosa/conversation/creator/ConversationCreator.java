package com.github.lpedrosa.conversation.creator;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.japi.Procedure;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.lpedrosa.conversation.creator.message.ConversationCreatorMessage;
import com.github.lpedrosa.conversation.creator.message.CreateConversation;
import com.github.lpedrosa.conversation.creator.pool.message.CreatorWorkerPoolMessage;
import com.github.lpedrosa.conversation.creator.pool.message.Workers;

public class ConversationCreator extends UntypedActor {

    private final BlockingDeque<CreateConversation> queuedRequests;
    private final Deque<ActorRef> availableWorkers;

    private final ActorRef workerPool;

    public static Props props(int queueSize, ActorRef workerPool) {
        return Props.create(ConversationCreator.class,
                            () -> new ConversationCreator(queueSize, workerPool));
    }

    private ConversationCreator(int queueSize, ActorRef workerPool) {
        this.queuedRequests = queueSize < 1 ? null : new LinkedBlockingDeque<>(queueSize);
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
        } else if (message instanceof Terminated) {
            handleTerminated((Terminated) message);
        } else {
            unhandled(message);
        }
    };

    private void createConversation(CreateConversation message) {
        ActorRef worker = this.availableWorkers.pollFirst();

        // no available workers
        if (worker == null) {
            boolean queuedRequest = queueRequest(message);
            if (!queuedRequest) {
                getSender().tell(ConversationCreatorMessage.CreatorOverloaded, self());
            }
            return;
        }

        worker.tell(message, getSelf());
    }

    private boolean queueRequest(CreateConversation message) {
        if (this.queuedRequests == null)
            return false;

        return this.queuedRequests.offer(message);
    }

    private void handleTerminated(Terminated message) {

    }
}
