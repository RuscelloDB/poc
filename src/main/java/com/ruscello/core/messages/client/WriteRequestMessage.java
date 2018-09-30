package com.ruscello.core.messages.client;

import java.util.UUID;

public abstract class WriteRequestMessage {

    private final UUID internalCorrId;
    private final UUID correlationId;
    // private final IEnvelope Envelope;
    private final boolean requireMaster;

    // private final IPrincipal User;
    private final String login;
    private final String password;

    protected WriteRequestMessage(UUID internalCorrId, UUID correlationId, boolean requireMaster, String login, String password) {
        this.internalCorrId = internalCorrId;
        this.correlationId = correlationId;
        this.requireMaster = requireMaster;
        this.login = login;
        this.password = password;
    }

    public UUID getInternalCorrId() {
        return internalCorrId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public boolean isRequireMaster() {
        return requireMaster;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
