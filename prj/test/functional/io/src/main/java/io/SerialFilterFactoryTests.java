/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package io;


import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.util.ExternalizableHelper;

import data.BlobExternalizableLite;

import java.io.IOException;

import java.util.function.BinaryOperator;

import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.coherence.testing.CheckJDK;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Validate {@link com.tangosol.io.ReadBuffer.BufferInput BufferInput} class deserialization
 * when SerialFilterFactory is configured.
 * <p>
 * Relies on ObjectInputFilter$Config#getSerialFilterFactory and
 * ObjectInputFilter$Config#setSerialFilterFactory(ObjectInputFilter) added
 * in JDK 17.
 *
 * @author jf  2021.09.29
 */
public class SerialFilterFactoryTests
    {
    /**
     * To use {@code FilterInThread} utility create an
     * instance and configure it as the JVM-wide filter factory.
     */
    @BeforeClass
    public static void setup() throws Throwable
        {
        System.setProperty("coherence.serialFilter.logging", "true");

        // configure a process-wide ObjectInputFilter
        ObjectInputFilterHelper.setConfigObjectInputStreamFilter("examples.*");

        // configure JVM-wide serial filter factory
        filterInThread = new FilterInThread();
        ObjectInputFilterHelper.setConfigSerialFilterFactory(filterInThread);
        }

    @Test
    public void validateSerialFilterFactory()
        {
        BinaryOperator factory = ExternalizableHelper.getConfigSerialFilterFactory();
        System.out.println("serial filter factory is " + factory + " class = " + factory.getClass().getName());
        assertEquals(factory.getClass(), FilterInThread.class);
        }

    /**
     * Test using a custom serial filter Factory.
     * The {@code doWithSerialFilter} method is invoked with a filter allowing the test
     * application and core classes to be deserialized.
     */
    @Test
    public void testSerialFilterFactory()
        {
        CheckJDK.assumeJDKVersionEqualOrGreater(17);

        // Create an application specific filter to allow data.* classes and reject all others
        Object filter = ObjectInputFilterHelper.createObjectInputFilter("data.*;!*");

        filterInThread.doWithSerialFilter(filter, () ->
           {
           BlobExternalizableLite blob = new BlobExternalizableLite(40);
           ByteArrayWriteBuffer baos   = new ByteArrayWriteBuffer(0);

           try
               {
               ExternalizableHelper.writeObject(baos.getBufferOutput(), blob);
               }
           catch (IOException e)
               {
               e.printStackTrace();
               }

           byte[] ab = baos.toByteArray();

           ByteArrayReadBuffer    bais = new ByteArrayReadBuffer(ab);
           ReadBuffer.BufferInput in   = bais.getBufferInput();

           assertTrue("must have process-wide filter",         in.getObjectInputFilter().toString().contains("examples.*"));
           assertTrue("must have application specific filter", in.getObjectInputFilter().toString().contains("data.*"));

           try
               {
               data.BlobExternalizableLite o = (data.BlobExternalizableLite) ExternalizableHelper.readObject(in, null);
               assertEquals("assert deserialized object equal to original serialized object", o, blob);
               }
           catch (IOException e)
               {
               e.printStackTrace();
               }
           });
        }

    /**
     * Test using a custom serial filter Factory.
     * The {@code doWithSerialFilter} method is invoked with a filter blocking the test
     * application class to be deserialized.
     */
    @Test
    public void testSerialFilterFactoryWithBlockingObjectInputFilter()
        {
        CheckJDK.assumeJDKVersionEqualOrGreater(17);

        // Create a filter to allow example.* classes and reject all others
        Object filter = ObjectInputFilterHelper.createObjectInputFilter("!data.*;!*");

        filterInThread.doWithSerialFilter(filter, () ->
            {
            BlobExternalizableLite blob = new BlobExternalizableLite(40);
            ByteArrayWriteBuffer baos   = new ByteArrayWriteBuffer(0);

            try
                {
                ExternalizableHelper.writeObject(baos.getBufferOutput(), blob);
                }
            catch (IOException e)
                {
                e.printStackTrace();
                }

            ByteArrayReadBuffer    bais = new ByteArrayReadBuffer(baos.toByteArray());
            ReadBuffer.BufferInput in   = bais.getBufferInput();

            assertTrue("must have process-wide filter",         in.getObjectInputFilter().toString().contains("examples.*"));
            assertTrue("must have application specific filter", in.getObjectInputFilter().toString().contains("!data.*"));

            try
                {
                data.BlobExternalizableLite o = (data.BlobExternalizableLite) ExternalizableHelper.readObject(in, null);
                fail("deserialization should not have passed with ObjectInputFilter: " + filter);
                }
            catch (IOException e)
                {
                assertTrue(e.getMessage().contains("was rejected"));
                }
            });
        }

    // ----- inner class: FilterInThread ---------------------------------------

    /**
     * Test implementation of a SerialFilterFactory, adapted from JDK 17 ObjectInputFilter javadoc example.
     *
     * Replaced all occurrences of ObjectInputFilter with Object since this code
     * has to compile and run with JDK 8 and higher.
     * All ObjectInputFilter methods are called by reflection.
     */
    public static final class FilterInThread
            implements BinaryOperator<Object>
        {
        // ----- constructors --------------------------------------------------

        /**
         * Construct a FilterInThread deserialization filter factory.
         */
        public FilterInThread()
            {
            }

        // ----- BinaryOperator methods ----------------------------------------

        /**
         * Returns a composite filter of the static JVM-wide filter, a thread-specific filter,
         * and the stream-specific filter.
         *
         * @param curr  an ObjectInputFilter
         * @param next  on ObjectInputFilter
         */
        public Object apply(Object curr, Object next)
            {
            if (curr == null)
                {
                // Called from the OIS or ABI constructor or perhaps OIS or BI setObjectInputFilter with no current filter
                Object filter = filterThreadLocal.get();
                if (filter != null)
                    {
                    // Wrap the filter to reject UNDECIDED results
                    filter = ObjectInputFilterHelper.rejectUndecidedClass(filter);
                    }
                if (next != null)
                    {
                    // Merge the next filter with the thread filter, if any
                    // Initially this is the static JVM-wide filter passed from the OIS/ABI constructor
                    filter = ObjectInputFilterHelper.merge(next, filter);
                    filter = ObjectInputFilterHelper.rejectUndecidedClass(filter);
                    }
                return filter;
                }
            else
                {
                // Called from OIS or BI setObjectInputFilter with a current filter and a stream-specific filter.
                // The curr filter already incorporates the thread filter and static JVM-wide filter
                // If there is a stream-specific filter wrap it and a filter to recheck for undecided
                if (next != null)
                    {
                    next = ObjectInputFilterHelper.merge(next, curr);
                    next = ObjectInputFilterHelper.rejectUndecidedClass(next);

                    return next;
                    }
                return curr;
                }
            }

        // ----- FilterInThread methods ----------------------------------------

        /**
         * Applies the filter to the thread and invokes the runnable
         *
         * @param filter    the application-specific ObjectInputFilter
         * @param runnable  the runnable thread
         */
        public void doWithSerialFilter(Object filter, Runnable runnable)
            {
            Object prevFilter = filterThreadLocal.get();
            try
                {
                filterThreadLocal.set(filter);
                runnable.run();
                }
            finally
                {
                filterThreadLocal.set(prevFilter);
                }
            }

        // ----- data members --------------------------------------------------

        private final ThreadLocal<Object> filterThreadLocal = new ThreadLocal<>();
        }

    // ----- data members ------------------------------------------------------

    private static FilterInThread filterInThread;
    }
