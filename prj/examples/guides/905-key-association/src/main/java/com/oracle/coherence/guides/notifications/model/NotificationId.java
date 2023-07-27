/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.notifications.model;

import com.tangosol.io.pof.schema.annotation.PortableType;

import com.tangosol.net.cache.KeyAssociation;

import com.tangosol.util.UUID;
import com.tangosol.util.comparator.SafeComparator;

import java.util.Comparator;
import java.util.Objects;

/**
 * An identifier for a notification.
 *
 * @author Jonathan Knight 2022.06.20
 * @since 22.06
 */
// # tag::src[]
@PortableType(id = 1011, version = 1)
public class NotificationId
        implements KeyAssociation<String>, Comparable<NotificationId> {

    /**
     * The customer the notification is for.
     */
    private String customerId;

    /**
     * The region the notification applies to.
     */
    private String region;

    /**
     * The notification unique identifier.
     */
    private UUID id;

    /**
     * Create a notification identifier.
     *
     * @param customerId  the customer the notification is for
     * @param region      the region the notification applies to
     * @param id          the notification identifier
     */
    public NotificationId(String customerId, String region, UUID id) {
        this.customerId = customerId;
        this.region = region;
        this.id = id;
    }

    /**
     * Returns the identifier of the customer the notification is for.
     *
     * @return the identifier of the customer the notification is for
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Returns the region the notification applies to.
     *
     * @return the region the notification applies to
     */
    public String getRegion() {
        return region;
    }

    /**
     * Returns the notification identifier.
     *
     * @return the notification identifier
     */
    public UUID getId() {
        return id;
    }

    // # tag::ka[]
    @Override
    public String getAssociatedKey() {
        return customerId;
    }
    // # end::ka[]

    @Override
    public int compareTo(NotificationId o) {
        int n = SafeComparator.compareSafe(Comparator.naturalOrder(), customerId, o.customerId);
        if (n == 0) {
            n = Long.compare(id.getTimestamp(), o.id.getTimestamp());
            if (n == 0) {
                n = Long.compare(id.getCount(), o.id.getCount());
            }
        }
        return n;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NotificationId that = (NotificationId) o;
        return Objects.equals(customerId, that.customerId) && Objects.equals(region, that.region)
               && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, region, id);
    }
}
// # end::src[]
