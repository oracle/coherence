/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;

import com.oracle.coherence.common.base.TimeHelper;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class PutWithExpiryTests
    {
    @BeforeClass
    public static void setup() throws Exception
        {
        session = Coherence.clusterMember().startAndWait().getSession();
        }

    @Test
    public void shouldPutWithLongMaxValueExpiry() throws Exception
        {
        NamedCache<String, String> cache = session.getCache("test");
        cache.put("key", "value", Long.MAX_VALUE);
        }

    @Test
    public void shouldPutWithExpiryPastUnixEpochEnd() throws Exception
        {
        long epochEnd = Long.MAX_VALUE;
        long now      = TimeHelper.getSafeTimeMillis();
        long expiry   = epochEnd - now + TimeUnit.DAYS.toMillis(1);
        // expiry delay would be one day past epoch end
        NamedCache<String, String> cache = session.getCache("test");
        cache.put("key", "value", expiry);
        }

    public static Session session;
    }
