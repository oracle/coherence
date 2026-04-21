/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package filter;


import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.LimitFilter;

import data.Person;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;
import static com.tangosol.util.Base.out;
import static org.junit.Assert.assertTrue;

/**
 * @author lh 2023.09.28
 */
public class ExtractorComparatorFilterTests
    {
    @BeforeClass
    public static void setup() throws Exception
        {
        ConfigurableCacheFactory ccf = s_clusterRunner.createSession(SessionBuilders.storageDisabledMember());
        s_cache = ccf.ensureTypedCache("dist-cache", null, withoutTypeChecking());
        }

    @Test
    public void testQueryMultiExtractors()
        {
        final int  CACHE_SIZE = 1000;
        NamedCache cache      = s_cache;

        cache.clear();
        Person.fillRandom(cache, CACHE_SIZE);
        out("Populated Cache with " + cache.size() + " persons.");

        int                   pageSize       = 100;
        MultiExtractor        valueExtractor = new MultiExtractor("getLastName,getId");
        MyExtractorComparator comparator     = new MyExtractorComparator(valueExtractor);
        LimitFilter           pagedFilter    = new LimitFilter(AlwaysFilter.INSTANCE, pageSize);

        Base.sleep(1000);

        Collection data;
        Set        allData    = new HashSet();
        int        iterations = 0;
        int        totalPages = CACHE_SIZE/pageSize;
        Person     pageFirst  = null;
        Person     pageSecond = null;
        while (iterations < totalPages)
            {
            data = cache.values(pagedFilter, comparator);

            if (data.isEmpty())
                {
                break;
                }

            int    i = 0;
            int    nFirst     = new Random().nextInt(pageSize / 2);
            int    nSecond    = new Random().nextInt(pageSize/2, pageSize - 1);
            Person first      = null;
            Person second     = null;

            Iterator<Person> iterator = data.iterator();
            while (iterator.hasNext())
                {
                Person p = iterator.next();
                Object o = comparator.getExtractor().extract(p);
                if (i == nFirst)
                    {
                    first = p;
                    }
                if (i == nSecond)
                    {
                    second = p;
                    }
                i++;

                if (iterations == 1)
                    {
                    pageFirst = second;
                    }
                else if (iterations == 5)
                    {
                    pageSecond = first;
                    }
                }
            assertTrue((comparator.compare(second, first) >= 0));

            allData.addAll(data);
            iterations++;
            pagedFilter.nextPage();
            }
        assertTrue((comparator.compare(pageSecond, pageFirst) > 0));
        assertTrue(cache.size() == allData.size());
        }

    static
        {
        System.setProperty("coherence.pof.enabled", "true");
        }

    @ClassRule
    public static final CoherenceClusterResource s_clusterRunner = new CoherenceClusterResource()
            .include(3, LocalStorage.enabled())
            .with(ClusterName.of(ExtractorComparatorFilterTests.class.getSimpleName() + "Cluster"),
                  Pof.enabled(),
                  Pof.config("filter-pof-config.xml"),
                  SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY,
                                    Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)));
    public static NamedCache<Integer, DomainObject> s_cache;
    }
