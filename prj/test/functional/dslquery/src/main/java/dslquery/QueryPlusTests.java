/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package dslquery;

import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Pof;

import com.oracle.coherence.io.json.JsonObject;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import data.pof.Address;
import data.pof.Person;
import data.pof.PhoneNumber;
import data.pof.PortablePerson;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.oracle.coherence.testing.processors.HasIndexProcessor;

import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.hasItem;

import static com.oracle.coherence.testing.util.CollectionUtils.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * QueryPlus functional tests
 *
 * @author jk 2014.03.01
 */
@SuppressWarnings({"unchecked", "rawtypes", "resource"})
public class QueryPlusTests
    {
    @Before
    public void startClusterAndPopulateData()
            throws Exception
        {
        m_ccf = m_clusterRunner.createSession(SessionBuilders.storageDisabledMember());

        m_queryPlusRunner.setConfigurableCacheFactory(m_ccf);

        m_simpleCache = m_ccf.ensureCache("dist-simple", null);
        m_simpleCache.clear();
        for (int i=0; i<10; i++)
            {
            m_simpleCache.put("Key-" + i, "Value-" + i);
            }

        m_peopleCache = m_ccf.ensureCache("dist-people", null);
        m_peopleCache.clear();
        for (int i=0; i<10; i++)
            {
            PortablePerson person  = new PortablePerson("Person-" + i, new Date());
            String         city    = (i % 2 == 0) ? "London" : "Boston";
            Address        address = new Address(i + " Any Street", city, null, String.valueOf(99990 + i));
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

            m_peopleCache.put(person.getName(), person);
            }

        m_queryPlusRunner.setExtendedLanguage(true);
        m_queryPlusRunner.setSanityCheck(true);
        m_queryPlusRunner.setTrace(false);
        }

    @Test
    public void shouldEnableTrace()
            throws Exception
        {
        m_queryPlusRunner.setTrace(false);
        m_queryPlusRunner.runCommand("trace on");
        assertThat(m_queryPlusRunner.isTraceEnabled(), is(true));
        }

    @Test
    public void shouldDisableTrace()
            throws Exception
        {
        m_queryPlusRunner.setTrace(true);
        m_queryPlusRunner.runCommand("trace off");
        assertThat(m_queryPlusRunner.isTraceEnabled(), is(false));
        }

    @Test
    public void shouldEnableSanityCheck_1()
            throws Exception
        {
        m_queryPlusRunner.setTrace(false);
        m_queryPlusRunner.runCommand("sanity check on");
        assertThat(m_queryPlusRunner.isSanityCheckEnabled(), is(true));
        }

    @Test
    public void shouldEnableSanityCheck_2()
            throws Exception
        {
        m_queryPlusRunner.setTrace(false);
        m_queryPlusRunner.runCommand("sanity on");
        assertThat(m_queryPlusRunner.isSanityCheckEnabled(), is(true));
        }

    @Test
    public void shouldDisableSanityCheck_1()
            throws Exception
        {
        m_queryPlusRunner.setTrace(true);
        m_queryPlusRunner.runCommand("sanity check off");
        assertThat(m_queryPlusRunner.isSanityCheckEnabled(), is(false));
        }

    @Test
    public void shouldDisableSanityCheck_2()
            throws Exception
        {
        m_queryPlusRunner.setTrace(true);
        m_queryPlusRunner.runCommand("sanity off");
        assertThat(m_queryPlusRunner.isSanityCheckEnabled(), is(false));
        }

    @Test
    public void shouldEnableExtendedLanguage_1()
            throws Exception
        {
        m_queryPlusRunner.setTrace(false);
        m_queryPlusRunner.runCommand("extended language on");
        assertThat(m_queryPlusRunner.isExtendedLanguageEnabled(), is(true));
        }

    @Test
    public void shouldEnableExtendedLanguage_2()
            throws Exception
        {
        m_queryPlusRunner.setTrace(false);
        m_queryPlusRunner.runCommand("extended language on");
        assertThat(m_queryPlusRunner.isExtendedLanguageEnabled(), is(true));
        }

    @Test
    public void shouldDisableExtendedLanguage_1()
            throws Exception
        {
        m_queryPlusRunner.setTrace(true);
        m_queryPlusRunner.runCommand("extended language off");
        assertThat(m_queryPlusRunner.isExtendedLanguageEnabled(), is(false));
        }

    @Test
    public void shouldDisableExtendedLanguage_2()
            throws Exception
        {
        m_queryPlusRunner.setTrace(true);
        m_queryPlusRunner.runCommand("extended off");
        assertThat(m_queryPlusRunner.isExtendedLanguageEnabled(), is(false));
        }

    @Test
    public void shouldRunCommands()
            throws Exception
        {
        List<String> out = m_queryPlusRunner.runCommand("commands");
        System.out.println(out);
        }

    @Test
    public void shouldRunHelp()
            throws Exception
        {
        List<String> out = m_queryPlusRunner.runCommand("help");
        System.out.println(out);
        }

    @Test
    public void shouldRunCreateCache() throws Exception
        {
        List<String> out = m_queryPlusRunner.runCommand("create cache 'dist-shouldCreateCache'");
        assertThat(out.get(0), is(""));
        assertThat(out.get(1), is(EMPTY_COHQL_PROMPT));
        CacheService cacheService = (CacheService) m_ccf.ensureService("DistributedCache");
        List<String> cacheNames   = asList(cacheService.getCacheNames());
        assertThat(cacheNames, hasItem("dist-shouldCreateCache"));
        }

    @Test
    public void shouldRunEnsureCache() throws Exception
        {
        List<String> out = m_queryPlusRunner.runCommand("ensure cache 'dist-shouldEnsureCache'");
        assertThat(out.get(0), is(""));
        assertThat(out.get(1), is(EMPTY_COHQL_PROMPT));
        CacheService cacheService = (CacheService) m_ccf.ensureService("DistributedCache");
        List<String> cacheNames   = asList(cacheService.getCacheNames());
        assertThat(cacheNames, hasItem("dist-shouldEnsureCache"));
        }

    @Test
    public void shouldRunInsertWithJSON() throws Exception
        {
        List<String> out = m_queryPlusRunner.runCommand("ensure cache 'dist-json-test'");
        assertThat(out.get(0), is(""));
        assertThat(out.get(1), is(EMPTY_COHQL_PROMPT));

        out = m_queryPlusRunner.runCommand("insert into 'dist-json-test' key 1 value new json('{\"foo\": 1}')");
        assertThat(out.get(0), is(""));
        assertThat(out.get(1), is(EMPTY_COHQL_PROMPT));
        CacheService cacheService = (CacheService) m_ccf.ensureService("DistributedCache");
        List<String> cacheNames   = asList(cacheService.getCacheNames());
        assertThat(cacheNames, hasItem("dist-json-test"));
        NamedCache   cache        = m_ccf.ensureCache("dist-json-test", null);
        Object oValue = cache.get(1);
        assertThat(oValue,  instanceOf(JsonObject.class));
        JsonObject json = (JsonObject) oValue;
        assertThat(json.get("foo"), is(1)); // Json Object will be returned as HashMap
        }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test(expected = IllegalStateException.class)
    public void shouldRunDestroyCache() throws Exception
        {
        String       cacheName    = "dist-shouldDestroyCache";
        NamedCache   cache        = m_ccf.ensureCache(cacheName, null);

        List<String> out = m_queryPlusRunner.runCommand("drop cache 'dist-shouldDestroyCache'");
        assertThat(out.get(0), is(""));
        assertThat(out.get(1), is(EMPTY_COHQL_PROMPT));

        // This should fail as the cache should have been released
        cache.size();
        }

    @Test
    public void shouldRunCreateIndex_1() throws Exception
        {
        String         cacheName = "dist-shouldCreateIndex_1";
        String         command   = "create index on 'dist-shouldCreateIndex_1' foo";
        ValueExtractor extractor = new ReflectionExtractor("getFoo");
        shouldCreateIndex(cacheName, command, extractor);
        }

    @Test
    public void shouldRunCreateIndex_2() throws Exception
        {
        String         cacheName = "shouldCreateIndex_2";
        String         command   = "create index 'shouldCreateIndex_2' foo";
        ValueExtractor extractor = new ReflectionExtractor("getFoo");
        shouldCreateIndex(cacheName, command, extractor);
        }

    @Test
    public void shouldRunEnsureIndex_1() throws Exception
        {
        String         cacheName = "dist-shouldEnsureIndex_1";
        String         command   = "ensure index on 'dist-shouldEnsureIndex_1' foo";
        ValueExtractor extractor = new ReflectionExtractor("getFoo");
        shouldCreateIndex(cacheName, command, extractor);
        }

    @Test
    public void shouldRunEnsureIndex_2() throws Exception
        {
        String         cacheName = "shouldEnsureIndex_2";
        String         command   = "ensure index 'shouldEnsureIndex_2' foo";
        ValueExtractor extractor = new ReflectionExtractor("getFoo");
        shouldCreateIndex(cacheName, command, extractor);
        }

    protected void shouldCreateIndex(String cacheName, String command, ValueExtractor extractor) throws Exception
        {
        NamedCache<Object,Object>        cache     = m_ccf.ensureCache(cacheName, null);
        List<String>                     out       = m_queryPlusRunner.runCommand(command);
        HasIndexProcessor<Object,Object> processor = new HasIndexProcessor(extractor, true, null);

        assertThat(out.get(0), is(""));
        assertThat(out.get(1), is(EMPTY_COHQL_PROMPT));
        Eventually.assertDeferred(() -> cache.invoke(null, processor), is(Boolean.TRUE));
        }

    @Test
    public void shouldRunDropIndex_1() throws Exception
        {
        String         cacheName = "dist-shouldDropIndex_1";
        String         command   = "drop index on 'dist-shouldDropIndex_1' foo";
        ValueExtractor extractor = new ReflectionExtractor("getFoo");
        shouldDropIndex(cacheName, command, extractor);
        }

    @Test
    public void shouldRunDropIndex_2() throws Exception
        {
        String         cacheName = "shouldDropIndex_2";
        String         command   = "drop index 'shouldDropIndex_2' foo";
        ValueExtractor extractor = new ReflectionExtractor("getFoo");
        shouldDropIndex(cacheName, command, extractor);
        }

    @Test
    public void shouldEchoStatementWithQueryPlusEchoOn() throws Exception
        {
        List<String> out = m_queryPlusEchoRunner.runCommand("ensure cache 'dist-people'");

        out = m_queryPlusEchoRunner.runCommand("ensure cache 'dist-simple'");

        // reported regression use multistatements on one line and always used END_OF_STATEMENT.
        String sCount1    = "select count() from 'dist-people';";
        String sCount2    = "select count() from 'dist-simple';";
        String sMultiLine = sCount1 + sCount2 + sCount1;

        // validate a multi-statment command line
        out = m_queryPlusEchoRunner.runCommand(sMultiLine);

        String[] arLine = new String[out.size()];
        out.toArray(arLine);

        // verify each CohQL statement is echo'ed to output along with expected result
        int cStatement = 0;
        int cPrompt    = 0;
        for (int i = 0; i < arLine.length; i++)
            {
            if (arLine[i].contains(sCount1) || arLine[i].contains(sCount2))
                {
                cStatement++;
                assertTrue("assert will not go beyond end of file", i + 2 < arLine.length);
                assertThat("asserting " + arLine[i] + "\"Results\"", arLine[i + 1], is("Results"));
                assertThat("asserting " + arLine[i] + " expected result of \"10\"", arLine[i + 2], is("10"));
                }
            else if (arLine[i].contains("CohQL>"))
                {
                cPrompt++;
                }
            }
        assertThat("assure number of statements in multistatement line", cStatement, is(3));
        assertThat("assure number of results matches number of commands", cPrompt, is(1));

        // validate a single statement command line
        out = m_queryPlusEchoRunner.runCommand(sCount1);
        cStatement = 0;
        cPrompt    = 0;
        arLine = new String[out.size()];
        out.toArray(arLine);
        assertTrue(arLine[0].contains(sCount1));
        assertTrue(arLine[1].contains("Results"));
        assertTrue(arLine[2].contains("10"));
        assertTrue(arLine[4].contains("CohQL>"));

        out = m_queryPlusEchoRunner.runCommand(sCount2);
        cStatement = 0;
        cPrompt    = 0;
        arLine = new String[out.size()];
        out.toArray(arLine);
        assertTrue(arLine[0].contains(sCount2));
        assertTrue(arLine[1].contains("Results"));
        assertTrue(arLine[2].contains("10"));
        assertTrue(arLine[4].contains("CohQL>"));
        }

    protected void shouldDropIndex(String cacheName, String command, ValueExtractor extractor) throws Exception
        {
        NamedCache<Object,Object>        cache     = m_ccf.ensureCache(cacheName, null);
        HasIndexProcessor<Object,Object> processor = new HasIndexProcessor(extractor, true, null);

        cache.addIndex(extractor, true, null);
        List<String> out = m_queryPlusRunner.runCommand(command);

        assertThat(out.get(0), is(""));
        assertThat(out.get(1), is(EMPTY_COHQL_PROMPT));
        Eventually.assertDeferred(() -> cache.invoke(null, processor), is(Boolean.FALSE));
        }

    // ----- data members ---------------------------------------------------

    public static final String CACHE_CONFIG       = "coherence-cache-config.xml";
    public static final String POF_CONFIG         = "dslquery-pof-config.xml";
    public static final String EMPTY_COHQL_PROMPT = "CohQL> ";

    static
        {
        System.setProperty("coherence.pof.enabled", "true");
        }

    /** JUnit rule to start a Coherence cluster of two storage nodes and a proxy node */
    @ClassRule
    public static CoherenceClusterResource m_clusterRunner   = new CoherenceClusterResource()
            .include(2, LocalStorage.enabled())    
            .with(CacheConfig.of(CACHE_CONFIG),
                  Pof.config(POF_CONFIG),
                  Pof.enabled(),
                  SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY,
                  Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)));

    /** JUnit rule to start a QueryPlus session */
    @ClassRule
    public static QueryPlusRunner m_queryPlusRunner = new QueryPlusRunner();

    /** JUnit rule to start a QueryPlus session with "-v" argument - enable echo of statement as read on standard output */
    @ClassRule
    public static QueryPlusRunner m_queryPlusEchoRunner = new QueryPlusRunner(new String[] {"-v"});

    /** Test cache containing data.pof.PortablePerson instances */
    protected NamedCache m_peopleCache;

    /** Test cache containing String key/value pairs */
    protected NamedCache m_simpleCache;

    protected ConfigurableCacheFactory m_ccf;
    }
