/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.Member;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.Convert;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;
import com.tangosol.util.filter.AlwaysFilter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jf 2019.11.20
 */
public class SubscriptionTest
    {
    @Test
    public void shouldSerializeUsingPof()
        {
        ConfigurablePofContext   serializer   = new ConfigurablePofContext("coherence-pof-config.xml");
        Subscription             subscription = new Subscription();
        Convert<String, Integer> convert      = Subscriber.Convert.using(Integer::parseInt);

        subscription.setSubscriptionHead(20);
        subscription.setPage(10);
        subscription.setPosition(1010);
        subscription.setFilter(new AlwaysFilter<>());
        subscription.setConverter(convert.getExtractor());

        Binary       binary = ExternalizableHelper.toBinary(subscription, serializer);
        Subscription result = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.getSubscriptionHead(), is(subscription.getSubscriptionHead()));
        assertThat(result.getPosition(), is(subscription.getPosition()));
        assertThat(result.getFilter().equals(subscription.getFilter()), is(true));
        assertThat(result.getConverter(), is(notNullValue()));
        assertThat(result.getDataVersion(), is(subscription.getImplVersion()));
        assertThat(result.toString(), is(notNullValue()));
        }
    }
