package com.github.lpedrosa.conversation.service;

import java.util.Objects;
import java.util.Optional;

public final class ConversationInfo {
    private final String id;
    private final String name;
    private final String applicationId;

    public ConversationInfo(String id, String name, String applicationId) {
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.applicationId = Objects.requireNonNull(applicationId);
    }

    public String getId() {
        return this.id;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(this.name);
    }

    public String getApplicationId() {
        return this.applicationId;
    }
}
