/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;

import com.tangosol.util.NullImplementation;

import java.security.AccessController;

import javax.security.auth.Subject;


/**
* A collection of security-related utilities.
*
* @author dag 2009.11.16
*/
public abstract class SecurityHelper
    {
    /**
    * Return the Subject from the current security context.
    *
    * @return the current Subject.
    */
    public static Subject getCurrentSubject()
        {
        return Subject.getSubject(AccessController.getContext());
        }

    
    // ----- constants  -----------------------------------------------------
    
    /**
     * A subject that represents nobody. 
     */
    public static final Subject EMPTY_SUBJECT = new Subject(true, NullImplementation.getSet(), 
            NullImplementation.getSet(), NullImplementation.getSet());

    }
