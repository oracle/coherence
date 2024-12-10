/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.PartitionAssignmentStrategyBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.internal.net.LegacyXmlConfigHelper;

import com.tangosol.net.OperationalContext;

import com.tangosol.net.partition.KeyAssociator;
import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.partition.PartitionAssignmentStrategy;
import com.tangosol.net.partition.PartitionListener;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.util.LinkedList;
import java.util.List;

/**
 * LegacyXmlPartitionedServiceHelper parses the XML to populate the
 * DefaultPartitionedServiceDependencies.
 *
 * @author pfm 2011.05.08
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlPartitionedServiceHelper
    {
    /**
     * Populate the DefaultPartitionedServiceDependencies object from the given
     * XML configuration.
     *
     * @param xml     the XML parent element that contains the child service elements
     * @param deps    the DefaultPartitionedServiceDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the service dependencies object
     */
    public static DefaultPartitionedServiceDependencies fromXml(XmlElement xml,
        DefaultPartitionedServiceDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlGridHelper.fromXml(xml, deps, ctx, loader);

        if (xml == null)
            {
            throw new IllegalArgumentException("XML argument cannot be null");
            }

        // configure local-storage
        boolean fOwnership = xml.getSafeElement("local-storage").getBoolean(deps.isOwnershipCapable());

        deps.setOwnershipCapable(fOwnership);
        if (fOwnership)
            {
            // configure partition-listener which is only valid for local-storage enabled
            // members
            XmlElement xmlListener = xml.getSafeElement("partition-listener");
            if (!XmlHelper.isInstanceConfigEmpty(xmlListener))
                {
                List<ParameterizedBuilder<PartitionListener>> bldrsListener =
                    new LinkedList<ParameterizedBuilder<PartitionListener>>();

                bldrsListener.add(LegacyXmlConfigHelper.createBuilder(xmlListener, PartitionListener.class));
                deps.setPartitionListenerBuilders(bldrsListener);
                }

            // persistence is not supported when using DCCF
            deps.setPersistenceDependencies(null);
            }

        // configure async-backup
        XmlElement xmlAsyncBackup = xml.getSafeElement("async-backup");
        if (xmlAsyncBackup != null)
            {
            Boolean FAsync = xml.getBoolean();

            deps.setAsyncBackupInterval(
                    FAsync == null ? new Duration(xmlAsyncBackup.getString()) :
                    FAsync         ? new Duration(0L) : null);
            }

        // configure partition-count
        deps.setPreferredPartitionCount(xml.getSafeElement("partition-count").getInt(
            deps.getPreferredPartitionCount()));

        // configure transfer-threshold.  XML provides value in KB, whereas it is stored
        // internally as bytes
        XmlElement element = xml.getElement("transfer-threshold");
        if (element != null)
            {
            deps.setTransferThreshold(element.getInt());
            }

        // set backup-count
        deps.setPreferredBackupCount(xml.getSafeElement("backup-count").getInt(deps.getPreferredBackupCount()));

        // set key-associator
        XmlElement xmlAssociator = xml.getSafeElement("key-associator");
        if (!XmlHelper.isInstanceConfigEmpty(xmlAssociator))
            {
            deps.setKeyAssociator((KeyAssociator) LegacyXmlConfigHelper.createInstance(xmlAssociator, "KeyAssociator"));
            }

        // set key_partitioning strategy
        XmlElement xmlKPS = xml.getSafeElement("key-partitioning");
        if (!XmlHelper.isInstanceConfigEmpty(xmlKPS))
            {
            deps.setKeyPartitioningStrategy((KeyPartitioningStrategy) LegacyXmlConfigHelper.createInstance(xmlKPS,
                "KeyPartitioningStrategy"));
            }

        // configure the partition-assignment-strategy
        // if the partition assignment strategy is not legacy then instantiate one and set it.
        XmlElement xmlPAS = xml.getSafeElement("partition-assignment-strategy");
        String     sPAS   = xmlPAS.getString();

        if ("legacy".equals(sPAS))
            {
            throw new ConfigurationException("'legacy' is not a supported partition assignment strategy",
                    "Change the partition-assignment-strategy to a supported option.");
            }

        PartitionAssignmentStrategyBuilder builder = null;
        if ("simple".equals(sPAS) || sPAS.startsWith("mirror:"))
            {
            builder = new PartitionAssignmentStrategyBuilder(sPAS, xmlPAS);
            }
        else if (!XmlHelper.isInstanceConfigEmpty(xmlPAS))
            {
            builder = new PartitionAssignmentStrategyBuilder(
                    LegacyXmlConfigHelper.createBuilder(xmlPAS,
                            PartitionAssignmentStrategy.class), xmlPAS);
            }

        if (builder != null)
            {
            deps.setPartitionAssignmentStrategyBuilder(builder);
            }

        // currently undocumented aggressiveness factor
        String sFactor = Config.getProperty("coherence.distributed.aggressive");
        if (sFactor != null && sFactor.length() > 0)
            {
            try
                {
                deps.setDistributionAggressiveness(Integer.parseInt(sFactor));
                }
            catch (NumberFormatException e) {}
                }

        // currently undocumented distribution synchronization flag
        String sFlag = Config.getProperty("coherence.distributed.synchronize");
        if (sFlag != null)
            {
            deps.setDistributionSynchronized(Boolean.valueOf(sFlag).booleanValue());
            }

        return deps;
        }
    }
