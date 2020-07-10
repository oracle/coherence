/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.impl;

import com.sun.tools.visualvm.modules.coherence.datasource.CoherenceClustersDataSource;
import com.sun.tools.visualvm.modules.coherence.datasource.CoherenceClusterDataSource;

import java.io.File;
import java.io.FilenameFilter;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.Utils;

import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

/**
 * The provider for Coherence cluster data sources.
 *
 * @author sr 12.10.2017
 *
 * @since Coherence 12.2.1.4.0
 */
public class CoherenceClusterProvider
    {
    // ----- CoherenceClusterProvider methods -------------------------------

    /**
     * Create a Coherence cluster data source, with the provided management URL.
     *
     * @param sUrl          the management URL of the cluster
     * @param sClusterName  the name of the Coherence cluster
     */
    public static void createCoherenceClusterDataSource(String sUrl, String sClusterName)
        {
        RequestProcessor.getDefault().post(new Runnable()
            {
            public void run()
                {
                createCoherenceClusterDatasourceImpl(sUrl, sClusterName);
                }
            });
        }

    /**
     * Create a Coherence cluster data source and add it to the repository.
     *
     * @param sUrl          the management URL of the cluster
     * @param sClusterName  the name of the Coherence cluster
     */
    private static void createCoherenceClusterDatasourceImpl(String sUrl, String sClusterName)
        {
        final String[] propNames  = new String[]{APPLICATION_URL, CLUSTER_NAME};
        final String[] propValues = new String[]{sUrl, sClusterName};

        try
            {
            URL    url      = new URL(sUrl);
            String fileName = url.getHost() + "_" + url.getPort();

            File customPropertiesStorage =
                    Utils.getUniqueFile(getStorageDirectory(), fileName, Storage.DEFAULT_PROPERTIES_EXT);

            Storage storage = new Storage(customPropertiesStorage.getParentFile(), customPropertiesStorage.getName());

            CoherenceClusterDataSource dataSource = new CoherenceClusterDataSource(sUrl, sClusterName, storage);
            storage.setCustomProperties(propNames, propValues);

            CoherenceClustersDataSource.sharedInstance().getRepository().addDataSource(dataSource);
            }
        catch (MalformedURLException e)
            {
            LOGGER.log(Level.SEVERE, "Error creating coherence cluster data source", e);
            return;
            }
        }

    /**
     * Initialize the Coherence cluster data source st JVisualVM startup. This included search the storage
     * directory and loading the data sources into the repository.
     */
    public static void initCoherenceClustersDataSource()
        {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable()
            {
            public void run()
                {
                RequestProcessor.getDefault().post(new Runnable()
                    {
                    public void run()
                        {
                        initCoherenceClustersDataSourcesFromStorage();
                        }
                    });
                }
            });
        }

    /**
     * Initialize the data sources from storage.
     */
    private static void initCoherenceClustersDataSourcesFromStorage()
        {
        if (!storageDirectoryExists())
            {
            return;
            }

        File[] files = getStorageDirectory().listFiles(new FilenameFilter()
            {
            public boolean accept(File dir, String name)
                {
                return name.endsWith(Storage.DEFAULT_PROPERTIES_EXT);
                }
            });

        Set<CoherenceClusterDataSource> coherenceClusters = new HashSet();
        for (File file : files)
            {
            Storage  storage    = new Storage(file.getParentFile(), file.getName());
            String[] propNames  = new String[]{APPLICATION_URL, CLUSTER_NAME};
            String[] propValues = storage.getCustomProperties(propNames);

            if (propValues != null && propValues.length == 2)
                {
                try
                    {
                    coherenceClusters.add(new CoherenceClusterDataSource(propValues[0], propValues[1], storage));
                    }
                catch (Exception e)
                    {
                    LOGGER.log(Level.INFO, "Error loading persisted coherence cluster", e);
                    }
                }
            }

        if (!coherenceClusters.isEmpty())
            {
            CoherenceClustersDataSource.sharedInstance().getRepository().addDataSources(coherenceClusters);
            }
        }

    /**
     * Returns true if the storage directory for coherence clusters data source already exists, false otherwise.
     *
     * @return true if the storage directory for coherence clusters data source already exists, false otherwise
     */
    public static boolean storageDirectoryExists()
        {
        return new File(getStorageDirectoryString()).isDirectory();
        }


    /**
     * Returns storage directory for coherence clusters data source.
.     *
     * @return storage directory for coherence clusters data source
     */
    public static File getStorageDirectory()
        {
        if (s_coherenceClustersStorageDirectory == null)
            {
            String snapshotsStorageString = getStorageDirectoryString();
            s_coherenceClustersStorageDirectory = new File(snapshotsStorageString);
            if (s_coherenceClustersStorageDirectory.exists() && s_coherenceClustersStorageDirectory.isFile())
                {
                throw new IllegalStateException("Cannot create Coherence clusters storage directory "
                        + snapshotsStorageString + ", file in the way");
                }
            if (s_coherenceClustersStorageDirectory.exists() && (!s_coherenceClustersStorageDirectory.canRead()
                    || !s_coherenceClustersStorageDirectory.canWrite()))
                {
                throw new IllegalStateException("Cannot access Coherence clusters storage directory "
                        + snapshotsStorageString + ", read&write permission required");
                }
            if (!Utils.prepareDirectory(s_coherenceClustersStorageDirectory))
                {
                throw new IllegalStateException("Cannot create Coherence clusters storage directory "
                        + snapshotsStorageString);
                }
            }
        return s_coherenceClustersStorageDirectory;
        }

    /**
     * The storage directory for Coherence clusters data source.
     *
     * @return the storage directory for Coherence clusters data source
     */
    static String getStorageDirectoryString()
        {
        return Storage.getPersistentStorageDirectoryString()
                + File.separator + COHERENCE_CLUSTERS_STORAGE_DIRNAME;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The constant for the storage directory of Coherence data sources.
     */
    private static final String COHERENCE_CLUSTERS_STORAGE_DIRNAME = "coherenceclusters";    // NOI18N

    /**
     * The key for the application URL.
     */
    private static final String APPLICATION_URL = "application_url";

    /**
     * The key for the Coherence cluster name.
     */
    private static final String CLUSTER_NAME = "cluster_name";

    // ----- data members ---------------------------------------------------

    /**
     * The coherence clusters data source directory.
     */
    private static File s_coherenceClustersStorageDirectory;

    /**
     * The logger to use.
     */
    private static final Logger LOGGER = Logger.getLogger(CoherenceClusterProvider.class.getName());
    }
