package com.github.lpedrosa.conversation.creator;

import akka.actor.Props;
import akka.actor.UntypedActor;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.lpedrosa.conversation.creator.message.ConversationCreatorMessage;
import com.github.lpedrosa.conversation.creator.message.CreateConversation;

public class ConversationCreator extends UntypedActor {

    private final BlockingDeque<CreateConversation> queuedRequests;

    public static Props props(int queueSize) {
        return Props.create(ConversationCreator.class, () -> new ConversationCreator(queueSize));
    }

    private ConversationCreator(int queueSize) {
        this.queuedRequests = new LinkedBlockingDeque<>(queueSize);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof CreateConversation) {
            createConversation((CreateConversation) message);
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
}
