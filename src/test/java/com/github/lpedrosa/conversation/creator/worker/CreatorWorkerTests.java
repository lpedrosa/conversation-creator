package com.github.lpedrosa.conversation.creator.worker;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import akka.actor.*;
import akka.dispatch.ExecutionContexts;
import akka.pattern.Backoff;
import akka.pattern.BackoffSupervisor;
import akka.testkit.JavaTestKit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.prism.paint.Stop;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.github.lpedrosa.conversation.creator.message.CreateConversation;
import com.github.lpedrosa.conversation.service.ConversationBackend;
import com.github.lpedrosa.conversation.service.ConversationInfo;
import com.github.lpedrosa.util.AkkaTest;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

@RunWith(JUnit4.class)
public class CreatorWorkerTests extends AkkaTest {

    private ConversationBackend mockBackend;
    private MonitoredRef creatorWorker;

    @Before
    public void before() {
        this.mockBackend = mock(ConversationBackend.class);
        Props creatorWorkerProps = CreatorWorker.props(mockBackend, system().dispatcher());
        this.creatorWorker = newDeadMonitor(creatorWorkerProps);
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

        CompletableFuture<ConversationInfo> response = new CompletableFuture<>();
        response.completeExceptionally(new Exception("Boom!"));

        when(this.mockBackend.createConversation(anyString()))
                    .thenReturn(response);

        // when
        this.creatorWorker.tell(new CreateConversation(applicationId), ActorRef.noSender());

        // then
        this.creatorWorker.expectActorToBeDead();
    }


}
