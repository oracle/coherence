/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.AsyncNamedMap;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("resource")
public class AsyncNamedMapExecutorTests
    {
    @BeforeClass
    public static void setup() throws Exception
        {
        System.setProperty("coherence.ttl",       "0");
        System.setProperty("coherence.wka",       "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");

        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        s_session = coherence.getSession();
        }

    @Test
    public void shouldUseExecutor() throws Exception
        {
        NamedCache<String, String> cache    = s_session.getCache("dist-test");
        Executor                   real     = Executors.newSingleThreadExecutor();
        Executor                   executor = mock(Executor.class);

        doAnswer(invocation ->
            {
            Runnable runnable = invocation.getArgument(0);
            real.execute(runnable);
            return null;
            })
            .when(executor).execute(any(Runnable.class));

        AsyncNamedCache<String, String> async  = cache.async(AsyncNamedMap.Complete.using(executor));
        CompletableFuture<Void>         future = async.put("key-1", "value-1");
        future.get(1, TimeUnit.MINUTES);
        Eventually.assertDeferred(() -> cache.get("key-1"), is("value-1"));

        verify(executor, atLeastOnce()).execute(any(Runnable.class));
        }

    // ----- data members ---------------------------------------------------

    private static Session s_session;
    }
