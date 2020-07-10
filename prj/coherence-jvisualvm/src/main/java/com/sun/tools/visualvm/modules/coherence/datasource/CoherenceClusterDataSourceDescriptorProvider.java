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
 * The {@link AbstractModelProvider} for {@link CoherenceClusterDataSourceDescriptor}.
 *
 * @author shyaradh 12.10.2017
 *
 * @since Coherence 12.2.1.4.0
 */
public class CoherenceClusterDataSourceDescriptorProvider
        extends AbstractModelProvider<DataSourceDescriptor, DataSource>
    {
    // ----- AbstractModelProvider methods ----------------------------------

    @Override
    public DataSourceDescriptor createModelFor(DataSource dataSource)
        {
        if (dataSource instanceof CoherenceClusterDataSource)
            {
            return new CoherenceClusterDataSourceDescriptor((CoherenceClusterDataSource) dataSource);
            }
        return null;
        }

    // ----- CoherenceClusterDataSourceDescriptorProvider methods --------

    /**
     * Register the provider with the factory.
     */
    public static void register()
        {
        DataSourceDescriptorFactory.getDefault().registerProvider(INSTANCE);
        }

    /**
     * Unregister the provider with the factory.
     */
    public static void unregister()
        {
        DataSourceDescriptorFactory.getDefault().unregisterProvider(INSTANCE);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The static singleton instance of {@link CoherenceClusterDataSourceDescriptorProvider}.
     */
    final private static CoherenceClusterDataSourceDescriptorProvider INSTANCE =
            new CoherenceClusterDataSourceDescriptorProvider();
    }
