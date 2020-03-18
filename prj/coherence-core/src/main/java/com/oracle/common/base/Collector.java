/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;


/**
 * A Collector is mechanism for receiving items.
 *
 * @param <V> the collected type
 *
 * @author mf  2010.10.06
 * @deprecated use {@link com.oracle.coherence.common.base.Collector} instead
 */
@Deprecated
public interface Collector<V>
        extends com.oracle.coherence.common.base.Collector<V>
    {
    }
