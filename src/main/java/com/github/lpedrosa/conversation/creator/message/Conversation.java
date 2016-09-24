package com.github.lpedrosa.conversation.creator.message;

import java.util.Optional;

public final class Conversation {
    private final String id;
    private final String name;
    private final String applicationId;

    public Conversation(String id, String name, String applicationId) {
        this.id = id;
        this.name = name;
        this.applicationId = applicationId;
    }

    public String getId() {
        return id;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public String getApplicationId() {
        return applicationId;
    }

}
