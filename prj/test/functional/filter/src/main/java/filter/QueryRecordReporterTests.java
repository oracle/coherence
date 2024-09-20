/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package filter;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Filter;
import com.tangosol.util.Resources;
import com.tangosol.util.SimpleQueryRecord;
import com.tangosol.util.SimpleQueryRecordReporter;
import com.tangosol.util.aggregator.QueryRecorder;

import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.BetweenFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.LessEqualsFilter;
import com.tangosol.util.filter.LikeFilter;
import com.tangosol.util.filter.PartitionedFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.pof.Address;

import filter.nestinglevel1.nestinglevel2.nestinglevel3.IntegerToStringPersonKeyExtractor;
import filter.nestinglevel1.nestinglevel2.nestinglevel3.StringToIntegerPersonAddressZipExtractor;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.net.URL;

import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.*;


/**
 * Tests of SimpleQueryRecordReporter
 *
 * @author op  2013.02.07
 */
public class QueryRecordReporterTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class by starting separate servers.
     */
    @BeforeClass
    public static void startup()
        {
        System.setProperty("coherence.distributed.localstorage", "true");
        CoherenceCacheServer clusterMember1 = (CoherenceCacheServer) startCacheServer("QueryRecordReporterTests-1", "filter", null, PROPS_SEONE);
        CoherenceCacheServer clusterMember2 = (CoherenceCacheServer) startCacheServer("QueryRecordReporterTests-2", "filter", null, PROPS_SEONE);
        Eventually.assertThat(invoking(clusterMember1).getClusterSize(), is(3));
        Eventually.assertThat(invoking(clusterMember2).getClusterSize(), is(3));

        NamedCache cache = CacheFactory.getCache(getCacheName());
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        Eventually.assertThat(clusterMember1, new PartitionedCacheServiceIsBalanced(service.getInfo().getServiceName()), is(true));
        Eventually.assertThat(clusterMember2, new PartitionedCacheServiceIsBalanced(service.getInfo().getServiceName()), is(true));
        }

    /**
    * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("QueryRecordReporterTests-1");
        stopCacheServer("QueryRecordReporterTests-2");
        }

    @Before
    public void populateCache()
        {
        NamedCache cache = getNamedCache(getCacheName());

        if (cache.size() == 100)
            {
            out(cache.getCacheName() + " is already populated.");
            return;
            }
        out("Populating " + cache.getCacheName());

        Address[] aAddress = { new Address("1 Main St.", "Chelsea", "MA", "02001"),
                               new Address("1 Calle Ocho", "Caguas", "PR", "13008"),
                               new Address("1 Heath Nook", "Wellingborough", "UK", "00071")};
        cache.put(0, new Person(0, "Thom", "Yorke", "012-34-5678", 1968, aAddress[2]));
        cache.put(1, new Person(1, "Chick", "Corea", "012-34-3456", 1941, aAddress[0]));
        cache.put(2, new Person(2, "Hector", "La Voz", "035-00-3456", 1928, aAddress[1]));

        for (int i=3; i < 100; i++ )
            {
            Person p = (Person) cache.get(i % 3);
            cache.put(i, new Person(i, p.getFirstName()+"-"+i, p.getLastName()+"-"+i, p.getSSN(),
                                    p.getBirthYear() + i/3, aAddress[i%3]));
            }
        assertTrue("Cache was not fully populated", cache.size() == 100);
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testReporterDoubleIndex() throws Exception
        {
        NamedCache cache = getNamedCache(getCacheName());

        cache.addIndex(IntegerToStringPersonKeyExtractor.INSTANCE, true, null);
        cache.addIndex(StringToIntegerPersonAddressZipExtractor.INSTANCE, true, null);

        // COH-14657 ensure that parallel index creations on every partition are done
        cache.keySet(AlwaysFilter.INSTANCE);

        Filter filterAll = new AllFilter( new Filter[]
            {
            new LikeFilter(IntegerToStringPersonKeyExtractor.INSTANCE, "&2%", '&', false),
            new LessEqualsFilter("getAge", 98),
            new LessEqualsFilter("getAge", 98),  // duplicate; should be removed
            new AndFilter( new BetweenFilter(StringToIntegerPersonAddressZipExtractor.INSTANCE, 100, 15000),  // nested AND filter; should be unwrapped and pulled up
                           new EqualsFilter("getFirstName", "Hector"))
            });

        out(" * * * * * * * Double-Index Query * * * * * * * * * * * * * * (testReporterDoubleIndex)");
        testReporter(cache, filterAll, QueryRecorder.RecordType.EXPLAIN, "explain-DoubleIndex.ptn");
        testReporter(cache, filterAll, QueryRecorder.RecordType.TRACE, "trace-DoubleIndex.ptn");

        cache.removeIndex(IntegerToStringPersonKeyExtractor.INSTANCE);
        cache.removeIndex(StringToIntegerPersonAddressZipExtractor.INSTANCE);
        }

    @Test
    public void testByPartitions() throws Exception
        {
        NamedCache cache   = getNamedCache(getCacheName());

        cache.addIndex(StringToIntegerPersonAddressZipExtractor.INSTANCE, true, null);

        // COH-14657 ensure that parallel index creations on every partition are done
        cache.keySet(AlwaysFilter.INSTANCE);

        DistributedCacheService service      = (DistributedCacheService) cache.getCacheService();
        int                      cPartitions = service.getPartitionCount();
        PartitionSet             parts       = new PartitionSet(cPartitions);
        for (int iPartition = 0; iPartition < cPartitions; iPartition = iPartition+2)
            {
            parts.add(iPartition);
            }

        Filter filterPart = new PartitionedFilter(
            new LessEqualsFilter(StringToIntegerPersonAddressZipExtractor.INSTANCE, 5000), parts);

        out(" * * * * * * * Partitioned Filter Query * * * * * * * * * * * * * * (testByPartitions)");
        testReporter(cache, filterPart, QueryRecorder.RecordType.EXPLAIN, "explain-Part.ptn");
        testReporter(cache, filterPart, QueryRecorder.RecordType.TRACE, "trace-Part.ptn");

        cache.removeIndex(StringToIntegerPersonAddressZipExtractor.INSTANCE);
        }

    // ----- internal methods -----------------------------------------------

    /**
     * Return the name of the cache.
     *
     * @return the name of the cache
     */
    private static String getCacheName()
        {
        return "dist-q-reporter";
        }

    /**
     * Test SimpleQueryRecordReporter on a record of specified type,
     * produced for the query based on provided filter
     *
     * @param cache           cache object where query is run
     * @param filter          the filter that defines a query
     * @param type            the type of QueryRecord to generate, either EXPLAIN or TRACE
     * @param sFileName       name of file to compare the output to
     */
    private void testReporter(NamedCache cache, Filter filter, QueryRecorder.RecordType type, String sFileName)
            throws Exception
        {
        QueryRecorder agent = new QueryRecorder(type);
        SimpleQueryRecord record = (SimpleQueryRecord) cache.aggregate(filter, agent);

        String sOutput = SimpleQueryRecordReporter.report(record);

        assertFalse("Report is null", sOutput == null);
        out(sOutput);
        match(sOutput, sFileName);
        }

    private void match(String sOut, String sFileName) throws Exception
        {
        Scanner scanPattern = null;
        Scanner scanResult  = null;
        int     nLine       = 0;
        try
            {
            URL url = Resources.findFileOrResource(sFileName, getClass().getClassLoader());
            scanPattern = new Scanner(url.openStream());
            scanResult = new Scanner(sOut);

            while (scanPattern.hasNextLine())
                {
                String sPattern = scanPattern.nextLine().trim();
                nLine++;
                String sResult = scanResult.nextLine().trim();
                assertTrue("Line " + nLine + " did not match: expected :" + sPattern
                                + ", but was :" + sResult, sResult.matches(sPattern));
                }

            if (scanResult.hasNextLine())
                {
                fail("Output was expected to have " + nLine + " lines, but has more");
                }
            }
        catch (FileNotFoundException e)
            {
            fail("File " + sFileName + " not found");
            }
        catch (NoSuchElementException e)
            {
            fail("Line " + nLine + " was expected but not found in output");
            }
        finally
            {
            if (scanPattern != null)
                {
                scanPattern.close();
                }
            if (scanResult != null)
                {
                scanResult.close();
                }
            }

        }

    // ----- inner classes --------------------------------------------------

    /**
     * Person objects that populate the cache
     */
    public class Person implements Serializable
        {
        public Person()
            {
            }

        public Person(int key, String sFirst, String sLast, String sSsn, int nYear, Address address)
            {
            m_nKey       = key;
            m_sFirstName = sFirst;
            m_sLastName  = sLast;
            m_sSSN       = sSsn;
            m_nBirthYear = nYear;
            m_address    = address;
            }

        public int getKey()
            {
            return m_nKey;
            }

        public String getFirstName()
            {
            return m_sFirstName;
            }

        public String getLastName()
            {
            return m_sLastName;
            }

        public int getBirthYear()
            {
            return m_nBirthYear;
            }

        public String getSSN()
            {
            return m_sSSN;
            }

        public Address getAddress()
            {
            return m_address;
            }

        public void setKey(int key)
            {
            this.m_nKey = key;
            }

        public void setFirstName(String firstName)
            {
            this.m_sFirstName = firstName;
            }

        public void setLastName(String lastName)
            {
            this.m_sLastName = lastName;
            }

        public void setBirthYear(int birthYear)
            {
            this.m_nBirthYear = birthYear;
            }

        public void setSSN(String SSN)
            {
            this.m_sSSN = SSN;
            }

        public void setAddress(Address address)
            {
            this.m_address = address;
            }

        // ----- allow natural ordering -------------------------------------------
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }

            Person person = (Person) o;

            if (m_sSSN != null ? !m_sSSN.equals(person.m_sSSN) : person.m_sSSN != null)
                {
                return false;
                }

            return true;
            }

        public int hashCode()
            {
            return m_sSSN != null ? m_sSSN.hashCode() : 0;
            }

        public String toString()
            {
            return "Person {\nm_nKey=" + m_nKey + ", m_sSSN=" + m_sSSN + ", m_sFirstName=" +
                    m_sFirstName + ", m_sLastName=" + m_sLastName + ", m_nBirthYear=" +
                    m_nBirthYear + "\n    m_address=" + m_address + "\n}";
            }

        // Serializable interface
        private synchronized void writeObject(ObjectOutputStream out)
                throws IOException
            {
            out.writeInt(m_nKey);
            out.writeInt(m_nBirthYear);
            out.writeUTF(m_sFirstName);
            out.writeUTF(m_sLastName);
            out.writeUTF(m_sSSN);
            out.writeUTF(m_address.getStreet());
            out.writeUTF(m_address.getCity());
            out.writeUTF(m_address.getState());
            out.writeUTF(m_address.getZip());
            }

        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            m_nKey = in.readInt();
            m_nBirthYear = in.readInt();
            m_sFirstName = in.readUTF();
            m_sLastName = in.readUTF();
            m_sSSN = in.readUTF();
            m_address = new Address(in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF());
            }

        // ----- calculated -----------------------------------------------------

        public int getAge()
            {
            return getAge(Calendar.getInstance().get(Calendar.YEAR));
            }

        public int getAge(int nYear)
            {
            return nYear - getBirthYear();
            }

        public String getFullName()
            {
            return getFirstName() + " " + getLastName();
            }

        int     m_nKey;
        int     m_nBirthYear;
        String  m_sFirstName;
        String  m_sLastName;
        String  m_sSSN;
        Address m_address;

        }
    }

