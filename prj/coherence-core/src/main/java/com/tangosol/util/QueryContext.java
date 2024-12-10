/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.tangosol.net.BackingMapContext;


/**
 * The QueryContext provides an execution context to server-side agents during
 * query execution.
 *
 * @author tb 2011.05.26
 *
 * @since Coherence 3.7.1
 */
public interface QueryContext
    {
    /**
     * Return the corresponding {@link BackingMapContext}.
     *
     * @return the backing map context
     */
    public BackingMapContext getBackingMapContext();
    }
