/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * Class to provide the view for Coherence JVisualVM plugin.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class VisualVMViewProvider
        extends DataSourceViewProvider<Application>
    {

    // ----- DataSourceViewProvider methods ---------------------------------

    /**
     * Returns true or false indicating if the JMX connection is actually for
     * a Coherence cluster or not.
     *
     * @return true if a Coherence cluster otherwise false
     */
    @Override
    public boolean supportsViewFor(Application application)
        {
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);

        // system property for disabling the MBean check as with connecting to WLS, sometimes
        // the Coherence MBean does not show up immediately and therefore the tab never gets
        // displayed
        String sDisableCheck = VisualVMModel.getSystemProperty("coherence.jvisualvm.disable.mbean.check");

        if (jmx != null && jmx.getConnectionState() == JmxModel.ConnectionState.CONNECTED)
            {
            if (sDisableCheck != null && "true".equals(sDisableCheck))
                {
                return true;
                }

            MBeanServerConnection connection = jmx.getMBeanServerConnection();

            try
                {
                if (connection.isRegistered(new ObjectName("Coherence:type=Cluster")))
                    {
                    return true;
                    }
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                }
            }

        return false;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataSourceView createView(Application application)
        {
        return new VisualVMView(application);
        }

    /**
     * Supports Save views.
     *
     * @param app {@link Application}
     *
     * @return true if support for save views
     */
    public boolean supportsSaveViewsFor(Application app)
        {
        return false;
        }

    /**
     * Save views.
     *
     * @param appSource source {@link Application}
     * @param appDest   destination {@link Application}
     */
    public void saveViews(Application appSource, Application appDest)
        {
        }

    /**
     * Initialize a new the view provider.
     */
    static void initialize()
        {
        DataSourceViewsManager.sharedInstance().addViewProvider(instance, Application.class);
        }

    /**
     * Unregister the view provider.
     */
    static void unregister()
        {
        DataSourceViewsManager.sharedInstance().removeViewProvider(instance);
        }

    // ----- constants ------------------------------------------------------

    private static DataSourceViewProvider<Application> instance = new VisualVMViewProvider();
    }
