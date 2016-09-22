package com.github.lpedrosa.conversation.creator;

import akka.actor.UntypedActor;
import com.github.lpedrosa.conversation.creator.message.CreateConversation;

public class ConversationCreator extends UntypedActor {

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof CreateConversation) {
            createConversation((CreateConversation) message);
        } else {
            unhandled(message);
        }
    }

    private void createConversation(CreateConversation message) {

    }
}
