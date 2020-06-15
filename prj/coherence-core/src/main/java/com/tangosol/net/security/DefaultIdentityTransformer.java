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
* The default implementation of the IdentityTransformer interface, which
* simply returns the Subject that is passed to it.
*
* @author dag 2009.12.21
*/
public class DefaultIdentityTransformer
        implements IdentityTransformer
    {
    // ----- IdentityTransformer implementation -----------------------------

    /**
    * {@inheritDoc}
    */
    public Object transformIdentity(Subject subject, Service service)
        {
        return subject;
        }


    // ----- constants ------------------------------------------------------

    /**
    * An instance of the DefaultIdentityTransformer.
    */
    public static final DefaultIdentityTransformer INSTANCE =
            new DefaultIdentityTransformer();
    }
