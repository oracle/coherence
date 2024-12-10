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

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author jf 2019.11.20
 */
public class UsageTest
    {
    @Test
    public void shouldSerializeUsingPof()
        throws Exception
        {
        ConfigurablePofContext serializer = new ConfigurablePofContext("coherence-pof-config.xml");
        Usage                  usage      = new Usage();

        usage.setPartitionHead(5);
        usage.setPartitionMax(257);
        usage.setPublicationTail(25);
        usage.removeAnonymousSubscriber(SubscriberGroupId.anonymous());
        usage.addAnonymousSubscriber(SubscriberGroupId.anonymous());
        usage.addAnonymousSubscriber(SubscriberGroupId.anonymous());
        usage.addAnonymousSubscriber(SubscriberGroupId.withName("durableSubscription"));

        Binary binary = ExternalizableHelper.toBinary(usage, serializer);
        Usage result = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.getPartitionHead(), is(usage.getPartitionHead()));
        assertThat(result.getPartitionMax(), is(usage.getPartitionMax()));
        assertThat(result.getPartitionTail(), is(usage.getPartitionTail()));
        assertTrue(result.getAnonymousSubscribers().containsAll(usage.getAnonymousSubscribers()));
        assertThat(result.getDataVersion(), is(usage.getImplVersion()));
        assertNotNull(usage.toString());
        }

    @Test
    public void shouldSerializeUsageKeyUsingPof()
        {
        ConfigurablePofContext serializer = new ConfigurablePofContext("coherence-pof-config.xml");
        Usage.Key              key        = new Usage.Key(12, 0);

        Binary    binary = ExternalizableHelper.toBinary(key, serializer);
        Usage.Key result = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.getChannelId(), is(key.getChannelId()));
        assertThat(result.getPartitionId(), is(key.getPartitionId()));
        assertThat(result.hashCode(), is(key.hashCode()));
        assertEquals(result, key);
        assertNotEquals(result, null);
        assertNotNull(result.toString());
        }
    }
