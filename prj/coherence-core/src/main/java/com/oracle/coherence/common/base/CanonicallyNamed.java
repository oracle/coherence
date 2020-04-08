/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

/**
 * CanonicallyNamed provides a way for objects to identify themselves by name.
 * By convention two objects with the same non-null canonical name are considered to be
 * {@link Object#equals(Object) equal} and will have the same {@link Object#hashCode() hashCode}.
 *
 * @author mf/jf  2017.12.15
 * @since 12.2.1.4
 */
public interface CanonicallyNamed
    {
    /**
     * Return a canonical name for the entity or {@code null}.
     *
     * @return canonical name or {@code null}
     */
    String getCanonicalName();
    }
