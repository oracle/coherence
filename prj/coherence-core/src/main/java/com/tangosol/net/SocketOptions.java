/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;


import com.oracle.coherence.common.net.SocketSettings;
import com.oracle.coherence.common.net.Sockets;

import com.tangosol.run.xml.XmlConfigurable;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.util.Map;


/**
* SocketOptions provides a means to configure the various aspects of Sockets.
* Unlike java.net.SocketOptions, unset options will result in a value of
* null when queried via getOption.
*
* @author mf  2010.05.20
* @since Coherence 3.6
*/
public class SocketOptions
    extends SocketSettings implements XmlConfigurable
    {
    // ----- SocketOptions methods ------------------------------------------

    /**
    * Return true iff the XmlSocketOptions have been configured with any options.
    *
    * @return true iff the XmlSocketOptions have been configured with any options.
    */
    public boolean isConfigured()
        {
        return !f_mapOptions.isEmpty();
        }

    /**
    * Configure the specified socket.
    *
    * @param socket  the socket to configure
    *
    * @throws SocketException if an I/O error occurs
    */
    public void apply(ServerSocket socket)
            throws SocketException
        {
        apply(this, socket);
        }

    /**
    * Configure the specified socket.
    *
    * @param socket  the socket to configure
    *
    * @throws SocketException if an I/O error occurs
    */
    public void apply(Socket socket)
            throws SocketException
        {
        apply(this, socket);
        }

    /**
    * Configure the specified socket.
    *
    * @param socket  the socket to configure
    *
    * @throws SocketException if an I/O error occurs
    */
    public void apply(DatagramSocket socket)
            throws SocketException
        {
        apply(this, socket);
        }

    /**
    * Configure the specified socket.
    *
    * @param socket  the socket to configure
    *
    * @throws SocketException if an I/O error occurs
    */
    public void apply(MulticastSocket socket)
            throws SocketException
        {
        apply((DatagramSocket) socket);
        }


    // ----- XmlConfigurable methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    @Deprecated
    public XmlElement getConfig()
        {
        return m_xml;
        }

    /**
    * {@inheritDoc}
    */
    @Deprecated
    public void setConfig(XmlElement xml)
        {
        // Note: while support is provided for setting all socket options
        // available in 1.5, but we only list a subset of commonly used ones
        // in the XSD to keep things simple

        if (m_xml != null)
            {
            throw new IllegalStateException("already configured");
            }
        else if (xml == null)
            {
            throw new IllegalArgumentException("Missing configuration");
            }

        XmlElement xmlSub;
        Map        map = f_mapOptions;
        if ((xmlSub = xml.getElement("reuse-address")) != null)
            {
            map.put(SO_REUSEADDR, xmlSub.getBoolean(true));
            }

        if ((xmlSub = xml.getElement("receive-buffer-size")) != null)
            {
            map.put(SO_RCVBUF, (int) Base.parseMemorySize(xmlSub.getString()));
            }

        if ((xmlSub = xml.getElement("send-buffer-size")) != null)
            {
            map.put(SO_SNDBUF, (int) Base.parseMemorySize(xmlSub.getString()));
            }

        if ((xmlSub = xml.getElement("timeout")) != null)
            {
            map.put(SO_TIMEOUT, (int) Base.parseTime(xmlSub.getString()));
            }

        if ((xmlSub = xml.getElement("linger-timeout")) != null)
            {
            map.put(SO_LINGER, (int) (Base.parseTime(xmlSub.getString()) / 1000));
            }

        if ((xmlSub = xml.getElement("keep-alive-enabled")) != null)
            {
            map.put(SO_KEEPALIVE, xmlSub.getBoolean(true));
            }

        if ((xmlSub = xml.getElement("out-of-band-inline")) != null)
            {
            map.put(SO_OOBINLINE, xmlSub.getBoolean(true));
            }

        if ((xmlSub = xml.getElement("tcp-delay-enabled")) != null)
            {
            map.put(TCP_NODELAY, !xmlSub.getBoolean(true));
            }

        if ((xmlSub = xml.getElement("traffic-class")) != null)
            {
            map.put(IP_TOS, xmlSub.getInt());
            }

        m_xml = xml;
        }

    /**
    * Copy the options in the specified SocketOptions into this SocketOptions.
    *
    * @param options  the options to copy
    *
    * @throws SocketException  if an error occurs
    */
    public void copyOptions(java.net.SocketOptions options)
            throws SocketException
        {
        setOptions(options);
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Parse the supplied XML and return the corresponding SocketOptions.
    *
    * @param xml  the xml configuration
    *
    * @return the options, or null if none were identified
    */
    @Deprecated
    public static SocketOptions load(XmlElement xml)
        {
        SocketOptions options = new SocketOptions();
        options.setConfig(xml);
        return options.isConfigured() ? options : null;
        }

    /**
    * Apply the specified options to a socket.
    *
    * @param options  the options to apply
    * @param socket   the socket to configure
    *
    * @throws SocketException if an I/O error occurs
    */
    public static void apply(java.net.SocketOptions options, ServerSocket socket)
            throws SocketException
        {
        Sockets.configure(socket, options);
        }

    /**
    * Apply the specified options to a socket.
    *
    * @param options  the options to apply
    * @param socket   the socket to configure
    *
    * @throws SocketException if an I/O error occurs
    */
    public static void apply(java.net.SocketOptions options, Socket socket)
            throws SocketException
        {
        Sockets.configure(socket, options);
        }

    /**
    * Apply the specified options to a socket.
    *
    * @param options  the options to apply
    * @param socket   the socket to configure
    *
    * @throws SocketException if an I/O error occurs
    */
    public static void apply(java.net.SocketOptions options, DatagramSocket socket)
            throws SocketException
        {
        Sockets.configure(socket, options);
        }

    /**
    * Apply the specified options to a socket.
    *
    * @param options  the options to apply
    * @param socket   the socket to configure
    *
    * @throws SocketException if an I/O error occurs
    */
    public void apply(java.net.SocketOptions options, MulticastSocket socket)
            throws SocketException
        {
        apply(options, (DatagramSocket) socket);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The configuration.
    */
    protected XmlElement m_xml;
    }
