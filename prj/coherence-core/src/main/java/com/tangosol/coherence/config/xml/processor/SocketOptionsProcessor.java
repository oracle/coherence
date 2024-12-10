/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.net.SocketOptions;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import java.net.SocketException;

import java.util.Map;

/**
 * An {@link ElementProcessor} for {@link SocketOptions}.
 *
 * @author bo  2013.07.10
 */
public class SocketOptionsProcessor
        implements ElementProcessor<SocketOptions>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public SocketOptions process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        SocketOptions socketOptions = new SocketOptions();

        try
            {
            XmlElement xmlOption;

            socketOptions.setOption(SocketOptions.SO_KEEPALIVE, Boolean.TRUE);
            socketOptions.setOption(SocketOptions.TCP_NODELAY, Boolean.TRUE);
            socketOptions.setOption(SocketOptions.SO_LINGER, Integer.valueOf(0));

            if ((xmlOption = xmlElement.getElement("reuse-address")) != null)
                {
                socketOptions.setOption(SocketOptions.SO_REUSEADDR, xmlOption.getBoolean(true));
                }

            if ((xmlOption = xmlElement.getElement("receive-buffer-size")) != null)
                {
                socketOptions.setOption(SocketOptions.SO_RCVBUF, (int) Base.parseMemorySize(xmlOption.getString()));
                }

            if ((xmlOption = xmlElement.getElement("send-buffer-size")) != null)
                {
                socketOptions.setOption(SocketOptions.SO_SNDBUF, (int) Base.parseMemorySize(xmlOption.getString()));
                }

            if ((xmlOption = xmlElement.getElement("timeout")) != null)
                {
                socketOptions.setOption(SocketOptions.SO_TIMEOUT, (int) Base.parseTime(xmlOption.getString()));
                }

            if ((xmlOption = xmlElement.getElement("linger-timeout")) != null)
                {
                socketOptions.setOption(SocketOptions.SO_LINGER, (int) (Base.parseTime(xmlOption.getString()) / 1000));
                }

            if ((xmlOption = xmlElement.getElement("keep-alive-enabled")) != null)
                {
                socketOptions.setOption(SocketOptions.SO_KEEPALIVE, xmlOption.getBoolean(true));
                }

            if ((xmlOption = xmlElement.getElement("out-of-band-inline")) != null)
                {
                socketOptions.setOption(SocketOptions.SO_OOBINLINE, xmlOption.getBoolean(true));
                }

            if ((xmlOption = xmlElement.getElement("tcp-delay-enabled")) != null)
                {
                socketOptions.setOption(SocketOptions.TCP_NODELAY, !xmlOption.getBoolean(true));
                }

            if ((xmlOption = xmlElement.getElement("traffic-class")) != null)
                {
                socketOptions.setOption(SocketOptions.IP_TOS, xmlOption.getInt());
                }

            return socketOptions;
            }
        catch (SocketException e)
            {
            throw new ConfigurationException("Illegal Socket Option defined in [" + xmlElement + "]",
                                             "Please ensure the Socket Options are valid", e);
            }
        }
    }
