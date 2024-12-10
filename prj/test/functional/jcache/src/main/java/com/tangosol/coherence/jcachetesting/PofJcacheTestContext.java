/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import java.net.URI;

/**
 * Test Context for POF support.
 * This context test user type pof and Coherence Jcache Adapter's pof methods for EntryProcessors and serializers for
 * jcache specification of Duration and ExpiryPolicy class implementations.
 *
 * @version        1.0
 * @author         jfialli
 */
public class PofJcacheTestContext
        extends AbstractJcacheTestContext
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public URI getDistributedCacheConfigURI()
        {
        return getURI("junit-client-distributed-cache-config.xml");
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getServerCacheConfigURI()
        {
        return getURI("junit-server-cache-config.xml");
        }

        /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsPof()
        {
        return true;
        }
    }
