/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;

/**
 * A Holder implementation which additionally provides an equals/hashCode implementation based on the held
 * object's identity.
 *
 * @author  mf  2017.03.27
 * @deprecated use {@link com.oracle.coherence.common.base.IdentityHolder} instead
 */
@Deprecated
public class IdentityHolder<V>
        extends com.oracle.coherence.common.base.IdentityHolder<V>
        implements Holder<V>
    {
    /**
     * Construct an IdentityHolder holding nothing.
     */
    public IdentityHolder()
        {
        super();
        }

    /**
     * Construct an IdentityHolder holding the specified object.
     *
     * @param v  the object to hold
     */
    public IdentityHolder(V v)
        {
        super(v);
        }
    }
