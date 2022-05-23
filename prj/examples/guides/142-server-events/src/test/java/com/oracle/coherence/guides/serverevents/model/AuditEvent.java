/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.serverevents.model;

import com.tangosol.util.UUID;

import java.io.Serializable;

import java.util.Objects;

/**
 * A class to store audit events.
 *
 * @author Tim Middleton 2022.05.04
 */
public class AuditEvent
        implements Serializable {

    // #tag::vars[]
    /**
     * Unique Id for the audit event.
     */
    private UUID id;

    /**
     * The target of the event such as cache, partition, etc.
     */
    private String target;

    /**
     * The type of event.
     */
    private String eventType;

    /**
     * Specific event data.
     */
    private String eventData;

    /**
     * Time of the event.
     */
    private long   eventTime;
    // #end::vars[]

    public AuditEvent(String target, String eventType, String eventData) {
        this.id =new UUID();
        this.target = target;
        this.eventType = eventType;
        this.eventData = eventData;
        this.eventTime = System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuditEvent that = (AuditEvent) o;

        if (eventTime != that.eventTime) return false;
        if (!Objects.equals(id, that.id)) return false;
        if (!Objects.equals(target, that.target)) return false;
        if (!Objects.equals(eventType, that.eventType)) return false;
        return Objects.equals(eventData, that.eventData);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 31 * result + (eventData != null ? eventData.hashCode() : 0);
        result = 31 * result + (int) (eventTime ^ (eventTime >>> 32));
        return result;
    }

    @Override
    public String toString() {
        String s = id.toString();
        return "AuditEvent{" +
               "id=" + s.substring(s.length() - 10) +
               ", target='" + target + '\'' +
               ", eventType='" + eventType + '\'' +
               ", eventData='" + eventData + '\'' +
               ", eventTime=" + eventTime +
               '}';
    }
}
