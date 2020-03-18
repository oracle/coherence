/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.extractor;

import java.lang.reflect.Method;
import java.util.Map;

import static com.tangosol.util.Base.azzert;

/**
 * Immutable descriptor of reflection computation of a target.
 * This descriptor enables very fast reference based matching in a homogeneous cache.
 *
 * @author jf 2016.08.12
 *
 * @see com.tangosol.util.extractor.UniversalExtractor
 * @see com.tangosol.util.extractor.UniversalUpdater
 */
public class TargetReflectionDescriptor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Descriptor of target reflection computation that resolved to a {@link Map}.
     *
     * @param clz target's class
     */
    public TargetReflectionDescriptor(Class clz)
        {
        this(clz, null);
        }

    /**
     * Descriptor of an extracted target computation for a method or property.
     *
     * @param clz    target class
     * @param method reflection resolved method
     */
    public TargetReflectionDescriptor(Class clz, Method method)
        {
        azzert(method != null || Map.class.isAssignableFrom(clz));
        f_clz    = clz;
        f_method = method;
        }

    // ----- TargetReflectionDescriptor methods ---------------------------------------

    /**
     * Return the target's class.
     *
     * @return the target's class
     */
    public Class getTargetClass()
        {
        return f_clz;
        }

    /**
     * Return the reflection resolved extracted target method.
     *
     * @return a Method or null if target class is a {@link Map}
     */
    public Method getMethod()
        {
        return f_method;
        }

    /**
     * Is this descriptor for a {@link Map}.
     *
     * @return true iff this descriptor is for a {@link Map}
     */
    public boolean isMap()
        {
        return f_clz != null && f_method == null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Target's class.
     */
    final private Class f_clz;

    /**
     * Reflection resolved target class method.
     * If null, {@link #f_clz} must be assignable to a {@link Map}.
     */
    final private Method f_method;
    }
