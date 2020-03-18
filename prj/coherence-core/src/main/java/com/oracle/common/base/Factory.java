/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;


/**
 * The Factory interface provides a means of producing objects of a given
 * type.
 *
 * @param <T>  the type of the created object
 *
 * @author mf  2010.11.23
 * @deprecated use {@link com.oracle.coherence.common.base.Factory} instead
 */
@Deprecated
public interface Factory<T>
        extends com.oracle.coherence.common.base.Factory<T>
    {
    }
