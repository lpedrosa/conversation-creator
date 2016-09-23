package com.github.lpedrosa.conversation.service;

import java.util.concurrent.CompletableFuture;

public interface ConversationBackend {
    CompletableFuture<ConversationInfo> createConversation(String applicationId);
}
