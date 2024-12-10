/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.topics;

import java.io.Serializable;
import java.util.Objects;

/**
 * A class to represent messages sent by a chat application.
 *
 * @author Tim Middleton 2021.02.16
 */
public class ChatMessage implements Serializable {
    // tag::properties[]
    /**
     * Date the message was sent.
     */
    private final long date;

    /**
     * The user who sent the message.
     */
    private final String fromUserId;

    /**
     * The recipient of the message or null if public message.
     */
    private final String toUserId;

    /**
     * The type of message.
     */
    private final Type type;

    /**
     * The contents of the message.
     */
    private final String message;
    // end::properties[]

    /**
     * Constructs a chat message.
     *
     * @param fromUserId user who sent the message
     * @param toUserId   recipient of the message or null if public message
     * @param type       type of message
     * @param message    contents of the message
     */
    public ChatMessage(String fromUserId, String toUserId, Type type, String message) {
        this.date = System.currentTimeMillis();
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.type = type;
        this.message = message;
    }

    /**
     * Returns the message date.
     * @return the message date
     */
    public long getDate() {
        return date;
    }

    /**
     * Returns the user who sent the message.
     * @return user who sent the message
     */
    public String getFromUserId() {
        return fromUserId;
    }

    /**
     * Returns the recipient of the message or null if public message
     * @return the recipient of the message or null if public message
     */
    public String getToUserId() {
        return toUserId;
    }

    /**
     * Returns the type of the message.
     * @return the type of the message
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the message content.
     * @return the message content
     */
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChatMessage that = (ChatMessage) o;
        return date == that.date &&
                Objects.equals(fromUserId, that.fromUserId) &&
                Objects.equals(toUserId, that.toUserId) &&
                type == that.type &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, fromUserId, toUserId, type, message);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "date=" + date +
                ", fromUserId='" + fromUserId + '\'' +
                ", toUserId='" + toUserId + '\'' +
                ", type=" + type +
                ", message='" + message + '\'' +
                '}';
    }

    public static enum Type {
        JOIN,
        LEAVE,
        MESSAGE
    }
}
