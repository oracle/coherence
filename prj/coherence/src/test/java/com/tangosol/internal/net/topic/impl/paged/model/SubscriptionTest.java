/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.Convert;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.filter.AlwaysFilter;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author jf 2019.11.20
 */
public class SubscriptionTest
    {
    @Test
    public void shouldSerializeUsingPof()
        throws Exception
        {
        ConfigurablePofContext   serializer   = new ConfigurablePofContext("coherence-pof-config.xml");
        Subscription             subscription = new Subscription();
        Convert<String, Integer> convert      = Subscriber.Convert.using(Integer::parseInt);

        subscription.setSubscriptionHead(20);
        subscription.setPage(10);
        subscription.setPosition(1010);
        subscription.setFilter(new AlwaysFilter());
        subscription.setConverter(convert.getFunction());

        Binary       binary = ExternalizableHelper.toBinary(subscription, serializer);
        Subscription result = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.getSubscriptionHead(), is(subscription.getSubscriptionHead()));
        assertThat(result.getPosition(), is(subscription.getPosition()));
        assertThat(result.getFilter().equals(subscription.getFilter()), is(true));
        assertNotNull(result.getConverter());
        assertThat(result.getDataVersion(), is(subscription.getImplVersion()));
        assertNotNull(result.toString());
        }
    }
