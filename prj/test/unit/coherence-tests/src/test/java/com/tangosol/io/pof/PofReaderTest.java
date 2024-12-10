/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.WriteBuffer;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import org.junit.Test;

import java.io.IOException;

import java.sql.Timestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


/**
* Test for PofReader/PofWriter.
*
* @author cp  2006.07.18
* @author jh  2006.07.19
*/
public class PofReaderTest
        extends Base
    {
    public static void main(String[] asArg) throws Exception
        {
        new PofReaderTest().createSimpleData();
        new PofReaderTest().readSimpleData();
        new PofReaderTest().readSimpleDataAsObjects();

        new PofReaderTest().createObjectData();
        new PofReaderTest().createObjectCompare();
        new PofReaderTest().readObjectData();

        new PofReaderTest().createVersionedObjectData();
        new PofReaderTest().readVersionedObjectData();
        new PofReaderTest().writeVersionedObjectData();
        new PofReaderTest().readVersionedObjectData();

        new PofReaderTest().createSimpleObjectData();
        new PofReaderTest().readSimpleObjectData();

        new PofReaderTest().createStatelessObjectData();
        new PofReaderTest().createStatelessObjectCompare();
        new PofReaderTest().readObjectData();

        new PofReaderTest().createExceptionData();
        new PofReaderTest().readExceptionData();

        new PofReaderTest().createMyTimestampData();
        new PofReaderTest().readMyTimestampData();

        getOut().flush();
        }

    @Test
    public void createSimpleData() throws Exception
        {
        WriteBuffer buf = new BinaryWriteBuffer(2000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        SimplePofContext ctx = new SimplePofContext();
        PofWriter writer = new PofBufferWriter(out, ctx);
        writer.writeBoolean(-1, false);
        writer.writeBoolean(-1, true);
        writer.writeChar(-1, (char) 0xFF);
        writer.writeInt(-1, 12345);
        writer.writeFloat(-1, Float.NEGATIVE_INFINITY);
        writer.writeFloat(-1, Float.POSITIVE_INFINITY);
        writer.writeFloat(-1, Float.NaN);
        writer.writeDouble(-1, Double.NEGATIVE_INFINITY);
        writer.writeDouble(-1, Double.POSITIVE_INFINITY);
        writer.writeDouble(-1, Double.NaN);
        writer.writeString(-1, "hello world");
        writer.writeBooleanArray(-1, new boolean[] {true});
        writer.writeByteArray(-1, new byte[] {(byte) 1});
        writer.writeCharArray(-1, new char[] {'a'});
        writer.writeShortArray(-1, new short[] {(short) 1});
        writer.writeIntArray(-1, new int[] {1});
        writer.writeLongArray(-1, new long[] {1L});
        writer.writeFloatArray(-1, new float[]{1.0F});
        writer.writeDoubleArray(-1, new double[]{1.0D});
        writer.writeObjectArray(-1, new Boolean[] {Boolean.FALSE, Boolean.TRUE});

        Date           date = new Date();
        Timestamp      ts   = new Timestamp(date.getTime());
        LocalDate      ld   = LocalDate.now();
        LocalDateTime  ldt  = LocalDateTime.now();
        LocalTime      lt   = LocalTime.now();
        OffsetDateTime odt  = OffsetDateTime.now();
        OffsetTime     ot   = OffsetTime.now();
        ZonedDateTime  zdt  = ZonedDateTime.now();

        writer.writeDate(-1, date);
        writer.writeDate(-1, ld);
        writer.writeDateTime(-1, date);
        writer.writeDateTime(-1, ldt);
        writer.writeDateTime(-1, ts);
        writer.writeDateTimeWithZone(-1, date);
        writer.writeDateTimeWithZone(-1, odt);
        writer.writeDateTimeWithZone(-1, zdt);
        writer.writeDateTimeWithZone(-1, ts);
        writer.writeTime(-1, date);
        writer.writeTime(-1, lt);
        writer.writeTime(-1, ts);
        writer.writeTimeWithZone(-1, date);
        writer.writeTimeWithZone(-1, ot);
        writer.writeTimeWithZone(-1, ts);

        out();
        out("result=");
        out(toHexDump(buf.toByteArray(), 16));

        s_binSimple = buf.toBinary();
        }

    @Test
    public void readSimpleData() throws Exception
        {
        SimplePofContext ctx = new SimplePofContext();

        if (s_binSimple == null)
            {
            createSimpleData();
            }
        PofReader reader = new PofBufferReader(s_binSimple.getBufferInput(), ctx);

        out();
        out("reading as simple types:");
        out("boolean=" + reader.readBoolean(-1));
        out("boolean=" + reader.readBoolean(-1));
        out("char=" + reader.readChar(-1));
        out("int=" + reader.readInt(-1));
        out("float=" + reader.readFloat(-1));
        out("float=" + reader.readFloat(-1));
        out("float=" + reader.readFloat(-1));
        out("double=" + reader.readDouble(-1));
        out("double=" + reader.readDouble(-1));
        out("double=" + reader.readDouble(-1));
        out("String=" + reader.readString(-1));
        out("boolean[]=" + reader.readBooleanArray(-1));
        out("byte[]=" + reader.readByteArray(-1));
        out("char[]=" + reader.readCharArray(-1));
        out("short[]=" + reader.readShortArray(-1));
        out("int[]=" + reader.readIntArray(-1));
        out("long[]=" + reader.readLongArray(-1));
        out("float[]=" + reader.readFloatArray(-1));
        out("double[]=" + reader.readDoubleArray(-1));
        out("Object[]=" + reader.readObjectArray(-1, (Object[]) null));
        out("RawDate=" + reader.readRawDate(-1));
        out("RawDate=" + reader.readRawDate(-1));
        out("RawDateTime=" + reader.readRawDateTime(-1));
        out("RawDateTime=" + reader.readRawDateTime(-1));
        out("RawDateTime=" + reader.readRawDateTime(-1));
        out("RawDateTime=" + reader.readRawDateTime(-1));
        out("RawDateTime=" + reader.readRawDateTime(-1));
        out("RawDateTime=" + reader.readRawDateTime(-1));
        out("RawDateTime=" + reader.readRawDateTime(-1));
        out("RawTime=" + reader.readRawTime(-1));
        out("RawTime=" + reader.readRawTime(-1));
        out("RawTime=" + reader.readRawTime(-1));
        out("RawTime=" + reader.readRawTime(-1));
        out("RawTime=" + reader.readRawTime(-1));
        out("RawTime=" + reader.readRawTime(-1));
        }

    @Test
    public void readSimpleDataAsObjects() throws Exception
        {
        SimplePofContext ctx = new SimplePofContext();
        PofReader reader = new PofBufferReader(s_binSimple.getBufferInput(), ctx);

        out();
        out("reading as Object types:");
        out("boolean=" + reader.readObject(-1));
        out("boolean=" + reader.readObject(-1));
        out("char=" + reader.readObject(-1));
        out("int=" + reader.readObject(-1));
        out("float=" + reader.readObject(-1));
        out("float=" + reader.readObject(-1));
        out("float=" + reader.readObject(-1));
        out("double=" + reader.readObject(-1));
        out("double=" + reader.readObject(-1));
        out("double=" + reader.readObject(-1));
        out("String=" + reader.readObject(-1));
        out("boolean[]=" + reader.readObject(-1));
        out("byte[]=" + reader.readObject(-1));
        out("char[]=" + reader.readObject(-1));
        out("short[]=" + reader.readObject(-1));
        out("int[]=" + reader.readObject(-1));
        out("long[]=" + reader.readObject(-1));
        out("float[]=" + reader.readObject(-1));
        out("double[]=" + reader.readObject(-1));
        out("Object[]=" + reader.readObject(-1));
        out("Date=" + reader.readObject(-1));
        out("Date=" + reader.readObject(-1));
        out("DateTime=" + reader.readObject(-1));
        out("DateTime=" + reader.readObject(-1));
        out("DateTime=" + reader.readObject(-1));
        out("DateTime=" + reader.readObject(-1));
        out("DateTime=" + reader.readObject(-1));
        out("DateTime=" + reader.readObject(-1));
        out("DateTime=" + reader.readObject(-1));
        out("Time=" + reader.readObject(-1));
        out("Time=" + reader.readObject(-1));
        out("Time=" + reader.readObject(-1));
        out("Time=" + reader.readObject(-1));
        out("Time=" + reader.readObject(-1));
        out("Time=" + reader.readObject(-1));
        }

    @Test
    public void createSimpleObjectData() throws Exception
        {
        WriteBuffer buf = new BinaryWriteBuffer(2000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        SimplePofContext ctx = new SimplePofContext();
        PofWriter writer = new PofBufferWriter(out, ctx);
        writer.writeInt(-1, 5);     // type id
        writer.writeInt(-1, 1);     // version id
        writer.writeInt(-1, 0);      // property 0
        writer.writeLong(-1, 23);
        writer.writeInt(-1, 1);     // property 1
        writer.writeInt(-1, 8);
        writer.writeInt(-1, 2);     // property 2
        writer.writeBinary(-1, new Binary());
        writer.writeInt(-1, 3);     // property 3
        writer.writeObject(-1, 1.0D);
        writer.writeInt(-1, -1);    // EOF

        out();
        out("result=");
        out(toHexDump(buf.toByteArray(), 16));

        s_binSimple = buf.toBinary();
        }

    public void readSimpleObjectData() throws Exception
        {
        SimplePofContext ctx = new SimplePofContext();
        PofReader reader = new PofBufferReader(s_binSimple.getBufferInput(), ctx);

        out();
        out("reading as simple types:");
        out("type=" + reader.readInt(-1));
        out("version=" + reader.readInt(-1));
        out("property=(" + reader.readInt(-1) + ", " + reader.readLong(-1) + ")");
        out("property=(" + reader.readInt(-1) + ", " + reader.readInt(-1) + ")");
        out("property=(" + reader.readInt(-1) + ", " + reader.readBinary(-1) + ")");
        out("property=(" + reader.readInt(-1) + ", " + reader.readObject(-1) + ")");
        assertTrue(reader.readInt(-1) == -1);
        }

    @Test
    public void createStatelessObjectData() throws Exception
        {
        WriteBuffer buf = new BinaryWriteBuffer(2000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(0, StatelessObject.class, new PortableObjectSerializer(0));

        StatelessObject so = new StatelessObject();

        out();
        out("object=" + so);

        ctx.serialize(out, so);
        out();
        out("object as POF stream=");
        out(toHexDump(buf.toByteArray(), 16));

        s_ctx = ctx;
        s_binObject = buf.toBinary();
        }

    @Test
    public void createStatelessObjectCompare() throws Exception
        {
        WriteBuffer buf = new BinaryWriteBuffer(2000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        PofHandler pofOut = new DuplexingPofHandler(
                new DuplexingPofHandler(new ValidatingPofHandler(), new LoggingPofHandler()),
                new WritingPofHandler(out));

        out();
        out("tediously writing data ..");
        pofOut.beginUserType(-1, 0, 0);
        pofOut.endComplexValue();

        Binary bin = buf.toBinary();
        out();
        out("result:");
        out(toHexDump(bin.toByteArray(), 16));
        }

    @Test
    public void createObjectData() throws Exception
        {
        WriteBuffer buf = new BinaryWriteBuffer(2000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(0, PofMaster.class, new PortableObjectSerializer(0));
        ctx.registerUserType(1, PofChild.class, new PortableObjectSerializer(1));
        ctx.registerUserType(2, StatelessObject.class, new PortableObjectSerializer(2));

        PofMaster pm     = new PofMaster();
        PofChild  pc1    = new PofChild();
        PofChild  pc2    = new PofChild();
        List      list1  = null;
        List      list2  = new ArrayList();
        List      list3  = new ArrayList();
        Map       map1   = null;
        Map       map2   = new HashMap();
        Map       map3   = new HashMap();

        list3.add(0);
        map3.put("key1", "value1");
        map3.put("key2", "value2");
        pc1.setId("child1");
        pc2.setId("child2");

        pm.setList1(list1);
        pm.setList2(list2);
        pm.setList3(list3);
        pm.setMap1(map1);
        pm.setMap2(map2);
        pm.setMap3(map3);
        pm.setNumber(9999);
        pm.setText("cross fingers");
        pm.setStatelessObject(new StatelessObject());
        pm.setChildren(new PofChild[] {pc1, pc2});

        out();
        out("object=" + pm);

        ctx.serialize(out, pm);
        out();
        out("object as POF stream=");
        out(toHexDump(buf.toByteArray(), 16));

        s_ctx = ctx;
        s_binObject = buf.toBinary();
        }

    @Test
    public void createObjectCompare() throws Exception
        {
        WriteBuffer buf = new BinaryWriteBuffer(2000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        PofHandler pofOut = new DuplexingPofHandler(
                new DuplexingPofHandler(new ValidatingPofHandler(), new LoggingPofHandler()),
                new WritingPofHandler(out));

        out();
        out("tediously writing data ..");
        pofOut.beginUserType(-1, 0, 0);
        pofOut.onNullReference(0);
        pofOut.beginCollection(1, 0);
        pofOut.endComplexValue();
        pofOut.beginCollection(2, 1);
        pofOut.onInt32(0, 0);
        pofOut.endComplexValue();
        pofOut.onNullReference(3);
        pofOut.beginMap(4, 0);
        pofOut.endComplexValue();
        pofOut.beginMap(5, 2);
        pofOut.onCharString(0, "key1");
        pofOut.onCharString(0, "value1");
        pofOut.onCharString(1, "key2");
        pofOut.onCharString(1, "value2");
        pofOut.endComplexValue();
        pofOut.onInt32(6, 9999);
        pofOut.onCharString(7, "cross fingers");
        pofOut.beginUserType(8, 2, 0);
        pofOut.endComplexValue();
        pofOut.beginUniformArray(9, 2, 1);
        pofOut.beginUserType(0, 1, 0);
        pofOut.onCharString(0, "child1");
        pofOut.endComplexValue();
        pofOut.beginUserType(1, 1, 0);
        pofOut.onCharString(0, "child2");
        pofOut.endComplexValue();
        pofOut.endComplexValue();
        pofOut.endComplexValue();

        Binary bin = buf.toBinary();
        out();
        out("result:");
        out(toHexDump(bin.toByteArray(), 16));
        }

    @Test
    public void readObjectData() throws Exception
        {
        out();
        out("reading as Object:");
        out("value=" + s_ctx.deserialize(s_binObject.getBufferInput()));
        }

    @Test
    public void createVersionedObjectData() throws Exception
        {
        WriteBuffer buf1 = new BinaryWriteBuffer(2000);
        WriteBuffer buf2 = new BinaryWriteBuffer(2000);

        WriteBuffer.BufferOutput out1 = buf1.getBufferOutput();
        WriteBuffer.BufferOutput out2 = buf2.getBufferOutput();

        SimplePofContext ctx1 = new SimplePofContext();
        ctx1.registerUserType(0, PofObjectV1.class, new PortableObjectSerializer(0));

        SimplePofContext ctx2 = new SimplePofContext();
        ctx2.registerUserType(0, PofObjectV2.class, new PortableObjectSerializer(0));

        PofObjectV1 po1 = new PofObjectV1();
        PofObjectV2 po2 = new PofObjectV2();

        po1.setText1("text1");
        po2.setText1("text1");
        po2.setText2("text2");

        out();
        out("object version 1=" + po1);

        ctx1.serialize(out1, po1);
        out();
        out("object version 1 as POF stream=");
        out(toHexDump(buf1.toByteArray(), 16));

        out();
        out("object version 2=" + po2);

        ctx2.serialize(out2, po2);
        out();
        out("object version 2 as POF stream=");
        out(toHexDump(buf2.toByteArray(), 16));

        s_binObject  = buf1.toBinary();
        s_binObject2 = buf2.toBinary();
        }

    @Test
    public void writeVersionedObjectData() throws Exception
        {
        WriteBuffer buf1 = new BinaryWriteBuffer(2000);
        WriteBuffer buf2 = new BinaryWriteBuffer(2000);

        WriteBuffer.BufferOutput out1 = buf1.getBufferOutput();
        WriteBuffer.BufferOutput out2 = buf2.getBufferOutput();

        SimplePofContext ctx1 = new SimplePofContext();
        ctx1.registerUserType(0, PofObjectV1.class, new PortableObjectSerializer(0));

        SimplePofContext ctx2 = new SimplePofContext();
        ctx2.registerUserType(0, PofObjectV2.class, new PortableObjectSerializer(0));

        PofObjectV1 po1 = (PofObjectV1) m_o;
        PofObjectV2 po2 = (PofObjectV2) m_o2;

        out();
        out("object version 1=" + po1);

        ctx1.serialize(out1, po1);
        out();
        out("object version 1 as POF stream=");
        out(toHexDump(buf1.toByteArray(), 16));

        out();
        out("object version 2=" + po2);

        ctx2.serialize(out2, po2);
        out();
        out("object version 2 as POF stream=");
        out(toHexDump(buf2.toByteArray(), 16));

        s_binObject  = buf1.toBinary();
        s_binObject2 = buf2.toBinary();
        }

    public void readVersionedObjectData() throws Exception
        {
        SimplePofContext ctx1 = new SimplePofContext();
        ctx1.registerUserType(0, PofObjectV1.class, new PortableObjectSerializer(0));

        SimplePofContext ctx2 = new SimplePofContext();
        ctx2.registerUserType(0, PofObjectV2.class, new PortableObjectSerializer(0));

        out();
        out("reading object as version 1:");
        out("value=" + (m_o = ctx1.deserialize(s_binObject2.getBufferInput())));

        out();
        out("reading object as version 2:");
        out("value=" + (m_o2 = ctx2.deserialize(s_binObject.getBufferInput())));
        }

    @Test
    public void createSimpleExceptionData() throws Exception
        {
        WriteBuffer buf = new BinaryWriteBuffer(2000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(0, Exception.class, new ThrowablePofSerializer());

        Exception e = new Exception();

        out();
        out("exception=" + e);
        out(e);

        ctx.serialize(out, e);
        out();
        out("exception as POF stream=");
        out(toHexDump(buf.toByteArray(), 16));

        s_ctx = ctx;
        s_binObject = buf.toBinary();
        }

    @Test
    public void createSimpleExceptionData2() throws Exception
        {
        WriteBuffer buf = new BinaryWriteBuffer(2000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(0, Exception.class, new ThrowablePofSerializer());

        Exception e = new Exception("Something went terribly wrong");

        out();
        out("exception=" + e);
        out(e);

        ctx.serialize(out, e);
        out();
        out("exception as POF stream=");
        out(toHexDump(buf.toByteArray(), 16));

        s_ctx = ctx;
        s_binObject = buf.toBinary();
        }

    @Test
    public void createSimpleExceptionData3() throws Exception
        {
        WriteBuffer buf = new BinaryWriteBuffer(2000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(0, Exception.class, new ThrowablePofSerializer());
        ctx.registerUserType(8, RuntimeException.class, new ThrowablePofSerializer());

        Exception e = new Exception(
                new RuntimeException("Something went terribly wrong"));

        out();
        out("exception=" + e);
        out(e);

        ctx.serialize(out, e);
        out();
        out("exception as POF stream=");
        out(toHexDump(buf.toByteArray(), 16));

        s_ctx = ctx;
        s_binObject = buf.toBinary();
        }

    @Test
    public void createExceptionData() throws Exception
        {
        WriteBuffer buf = new BinaryWriteBuffer(2000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(0, Throwable.class, new ThrowablePofSerializer());
        ctx.registerUserType(1, Exception.class, new ThrowablePofSerializer());
        ctx.registerUserType(2, RuntimeException.class, new ThrowablePofSerializer());

        Exception e = new Exception(
                "Caught an exception while processing",
                new RuntimeException("Something went terribly wrong"));

        out();
        out("exception=" + e);
        out(e);

        ctx.serialize(out, e);
        out();
        out("exception as POF stream=");
        out(toHexDump(buf.toByteArray(), 16));

        s_ctx = ctx;
        s_binObject = buf.toBinary();
        }

    @Test
    public void readExceptionData() throws Exception
        {
        out();
        out("reading as Exception:");

        Exception e = (Exception) s_ctx.deserialize(s_binObject.getBufferInput());

        out("value=" + e);
        out(e);
        }

    @Test
    public void createMyTimestampData() throws Exception
        {
        WriteBuffer buf = new BinaryWriteBuffer(2000);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(0, MyTimestamp.class, new MyTimestampSerializer());

        MyTimestamp ts = new MyTimestamp(System.currentTimeMillis());

        out();
        out("timestamp=" + ts);
        out(ts);

        ctx.serialize(out, ts);
        out();
        out("timestamp as POF stream=");
        out(toHexDump(buf.toByteArray(), 16));

        s_ctx = ctx;
        s_binObject = buf.toBinary();
        }

    @Test
    public void readMyTimestampData() throws Exception
        {
        out();
        out("reading as Object:");

        Object o = s_ctx.deserialize(s_binObject.getBufferInput());

        out("value=" + o);
        }

    public static class PofMaster
            implements PortableObject
        {
        public PofMaster()
            {
            }

        public List getList1()
            {
            return m_list1;
            }

        public void setList1(List list)
            {
            m_list1 = list;
            }

        public List getList2()
            {
            return m_list2;
            }

        public void setList2(List list)
            {
            m_list2 = list;
            }

        public List getList3()
            {
            return m_list3;
            }

        public void setList3(List list)
            {
            m_list3 = list;
            }

        public Map getMap1()
            {
            return m_map1;
            }

        public void setMap1(Map map)
            {
            m_map1 = map;
            }

        public Map getMap2()
            {
            return m_map2;
            }

        public void setMap2(Map map)
            {
            m_map2 = map;
            }

        public Map getMap3()
            {
            return m_map3;
            }

        public void setMap3(Map map)
            {
            m_map3 = map;
            }

        public int getNumber()
            {
            return m_n;
            }

        public void setNumber(int n)
            {
            m_n = n;
            }

        public String getText()
            {
            return m_s;
            }

        public void setText(String s)
            {
            m_s = s;
            }

        public StatelessObject getStatelessObject()
            {
            return m_so;
            }

        public void setStatelessObject(StatelessObject so)
            {
            m_so = so;
            }

        public PofChild[] getChildren()
            {
            return m_apc;
            }

        public void setChildren(PofChild[] apc)
            {
            m_apc = apc;
            }

        public void readExternal(PofReader in) throws IOException
            {
            setList1((List) in.readCollection(0, (Collection) null));
            setList2((List) in.readCollection(1, new ArrayList()));
            setList3((List) in.readCollection(2, (Collection) null));
            setMap1(in.readMap(3, (Map) null));
            setMap2(in.readMap(4, new HashMap()));
            setMap3(in.readMap(5, (Map) null));
            setNumber(in.readInt(6));
            setText(in.readString(7));
            setStatelessObject((StatelessObject) in.readObject(8));
            setChildren((PofChild[]) in.readObjectArray(9, new PofChild[0]));
            }

        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeCollection(0, getList1());
            out.writeCollection(1, getList2());
            out.writeCollection(2, getList3());
            out.writeMap(3, getMap1());
            out.writeMap(4, getMap2());
            out.writeMap(5, getMap3());
            out.writeInt(6, getNumber());
            out.writeString(7, getText());
            out.writeObject(8, getStatelessObject());
            out.writeObjectArray(9, getChildren(), PofChild.class);
            }

        public String toString()
            {
            PofChild[]   apc = getChildren();
            StringBuffer sb  = new StringBuffer();
            if (apc == null)
                {
                sb.append("null");
                }
            else
                {
                sb.append('[');
                for (int i = 0, c = apc.length; i < c; ++i)
                    {
                    sb.append(apc[i]);
                    if (i + 1 < c)
                        {
                        sb.append(", ");
                        }
                    }
                sb.append(']');
                }

            return "PofMaster\n    ("
                    + "\n    List1="           + getList1()
                    + "\n    List2="           + getList2()
                    + "\n    List3="           + getList3()
                    + "\n    Map1="            + getMap1()
                    + "\n    Map2="            + getMap2()
                    + "\n    Map3="            + getMap3()
                    + "\n    Number="          + getNumber()
                    + "\n    Text="            + getText()
                    + "\n    StatelessObject=" + getStatelessObject()
                    + "\n    Children="        + sb.toString()
                    + "\n    )";
            }

        private List            m_list1;
        private List            m_list2;
        private List            m_list3;
        private Map             m_map1;
        private Map             m_map2;
        private Map             m_map3;
        private int             m_n;
        private String          m_s;
        private StatelessObject m_so;
        private PofChild[]      m_apc;
        }

    public static class PofChild
            implements PortableObject
        {
        public PofChild()
            {
            }

        public String getId()
            {
            return m_sId;
            }

        public void setId(String sId)
            {
            m_sId = sId;
            }

        public void readExternal(PofReader in)
                throws IOException
            {
            setId(in.readString(0));
            }

        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, getId());
            }

        public String toString()
            {
            return "PofChild(Id=" + getId() + ')';
            }

        private String m_sId;
        }

    public static class PofObjectV1
            implements EvolvablePortableObject
        {
        public String getText1()
            {
            return m_s1;
            }

        public void setText1(String s)
            {
            m_s1 = s;
            }

        public int getImplVersion()
            {
            return 1;
            }

        public int getDataVersion()
            {
            return m_nVersionId;
            }

        public void setDataVersion(int nVersion)
            {
            m_nVersionId = nVersion;
            }

        public Binary getFutureData()
            {
            return m_binFuture;
            }

        public void setFutureData(Binary buf)
            {
            m_binFuture = buf;
            }

        public void readExternal(PofReader in)
                throws IOException
            {
            setText1(in.readString(0));
            }

        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, getText1());
            }

        public String toString()
            {
            return "PofObjectV1(Text1=" + getText1() + ')';
            }

        private String m_s1;
        private int    m_nVersionId;
        private Binary m_binFuture;
        }

    public static class PofObjectV2
            extends PofObjectV1
        {
        public String getText2()
            {
            return m_s2;
            }

        public void setText2(String s)
            {
            m_s2 = s;
            }

        public int getImplVersion()
            {
            return 2;
            }

        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            setText2(in.readString(1));
            }

        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeString(1, getText2());
            }

        public String toString()
            {
            return "PofObjectV2(Text1=" + getText1()
                    + ", Text2=" + getText2() + ')';
            }

        private String m_s2;
        }

    public static class StatelessObject
            implements PortableObject
        {
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        public void writeExternal(PofWriter out)
                throws IOException
            {
            }
        }

    public static class MyTimestamp
            extends Timestamp
        {
        public MyTimestamp(long ldt)
            {
            super(ldt);
            }

        public String toString()
            {
            return "MyTimestamp: " + super.toString();
            }
        }

    public static class MyTimestampSerializer
            implements PofSerializer
        {
        public MyTimestampSerializer()
            {
            super();
            }

        public void serialize(PofWriter out, Object o)
                throws IOException
            {
            MyTimestamp ts = (MyTimestamp) o;
            out.writeLong(0, ts.getTime());
            out.writeRemainder(null);
            }

        public Object deserialize(PofReader in)
                throws IOException
            {
            Object o = new MyTimestamp(in.readLong(0));
            in.registerIdentity(o);
            in.readRemainder();
            return o;
            }
        }

    static Object m_o;
    static Object m_o2;
    static Binary s_binSimple;
    static Binary s_binObject;
    static Binary s_binObject2;
    static PofContext s_ctx;
    }
