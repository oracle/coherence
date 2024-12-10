/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extractor;


import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;
import com.tangosol.io.pof.reflect.SimplePofPath;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ConditionalIndex;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ConditionalExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.IndexAwareExtractor;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EntryFilter;
import com.tangosol.util.processor.AbstractProcessor;

import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.LessFilter;
import com.tangosol.util.filter.NotEqualsFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.Person;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.tangosol.util.ExternalizableHelper.readObject;
import static com.tangosol.util.ExternalizableHelper.writeObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
* A collection of functional tests for the {@link ConditionalExtractor}.
*
* @author tb 02/12/2010
*/
public class ConditionalExtractorTests
        extends AbstractFunctionalTest

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

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    /**
    * Test of how the Conditional Extractor works with Coherence.
    */
    @Test
    public void test()
        {
        doTest(new ReflectionExtractor("getBirthYear"),
               new NotEqualsFilter("getId", null));

        doTest(new PofExtractor(Integer.class, Person.BIRTH_YEAR),
               new NotEqualsFilter(new PofExtractor(String.class, Person.SSN), null));

        doTest(new ReflectionExtractor("getBirthYear"),
               new NotEqualsFilter(new PofExtractor(String.class, Person.SSN), null));

        doTest(new PofExtractor(Integer.class, Person.BIRTH_YEAR),
               new NotEqualsFilter("getId", null));
        }

    /**
    * Helper method for the main test.
    *
    * @param extractorYear the BirthYear extractor
    * @param filterSSN  the SSN != null filter
    */
    protected void doTest(ValueExtractor extractorYear, Filter filterSSN)
        {
        NamedCache cache = getNamedCache();

        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            Person person = new Person();
            person.setFirstName("FirstName"+i);
            person.setLastName("LastName"+i);
            person.setBirthYear(1900 + i );

            if (i % 2 == 0 )
                {
                person.setId(""+i);
                }
            else
                {
                person.setId(null);
                }

            cache.put(Integer.valueOf(i), person);
            }

        // test with forward index support == true
        ValueExtractor condExtractor = new ConditionalExtractor(filterSSN, extractorYear, true);

        Filter queryFilter = new GreaterFilter(extractorYear, 1949);
        Assert.assertEquals("ConditionalExtractor : The query results should contain all keys ", 50,
                            cache.entrySet(queryFilter).size());

        // add the conditional index which should only contain values for the
        // entries with non-null Ids
        cache.addIndex(condExtractor, true, null);

        Assert.assertEquals("ConditionalExtractor : The query should be based on the conditional index ", 25,
                            cache.entrySet(queryFilter).size());

        // add an entry to the cache that should add a value to the index
        Person person = new Person("100", "FirstName100", "LastName100", 2000,
                  "", new String[]{});
        cache.put(100, person);

        Assert.assertEquals("ConditionalExtractor : The query should be based on the conditional index ", 26,
                            cache.entrySet(queryFilter).size());

        // add an entry to the cache that should not be used for the index
        person = new Person(null, "FirstName101", "LastName101", 2001,
                  "", new String[]{});
        cache.put(101, person);

        Assert.assertEquals("ConditionalExtractor : The query should be based on the conditional index ", 26,
                            cache.entrySet(queryFilter).size());

        // remove an entry from the cache that should not impact the index
        cache.remove(101);

        Assert.assertEquals("ConditionalExtractor : The query should be based on the conditional index ", 26,
                            cache.entrySet(queryFilter).size());

        // remove an entry from the cache that should remove from the index
        cache.remove(100);

        Assert.assertEquals("ConditionalExtractor : The query should be based on the conditional index ", 25,
                            cache.entrySet(queryFilter).size());

        // remove the conditional index
        cache.removeIndex(condExtractor);

        Assert.assertEquals("ConditionalExtractor : The query results should contain all keys ", 50,
                            cache.entrySet(queryFilter).size());

        // test with forward index support == false
        condExtractor = new ConditionalExtractor(filterSSN, extractorYear, false);

        // add the conditional index which should only contain values for the
        // entries with non-null Ids
        cache.addIndex(condExtractor, true, null);

        Assert.assertEquals("ConditionalExtractor : The query should be based on the conditional index ", 25,
                            cache.entrySet(queryFilter).size());

        // add an entry to the cache that should add a value to the index
        person = new Person("100", "FirstName100", "LastName100", 2000,
                  "", new String[]{});
        cache.put(100, person);

        Assert.assertEquals("ConditionalExtractor : The query should be based on the conditional index ", 26,
                            cache.entrySet(queryFilter).size());

        // add an entry to the cache that should not be used for the index
        person = new Person(null, "FirstName101", "LastName101", 2001,
                  "", new String[]{});
        cache.put(101, person);

        Assert.assertEquals("ConditionalExtractor : The query should be based on the conditional index ", 26,
                            cache.entrySet(queryFilter).size());

        // remove an entry from the cache that should not impact the index
        cache.remove(101);

        Assert.assertEquals("ConditionalExtractor : The query should be based on the conditional index ", 26,
                            cache.entrySet(queryFilter).size());

        // remove an entry from the cache that should remove from the index
        cache.remove(100);

        Assert.assertEquals("ConditionalExtractor : The query should be based on the conditional index ", 25,
                            cache.entrySet(queryFilter).size());

        // remove the conditional index
        cache.removeIndex(condExtractor);

        Assert.assertEquals("ConditionalExtractor : The query results should contain all keys ", 50,
                            cache.entrySet(queryFilter).size());

        cache.release();
        }

    /**
    * Regression test for COH-3146 (updates with no forward-index).
    */
    @Test
    public void testCoh3146()
        {
        final ValueExtractor extractor     = new ReflectionExtractor("getLastName");
        final Filter         filter        = new NotEqualsFilter("getId", null);
        final NamedCache     cache         = getNamedCache();
        final MapIndex[]     aMapIndex     = new MapIndex[1];
        IndexAwareExtractor  condExtractor = new IndexAwareExtractor()
            {
            public MapIndex createIndex(boolean fOrdered, Comparator comparator,
                                        Map mapIndex, BackingMapContext ctx)
                {
                return aMapIndex[0] = m_condExtractor.createIndex(
                    fOrdered, comparator, mapIndex, null);
                }

            public MapIndex destroyIndex(Map mapIndex)
                {
                return m_condExtractor.destroyIndex(mapIndex);
                }

            public Object extract(Object oTarget)
                {
                return m_condExtractor.extract(oTarget);
                }
            ConditionalExtractor m_condExtractor =
                new ConditionalExtractor(filter, extractor, false);
            };

        cache.clear();
        cache.addIndex(condExtractor, true, null);

        Person person = new Person("100", "FirstName100", "LastName100", 0,
                                   "", new String[]{});
        cache.put(100, person);
        cache.remove(100);

        assertTrue(aMapIndex[0].getIndexContents().isEmpty());

        cache.removeIndex(condExtractor);
        cache.release();
        }


    /**
    * Regression tests for Coh-3966
    */
    @Test
    public void testCoh3966()
        {
        doCoh3966Test(new LessFilter(IdentityExtractor.INSTANCE, 0));
        doCoh3966Test(new LessFilter(IdentityExtractor.INSTANCE, 25));
        doCoh3966Test(new LessFilter(IdentityExtractor.INSTANCE, 100));
        }

    /**
    * Helper method to run a regression test for COH-3966
    *
    * @param filter  the filter to test with
    */
    protected void doCoh3966Test(Filter filter)
        {
        NamedCache cache      = getNamedCache();
        String     sCacheName = cache.getCacheName();

        cache.destroy();
        cache = getNamedCache();

        // populate the cache
        for (int i = 0; i < 100; i++)
            {
            cache.put(i, i);
            }

        // add the conditional index
        cache.addIndex(new Coh3966Extractor(filter), false, null);

        // start another storage member and ensure that the index is created properly
        startCacheServer("Coh3966-1", "extractor");

        Coh3966VerifierProcessor processor =
                new Coh3966VerifierProcessor(sCacheName, filter);
        for (int i = 0; i < 100; i++)
            {
            Assert.assertEquals(Boolean.TRUE, cache.invoke(i, processor));
            }

        stopCacheServer("Coh3966-1");

        cache.release();
        }

    public static class Coh3966Extractor
            implements IndexAwareExtractor, PortableObject
        {
        /**
        * Default constructor.
        */
        public Coh3966Extractor()
            {
            }

        /**
        * Construct an extractor with the specified filter.
        *
        * @param filter  the filter to use to create the conditional index
        */
        public Coh3966Extractor(Filter filter)
            {
            m_filter = filter;
            }

        /**
        * {@inheritDoc}
        */
        public MapIndex createIndex(boolean fOrdered, Comparator comparator, Map mapIndex, BackingMapContext ctx)
            {
            ValueExtractor extractor = IdentityExtractor.INSTANCE;
            MapIndex       idx       = new ConditionalIndex(
                m_filter, extractor, false, null, false, ctx);

            mapIndex.put(extractor, idx);
            return idx;
            }

        /**
        * {@inheritDoc}
        */
        public MapIndex destroyIndex(Map mapIndex)
            {
            return (MapIndex) mapIndex.remove(IdentityExtractor.INSTANCE);
            }

        /**
        * {@inheritDoc}
        */
        public Object extract(Object oTarget)
            {
            return oTarget;
            }

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_filter = (Filter) in.readObject(0);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_filter);
            }

        // ----- data members ---------------------------------------------

        /**
        * The filter to use for the conditional extractor.
        */
        private Filter m_filter;
        }

    public static class Coh3966VerifierProcessor
            extends AbstractProcessor
            implements PortableObject
        {
        /**
        * Default constructor.
        */
        public Coh3966VerifierProcessor()
            {
            }

        /**
        * Construct a verifier processor.
        *
        * @param sCacheName  the cache name
        * @param filter      the filter to verify with
        */
        public Coh3966VerifierProcessor(String sCacheName, Filter filter)
            {
            m_sCacheName = sCacheName;
            m_filter     = filter;
            }

        /**
        * {@inheritDoc}
        */
        public Object process(InvocableMap.Entry entry)
            {
            BinaryEntry              binEntry = (BinaryEntry) entry;
            BackingMapManagerContext ctx      = binEntry.getContext();

            try
                {
                Object   oStorage = ClassHelper.invoke(
                    ctx.getCacheService(), "getStorage", new Object[] { m_sCacheName });
                Map      mapIndex = (Map) ClassHelper.invoke(
                    oStorage, "getIndexMap", ClassHelper.VOID);
                MapIndex index    = (MapIndex) mapIndex.values().iterator().next();

                Boolean FResult = Boolean.TRUE;
                for (Iterator iter = index.getIndexContents().keySet().iterator();
                     iter.hasNext(); )
                    {
                    Object oValue = iter.next();
                    if (!m_filter.evaluate(oValue))
                        {
                        Base.err("Index contains value: " + oValue +
                                 " which does not satisfy the filter");
                        FResult = Boolean.FALSE;
                        }
                    }

                return FResult;
                }
            catch (Exception e)
                {
                Base.err("Unexpected exception: " + e);
                Base.err(Base.getStackTrace(e));
                return Boolean.FALSE;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_sCacheName = in.readString(0);
            m_filter     = (Filter) in.readObject(1);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sCacheName);
            out.writeObject(1, m_filter);
            }

        // ----- data members ---------------------------------------------

        /**
        * The cache name.
        */
        private String m_sCacheName;

        /**
        * The filter to check the index contents against.
        */
        private Filter m_filter;
        }

    /**
    * Regression test for COH-5516.
    */
    @Test
    public void testCoh5516()
        {
        NamedCache     cache                = getNamedCache();
        Filter         typeFilter           = new EqualsFilter("getType", "bird");
        ValueExtractor intExtractor         = new ReflectionExtractor("getWings");
        ValueExtractor conditionalExtractor = new ConditionalExtractor(typeFilter, intExtractor, false);

        cache.clear();
        cache.addIndex(conditionalExtractor, false, null);

        // add entries of type Fish and Bird - only the Birds should get indexed
        cache.put(0, new Bird());
        cache.put(1, new Fish());

        // remove entries of type Fish and Bird - only the Birds should get indexed
        cache.remove(0);
        cache.remove(1);
        cache.release();
        }

    @Portable
    public static class Employee implements Serializable
        {
        public Employee()
            {
            }

        public Employee(APerson person)
            {
            m_person = person;
            }

        public APerson getPerson()
            {
            return m_person;
            }

        public String toString()
            {
            return "Employee[person =" + ((m_person == null) ? "NULL" :  m_person.toString()) + "]";
            }

        @PortableProperty(0)
        public APerson m_person;
        }

    @Portable
    public static class APerson implements Serializable
        {
        public APerson()
            {
            }

        public APerson(String lastName, MyAddress address)
            {
            m_lastName = lastName;
            m_address  = address;
            }

        public MyAddress getAddress()
            {
            return m_address;
            }

        public String toString()
            {
            return "APerson[lastName=" + m_lastName + " address= " + ((m_address == null) ? "NULL" : m_address.toString()) + "]";
            }

        @PortableProperty(0)
        public MyAddress m_address;

        @PortableProperty(1)
        public String m_lastName;
        }

    @Portable
    public static class MyAddress implements Serializable
        {
        public MyAddress()
            {
            }

        public MyAddress(String city, String zipcode)
            {
            m_city = city;
            m_zipcode = zipcode;
            }
        public String getCity()
            {
            return m_city;
            }

        public String toString()
            {
            return "MyAddress[ city=" + m_city + ", zipCode=" + m_zipcode + "]";
            }

        @PortableProperty(0)
        public String m_city;

        @PortableProperty(1)
        public String m_zipcode;
        }


    public static class ReporterIsNotNullFilter<K, V>
            implements EntryFilter<K,V>,
            ExternalizableLite, PortableObject
        {
        public ReporterIsNotNullFilter(ValueExtractor extractor)
            {
            m_extractor = extractor;
            }

        @Override
        public boolean evaluateEntry(Entry<? extends K, ? extends V> entry)
            {
            boolean result = evaluate(entry.getValue());
            if (! result)
                {
                System.out.println("unexpected value for entry.key=" + entry.getKey()
                        + " entry.value=" + entry.getValue());
                }
            return result;
            }

        @Override
        public boolean evaluate(V o)
            {
            return o != null && m_extractor.extract(o) != null;
            }

        // ----- ExternalizableLite interface -----------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            m_extractor = readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            writeObject(out, m_extractor);
            }


        // ----- PortableObject interface ---------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_extractor = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_extractor);
            }



        private ValueExtractor m_extractor;
        }

    // COH-18199 : regression test : confirm no NPE building conditional index
    @Test
    public void testConditionalExtractorPofNavigation()
        {
        ValueExtractor pofCityExtractor     = new PofExtractor(String.class, new SimplePofPath(new int[] {0, 0, 0}));
        ValueExtractor conditionalExtractor = new ConditionalExtractor(new AlwaysFilter(), pofCityExtractor, false, false);

        NamedCache cache = getNamedCache();

        cache.clear();
        cache.addIndex(conditionalExtractor, false, null);

        cache.put("MissingPerson", new Employee());
        cache.put("Smith", new Employee(new APerson("Smith", new MyAddress("Reading", "01867"))));
        cache.put("Green", new Employee(new APerson("Green", new MyAddress("Washington", "55555"))));

        Collection<Employee> employees = cache.values(new EqualsFilter(pofCityExtractor, "Reading"));
        assertEquals(1, employees.size());
        }


    @SuppressWarnings("serial")
    public static class Fish
            implements PortableObject, Serializable
        {
        public String getType()
            {
            return "fish";
            }

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }
        }

    @SuppressWarnings("serial")
    public static class Bird
            implements PortableObject, Serializable
        {
        public String getType()
            {
            return "bird";
            }

        public int getWings()
            {
            return 2;
            }

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }
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
    }
