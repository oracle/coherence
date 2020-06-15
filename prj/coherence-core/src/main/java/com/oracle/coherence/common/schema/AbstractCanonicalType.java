/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

/**
 * An abstract base class for canonical (not platform specific) type extension
 * implementations.
 *
 * @author as  2013.08.30
 */
public abstract class AbstractCanonicalType<P extends Property>
        extends AbstractType<P, CanonicalTypeDescriptor>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct an {@code AbstractCanonicalType} instance.
     *
     * @param parent  the parent {@code ExtensibleType} instance
     */
    protected AbstractCanonicalType(ExtensibleType parent)
        {
        super(parent);
        }

    // ---- Type implementation -----------------------------------------

    @Override
    public CanonicalTypeDescriptor getDescriptor()
        {
        return getParent().getDescriptor();
        }
    }
