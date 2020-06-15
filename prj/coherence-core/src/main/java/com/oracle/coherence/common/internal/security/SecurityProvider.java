/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.internal.security;


import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Provider;
import java.security.Security;


/**
* Security Provider implementation that returns custom security services.
*
* @author jh  2010.05.11
*/
public class SecurityProvider
        extends Provider
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    SecurityProvider()
        {
        super(NAME, 1.0, "Oracle Commons Security Provider");

        putService(new Service(this,
                "TrustManagerFactory",
                PeerX509TrustManager.ALGORITHM,
                PeerX509TrustManagerFactory.class.getName(),
                null,
                null));
        }


    // ----- SecurityProvider methods ---------------------------------------

    /**
    * Ensure that an instance of this provider has been registered with the
    * system.
    */
    public static void ensureRegistration()
        {
        try
            {
            synchronized (Security.class)
                {
                if (Security.getProvider(SecurityProvider.NAME) == null)
                    {
                    AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Object>()
                            {
                            public Object run() throws Exception
                                {
                                Security.addProvider(new SecurityProvider());
                                return null;
                                }
                            });
                    }
                }
            }
        catch (PrivilegedActionException e)
            {
            // should never happen
            throw new RuntimeException(e.getException());
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * The name of this provider.
    */
    public static final String NAME = "OracleCommonsSecurityProvider";
    }
