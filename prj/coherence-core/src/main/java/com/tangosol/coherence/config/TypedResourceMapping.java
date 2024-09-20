/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.config;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.util.ClassHelper;

/**
 * A typed {@link ResourceMapping}.
 */
public abstract class TypedResourceMapping<R>
        extends ResourceMapping<R>
    {
    /**
     * Construct a {@link TypedResourceMapping} that will use raw types by default.
     *
     * @param sNamePattern  the pattern that maps names to schemes
     * @param sSchemeName   the name of the scheme to which resources matching this
     *                      {@link TypedResourceMapping} will be associated
     */
    public TypedResourceMapping(String sNamePattern, String sSchemeName)
        {
        super(sNamePattern, sSchemeName);
        m_sValueClassName = null;
        }

    /**
     * Obtains the name of the value class for the resource using this {@link TypedResourceMapping}.
     *
     * @return the name of the value class or <code>null</code> if raw types are being used
     */
    public String getValueClassName()
        {
        return m_sValueClassName;
        }

    /**
     * Sets the name of the value class for the resources using this {@link TypedResourceMapping}.
     *
     * @param sElementClassName the name of the value class or <code>null</code> if raw types are being used
     */
    @Injectable("value-type")
    public void setValueClassName(String sElementClassName)
        {
        m_sValueClassName = ClassHelper.getFullyQualifiedClassNameOf(sElementClassName);
        }

    /**
     * Determines if the {@link TypedResourceMapping} is configured to use raw-types
     * (ie: no type checking or constraints)
     *
     * @return <code>true</code> if using raw types, <code>false</code> otherwise
     */
    public boolean usesRawTypes()
        {
        return m_sValueClassName == null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the value class or <code>null</code> if raw types are being used (the default).
     */
    private String m_sValueClassName;
    }
