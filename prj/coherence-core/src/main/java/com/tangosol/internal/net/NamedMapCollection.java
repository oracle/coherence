/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedMap;

import java.util.Collection;

/**
 * A {@link Collection} that wraps a {@link NamedMap}.
 */
public interface NamedMapCollection<K, V, E>
        extends NamedCollection, Collection<E>
    {
    /**
     * Return the wrapped {@link NamedMap}.
     *
     * @return the wrapped {@link NamedMap}
     */
    NamedMap<K, V> getNamedMap();
    }
