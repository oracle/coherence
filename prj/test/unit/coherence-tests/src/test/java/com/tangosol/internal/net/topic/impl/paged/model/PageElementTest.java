/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import org.junit.Test;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PageElementTest
    {
    @Test
    public void shouldConvertFromBinary()
        {
        int        nChannel   = 19;
        long       lPage      = 6676;
        int        nOffset    = 10;
        long       lMillis    = Instant.now().toEpochMilli();
        Serializer serializer = new DefaultSerializer();
        Binary     binValue   = ExternalizableHelper.toBinary("value-one", serializer);

        Binary     binary     = PageElement.toBinary(nChannel, lPage, nOffset, lMillis, binValue);

        PageElement<String> element = PageElement.fromBinary(binary, serializer);
        assertThat(element, is(notNullValue()));
        assertThat(element.getChannel(), is(nChannel));
        assertThat(element.getPosition(), is(new PagedPosition(lPage, nOffset)));
        assertThat(element.getTimestamp(), is(Instant.ofEpochMilli(lMillis)));
        assertThat(element.getBinaryValue(), is(binValue));
        assertThat(element.getValue(), is("value-one"));
        }

    @Test
    public void shouldConvertFromBinaryWithNullValue()
        {
        int        nChannel   = 19;
        long       lPage      = 6676;
        int        nOffset    = 10;
        long       lMillis    = Instant.now().toEpochMilli();
        Serializer serializer = new DefaultSerializer();
        Binary     binValue   = ExternalizableHelper.toBinary(null, serializer);

        Binary     binary     = PageElement.toBinary(nChannel, lPage, nOffset, lMillis, binValue);

        PageElement<String> element = PageElement.fromBinary(binary, serializer);
        assertThat(element, is(notNullValue()));
        assertThat(element.getChannel(), is(nChannel));
        assertThat(element.getPosition(), is(new PagedPosition(lPage, nOffset)));
        assertThat(element.getTimestamp(), is(Instant.ofEpochMilli(lMillis)));
        assertThat(element.getBinaryValue(), is(binValue));
        assertThat(element.getValue(), is(nullValue()));
        }

    }
