/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.xtangosol.tools.ant;


import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Random;


/**
* A set of utility methods used by the various Ant tasks.
*
* @author jh  2009.12.15
*/
public abstract class AntUtils
    {
    /**
    * Return a unique IP address for this machine.
    *
    * @param fMulticast  true if the returned address should be a multicast
    *                    address
    *
    * @return a unique IP address for this machine
    */
    public static String generateUniqueAddress(boolean fMulticast)
        {
        String sAddr;
        try
            {
            sAddr = InetAddress.getLocalHost().getHostAddress();
            }
        catch (UnknownHostException e)
            {
            sAddr = "127.0.0.1";
            }

        String[] asAddr = sAddr.split("\\.");

        if (fMulticast)
            {
            sAddr = "224";
            }
        else
            {
            sAddr = asAddr[0];
            }

        Random rnd = new Random();
        for (int i = 0; i < 2; ++i)
            {
            sAddr += ".";
            sAddr += rnd.nextInt(256);
            }
        sAddr += "." + asAddr[3];

        return sAddr;
        }

    /**
    * Return a unique port for this machine.
    *
    * @return a unique port for this machine
    */
    public static int generateUniquePort()
        {
        return generateUniquePorts(1)[0];
        }

    /**
    * Return the specified number of unique ports for this machine.
    *
    * @param cPort  the number of unique ports to generate; must be <= 1
    *
    * @return the specified number of unique ports for this machine
    */
    public static int[] generateUniquePorts(int cPort)
        {
        if (cPort < 1)
            {
            throw new IllegalArgumentException();
            }

        String sAddr;
        try
            {
            sAddr = InetAddress.getLocalHost().getHostAddress();
            }
        catch (UnknownHostException e)
            {
            sAddr = "127.0.0.1";
            }

        Random rnd   = new Random();
        String sPort = sAddr.split("\\.")[3];
        for (int i = sPort.length(); i < 4; ++i)
            {
            sPort = (rnd.nextInt(9) + 1) + sPort;
            }

        int   nPort  = Integer.valueOf(sPort);
        int[] anPort = new int[cPort];

        for (int i = 0; i < cPort; ++i)
            {
            anPort[i] = nPort++;
            }

        return anPort;
        }
    }
