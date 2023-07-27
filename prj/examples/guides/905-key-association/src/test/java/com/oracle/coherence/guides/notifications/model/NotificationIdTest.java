/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.notifications.model;

import com.tangosol.util.UUID;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NotificationIdTest {
    @Test
    public void shouldSort() {
        InetAddress    address = null;
        NotificationId id1     = new NotificationId("A1", "Z", new UUID(0, address, 0, 9));
        NotificationId id2     = new NotificationId("A1", "A", new UUID(1, address, 0, 8));
        NotificationId id3     = new NotificationId("A1", "Z", new UUID(2, address, 0, 7));
        NotificationId id4     = new NotificationId("B1", "A", new UUID(0, address, 0, 8));
        NotificationId id5     = new NotificationId("B1", "Z", new UUID(1, address, 0, 8));
        NotificationId id6     = new NotificationId("B1", "A", new UUID(2, address, 0, 7));

        List<NotificationId> list = new ArrayList<>();
        list.add(id6);
        list.add(id5);
        list.add(id4);
        list.add(id3);
        list.add(id2);
        list.add(id1);

        Collections.sort(list);

        assertThat(list.get(0), is(id1));
        assertThat(list.get(1), is(id2));
        assertThat(list.get(2), is(id3));
        assertThat(list.get(3), is(id4));
        assertThat(list.get(4), is(id5));
        assertThat(list.get(5), is(id6));
    }

}
