/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence;

import com.sun.tools.visualvm.modules.coherence.datasource.CoherenceClusterDataSource;

import com.sun.tools.visualvm.modules.coherence.helper.HttpRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.JMXRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;

import com.sun.tools.visualvm.modules.coherence.panel.CoherenceHttpProxyPanel;

import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ClusterData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;

import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceCachePanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceClusterOverviewPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceElasticDataPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceHttpSessionPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceJCachePanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceMachinePanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceMemberPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherencePersistencePanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceProxyPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceServicePanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceFederationPanel;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;

import java.awt.Image;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.ObjectName;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.Timer;

/**
 * The implementation of the {@link DataSourceView} for displaying the
 * Coherence Cluster Snapshot tab.
 *
 * @author tam  2013.11.14
 */
public class VisualVMView
        extends DataSourceView
    {

    // ----- constructors ----------------------------------------------------

    /**
     * Creates the new instance of the tab.
     *
     * @param application
     */
    public VisualVMView(Application application)
        {
        super(application, "Oracle Coherence", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 60,
              false);
        if (application == null)
            {
            throw new RuntimeException("Application is null");
            }
        this.application = application;

        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);

        requestSender = new JMXRequestSender(jmx.getMBeanServerConnection());
        }

    /**
     * Creates the new instance of the tab.
     *
     * @param dataSource the Coherence management data source
     */
    public VisualVMView(CoherenceClusterDataSource dataSource)
        {
        super(dataSource, "Oracle Coherence", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 60,
                false);
        requestSender = new HttpRequestSender(dataSource.getUrl());

        // BUG 29213475 - Check for a valid HttpRequestSender URL before we start the refresh
        if (requestSender instanceof HttpRequestSender)
            {
            String sMessage = Localization.getLocalText("ERR_Invalid_URL");
            try
                {
                Set<ObjectName> allClusters = requestSender.getAllClusters();
                if (allClusters == null || allClusters.size() == 0)
                    {
                    LOGGER.warning(sMessage);
                    DialogDisplayer.getDefault().notify(
                        new NotifyDescriptor.Message(sMessage));
                    }
                }
            catch (Exception e)
                {
                LOGGER.warning(sMessage);
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(sMessage));
                throw new RuntimeException(sMessage, e);
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Create the new {@link DataViewComponent} which will display all the
     * Coherence related information.
     */
    protected DataViewComponent createComponent()
        {
        final VisualVMModel model = VisualVMModel.getInstance();

        // Data area for master view
        JEditorPane       generalDataArea  = new JEditorPane();
        generalDataArea.setEditable(false);

        // do an initial refresh of the data so we can see if we need to display
        // the proxy server tab
        model.refreshStatistics(requestSender);

        // we then construct the panels after the initial refresh so we can utilize
        // any information we have gathered in the startup

        final CoherenceClusterOverviewPanel pnlClusterOverview = new CoherenceClusterOverviewPanel(model);
        final CoherenceMachinePanel         pnlMachine         = new CoherenceMachinePanel(model);
        final CoherenceMemberPanel          pnlMember          = new CoherenceMemberPanel(model);
        final CoherenceServicePanel         pnlService         = new CoherenceServicePanel(model);
        final CoherenceCachePanel           pnlCache           = new CoherenceCachePanel(model);
        final CoherenceProxyPanel           pnlProxy           = new CoherenceProxyPanel(model);
        final CoherencePersistencePanel     pnlPersistence     = new CoherencePersistencePanel(model);
        final CoherenceHttpSessionPanel     pnlHttpSession     = new CoherenceHttpSessionPanel(model);
        final CoherenceFederationPanel      pnlFederation      = new CoherenceFederationPanel(model);
        final CoherenceElasticDataPanel     pnlElasticData     = new CoherenceElasticDataPanel(model);
        final CoherenceJCachePanel          pnlJCache          = new CoherenceJCachePanel(model);
        final CoherenceHttpProxyPanel       pnlHttpProxy       = new CoherenceHttpProxyPanel(model);

        String sClusterVersion = model.getClusterVersion();
        String sClusterName    = null;

        List<Map.Entry<Object, Data>> clusterData = model.getData(VisualVMModel.DataType.CLUSTER);
        for (Map.Entry <Object, Data > entry : clusterData)
            {
            sClusterName = entry.getValue().getColumn(ClusterData.CLUSTER_NAME).toString();
            break;
            }

        // Master view:
        DataViewComponent.MasterView masterView =
            new DataViewComponent.MasterView(Localization.getLocalText("LBL_cluster_information",
                                new String[] {sClusterName, sClusterVersion } ), null,
                                generalDataArea);

        // Configuration of master view:
        DataViewComponent.MasterViewConfiguration masterConfiguration =
            new DataViewComponent.MasterViewConfiguration(false);

        // Add the master view and configuration view to the component:
        dvc = new DataViewComponent(masterView, masterConfiguration);

        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(Localization.getLocalText(
                "LBL_cluster_overview"), false), DataViewComponent.TOP_RIGHT);

        // Add detail views to the components
        dvc.addDetailsView(new DataViewComponent.DetailsView(Localization.getLocalText("LBL_cluster_overview"),
                null, 10, pnlClusterOverview, null), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(Localization.getLocalText("LBL_machines"),
                null, 10, pnlMachine, null), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(Localization.getLocalText("LBL_members"),
                null, 10, pnlMember, null), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(Localization.getLocalText("LBL_services"),
                null, 10, pnlService, null), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(Localization.getLocalText("LBL_caches"),
                null, 10, pnlCache, null), DataViewComponent.TOP_RIGHT);

        // selectively add tabs based upon used functionality
        if (model.isFederationCongfigured())
            {
            dvc.addDetailsView(new DataViewComponent.DetailsView(Localization.getLocalText("LBL_federation"),
                    null, 10, pnlFederation, null), DataViewComponent.TOP_RIGHT);
            }

        if (model.isCoherenceExtendConfigured())
            {
            dvc.addDetailsView(new DataViewComponent.DetailsView(Localization.getLocalText("LBL_proxy_servers"),
                    null, 10, pnlProxy, null), DataViewComponent.TOP_RIGHT);
            }

        if (model.isHttpProxyConfigured())
            {
            dvc.addDetailsView(new DataViewComponent.DetailsView(Localization.getLocalText("LBL_http_proxy_servers"),
                    null, 10, pnlHttpProxy, null), DataViewComponent.TOP_RIGHT);
            }

        if (model.isPersistenceConfigured())
            {
            dvc.addDetailsView(new DataViewComponent.DetailsView(Localization.getLocalText("LBL_persistence"),
                    null, 10, pnlPersistence, null), DataViewComponent.TOP_RIGHT);
            }

        if (model.isCoherenceWebConfigured())
            {
            dvc.addDetailsView(new DataViewComponent.DetailsView(Localization.getLocalText("LBL_Coherence_web"),
                    null, 10, pnlHttpSession, null), DataViewComponent.TOP_RIGHT);
            }

        if (model.isElasticDataConfigured())
            {
            dvc.addDetailsView(new DataViewComponent.DetailsView(Localization.getLocalText("LBL_elastic_data"),
                    null, 10, pnlElasticData, null), DataViewComponent.TOP_RIGHT);
            }

        if (model.isJCacheConfigured())
            {
            dvc.addDetailsView(new DataViewComponent.DetailsView(Localization.getLocalText("LBL_JCache"),
                    null, 10, pnlJCache, null), DataViewComponent.TOP_RIGHT);
            }

        // update the request sender
        pnlClusterOverview.setRequestSender(requestSender);
        pnlMachine.setRequestSender(requestSender);
        pnlMember.setRequestSender(requestSender);
        pnlService.setRequestSender(requestSender);
        pnlCache.setRequestSender(requestSender);
        pnlProxy.setRequestSender(requestSender);
        pnlPersistence.setRequestSender(requestSender);
        pnlHttpSession.setRequestSender(requestSender);
        pnlFederation.setRequestSender(requestSender);
        pnlJCache.setRequestSender(requestSender);

        // display a warning if we are connected to a WLS domain and we can
        // see more that 1 domainPartition key. This code relies on us
        // using JMX queries rather than the reporter.
        if (model.getDomainPartitions().size() > 1)
            {
            JOptionPane.showMessageDialog(null, Localization.getLocalText("LBL_mt_warning"));
            }

        // create a timer that will refresh the TAB's as required
        timer = new Timer(GlobalPreferences.sharedInstance().getMonitoredDataPoll() * 1000, new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                if (refreshRunning)
                    {
                    return;
                    }

                refreshRunning = true;
                RequestProcessor.getDefault().post(new Runnable()
                    {
                    public void run()
                        {
                        try
                            {
                            // application may be null inside the constructor
                            if (application == null || application.getState() == Application.STATE_AVAILABLE)
                                {
                                // Schedule the SwingWorker to update the GUI
                                model.refreshStatistics(requestSender);

                                pnlClusterOverview.updateData();
                                pnlClusterOverview.updateGUI();
                                pnlMember.updateData();
                                pnlMember.updateGUI();
                                pnlService.updateData();
                                pnlService.updateGUI();
                                pnlCache.updateData();
                                pnlCache.updateGUI();

                                if (model.isFederationCongfigured())
                                    {
                                    pnlFederation.updateData();
                                    pnlFederation.updateGUI();
                                    }

                                if (model.isCoherenceExtendConfigured())
                                    {
                                    pnlProxy.updateData();
                                    pnlProxy.updateGUI();
                                    }

                                pnlMachine.updateData();
                                pnlMachine.updateGUI();

                                if (model.isPersistenceConfigured())
                                    {
                                    pnlPersistence.updateData();
                                    pnlPersistence.updateGUI();
                                    }

                                if (model.isCoherenceWebConfigured())
                                    {
                                    pnlHttpSession.updateData();
                                    pnlHttpSession.updateGUI();
                                    }

                                if (model.isElasticDataConfigured())
                                    {
                                    pnlElasticData.updateData();
                                    pnlElasticData.updateGUI();
                                    }

                                if (model.isJCacheConfigured())
                                    {
                                    pnlJCache.updateData();
                                    pnlJCache.updateGUI();
                                    }

                                if (model.isHttpProxyConfigured())
                                    {
                                    pnlHttpProxy.updateData();
                                    pnlHttpProxy.updateGUI();
                                    }
                                }
                            }
                        catch (Exception ex)
                            {
                            LOGGER.warning("Error while refreshing tabs. " + ex.toString());
                            ex.printStackTrace();
                            }
                        finally
                            {
                            refreshRunning = false;
                            }
                        }


                    });
                }


            });
        timer.setInitialDelay(800);
        timer.start();

        return dvc;

        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void removed()
        {
        timer.stop();
        }

    /**
     * Called on removal.
     *
     * @param app {@link Application} to remove
     */
    public void dataRemoved(Application app)
        {
        timer.stop();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The Coherence standard icon to use.
     */
    public static final String IMAGE_PATH = "com/sun/tools/visualvm/modules/coherence/coherence_grid_icon.png";

    /**
     * The Coherence icon.
     */
    public static final Image NODE_ICON = Utilities.loadImage(IMAGE_PATH, true);

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(VisualVMView.class.getName());

    // ----- data members ---------------------------------------------------

    /**
     * Component used to display the tabs.
     */
    private DataViewComponent dvc;

    /**
     * Timer used to refresh the screen
     */
    private Timer       timer;
    private Application application;

    /**
     * Indicates if the refresh is running.
     */
    private boolean refreshRunning;

    /**
     * The Request Sender to use.
     */
    private RequestSender requestSender = null;
    }
