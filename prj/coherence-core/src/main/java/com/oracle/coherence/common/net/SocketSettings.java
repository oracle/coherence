/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import java.util.Map;
import java.util.HashMap;
import java.net.SocketException;
import java.net.SocketOptions;


/**
* SocketSettings provides a means to configure the various aspects of
* Sockets. Unlike java.net.SocketOptions, unset options will result in a value
* of null when queried via getOption.
*
* @author mf  2010.05.20
*/
public class SocketSettings
    implements SocketOptions
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an empty SocketOptions configuration.
     */
    public SocketSettings()
        {
        }

    /**
     * Construct a SocketOptions configuration based upon the supplied options.
     *
     * @param options the options to copy
     *
     * @throws IllegalArgumentException on error
     */
    public SocketSettings(SocketOptions options)
        {
        setOptions(options);
        }


    // ----- SocketOptions methods ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void setOption(int optID, Object value)
            throws SocketException
        {
        f_mapOptions.put(optID, value);
        }

    /**
    * {@inheritDoc}
    */
    public Object getOption(int optID)
            throws SocketException
        {
        return f_mapOptions.get(optID);
        }

    /**
    * Set any options indicated by the supplied SocketOptions into this
    * SocketOptions.
    *
    * @param options  the options to set
    *
    * @throws IllegalArgumentException on error
    */
    public void setOptions(SocketOptions options)
        {
        try
            {
            if (options != null)
                {
                Map map = f_mapOptions;
                for (int nOp : new int[] {
                        TCP_NODELAY,
                        SO_REUSEADDR,
                        SO_BROADCAST,
                        IP_TOS,
                        SO_LINGER,
                        SO_TIMEOUT,
                        SO_SNDBUF,
                        SO_RCVBUF,
                        SO_KEEPALIVE,
                        SO_OOBINLINE})
                    {
                    Object oVal = options.getOption(nOp);
                    if (oVal != null)
                        {
                        map.put(nOp, oVal);
                        }
                    }
                }
            }
        catch (SocketException e)
            {
            throw new IllegalArgumentException(e);
            }
        }

    /**
     * Set the specified option.
     *
     * @param optID  the option id
     * @param value  the option value
     *
     * @return this object
     *
     * @throws IllegalArgumentException on error
     */
    public SocketSettings set(int optID, Object value)
        {
        try
            {
            setOption(optID, value);
            return this;
            }
        catch (SocketException e)
            {
            throw new IllegalArgumentException(e);
            }
        }

    /**
     * Return the specified option.
     *
     * @param optID  the option id
     *
     * @return the option value
     *
     * @throws IllegalArgumentException on error
     */
    public Object get(int optID)
        {
        try
            {
            return getOption(optID);
            }
        catch (SocketException e)
            {
            throw new IllegalArgumentException(e);
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        Map          map     = f_mapOptions;
        StringBuffer sb      = new StringBuffer("SocketOptions{");
        String       sDelim  = "";
        String       sComma  = ", ";
        Object       oOption;

        if ((oOption = map.get(SO_REUSEADDR)) != null)
            {
            sb.append(sDelim).append("ReuseAddress=").append(oOption);
            sDelim = sComma;
            }
        if ((oOption = map.get(SO_RCVBUF)) != null)
            {
            sb.append(sDelim).append("ReceiveBufferSize=").append(oOption);
            sDelim = sComma;
            }
        if ((oOption = map.get(SO_SNDBUF)) != null)
            {
            sb.append(sDelim).append("SendBufferSize=").append(oOption);
            sDelim = sComma;
            }
        if ((oOption = map.get(SO_TIMEOUT)) != null)
            {
            sb.append(sDelim).append("Timeout=").append(oOption);
            sDelim = sComma;
            }
        if ((oOption = map.get(SO_LINGER)) != null)
            {
            sb.append(sDelim).append("LingerTimeout=").append(oOption);
            sDelim = sComma;
            }
        if ((oOption = map.get(SO_KEEPALIVE)) != null)
            {
            sb.append(sDelim).append("KeepAlive=").append(oOption);
            sDelim = sComma;
            }
        if ((oOption = map.get(TCP_NODELAY)) != null)
            {
            sb.append(sDelim).append("TcpNoDelay=").append(oOption);
            sDelim = sComma;
            }
        if ((oOption = map.get(IP_TOS)) != null)
            {
            sb.append(sDelim).append("TrafficClass=").append(oOption);
            // sDelim = sComma;
            }

        return sb.append('}').toString();
        }


    // ----- data members ---------------------------------------------------

    /**
    * A map of the specified options.
    */
    protected final Map f_mapOptions = new HashMap();
    }
