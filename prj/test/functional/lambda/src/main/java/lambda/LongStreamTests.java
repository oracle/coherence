/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package lambda;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilder;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.coherence.testing.tests.streams.AbstractLongStreamTest;
import com.tangosol.net.ConfigurableCacheFactory;

import com.tangosol.util.InvocableMap;

import data.pof.Person;

import java.util.Arrays;
import java.util.Collection;

import lambda.framework.LambdaTestCluster;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * @author Aleksandar Seovic  2014.06.13
 */
@RunWith(Parameterized.class)
public class LongStreamTests
        extends AbstractLongStreamTest
    {
    @ClassRule
    public static CoherenceClusterResource CLUSTER = new LambdaTestCluster();

    public static SessionBuilder MEMBER = SessionBuilders.storageDisabledMember();

    public static SessionBuilder EXTEND = SessionBuilders.extendClient(
            "client-cache-config.xml");

    @Parameterized.Parameters(name = "client={0}, serializer={1}, parallel={2}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]{
                {MEMBER, "java", true},
                {MEMBER, "java", false},
                {MEMBER, "pof", true},
                {MEMBER, "pof", false},
                {EXTEND, "java", true},
                {EXTEND, "java", false},
                {EXTEND, "pof", true},
                {EXTEND, "pof", false},
            });
        }

    private final SessionBuilder m_bldrSession;

    private final String m_sSerializer;

    public LongStreamTests(SessionBuilder bldrSession, String sSerializer, boolean fParallel)
        {
        super(fParallel);
        m_bldrSession = bldrSession;
        m_sSerializer = sSerializer;
        }

    @Override
    protected InvocableMap<String, Person> getPeopleMap()
        {
        ConfigurableCacheFactory client = CLUSTER.createSession(m_bldrSession);

        InvocableMap<String, Person> map = client.ensureTypedCache(
                m_sSerializer,
                null,
                withoutTypeChecking());
        map.clear();
        populateMap(map);
        return map;
        }
    }
