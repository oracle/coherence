/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.datasource;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.model.AbstractModelProvider;

/**
 * The Coherence Clusters {@link DataSource}. The data source is a single
 * data source as there is only one Coherence Clusters section on the lhs of
 * JVisualVM.
 *
 * @author sr 12.10.2017
 *
 * @since Coherence 12.2.1.4.0
 */
public class CoherenceClustersDataSource
        extends DataSource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    private CoherenceClustersDataSource()
        {
        DataSourceDescriptorFactory.getDefault().registerProvider(
                new AbstractModelProvider<DataSourceDescriptor,DataSource>() {
                public DataSourceDescriptor createModelFor(DataSource ds) {
                if (CoherenceClustersDataSource.sharedInstance().equals(ds))
                    {
                    return new CoherenceClustersDataSourceDescriptor();
                    }
                else
                    {
                    return null;
                    }
                }
                }
        );
        }

    // ----- CoherenceClustersDataSource methods ----------------------------

    /**
     * Return the singleton instance of {@link CoherenceClustersDataSource}.
     *
     * @return the CoherenceClustersDataSource
     */
    public static synchronized CoherenceClustersDataSource sharedInstance()
        {
        if (s_sharedInstance == null)
            {
            s_sharedInstance = new CoherenceClustersDataSource();
            }
        return s_sharedInstance;
        }

    /**
     * Register the {@link CoherenceClustersDataSource} to the repository.
     */
    public static void register()
        {
        DataSource.ROOT.getRepository().addDataSource(sharedInstance());
        }

    /**
     * Unregister the {@link CoherenceClustersDataSource} from the repository.
     */
    public static void unregister()
        {
        DataSource.ROOT.getRepository().removeDataSource(sharedInstance());
        }
    // ----- data members ---------------------------------------------------

    /**
     * The singleton instance of {@link CoherenceClustersDataSource}.
     */
    private static CoherenceClustersDataSource s_sharedInstance;
    }
