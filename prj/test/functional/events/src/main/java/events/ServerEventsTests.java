/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.Processors;
import com.tangosol.util.processor.AbstractProcessor;

import data.Person;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Basic tests for server events
 */
public class ServerEventsTests
        extends AbstractFunctionalTest
    {
    @BeforeClass
    public static void _startup()
        {
        startCluster();
        }

    @Test
    public void testEntryProcessorInterceptor()
        {
        CoherenceClusterMember      member = startCacheServer("EntryProcessorEventsTests", "events", CFG_FILE);
        NamedCache<Integer, Person> cache  = member.getCache("dist-ep-events");
        NamedCache<String, Integer> events = member.getCache("results");

        // add some entries
        cache.put(1, new Person("1234", "Tom", "Bombadil", 1122, "2233", new String[] {}));
        cache.put(2, new Person("5678", "Samwise", "Gamgee", 1567, "4444", new String[] {}));
        cache.put(3, new Person("1357", "Frodo", "Baggins", 1565, "5555", new String[] {}));
        Eventually.assertDeferred(cache::size, Matchers.is(3));

        // invoke an entry processor on one entry
        cache.invoke(1, Processors.update(Person::setBirthYear, 1555));

        // ensure all entries invoked are accounted for
        Eventually.assertDeferred(() -> events.get("entryset-size"), Matchers.is(1));

        // reset
        events.clear();

        // invoke an entry processor across all entries
        cache.invokeAll(Processors.update(Person::setBirthYear, 1389));

        // ensure all entries invoked are accounted for
        Eventually.assertDeferred(() -> events.get("entryset-size"), Matchers.is(3));

        // reset
        events.clear();

        CustomProcessor processor = new CustomProcessor();
        try
            {
            cache.invokeAll(processor);
            }
        catch (Exception e)
            {
            //swallow
            }

        // ensure all entries invoked are accounted for
        Eventually.assertDeferred(() -> events.get("entryset-size"), Matchers.is(2));
        }

    public static class CustomProcessor
            extends AbstractProcessor
        {
        @Override
        public Object process(InvocableMap.Entry entry)
            {
            return null;
            }

        public Map processAll(Set setEntries)
            {
            for (Iterator iter = setEntries.iterator(); iter.hasNext();)
                {
                InvocableMap.Entry entry = (InvocableMap.Entry)iter.next();
                process(entry);
                if ((Integer) entry.getKey() == 3)
                    {
                    // do not remove
                    }
                else
                    {
                    iter.remove();
                    }
                }

            throw ensureRuntimeException(new Exception("on purpose"));
            }
        }

        public static final String CFG_FILE = "basic-server-cache-config.xml";
    }
