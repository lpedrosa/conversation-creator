package com.github.lpedrosa.conversation.creator.pool;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import com.github.lpedrosa.conversation.creator.worker.CreatorWorker;
import com.github.lpedrosa.conversation.service.ConversationBackend;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class CreatorWorkerPool extends UntypedActor {

    private final int numberOfWorkers;
    private final ConversationBackend backend;

    private Deque<ActorRef> availableWorkers;

    public static Props props(int numberOfWorkers, ConversationBackend backend) {
        return Props.create(CreatorWorkerPool.class,
                () -> new CreatorWorkerPool(numberOfWorkers, backend));
    }

    private CreatorWorkerPool(int numberOfWorkers, ConversationBackend backend) {
        this.numberOfWorkers = numberOfWorkers;
        this.backend = Objects.requireNonNull(backend);
        this.availableWorkers = new ArrayDeque<>(this.numberOfWorkers);
    }

    @Override
    public void preStart() throws Exception {
        for (int i = 0; i < numberOfWorkers; i++) {
            Props workerProps = CreatorWorker.props(this.backend, getContext().system().dispatcher());
            ActorRef worker = getContext().actorOf(workerProps);
            this.availableWorkers.add(worker);
        }
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        final ActorRef worker = this.availableWorkers.poll();

        if (worker == null)
            throw new UnsupportedOperationException("No more workers!");

        worker.tell(message, getSender());
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return SupervisorStrategy.stoppingStrategy();
    }
}
