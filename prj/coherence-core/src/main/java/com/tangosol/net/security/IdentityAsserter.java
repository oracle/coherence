/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;


import com.tangosol.net.Service;

import javax.security.auth.Subject;


/**
* IdentityAsserter validates a token in order to establish a user's identity.
* The token is an identity assertion, a statement that asserts an identity.
* <p>
* A token is opaque to Coherence. It could be a standard type such as a SAML
* Assertion or a proprietary type.
*
* @author dag 2009.10.30
*
* @since Coherence 3.6
*/
public interface IdentityAsserter
    {
    /**
    * Asserts an identity based on a token-based identity assertion.
    *
    * @param oToken   the token that asserts identity.
    * @param service  the Service asserting the identity token
    *
    * @return a Subject representing the identity.
    *
    * @throws SecurityException if the identity assertion fails.
    * 
    * @since Coherence 3.7 added service param which intentionally breaks
    *        compatibility with Coherence 3.6
    */
    public Subject assertIdentity(Object oToken, Service service)
            throws SecurityException;
    }
