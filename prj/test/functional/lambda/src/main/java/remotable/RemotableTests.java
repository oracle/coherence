/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package remotable;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilder;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.coherence.testing.tests.invoke.AbstractRemotableTest;
import com.tangosol.coherence.config.Config;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.TypeAssertion;

import data.Trade;

import java.util.Arrays;
import java.util.Collection;

import lambda.framework.LambdaTestCluster;

import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author as  2015.08.28
 */
@RunWith(Parameterized.class)
public class RemotableTests
        extends AbstractRemotableTest
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

    private final SessionBuilder m_bldrSession;

    private final String m_sSerializer;

    public RemotableTests(SessionBuilder bldrSession, String sSerializer)
        {
        m_bldrSession = bldrSession;
        m_sSerializer = sSerializer;
        }

    @Override
    protected NamedCache<Integer, Trade> getCache()
        {
        ConfigurableCacheFactory cacheFactory = cluster.createSession(m_bldrSession);

        NamedCache<Integer, Trade> cache = cacheFactory.ensureTypedCache(m_sSerializer, null,
                TypeAssertion.withTypes(Integer.class, Trade.class));
        cache.clear();
        return cache;
        }

    @Test
    @Override
    public void testRemotableClass()
        {
        String sLambdas = Config.getProperty("coherence.lambdas");
        String sMode    = Config.getProperty("coherence.mode", "dev");

        boolean fDynamic = sLambdas == null
                           ? !sMode.equalsIgnoreCase("prod")
                           : sLambdas.equalsIgnoreCase("dynamic");

        Assume.assumeTrue("skip test using an instance of AbstractRemotable if dynamic lambdas disabled", fDynamic);
        super.testRemotableClass();
        }
    }
