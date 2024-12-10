/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;

/**
 * LegacyXmlReplicatedCacheHelper parses the XML to populate the
 * DefaultReplicatedCacheDependencies.
 *
 * @author pfm 2011.07.07
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlReplicatedCacheHelper
    {
    /**
     * Populate the DefaultReplicatedCacheDependencies object from the XML DOM.
     *
     * @param xml     the XML parent element that contains the replicated cache elements
     * @param deps    the DefaultReplicatedCacheDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultReplicatedCacheDependencies object that was passed in
     */
    public static DefaultReplicatedCacheDependencies fromXml(XmlElement xml,
            DefaultReplicatedCacheDependencies deps, OperationalContext ctx,
            ClassLoader loader)
        {
        LegacyXmlGridHelper.fromXml(xml, deps, ctx, loader);

        if (xml == null)
            {
            throw new IllegalArgumentException("XML argument cannot be null");
            }

        long cLeaseMillis = xml.getSafeElement("standard-lease-milliseconds")
            .getLong(deps.getStandardLeaseMillis());
        if (cLeaseMillis >= 0)
            {
            deps.setStandardLeaseMillis(cLeaseMillis);
            }

        String sLeaseGranularity = xml.getSafeElement("lease-granularity").getString("thread");
        deps.setLeaseGranularity(
            sLeaseGranularity.equals("member") ? LeaseConfig.LEASE_BY_MEMBER
                                               : LeaseConfig.LEASE_BY_THREAD);

        // compatibility parameter for pre-Coherence 3.6 lease issue behavior
        deps.setMobileIssues(xml.getSafeElement("mobile-issues")
            .getBoolean(deps.isMobileIssues()));

        deps.setGraveyardSize(xml.getSafeElement("graveyard-size")
            .getInt(deps.getGraveyardSize()));

        deps.setEnsureCacheTimeoutMillis(xml.getSafeElement("ensure-cache-timeout")
            .getLong(deps.getEnsureCacheTimeoutMillis()));

        return deps;
        }
    }
