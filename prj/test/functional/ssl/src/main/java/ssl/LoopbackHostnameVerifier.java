/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ssl;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Hostname verifier for SSL functional tests that communicate over loopback.
 */
public class LoopbackHostnameVerifier
        implements HostnameVerifier
    {
    @Override
    public boolean verify(String sHostname, SSLSession session)
        {
        String sName = sHostname == null && session != null ? session.getPeerHost() : sHostname;
        if (sName == null)
            {
            return false;
            }

        try
            {
            return InetAddress.getByName(sName).isLoopbackAddress();
            }
        catch (UnknownHostException e)
            {
            return "localhost".equalsIgnoreCase(sName);
            }
        }
    }
