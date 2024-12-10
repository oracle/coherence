/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package dslquery;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.coherence.io.json.JsonObject;

import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.dslquery.ExtractorBuilder;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import data.pof.Address;
import data.pof.Person;
import data.pof.PersonRecord;
import data.pof.PhoneNumber;
import data.pof.PortablePerson;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.junit.experimental.theories.DataPoints;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeSet;

import static com.tangosol.net.cache.TypeAssertion.withTypes;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import static org.junit.Assert.assertThat;

/**
 * @author jk 2014.07.04
 */
@RunWith(Parameterized.class)
public class QueryPlusQueryTests
    {

    // ----- Parameterized Setup --------------------------------------------

    /**
     * Create the parameters that will be used to run different combinations
     * of this test fixture.
     *
     * The combinations will be all of the different types of Coherence connection
     * combined with all the different NamedQueue implementations.
     *
     * @return the different test combinations to run
     */
    @Parameterized.Parameters(name = "Query={0} Mapper={1}")
    public static Collection<Object[]> getTestParameters() throws Exception
        {
        Query[]             queries    = loadQueryParams();
        ExtractorBuilder[]  builders   = loadMappers();
        ArrayList<Object[]> parameters = new ArrayList<>();

        for (Query query : queries)
            {
            for (ExtractorBuilder builder : builders)
                {
                parameters.add(new Object[]{query, builder});
                }
            }

        return parameters;
        }

    @Before
    public void startClusterAndPopulateData()
            throws Exception
        {
        ConfigurableCacheFactory ccf = m_clusterRunner.createSession(SessionBuilders.storageDisabledMember());

        m_queryPlusRunner.setConfigurableCacheFactory(ccf);

        m_simpleCache = ccf.ensureTypedCache("dist-simple", null, withTypes(String.class, String.class));
        m_simpleCache.clear();
        for (int i=0; i<10; i++)
            {
            m_simpleCache.put("Key-" + i, "Value-" + i);
            }

        m_peopleCache = ccf.ensureTypedCache("dist-people", null, withTypes(String.class, PortablePerson.class));
        m_peopleCache.clear();

        m_phoneCache = ccf.ensureTypedCache("dist-phones", null, withTypes(PhoneNumber.class, String.class));
        m_phoneCache.clear();


        for (int i=0; i<10; i++)
            {
            PortablePerson person  = new PortablePerson("Person-" + i, new Date());
            String         city    = (i % 2 == 0) ? "London" : "Boston";
            Address address = new Address(i + " Any Street", city, null, String.valueOf(99990 + i));
            int            age     = 20 + (i % 5);
            person.setAge(age);
            person.setAddress(address);

            PortablePerson spouse  = new PortablePerson("Spouse-" + i, new Date());
            spouse.setAge(age - 1);
            spouse.setAddress(address);

            person.setSpouse(spouse);

            PortablePerson child1  = new PortablePerson("Child-" + i + ".1", new Date());
            child1.setAge(5 + i);
            child1.setAddress(address);
            PortablePerson child2  = new PortablePerson("Child-" + i + ".2", new Date());
            child2.setAge(10 + i);
            child2.setAddress(address);

            person.setChildren(new Person[]{child1, child2});

            int countryCode = (i % 2 == 0) ? 44 : 1;
            person.addPhoneNumber("Work",  new PhoneNumber(countryCode, "207562567" + i));
            person.addPhoneNumber("Home",  new PhoneNumber(countryCode, "207511687" + i));

            m_phoneCache.put(new PhoneNumber(countryCode, "207562567" + i), person.getName());

            m_peopleCache.put(person.getName(), person);
            }

        m_personJsonValueCache = ccf.ensureCache("dist-person-jsonvalue", null, withTypes(String.class, JsonObject.class));
        m_personJsonValueCache.clear();
        for (int i=0; i<10; i++)
            {
            JsonObject value = new JsonObject();

            value.put("name", "Person-" + i );
            value.put("age", 20 + (i % 5));

            String  city    = (i % 2 == 0) ? "London" : "Boston";
            Address address = new Address(i + " Any Street", city, null, String.valueOf(99990 + i));

            value.put("city", city);
            value.put("address", address);
            m_personJsonValueCache.put((String) value.get("name"), value);
            }

        m_personRecordCache = ccf.ensureCache("dist-person-record", null, withTypes(String.class, PersonRecord.class));
        m_personRecordCache.clear();
        for (int i=0; i<10; i++)
            {
            String sName = "Person-" + i;
            String  city    = (i % 2 == 0) ? "London" : "Boston";
            Address address = new Address(i + " Any Street", city, null, String.valueOf(99990 + i));

            m_personRecordCache.put(sName, new PersonRecord(sName, 20 + (i % 5), city, address));
            }

        m_queryPlusRunner.setExtendedLanguage(true);
        m_queryPlusRunner.setSanityCheck(true);
        m_queryPlusRunner.setTrace(false);
        }

    @After
    public void tearDown()
        {
        m_queryPlusRunner.getLanguage().setExtractorBuilder(null);
        }

    /**
     * Construct a QueryPlusQueryTests to test a specific
     * query and {@link ExtractorBuilder} combination
     *
     * @param query   the CohQL query to test
     * @param builder  the ExtractorBuilder to use
     */
    public QueryPlusQueryTests(Query query, ExtractorBuilder builder)
        {
        m_query  = query;
        m_mapper = builder;
        }

    /**
     * Execute the specific query with the specific query mapper.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void shouldExecuteQueries() throws Exception
        {
        String                 queryString = m_query.getQuery();
        String[]               queries     = queryString.split("\\n");
        LinkedList<String>     out         = null;

        System.out.println("shouldExecuteQueries(query=" + m_query + " mapper=" + m_mapper + ")");

        m_queryPlusRunner.getLanguage().setExtractorBuilder(m_mapper);

        for (String q : queries)
            {
            out = m_queryPlusRunner.runCommand(q);
            }

        String   expectedResult = m_query.getExpectedResult();
        String[] resultLines    = expectedResult.split("\\n");
        for (int i=0; i<resultLines.length; i++)
            {
            resultLines[i] = resultLines[i].trim();
            }

        assertResults(out, resultLines);
        }


    /**
     * This method asserts that the lines List contains
     * the expected data.
     *
     * @param lines    the actual output from QueryPlus
     * @param expected the expected output from QueryPlus
     */
    public void assertResults(LinkedList<String> lines, String... expected)
        {
        assertThat("Expected some results to be printed but just found\n" + lines, lines.size(), is(greaterThanOrEqualTo(3)));
        assertThat(lines.pollFirst(), is("Results"));
        assertThat(lines.pollLast(), is(QueryPlusRunner.PROMPT_LINE));
        assertThat(lines.pollLast(), is(""));
        assertThat(lines, containsInAnyOrder(expected));
        }

    /**
     * Create the QueryMapper instances that should be combined
     * with the queries to build the different test combinations.
     *
     * @return the QueryMapper instances that should be combined
     *         with the queries to build the different test combinations
     */
    public static ExtractorBuilder[] loadMappers()
        {
        DummyPofExtractorBuilder queryMapper = new DummyPofExtractorBuilder();

        queryMapper.addCacheNameToTypeMapping("dist-people", null, "Person");
        queryMapper.addCacheNameToTypeMapping("dist-phones", "PhoneNumber", null);
        queryMapper.addCacheNameToTypeMapping("dist-person-jsonvalue", "String", "com.oracle.coherence.io.json.JsonObject");
        queryMapper.addCacheNameToTypeMapping("dist-person-record", "String", "data.pof.PersonRecord");

        queryMapper.addAttributeMapping("Person", "name", null, PortablePerson.NAME);
        queryMapper.addAttributeMapping("Person", "dateOfBirth", null, PortablePerson.DOB);
        queryMapper.addAttributeMapping("Person", "age", null, PortablePerson.AGE);
        queryMapper.addAttributeMapping("Person", "spouse", "Person", PortablePerson.SPOUSE);
        queryMapper.addAttributeMapping("Person", "children", null, PortablePerson.CHILDREN);
        queryMapper.addAttributeMapping("Person", "address", "Address", PortablePerson.ADDRESS);
        queryMapper.addAttributeMapping("Person", "phoneNumbers", null, PortablePerson.PHONE);

        queryMapper.addAttributeMapping("Address", "street", null, Address.STREET);
        queryMapper.addAttributeMapping("Address", "city", null, Address.CITY);
        queryMapper.addAttributeMapping("Address", "state", null, Address.STATE);
        queryMapper.addAttributeMapping("Address", "zip", null, Address.ZIP);

        queryMapper.addAttributeMapping("JsonObject", "name", null, 0);
        queryMapper.addAttributeMapping("JsonObject", "age", null, 1);
        queryMapper.addAttributeMapping("JsonObject", "city", null, 2);
        queryMapper.addAttributeMapping("JsonObject", "address", null, 3);

        queryMapper.addAttributeMapping("PersonRecord", "name", null, PersonRecord.NAME);
        queryMapper.addAttributeMapping("PersonRecord", "age", null, PersonRecord.AGE);
        queryMapper.addAttributeMapping("PersonRecord", "city", null, PersonRecord.CITY);
        queryMapper.addAttributeMapping("PersonRecord", "address", null, PersonRecord.ADDRESS);

        queryMapper.addAttributeMapping("PhoneNumber", "countryCode", null, 0);
        queryMapper.addAttributeMapping("PhoneNumber", "phoneNumber", null, 1);

        return new ExtractorBuilder[]{new DummyPofExtractorBuilder(), queryMapper};
        }

    /**
     * Read the dslquery-tests.properties file and convert the properties
     * into {@link QueryPlusQueryTests.Query} instances that will then
     * be passed to the {@link QueryPlusQueryTests} constructor by JUnit.
     *
     * @return an array of {@link Query} instances
     *
     * @throws Exception if there is an error
     */
    @DataPoints
    public static Query[] loadQueryParams() throws Exception
        {
        InputStream stream          = QueryPlusQueryTests.class.getResourceAsStream("/dslquery-tests.properties");
        Properties  queryProperties = new Properties();
        queryProperties.load(stream);

        TreeSet<Query> queries = new TreeSet<>();
        for (String key : queryProperties.stringPropertyNames())
            {
            if (key.startsWith("query."))
                {
                String query  = queryProperties.getProperty(key);
                String suffix = key.substring(6);
                String result = queryProperties.getProperty("result." + suffix);
                if (query != null)
                    {
                    queries.add(new Query(key, query, result));
                    }
                }
            }

        return queries.toArray(new Query[queries.size()]);
        }

    // ----- inner classes --------------------------------------------------

    /**
     * Inner class used to hold details of tests QueryPlus
     * commands read from the dslquery-tests.properties file.
     */
    private static class Query implements Comparable<Query>
        {
        private Query(String key, String query, String result)
            {
            m_key       = key;
            m_query     = query;
            m_result    = result;

            String[] parts = key.split("\\.");
            m_keys  = new int[parts.length -1];

            for (int i=1; i<parts.length; i++)
                {
                m_keys[i-1] = Integer.parseInt(parts[i]);
                }
            }

        public String getKey()
            {
            return m_key;
            }

        public String getQuery()
            {
            return m_query;
            }

        public String getExpectedResult()
            {
            return m_result;
            }

        public String toString()
            {
            StringBuilder s = new StringBuilder("query");
            for (int k : m_keys)
                {
                s.append('.').append(k);
                }

            s.append(' ').append(m_query);

            return s.toString();
            }

        @Override
        public int compareTo(Query o)
            {
            for (int i=0; i<m_keys.length; i++)
                {
                if (i >= o.m_keys.length)
                    {
                    return 1;
                    }

                int result = m_keys[i] - o.m_keys[i];
                if (result != 0)
                    {
                    return result;
                    }
                }

            return (m_keys.length == o.m_keys.length) ? 0 : -1;
            }

        @Override
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

            Query queryKey = (Query) o;

            return Arrays.equals(m_keys, queryKey.m_keys);
            }

        @Override
        public int hashCode()
            {
            return m_keys != null ? Arrays.hashCode(m_keys) : 0;
            }

        private String m_key;
        private int[]  m_keys;
        private String m_query;
        private String m_result;
        }


    // ----- data members ---------------------------------------------------

    public static final String CACHE_CONFIG       = "cohql-cache-config.xml";
    public static final String POF_CONFIG         = "dslquery-pof-config.xml";

    /**
     * The Query to execute in the shouldExecuteQueries test
     */
    protected Query m_query;

    /**
     * The ExtractorBuilder to use in the shouldExecuteQueries test
     */
    protected ExtractorBuilder m_mapper;

    // COH-23847 - hack to set the required system property outside the bedrock
    static
        {
        System.setProperty("coherence.pof.enabled", "true");
        }

    /**
     * 
     * JUnit rule to start a Coherence cluster of two storage nodes and a proxy node
     */
    @ClassRule
    public static CoherenceClusterResource m_clusterRunner = new CoherenceClusterResource()
            .include(2, LocalStorage.enabled())
            .with(CacheConfig.of(CACHE_CONFIG),
                         Pof.config(POF_CONFIG),
                         Pof.enabled(),
                         SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)));

    /**
     * JUnit rule to start a QueryPlus session
     */
    @ClassRule
    public static QueryPlusRunner m_queryPlusRunner = new QueryPlusRunner();

    /**
     * Test cache containing data.pof.PortablePerson instances
     */
    protected NamedCache<String,PortablePerson> m_peopleCache;

    /**
     * Test cache containing data.pof.PhoneNumber instances as keys
     */
    protected NamedCache<PhoneNumber,String> m_phoneCache;

    /**
     * Test cache containing String key/value pairs
     */
    protected NamedCache<String,String> m_simpleCache;


    /** Test cache containing String key/JsonObject value. */
    protected NamedCache<String, JsonObject> m_personJsonValueCache;

    /** Test cache containing String key/PersonRecord value. */
    protected NamedCache<String, PersonRecord> m_personRecordCache;
    }
