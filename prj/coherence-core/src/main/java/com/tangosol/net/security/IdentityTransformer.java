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
* IdentityTransformer transforms a Subject to a token that asserts identity.
*
* @author dag 2009.12.04
*
* @since Coherence 3.6
*/
public interface IdentityTransformer
    {
    /**
    * Transform a Subject to a token that asserts an identity.
    *
    * @param subject  the Subject representing a user.
    * @param service  the Service requesting an identity token
    *
    * @return the token that asserts identity.
    *
    * @throws SecurityException if the identity transformation fails.
    *
    * @since Coherence 3.7 added service param which intentionally breaks
    *        compatibility with Coherence 3.6
    */
    public Object transformIdentity(Subject subject, Service service)
            throws SecurityException;
    }
