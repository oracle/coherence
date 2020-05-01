/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package cache;

import com.tangosol.net.cache.CacheStore;
import java.util.Collection;
import java.util.Map;

/**
 * {@link CacheStore} implementation for testing with AsyncNamedcache
 * putAll operation.
 *
 * @author cp  2020.04.29
 */
public class TestCacheStore
        implements CacheStore
    {

    @Override
    public void store(Object key, Object value)
        {
        throw new RuntimeException("This is exceptional Cache Store!");
        }

    @Override
    public void storeAll(Map mapEntries)
        {
        throw new RuntimeException("This is exceptional Cache Store!");
        }

    @Override
    public void erase(Object key)
        {
        throw new RuntimeException("This is exceptional Cache Store!");
        }

    @Override
    public void eraseAll(Collection colKeys)
        {
        throw new RuntimeException("This is exceptional Cache Store!");
        }

    @Override
    public Object load(Object key)
        {
        throw new RuntimeException("This is exceptional Cache Store!");
        }

    @Override
    public Map loadAll(Collection colKeys)
        {
        throw new RuntimeException("This is exceptional Cache Store!");
        }
    }
