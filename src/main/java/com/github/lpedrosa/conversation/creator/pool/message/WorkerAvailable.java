package com.github.lpedrosa.conversation.creator.pool.message;

import akka.actor.ActorRef;

public final class WorkerAvailable {

    private final ActorRef workerRef;

    public WorkerAvailable(ActorRef workerRef) {
        this.workerRef = workerRef;
    }

    public ActorRef getWorkerRef() {
        return workerRef;
    }

}
