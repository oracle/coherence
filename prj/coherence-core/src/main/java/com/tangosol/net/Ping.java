/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.oracle.coherence.common.base.Blocking;

import com.tangosol.coherence.config.Config;

import com.tangosol.util.Base;

import java.net.InetAddress;
import java.net.NetworkInterface;

import java.util.Enumeration;


/**
* Ping is a simple utility for testing if a machine is reachable.
*
* @author mf  2010.07.20
*/
public class Ping
    {
    /**
    * Check if the specified address is reachable.
    */
    public static void main(String[] asArg)
            throws Exception
        {
        if (asArg.length < 1 || asArg.length > 2)
            {
            System.err.println("usage: com.tangosol.net.Ping [source ip] <destination ip>");
            return;
            }

        String sSrc            = asArg.length == 1 ? null     : asArg[0];
        String sDst            = asArg.length == 1 ? asArg[0] : asArg[1];
        long   cMillisInterval = Base.parseTime(Config.getProperty("coherence.net.ping.interval", "1s"));
        int    cMillisTimeout  = (int) Base.parseTime(Config.getProperty("coherence.net.ping.timeout", "5s"));

        NetworkInterface nicSrc = null;
        if (sSrc != null)
            {
            nicSrc = NetworkInterface.getByInetAddress(InetAddress.getByName(sSrc));
            if (nicSrc == null)
                {
                System.err.println(sSrc + " is not a local address");
                System.exit(1);
                }

            Enumeration<InetAddress> iterIP = nicSrc.getInetAddresses();
            iterIP.nextElement();
            if (iterIP.hasMoreElements())
                {
                // Note we don't disable multi-IP source NICs in this test as the point of the test is to diagnose
                // problems, so we simply warn what may happen, the user is free to re-run without a source address
                System.err.println(sSrc + " is associated with network interface " + nicSrc.getDisplayName()
                        + " which has multiple IPs, this may prevent Pings from being routed properly.\n"
                        + "Note: Coherence will detect and automatically avoid this issue.");
                }
            }

        InetAddress addrDst = InetAddress.getByName(sDst);
        while (true)
            {
            long ldtStart = System.nanoTime();
            if (addrDst.isReachable(nicSrc, 0, cMillisTimeout))
                {
                long ldtDelta = System.nanoTime() - ldtStart;
                System.out.println("Response received from " + addrDst +
                        " after " + (ldtDelta / 1000000.0) + "ms");
                Blocking.sleep(cMillisInterval);
                }
            else
                {
                System.out.println("Request timeout for " + addrDst +
                        " after " + cMillisTimeout + "ms; using " +
                        "java.net.InetAddress.isReachable");
                if (cMillisTimeout < cMillisInterval)
                    {
                    Blocking.sleep(cMillisInterval - cMillisTimeout);
                    }
                }
            }
        }
    }
