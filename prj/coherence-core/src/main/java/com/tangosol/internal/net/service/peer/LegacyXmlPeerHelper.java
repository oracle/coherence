/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer;

import com.tangosol.internal.net.LegacyXmlConfigHelper;
import com.tangosol.internal.net.service.LegacyXmlServiceHelper;

import com.tangosol.io.WrapperStreamFactory;

import com.tangosol.net.OperationalContext;
import com.tangosol.net.messaging.Codec;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ImmutableArrayList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * LegacyXmlPeerHelper parses XML to populate a DefaultPeerDependencies object.
 *
 * @author pfm 2011.05.08
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlPeerHelper
    {
    /**
     * Populate the DefaultPeerDependencies object from the given XML configuration.
     *
     * @param xml     the XML parent element that contains the child Peer elements
     * @param deps    the DefaultPeerDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultPeerDependencies object that was passed in
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static DefaultPeerDependencies fromXml(XmlElement xml,
            DefaultPeerDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlServiceHelper.fromXml(xml, deps, ctx);

        // <outgoing-message-handler>
        XmlElement xmlCat = xml.getSafeElement("outgoing-message-handler");

        // <request-timeout>
        deps.setRequestTimeoutMillis(XmlHelper.parseTime(xmlCat, "request-timeout",
                deps.getRequestTimeoutMillis()));

        // <heartbeat-timeout> - this defaults to the request timeout.
        deps.setPingTimeoutMillis(XmlHelper.parseTime(xmlCat, "heartbeat-timeout",
                deps.getRequestTimeoutMillis()));

        // <heartbeat-interval>
        deps.setPingIntervalMillis(XmlHelper.parseTime(xmlCat, "heartbeat-interval",
                deps.getPingIntervalMillis()));

        // <max-message-size>
        XmlElement xmlSub = xmlCat.getElement("max-message-size");
        if (xmlSub != null)
            {
            deps.setMaxOutgoingMessageSize((int) Base.parseMemorySize(xmlSub.getString()));
            }

        // <incoming-message-handler>
        xmlCat = xml.getSafeElement("incoming-message-handler");

        int cThreads = xmlCat.getSafeElement("thread-count").getInt();
        if (cThreads > 0)
            {
            deps.setWorkerThreadCount(cThreads);

            long cHungMillis = XmlHelper.parseTime(xmlCat, "task-hung-threshold", 0L);
            if (cHungMillis > 0L)
                {
                deps.setTaskHungThresholdMillis(cHungMillis);
                }

            long cTimeoutMillis = XmlHelper.parseTime(xmlCat, "task-timeout", 0L);
            if (cTimeoutMillis > 0L)
                {
                deps.setTaskTimeoutMillis(cTimeoutMillis);
                }
            }

        // <max-message-size>
        xmlSub = xmlCat.getElement("max-message-size");
        if (xmlSub != null)
            {
            deps.setMaxIncomingMessageSize((int) Base.parseMemorySize(xmlSub.getString()));
            }

        long cRequestTimeout = XmlHelper.parseTime(xmlCat, "request-timeout", 0L);
        if (cRequestTimeout > 0L)
            {
            deps.setRequestTimeoutMillis(cRequestTimeout);
            }

        // <use-filters>
        Map  mapFilter = ctx.getFilterMap();
        List<WrapperStreamFactory> listFactory    = new ArrayList<WrapperStreamFactory>();
        List<String>               listUseFilters = LegacyXmlConfigHelper.parseFilterList(xml);

        for (Iterator<String> iter = listUseFilters.iterator(); iter.hasNext(); )
            {
            String sName  =  iter.next().trim();
            WrapperStreamFactory filter = (WrapperStreamFactory) mapFilter.get(sName);
            if (filter == null)
                {
                throw new IllegalArgumentException("invalid filter-name " + sName
                        + " in use-filters:\n" + xmlCat);
                }
            listFactory.add(filter);
            }
        if (!listFactory.isEmpty())
            {
            deps.setFilterList(new ImmutableArrayList(listFactory));
            }

        // <message-codec>
        xmlCat = xml.getElement("message-codec");
        if (xmlCat != null)
            {
            deps.setMessageCodec((Codec) XmlHelper.createInstance(xmlCat,
                    loader, /*resolver*/ null, Codec.class));
            }

        return deps;
        }
    }
