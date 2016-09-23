package com.github.lpedrosa.conversation.creator.worker;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status.Failure;
import akka.actor.UntypedActor;
import akka.dispatch.ExecutionContexts;
import akka.pattern.PatternsCS;
import scala.concurrent.ExecutionContext;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.github.lpedrosa.conversation.creator.message.CreateConversation;
import com.github.lpedrosa.conversation.service.ConversationBackend;
import com.github.lpedrosa.conversation.service.ConversationInfo;

public class CreatorWorker extends UntypedActor {

    private final ConversationBackend backend;
    private final ExecutorService waitingPool;

    private ActorRef originalSender;

    public static Props props(ConversationBackend backend, ExecutorService waitingPool) {
        return Props.create(CreatorWorker.class, () -> new CreatorWorker(backend, waitingPool));
    }

    private CreatorWorker(ConversationBackend backend, ExecutorService waitingPool) {
        this.backend = Objects.requireNonNull(backend);
        this.waitingPool = Objects.requireNonNull(waitingPool);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof CreateConversation) {
            createConversation((CreateConversation) message);
        } else if (message instanceof ConversationInfo) {
            this.originalSender.tell(message, ActorRef.noSender());
        } else if (message instanceof Failure) {
            throw ((Failure)message).cause();
        } else {
            unhandled(message);
        }
    }

    private void createConversation(CreateConversation message) {
        this.originalSender = getSender();

        CompletableFuture<ConversationInfo> response =
                this.backend.createConversation(message.getApplicationId());

        ExecutionContext context = ExecutionContexts.fromExecutor(this.waitingPool);
        PatternsCS.pipe(response, context).to(getSelf());
    }
}
