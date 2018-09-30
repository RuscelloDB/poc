package com.ruscello.core.messages.client;

import java.util.UUID;

public class DeleteStream extends WriteRequestMessage {

    private final String eventStreamId;
    private final long expectedVersion;
    private final boolean hardDelete;


    public DeleteStream(UUID internalCorrId,
                        UUID correlationId,
                        // IEnvelope envelope,
                        boolean requireMaster,
                        String eventStreamId,
                        long expectedVersion,
                        boolean hardDelete
                        // IPrincipal user,
                        ) {
        this(internalCorrId, correlationId, requireMaster, eventStreamId, expectedVersion, hardDelete, null, null);
    }

    public DeleteStream(UUID internalCorrId,
                        UUID correlationId,
                        // IEnvelope envelope,
                        boolean requireMaster,
                        String eventStreamId,
                        long expectedVersion,
                        boolean hardDelete,
                        // IPrincipal user,
                        String login,
                        String password) {
        super(internalCorrId, correlationId,
                // envelope,
                requireMaster,
                //user,
                login, password);
        // Ensure.NotNullOrEmpty(eventStreamId, "eventStreamId");
        // if (expectedVersion < Data.ExpectedVersion.Any) throw new ArgumentOutOfRangeException("expectedVersion");

        this.eventStreamId = eventStreamId;
        this.expectedVersion = expectedVersion;
        this.hardDelete = hardDelete;
    }

    public String getEventStreamId() {
        return eventStreamId;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }

    public boolean isHardDelete() {
        return hardDelete;
    }
}
