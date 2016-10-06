package com.github.lpedrosa.conversation.creator;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.PatternsCS;
import akka.testkit.JavaTestKit;
import scala.concurrent.duration.Duration;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.github.lpedrosa.conversation.creator.message.ConversationCreatorMessage;
import com.github.lpedrosa.conversation.creator.message.CreateConversation;
import com.github.lpedrosa.conversation.creator.pool.message.WorkerAvailable;
import com.github.lpedrosa.util.AkkaTest;

@RunWith(JUnit4.class)
public class ConversationCreatorTests extends AkkaTest {

    @Test
    public void itShouldExecuteRequestsOnAWorkerWhenThereIsOneAvailable() {
        final int queueSize = 1;
        final JavaTestKit mockWorkerPool = mockActor();
        final JavaTestKit mockRequester = mockActor();
        final ActorRef conversationCreator = newConversationCreator(queueSize, mockWorkerPool.getRef());

        // tell the creator there is a worker available
        final JavaTestKit mockWorker = mockActor();
        conversationCreator.tell(new WorkerAvailable(mockWorker.getRef()), mockWorkerPool.getRef());

        // tell the creator to do some work
        conversationCreator.tell(newCreateConversationMessage(), mockRequester.getRef());

        // worker should have received the work
        mockWorker.expectMsgClass(CreateConversation.class);
    }

    @Test
    public void itShouldExecuteQueuedRequestsWhenThereIsAWorkerAvailable() {
        final int queueSize = 1;
        final JavaTestKit mockWorkerPool = mockActor();
        final JavaTestKit mockRequester = mockActor();
        final ActorRef conversationCreator = newConversationCreator(queueSize, mockWorkerPool.getRef());

        // tell the creator to do some work
        conversationCreator.tell(newCreateConversationMessage(), mockRequester.getRef());

        // tell the creator there is a worker available
        final JavaTestKit mockWorker = mockActor();
        conversationCreator.tell(new WorkerAvailable(mockWorker.getRef()), mockWorkerPool.getRef());

        // worker should have received the work
        mockWorker.expectMsgClass(CreateConversation.class);
    }

    @Test
    public void itShouldRejectRequestsIfTheWorkerSupervisorIsDown() {
        final int queueSize = 1;
        final JavaTestKit mockWorkerPool = mockActor();
        final JavaTestKit mockRequester = mockActor();

        final ActorRef conversationCreator = newConversationCreator(queueSize, mockWorkerPool.getRef());

        // kill the pool
        // TODO extract into a method
        final CompletionStage<Boolean> stopped = PatternsCS.gracefulStop(mockWorkerPool.getRef(), Duration.create(50, TimeUnit.MILLISECONDS));
        try {
            stopped.toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        conversationCreator.tell(newCreateConversationMessage(), mockRequester.getRef());

        mockRequester.expectMsgEquals(ConversationCreatorMessage.CreatorOverloaded);
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
