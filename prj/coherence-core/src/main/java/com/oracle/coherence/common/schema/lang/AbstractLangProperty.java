/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang;

import com.oracle.coherence.common.schema.AbstractProperty;
import com.oracle.coherence.common.schema.ExtensibleProperty;
import com.oracle.coherence.common.schema.TypeDescriptor;


/**
 * An abstract base class for language-specific property extension
 * implementations.
 *
 * @author as  2013.11.20
 */
public abstract class AbstractLangProperty<TD extends TypeDescriptor>
        extends AbstractProperty<TD>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct an {@code AbstractLangProperty} instance.
     *
     * @param parent  the parent {@code ExtensibleProperty} instance
     */
    protected AbstractLangProperty(ExtensibleProperty parent)
        {
        super(parent);
        }

    // ---- Property implementation -----------------------------------------

    @Override
    public TD getType()
        {
        return m_type;
        }

    // ---- public API ------------------------------------------------------

    /**
     * Set the property type.
     *
     * @param type  the property type
     */
    public void setType(TD type)
        {
        m_type = type;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The property type.
     */
    private TD m_type;
    }
