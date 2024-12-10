/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

/**
 * @author jk  2018.01.23
 */
public class PublisherOptionTest
    {
    @Test
    public void shouldSerializeOnFailureUsingPof() throws Exception
        {
        Binary binary = ExternalizableHelper.toBinary(Publisher.OnFailure.Stop, f_serializer);
        Publisher.OnFailure result = ExternalizableHelper.fromBinary(binary, f_serializer);

        assertThat(result, is(Publisher.OnFailure.Stop));
        }

    @Test
    public void shouldSerializeOnFailureUsingExternalizable() throws Exception
        {
        Binary              binary = ExternalizableHelper.toBinary(Publisher.OnFailure.Stop);
        Publisher.OnFailure result = ExternalizableHelper.fromBinary(binary);

        assertThat(result, is(Publisher.OnFailure.Stop));
        }

    @Test
    public void shouldSerializeFailOnFullUsingPof() throws Exception
        {
        Binary               binary = ExternalizableHelper.toBinary(Publisher.FailOnFull.enabled(), f_serializer);
        Publisher.FailOnFull result = ExternalizableHelper.fromBinary(binary, f_serializer);

        assertThat(result, is(notNullValue()));
        }

    @Test
    public void shouldSerializeFailOnFullUsingExternalizable() throws Exception
        {
        Binary               binary = ExternalizableHelper.toBinary(Publisher.FailOnFull.enabled());
        Publisher.FailOnFull result = ExternalizableHelper.fromBinary(binary);

        assertThat(result, is(notNullValue()));
        }

    @Test
    public void shouldSerializeOrderByIdUsingPof() throws Exception
        {
        Binary            binary = ExternalizableHelper.toBinary(Publisher.OrderBy.id(19), f_serializer);
        Publisher.OrderBy result = ExternalizableHelper.fromBinary(binary, f_serializer);

        assertThat(result, is(notNullValue()));
        assertThat(result.getOrderId("foo"), is(19));
        assertNotNull(result.toString());
        }

    @Test
    public void shouldSerializeOrderByIdUsingExternalizable() throws Exception
        {
        Binary            binary = ExternalizableHelper.toBinary(Publisher.OrderBy.id(19));
        Publisher.OrderBy result = ExternalizableHelper.fromBinary(binary);

        assertThat(result, is(notNullValue()));
        assertThat(result.getOrderId("foo"), is(19));
        }

    @Test
    public void shouldSerializeOrderByNoneUsingPof() throws Exception
        {
        Binary            binary = ExternalizableHelper.toBinary(Publisher.OrderBy.none(), f_serializer);
        Publisher.OrderBy result = ExternalizableHelper.fromBinary(binary, f_serializer);

        assertThat(result, is(instanceOf(Publisher.OrderByNone.class)));
        assertNotNull(result.toString());
        }

    @Test
    public void shouldSerializeOrderByNoneUsingExternalizable() throws Exception
        {
        Binary            binary = ExternalizableHelper.toBinary(Publisher.OrderBy.none());
        Publisher.OrderBy result = ExternalizableHelper.fromBinary(binary);

        assertThat(result, is(instanceOf(Publisher.OrderByNone.class)));
        }

    @Test
    public void shouldSerializeOrderByThreadUsingPof() throws Exception
        {
        Binary            binary = ExternalizableHelper.toBinary(Publisher.OrderBy.thread(), f_serializer);
        Publisher.OrderBy result = ExternalizableHelper.fromBinary(binary, f_serializer);

        assertThat(result, is(instanceOf(Publisher.OrderByThread.class)));
        assertNotNull(result.toString());
        }

    @Test
    public void shouldSerializeOrderByThreadUsingExternalizable() throws Exception
        {
        Binary            binary = ExternalizableHelper.toBinary(Publisher.OrderBy.thread());
        Publisher.OrderBy result = ExternalizableHelper.fromBinary(binary);

        assertThat(result, is(instanceOf(Publisher.OrderByThread.class)));
        }

    @Test
    public void shouldSerializeOrderByValueUsingPof() throws Exception
        {
        Binary            binary = ExternalizableHelper.toBinary(Publisher.OrderBy.value(v -> 100), f_serializer);
        Publisher.OrderBy result = ExternalizableHelper.fromBinary(binary, f_serializer);

        assertThat(result, is(notNullValue()));
        assertThat(result.getOrderId("foo"), is(100));
        assertNotNull(result.toString());
        }

    @Test
    public void shouldSerializeOrderByValueUsingExternalizable() throws Exception
        {
        Binary            binary = ExternalizableHelper.toBinary(Publisher.OrderBy.value(v -> 100));
        Publisher.OrderBy result = ExternalizableHelper.fromBinary(binary);

        assertThat(result, is(notNullValue()));
        assertThat(result.getOrderId("foo"), is(100));
        }


    private static final ConfigurablePofContext f_serializer = new ConfigurablePofContext("coherence-pof-config.xml");
    }
