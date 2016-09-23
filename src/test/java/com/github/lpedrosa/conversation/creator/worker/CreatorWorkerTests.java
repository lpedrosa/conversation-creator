package com.github.lpedrosa.conversation.creator.worker;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.testkit.JavaTestKit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.github.lpedrosa.conversation.creator.message.CreateConversation;
import com.github.lpedrosa.conversation.service.ConversationBackend;
import com.github.lpedrosa.conversation.service.ConversationInfo;
import com.github.lpedrosa.util.AkkaTest;

@RunWith(JUnit4.class)
public class CreatorWorkerTests extends AkkaTest {

    private ConversationBackend mockBackend;
    private ExecutorService testService;
    private ActorRef creatorWorker;

    @Before
    public void before() {
        this.mockBackend = mock(ConversationBackend.class);
        this.testService = Executors.newSingleThreadExecutor();
        this.creatorWorker = system().actorOf(CreatorWorker.props(mockBackend, testService));
    }

    @Test
    public void itShouldReplyWithSuccessfulCreationWhenEverythingWasOk() {
        // given
        final String applicationId = "applicationId";

        ConversationInfo expectedResponse = new ConversationInfo("id", null, applicationId);
        when(mockBackend.createConversation(anyString()))
                .thenReturn(CompletableFuture.completedFuture(expectedResponse));

        final JavaTestKit mockRequester = mockActor();

        // when
        this.creatorWorker.tell(new CreateConversation(applicationId), mockRequester.getRef());

        // then
        ConversationInfo response = mockRequester.expectMsgClass(ConversationInfo.class);
        Assert.assertEquals(expectedResponse.getId(), response.getId());
        Assert.assertEquals(expectedResponse.getApplicationId(), response.getApplicationId());
    }

    @Test
    public void itShouldDieIfTheServiceCallFailed() {
        // given
        final String applicationId = "applicationId";

        ConversationInfo expectedResponse = new ConversationInfo("id", null, applicationId);
        CompletableFuture<ConversationInfo> response = new CompletableFuture<>();
        response.completeExceptionally(new Exception("Boom!"));
        when(this.mockBackend.createConversation(anyString()))
                    .thenReturn(response);

        final JavaTestKit creatorWatcher = newDeadWatcher(this.creatorWorker);

        // when
        this.creatorWorker.tell(new CreateConversation(applicationId), ActorRef.noSender());

        // then
        creatorWatcher.expectMsgClass(Terminated.class);
    }

}
