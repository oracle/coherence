/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;

import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.annotation.Portable;

import com.tangosol.io.pof.generator.PortableTypeGenerator;

import com.tangosol.io.pof.schema.annotation.PortableType;

import com.tangosol.util.Base;

import com.tangosol.util.extractor.ChainedExtractor;

import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.LikeFilter;

import java.io.File;
import java.nio.file.Files;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import java.util.function.Function;

import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;

import java.math.BigInteger;

import static org.hamcrest.CoreMatchers.instanceOf;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
* Test for the ConfigurablePofHandler class.
* <p/>
* @author cp 2006.07.26
* @author Gunnar Hillert 2024.04.15
*/
public class ConfigurablePofContextTest
        extends Base
    {
    @Test
    public void testSimple() throws Exception
        {
        String sPath = "com/tangosol/io/pof/tangosol-pof-config.xml";
        ConfigurablePofContext ctx = new ConfigurablePofContext(sPath);
        ctx.setContextClassLoader(PofMaster.class.getClassLoader());
        assertFalse(ctx.isPreferJavaTime());

        PofMaster pm     = new PofMaster();
        PofChild  pc1    = new PofChild();
        PofChild  pc2    = new PofChild();
        List      list1  = null;
        List      list2  = new ArrayList();
        List      list3  = new ArrayList();
        Map       map1   = null;
        Map       map2   = new HashMap();
        Map       map3   = new HashMap();

        list3.add(Integer.valueOf(0));
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
        pm.setChildren(new PofChild[] {pc1, pc2});

        WriteBuffer buf = new ByteArrayWriteBuffer(20);
        ctx.serialize(buf.getBufferOutput(), pm);

        ConfigurablePofContext ctx2 = new ConfigurablePofContext(sPath);
        ctx2.setContextClassLoader(PofMaster.class.getClassLoader());
        PofMaster pm2 = (PofMaster) ctx2.deserialize(buf.toBinary().getBufferInput());

        assertEquals(pm, pm2);
        }

    @Test
    public void testBigIntegerSerializer() throws Exception
        {
        String                 sPath = "com/tangosol/io/pof/tangosol-pof-config.xml";
        ConfigurablePofContext ctx   = new ConfigurablePofContext(sPath);

        byte[] byteArray = new byte[300];
        Random random    = new Random();
        random.nextBytes(byteArray);

        BigInteger  bigInt = new BigInteger(byteArray);
        WriteBuffer buf    = new ByteArrayWriteBuffer(400);
        ctx.serialize(buf.getBufferOutput(), bigInt);

        BigInteger bigInt2 = (BigInteger) ctx.deserialize(buf.toBinary().getBufferInput());
        assertEquals(bigInt, bigInt2);
        assertTrue(ctx.isUserType(bigInt));
        assertTrue(ctx.isUserType(BigInteger.class));
        assertTrue(ctx.isUserType(BigInteger.class.getName()));
        assertEquals(3003, ctx.getUserTypeIdentifier(bigInt));
        assertEquals(3003, ctx.getUserTypeIdentifier(BigInteger.class));
        assertEquals(3003, ctx.getUserTypeIdentifier(BigInteger.class.getName()));
        assertEquals(PofConstants.J_USER_TYPE, PofHelper.getJavaTypeId(bigInt, ctx));
        assertEquals(3003, PofHelper.getPofTypeId(BigInteger.class, ctx));
        }

    @Test
    public void testFilter() throws Exception
        {
        ConfigurablePofContext ctx = new ConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        ctx.setContextClassLoader(PofMaster.class.getClassLoader());

        Object o = new AndFilter(
                new EqualsFilter("toString", "hello world"),
                new LikeFilter(new ChainedExtractor("getClass.getName"), "com.tangosol.%", '\\', false));

        WriteBuffer buf = new ByteArrayWriteBuffer(20);
        ctx.serialize(buf.getBufferOutput(), o);

        ConfigurablePofContext ctx2 = new ConfigurablePofContext();
        ctx2.setEnableAutoTypeDiscovery(false);
        ctx2.setContextClassLoader(PofMaster.class.getClassLoader());
        Object o2 = ctx2.deserialize(buf.toBinary().getBufferInput());

        if (o2 == null)
            {
            assertNull("value is null!", o2);
            }
        else
            {
            assertEquals(o.getClass(), o2.getClass());
            }
        }

    @Test
    public void testInclude() throws Exception
        {
        ConfigurablePofContext ctx = new ConfigurablePofContext(
                "com/tangosol/io/pof/include-pof-config.xml");
        assertTrue(ctx.getPofSerializer(0) instanceof com.tangosol.io.pof.ThrowablePofSerializer);
        }

    @Test
    public void testMultipleIncludes() throws Exception
        {
        ConfigurablePofContext ctx = new ConfigurablePofContext(
                "com/tangosol/io/pof/multiple-include-pof-config.xml");
        assertTrue(ctx.getPofSerializer(0) instanceof com.tangosol.io.pof.ThrowablePofSerializer);
        }

    @Test
    public void testDefaultSerializer() throws Exception
        {
        ConfigurablePofContext ctx = new ConfigurablePofContext(
                "com/tangosol/io/pof/default-serializer-pof-config.xml");
        assertTrue(ctx.getPofSerializer(1001) instanceof com.tangosol.io.pof.ThrowablePofSerializer);
        }

    @Test
    public void testDefaultLambda() throws Exception
        {
        ConfigurablePofContext ctx = new ConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        ctx.setContextClassLoader(PofMaster.class.getClassLoader());


        Function<Object, String> func = (Function<Object, String> & Serializable) (n) -> n.toString();

        WriteBuffer buf = new ByteArrayWriteBuffer(20);
        ctx.serialize(buf.getBufferOutput(), func);

        Function<Object, String> func2 = (Function<Object, String> & Serializable) ctx.deserialize(buf.toBinary().getBufferInput());

        if (func2 == null)
            {
            assertNull("value is null!", func2);
            }
        else
            {
            assertEquals(func2.apply(123), "123");
            }
        }

    @Test
    public void testPortableTypeWithIds()
            throws IOException
        {
        File fileIndex = setupIndex(PortableTypeTest1.class);
        try
            {
            ConfigurablePofContext ctx = new ConfigurablePofContext("com/tangosol/io/pof/portable-type-pof-config1.xml");
            ctx.setIndexFileName(fileIndex.getAbsolutePath());
            ctx.setContextClassLoader(PortableTypeTest1.class.getClassLoader());
            ctx.ensureInitialized();

            PofSerializer pofSerializer = ctx.getPofSerializer(1000);
            assertThat(pofSerializer, is(instanceOf(PortableTypeSerializer.class)));
            assertThat(ctx.getUserTypeIdentifier(PortableTypeTest1.class), is(1000));
            assertThat(ctx.getUserTypeIdentifier(PortableTypeTest1.class.getName()), is(1000));
            Class<?> clazz = ctx.getClass(1000);
            assertThat(clazz.getName(), is(PortableTypeTest1.class.getName()));
            }
        finally
            {
            fileIndex.delete();
            }
        }

    @Test(expected = IllegalArgumentException.class)
    public void testPortableTypeWithAllowDiscoveryFalse()
            throws IOException
        {
        File fileIndex = setupIndex(PortableTypeTest1.class);

        try
            {
            ConfigurablePofContext ctx = new ConfigurablePofContext("com/tangosol/io/pof/portable-type-pof-config5.xml");
            ctx.setIndexFileName(fileIndex.getAbsolutePath());
            ctx.setContextClassLoader(PortableTypeTest1.class.getClassLoader());
            ctx.ensureInitialized();

            PofSerializer pofSerializer = ctx.getPofSerializer(1000);
            }
        finally
            {
            fileIndex.delete();
            }
        }

    @Test
    public void testPortableTypeEnumWithId()
            throws IOException
        {
        File fileIndex = setupIndex(TestEnum.class);

        try
            {
            ConfigurablePofContext ctx = new ConfigurablePofContext("com/tangosol/io/pof/portable-type-pof-config7.xml");
            ctx.setIndexFileName(fileIndex.getAbsolutePath());
            ctx.setContextClassLoader(TestEnum.class.getClassLoader());
            ctx.ensureInitialized();

            int nTypeId = ctx.getUserTypeIdentifier(TestEnum.class);
            PofSerializer pofSerializer = ctx.getPofSerializer(nTypeId);
            assertThat(pofSerializer, is(instanceOf(EnumPofSerializer.class)));
            assertThat(ctx.getUserTypeIdentifier(TestEnum.class.getName()), is(nTypeId));
            Class<?> clazz = ctx.getClass(nTypeId);
            assertThat(clazz.getName(), is(TestEnum.class.getName()));
            }
        finally
            {
            fileIndex.delete();
            }
        }

    @Test(expected = IllegalStateException.class)
    public void testPortableTypeWithConflictingId()
            throws IOException
        {
        File fileIndex = setupIndex(PortableTypeTestConflicting.class);

        try
            {
            ConfigurablePofContext ctx = new ConfigurablePofContext("com/tangosol/io/pof/portable-type-pof-config3.xml");
            ctx.setIndexFileName(fileIndex.getAbsolutePath());
            ctx.setContextClassLoader(PortableTypeTestConflicting.class.getClassLoader());
            ctx.ensureInitialized();
            }
        finally
            {
            fileIndex.delete();
            }
        }

    @Test(expected = IllegalStateException.class)
    public void testPortableTypeWithNoAllowInterfaces()
            throws IOException
        {
        File fileIndex = setupIndex(PortableTypeTestInterface.class);

        try
            {
            ConfigurablePofContext ctx = new ConfigurablePofContext("com/tangosol/io/pof/portable-type-pof-config.xml");
            ctx.setIndexFileName(fileIndex.getAbsolutePath());
            ctx.setContextClassLoader(PortableTypeTestInterface.class.getClassLoader());
            ctx.ensureInitialized();
            }
        finally
            {
            fileIndex.delete();
            }
        }

    /**
     * Test classes annotated with {@link com.tangosol.io.pof.annotation.Portable}
     * result in the return of a PofAnnotationSerializer
     */
    @Test
    public void testPofAnnotationSerializer()
        {
        ConfigurablePofContext ctx = new ConfigurablePofContext(
                "com/tangosol/io/pof/multiple-include-pof-config.xml");

        assertThat(ctx.getPofSerializer(1001), instanceOf(PortableObjectSerializer.class));
        assertThat(ctx.getPofSerializer(2002), instanceOf(PofAnnotationSerializer.class));
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
            setChildren((PofChild[]) in.readObjectArray(8, new PofChild[0]));
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
            out.writeObjectArray(8, getChildren(), PofChild.class);
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
                   + "\n    List1="    + getList1()
                   + "\n    List2="    + getList2()
                   + "\n    List3="    + getList3()
                   + "\n    Map1="     + getMap1()
                   + "\n    Map2="     + getMap2()
                   + "\n    Map3="     + getMap3()
                   + "\n    Number="   + getNumber()
                   + "\n    Text="     + getText()
                   + "\n    Children=" + sb.toString()
                   + "\n    )";
            }

        public boolean equals(Object obj)
            {
            if (this == obj)
                {
                return true;
                }
            if (obj == null)
                    {
                    return false;
                }
            if (!(obj instanceof PofMaster))
                {
                return false;
                }

            PofMaster other = (PofMaster )obj;
            return Base.equalsDeep(this.getList1(), other.getList1()) &&
                   Base.equalsDeep(this.getList2(), other.getList2()) &&
                   Base.equalsDeep(this.getList3(), other.getList3()) &&
                   Base.equalsDeep(this.getMap1(), other.getMap1()) &&
                   Base.equalsDeep(this.getMap2(), other.getMap2()) &&
                   Base.equalsDeep(this.getMap3(), other.getMap3()) &&
                   this.getNumber() == other.getNumber() &&
                   this.getText().equals(other.getText()) &&
                   Base.equalsDeep(this.getChildren(), other.getChildren());
            }

        private List       m_list1;
        private List       m_list2;
        private List       m_list3;
        private Map        m_map1;
        private Map        m_map2;
        private Map        m_map3;
        private int        m_n;
        private String     m_s;
        private PofChild[] m_apc;
        }

    @Portable
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

	    public boolean equals(Object obj)
            {
            if (this == obj)
                {
                return true;
                }
            if (obj == null)
                {
            	return false;
            	}

            if(!(obj instanceof PofChild))
                {
                return false;
                }

            PofChild other = (PofChild)obj;
            return this.m_sId.equals(other.m_sId);
            }

        private String m_sId;
        }

    public static abstract class PortableTypeTestBase
        {
        private int m_nId;
        private String m_sString;

        public PortableTypeTestBase()
            {
            }

        public PortableTypeTestBase(int nId, String sString)
            {
            m_nId     = nId;
            m_sString = sString;
            }

        public int getId()
            {
            return m_nId;
            }

        public void setId(int nId)
            {
            this.m_nId = nId;
            }

        public String getString()
            {
            return m_sString;
            }

        public void setString(String sString)
            {
            this.m_sString = sString;
            }
        }

    @PortableType(id = 1000)
    public static class PortableTypeTest1
        extends PortableTypeTestBase
        {

        public PortableTypeTest1()
            {
            super();
            }

        public PortableTypeTest1(int nId, String sString)
             {
             super(nId, sString);
             }
        }

    @PortableType(id = 1)
    public static class PortableTypeTestConflicting
        extends PortableTypeTestBase
        {
        public PortableTypeTestConflicting()
            {
            super();
            }

        public PortableTypeTestConflicting(int nId, String sString)
             {
             super(nId, sString);
             }
        }

    @PortableType(id = 2000)
    public interface PortableTypeTestInterface
        {
        }

    @PortableType(id = 1234)
    public enum TestEnum
        {
        BRONZE, SILVER, GOLD
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Create a POF index using the given file name and classes.
     *
     * @param pofIndexFile the file to write the index to. The classes should be in the format of "java/lang/Thread.class"
     * @param clazzes  classes to index
     */
    public void createManualIndex(File pofIndexFile, Class... clazzes) throws IOException
        {
        final PofIndexer pofIndexer = new PofIndexer(new PortableTypeGenerator.ConsoleLogger())
                .withIndexFileName(pofIndexFile.getName())
                .withClasses(Arrays.stream(clazzes).toList());
        pofIndexer.createIndex(pofIndexFile);
        }

    /**
     * Setup an index file for the given {@link Class}es.
     *
     * @param clazzes classes to setup index for
     * @throws IOException
     */
    protected File setupIndex(Class<?>... clazzes) throws IOException
        {
        File fileIndex = Files.createTempFile("index" + System.currentTimeMillis(), ".idx").toFile();
        createManualIndex(fileIndex, clazzes);
        assertThat(fileIndex.exists(), is(true));
        return fileIndex;
        }

    }
