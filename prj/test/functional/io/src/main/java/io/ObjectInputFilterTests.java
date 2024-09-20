/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package io;


import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.util.invoke.RemoteConstructor;
import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.MultiBufferReadBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.ReadBuffer.BufferInput;
import com.tangosol.io.nio.ByteBufferReadBuffer;
import com.tangosol.util.AssertionException;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.function.Remote;
import com.oracle.coherence.testing.AbstractTestInfrastructure;
import data.Address;
import data.AddressExternalizableLite;
import data.Customer;
import data.CustomerExternalizableLite;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Properties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.tangosol.util.ExternalizableHelper.ensureSerializer;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


public class ObjectInputFilterTests
        extends AbstractTestInfrastructure
    {
    @BeforeClass
    public static void _startup()
        {
        String sFile = Thread.currentThread().getContextClassLoader().getResource("enableserialfilter-logging.properties").getFile();

        System.setProperty("java.util.logging.config.file", sFile);
        System.setProperty("coherence.serialfilter.logging", "true");

        try
            {
            ObjectInputFilterHelper.setConfigObjectInputStreamFilter("data.*;!com.tangosol.internal.util.invoke.RemoteConstructor;maxarray=30000");
            }
        catch (Throwable t) {}

        // don't start cluster
        setupProps();
        propsCommon = new Properties();
        propsCommon.put("test.log.level", "6");
        // configure JDK java.io.serialization filter logging for the Java Serialization tests
        propsCommon.put("java.util.logging.config.file", sFile);
        //propsCommon.put("jdk.serialFilterFactory", "io.SerialFilterFactoryTests$FilterInThread");
        }

    @AfterClass
    public static void _shutdown()
        {
        stopAllApplications();
        }

    @Test
    public void testDirectExternalizableLiteWithObjectInputFilter()
        {
        System.setProperty("test.fExternalizableLite", "true");
        String[] args = new String[1];
        TestBufferInput.main(args);
        }

    @Test
    public void testMultiBufferReadBufferForGlobalObjectInputFilter()
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        MultiBufferReadBuffer buf      = new MultiBufferReadBuffer(
                createReadBuffers(cBuffers, cSize));

        assertTrue("Ensure defaulted to JVM-Wide ObjectInputFilter set in _startup",
                   buf.getDestructiveBufferInput().getObjectInputFilter().toString().contains("data.*"));
        }

    @Test
    public void testMultiBufferReadBufferSetObjectInputFilter()
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        MultiBufferReadBuffer buf      = new MultiBufferReadBuffer(
                createReadBuffers(cBuffers, cSize));

        BufferInput bIn = buf.getDestructiveBufferInput();

        assertTrue("Ensure defaulted to JVM-Wide ObjectInputFilter",
                   bIn.getObjectInputFilter().toString().contains("data.*"));

        Object      oInputFilter = ObjectInputFilterHelper.createObjectInputFilter("data.*");

        bIn.setObjectInputFilter(oInputFilter);
        assertEquals(oInputFilter, bIn.getObjectInputFilter());
        }

    @Test
    public void testDirectExternalizableLiteWithBlockingObjectInputFilterUsinggBinary()
        {
        m_filterProcessWide   = ObjectInputFilterHelper.createObjectInputFilter("!data.Address");
        f_factorySerialFilter = null;
        s_fFilterEnabled      = m_filterProcessWide != null;

        testExternalizableLiteObjectInputFilterUsingBinary(true, "!data.Address*");
        }

    @Test
    public void testDirectExternalizableLiteWithbjectInputFilterUsinggBinary()
        {
        m_filterProcessWide   = ObjectInputFilterHelper.createObjectInputFilter("data.Customer*");
        f_factorySerialFilter = null;
        s_fFilterEnabled      = m_filterProcessWide != null;

        testExternalizableLiteObjectInputFilterUsingBinary(true, "data.Customer*");
        }

    @Test
    public void testDirectJavaWithbjectInputFilterUsinggBinary()
        {
        m_filterProcessWide   = ObjectInputFilterHelper.createObjectInputFilter("data.Customer*");
        f_factorySerialFilter = null;
        s_fFilterEnabled      = m_filterProcessWide != null;

        testExternalizableLiteObjectInputFilterUsingBinary(true, "data.Customer*");
        }

    @Test
    public void testExternalizableLiteWithObjectInputFilter()
        {
        Properties props = new Properties();

        props.put("jdk.serialFilter", "data.Address*");
        props.put("test.fExternalizableLite", "true");

        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("OIFtestExternalizableLiteWithObjectInputFilter",
                                                              "io.ObjectInputFilterTests$TestBufferInput",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    @Test
    public void testExternalizableLiteWithBlockingObjectInputFilter()
        {
        Properties props = new Properties();

        props.put("jdk.serialFilter", "!data.Address*");
        props.put("test.fExternalizableLite", "true");
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("OIFtestExternalizableLiteWithBlockingObjectInputFilter",
                                                              "io.ObjectInputFilterTests$TestBufferInput",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    @Test
    public void testExternalizableLiteWithoutObjectInputFilter()
        {
        Properties props = new Properties();
        props.put("test.fExternalizableLite", "true");
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("OIFtestExternalizableLiteWithoutObjectInputFilter",
                                                              "io.ObjectInputFilterTests$TestBufferInput",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    @Test
    public void testDirectJavaSerializableWithObjectInputFilter()
        {
        System.setProperty("test.fExternalizableLite", "false");
        String[] args = new String[1];
        TestBufferInput.main(args);
        }

    @Test
    public void testJavaSerializableWithObjectInputFilter()
        {
        Properties props = new Properties();

        props.put("jdk.serialFilter", "data.Address*");
        props.put("test.fExternalizableLite", "false");

        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("OIFtestJavaSerializableWithObjectInputFilter",
                                                              "io.ObjectInputFilterTests$TestBufferInput",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    @Test
    public void testJavaSerializableWithBlockingObjectInputFilter()
        {
        Properties props = new Properties();

        props.put("jdk.serialFilter", "!data.Address*");
        props.put("test.fExternalizableLite", "false");
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("OIFtestJavaSerializableWithBlockingObjectInputFilter",
                                                              "io.ObjectInputFilterTests$TestBufferInput",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    @Test
    public void testJavaSerializableithoutObjectInputFilter()
        {
        Properties props = new Properties();
        props.put("test.fExternalizableLite", "false");
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("OIFtestJavaSerializableithoutObjectInputFilter",
                                                              "io.ObjectInputFilterTests$TestBufferInput",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    @Test
    public void testWithObjectInputFilter()
        {
        Properties props = new Properties();
        props.put("jdk.serialFilter", "data.*");
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("OIFtestWithObjectInputFilter",
                                                              "io.ObjectInputFilterTests$TestObjectInputStream",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    @Test
    public void testWithBlockingObjectInputFilter()
        {
        Properties props = new Properties();
        props.put("jdk.serialFilter", "!data.Address*");
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("OIFtestWithBlockingObjectInputFilter",
                                                              "io.ObjectInputFilterTests$TestObjectInputStream",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }


    @Test
    public void testWithoutObjectInputFilter()
        {
        Properties props = new Properties();
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("OIFtestWithoutObjectInputFilter",
                                                              "io.ObjectInputFilterTests$TestObjectInputStream",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    /**
     * Test ObjectInputStream deserialization with ObjectInputFilter.
     *
     * @param fExternalizableLite  serialize class implementing ExternalizableLite rather than Java Serializable
     */
    public static void testObjectInputFilter(boolean fExternalizableLite)
        {
        boolean  fFilter  = s_fFilterEnabled;
        boolean  fFail    = s_fFilterEnabled && m_filterProcessWide.toString().contains("!data.");
        Customer customer = generateCustomer(fExternalizableLite);

        Exception   exception  = null;
        String      sFilter    = m_filterProcessWide == null ? null : m_filterProcessWide.toString();
        Customer    deserialized = null;

        try
            {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream    oos  = new ObjectOutputStream(baos);

            ExternalizableHelper.writeObject(oos, customer);
            oos.flush();

            byte[] ab = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(ab);
            ObjectInputStream    ios  = new ObjectInputStream(bais);

            // Provide non-null classloader to test Wrappers used for containers
            deserialized = ExternalizableHelper.readObject(ios, Thread.currentThread().getContextClassLoader());
            ios.close();
            }
        catch (Exception e)
            {
            exception = e;
            }

        if (!fFilter || !fFail)
            {
            assertEquals("compare serialized and deserialized value, should be equal", customer, deserialized);
            }
        else
            {
            assertTrue("expected an InvalidClassException for ObjectInputFilter " + sFilter,
                       exception != null && (exception.getCause() instanceof InvalidClassException  || exception instanceof InvalidClassException));
            }
        }

    /**
     * Test ExternalizableLite ReadBuffer deserialization with ObjectInputFilter.
     *
     * @param fExternalizableLite  serialize class implementing ExternalizableLite rather than Java Serializable
     */
    public static void testExternalizableLiteObjectInputFilter(boolean fExternalizableLite)
        {
        boolean  fFilter = s_fFilterEnabled;
        boolean  fFail    = s_fFilterEnabled && m_filterProcessWide.toString().contains("!data.");
        Customer customer = generateCustomer(fExternalizableLite);

        Exception exception    = null;
        String    sFilter      = m_filterProcessWide == null ? null : m_filterProcessWide.toString();
        Customer  deserialized = null;

        try
            {
            ByteArrayWriteBuffer baos = new ByteArrayWriteBuffer(0);

            ExternalizableHelper.writeObject(baos.getBufferOutput(), customer);

            byte[] ab = baos.toByteArray();

            ByteArrayReadBuffer bais = new ByteArrayReadBuffer(ab);
            BufferInput         in   = bais.getBufferInput();

            if (s_fFilterEnabled)
                {
                assertEquals(ExternalizableHelper.getConfigSerialFilter(), in.getObjectInputFilter());
                }

            // Provide non-null classloader to test Wrappers used for containers
            deserialized = ExternalizableHelper.readObject(in, Thread.currentThread().getContextClassLoader());
            }
        catch (Exception e)
            {
            exception = e;
            }

        if (!fFilter || !fFail)
            {
            assertEquals("compare serialized and deserialized value, should be equal", customer, deserialized);
            }
        else
            {
            assertTrue("expected an InvalidClassException for ObjectInputFilter " + sFilter,
                       exception != null && (exception.getCause() instanceof InvalidClassException  || exception instanceof InvalidClassException));
            }
        }

    /**
     * Test ExternalizableLite ReadBuffer deserialization with ObjectInputFilter.
     *
     * @param fExternalizableLite  serialize class implementing ExternalizableLite rather than Java Serializable
     */
    public static void testExternalizableLiteObjectInputFilterUsingBinary(boolean fExternalizableLite, String sFilter)
        {
        boolean  fFilter = s_fFilterEnabled;
        boolean  fFail    = s_fFilterEnabled && m_filterProcessWide.toString().contains("!data");
        Customer customer = generateCustomer(fExternalizableLite);

        Exception exception    = null;
        Customer  deserialized = null;

        try
            {
            ByteArrayWriteBuffer baos = new ByteArrayWriteBuffer(0);

            ExternalizableHelper.writeObject(baos.getBufferOutput(), customer);

            byte[] ab = baos.toByteArray();

            Binary bin = new Binary(ab);

            Remote.Function<BufferInput, BufferInput> supplierAddSerialFilter =
                (in) ->
                    {
                    in.setObjectInputFilter(ObjectInputFilterHelper.createObjectInputFilter(sFilter));
                    return in;
                    };

            deserialized = ExternalizableHelper.fromBinary(bin, ensureSerializer(Thread.currentThread().getContextClassLoader()), supplierAddSerialFilter);
            }
        catch (Exception e)
            {
            exception = e;
            }

        if (!fFilter || !fFail)
            {
            assertEquals("compare serialized and deserialized value, should be equal", customer, deserialized);
            }
        else
            {
            assertTrue("expected an InvalidClassException for ObjectInputFilter " + sFilter, containsInvalidClassExceptionCause(exception));
            }
        }

    @Test
    public void testExternalizableLiteArrayBlockingObjectInputFilter()
        {
        testExternalizableLiteArrayObjectInputFilter(/*sFilter*/"data.*;maxarray=9999", /*fFail*/ true, /*cLength*/10000);
        }

    @Test
    public void testExternalizableLiteArrayWithObjectInputFilter()
        {
        testExternalizableLiteArrayObjectInputFilter(/*sFilter*/"data.*;maxarray=9999", /*fFail*/ false, /*cLength*/9999);
        }

    @Test
    public void testExternalizableLiteArrayWithoutObjectInputFilter()
        {
        testExternalizableLiteArrayObjectInputFilter(/*sFilter*/null, /*fFail*/ false, /*cLength*/10000);
        }

    @Test
    public void testArrayWithoutObjectInputFilter() throws Throwable
        {
        testArrayObjectInputFilter(/*sFilter*/null, /*fFail*/ false, /*cLength*/10000);
        }

    @Test
    public void testArrayBlockingObjectInputFilter() throws Throwable
        {
        testArrayObjectInputFilter(/*sFilter*/"data.*;maxarray=9999", /*fFail*/ true, /*cLength*/10000);
        }

    @Test
    public void testArrayWithObjectInputFilter() throws Throwable
        {
        testArrayObjectInputFilter(/*sFilter*/"data.*;maxarray=9999", /*fFail*/ false, /*cLength*/9998);
        }

    /**
     * Test ExternalizableLite ReadBuffer deserialization with ObjectInputFilter.
     */
    public void testExternalizableLiteArrayObjectInputFilter(String sFilter, boolean fFail, int cLength)
        {
        boolean  fFilter = sFilter != null;

        Exception  exception      = null;
        Object     aDeserialized  = null;
        Class<?>[] aComponentType = {byte.class, char.class, short.class, int.class, long.class, float.class, long.class, String.class, Customer.class};
        Object     ao;

        for (Class<?> componentType : aComponentType)
            {
            ao = createAndFillArray(componentType, cLength);

            try
                {
                ByteArrayWriteBuffer baos = new ByteArrayWriteBuffer(0);

                ExternalizableHelper.writeObject(baos.getBufferOutput(), ao);

                byte[] ab = baos.toByteArray();

                ByteArrayReadBuffer bais = new ByteArrayReadBuffer(ab);
                BufferInput         in   = bais.getBufferInput();

                if (fFilter)
                    {
                    in.setObjectInputFilter(ObjectInputFilterHelper.createObjectInputFilter(sFilter));
                    }

                // Provide non-null classloader to test Wrappers used for containers
                aDeserialized = ExternalizableHelper.readObject(in, Thread.currentThread().getContextClassLoader());
                }
            catch (Exception e)
                {
                exception = e;
                }

            if (!fFilter || !fFail)
                {
                assertTrue("compare serialized and deserialized value for array of " + componentType.getName() + ", should be equal", arrayEquals(ao, aDeserialized));
                }
            else
                {
                assertTrue("expected an InvalidClassException for " + ao.getClass().getName() + " ObjectInputFilter " + sFilter, containsInvalidClassExceptionCause(exception));
                }
            }
        }

    /**
     * Test ObjectInputStream deserialization with ObjectInputFilter.
     */
    public void testArrayObjectInputFilter(String sFilter, boolean fFail, int cLength)
            throws Throwable
        {
        boolean  fFilter = sFilter != null;

        Exception  exception      = null;
        Object     aDeserialized  = null;
        Class<?>[] aComponentType = {byte.class, char.class, short.class, int.class, long.class, float.class, long.class, String.class, Customer.class};
        Object     ao;

        for (Class<?> componentType : aComponentType)
            {
            ao = createAndFillArray(componentType, cLength);

            try
                {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream    oos  = new ObjectOutputStream(baos);

                ExternalizableHelper.writeObject(oos, ao);
                oos.flush();

                byte[] ab = baos.toByteArray();

                ByteArrayInputStream bais = new ByteArrayInputStream(ab);
                ObjectInputStream    ios  = new ObjectInputStream(bais);

                if (fFilter)
                    {
                    ObjectInputFilterHelper.setObjectInputStreamFilter(ios, sFilter);
                    }

                // Provide non-null classloader to test Wrappers used for containers
                aDeserialized = ExternalizableHelper.readObject(ios, Thread.currentThread().getContextClassLoader());
                }
            catch (Exception e)
                {
                exception = e;
                }

            if (!fFilter || !fFail)
                {
                assertTrue("compare serialized and deserialized value for array of " + componentType.getName() + ", should be equal", arrayEquals(ao, aDeserialized));
                }
            else
                {
                assertTrue("expected an InvalidClassException for " + ao.getClass().getName() + " ObjectInputFilter " + sFilter, containsInvalidClassExceptionCause(exception));
                }
            }
        }

    /**
     * Test ExternalizableLite ReadBuffer deserialization with ObjectInputFilter.
     */
    @Test
    public void testArrayObjectInputFilterUsingBinary()
        {
        boolean  fFilter = true;
        boolean  fFail   = true;

        Exception  exception      = null;
        Object     aDeserialized  = null;
        Class<?>[] aComponentType = {byte.class, char.class, short.class, int.class, long.class, float.class, long.class, String.class, Customer.class};
        Object ao;

        for (Class<?> componentType : aComponentType)
            {
            ao = createAndFillArray(componentType, 30001);

            try
                {
                ByteArrayWriteBuffer baos = new ByteArrayWriteBuffer(0);

                ExternalizableHelper.writeObject(baos.getBufferOutput(), ao);

                byte[] ab = baos.toByteArray();

                Binary bin = new Binary(ab);

                aDeserialized = ExternalizableHelper.fromBinary(bin, ensureSerializer(Thread.currentThread().getContextClassLoader()));
                }
            catch (Exception e)
                {
                exception = e;
                }

            if (!fFilter || !fFail)
                {
                assertTrue("compare serialized and deserialized value for array of " + componentType.getName() + ", should be equal", arrayEquals(ao, aDeserialized));
                }
            else
                {
                assertTrue("expected an InvalidClassException for " + ao.getClass().getName() + " ObjectInputFilter " + ObjectInputFilterHelper.getConfigSerialFilter(), containsInvalidClassExceptionCause(exception));
                }
            }
        }

    /**
     * Confirm JVM wide serial filter applies to Binary deserialization via ExternalizableLite.
     */
    @Test
    public void testExternalizableLiteObjectInputFilterUsingBinaryAndRemoteConstructor()
        {
        Exception exception = null;
        Customer deserialized = null;
        Object[] oa = {};
        RemoteConstructor remoteCtr = new RemoteConstructor(null, oa);

        try
            {
            ByteArrayWriteBuffer baos = new ByteArrayWriteBuffer(0);

            ExternalizableHelper.writeObject(baos.getBufferOutput(), remoteCtr);

            byte[] ab = baos.toByteArray();

            Binary bin = new Binary(ab);

            deserialized = ExternalizableHelper.fromBinary(bin, ensureSerializer(Thread.currentThread().getContextClassLoader()));
            }
        catch (Exception e)
            {
            exception = e;
            }

        assertNull(deserialized);
        assertTrue("expected an InvalidClassException for ObjectInputFilter " + ExternalizableHelper.getConfigSerialFilter(), containsInvalidClassExceptionCause(exception));
        }

    // ----- inner class: TestBufferInput ------------------------------------

    public static class TestBufferInput
            implements java.io.Serializable
        {
        public static void main(String[] args)
            {
            m_filterProcessWide   = ObjectInputFilterHelper.getConfigSerialFilter();
            f_factorySerialFilter = ObjectInputFilterHelper.getConfigSerialFilterFactory();
            s_fFilterEnabled      = m_filterProcessWide != null;

            System.out.println("Process Wide ObjectInputFilter=" + m_filterProcessWide + " serial filter factory=" + f_factorySerialFilter + " java verson: " + System.getProperty("java.version"));

            try
                {
                testExternalizableLiteObjectInputFilter(Config.getBoolean("test.fExternalizableLite", true));
                }
            catch (AssertionException e)
                {
                e.printStackTrace();
                System.err.flush();
                System.out.flush();
                Runtime.getRuntime().exit(1);
                }
            catch (Throwable t)
                {
                t.printStackTrace();
                System.err.flush();
                System.out.flush();
                Runtime.getRuntime().exit(1);
                }
            }
        }

    // ----- inner class: TestObjectInputStream ------------------------------

    public static class TestObjectInputStream
            implements java.io.Serializable
        {
        public static void main(String[] args)
            {
            m_filterProcessWide   = ObjectInputFilterHelper.getConfigSerialFilter();
            f_factorySerialFilter = ObjectInputFilterHelper.getConfigSerialFilterFactory();
            s_fFilterEnabled      = m_filterProcessWide != null;

            System.out.println("Process Wide ObjectInputFilter=" + m_filterProcessWide + " serial filter factory=" + f_factorySerialFilter + " java verson: " + System.getProperty("java.version"));

            try
                {
                testObjectInputFilter(Config.getBoolean("test.fExternalizableLite", true));
                }
            catch (Throwable t)
                {
                t.printStackTrace();
                System.err.flush();
                System.out.flush();
                Runtime.getRuntime().exit(1);
                }
            }
        }

    // ----- helpers ----------------------------------------------------------

    public static Customer generateCustomer(boolean fExternalizableLite)
        {
        Customer customer = fExternalizableLite ? new CustomerExternalizableLite() : new Customer();
        customer.setName("John Doe");

        Address address = fExternalizableLite ? new AddressExternalizableLite() : new Address();
        address.setStreet("Sesame Street");
        address.setState("NY");
        address.setCity("New York");
        address.setZipcode(55555);
        customer.setAddress(address);
        customer.setId(5);

        return customer;
        }

    /**
     * Return true iff one of the causes is InvalidClassException.
     *
     * @param t  Throwable to check for InvalidClassException
     *
     * @return true iff one of the causes is InvalidClassExceptions
     */
    public static boolean containsInvalidClassExceptionCause(Throwable t)
        {
        while (t != null)
            {
            if (t instanceof InvalidClassException)
                {
                return true;
                }
            t = t.getCause();
            }
        return false;
        }

    /**
     *
     * @param cReadBuffers
     * @param cSize
     */
    private ReadBuffer[] createReadBuffers(int cReadBuffers, int cSize)
        {
        ReadBuffer[] buffers = new ReadBuffer[cReadBuffers];
        for (int i = 0; i < cReadBuffers; i++)
            {
            buffers[i] = createAndWrap(cSize);
            }
        return buffers;
        }

    /**
     *
     * @param cSize
     *
     * @return a ByteBufferReadBuffer with cSize elements
     */
    private ReadBuffer createAndWrap(int cSize)
        {
        return new ByteBufferReadBuffer(ByteBuffer.wrap(createByteArray(cSize)));
        }

    /**
     * Create a new Byte array of cSize bytes. The content of the array will
     * initialized to index % 255.
     *
     * @param cSize  the number of bytes in the array
     *
     * @return  a byte array
     */
    private byte[] createByteArray(int cSize)
        {
        byte[] ab = new byte[cSize];
        for (int i = 0; i < cSize; i++)
            {
            ab[i] = (byte) (i % 0xFF);
            }
        return ab;
        }

    /**
     * Create a new array of componentType of cSize. The content of the array will
     * be initialized.
     *
     * @param cSize  the number of items in the array
     *
     * @return  an array of componentType with cSize items
     */
    private Object createAndFillArray(Class<?> componentType, int cSize)
        {
        Object a     = Array.newInstance(componentType, cSize);
        Object value = null;

        if (componentType.isPrimitive())
            {
            if (componentType == Integer.TYPE)
                {
                value = (int) 4;
                }
            else if (componentType == Byte.TYPE)
                {
                value = (byte) 5;
                }
            else if (componentType == Long.TYPE)
                {
                value = 30L;
                }
            else if (componentType == Float.TYPE)
                {
                value = 1.0f;
                }
            else if (componentType == Double.TYPE)
                {
                value = 2.0d;
                }
            else if (componentType == Short.TYPE)
                {
                value = (short) 5;
                }
            else if (componentType == Character.TYPE)
                {
                value = 'c';
                }
            else if (componentType == Boolean.TYPE)
                {
                value = true;
                }
            }
        else if (componentType.getClass().equals(Customer.class))
            {
            value = generateCustomer(true);
            }
        else if (componentType.getClass().equals(String.class))
            {
            value = "test";
            }

        for (int i=0; i < cSize; i++)
            {
            Array.set(a, i, value);
            }

        return a;
        }

    static private boolean arrayEquals(Object ao1, Object ao2)
        {
        if (ao1.getClass().isArray() && (ao1.getClass().isArray() == ao2.getClass().isArray()))
            {
            if (Array.getLength(ao1) != Array.getLength(ao2))
                {
                return false;
                }
            else
                {
                return true;
                }
            }
        return false;
        }

    // ----- constants --------------------------------------------------------

    private static Object m_filterProcessWide;

    private static Object f_factorySerialFilter;

    private static boolean s_fFilterEnabled;

    /**
     * Properties common to all cache applications launched in this functional test.
     */
    private static Properties propsCommon;
    }
