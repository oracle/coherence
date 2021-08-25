/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package lambda;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilder;
import com.oracle.bedrock.junit.SessionBuilders;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.QueryMap;
import com.tangosol.util.QueryMapTest;

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
public class QueryMapTests
        extends QueryMapTest
    {
    @ClassRule
    public static CoherenceClusterResource cluster = new LambdaTestCluster();

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

    private String         m_sSerializer;

    public QueryMapTests(SessionBuilder bldrSession, String sSerializer)
        {
        m_bldrSession = bldrSession;
        m_sSerializer = sSerializer;
        }

    protected QueryMap getMap()
        {
        ConfigurableCacheFactory cacheFactory = cluster.createSession(m_bldrSession);

        NamedCache cache = cacheFactory.ensureCache(m_sSerializer, null);
        cache.clear();
        return cache;
        }
    }
