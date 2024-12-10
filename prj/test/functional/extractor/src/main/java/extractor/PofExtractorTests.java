/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extractor;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.PofExtractor;

import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.LessEqualsFilter;
import com.tangosol.util.filter.LikeFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.Person;

import java.io.IOException;

import java.math.BigDecimal;

import java.util.Calendar;
import java.util.Set;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
* Functional tests for the {@link PofExtractor} implementation.
*
* @author as 02/10/2009
*/
public class PofExtractorTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("PofExtractorTests-1", "extractor");
        startCacheServer("PofExtractorTests-2", "extractor");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("PofExtractorTests-1");
        stopCacheServer("PofExtractorTests-2");
        }


    // ----- test methods ---------------------------------------------------

    /**
    * Test of how the PofIndex extractor works with Coherence filters.
    */
    @Test
    public void filterIntegrationTest()
        {
        NamedCache cache        = getNamedCache();
        Integer    iCurrentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = 0; i < 100; i++)
            {
            Person customers = new Person();
            customers.setFirstName("FirstName"+i);
            customers.setLastName("LastName"+i);
            customers.setId(""+i);
            customers.setBirthYear(iCurrentYear.intValue() - i);
            cache.put(i, customers);
            }

        Integer  nBirthYear   = iCurrentYear.intValue() - 30;

        ValueExtractor itTestExt = new PofExtractor(null, Person.BIRTH_YEAR);
        Filter         filter2   = new LessEqualsFilter(itTestExt, nBirthYear);
        org.junit.Assert.assertEquals(70, cache.entrySet(filter2).size());
        cache.release();
        }

    /**
    * Test PofExtractor on CQC.
    */
    @Test
    public void cqcPofExtractorTest()
        {
        NamedCache cache = getNamedCache();
        // make sure leftover values don't affect the test
        cache.clear();

        cache.put("1", new Person("1", "Bob", "Smith", 1974, null,
                new String[]{}));

        ContinuousQueryCache cacheCQC = new ContinuousQueryCache(cache,
                new EqualsFilter(new PofExtractor(String.class,
                        Person.FIRST_NAME), "Bob"));

        org.junit.Assert.assertEquals(1, cacheCQC.size());

        cache.put("2", new Person("2", "John", "Smith", 1974, null, new String[]{}));
        cache.put("3", new Person("3", "Bob", "Smith", 1974, null,
                new String[]{}));
        cache.put("4", new Person("4", "Bob", "Smith", 1974, null,
                new String[]{}));

        org.junit.Assert.assertEquals(3, cacheCQC.size());

        cache.remove("3");

        org.junit.Assert.assertEquals(2, cacheCQC.size());

        cache.remove("1");
        cache.remove("4");

        org.junit.Assert.assertEquals(0, cacheCQC.size());

        //Remove the last one
        cache.remove("2");

        // test clear()
        cache.put("2", new Person("2", "John", "Smith", 1974, null, new String[]{}));
        cache.put("3", new Person("3", "Bob", "Smith", 1974, null,
                new String[]{}));
        cache.put("4", new Person("4", "Bob", "Smith", 1974, null,
                new String[]{}));

        cache.clear();

        cacheCQC.release();

        // test 0 handling
        cache.put("1", new Person("1", "Bob", "Smith", 0, null, new String[]{}));

        cacheCQC = new ContinuousQueryCache(cache, new EqualsFilter(
            new PofExtractor(int.class, Person.BIRTH_YEAR), 0));

        org.junit.Assert.assertEquals(1, cacheCQC.size());

        cache.remove("1");
        }

    /**
    * Test KeyExtractor on CQC.
    */
    @Test
    public void cqcKeyExtractorTest()
        {
        NamedCache cache = getNamedCache();
        // make sure leftover values don't affect the test
        cache.clear();

        cache.put("Bob1", new Person("1", "Bob", "Smith", 1974, null,
                new String[]{}));

        ContinuousQueryCache cacheCQC = new ContinuousQueryCache(cache,
                new LikeFilter(new KeyExtractor(), "Bob%", '\\', false));

        org.junit.Assert.assertEquals(1, cacheCQC.size());

        cache.put("Bob2",
                new Person("2", "John", "Smith", 1974, null, new String[]{}));
        cache.put("Tom1", new Person("3", "Bob", "Smith", 1974, null,
                new String[]{}));
        cache.put("Bob3", new Person("4", "Bob", "Smith", 1974, null,
                new String[]{}));

        org.junit.Assert.assertEquals(3, cacheCQC.size());

        cache.remove("Bob2");

        org.junit.Assert.assertEquals(2, cacheCQC.size());

        cache.remove("Bob1");
        cache.remove("Bob3");

        org.junit.Assert.assertEquals(0, cacheCQC.size());

        //Remove the last one
        cache.remove("Tom1");
        }


    /**
    * Regression test for COH-6330.
    */
    @Test
    public void nullValueTest()
        {
        NamedCache cache = getNamedCache();

        // make sure leftover values don't affect the test
        cache.clear();

        cache.put("Bob1", null);

        cache.put("Bob2", new Person("2", "Bob", "Smith", 1974, null, new String[]{}));

        Set setEntries = cache.entrySet(new GreaterFilter("getBirthYear", 1970));
        assertEquals(1, setEntries.size());

        setEntries = cache.entrySet(new GreaterFilter(new PofExtractor(Integer.class, Person.BIRTH_YEAR), 1970));
        assertEquals(1, setEntries.size());
        }

    /**
    * Test case for COH-7541.
    */
    @Test
    public void bigDecimalTest()
        {
        BigDecimal bd    = new BigDecimal("1.23");
        NamedCache cache = getNamedCache();
        cache.clear();
        cache.put(1, new BigDecimalObject(bd));
        Assert.assertEquals(1, cache.size());

        ValueExtractor ve         = new PofExtractor(BigDecimal.class, 0);
        Set            setEntries = cache.entrySet(new EqualsFilter(ve, bd));
        Assert.assertEquals(1, setEntries.size());
        cache.clear();
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Return the cache used by all test methods.
    *
    * @return the test cache
    */
    protected NamedCache getNamedCache()
        {
        return getNamedCache("dist-test");
        }

    public static class BigDecimalObject implements PortableObject
        {
        private BigDecimal m_bd;

        public BigDecimalObject()
            {
            m_bd = new BigDecimal("0.0");
            }

        public BigDecimalObject(BigDecimal bd)
            {
            m_bd = bd;
            }

        @Override
        public void readExternal(PofReader reader) throws IOException
            {
            m_bd = reader.readBigDecimal(0);
            }

        @Override
        public void writeExternal(PofWriter writer) throws IOException
            {
            writer.writeBigDecimal(0, m_bd);
            }
        }
    }