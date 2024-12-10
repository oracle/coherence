/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;

import com.tangosol.net.Cluster;

import com.tangosol.util.Base;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

import javax.security.auth.callback.CallbackHandler;


/**
* The Security class is used to associate client's identity with an action
* that requires access to protected clustered resources.
* <p>
* Consider the following code example:
* <pre>
    Subject subject = Security.login(sPrincipal, acPassword);
    PrivilegedAction action =
        new PrivilegedAction()
            {
            public Object run()
                {
                return CacheFactory.getCache(sCacheName);
                }
            };
    NamedCache cache = (NamedCache) Security.runAs(subject, action);
* </pre>
*
* The implementation of the run() method in PrivilegedAction does not have to
* be an immediate CacheFactory related call; it could be any sequence of code.
* Any calls that made within that context will be executed with the same
* privileges.
* <p>
* If a call that accesses a protected clustered resource is made outside of
* the "runAs" scope, the AccessController will instantiate and use a
* CallbackHandler specified in the tangosol-coherence.xml descriptor.
* If it is not specified and security is enabled the resource access request
* will be rejected.
*<p>
* A SecurityException is thrown if the caller does not have permission to call
* a particular method; the controlling permissions are instances of
* javax.security.auth.AuthPermission with corresponding target names such
* as "coherence.login" or "coherence.runAs".
*
* @author gg  2004.06.02
* @since Coherence 2.5
*/
public abstract class Security
        extends Base
    {
    /**
    * Perform the authentication. This method does nothing and returns null
    * if Coherence security is disabled.
    *
    * @param sName       the user name to use for authentication
    * @param acPassword  the password to use for authentication
    *
    * @return the authenticated Subject object that has associated
    *         Principals and Credentials; null if security is disabled
    * @throws SecurityException if authentication fails
    */
    public static Subject login(String sName, char[] acPassword)
        {
        return login(new SimpleHandler(sName, acPassword));
        }

    /**
    * Perform the authentication. This method does nothing and returns null
    * if Coherence security is disabled.
    *
    * @param handler  the CallbackHandler to be used for authentication
    *
    * @return the authenticated Subject object that has associated
    *         Principals and Credentials; null if security is disabled
    * @throws SecurityException if authentication fails
    */
    public static Subject login(CallbackHandler handler)
        {
        if (s_eState != null)
            {
            throw ensureRuntimeException(s_eState);
            }

        try
            {
            return (Subject) s_methLogin.invoke(null, new Object[] {handler});
            }
        catch (InvocationTargetException e)
            {
            throw ensureRuntimeException(e.getTargetException());
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Executes a privileged action on behalf of the user identity.
    * If Coherence security is disabled the subject parameter is ignored
    * and this method behaves effectively as "return action.run()"
    *
    * @param subject the identity to perform action on behalf of
    * @param action  the privileged action to perform
    *
    * @return the result of the action
    */
    public static Object runAs(Subject subject, PrivilegedAction action)
        {
        if (s_eState != null)
            {
            throw ensureRuntimeException(s_eState);
            }

        azzert(action != null, "Action is null");
        try
            {
            return s_methRunAs.invoke(null, new Object[] {subject, action});
            }
        catch (InvocationTargetException e)
            {
            throw ensureRuntimeException(e.getTargetException());
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Executes a privileged exception action on behalf of the user identity.
    * If Coherence security is disabled the subject parameter is ignored
    * and this method behaves effectively as "return action.run()"
    *
    * @param subject the identity to perform action on behalf of
    * @param action  the privileged exception action to perform
    *
    * @return the result of the action
    *
    * @throws PrivilegedActionException if the specified action's run method
    *         threw a checked exception
    */
    public static Object runAs(Subject subject, PrivilegedExceptionAction action)
            throws PrivilegedActionException
        {
        if (s_eState != null)
            {
            throw ensureRuntimeException(s_eState);
            }

        azzert(action != null, "Action is null");
        try
            {
            return s_methRunAs.invoke(null, new Object[] {subject, action});
            }
        catch (InvocationTargetException e)
            {
            Throwable te = e.getTargetException();
            if (te instanceof PrivilegedActionException)
                {
                throw (PrivilegedActionException) te;
                }
            throw ensureRuntimeException(te);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Check if the current user has permission to perform the action against
    * "clustered resources", such as clustered services and caches.
    *
    * @param cluster       the Cluster object
    * @param sServiceName  the name of the Service
    * @param sCacheName    the name of the Cache
    * @param sAction       the action to be performed (for example,
    *                      "create", "destroy", "join")
    *
    * @throws SecurityException if permission is denied
    */
    public static void checkPermission(Cluster cluster, String sServiceName,
            String sCacheName, String sAction)
        {
        if (ENABLED)
            {
            if (s_eState != null)
                {
                throw ensureRuntimeException(s_eState);
                }

            try
                {
                s_methCheckPermission.invoke(null,
                        cluster, sServiceName, sCacheName, sAction);
                }
            catch (InvocationTargetException e)
                {
                throw ensureRuntimeException(e.getTargetException());
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }


    // ----- static data  ---------------------------------------------------

    /**
    * Indicates if security is enabled by the operational configuration.
    */
    public static final boolean ENABLED;

    /**
    * Indicates if subject scope is enabled by the operational configuration.
    */
    public static final boolean SUBJECT_SCOPED;

    // ----- class initialization -------------------------------------------

    private static final String SECURITY = "com.tangosol.coherence.component.net.Security";

    /**
    * Optimization: Security.login() method
    */
    private static Method s_methLogin;

    /**
    * Optimization: Security.runAs() method
    */
    private static Method s_methRunAs;

    /**
    * Optimization: Security.checkPermission() method
    */
    private static Method s_methCheckPermission;

    /**
    * Last reflection exception.
    */
    private static Throwable s_eState;

    static
        {
        boolean fEnabled = false;
        boolean fScoped  = false;
        try
            {
            Class clzSecurity     = Class.forName(SECURITY);
            s_methLogin           = clzSecurity.getMethod("login",
                                    new Class[] {CallbackHandler.class});
            s_methRunAs           = clzSecurity.getMethod("runAs",
                                    new Class[] {Subject.class, Object.class});
            s_methCheckPermission = clzSecurity.getMethod("checkPermission",
                                    new Class[] {Cluster.class, String.class, String.class, String.class});
            fEnabled              = ((Boolean) clzSecurity.getMethod("isSecurityEnabled").
                                    invoke(null)).booleanValue();
            fScoped               = ((Boolean) clzSecurity.getMethod("isSubjectScoped").
                                    invoke(null)).booleanValue();
            }
        catch (Throwable e)
            {
            s_eState = e;
            }
        ENABLED        = fEnabled;
        SUBJECT_SCOPED = fScoped;
        }
    }
