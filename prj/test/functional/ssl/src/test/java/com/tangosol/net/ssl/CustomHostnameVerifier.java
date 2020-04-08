/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.ssl;


import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;


/**
* Custom HostnameVerifier.
*
* @author jh  2010.04.29
*/
public class CustomHostnameVerifier
        implements HostnameVerifier
    {
    public CustomHostnameVerifier(boolean fAllow)
        {
        m_fAllow = fAllow;
        }

    public boolean verify(String s, SSLSession sslSession)
        {
        return isAllowed();
        }

    public boolean isAllowed()
        {
        return m_fAllow;
        }

    private final boolean m_fAllow;
    }
