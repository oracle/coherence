/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package processor;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.tangosol.net.NamedCache;
import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.ContainsAnyFilter;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;


public class Coh7206Tests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public Coh7206Tests()
        {
        super();
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.distributed.threads", "2");
        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------
    @Test
    public void testCollection() throws Exception
        {
        NamedCache cache = getNamedCache("dist-std-tests");
        try
            {
            cache.clear();
            cache.addIndex(IdentityExtractor.INSTANCE, false, null);

            DoPutALL doDoPutAll = new DoPutALL(cache, "collection");

            new Thread(doDoPutAll).start();

            Blocking.sleep(2222);

            int cSize = cache.size();
            for (int i = 0; i<50000; i++)
                {
                int cResult = cache.entrySet(FILTER).size();
                Base.azzert(cResult == cSize, "[" + i + "] expected:" + cSize + ", actual:" + cResult);
                }

            setStop(true);
            }
        finally
            {
            cache.destroy();
            out("--- COH7206: testCollection() done ---");
            }
        }

    @Test
    public void testArray() throws Exception
        {
        NamedCache cache = getNamedCache("dist-std-tests");
        try
            {
            cache.clear();
            cache.addIndex(IdentityExtractor.INSTANCE, false, null);

            DoPutALL doDoPutAll = new DoPutALL(cache, "array");

            new Thread(doDoPutAll).start();

            Blocking.sleep(2222);

            int cSize = cache.size();
            for (int i = 0; i<50000; i++)
                {
                int cResult = cache.entrySet(FILTER).size();
                Base.azzert(cResult == cSize, "[" + i + "] expected:" + cSize + ", actual:" + cResult);
                }

            setStop(true);
            }
        finally
            {
            cache.destroy();
            out("--- COH7206: testArray() done ---");
            }
        }

    class DoPutALL implements Runnable
        {
        public DoPutALL(NamedCache cache, String name)
            {
            m_cache = cache;
            m_name  = name;
            }

        public void run()
            {
            NamedCache cache = m_cache;
            String     sname = m_name;

            Random random = new Random();

            if (!isStop())
                {
                if (sname.equals("collection"))
                    {
                    doCollection(cache, sname, random);
                    }
                else
                    {
                    doArray(cache, sname, random);
                    }
                }
            }

        private void doCollection(NamedCache cache, String sname, Random random)
            {
            List values = new ArrayList();
            for (int i = 0; i <= 8; i++)
                {
                // duplicates will cause Missing inverse index messages
                values.add("test");
                values.add("test " + random.nextInt(4));

                cache.putAll(Collections.singletonMap(sname, values));
                }
            }

        private void doArray(NamedCache cache, String sname, Random random)
            {
            String[] values = {"null", "null", "null", "null", "null", "null", "null", "null", "null", "null"};
            for (int i = 0; i <= 8; i++)
                {
                // duplicates will cause Missing inverse index messages
                values[i]     = "test";
                values[i + 1] = "test " + random.nextInt(4);

                cache.putAll(Collections.singletonMap(sname, values));
                }
            }

        private NamedCache m_cache;
        private String     m_name;
        }

    void setStop(boolean fStop)
        {
        m_fStop = fStop;
        }

    boolean isStop()
        {
        return m_fStop;
        }

    protected boolean m_fStop = false;
    static Filter FILTER = new ContainsAnyFilter(IdentityExtractor.INSTANCE, Collections.singleton("test"));
    }
