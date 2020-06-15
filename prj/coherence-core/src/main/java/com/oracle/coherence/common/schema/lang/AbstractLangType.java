/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang;


import com.oracle.coherence.common.schema.AbstractType;
import com.oracle.coherence.common.schema.ExtensibleType;
import com.oracle.coherence.common.schema.Property;
import com.oracle.coherence.common.schema.TypeDescriptor;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * An abstract base class for language-specific type extension implementations.
 *
 * @author as  2013.11.20
 */
public abstract class AbstractLangType<P extends Property, TD extends TypeDescriptor>
        extends AbstractType<P, TD>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct an {@code AbstractLangType} instance.
     *
     * @param parent  the parent {@code ExtensibleType} instance
     */
    protected AbstractLangType(ExtensibleType parent)
        {
        super(parent);
        }

    // ---- Type implementation ---------------------------------------------

    @Override
    public TD getDescriptor()
        {
        return m_descriptor;
        }

    // ---- public API ------------------------------------------------------

    /**
     * Set type descriptor for this type.
     *
     * @param descriptor  the type descriptor
     */
    public void setDescriptor(TD descriptor)
        {
        m_descriptor = descriptor;
        }

    /**
     * Return the aliases for this type.
     *
     * @return the aliases for this type
     */
    public Set<TD> getAliases()
        {
        return Collections.unmodifiableSet(m_aliases);
        }

    /**
     * Add an alias for this type.
     *
     * @param descriptor  the alias type descriptor
     */
    public void addAlias(TD descriptor)
        {
        m_aliases.add(descriptor);
        }

    // ---- data members ----------------------------------------------------

    /**
     * The type descriptor for this type.
     */
    private TD m_descriptor;

    /**
     * A set of aliases for this type.
     */
    private Set<TD> m_aliases = new LinkedHashSet<>();
    }
