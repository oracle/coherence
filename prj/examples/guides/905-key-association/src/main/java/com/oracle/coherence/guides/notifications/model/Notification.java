/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.notifications.model;

import com.tangosol.io.pof.schema.annotation.PortableType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A representation of a notification with body text and
 * a specific time-to-live.
 *
 * @author Jonathan Knight 2022.06.20
 * @since 22.06
 */
// # tag::src[]
@PortableType(id = 1010, version = 1)
public class Notification {

    /**
     * The notification text.
     */
    private String body;

    /**
     * The time the notification expires.
     */
    private LocalDateTime ttl;

    /**
     * Create a {@link Notification}.
     *
     * @param body  the notification text
     * @param ttl   the time the notification expires
     */
    public Notification(String body, LocalDateTime ttl) {
        this.body = body;
        this.ttl = ttl;
    }

    /**
     * Returns the notification text.
     *
     * @return the notification text
     */
    public String getBody() {
        return body;
    }

    /**
     * Returns the time the notification expires.
     *
     * @return the time the notification expires
     */
    public LocalDateTime getTTL() {
        return ttl;
    }
}
// # end::src[]
