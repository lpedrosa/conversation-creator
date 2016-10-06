package com.github.lpedrosa.conversation.creator.pool.message;

import akka.actor.ActorRef;

import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public final class Workers {

    private final Set<ActorRef> workerAvailable;

    public Workers(Set<ActorRef> workerAvailable) {
        this.workerAvailable = ImmutableSet.copyOf(Objects.requireNonNull(workerAvailable));
    }

    public Set<ActorRef> getWorkersAvailable() {
        return this.workerAvailable;
    }

}
