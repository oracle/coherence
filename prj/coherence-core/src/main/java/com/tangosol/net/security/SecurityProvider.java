/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;


import com.tangosol.util.Base;

import com.oracle.coherence.common.internal.security.PeerX509TrustManager;
import com.oracle.coherence.common.internal.security.PeerX509TrustManagerFactory;

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
        super(NAME, 1.0, "Coherence Security Provider");

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
                    Security.addProvider(new SecurityProvider());
                    }
                }
            }
        catch (SecurityException e)
            {
            // should never happen
            Base.err(e);
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * The name of this provider.
    */
    public static final String NAME = "CoherenceSecurityProvider";
    }
