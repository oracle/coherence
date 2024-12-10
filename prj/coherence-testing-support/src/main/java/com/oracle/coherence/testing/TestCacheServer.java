/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Blocking;
import com.tangosol.net.DefaultCacheServer;

/**
 * This TestCacheServer will catch any exception
 * from {@link DefaultCacheServer#main(String[])}
 * and exit with an exit code of 1.
 *
 * @author jk  2015.02.17
 */
public final class TestCacheServer
    {

    private static final Object f_monitor = new Object();

    private static Throwable s_throwable;

    public static void main(String[] asArg)
            throws Exception
        {
        try
            {
            DefaultCacheServer.main(asArg);
            }
            catch (Throwable t)
                {
                s_throwable = t;
                }

        synchronized (f_monitor)
            {
            Blocking.wait(f_monitor);
            }
        }

    public static class GetThrowable
            implements RemoteCallable<String>
        {
        @Override
        public String call() throws Exception
            {
            return TestCacheServer.s_throwable == null ? null : TestCacheServer.s_throwable.getMessage();
            }
        }
    }
