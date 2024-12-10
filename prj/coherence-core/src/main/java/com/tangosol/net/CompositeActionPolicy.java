/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.util.NullImplementation;

/**
 * CompositeActionPolicy represents the "intersection" of two policies.  More
 * formally, a given action is {@link #isAllowed allowed} iff both component
 * policies allow it.
 *
 * @author rhl 2012.09.20
 */
public class CompositeActionPolicy
        implements ActionPolicy
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CompositeActionPolicy representing the specified policies.
     *
     * @param policyPrimary    the first policy to consult
     * @param policySecondary  the second policy to consult
     */
    public CompositeActionPolicy(ActionPolicy policyPrimary, ActionPolicy policySecondary)
        {
        f_policyPrimary   = policyPrimary   == null ? NullImplementation.getActionPolicy() : policyPrimary;
        f_policySecondary = policySecondary == null ? NullImplementation.getActionPolicy() : policySecondary;
        }

    // ----- ActionPolicy methods -------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void init(Service service)
        {
        f_policyPrimary  .init(service);
        f_policySecondary.init(service);
        }

    /**
     * {@inheritDoc}
     */
    public boolean isAllowed(Service service, Action action)
        {
        return f_policyPrimary  .isAllowed(service, action) &&
               f_policySecondary.isAllowed(service, action);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the primary policy.
     *
     * @return the primary policy
     */
    public ActionPolicy getPrimaryPolicy()
        {
        return f_policyPrimary;
        }

    /**
     * Return the secondary policy.
     *
     * @return the secondary policy
     */
    public ActionPolicy getSecondaryPolicy()
        {
        return f_policySecondary;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The primary ActionPolicy.
     */
    protected final ActionPolicy f_policyPrimary;

    /**
     * The secondary ActionPolicy.
     */
    protected final ActionPolicy f_policySecondary;
    }
