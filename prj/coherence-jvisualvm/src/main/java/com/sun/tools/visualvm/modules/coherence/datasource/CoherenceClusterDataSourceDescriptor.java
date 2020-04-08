/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.datasource;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.modules.coherence.Localization;
import com.sun.tools.visualvm.modules.coherence.VisualVMView;

/**
 * The {@link DataSourceDescriptor} for {@link CoherenceClustersDataSource}.
 *
 * @author shyaradh 10/12/17
 *
 * @since Coherence 12.2.1.4.0
 */
public class CoherenceClusterDataSourceDescriptor
        extends DataSourceDescriptor<CoherenceClusterDataSource>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link CoherenceClusterDataSourceDescriptor} for the provided data source.
     *
     * @param dataSource  the {@link CoherenceClusterDataSource}
     */
    public CoherenceClusterDataSourceDescriptor(CoherenceClusterDataSource dataSource)
        {
        super(dataSource, dataSource.getName(),
                Localization.getLocalText("TXT_Coherence_Data_Source"),
                VisualVMView.NODE_ICON,
                POSITION_AT_THE_END,
                EXPAND_NEVER);
        }

    /**
     * Returns true if the General properties section should be available for
     * the DataSource, false otherwise.
     *
     * @return true if the General properties section should be available for
     * the DataSource, false otherwise
     *
     * @since VisualVM 1.2
     */
    @Override
    public boolean providesProperties()
        {
        return true;
        }
    }
