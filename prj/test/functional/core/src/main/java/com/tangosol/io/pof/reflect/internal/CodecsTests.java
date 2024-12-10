/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

import com.tangosol.io.pof.PofAnnotationSerializer;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;
import com.tangosol.io.pof.reflect.Codec;
import com.tangosol.io.pof.reflect.Codecs;

import com.tangosol.net.CacheFactory;
import com.tangosol.util.LongArray;
import com.tangosol.util.SimpleLongArray;
import org.junit.Test;

import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tangosol.util.ExternalizableHelper.fromBinary;
import static com.tangosol.util.ExternalizableHelper.toBinary;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test {@link Codec} implementations encapsulated in {@link Codecs}  
 *
 * @author hr
 * @since 3.7.1
 */
public class CodecsTests
    {
    /**
     * Test the {@link com.tangosol.io.pof.reflect.Codec} implementations ability to write to
     * {@link PofReader} and {@link PofWriter}
     *
     * @throws IOException
     */
    @Test
    public void testObject() throws IOException
        {
        PofReader in = Mockito.mock(PofReader.class);
        when(in.readObject(0)).thenReturn(true);

        PofWriter out = Mockito.mock(PofWriter.class);

        Codec codec = Codecs.DEFAULT_CODEC;
        assertThat((Boolean)codec.decode(in, 0), is(true));

        codec.encode(out, 0, true);
        verify(out).writeObject(0, true);
        }

    /**
     * Test the {@link com.tangosol.io.pof.reflect.Codecs.CollectionCodec} implementations ability to write to
     * {@link PofReader} and {@link PofWriter}
     *
     * @throws IOException
     */
    @Test
    public void testCollectionCodec() throws IOException
        {
        PofReader in = Mockito.mock(PofReader.class);
        List list = new ArrayList();

        when(in.readCollection(anyInt(), any())).thenReturn(list);

        Codec codec = Codecs.getCodec(list.getClass());
        assertEquals(codec.decode(in, 0), list);
        }

    /**
     * Test the {@link com.tangosol.io.pof.reflect.Codecs.MapCodec} implementations ability to write to
     * {@link PofReader} and {@link PofWriter}
     *
     * @throws IOException
     */
    @Test
    public void testMapCodec() throws Exception
        {
        PofReader in = Mockito.mock(PofReader.class);
        Map map = new HashMap();

        when(in.readMap(anyInt(), any())).thenReturn(map);

        Codec codec = Codecs.getCodec(map.getClass());
        assertEquals(codec.decode(in, 0), map);
        }

    /**
     * Test the {@link com.tangosol.io.pof.reflect.Codecs.LongArrayCodec} implementations ability to write to
     * {@link PofReader} and {@link PofWriter}
     *
     * @throws IOException
     */
    @Test
    public void testLongArrayCodec() throws Exception
        {
        PofReader in = Mockito.mock(PofReader.class);
        LongArray along = new SimpleLongArray();

        when(in.readLongArray(anyInt(), any())).thenReturn(along);

        Codec codec = Codecs.getCodec(along.getClass());
        assertEquals(codec.decode(in, 0), along);
        }

    /**
     * Test the {@link com.tangosol.io.pof.reflect.Codecs.ArrayCodec} implementations ability to write to
     * {@link PofReader} and {@link PofWriter}
     *
     * @throws IOException
     */
    @Test
    public void testArrayCodec() throws Exception
        {
        PofReader in = Mockito.mock(PofReader.class);
        String[] aStr = new String[3];

        when(in.readArray(anyInt(), any())).thenReturn(aStr);

        Codec codec = Codecs.getCodec(aStr.getClass());
        assertEquals(codec.decode(in, 0), aStr);
        }

    /**
     * Test various Wrapper Codec implementations in {@link com.tangosol.io.pof.reflect.Codecs}
     *
     */
    @Test
    public void testClassCodec()
        {
        CacheFactory.ensureCluster();
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(1005, Order.class, new PofAnnotationSerializer<Order>(1005, Order.class, true));

        Order order1 = Order.create();
        Order order2 = (Order) fromBinary(toBinary(order1, ctx), ctx);

        assertThat(order2.m_mapItemId.get("somekey"), is("somevalue"));
        assertThat(order2.m_ao[3]                   , is(4));
        assertThat(order2.m_aItemCodes.getSize()    , is(1));
        assertNotNull(order2.m_lItems);
        }

    // ----- inner class: Order --------------------------------------

    @Portable
    public static class Order
        {
        public static Order create()
            {
            Order order = new Order();
            order.m_mapItemId = new HashMap();
            order.m_mapItemId.put("somekey", "somevalue");
            order.m_lItems = new ArrayList();
            order.m_lItems.add("SomeItem");
            order.m_aItemCodes = new SimpleLongArray();
            order.m_aItemCodes.add("someItemCode");
            order.m_ao= new Object[] {1,2,3,4,5};

            return order;
            }

        @PortableProperty(value=1001, codec=ArrayList.class)
        private List m_lItems;
        @PortableProperty(value=1002, codec=HashMap.class)
        private Map m_mapItemId;
        @PortableProperty(value=1003, codec=Object[].class)
        private Object[] m_ao;
        @PortableProperty(value=1004, codec=SimpleLongArray.class)
        private LongArray m_aItemCodes;
        }
    }
