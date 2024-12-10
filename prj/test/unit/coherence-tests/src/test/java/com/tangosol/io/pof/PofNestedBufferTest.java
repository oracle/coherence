/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.util.Base;
import com.tangosol.util.ImmutableArrayList;


public class PofNestedBufferTest
    {
    private static final String complexValue = "901F000041A40101901F0000901F00004E0B48656C6C6F20576F726C64025844043F8000004000000040533333408CCCCD03901F0000584E03036F6E650374776F05746872656540400155054E04666F75724E04666976654E037369784E05736576656E4E056569676874026B036E04564E0504666F757204666976650373697805736576656E0565696768740A454001C6A7EF9DB22D4002454011C6A7EF9DB22D037840";
    private static final String simpleValue  = "911F00004100016A026B03911F00004100016A026B40046D056E40";

    ConfigurablePofContext cpc = new ConfigurablePofContext(
                                       "com/tangosol/io/pof/include-pof-config.xml");
    @Test
    public void testNestedWriter() throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1000);
        NestedType nt = new NestedType();
        cpc.serialize(buffer.getBufferOutput(), nt);
        System.out.println(Base.toHex(buffer.toByteArray()));
        cpc.deserialize(buffer.getReadBuffer().getBufferInput());
        }

    @Test
    public void testNestedSimple() throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1000);
        SimpleType nt = new SimpleType();
        cpc.serialize(buffer.getBufferOutput(), nt);
        System.out.println(Base.toHex(buffer.toByteArray()));
        cpc.deserialize(buffer.getReadBuffer().getBufferInput());
        }

    public static class NestedType implements PortableObject
        {

        private static final int         INTEGER      = 100;
        private static final String      STRING       = "Hello World";
        private static final String[]    STRING_ARRAY = new String[] { "one",
                                                              "two", "three" };
        private static final float[]     FLOAT_ARRAY  = new float[] { 1.0f,
                                                              2.0f, 3.3f, 4.4f };
        private static final List<String> list;

        static
            {
            list = new ArrayList<String>();
            list.add("four");
            list.add("five");
            list.add("six");
            list.add("seven");
            list.add("eight");
            }

        public void readExternal(PofReader reader) throws IOException
            {
            Assert.assertEquals(INTEGER, reader.readInt(0));
            PofReader nested1 = reader.createNestedPofReader(1);

            PofReader nested2 = nested1.createNestedPofReader(0);
            Assert.assertEquals(STRING, nested2.readString(0));
            float[] floatArray = nested2.readFloatArray(2);
            Assert.assertEquals(Arrays.equals(FLOAT_ARRAY, floatArray), true);

            PofReader nested3 = nested2.createNestedPofReader(3);
            String[] stringArray = (String[]) nested3.readObjectArray(0,
                    new String[0]);
            Assert.assertTrue(Arrays.equals(stringArray, STRING_ARRAY));
            nested3.readRemainder();

            // close nested3 and continue to nested2
            boolean bool = nested2.readBoolean(4);
            Assert.assertEquals(false, bool);

            // nested1
            Collection<String> col = nested1.readCollection(1, (Collection) null);
            for (String string : list)
                {
                Assert.assertTrue(col.contains(string));
                }

            Assert.assertEquals(2.0, nested1.readDouble(2));
            Assert.assertEquals(5, nested1.readInt(3));

            col = nested1.readCollection(4, new ArrayList<String>());
            for (String string : list)
                {
                Assert.assertTrue(col.contains(string));
                }

            Assert.assertEquals(2.222, nested1.readDouble(10));

            nested1.readRemainder();

            Assert.assertEquals(4.444, reader.readDouble(2));
            Assert.assertEquals(15, reader.readInt(3));
            }

        public void writeExternal(PofWriter writer) throws IOException
            {
            writer.writeInt(0, INTEGER);

            PofWriter nested1 = writer.createNestedPofWriter(1);

            PofWriter nested2 = nested1.createNestedPofWriter(0);
            nested2.writeString(0, STRING);
            nested2.writeFloatArray(2, FLOAT_ARRAY);

            PofWriter nested3 = nested2.createNestedPofWriter(3);
            nested3.writeObjectArray(0, STRING_ARRAY, String.class);

            nested2.writeBoolean(4, false);
            nested2.writeRemainder(null);

            nested1.writeCollection(1, list);
            nested1.writeDouble(2, 2.0);
            nested1.writeInt(3, 5);
            nested1.writeCollection(4, list, String.class);
            nested1.writeDouble(10, 2.222);

            writer.writeDouble(2, 4.444);
            writer.writeInt(3, 15);
            }
        }

    public static class SimpleType implements PortableObject
        {
        public void readExternal(PofReader reader) throws IOException
            {
            Assert.assertEquals(0, reader.readInt(0));
            Assert.assertEquals(1, reader.readInt(1));
            Assert.assertEquals(2, reader.readInt(2));
            PofReader reader2 = reader.createNestedPofReader(3);
            Assert.assertEquals(0, reader2.readInt(0));
            Assert.assertEquals(1, reader2.readInt(1));
            Assert.assertEquals(2, reader2.readInt(2));
            Assert.assertEquals(4, reader.readInt(4));
            Assert.assertEquals(5, reader.readInt(5));
            }

        public void writeExternal(PofWriter writer) throws IOException
            {
            writer.writeInt(0, 0);
            writer.writeInt(1, 1);
            writer.writeInt(2, 2);
            PofWriter writer2 = writer.createNestedPofWriter(3);
            writer2.writeInt(0, 0);
            writer2.writeInt(1, 1);
            writer2.writeInt(2, 2);
            writer.writeInt(4, 4);
            writer.writeInt(5, 5);
            }
        }
    }
