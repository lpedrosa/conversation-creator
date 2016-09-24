package com.github.lpedrosa.conversation.creator;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;

import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.lpedrosa.conversation.creator.message.ConversationCreatorMessage;
import com.github.lpedrosa.conversation.creator.message.CreateConversation;
import com.google.common.base.Preconditions;

public class ConversationCreator extends UntypedActor {

    private final BlockingDeque<CreateConversation> queuedRequests;
    private final ActorRef workerPool;

    public static Props props(int queueSize, ActorRef workerPool) {
        return Props.create(ConversationCreator.class,
                            () -> new ConversationCreator(queueSize, workerPool));
    }

    private ConversationCreator(int queueSize, ActorRef workerPool) {
        Preconditions.checkArgument(queueSize > 0, "queueSize <= 0");

        this.queuedRequests = new LinkedBlockingDeque<>(queueSize);
        this.workerPool = Objects.requireNonNull(workerPool);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof CreateConversation) {
            createConversation((CreateConversation) message);
        } else if (message instanceof Terminated) {
            handleTerminated((Terminated) message);
        } else {
            unhandled(message);
        }
    }

    private void createConversation(CreateConversation message) {
        boolean queuedRequest = this.queuedRequests.offer(message);

        if (!queuedRequest) {
            sender().tell(ConversationCreatorMessage.CreatorOverloaded, self());
        }
    }

    private void handleTerminated(Terminated message) {

    }
}
