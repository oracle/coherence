/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * A Cloner provides an external means for producing copies of objects as
 * prescribed by the {@link Object#clone()} contract.
 *
 * @author gg/mf  2012.07.12
 */
public interface Cloner
    {
    /**
     * Return a copy of the specified object.
     *
     * @param o  the object to clone
     * @param <T> the type of the object
     *
     * @return the new object
     */
    public <T> T clone(T o);
    }
