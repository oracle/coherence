/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;

import com.tangosol.util.Binary;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Unit tests for Nullable implementations.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.10
 */
public class NullableTest
    {
    @Test
    public void testEmpty()
        {
        Object o = null;
        assertThat(Nullable.of(o), instanceOf(NullableEmpty.class));
        assertThat(Nullable.of(o), sameInstance(Nullable.of((Long) null)));
        assertThat(Nullable.of(o).get(), is(nullValue()));
        }

    @Test
    public void testBoolean()
        {
        assertThat(Nullable.of(true), instanceOf(NullableBoolean.class));
        assertThat(Nullable.of(false), instanceOf(NullableBoolean.class));
        assertThat(Nullable.of(Boolean.TRUE), instanceOf(NullableBoolean.class));
        assertThat(Nullable.of(Boolean.FALSE), instanceOf(NullableBoolean.class));

        assertThat(Nullable.of(true).get(), is(true));
        assertThat(Nullable.of(false).get(), is(false));
        assertThat(Nullable.of(Boolean.TRUE).get(), is(true));
        assertThat(Nullable.of(Boolean.FALSE).get(), is(false));

        assertThat(Nullable.of(true), sameInstance(Nullable.of(true)));
        assertThat(Nullable.of(false), sameInstance(Nullable.of(false)));
        assertThat(Nullable.of(true), sameInstance(Nullable.of(Boolean.TRUE)));
        assertThat(Nullable.of(false), sameInstance(Nullable.of(Boolean.FALSE)));
        }

    @Test
    public void testInteger()
        {
        assertThat(Nullable.of(1), instanceOf(NullableInt.class));
        assertThat(Nullable.of(Integer.valueOf(0)), instanceOf(NullableInt.class));

        assertThat(Nullable.of(1).get(), is(1));
        assertThat(Nullable.of(Integer.valueOf(0)).get(), is(0));
        }

    @Test
    public void testLong()
        {
        assertThat(Nullable.of(1L), instanceOf(NullableLong.class));
        assertThat(Nullable.of(Long.valueOf(0L)), instanceOf(NullableLong.class));

        assertThat(Nullable.of(1L).get(), is(1L));
        assertThat(Nullable.of(Long.valueOf(0L)).get(), is(0L));
        }

    @Test
    public void testShort()
        {
        assertThat(Nullable.of((short) 1), instanceOf(NullableShort.class));
        assertThat(Nullable.of(Short.valueOf((short) 0)), instanceOf(NullableShort.class));

        assertThat(Nullable.of((short) 1).get(), is((short) 1));
        assertThat(Nullable.of(Short.valueOf((short) 0)).get(), is((short) 0));
        }

    @Test
    public void testByte()
        {
        assertThat(Nullable.of((byte) 1), instanceOf(NullableByte.class));
        assertThat(Nullable.of(Byte.valueOf((byte) 0)), instanceOf(NullableByte.class));

        assertThat(Nullable.of((byte) 1).get(), is((byte) 1));
        assertThat(Nullable.of(Byte.valueOf((byte) 0)).get(), is((byte) 0));
        }

    @Test
    public void testDouble()
        {
        assertThat(Nullable.of(1.0d), instanceOf(NullableDouble.class));
        assertThat(Nullable.of(Double.valueOf(0.0d)), instanceOf(NullableDouble.class));

        assertThat(Nullable.of(1.0d).get(), is(1.0d));
        assertThat(Nullable.of(Double.valueOf(0.0d)).get(), is(0.0d));
        }

    @Test
    public void testFloat()
        {
        assertThat(Nullable.of(1.0f), instanceOf(NullableFloat.class));
        assertThat(Nullable.of(Float.valueOf(0.0f)), instanceOf(NullableFloat.class));

        assertThat(Nullable.of(1.0f).get(), is(1.0f));
        assertThat(Nullable.of(Float.valueOf(0.0f)).get(), is(0.0f));
        }

    @Test
    public void testNullableReference()
        {
        assertThat(Nullable.of(Binary.TRUE), instanceOf(Binary.class));

        assertThat(Nullable.of(Binary.TRUE).get(), is(Binary.TRUE));
        assertThat(Nullable.of(Binary.TRUE).get(), sameInstance(Binary.TRUE));
        }

    @Test
    public void testNonNullableReference()
        {
        assertThat(Nullable.of("foo"), instanceOf(NullableWrapper.class));
        assertThat(Nullable.of("bar").get(), is("bar"));
        }
    }
