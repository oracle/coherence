/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.util.Binary;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.05.19
 */
public class PositionTest
    {
    @Test
    public void shouldSerializeToBinary()
            throws Exception
        {
        Position position = new Position(0, 783L, 1500);
        Binary   binary   = position.toBinary(23);
        Position result   = Position.fromBinary(binary, /*fDecorated*/ true);

        assertThat(result.getChannel(), is(0));
        assertThat(result.getPage(), is(783L));
        assertThat(result.getElement(), is(1500));
        }

    @Test
    public void shouldReturnPartitionId()
            throws Exception
        {
        Position position = new Position(0, 19L, 100);

        assertThat(position.getPartitionId(), is(Page.Key.mapPageToPartition(0, 19)));
        }

    @Test
    public void shouldBeEqual()
            throws Exception
        {
        Position position1 = new Position(0, 19L, 100);
        Position position2 = new Position(0, 19L, 100);

        assertThat(position1.equals(position2), is(true));
        assertEquals(position1, position1);
        assertThat(position1.hashCode(), is(position2.hashCode()));
        }

    @Test
    public void shouldBeEqualWithDifferentPageId()
            throws Exception
        {
        Position position1 = new Position(0, 19L, 100);
        Position position2 = new Position(0, 20L, 100);

        assertThat(position1.equals(position2), is(false));
        }

    @Test
    public void shouldBeEqualWithDifferentElementId()
            throws Exception
        {
        Position position1 = new Position(0, 19L, 100);
        Position position2 = new Position(0, 19L, 101);

        assertThat(position1.equals(position2), is(false));
        }

    @Test
    public void shouldBeEqualWithDifferentType()
            throws Exception
        {
        Position position1 = new Position(0, 19L, 100);

        assertThat(position1.equals("Foo"), is(false));
        }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionForInvalidBinary()
        {
        Binary bin = new Binary();

        Position.fromBinary(bin);
        }
    }
