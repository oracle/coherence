/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;


/**
 * The Disposable interface is used for life-cycle management of resources.
 *
 * Disposable is also AutoCloseable and thus is compatible with the try-with-resources pattern.
 *
 * @author ch  2010.01.11
 * @deprecated use {@link com.oracle.coherence.common.base.Disposable} instead
 */
@Deprecated
public interface Disposable
        extends com.oracle.coherence.common.base.Disposable
    {
    }
