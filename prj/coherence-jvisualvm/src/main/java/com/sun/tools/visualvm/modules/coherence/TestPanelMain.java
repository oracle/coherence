/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence;

import com.sun.tools.visualvm.modules.coherence.helper.HttpRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceCachePanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceClusterOverviewPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceElasticDataPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceFederationPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceHttpProxyPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceMachinePanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceMemberPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceProxyPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceServicePanel;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.io.IOException;

import java.net.MalformedURLException;

import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanServerConnection;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import javax.swing.border.EmptyBorder;

/**
 * Test driver for panels.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class TestPanelMain
    {

    // ----- helpers --------------------------------------------------------

    private static void createAndShowGUI(JPanel jtop)
        {
        // Create and set up the window.
        JFrame frame = new JFrame("Coherence");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane.
        JComponent contentPane = (JComponent) frame.getContentPane();

        contentPane.add(jtop, BorderLayout.CENTER);
        contentPane.setOpaque(true);    // content panes must be opaque
        contentPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        frame.setContentPane(contentPane);

        // Display the window.
        frame.pack();
        frame.setVisible(true);
        }

    /**
     * Main entry point to test the plugin. This will create a panel with the
     * tabs to display the various statistics outside of JVisualVM.
     * Provide a hostname and port to connect to.  The default is localhost and 10001.<br>
     *
     * Note: You must also supply the jvisualvm dependencies (identified in pom.xml), on the classpath
     * to run this.
     *
     * @param args arguments to main  hostname and port
     *
     * @throws Exception if timer is interrupted
     */
    public static void main(String[] args)
            throws Exception
        {
        String sHostname = "localhost";
        int    nPort     = 10001;

        if (args.length == 2)
            {
            sHostname = args[0];
            nPort     = Integer.parseInt(args[1]);
            }

        requestSender = new HttpRequestSender("http://localhost:8080");

        VisualVMModel                       model                = VisualVMModel.getInstance();

        final CoherenceClusterOverviewPanel clusterOverviewPanel = new CoherenceClusterOverviewPanel(model);
        final CoherenceMemberPanel          memberPanel          = new CoherenceMemberPanel(model);
        final CoherenceServicePanel     servicePanel     = new CoherenceServicePanel(model);
        final CoherenceCachePanel       cachePanel       = new CoherenceCachePanel(model);
        final CoherenceProxyPanel       proxyPanel       = new CoherenceProxyPanel(model);
        final CoherenceMachinePanel     machinePanel     = new CoherenceMachinePanel(model);
        final CoherenceHttpProxyPanel   httpPanel        = new CoherenceHttpProxyPanel(model);
        final CoherenceElasticDataPanel elasticDataPanel = new CoherenceElasticDataPanel(model);
        final CoherenceFederationPanel  federationDataPanel = new CoherenceFederationPanel(model);

        cachePanel.setRequestSender(requestSender);
        memberPanel.setRequestSender(requestSender);
        clusterOverviewPanel.setRequestSender(requestSender);
        servicePanel.setRequestSender(requestSender);
        proxyPanel.setRequestSender(requestSender);
        machinePanel.setRequestSender(requestSender);
        httpPanel.setRequestSender(requestSender);
        elasticDataPanel.setRequestSender(requestSender);
        federationDataPanel.setRequestSender(requestSender);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Coherence Overview", clusterOverviewPanel);
        tabbedPane.addTab("Coherence Machines", machinePanel);
        tabbedPane.addTab("Coherence Members", memberPanel);
        tabbedPane.addTab("Coherence Services", servicePanel);
        tabbedPane.addTab("Coherence Caches", cachePanel);
        tabbedPane.addTab("Coherence Proxies", proxyPanel);
        tabbedPane.addTab("Coherence Http Proxies", httpPanel);
        tabbedPane.addTab("Coherence Elastic", elasticDataPanel);
        tabbedPane.addTab("Coherence Federation", federationDataPanel);

        final JPanel top = new JPanel(new GridLayout(1, 1));

        top.add(tabbedPane);

        // A timer task to update GUI per each interval
        TimerTask timerTask = new TimerTask()
            {
            @Override
            public void run()
                {
                // Schedule the SwingWorker to update the GUI
                model.refreshStatistics(requestSender);

                clusterOverviewPanel.updateData();
                clusterOverviewPanel.updateGUI();
                memberPanel.updateData();
                memberPanel.updateGUI();
                servicePanel.updateData();
                servicePanel.updateGUI();
                cachePanel.updateData();
                cachePanel.updateGUI();
                proxyPanel.updateData();
                proxyPanel.updateGUI();
                machinePanel.updateData();
                machinePanel.updateGUI();
                httpPanel.updateData();
                httpPanel.updateGUI();
                elasticDataPanel.updateData();
                elasticDataPanel.updateGUI();
                federationDataPanel.updateData();
                federationDataPanel.updateGUI();
                }
            };

        // Create the standalone window with TestPanel panel
        // by the event dispatcher thread
        SwingUtilities.invokeAndWait(new Runnable()
            {
            public void run()
                {
                createAndShowGUI(top);
                }


            });

        // refresh every 15 seconds
        Timer timer = new Timer("TestPanel Sampling thread");

        timer.schedule(timerTask, 0, 15000);

        }

    /**
     * Create a {@link MBeanServerConnection} given a hostname and port
     *
     * @param hostname  the hostname to connect to
     * @param port      the port to connect to
     *
     * @return a new {@link MBeanServerConnection}
     */
    private static MBeanServerConnection connect(String hostname, int port)
        {
        String                urlPath = "/jndi/rmi://" + hostname + ":" + port + "/jmxrmi";
        MBeanServerConnection server  = null;

        try
            {
            JMXServiceURL url  = new JMXServiceURL("rmi", "", 0, urlPath);
            JMXConnector  jmxc = JMXConnectorFactory.connect(url);

            server = jmxc.getMBeanServerConnection();
            }
        catch (MalformedURLException e)
            {
            }
        catch (IOException e)
            {
            System.err.println("\nCommunication error: " + e.getMessage());
            System.exit(1);
            }

        return server;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The model which holds and refreshes data.
     */
    private static VisualVMModel clusterStats = null;

    /**
     * The Request sender.
     */
    private static RequestSender requestSender;
    }
