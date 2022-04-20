/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.08.19
 */
public class SubscriptionKeyTest
    {
    @Test
    public void shouldSerializeUsingPof()
            throws Exception
        {
        ConfigurablePofContext serializer = new ConfigurablePofContext("coherence-pof-config.xml");
        Subscription.Key       key        = new Subscription.Key(19, 0, ID_FOO);
        Binary                 binary     = ExternalizableHelper.toBinary(key, serializer);
        Subscription.Key       result     = (Subscription.Key) ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.getPartitionId(), is(key.getPartitionId()));
        assertThat(result.getGroupId(), is(key.getGroupId()));
        }

    @Test
    public void shouldTestForEquality() throws Exception
        {
        HashMap<Subscription.Key,String> map = new HashMap<>();

        map.put(new Subscription.Key(1, 0, ID_FOO), "1-Foo");
        map.put(new Subscription.Key(2, 0, ID_FOO), "2-Foo");
        map.put(new Subscription.Key(1, 0, ID_BAR), "1-Bar");
        map.put(new Subscription.Key(2, 0, ID_BAR), "2-Bar");

        assertThat(map.get(new Subscription.Key(1, 0, ID_FOO)), is("1-Foo"));
        assertThat(map.get(new Subscription.Key(2, 0, ID_FOO)), is("2-Foo"));
        assertThat(map.get(new Subscription.Key(1, 0, ID_BAR)), is("1-Bar"));
        assertThat(map.get(new Subscription.Key(2, 0, ID_BAR)), is("2-Bar"));

        Subscription.Key key = new Subscription.Key(1, 0, ID_FOO);
        assertThat(key.equals(key), is(true));
        assertEquals(key, new Subscription.Key(1, 0, SubscriberGroupId.withName("Foo")));
        assertThat(key.equals(null), is(false));
        assertThat(key.equals(new Subscription.Key(1, 0, ID_BAR)), is(false));
        assertThat(key.equals(new Subscription.Key(2, 0, ID_FOO)), is(false));
        assertNotNull(key.toString());
        }

    public static final SubscriberGroupId ID_FOO = new SubscriberGroupId("Foo");
    public static final SubscriberGroupId ID_BAR = new SubscriberGroupId("Bar");
    }
