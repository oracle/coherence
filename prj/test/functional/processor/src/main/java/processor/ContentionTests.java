/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package processor;


import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.NamedCache;

import com.oracle.coherence.common.base.Blocking;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.processor.AbstractProcessor;

import org.junit.BeforeClass;
import org.junit.Test;


/**
 * A regression test for COH-5727
 */
public class ContentionTests
        extends AbstractEntryProcessorTests
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractEntryProcessorTests._startup();
        }

    /**
     * A shared processor which is used to verify that two invocations can
     * execute simultaneously
     *
     * NOTE: This processor is not intended to be serialized
     */
    public static class CheckProcessor
            extends AbstractProcessor
        {

        public CheckProcessor(int nFlags)
            {
            m_afFlags = new boolean[nFlags];
            }

        @Override
        public Object process(Entry entry)
            {
            Integer iEntry = (Integer) entry.getValue();
            System.out.println("Running EntryProcessor for entry " + iEntry);

            m_afFlags[iEntry] = true;

            for (int i = 0; i < 1000; i++)
                {
                if (checkComplete())
                    {
                    System.out.println("Done at iteration " + i + " for entry " + iEntry);
                    break;
                    }
                try
                    {
                    System.out.println("Iteration " + i + " for entry " + iEntry + " still not complete retrying...");
                    Blocking.sleep(10);
                    }
                catch (InterruptedException e)
                    {
                    e.printStackTrace();
                    }
                }

            Eventually.assertThat(invoking(this).checkComplete(), is(true));

            return checkComplete();
            }

        /**
         * @return true iff all the flags are set
         */
        public boolean checkComplete()
            {
            for (boolean fFlag : m_afFlags)
                {
                if (!fFlag)
                    {
                    return false;
                    }
                }
            return true;
            }

        /**
         * @return true iff all the flags except <tt>nExclude</tt>
         */
        private boolean checkComplete(int nExclude)
            {
            for (int i = 0; i < m_afFlags.length; i++)
                {
                if (i != nExclude && !m_afFlags[i])
                    {
                    return false;
                    }
                }
            return true;
            }

        static volatile boolean m_afFlags[];
        }

    public class InvocationThread
            extends Thread
        {

        /**
         * @return the fComplete
         */
        public boolean isComplete()
            {
            return m_fComplete;
            }

        public InvocationThread(int nEntry)
            {
            m_nEntry = nEntry;
            }

        private volatile boolean m_fComplete;

        /**
         * {@inheritDoc}
         */
        @Override
        public void run()
            {
            System.out.println("Starting thread for Entry " + m_nEntry);
            m_fComplete = (Boolean) getNamedCache().invokeAll(new EqualsFilter(IdentityExtractor.INSTANCE, m_nEntry),
                    m_processor).get(m_nEntry);
            System.out.println("Stopping thread for Entry " + m_nEntry);
            }

        private final int m_nEntry;
        }

    /**
     * Default constructor.
     */
    public ContentionTests()
        {
        super("dist-pool-test1");
        }

    @Test
    public void testContention()
        {
        final NamedCache cache = getNamedCache();
        for (int i = 0; i < 2; i++)
            {
            cache.put(i, i);
            }

        m_processor = new CheckProcessor(2);
        InvocationThread[] aThreads = new InvocationThread[2];
        for (int i = 0; i < 2; i++)
            {
            (aThreads[i] = new InvocationThread(i)).start();
            }

        boolean fComplete = true    ;
        for (int i = 0; i < 2; i++)
            {
            try
                {
                System.out.println("Waiting for Thread " + i + " to complete");
                aThreads[i].join();
                System.out.println("Thread " + i + " done (" + aThreads[i].isComplete() + ")");
                fComplete = fComplete && aThreads[i].isComplete();
                }
            catch (InterruptedException e)
                {
                e.printStackTrace();
                }
            }

        assertTrue(fComplete);
        }

    CheckProcessor m_processor;
    }
