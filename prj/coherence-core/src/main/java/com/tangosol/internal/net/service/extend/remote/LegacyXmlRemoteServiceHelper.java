/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;

import com.tangosol.internal.net.service.peer.initiator.DefaultJmsInitiatorDependencies;
import com.tangosol.internal.net.service.peer.initiator.DefaultTcpInitiatorDependencies;
import com.tangosol.internal.net.service.peer.initiator.InitiatorDependencies;
import com.tangosol.internal.net.service.peer.initiator.LegacyXmlJmsInitiatorHelper;
import com.tangosol.internal.net.service.peer.initiator.LegacyXmlTcpInitiatorHelper;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * LegacyXmlRemoteServiceHelper parses XML to populate a DefaultRemoteServiceDependencies
 * object.
 *
 * @author pfm 2011.09.05
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlRemoteServiceHelper
    {
    /**
     * Populate the DefaultRemoteServiceDependencies object from the given XML configuration.
     *
     * @param xml     the XML parent element that contains the RemoteService elements
     * @param deps    the DefaultRemoteServiceDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultRemoteServiceDependencies object that was passed in
     */
    public static DefaultRemoteServiceDependencies fromXml(XmlElement xml,
            DefaultRemoteServiceDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        if (xml == null)
            {
            throw new IllegalArgumentException("XML argument cannot be null");
            }

        deps.setRemoteServiceName(XmlHelper.ensureElement(xml, "proxy-service-name").getString());

        XmlElement xmlConfig;
        if (xml.getName().equals("initiator-config"))
            {
            xmlConfig = xml;
            }
        else
            {
            xmlConfig = xml.getSafeElement("initiator-config");
            }

        // inject service configuration into the initiator XML
        XmlElement xmlHandler = XmlHelper.ensureElement(xmlConfig,
                "incoming-message-handler");

        XmlElement xmlSub = XmlHelper.ensureElement(xmlHandler, "thread-count");
        if (xmlSub.getValue() == null)
            {
            xmlSub.setInt(xml.getSafeElement("thread-count").getInt(
                    deps.getWorkerThreadCount()));
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

        // create and populate either a JMS or TCP initiator dependencies object
        InitiatorDependencies depsInitiator;
        if (xmlConfig.getElement("jms-initiator") != null)
            {
            depsInitiator = LegacyXmlJmsInitiatorHelper.fromXml(xmlConfig,
                    new DefaultJmsInitiatorDependencies(), ctx, loader);
            }
        else if (xmlConfig.getElement("tcp-initiator") != null)
            {
            depsInitiator = LegacyXmlTcpInitiatorHelper.fromXml(xmlConfig,
                    new DefaultTcpInitiatorDependencies(), ctx, loader);
            }
        else
            {
            throw new IllegalArgumentException("the \"initiator-config\" element is either"
                    + " missing, empty, or does not contain a valid transport-specific"
                    + " child element:\n" + xml);
            }
        deps.setInitiatorDependencies(depsInitiator);

        return deps;
        }
    }
