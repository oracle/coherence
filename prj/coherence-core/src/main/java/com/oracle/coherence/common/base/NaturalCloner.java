/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * A Cloner that clones {@link Cloneable} objects.
 *
 * @author gg/mf 2012.07.12
 */
public class NaturalCloner
        implements Cloner
    {
    @Override
    public <T> T clone(T o)
        {
        return o == null ? null : (T) ((Cloneable) o).clone();
        }
    }
