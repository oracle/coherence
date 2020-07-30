/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package aggregator;


import com.oracle.bedrock.junit.CoherenceClusterOrchestration;
import com.oracle.bedrock.junit.SessionBuilder;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.PriorityTask;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.util.Base;

import com.tangosol.util.aggregator.DoubleAverage;
import com.tangosol.util.aggregator.PriorityAggregator;

import com.tangosol.util.Filter;

import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.AlwaysFilter;

import org.junit.BeforeClass;
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
* A collection of functional tests for the various
* {@link PriorityAggregator} implementations that use the
* "Priority-test" cache.
*
*/
@RunWith(Parameterized.class)
public class PriorityAggregatorTests
    {
    // ----- constructors ---------------------------------------------------

    public PriorityAggregatorTests(String name, SessionBuilder sessionBuilder, long timeout, boolean fShouldTimeout)
        {
        m_sessionBuilder = sessionBuilder;
        m_timeout        = timeout;
        m_fShouldTimeout = fShouldTimeout;
        }

    // ----- test lifecycle -------------------------------------------------

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

    // ----- AbstractEntryAggregatorTests methods ---------------------------

    @BeforeClass
    public static void setup()
        {
        NamedCache cache = s_orchestration.getCluster().getCache("dist-test");

        cache.put("Foo1", 19.0d);
        cache.put("Foo2", 19.0d);
        }

    @Test
    public void shouldRunPriorityProcessorWithInvokeAllKeys() throws Exception
        {
        ConfigurableCacheFactory ccf    = s_orchestration.getSessionFor(m_sessionBuilder);
        NamedCache               cache  = ccf.ensureCache("dist-test", null);
        String                   sKey   = "Foo1";
        Collection               keys   = Collections.singleton(sKey);

        if (m_fShouldTimeout)
            {
            m_exception.expect(RequestTimeoutException.class);
            }

        PriorityAggregator priorityAggregator = new PriorityAggregator(new SlowAggregator());

        priorityAggregator.setExecutionTimeoutMillis(m_timeout);
        priorityAggregator.setRequestTimeoutMillis(m_timeout);
        priorityAggregator.setSchedulingPriority(PriorityTask.SCHEDULE_STANDARD);

        Double result = (Double) cache.aggregate(keys, priorityAggregator);

        assertThat(result, is(19.0d));
        }


    @Test
    public void shouldRunPriorityProcessorWithInvokeAllFilter() throws Exception
        {
        ConfigurableCacheFactory ccf    = s_orchestration.getSessionFor(m_sessionBuilder);
        NamedCache               cache  = ccf.ensureCache("dist-test", null);
        Filter                   filter = AlwaysFilter.INSTANCE;

        if (m_fShouldTimeout)
            {
            m_exception.expect(RequestTimeoutException.class);
            }

        PriorityAggregator priorityAggregator = new PriorityAggregator(new SlowAggregator());

        priorityAggregator.setExecutionTimeoutMillis(m_timeout);
        priorityAggregator.setRequestTimeoutMillis(m_timeout);
        priorityAggregator.setSchedulingPriority(PriorityTask.SCHEDULE_STANDARD);

        Double result = (Double) cache.aggregate(filter, priorityAggregator);

        assertThat(result, is(19.0d));
        }


    // ----- inner class: SlowAggregator ------------------------------------

    /**
    * Helper aggregator to test the request-timeout.
    */
    public static class SlowAggregator extends DoubleAverage
        {
        public SlowAggregator()
            {
            super(IdentityExtractor.INSTANCE);
            }

        @Override
        protected Object finalizeResult(boolean fFinal)
            {
            if (!fFinal)
                {
                Base.sleep(10000L);
                }

            return super.finalizeResult(fFinal);
            }
        }

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static CoherenceClusterOrchestration s_orchestration = new CoherenceClusterOrchestration()
            .withOptions(Logging.at(9),
                         CacheConfig.of("timeout-server-cache-config.xml"),
                         SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)));


    private SessionBuilder m_sessionBuilder;

    private long m_timeout;

    private boolean m_fShouldTimeout;

    @Rule
    public ExpectedException m_exception = ExpectedException.none();
    }
