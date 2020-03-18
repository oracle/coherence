/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.internal.net.service.peer.LegacyXmlPeerHelper;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * LegacyXmlAcceptorHelper parses XML to populate a AbstractAcceptorDependencies
 * object.
 *
 * @author pfm 2011.06.27
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlAcceptorHelper
    {
    /**
     * Populate the AbstractAcceptorDependencies object from the given XML configuration.
     *
     * @param xml     the XML parent element that contains the acceptor XML fragment
     * @param deps    the AbstractAcceptorDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultAcceptorDependencies object that was passed in
     */
    @SuppressWarnings({ })
    public static AbstractAcceptorDependencies fromXml(XmlElement xml,
            AbstractAcceptorDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlPeerHelper.fromXml(xml, deps, ctx, loader);

        // <connection-limit>
        deps.setConnectionLimit(xml.getSafeElement("connection-limit").getInt(
                deps.getConnectionLimit()));

        return deps;
        }

    /**
     * Parse the Acceptor XML and return an AcceptorDependencies object.
     *
     * @param xml     the Acceptor XML
     * @param ctx     the operational context
     * @param loader  the class loader for the current context
     *
     * @return the AcceptorDependencies
     */
    public static AcceptorDependencies createAcceptorDeps(XmlElement xml, OperationalContext ctx,
                                                          ClassLoader loader)
        {
        return createAcceptorDeps(xml, ctx, loader, null);
        }

    /**
     * Parse the Acceptor XML and return an AcceptorDependencies object.
     *
     * @param xml     the Acceptor XML
     * @param ctx     the operational context
     * @param loader  the class loader for the current context
     * @param deps    AcceptorDependencies class to use (may be null)
     *
     * @return the AcceptorDependencies
     *
     * @since Coherence 12.2.1
     */
    public static AcceptorDependencies createAcceptorDeps(XmlElement xml, OperationalContext ctx,
            ClassLoader loader, AcceptorDependencies deps)
        {
        XmlElement xmlConfig = xml.getElement("acceptor-config");

        // The xmlConfig should not be null; we inject it in pre-process XML
        // if one does not present (COH-6872). But check anyways.
        if (xmlConfig == null)
            {
            throw new IllegalArgumentException("the \"acceptor-config\" element is missing:\n" + xml);
            }

        // inject service configuration into acceptor xml
        XmlElement xmlHandler = XmlHelper.ensureElement(xmlConfig,
                "incoming-message-handler");

        XmlElement xmlSub = XmlHelper.ensureElement(xmlHandler, "thread-count");
        if (xmlSub.getValue() == null)
            {
            xmlSub.setInt(xml.getSafeElement("thread-count").getInt());
            }
        xmlSub = XmlHelper.ensureElement(xmlHandler, "thread-count-max");
        if (xmlSub.getValue() == null)
            {
            xmlSub.setInt(xml.getSafeElement("thread-count-max").getInt(Integer.MAX_VALUE));
            }
        xmlSub = XmlHelper.ensureElement(xmlHandler, "thread-count-min");
        if (xmlSub.getValue() == null)
            {
            xmlSub.setInt(xml.getSafeElement("thread-count-min").getInt(1));
            }
        xmlSub = XmlHelper.ensureElement(xmlHandler, "task-hung-threshold");
        if (xmlSub.getValue() == null)
            {
            xmlSub.setString(xml.getSafeElement("task-hung-threshold").getString());
            }
        xmlSub = XmlHelper.ensureElement(xmlHandler, "task-timeout");
        if (xmlSub.getValue() == null)
            {
            xmlSub.setString(xml.getSafeElement("task-timeout").getString());
            }

        // create the Acceptor dependencies
        if (xmlConfig.getElement("http-acceptor") != null)
            {
            return LegacyXmlHttpAcceptorHelper.fromXml(xmlConfig, deps == null
                    ? new DefaultHttpAcceptorDependencies()
                    : (DefaultHttpAcceptorDependencies) deps, ctx, loader);
            }
        else if (xmlConfig.getElement("jms-acceptor") != null)
            {
            return LegacyXmlJmsAcceptorHelper.fromXml(xmlConfig, deps == null
                    ? new DefaultJmsAcceptorDependencies()
                    : (DefaultJmsAcceptorDependencies) deps, ctx, loader);
            }
        else if (xmlConfig.getElement("memcached-acceptor") != null)
            {
            return LegacyXmlMemcachedAcceptorHelper.fromXml(xmlConfig, deps == null
                    ? new DefaultMemcachedAcceptorDependencies()
                    : (DefaultMemcachedAcceptorDependencies) deps, ctx, loader);
            }
        else // assume tcp-acceptor
            {
            return LegacyXmlTcpAcceptorHelper.fromXml(xmlConfig, deps == null
                    ? new DefaultTcpAcceptorDependencies()
                    : (DefaultTcpAcceptorDependencies) deps, ctx, loader);
            }
        }
    }
