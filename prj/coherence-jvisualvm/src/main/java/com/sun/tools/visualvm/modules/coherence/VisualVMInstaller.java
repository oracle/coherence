/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence;

import com.sun.tools.visualvm.modules.coherence.datasource.CoherenceClusterDataSourceViewProvider;
import com.sun.tools.visualvm.modules.coherence.datasource.CoherenceClustersDataSource;
import com.sun.tools.visualvm.modules.coherence.datasource.CoherenceClusterDataSourceDescriptorProvider;

import com.sun.tools.visualvm.modules.coherence.impl.CoherenceClusterProvider;

import org.openide.modules.ModuleInstall;

/**
 * Installer module for the Coherence plugin.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class VisualVMInstaller
        extends ModuleInstall
    {

    // ----- ModuleInstall methods ------------------------------------------

    /**
     * {@inheritDoc }
     */
    @Override
    public void restored()
        {
        VisualVMViewProvider.initialize();
        CoherenceClustersDataSource.register();
        CoherenceClusterDataSourceDescriptorProvider.register();
        CoherenceClusterDataSourceViewProvider.register();
        CoherenceClusterProvider.initCoherenceClustersDataSource();
        }

    /**
     * {@inheritDoc }
     */
    @Override
    public void uninstalled()
        {
        VisualVMViewProvider.unregister();
        CoherenceClustersDataSource.unregister();
        CoherenceClusterDataSourceDescriptorProvider.unregister();
        CoherenceClusterDataSourceViewProvider.unregister();
        }
    }
