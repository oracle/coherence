/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * This interface is meant to "fix" the usability issues related to the
 * {@link java.lang.Cloneable} method-less interface.
 *
 * @author gg/mf 2012.07.12
 */
public interface Cloneable
        extends java.lang.Cloneable
    {
    /**
     * Create and return a copy of this object as described by the
     * {@link Object#clone()} contract.
     *
     * @return a copy of this object
     */
    public Object clone();
    }
