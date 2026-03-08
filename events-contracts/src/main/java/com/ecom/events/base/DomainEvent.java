package com.ecom.events.base;


import java.time.Instant;
import java.util.Objects;
import java.util.UUID;


public abstract class DomainEvent {

    private String eventType;
    private int eventVersion;
    private UUID eventId;
    private Instant occurredAt;

    protected DomainEvent() {}

    protected DomainEvent(String eventType) {
        this.eventType = eventType;
        this.eventVersion = 1;
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public String getEventType() {
        return eventType;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setEventId(UUID uuid) {
        this.eventId = uuid;
    }
    public void setOccurredAt(Instant now) {
        this.occurredAt = now;
    }
    public void setEventVersion(int version) {
        this.eventVersion = version;
    }
}
