/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.coherence.config.Config;

import com.tangosol.io.BinaryDeltaCompressor;
import com.tangosol.io.DecoratedBinaryDeltaCompressor;
import com.tangosol.io.DecorationOnlyDeltaCompressor;
import com.tangosol.io.DeltaCompressor;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

/**
 * LegacyXmlPartitionedCacheHelper parses the XML to populate the
 * DefaultPartitionedCacheDependencies.
 *
 * @author pfm 2011.05.08
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlPartitionedCacheHelper
    {
    /**
     * Populate the DefaultPartitionedCacheDependencies object from the given
     * XML configuration.
     *
     * @param xml     the XML parent element that contains the child partitioned cache elements
     * @param deps    the DefaultPartitionedCacheDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the service dependencies object
     */
    public static DefaultPartitionedCacheDependencies fromXml(XmlElement xml,
            DefaultPartitionedCacheDependencies deps, OperationalContext ctx,
            ClassLoader loader)
        {
        LegacyXmlPartitionedServiceHelper.fromXml(xml, deps, ctx, loader);

        if (xml == null)
            {
            throw new IllegalArgumentException("XML argument cannot be null");
            }

        deps.setBackupCountAfterWriteBehind(xml.getSafeElement("backup-count-after-writebehind")
                .getInt(deps.getPreferredBackupCount()));

        deps.setStandardLeaseMillis(xml.getSafeElement("standard-lease-milliseconds")
                .getLong(deps.getStandardLeaseMillis()));

        String sLeaseGranularity = xml.getSafeElement("lease-granularity").getString("");
        if (sLeaseGranularity.equals("member"))
            {
            deps.setLeaseGranularity(PartitionedCacheDependencies.LEASE_BY_MEMBER);
            }
        else if (sLeaseGranularity.equals("thread"))
            {
            deps.setLeaseGranularity(PartitionedCacheDependencies.LEASE_BY_THREAD);
            }

        // currently undocumented delta-compressor class
        DeltaCompressor dc;
        XmlElement      xmlCompressor = xml.getSafeElement("compressor");
        String          sCompressor   = xmlCompressor.getString("");

        if (sCompressor.equalsIgnoreCase("standard"))
            {
            // COH-5528 workaround: use the plain binary delta compressor even
            //          for POF until the performance regression is solved.
            dc = new DecoratedBinaryDeltaCompressor(new BinaryDeltaCompressor());

            // COH-5250: create a "dummy" serializer to check for POF
            // ClassLoader         loader     = Base.getContextClassLoader();
            // SerializerFactory   factory    = deps.getSerializerFactory();
            // Serializer          serializer = factory == null
            //    ? ExternalizableHelper.ensureSerializer(loader) : factory.createSerializer(loader);
            //
            // dc = new DecoratedBinaryDeltaCompressor(serializer instanceof PofContext
            //    ? ExternalizableHelper.getDeltaCompressor(serializer, new PofDeltaCompressor())
            //    : new BinaryDeltaCompressor());
            }
        else if (!XmlHelper.isInstanceConfigEmpty(xmlCompressor))
            {
            dc = (DeltaCompressor) XmlHelper.createInstance(xmlCompressor,
                    Base.getContextClassLoader(), null);
            }
        else
            {
            dc = new DecorationOnlyDeltaCompressor();
            }
        deps.setDeltaCompressor(dc);

        // currently undocumented strict partitioning flag
        String sFlag = Config.getProperty("coherence.distributed.strict");
        if (sFlag != null)
            {
            deps.setStrictPartitioning(Boolean.valueOf(sFlag).booleanValue());
            }

        return deps;
        }
    }
