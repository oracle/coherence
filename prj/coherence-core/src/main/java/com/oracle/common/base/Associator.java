/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;


/**
 * The Associator interface facilitates the creation of a very generic
 * <a href="http://en.wikipedia.org/wiki/Equivalence_relation">
 * equivalence relation</a> between different objects and allows to group them
 * based on the equality of the "association key" object returned by the
 * {@link #getAssociatedKey} method.
 *
 * @author gg 2012.03.11
 *
 * @see Associated
 * @deprecated use {@link com.oracle.coherence.common.base.Associator} instead
 */
@Deprecated
public interface Associator
        extends com.oracle.coherence.common.base.Associator
    {
    }