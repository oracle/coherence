/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.datasource;

import com.sun.tools.visualvm.modules.coherence.VisualVMView;

import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.DataSourceViewProvider;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;

/**
 * The {@link DataSourceViewProvider} for {@link CoherenceClusterDataSource}.
 *
 * @author shyaradh 12.10.2017
 *
 * @since Coherence 12.2.1.4.0
 */
public class CoherenceClusterDataSourceViewProvider
        extends DataSourceViewProvider<CoherenceClusterDataSource>
    {
    // ----- DataSourceViewProvider methods ---------------------------------

    @Override
    protected boolean supportsViewFor(CoherenceClusterDataSource coherenceClusterDataSource)
        {
        return true;
        }

    @Override
    protected DataSourceView createView(CoherenceClusterDataSource coherenceClusterDataSource)
        {
        return new VisualVMView(coherenceClusterDataSource);
        }

    // ----- CoherenceClusterDataSourceViewProvider methods --------------

    /**
     * Register the view provider with the manager.
     */
    public static void register()
        {
        DataSourceViewsManager.sharedInstance().addViewProvider(INSTANCE, CoherenceClusterDataSource.class);
        }

    /**
     * Unregister the view provider with the manager.
     */
    public static void unregister()
        {
        DataSourceViewsManager.sharedInstance().removeViewProvider(INSTANCE);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The singleton instance of {@link CoherenceClusterDataSourceViewProvider}.
     */
    final protected static CoherenceClusterDataSourceViewProvider INSTANCE =
            new CoherenceClusterDataSourceViewProvider();
    }
