/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;

import com.tangosol.config.annotation.Injectable;

/**
 * The DefaultRemoteCacheServiceDependencies class provides a default implementation of
 * RemoteCacheServiceDependencies.
 *
 * @author pfm 2011.09.05
 * @since Coherence 12.1.2
 */
public class DefaultRemoteCacheServiceDependencies
        extends DefaultRemoteServiceDependencies
        implements RemoteCacheServiceDependencies
    {
    /**
     * Construct a DefaultRemoteCacheServiceDependencies object.
     */
    public DefaultRemoteCacheServiceDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultRemoteCacheServiceDependencies object, copying the values from
     * the specified RemoteCacheServiceDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultRemoteCacheServiceDependencies(RemoteCacheServiceDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_fDeferKeyAsssocationCheck = deps.isDeferKeyAssociationCheck();
            }
        }

    // ----- DefaultRemoteCacheServiceDependencies methods ------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeferKeyAssociationCheck()
        {
        return m_fDeferKeyAsssocationCheck;
        }

    /**
     * Set the flag to defer the KeyAssociation check.
     *
     * @param fDefer  the KeyAssociation check defer flag
     */
    @Injectable("defer-key-association-check")
    public void setDeferKeyAssociationCheck(boolean fDefer)
        {
        m_fDeferKeyAsssocationCheck = fDefer;
        }

    /**
     * Validate the supplied dependencies.
     *
     * @throws IllegalArgumentException if the dependencies are not valid
     *
     * @return this object
     */
    @Override
    public DefaultRemoteServiceDependencies validate()
        {
        super.validate();

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString()
               + "{DeferKeyAssociationCheck=" + isDeferKeyAssociationCheck()
               + "}";
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The flag to indicate if the KeyAssociation check is deferred.
     */
    private boolean m_fDeferKeyAsssocationCheck;
    }
