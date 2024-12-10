/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.CacheFactory;

public class SuspendService
        implements RemoteCallable<Boolean>
    {
    public SuspendService(String sName)
        {
        f_sName = sName;
        }

    @Override
    public Boolean call() throws Exception
        {
        Logger.info("Suspending service " + f_sName);
        CacheFactory.ensureCluster().suspendService(f_sName);
        Logger.info("Suspended service " + f_sName);
        return true;
        }

    private final String f_sName;
    }
