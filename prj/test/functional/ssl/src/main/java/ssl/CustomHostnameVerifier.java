/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ssl;


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
