package com.github.lpedrosa.conversation.creator.pool.message;

import akka.actor.ActorRef;

import java.util.Objects;

public final class WorkerAvailable {

    private final ActorRef workerRef;

    public WorkerAvailable(ActorRef workerRef) {
        this.workerRef = Objects.requireNonNull(workerRef);
    }

    public ActorRef getWorkerRef() {
        return workerRef;
    }

}
