/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.net.Member;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jf 2020.01.14
 */
public class SubscriberGroupIdTest
    {
    @Test
    public void testEquality()
        {
        SubscriberGroupId groupSubscriber = SubscriberGroupId.withName("durableSubscriber");

        assertEquals(groupSubscriber, groupSubscriber);
        assertEquals(groupSubscriber, SubscriberGroupId.withName("durableSubscriber"));
        assertNotEquals(groupSubscriber, SubscriberGroupId.withName("differentSubscriber"));
        assertNotEquals(groupSubscriber, null);

        Member member = mock(Member.class);
        when(member.getTimestamp()).thenReturn(12345L);

        SubscriberGroupId anonymousGroup1 = new SubscriberGroupId(member);
        SubscriberGroupId anonymousGroup2 = new SubscriberGroupId(member);

        assertNotEquals(anonymousGroup1, anonymousGroup2);
        assertEquals(anonymousGroup1, anonymousGroup1);
        assertEquals(anonymousGroup2, anonymousGroup2);
        }
    }
