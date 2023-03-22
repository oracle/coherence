/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.responsecaching;

import java.io.Serializable;

/**
 * Domain class, representing a message.
 */
public class Message
        implements Serializable {
    private String message;

    public Message() {
    }

    public Message(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
                '}';
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Message message1 = (Message) o;

        return getMessage() != null
                ? getMessage().equals(message1.getMessage())
                : message1.getMessage() == null;
    }

    public int hashCode() {
        return getMessage() != null ? getMessage().hashCode() : 0;
    }
}
