/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package io;


import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.util.AssertionException;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import com.oracle.coherence.testing.AbstractTestInfrastructure;

import data.BlobExternalizableLite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

import java.nio.file.Files;

import java.util.Arrays;
import java.util.Properties;

import org.hamcrest.Matchers;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


public class ProcessWideObjectInputFilterPerformanceTests
        extends AbstractTestInfrastructure
    {
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.serialfilter.logging", "false");
        try
            {
            ObjectInputFilterHelper.setConfigObjectInputStreamFilter("data.BlobExternalizableLite");
            }
        catch (Throwable t) {}

        // don't start cluster
        setupProps();
        propsCommon = new Properties();
        propsCommon.put("test.log.level", "6");
        //propsCommon.put("jdk.serialFilterFactory", "io.SerialFilterFactoryTests$FilterInThread");
        }

    @AfterClass
    public static void _shutdown()
        {
        stopAllApplications();
        if (outReport != null)
            {
            outReport.close();
            }
        }

    //@Test
    public void testDirectExternalizableLiteWithObjectInputFilter()
        {
        String[] args = new String[1];
        TestBufferInput.main(args);
        }

    @Test
    public void testExternalizableLiteWithObjectInputFilter()
        {
        Properties props = new Properties();

        props.put("jdk.serialFilter", "data.BlobExternalizableLite");
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("testExternalizableLiteWithObjectInputFilter",
                                                              "io.ProcessWideObjectInputFilterPerformanceTests$TestBufferInput",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    @Test
    public void testExternalizableLiteWithBlockingObjectInputFilter()
        {
        Properties props = new Properties();

        props.put("jdk.serialFilter", "!data.BlobExternalizableLite");
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("testExternalizableLiteWithBlockingObjectInputFilter",
                                                              "io.ProcessWideObjectInputFilterPerformanceTests$TestBufferInput",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    @Test
    public void testExternalizableLiteWithoutObjectInputFilter()
        {
        Properties props = new Properties();
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("testExternalizableLiteWithoutObjectInputFilter",
                                                              "io.ProcessWideObjectInputFilterPerformanceTests$TestBufferInput",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    @Test
    public void testWithObjectInputFilter()
        {
        Properties props = new Properties();
        props.put("jdk.serialFilter", "data.BlobExternalizableLite");
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("testObjectInputFilter",
                                                              "io.ProcessWideObjectInputFilterPerformanceTests$TestObjectInputStream",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    @Test
    public void testWithBlockingObjectInputFilter()
        {
        Properties props = new Properties();
        props.put("jdk.serialFilter", "!data.BlobExternalizableLite");
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("testWithBlockingObjectInputFilter",
                                                              "io.ProcessWideObjectInputFilterPerformanceTests$TestObjectInputStream",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }


    @Test
    public void testWithoutObjectInputFilter()
        {
        Properties props = new Properties();
        props.putAll(propsCommon);

        CoherenceClusterMember member = startCacheApplication("testWithoutObjectInputFilter",
                                                              "io.ProcessWideObjectInputFilterPerformanceTests$TestObjectInputStream",
                                                              "io", "", props);

        int result = member.waitFor(Timeout.after("30s"));
        assertThat(result, is(0));
        }

    /**
     * Measure ObjectInputStream deserialization in nanoseconds.
     *
     * @param nIterations  number of deserialization iterations
     * @param nSize        size in bytes of instance being deserialized
     */
    public static void testObjectInputFilter(int nIterations, int nSize)
        {
        boolean                fFilter = s_fFilterEnabled;
        boolean                fFail   = s_fFilterEnabled && m_filterProcessWide.toString().contains("!data.");
        BlobExternalizableLite blob    = new BlobExternalizableLite(nSize);

        Exception   exception  = null;
        long        ldtStart   = -1;
        String      sFilter    = m_filterProcessWide == null ? null : m_filterProcessWide.toString();
        int         nReadCount = 0;

        try
            {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream    oos  = new ObjectOutputStream(baos);

            ExternalizableHelper.writeObject(oos, blob);
            oos.flush();

            byte[] ab = baos.toByteArray();
            assertThat(ab.length, Matchers.greaterThan(nSize));

            ByteArrayInputStream bais = new ByteArrayInputStream(ab);

            ldtStart = start();
            for (int i = 0; i < nIterations; i++)
                {
                ObjectInputStream ios = new ObjectInputStream(bais);

                ExternalizableHelper.readObject(ios, null);
                ios.close();

                nReadCount++;
                bais.reset();
                }
            }
        catch (Exception e)
            {
            exception = e;
            }

        //String sDescription = String.format("%s filter: %12s count=%d objectsize=%5d", "Externalizable ObjectInput: ",
        //                                    sFilter, nIterations, nSize);

        String sDescription = String.format("%-17s\t%12s\t%d\t%5d\t", "ObjectInputStream", sFilter, nIterations, nSize);

        // skip reporting stats for MAX_WARMUP
        if (nIterations == MAX_ITERATIONS)
            {
            stop(ldtStart, sDescription);
            }
        if (!fFilter || !fFail)
            {
            assertEquals("expected deserialized collection to be equal to serialized collection for ObjectInputFilter=" + sFilter,
                         nReadCount, nIterations);
            }
        else
            {
            assertTrue("expected an InvalidClassException for ObjectInputFilter " + sFilter,
                       exception != null && exception.getCause() instanceof InvalidClassException);
            }
        }

    /**
     * Measure ExternalizableLite ReadBuffer deserialization in nanoseconds.
     *
     * @param nIterations  number of deserialization iterations
     * @param nSize        size in bytes of instance being deserialized
     */
    public static void testExternalizableLiteObjectInputFilter(int nIterations, int nSize)
        {
        boolean                fFilter = s_fFilterEnabled;
        boolean                fFail   = s_fFilterEnabled && m_filterProcessWide.toString().contains("!data.");
        BlobExternalizableLite blob    = new BlobExternalizableLite(nSize);

        Exception   exception  = null;
        long        ldtStart   = -1;
        String      sFilter    = m_filterProcessWide == null ? null : m_filterProcessWide.toString();
        int         nReadCount = 0;

        try
            {
            ByteArrayWriteBuffer baos = new ByteArrayWriteBuffer(0);

            ExternalizableHelper.writeObject(baos.getBufferOutput(), blob);

            byte[] ab = baos.toByteArray();

            assertThat(ab.length, Matchers.greaterThan(nSize));

            ByteArrayReadBuffer    bais = new ByteArrayReadBuffer(ab);
            ReadBuffer.BufferInput in   = bais.getBufferInput();

            if (s_fFilterEnabled)
                {
                assertEquals(ExternalizableHelper.getConfigSerialFilter(), in.getObjectInputFilter());
                }

            in.mark(2000000000);

            ldtStart = start();
            for (int i = 0; i < nIterations; i++)
                {
                ExternalizableHelper.readObject(in, null);
                nReadCount++;
                in.reset();
                }
            }
        catch (Exception e)
            {
            exception = e;
            }

        //String sDescription = String.format("%s filter: %12s count=%d objectsize=%5d", "Externalizable BufferInput: ", sFilter, nIterations, nSize);
        String sDescription = String.format("%-17s\t%12s\t%d\t%5d\t", "BufferInput", sFilter, nIterations, nSize);

        // skip reporting stats for MAX_WARMUP
        if (nIterations == MAX_ITERATIONS)
            {
            stop(ldtStart, sDescription);
            }

        if (!fFilter || !fFail)
            {
            assertEquals("expected deserialized collection to be equal to serialized collection for ObjectInputFilter=" + sFilter,
                         nReadCount, nIterations);
            }
        else
            {
            assertTrue("expected an InvalidClassException for ObjectInputFilter " + sFilter,
                       exception != null && exception.getCause() instanceof InvalidClassException);
            }
        }

    /**
     * Return start time in millieconds.
     *
     * @return start time in milliseconds
     */
    public static long start()
        {
        return System.nanoTime();
        }

    /**
     * Log a description of an operation being timed and its duration in milliseconds.
     *
     * @param ldtStart      start time in milliseconds of operation being timed
     * @param sDescription  operation description
     */
    public static void stop(long ldtStart, String sDescription)
        {
        long ldtElapsed = System.nanoTime() - ldtStart;

        Base.out(String.format("%s  %10dns", sDescription, ldtElapsed));

        // skip blocked scenario in report
        if (!sDescription.contains("!data."))
            {
            outReport.write(String.format("%s  %10d\n", sDescription, ldtElapsed));
            }
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
                File fileReport = new File(System.getProperty("test.root.dir", "/Users/jfialli/p4-coh/dev/main/prj") + "/test/functional/io/target/test-output", "report_ProcessWideObjectInputFilterPerformanceTests.txt");

                outReport = new PrintWriter(Files.newOutputStream(fileReport.toPath(), CREATE, APPEND));
                }
            catch (IOException e)
                {
                e.printStackTrace();
                }

            try
                {
                Arrays.stream(anSizes).forEach(s -> testExternalizableLiteObjectInputFilter(MAX_WARMUP, s));
                System.gc();
                Arrays.stream(anSizes).forEach(s -> testExternalizableLiteObjectInputFilter(MAX_ITERATIONS, s));
                outReport.close();
                }
            catch (AssertionException e)
                {
                e.printStackTrace();
                System.err.flush();
                System.out.flush();
                outReport.close();
                Runtime.getRuntime().exit(1);
                }
            catch (Throwable t)
                {
                t.printStackTrace();
                System.err.flush();
                System.out.flush();
                outReport.close();
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
                File fileReport = new File(System.getProperty("test.root.dir") + "/test/functional/io/target/test-output", "report_ProcessWideObjectInputFilterPerformanceTests.txt");

                outReport = new PrintWriter(Files.newOutputStream(fileReport.toPath(), CREATE, APPEND));
                }
            catch (IOException e)
                {
                e.printStackTrace();
                }

            try
                {
                Arrays.stream(anSizes).forEach(s -> testObjectInputFilter(MAX_WARMUP, s));
                System.gc();
                Arrays.stream(anSizes).forEach(s -> testObjectInputFilter(MAX_ITERATIONS, s));
                outReport.close();
                }
            catch (Throwable t)
                {
                t.printStackTrace();
                System.err.flush();
                System.out.flush();
                outReport.close();
                Runtime.getRuntime().exit(1);
                }
            }
        }

    // ----- constants --------------------------------------------------------

    /**
     * Number of iterations for warmup.
     */
    private static final int MAX_WARMUP     = 20000;

    /**
     * Number of times to run time measured deserialization.
     */
    private static final int MAX_ITERATIONS = 1000000;

    /**
     * List of sizes in bytes of serialization objects to be tested.
     */
    private static final int[] anSizes = {100, 1024, 1024 * 2, 1024 * 4};

    private static Object m_filterProcessWide;

    private static Object f_factorySerialFilter;

    private static boolean s_fFilterEnabled;

    private static PrintWriter outReport;

    /**
     * Properties common to all cache applications launched in this functional test.
     */
    private static Properties propsCommon;
    }
