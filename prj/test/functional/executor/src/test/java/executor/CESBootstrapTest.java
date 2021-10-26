/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package executor;

import com.oracle.coherence.concurrent.executor.ClusteredExecutorInfo;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;
import com.oracle.coherence.concurrent.executor.Executors;

import com.oracle.coherence.concurrent.executor.TaskExecutorService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.junit.Assert.assertThat;

/**
 * Unit test to ensure the {@link ClusteredExecutorService} can be initialized
 * via the {@code Bootstrap API}.
 *
 * @author rl  2021.10.14
 * @since 21.12
 */
@SuppressWarnings("deprecation")
public class CESBootstrapTest
    {
    // ----- test initialization -------------------------------------------

    @BeforeClass
    public static void startUp()
        {
        Coherence.clusterMember().start().join();
        }

    @AfterClass
    public static void shutDown()
        {
        Coherence.closeAll();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldFindSession()
        {
        Session executorSession = Executors.getExecutorSession();
        assertThat(executorSession, is(notNullValue()));
        }

    @Test
    public void shouldGetLocalTaskExecutorService()
        {
        TaskExecutorService taskExecutorService = Executors.getLocalExecutorService();
        assertThat(taskExecutorService, is(notNullValue()));
        }

    @Test
    public void shouldHaveExpectedInternalCacheSize()
        {
        Session session = Executors.getExecutorSession();
        NamedCache<?, ?> executors = session.getCache(ClusteredExecutorInfo.CACHE_NAME);
        assertThat(executors.size(), is(1));
        }
    }
