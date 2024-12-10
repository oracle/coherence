/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.notifications.extractors;

import com.oracle.coherence.guides.notifications.CustomerRepository;

import com.oracle.coherence.guides.notifications.model.Customer;
import com.oracle.coherence.guides.notifications.model.Notification;
import com.oracle.coherence.guides.notifications.model.NotificationId;

import com.tangosol.io.pof.schema.annotation.PortableType;

import com.tangosol.net.BackingMapContext;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapIndex;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.filter.EqualsFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.stream.Collectors;

/**
 * A {@link ValueExtractor} that extracts a {@link List} of {@link Notification notifications}
 * for a {@link Customer} and optional region.
 *
 * @author Jonathan Knight 2022.06.20
 * @since 22.06
 */
// # tag::one[]
@PortableType(id = 1200, version = 1)
public class NotificationExtractor
        extends AbstractExtractor<Customer, List<Notification>> {

    /**
     * An optional region identifier to use to retrieve
     * only notifications for a specific region.
     */
    private String region;

    /**
     * Create a {@link NotificationExtractor} that will specifically
     * target the key when used to extract from a cache entry.
     *
     * @param region an optional region identifier
     */
    public NotificationExtractor(String region) {
        this.region = region;
    }
    // # end::one[]

    // # tag::extract[]
    @Override
    @SuppressWarnings( {"rawtypes", "unchecked"})
    public List<Notification> extractFromEntry(Map.Entry entry) {
        BinaryEntry binaryEntry = (BinaryEntry) entry;
        BackingMapContext ctx = binaryEntry.getContext()
                                           .getBackingMapContext(CustomerRepository.NOTIFICATIONS_MAP_NAME);
        Map<ValueExtractor, MapIndex> indexMap = ctx.getIndexMap(binaryEntry.getKeyPartition());

        MapIndex<Binary, Notification, String> index = indexMap
                .get(ValueExtractor.of(NotificationId::getCustomerId).fromKey());

        String      customerId = (String) entry.getKey();
        Set<Binary> keys       = index.getIndexContents().get(customerId);

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        if (region != null && !region.isBlank()) {
            // copy the keys, so we don't modify the underlying index
            keys = new HashSet<>(keys);

            ValueExtractor<NotificationId, String> extractor = ValueExtractor.of(NotificationId::getRegion).fromKey();
            EqualsFilter<NotificationId, String>   filter    = new EqualsFilter<>(extractor, region);
            filter.applyIndex(indexMap, keys);
        }

        // # tag::streams[]
        Comparator<InvocableMap.Entry> comparator = (e1, e2)->
                SafeComparator.compareSafe(Comparator.naturalOrder(), e1.getKey(), e2.getKey());

        return keys.stream()
                   .map(ctx::getReadOnlyEntry)             // <1>
                   .filter(InvocableMap.Entry::isPresent)  // <2>
                   .sorted(comparator)                     // <3>
                   .map(InvocableMap.Entry::getValue)      // <4>
                   .map(Notification.class::cast)          // <5>
                   .collect(Collectors.toList());          // <6>
        // # end::streams[]
    }
    // # end::extract[]

    @Override
    @SuppressWarnings("rawtypes")
    public List<Notification> extractOriginalFromEntry(MapTrigger.Entry entry) {
        return extractFromEntry(entry);
    }

    // # tag::two[]
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        NotificationExtractor that = (NotificationExtractor) o;
        return Objects.equals(region, that.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), region);
    }
    // # end::two[]
}
