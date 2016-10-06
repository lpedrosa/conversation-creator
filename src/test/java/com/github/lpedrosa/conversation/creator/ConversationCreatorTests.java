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
        final ActorRef conversationCreator = newConversationCreator(1, mockActor().getRef());

        // tell the creator there is a worker available
        final JavaTestKit mockWorker = mockActor();
        conversationCreator.tell(new WorkerAvailable(mockWorker.getRef()), ActorRef.noSender());

        // tell the creator to do some work
        conversationCreator.tell(newCreateConversationMessage(), mockActor().getRef());

        // worker should have received the work
        mockWorker.expectMsgClass(CreateConversation.class);
    }

    @Test
    public void itShouldExecuteQueuedRequestsWhenThereIsAWorkerAvailable() {
        final ActorRef conversationCreator = newConversationCreator(1, mockActor().getRef());

        // tell the creator to do some work
        conversationCreator.tell(newCreateConversationMessage(), mockActor().getRef());

        // tell the creator there is a worker available
        final JavaTestKit mockWorker = mockActor();
        conversationCreator.tell(new WorkerAvailable(mockWorker.getRef()), ActorRef.noSender());

        // worker should have received the work
        mockWorker.expectMsgClass(CreateConversation.class);
    }

    @Test
    public void itShouldRejectRequestsIfTheWorkerSupervisorIsDown() {
        final JavaTestKit mockWorkerPool = mockActor();
        final ActorRef conversationCreator = newConversationCreator(1, mockWorkerPool.getRef());

        // kill the pool
        // TODO extract into a method
        final CompletionStage<Boolean> stopped = PatternsCS.gracefulStop(mockWorkerPool.getRef(), Duration.create(50, TimeUnit.MILLISECONDS));
        try {
            stopped.toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        final JavaTestKit mockRequester = mockActor();

        conversationCreator.tell(newCreateConversationMessage(), mockRequester.getRef());

        mockRequester.expectMsgEquals(ConversationCreatorMessage.CreatorOverloaded);
    }

    @Test
    public void itShouldRejectRequestsWhenQueueSizeIsReached() {
        final ActorRef conversationCreator = newConversationCreator(1, mockActor().getRef());
        final JavaTestKit mockRequester = mockActor();

        conversationCreator.tell(newCreateConversationMessage(), mockRequester.getRef());
        conversationCreator.tell(newCreateConversationMessage(), mockRequester.getRef());

        mockRequester.expectMsgEquals(ConversationCreatorMessage.CreatorOverloaded);
    }

    @Test
    public void itShouldRejectRequestsIfThereIsNoQueueConfigured() {
        final ActorRef conversationCreator = newConversationCreator(0, mockActor().getRef());
        final JavaTestKit mockRequester = mockActor();

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
