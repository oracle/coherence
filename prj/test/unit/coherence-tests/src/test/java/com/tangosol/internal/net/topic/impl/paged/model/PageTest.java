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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.05.19
 */
public class PageTest
    {
    @Test
    public void shouldSerializeUsingPof()
        throws Exception
        {
        ConfigurablePofContext serializer = new ConfigurablePofContext("coherence-pof-config.xml");
        Page                   page       = new Page();

        page.setTail(20);
        page.setByteSize(1000);

        Binary binary = ExternalizableHelper.toBinary(page, serializer);
        Page   result = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.isEmpty(), is(page.isEmpty()));
        assertThat(result.getByteSize(), is(page.getByteSize()));
        assertThat(result.getDataVersion(), is(page.getImplVersion()));
        assertNotNull(page.toString());
        }

    @Test
    public void shouldBeEmptyOnCreation()
            throws Exception
        {
        Page page = new Page();

        assertThat(page.isEmpty(), is(true));
        assertThat(page.getByteSize(), is(0));
        }

    @Test
    public void shouldMarkAsEmpty()
            throws Exception
        {
        Page page = new Page();

        page.setTail(20);
        page.setByteSize(1000);

        assertThat(page.isEmpty(), is(false));
        assertThat(page.getByteSize(), is(1000));

        page.markEmpty();

        assertThat(page.isEmpty(), is(true));
        assertThat(page.getByteSize(), is(0));
        }

    @Test
    public void shouldSerializePageKeyWithPof()
        {
        ConfigurablePofContext serializer = new ConfigurablePofContext("coherence-pof-config.xml");

        Page.Key key = new Page.Key(1, 10L);

        Binary    binary = ExternalizableHelper.toBinary(key, serializer);
        Page.Key  result = ExternalizableHelper.fromBinary(binary, serializer);
        assertEquals(key, result);
        }

    @Test
    public void testPageKeyEquality()
        {
        Page.Key key1 = new Page.Key(1, 10L);
        Page.Key key2 = new Page.Key(2, 12L);
        Page.Key key3 = new Page.Key(1, 10L);

        assertNotEquals(key1, key2);
        assertEquals(key1, key3);
        assertNotEquals(key1, null);
        assertEquals(key1, key1);
        assertNotNull(key1.toString());
        assertEquals(key1.hashCode(), key3.hashCode());
        }
    }
