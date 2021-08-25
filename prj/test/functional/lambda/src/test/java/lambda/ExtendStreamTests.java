/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package lambda;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;

import com.tangosol.net.ConfigurableCacheFactory;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.StreamTest;

import data.pof.Person;

import lambda.framework.LambdaTestCluster;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * @author Aleksandar Seovic  2014.06.13
 */
@RunWith(Parameterized.class)
public class ExtendStreamTests
        extends StreamTest
    {
    @ClassRule
    public static CoherenceClusterResource cluster = new LambdaTestCluster();

    @Parameterized.Parameters(name = "serializer={0}, parallel={1}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]{
            {"pof", true},
            {"pof", false},
            {"java", true},
            {"java", false},
        });
    }

    private String m_sSerializer;

    public ExtendStreamTests(String sSerializer, boolean fParallel)
        {
        super(fParallel);
        m_sSerializer = sSerializer;
        }

    protected InvocableMap<String, Person> getPeopleMap()
        {
        ConfigurableCacheFactory cacheFactory = cluster.createSession(
            SessionBuilders.extendClient("client-cache-config.xml"));

        InvocableMap<String, Person> map = cacheFactory.ensureTypedCache(
                                            m_sSerializer,
                                            null,
                                            withoutTypeChecking());
        map.clear();
        populateMap(map);
        return map;
        }
    }
