/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package processor;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilder;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.AlwaysFilter;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2016.03.30
 */
@RunWith(Parameterized.class)
public class PriorityTaskProcessorTest
    {
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters()
        {
        List<Object[]> list = new ArrayList<>();

        list.add(new Object[]{"ClusterMember Default", SessionBuilders.storageDisabledMember(), PriorityTask.TIMEOUT_DEFAULT, true});
        list.add(new Object[]{"ClusterMember None", SessionBuilders.storageDisabledMember(), PriorityTask.TIMEOUT_NONE, false});
        list.add(new Object[]{"Extend Default", SessionBuilders.extendClient("timeout-client-cache-config.xml"), PriorityTask.TIMEOUT_DEFAULT, true});
        list.add(new Object[]{"Extend None", SessionBuilders.extendClient("timeout-client-cache-config.xml"), PriorityTask.TIMEOUT_NONE, false});

        return list;
        }

    public PriorityTaskProcessorTest(String name, SessionBuilder sessionBuilder, long timeout, boolean fShouldTimeout)
        {
        m_sessionBuilder = sessionBuilder;
        m_timeout        = timeout;
        m_fShouldTimeout = fShouldTimeout;
        }


    @Test
    public void shouldRunPriorityProcessorWithInvoke() throws Exception
        {
        ConfigurableCacheFactory ccf    = s_cluster.createSession(m_sessionBuilder);
        NamedCache               cache  = ccf.ensureCache("dist-test", null);
        String                   sKey   = "Foo1";
        String                   sValue = "Bar";

        if (m_fShouldTimeout)
            {
            m_exception.expect(RequestTimeoutException.class);
            }

        String result = (String) cache.invoke(sKey, new PriorityProcessor(10000, m_timeout, sValue));

        assertThat(result, is(sValue));
        }


    @Test
    public void shouldRunPriorityProcessorWithInvokeAllKeys() throws Exception
        {
        ConfigurableCacheFactory ccf    = s_cluster.createSession(m_sessionBuilder);
        NamedCache               cache  = ccf.ensureCache("dist-test", null);
        String                   sKey   = "Foo2";
        String                   sValue = "Bar";
        Collection               keys   = Collections.singleton(sKey);

        if (m_fShouldTimeout)
            {
            m_exception.expect(RequestTimeoutException.class);
            }

        Map<String,String> map = cache.invokeAll(keys, new PriorityProcessor(10000, m_timeout, sValue));

        assertThat(map.size(), is(1));
        assertThat(map.get(sKey), is(sValue));
        }


    @Test
    public void shouldRunPriorityProcessorWithInvokeAllFilter() throws Exception
        {
        ConfigurableCacheFactory ccf    = s_cluster.createSession(m_sessionBuilder);
        NamedCache               cache  = ccf.ensureCache("dist-test", null);
        Filter                   filter = AlwaysFilter.INSTANCE;
        String                   sKey   = "Foo3";
        String                   sValue = "Bar";

        s_cluster.getCluster().getCache("dist-test").put(sKey, sValue);

        if (m_fShouldTimeout)
            {
            m_exception.expect(RequestTimeoutException.class);
            }

        Map<String,String> map = cache.invokeAll(filter, new PriorityProcessor(10000, m_timeout, sValue));

        assertThat(map.size(), is(1));
        assertThat(map.get(sKey), is(sValue));
        }


    @ClassRule
    public static CoherenceClusterResource s_cluster = new CoherenceClusterResource()
            .include(2, LocalStorage.enabled())
            .with(Logging.at(9),
                  CacheConfig.of("timeout-server-cache-config.xml"),
                  SystemProperty.of("coherence.localhost", "127.0.0.1"),
                  SystemProperty.of("coherence.extend.enabled", true),
                  SystemProperty.of("coherence.extend.port", LocalPlatform.get().getAvailablePorts().next()),
                  SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY,
                                    Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)));

    private SessionBuilder m_sessionBuilder;

    private long m_timeout;

    private boolean m_fShouldTimeout;

    @Rule
    public ExpectedException m_exception = ExpectedException.none();
    }
