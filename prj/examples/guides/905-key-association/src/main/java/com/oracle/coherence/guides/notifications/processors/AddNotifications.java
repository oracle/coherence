/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.notifications.processors;

import com.oracle.coherence.guides.notifications.CustomerRepository;
import com.oracle.coherence.guides.notifications.model.Customer;
import com.oracle.coherence.guides.notifications.model.Notification;
import com.oracle.coherence.guides.notifications.model.NotificationId;
import com.tangosol.io.pof.schema.annotation.PortableType;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.UUID;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * An {@link com.tangosol.util.InvocableMap.EntryProcessor} that will add
 * notifications to a customer.
 *
 * @author Jonathan Knight 2022.06.20
 * @since 22.06
 */
// # tag::src-one[]
@PortableType(id = 1100, version = 1)
public class AddNotifications
        implements InvocableMap.EntryProcessor<String, Customer, Void> {

    /**
     * The notifications to add to the customer.
     */
    private Map<String, List<Notification>> notifications;

    /**
     * Create a {@link AddNotifications} processor.
     *
     * @param notifications  the notifications to add to the customer
     */
    public AddNotifications(Map<String, List<Notification>> notifications) {
        this.notifications = notifications;
    }

    // # tag::process[]
    @Override
    @SuppressWarnings("unchecked")
    public Void process(InvocableMap.Entry<String, Customer> entry)
    // # end::src-one[]
    {
        BackingMapManagerContext          context          = entry.asBinaryEntry().getContext();
        Converter<NotificationId, Binary> converter        = context.getKeyToInternalConverter();
        BackingMapContext                 ctxNotifications = context.getBackingMapContext(
                CustomerRepository.NOTIFICATIONS_MAP_NAME);
        String                            customerId       = entry.getKey();
        LocalDateTime                     now              = LocalDateTime.now();

        notifications.forEach((region, notificationsForRegion)->
        {
            notificationsForRegion.forEach(notification->
            {
                long ttlInMillis = ChronoUnit.MILLIS.between(now, notification.getTTL());
                if (ttlInMillis > 0) {
                    NotificationId id        = new NotificationId(customerId, region, new UUID());
                    Binary         binaryKey = converter.convert(id);
                    BinaryEntry<NotificationId, Notification> binaryEntry =
                            (BinaryEntry<NotificationId, Notification>) ctxNotifications.getBackingMapEntry(binaryKey);

                    binaryEntry.setValue(notification);
                    binaryEntry.expire(ttlInMillis);
                }
            });
        });

        return null;
    }
    // # end::process[]
}
