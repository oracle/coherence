/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.panel;

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;
import com.sun.tools.visualvm.modules.coherence.helper.GraphHelper;
import com.sun.tools.visualvm.modules.coherence.helper.RenderHelper;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ClusterData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.MachineData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.MemberData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ServiceData;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import java.util.Date;
import java.util.List;

import java.util.Map.Entry;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.graalvm.visualvm.charts.SimpleXYChartSupport;

/**
 * An implementation of an {@link AbstractCoherencePanel} to
 * view various overview graphs for a Coherence cluster.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class CoherenceClusterOverviewPanel
        extends AbstractCoherencePanel
    {

    // ---- constructors ----------------------------------------------------

    /**
     * Create the layout for the {@link CoherenceClusterOverviewPanel}.
     *
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherenceClusterOverviewPanel(VisualVMModel model)
        {
        super(new BorderLayout(), model);

        this.setPreferredSize(new Dimension(500, 300));

        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new FlowLayout());
        pnlHeader.setOpaque(false);

        txtClusterName = getTextField(15, JTextField.LEFT);
        pnlHeader.add(getLocalizedLabel("LBL_cluster_name", txtClusterName));
        pnlHeader.add(txtClusterName);

        txtVersion = getTextField(8, JTextField.LEFT);
        pnlHeader.add(getLocalizedLabel("LBL_version", txtVersion));
        pnlHeader.add(txtVersion);

        txtClusterSize = getTextField(3, JTextField.RIGHT);
        pnlHeader.add(getLocalizedLabel("LBL_members", txtClusterSize));
        pnlHeader.add(txtClusterSize);

        txtRefreshDate = getTextField(18, JTextField.LEFT);
        pnlHeader.add(getLocalizedLabel("LBL_refresh_date", txtRefreshDate));
        pnlHeader.add(txtRefreshDate);

        txtClusterStatusHA = getTextField(10, JTextField.LEFT);
        pnlHeader.add(getLocalizedLabel("LBL_cluster_statusha", txtClusterStatusHA));
        pnlHeader.add(txtClusterStatusHA);

        JPanel pnlData = new JPanel();

        pnlData.setLayout(new GridLayout(2, 2));

        // create a chart for total cluster memory
        memoryGraph = GraphHelper.createClusterMemoryGraph();

        JPanel pnlPlotter = new JPanel(new GridLayout(1, 1));

        pnlPlotter.add(memoryGraph.getChart());

        pnlData.add(pnlPlotter);

        // create a chart for publisher success rate
        publisherGraph = GraphHelper.createPublisherGraph();

        JPanel pnlPlotter2 = new JPanel(new GridLayout(1, 1));

        pnlPlotter2.add(publisherGraph.getChart());
        pnlData.add(pnlPlotter2);

        // create a chart for machine load average
        loadAverageGraph = GraphHelper.createMachineLoadAverageGraph(model);

        JPanel pnlPlotter4 = new JPanel(new GridLayout(1, 1));

        pnlPlotter4.add(loadAverageGraph.getChart());
        pnlData.add(pnlPlotter4);

        // create a chart for receiver success rate
        receiverGraph = GraphHelper.createReceiverGraph();

        JPanel pnlPlotter3 = new JPanel(new GridLayout(1, 1));

        pnlPlotter3.add(receiverGraph.getChart());
        pnlData.add(pnlPlotter3);

        add(pnlHeader, BorderLayout.PAGE_START);
        add(pnlData, BorderLayout.CENTER);
        }

    // ----- AbstractCoherencePanel methods ---------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGUI()
        {
        int   cTotalMemory        = 0;
        int   cTotalMemoryUsed    = 0;
        float cTotalPublisherRate = 0.0f;
        float cTotalReceiverRate  = 0.0f;
        float cMinPublisherRate   = -1;
        float cMinReceiverRate    = -1;

        // get the min /max values for publisher and receiver success rates
        if (memberData != null)
            {
            int   count = 0;
            float cRate = 0;

            for (Entry<Object, Data> entry : memberData)
                {
                // only include memory is the node is storage enabled
                if (isNodeStorageEnabled((Integer) entry.getValue().getColumn(MemberData.NODE_ID)))
                    {
                    cTotalMemory     += (Integer) entry.getValue().getColumn(MemberData.MAX_MEMORY);
                    cTotalMemoryUsed += (Integer) entry.getValue().getColumn(MemberData.USED_MEMORY);
                    }

                count++;
                cRate               = (Float) entry.getValue().getColumn(MemberData.PUBLISHER_SUCCESS);
                cTotalPublisherRate += cRate;

                if (cMinPublisherRate == -1 || cRate < cMinPublisherRate)
                    {
                    cMinPublisherRate = cRate;
                    }

                cRate              = (Float) entry.getValue().getColumn(MemberData.RECEIVER_SUCCESS);
                cTotalReceiverRate += cRate;

                if (cMinReceiverRate == -1 || cRate < cMinReceiverRate)
                    {
                    cMinReceiverRate = cRate;
                    }
                }

            // update the publisher graph
            GraphHelper.addValuesToPublisherGraph(publisherGraph, cMinPublisherRate,
                count == 0 ? 0 : cTotalPublisherRate / count);

            GraphHelper.addValuesToReceiverGraph(receiverGraph, cMinReceiverRate,
                count == 0 ? 0 : cTotalReceiverRate / count);
            }

        // update the memory graph
        if (cTotalMemory != 0)
            {
            GraphHelper.addValuesToClusterMemoryGraph(memoryGraph, cTotalMemory, cTotalMemoryUsed);
            }

        // update cluster information
        if (clusterData != null)
            {
            for (Entry<Object, Data> entry : clusterData)
                {
                txtClusterName.setText(entry.getValue().getColumn(ClusterData.CLUSTER_NAME).toString());
                txtRefreshDate.setText(new Date(model.getLastUpdate()).toString());
                txtVersion.setText(entry.getValue().getColumn(ClusterData.VERSION).toString().replaceFirst(" .*$", ""));
                txtClusterSize.setText(String.format("%d", entry.getValue().getColumn(ClusterData.CLUSTER_SIZE)));

                }
            }

        // update the statusHA value for the cluster
        if (serviceData != null)
            {
            // start at best value of SITE-SAFE and get a "cluster statusHA" by working backwards
            int bestStatusHA = STATUSHA_VALUES.length;

            for (Entry<Object, Data> entry : serviceData)
                {
                if (!"n/a".equals(entry.getValue().getColumn(ServiceData.STATUS_HA)))
                    {
                    int statusHAIndex = getStatusHAIndex(entry.getValue().getColumn(ServiceData.STATUS_HA).toString());

                    if (statusHAIndex < bestStatusHA)
                        {
                        bestStatusHA = statusHAIndex;
                        }
                    }
                }

            if (bestStatusHA < STATUSHA_VALUES.length)
                {
                // now set the "cluster statusHA"
                String sStatusHA = STATUSHA_VALUES[bestStatusHA];

                if (bestStatusHA == 0)
                    {
                    txtClusterStatusHA.setBackground(Color.red);
                    txtClusterStatusHA.setForeground(Color.white);
                    txtClusterStatusHA.setToolTipText(RenderHelper.ENDANGERED_TOOLTIP);
                    }
                else if (bestStatusHA == 1)
                    {
                    txtClusterStatusHA.setBackground(Color.orange);
                    txtClusterStatusHA.setForeground(Color.black);
                    txtClusterStatusHA.setToolTipText(RenderHelper.NODE_SAFE_TOOLTIP);
                    }
                else
                    {
                    txtClusterStatusHA.setBackground(Color.green);
                    txtClusterStatusHA.setForeground(Color.black);
                    txtClusterStatusHA.setToolTipText(RenderHelper.MACHINE_SAFE_TOOLTIP);
                    }

                txtClusterStatusHA.setText(sStatusHA);
                }
            else
                {
                txtClusterStatusHA.setText("");
                }
            }

        int    count             = 0;
        double cLoadAverage      = 0;
        double cMax              = -1;
        double cTotalLoadAverage = 0;

        // work out the max and average load averages for the graph
        if (machineData != null)
            {
            for (Entry<Object, Data> entry : machineData)
                {
                count++;
                cLoadAverage      = (Double) entry.getValue().getColumn(MachineData.SYSTEM_LOAD_AVERAGE);
                cTotalLoadAverage += cLoadAverage;

                if (cMax == -1 || cLoadAverage > cMax)
                    {
                    cMax = cLoadAverage;
                    }
                }

            // update graph
            GraphHelper.addValuesToLoadAverageGraph(loadAverageGraph, (float) cMax,
                (float) (cTotalLoadAverage == 0 ? 0 : cTotalLoadAverage / count));
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateData()
        {
        memberData  = model.getData(VisualVMModel.DataType.MEMBER);
        clusterData = model.getData(VisualVMModel.DataType.CLUSTER);
        serviceData = model.getData(VisualVMModel.DataType.SERVICE);
        machineData = model.getData(VisualVMModel.DataType.MACHINE);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Returns the status HA index from 0-4. 0 being ENDANGERED and 4 being SITE-SAFE.
     *
     * @param sStatusHA  the textual version of statusHA
     *
     * @return  the index that the textual version matches
     */
    private int getStatusHAIndex(String sStatusHA)
        {
        for (int i = 0; i < STATUSHA_VALUES.length; i++)
            {
            if (STATUSHA_VALUES[i].equals(sStatusHA))
                {
                return i;
                }
            }

        return -1;

        }

    // ----- constants ------------------------------------------------------

    /**
     * Text value of statusHA.
     */
    private static final String[] STATUSHA_VALUES = new String[] {"ENDANGERED", "NODE-SAFE", "MACHINE-SAFE",
        "RACK-SAFE", "SITE-SAFE"};

    private static final long serialVersionUID = 2602085070795849149L;

    // ----- data members ---------------------------------------------------

    /**
     * The cluster name.
     */
    private JTextField txtClusterName;

    /**
     * The cluster version with anything after the version number stripped out.
     */
    private JTextField txtVersion;

    /**
     * The date when the JConsole tab was refreshed. This is different to the data
     * refresh.
     */
    private JTextField txtRefreshDate;

    /**
     * The cluster statusHA value obtained from all the services that have statusHA.
     */
    private JTextField txtClusterStatusHA;

    /**
     * The cluster size.
     */
    private JTextField txtClusterSize;

    /**
     * The graph of overall cluster memory.
     */
    private SimpleXYChartSupport memoryGraph = null;

    /**
     * The graph of packet publisher success rates.
     */
    private SimpleXYChartSupport publisherGraph = null;

    /**
     * The graph of packet receiver success rates.
     */
    private SimpleXYChartSupport receiverGraph = null;

    /**
     * The graph of primary memory cache size.
     */
    private SimpleXYChartSupport loadAverageGraph = null;

    /**
     * The member statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> memberData;

    /**
     * The cluster statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> clusterData;

    /**
     * The service statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> serviceData;

    /**
     * The machine statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> machineData;
    }
