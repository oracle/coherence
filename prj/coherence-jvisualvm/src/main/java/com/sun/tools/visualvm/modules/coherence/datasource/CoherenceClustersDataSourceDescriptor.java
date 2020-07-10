/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.datasource;

import com.sun.tools.visualvm.modules.coherence.Localization;
import com.sun.tools.visualvm.modules.coherence.VisualVMView;

import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;

/**
 * The {@link DataSourceDescriptor} for Coherence clusters. Coherence cluster is a
 * data source, and the data source will be shown in the left hand side of JVisualVM.
 *
 * @author sr 12.10.2017
 *
 * @since Coherence 12.2.1.4.0
 */
public class CoherenceClustersDataSourceDescriptor
        extends DataSourceDescriptor<CoherenceClustersDataSource>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public CoherenceClustersDataSourceDescriptor()
        {
        super(CoherenceClustersDataSource.sharedInstance(),
                Localization.getLocalText("TXT_Coherence_Data_Source"),
                Localization.getLocalText("TXT_Coherence_Data_Source"),
                VisualVMView.NODE_ICON,
                POSITION_AT_THE_END,
                EXPAND_ON_EACH_NEW_CHILD);
        }
    }
