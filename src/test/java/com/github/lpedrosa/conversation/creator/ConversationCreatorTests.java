package com.github.lpedrosa.conversation.creator;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.github.lpedrosa.conversation.creator.message.ConversationCreatorMessage;
import com.github.lpedrosa.conversation.creator.message.CreateConversation;
import com.github.lpedrosa.util.AkkaTest;

@RunWith(JUnit4.class)
public class ConversationCreatorTests extends AkkaTest {

    @Test
    public void itShouldRejectRequestsWhenQueueSizeIsReached() {
        final int queueSize = 1;
        final ActorRef conversationCreator = system().actorOf(ConversationCreator.props(queueSize));
        final JavaTestKit mockRequester = mockActor();

        conversationCreator.tell(new CreateConversation(), mockRequester.getRef());
        conversationCreator.tell(new CreateConversation(), mockRequester.getRef());

        mockRequester.expectMsgEquals(ConversationCreatorMessage.CreatorOverloaded);
    }
}
