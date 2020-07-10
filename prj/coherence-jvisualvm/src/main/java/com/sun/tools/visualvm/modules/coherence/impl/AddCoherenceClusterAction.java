/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.impl;

import com.sun.tools.visualvm.modules.coherence.Localization;
import com.sun.tools.visualvm.modules.coherence.VisualVMView;
import com.sun.tools.visualvm.modules.coherence.datasource.CoherenceClustersDataSource;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import org.graalvm.visualvm.core.ui.actions.SingleDataSourceAction;

/**
 * The {@link SingleDataSourceAction} for adding a Coherence cluster to the {@link CoherenceClustersDataSource}.
 *
 * @author sr 12.10.2017
 *
 * @since Coherence 12.2.1.4.0
 */
public class AddCoherenceClusterAction
        extends SingleDataSourceAction<CoherenceClustersDataSource>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create an instance of {@link AddCoherenceClusterAction}.
     */
    private AddCoherenceClusterAction()
        {
        super(CoherenceClustersDataSource.class);
        putValue(NAME, Localization.getLocalText("LBL_Add_Coherence_Cluster"));
        putValue(SHORT_DESCRIPTION, Localization.getLocalText("TTIP_Add_Coherence_Cluster"));
        }

    // ----- SingleDataSourceAction methods ---------------------------------

    @Override
    protected void actionPerformed(CoherenceClustersDataSource coherenceApplicationsDataSource, ActionEvent actionEvent)
        {
        CoherenceClusterConfigurator coherenceClusterConfigurator =
                CoherenceClusterConfigurator.defineApplication();
        if (coherenceClusterConfigurator != null)
            {
            CoherenceClusterProvider.createCoherenceClusterDataSource(coherenceClusterConfigurator.getAppUrl(),
                    coherenceClusterConfigurator.getClusterName());
            }
        }

    @Override
    protected boolean isEnabled(CoherenceClustersDataSource coherenceApplicationsDataSource)
        {
        return true;
        }

    // ----- AddCoherenceClusterAction methods --------------------------

    /**
     * Return the always enabled action. This method is specified in the layer.xml.
     *
     * @return the always enabled action
     */
    public static synchronized AddCoherenceClusterAction alwaysEnabled()
        {
        if (alwaysEnabled == null)
            {
            alwaysEnabled = new AddCoherenceClusterAction();
            alwaysEnabled.putValue(SMALL_ICON, new ImageIcon(VisualVMView.NODE_ICON));
            alwaysEnabled.putValue("iconBase", VisualVMView.IMAGE_PATH);
            }
        return alwaysEnabled;
        }

    /**
     * Return the selection aware action. This method is specified in the layer.xml.
     *
     * @return the selection aware action
     */
    public static synchronized AddCoherenceClusterAction selectionAware()
        {
        if (selectionAware == null)
            {
            selectionAware = new AddCoherenceClusterAction();
            }
        return selectionAware;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The always enabled application action.
     */
    // This variable name is exposed to layer.xml, so it does not follow source code naming convention.
    private static AddCoherenceClusterAction alwaysEnabled;

    /**
     * The selection awarw application action.
     */
    // This variable name is exposed to layer.xml, so it does not follow source code naming convention.
    private static AddCoherenceClusterAction selectionAware;
    }
