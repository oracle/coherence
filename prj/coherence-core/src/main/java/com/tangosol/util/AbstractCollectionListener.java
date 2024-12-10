/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

public class AbstractCollectionListener<V>
        implements CollectionListener<V>
    {
    @Override
    public void entryInserted(CollectionEvent<V> evt)
        {
        }

    @Override
    public void entryUpdated(CollectionEvent<V> evt)
        {
        }

    @Override
    public void entryDeleted(CollectionEvent<V> evt)
        {
        }
    }
