/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.util.ObservableMap;

/**
 * A backing map used by a paged topic contents cache.
 *
 * @author Jonathan Knight  2022.08.11
 * @since 14.1.1
 */
@SuppressWarnings("rawtypes")
public class PagedTopicContentBackingMap
        extends PagedTopicBackingMap
    {
    /**
     * Create a {@link PagedTopicContentBackingMap}.
     *
     * @param map  the wrapped backing map
     */
    public PagedTopicContentBackingMap(ObservableMap map)
        {
        super(map);
        }
    }
