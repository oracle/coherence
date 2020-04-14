/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;


import com.tangosol.net.Service;

import com.tangosol.util.Base;

import javax.security.auth.Subject;


/**
* The default implementation of the IdentityAsserter interface.
* <p>
* The default implementation asserts that the token is a Subject.
*
* @author dag 2009.11.16
*/
public class DefaultIdentityAsserter
        extends Base
        implements IdentityAsserter
    {
    // ----- IdentityAsserter implementation --------------------------------

    /**
    * {@inheritDoc}
    */
    public Subject assertIdentity(Object oToken, Service service)
            throws SecurityException
        {
        // support old behavior where a null token is passed if no Subject is
        // in the client context
        if (oToken == null)
            {
            return null;
            }
        if (oToken instanceof Subject)
            {
            return (Subject) oToken;
            }
        err("DefaultIdentityAsserter expected Subject but found: " + oToken);
        throw new SecurityException("identity token is unsupported type");
        }


    // ----- constants ------------------------------------------------------

    /**
    * An instance of the DefaultIdentityAsserter.
    */
    public static final DefaultIdentityAsserter INSTANCE =
            new DefaultIdentityAsserter();
    }
