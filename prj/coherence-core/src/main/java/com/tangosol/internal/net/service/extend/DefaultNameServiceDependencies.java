/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend;

import com.tangosol.internal.net.service.DefaultServiceDependencies;
import com.tangosol.internal.net.service.peer.acceptor.AcceptorDependencies;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

/**
 * The DefaultNameServiceDependencies class provides a default implementation
 * of NameServiceDependencies.
 *
 * @author phf 2012.02.01
 *
 * @since Coherence 12.1.2
 */
public class DefaultNameServiceDependencies
        extends DefaultServiceDependencies
        implements NameServiceDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultNameServiceDependencies object.
     */
    public DefaultNameServiceDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultNameServiceDependencies object, copying the values from the
     * specified NameServiceDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultNameServiceDependencies(NameServiceDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_acceptorDependencies = deps.getAcceptorDependencies();
            }
        }

    // ----- NameServiceDependencies interface ------------------------------

    /**
     * {@inheritDoc}
     */
    public AcceptorDependencies getAcceptorDependencies()
        {
        return m_acceptorDependencies;
        }

    // ----- DefaultNameServiceDependencies methods -------------------------

    /**
     * Set the AcceptorDependencies.
     *
     * @param deps  the AcceptorDependencies
     *
     * @return this object
     */
    public DefaultNameServiceDependencies setAcceptorDependencies(AcceptorDependencies deps)
        {
        m_acceptorDependencies = deps;

        return this;
        }

    /**
     * Validate the dependencies.
     *
     * @return this object
     */
    public DefaultNameServiceDependencies validate()
        {
        Base.checkNotNull(getAcceptorDependencies(), "Acceptor");

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + "{AcceptorDependencies=" + getAcceptorDependencies() + "}";
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The AcceptorDependencies.
     */
    private AcceptorDependencies m_acceptorDependencies;
    }
