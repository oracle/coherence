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
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapTest;

import java.util.Arrays;
import java.util.Collection;

import lambda.framework.LambdaTestCluster;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Aleksandar Seovic  2014.06.13
 */
@RunWith(Parameterized.class)
public class InvocableMapTests
        extends InvocableMapTest
    {
    @ClassRule
    public static CoherenceClusterOrchestration orchestration = new LambdaTestCluster();

    public static SessionBuilder MEMBER = SessionBuilders.storageDisabledMember();

    public static SessionBuilder EXTEND = SessionBuilders.extendClient(
            "client-cache-config.xml");

    @Parameterized.Parameters(name = "client={0}, serializer={1}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]{
                {MEMBER, "pof"}, {MEMBER, "java"},
                {EXTEND, "pof"}, {EXTEND, "java"}
            });
        }

    private SessionBuilder m_bldrSession;

    private String m_sSerializer;

    public InvocableMapTests(SessionBuilder bldrSession, String sSerializer)
        {
        m_bldrSession = bldrSession;
        m_sSerializer = sSerializer;
        }

    protected InvocableMap getInvocableMap()
        {
        ConfigurableCacheFactory cacheFactory = orchestration.getSessionFor(
                m_bldrSession);

        NamedCache cache = cacheFactory.ensureCache(m_sSerializer, null);
        cache.clear();
        return cache;
        }
    }
