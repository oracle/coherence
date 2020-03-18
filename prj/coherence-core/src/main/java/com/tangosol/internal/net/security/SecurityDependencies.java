/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.IdentityTransformer;
import com.tangosol.net.security.Authorizer;

/**
 * The SecurityDependencies interface provides a Security object with
 * external dependencies.
 *
 * @author der  2011.12.01
 * @since Coherence 12.1.2
 */
public interface SecurityDependencies
    {
    /**
     * Return a flag indicating whether security is enabled.
     *
     * @return flag indicating whether security is enabled
     */
    public boolean isEnabled();

    /**
     * Return a flag indicating whether subject scoping is specified.
     *
     * @return flag indicating whether subject scope is specified
     */
    public boolean isSubjectScoped();

    /**
     * Return the Identity Asserter object.
     *
     * @return Identity Asserter object
     */
    public IdentityAsserter getIdentityAsserter();

    /**
     * Return the Identity Transformer object.
     *
     * @return Identity Transformer object
     */
    public IdentityTransformer getIdentityTransformer();

    /**
     * Return the Authorizer object.
     *
     * @return Authorizer object
     */
    public Authorizer getAuthorizer();

    /**
     * Return the security model.
     *
     * @return security model
     */
    public String getModel();
    }
