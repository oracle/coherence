/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.util.Binary;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk 2015.05.19
 */
public class ContentKeyTest
    {
    @Test
    public void shouldSerializeToBinary()
        {
        ContentKey position = new ContentKey(0, 783L, 1500);
        Binary     binary   = position.toBinary(23);
        ContentKey result   = ContentKey.fromBinary(binary, /*fDecorated*/ true);

        assertThat(result.getChannel(), is(0));
        assertThat(result.getPage(), is(783L));
        assertThat(result.getElement(), is(1500));
        }

    @Test
    public void shouldReturnPartitionId()
        {
        ContentKey position = new ContentKey(0, 19L, 100);

        assertThat(position.getPartitionId(), is(Page.Key.mapPageToPartition(0, 19)));
        }

    @Test
    public void shouldBeEqual()
        {
        ContentKey position1 = new ContentKey(0, 19L, 100);
        ContentKey position2 = new ContentKey(0, 19L, 100);

        assertThat(position1.equals(position2), is(true));
        assertThat(position1, is(position1));
        assertThat(position1.hashCode(), is(position2.hashCode()));
        }

    @Test
    public void shouldBeEqualWithDifferentPageId()
        {
        ContentKey position1 = new ContentKey(0, 19L, 100);
        ContentKey position2 = new ContentKey(0, 20L, 100);

        assertThat(position1.equals(position2), is(false));
        }

    @Test
    public void shouldBeEqualWithDifferentElementId()
        {
        ContentKey position1 = new ContentKey(0, 19L, 100);
        ContentKey position2 = new ContentKey(0, 19L, 101);

        assertThat(position1.equals(position2), is(false));
        }

    @Test
    public void shouldBeEqualWithDifferentType()
        {
        ContentKey position1 = new ContentKey(0, 19L, 100);

        assertThat(position1, is(not("Foo")));
        }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionForInvalidBinary()
        {
        Binary bin = new Binary();

        ContentKey.fromBinary(bin);
        }
    }
