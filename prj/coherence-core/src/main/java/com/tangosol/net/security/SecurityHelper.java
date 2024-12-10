/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;


import com.tangosol.util.NullImplementation;

import java.lang.reflect.Method;

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
        try
            {
            return (currentMethod != null) ? (Subject) currentMethod.invoke(null, (Object[])null) :
                Subject.getSubject(AccessController.getContext());
            }
        catch (Exception ignore)
            {
            }
        return null;
        }

    
    // ----- constants  -----------------------------------------------------
    
    /**
     * A subject that represents nobody. 
     */
    public static final Subject EMPTY_SUBJECT = new Subject(true, NullImplementation.getSet(), 
            NullImplementation.getSet(), NullImplementation.getSet());

    static Method currentMethod = null;
    static
        {
        Class c;
        try
            {
            c = Class.forName("javax.security.auth.Subject");
            currentMethod = c.getMethod("current", (Class[]) null);
            }
        catch (Exception ignore)
            {
            // pre-JDK23
            }
        }
    }
