/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

/**
 * An abstract base class for canonical (not platform specific) property
 * extension implementations.
 *
 * @author as  2013.08.30
 */
public abstract class AbstractCanonicalProperty
        extends AbstractProperty<CanonicalTypeDescriptor>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct an {@code AbstractCanonicalProperty} instance.
     *
     * @param parent  the parent {@code ExtensibleProperty} instance
     */
    protected AbstractCanonicalProperty(ExtensibleProperty parent)
        {
        super(parent);
        }

    // ---- Property implementation -----------------------------------------

    @Override
    public CanonicalTypeDescriptor getType()
        {
        return getParent().getType();
        }
    }
