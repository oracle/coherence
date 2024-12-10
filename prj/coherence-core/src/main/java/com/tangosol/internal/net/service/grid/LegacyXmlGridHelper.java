/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.internal.net.LegacyXmlConfigHelper;

import com.tangosol.internal.net.cluster.LegacyXmlConfigurableQuorumPolicy;

import com.tangosol.internal.net.service.LegacyXmlServiceHelper;

import com.tangosol.net.MemberListener;
import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * LegacyXmlGridHelper parses the XML to populate the DefaultGridDependencies.
 *
 * @author pfm 2011.05.08
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlGridHelper
    {
    /**
     * Populate the DefaultGridDependencies object from the given XML configuration.
     *
     * @param xml     the XML parent element that contains the child grid elements
     * @param deps    the DefaultGridDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the grid dependencies object
     */
    public static DefaultGridDependencies fromXml(XmlElement xml, DefaultGridDependencies deps,
            OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlServiceHelper.fromXml(xml, deps, ctx);

        if (xml == null)
            {
            throw new IllegalArgumentException("XML argument cannot be null");
            }

        // configure the action-policy
        XmlElement xmlPolicy = xml.getSafeElement("partitioned-quorum-policy-scheme");
        deps.setActionPolicyBuilder(new LegacyXmlConfigurableQuorumPolicy().createPolicyBuilder(
                            xmlPolicy, ctx, loader));

        XmlElement xmlListener = xml.getSafeElement("member-listener");
        if (!XmlHelper.isInstanceConfigEmpty(xmlListener))
            {
            List<ParameterizedBuilder<MemberListener>> listeners = new ArrayList<>(1);
            listeners.add(LegacyXmlConfigHelper.createBuilder(xmlListener, MemberListener.class));
            deps.setMemberListenerBuilders(listeners);
            }

        deps.setDefaultGuardTimeoutMillis(
            XmlHelper.parseTime(xml, "guardian-timeout", deps.getDefaultGuardTimeoutMillis()));

        deps.setServiceFailurePolicyBuilder(LegacyXmlConfigHelper.parseServiceFailurePolicyBuilder(
            xml.getSafeElement("service-failure-policy")));

        deps.setReliableTransport(xml.getSafeElement("reliable-transport").getString());

        return deps;
        }
    }
