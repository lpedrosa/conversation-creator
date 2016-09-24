package com.github.lpedrosa.conversation.creator.message;

public final class CreateConversation {
    private final String applicationId;

    public CreateConversation(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationId() {
        return applicationId;
    }
}
