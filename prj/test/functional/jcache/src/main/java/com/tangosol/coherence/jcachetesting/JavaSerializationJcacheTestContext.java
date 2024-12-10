/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import java.net.URI;

/**
 * Reference cache configuration files that default to java serialization.
 * This test context tests Java Serialiation and ExternalizableLite implementations
 * of Coherence Jcache Adapter.
 *
 * @version        1.0
 * @author         jfialli
 */
public class JavaSerializationJcacheTestContext
        extends AbstractJcacheTestContext
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public URI getDistributedCacheConfigURI()
        {
        return getURI("junit-client-distributed-cache-config-java.xml");
        }

    /**
     * {@inheritDoc}

     */
    @Override
    public URI getServerCacheConfigURI()
        {
        return getURI("junit-server-cache-config-java.xml");
        }

        /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsPof()
        {
        return false;
        }
    }
