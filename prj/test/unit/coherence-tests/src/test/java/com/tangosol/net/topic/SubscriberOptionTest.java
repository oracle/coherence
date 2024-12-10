/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk  2018.01.30
 */
public class SubscriberOptionTest
    {
    @Test
    public void shouldSerializeCompleteOnEmptyValueUsingPof() throws Exception
        {
        Binary binary = ExternalizableHelper.toBinary(Subscriber.CompleteOnEmpty.enabled(), f_serializer);
        Subscriber.CompleteOnEmpty result = ExternalizableHelper.fromBinary(binary, f_serializer);

        assertThat(result, is(notNullValue()));
        }

    @Test
    public void shouldSerializeOrderByValueUsingExternalizable() throws Exception
        {
        Binary                     binary = ExternalizableHelper.toBinary(Subscriber.CompleteOnEmpty.enabled());
        Subscriber.CompleteOnEmpty result = ExternalizableHelper.fromBinary(binary);

        assertThat(result, is(notNullValue()));
        }

    @Test
    public void shouldSerializeConvertValueUsingPof() throws Exception
        {
        Binary             binary = ExternalizableHelper.toBinary(Subscriber.Convert.using(Object::toString), f_serializer);
        Subscriber.Convert result = ExternalizableHelper.fromBinary(binary, f_serializer);

        assertThat(result, is(notNullValue()));
        assertThat(result.getExtractor().apply(1234), is("1234"));
        }

    @Test
    public void shouldSerializeConvertUsingExternalizable() throws Exception
        {
        Binary             binary = ExternalizableHelper.toBinary(Subscriber.Convert.using(Object::toString));
        Subscriber.Convert result = ExternalizableHelper.fromBinary(binary);

        assertThat(result, is(notNullValue()));
        assertThat(result.getExtractor().apply(1234), is("1234"));
        }

    @Test
    public void shouldSerializeFilteredValueUsingPof() throws Exception
        {
        Filter              filter = new EqualsFilter("foo", "bar");
        Binary              binary = ExternalizableHelper.toBinary(Subscriber.Filtered.by(filter), f_serializer);
        Subscriber.Filtered result = ExternalizableHelper.fromBinary(binary, f_serializer);

        assertThat(result, is(notNullValue()));
        assertThat(result.getFilter(), is(filter));
        }

    @Test
    public void shouldSerializeFilteredUsingExternalizable() throws Exception
        {
        Filter              filter = new EqualsFilter("foo", "bar");
        Binary              binary = ExternalizableHelper.toBinary(Subscriber.Filtered.by(filter));
        Subscriber.Filtered result = ExternalizableHelper.fromBinary(binary);

        assertThat(result, is(notNullValue()));
        assertThat(result.getFilter(), is(filter));
        }


    @Test
    public void shouldSerializeNameValueUsingPof() throws Exception
        {
        Binary          binary = ExternalizableHelper.toBinary(Subscriber.Name.of("foo"), f_serializer);
        Subscriber.Name result = ExternalizableHelper.fromBinary(binary, f_serializer);

        assertThat(result, is(notNullValue()));
        assertThat(result.getName(), is("foo"));
        }

    @Test
    public void shouldSerializeNametUsingExternalizable() throws Exception
        {
        Binary          binary = ExternalizableHelper.toBinary(Subscriber.Name.of("foo"));
        Subscriber.Name result = ExternalizableHelper.fromBinary(binary);

        assertThat(result, is(notNullValue()));
        assertThat(result.getName(), is("foo"));
        }


    private static final ConfigurablePofContext f_serializer = new ConfigurablePofContext("coherence-pof-config.xml");
    }
