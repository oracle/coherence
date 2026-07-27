/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;


import com.tangosol.net.Service;

import com.tangosol.util.Base;

import javax.security.auth.Subject;


/**
* The default implementation of the IdentityAsserter interface.
* <p>
* The default implementation accepts only an absent identity token.
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
        String sType = oToken.getClass().getName();

        err("DefaultIdentityAsserter does not accept identity token type: " + sType);
        throw new SecurityException("identity token is unsupported type: " + sType);
        }


    // ----- constants ------------------------------------------------------

    /**
    * An instance of the DefaultIdentityAsserter.
    */
    public static final DefaultIdentityAsserter INSTANCE =
            new DefaultIdentityAsserter();
    }
