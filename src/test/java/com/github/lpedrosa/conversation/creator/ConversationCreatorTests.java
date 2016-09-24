package com.github.lpedrosa.conversation.creator;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;

import com.github.lpedrosa.conversation.creator.message.Conversation;
import com.github.lpedrosa.conversation.creator.pool.message.WorkerAvailable;
import com.github.lpedrosa.conversation.service.ConversationInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.github.lpedrosa.conversation.creator.message.ConversationCreatorMessage;
import com.github.lpedrosa.conversation.creator.message.CreateConversation;
import com.github.lpedrosa.util.AkkaTest;

@RunWith(JUnit4.class)
public class ConversationCreatorTests extends AkkaTest {

    @Test
    public void itShouldExecuteRequestsOnAWorkerWhenThereIsOneAvailable() {
        final int queueSize = 1;
        final JavaTestKit mockWorkerPool = mockActor();
        final JavaTestKit mockRequester = mockActor();
        final ActorRef conversationCreator = newConversationCreator(queueSize, mockWorkerPool.getRef());

        conversationCreator.tell(newCreateConversationMessage(), mockRequester.getRef());

        // tell the creator there is a worker available
        final JavaTestKit mockWorker = mockActor();
        conversationCreator.tell(new WorkerAvailable(mockWorker.getRef()), mockWorkerPool.getRef());

        // simulate worker result
        conversationCreator.tell(new ConversationInfo("id", "name", "appId"), mockWorker.getRef());

        mockRequester.expectMsgClass(Conversation.class);
    }

    @Test
    public void itShouldQueueRequestsWhenThereAreNoWorkersAvailable() {

    }

    @Test
    public void itShouldExecuteQueuedRequestsWhenThereIsAWorkerAvailable() {

    }

    @Test
    public void itShouldRejectRequestsIfTheWorkerSupervisorIsDown() {

    }

    @Test
    public void itShouldRejectRequestsWhenQueueSizeIsReached() {
        final int queueSize = 1;
        final JavaTestKit mockWorkerPool = mockActor();
        final JavaTestKit mockRequester = mockActor();
        final ActorRef conversationCreator = newConversationCreator(queueSize, mockWorkerPool.getRef());

        conversationCreator.tell(newCreateConversationMessage(), mockRequester.getRef());
        conversationCreator.tell(newCreateConversationMessage(), mockRequester.getRef());

        mockRequester.expectMsgEquals(ConversationCreatorMessage.CreatorOverloaded);
    }

    private CreateConversation newCreateConversationMessage() {
        return new CreateConversation("randomId");
    }

    private ActorRef newConversationCreator(int queueSize, ActorRef workerPool) {
        final Props creatorProps = ConversationCreator.props(queueSize, workerPool);
        return system().actorOf(creatorProps);
    }
}
