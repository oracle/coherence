/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package lambda;

import com.oracle.bedrock.junit.CoherenceClusterOrchestration;
import com.oracle.bedrock.junit.SessionBuilder;
import com.oracle.bedrock.junit.SessionBuilders;

import com.tangosol.net.ConfigurableCacheFactory;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.LongStreamTest;

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
        extends LongStreamTest
    {
    @ClassRule
    public static CoherenceClusterOrchestration CLUSTER = new LambdaTestCluster();

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

    private SessionBuilder m_bldrSession;

    private String m_sSerializer;

    public LongStreamTests(SessionBuilder bldrSession, String sSerializer, boolean fParallel)
        {
        super(fParallel);
        m_bldrSession = bldrSession;
        m_sSerializer = sSerializer;
        }

    protected InvocableMap<String, Person> getPeopleMap()
        {
        ConfigurableCacheFactory client = CLUSTER.getSessionFor(m_bldrSession);

        InvocableMap<String, Person> map = client.ensureTypedCache(
                m_sSerializer,
                null,
                withoutTypeChecking());
        map.clear();
        populateMap(map);
        return map;
        }
    }
