package model;

import java.time.LocalDateTime;

// Audit trail row for sensitive blood-service actions.
public class AuditLogRecord {
    private final LocalDateTime eventTime;
    private final String actor;
    private final String actionType;
    private final String entityType;
    private final String entityId;
    private final String details;

    public AuditLogRecord(LocalDateTime eventTime, String actor, String actionType, String entityType, String entityId, String details) {
        this.eventTime = eventTime;
        this.actor = actor;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public String getActor() {
        return actor;
    }

    public String getActionType() {
        return actionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }
}
