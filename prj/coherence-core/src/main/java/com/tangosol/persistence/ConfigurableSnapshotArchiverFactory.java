/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.tangosol.io.FileHelper;

import com.tangosol.run.xml.XmlConfigurable;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

import java.io.File;

/**
 * A {@link SnapshotArchiverFactory} implementation that creates instances of
 * a SnapshotArchiver class configured using an XmlElement.
 *
 * @since 12.2.1
 * @author jh  2014.08.15
 */
public class ConfigurableSnapshotArchiverFactory
        implements SnapshotArchiverFactory, XmlConfigurable
    {

    // ----- SnapshotArchiverFactory interface ------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public SnapshotArchiver createSnapshotArchiver(String sClusterName, String sServiceName)
        {
        // validate the cluster name and create a filesystem-safe version
        if (sClusterName == null || (sClusterName = sClusterName.trim()).length() == 0)
            {
            throw new IllegalArgumentException("invalid cluster name");
            }
        final String sCluster = FileHelper.toFilename(sClusterName);

        // validate the service name and create a filesystem-safe version
        if (sServiceName == null || (sServiceName = sServiceName.trim()).length() == 0)
            {
            throw new IllegalArgumentException("invalid service name");
            }
        final String sService = FileHelper.toFilename(sServiceName);

        // validate configuration
        XmlElement xmlConfig = getConfig();
        if (xmlConfig == null)
            {
            throw new IllegalStateException("missing configuration");
            }

        SnapshotArchiver archiver = null;

        // create the archiver
        boolean fCustomArchiver = false;
        try
            {
            if (xmlConfig.getName().equals("directory-archiver"))
                {
                // parse the archive directory configuration
                String sDirectory = xmlConfig.getSafeElement("archive-directory").getString().trim();
                final File fileArchive = new File(new File(new File(sDirectory), sCluster), sService);
                archiver = new DirectorySnapshotArchiver(sClusterName, sServiceName, fileArchive);
                }
            else if (xmlConfig.getName().equals("custom-archiver"))
                {
                fCustomArchiver = true;
                archiver = (SnapshotArchiver) XmlHelper.createInstance(xmlConfig,
                        Base.getContextClassLoader(),
                        new XmlHelper.ParameterResolver()
                            {
                            @Override
                            public Object resolveParameter(String sType, String sValue)
                                {
                                switch (sValue)
                                    {
                                    case "{cluster-name}":
                                        return sCluster;
                                    case "{service-name}":
                                        return sService;
                                    default:
                                        // return value as this will be the URI for custom archiver
                                        return sValue;
                                    }
                                }
                            },
                        SnapshotArchiver.class);
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e,
                    "Error creating " + (fCustomArchiver ? "custom" : "")
                  + " SnapshotArchiver with config: " + xmlConfig);
            }

        if (archiver == null)
            {
            throw new IllegalArgumentException("unknown snapshot archiver type: "
                    + xmlConfig);
            }

        return archiver;
        }

    // ----- XmlConfigurable ------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfig(XmlElement xml)
        {
        m_xmlConfig = xml;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public XmlElement getConfig()
        {
        return m_xmlConfig;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "ConfigurableSnapshotArchiverFactory";
        }

    // ---- data members ----------------------------------------------------

    /**
     * XML configuration for this ConfigurableSnapshotArchiverFactory.
     */
    private XmlElement m_xmlConfig;
    }
