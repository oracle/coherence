/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.panel;

import com.sun.tools.visualvm.modules.coherence.Localization;
import com.sun.tools.visualvm.modules.coherence.VisualVMModel;
import com.sun.tools.visualvm.modules.coherence.helper.GraphHelper;
import com.sun.tools.visualvm.modules.coherence.helper.RenderHelper;
import com.sun.tools.visualvm.modules.coherence.panel.util.ExportableJTable;
import com.sun.tools.visualvm.modules.coherence.tablemodel.ProxyTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ProxyData;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;

import java.util.List;
import java.util.Map.Entry;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.graalvm.visualvm.charts.SimpleXYChartSupport;

/**
 * An implementation of an {@link AbstractCoherencePanel} to view
 * summarized proxy server data.
 *
 * @author tam  2013.11.14
 */
public class CoherenceProxyPanel
        extends AbstractCoherencePanel
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create the layout for the {@link CoherenceProxyPanel}.
     *
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherenceProxyPanel(VisualVMModel model)
        {
        super(new BorderLayout(), model);

        // create a split pane for resizing
        JSplitPane pneSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pneSplit.setOpaque(false);

        // Create the header panel
        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new FlowLayout());
        pnlHeader.setOpaque(false);

        txtTotalProxyServers = getTextField(5, JTextField.RIGHT);
        pnlHeader.add(getLocalizedLabel("LBL_total_proxy_servers", txtTotalProxyServers));
        pnlHeader.add(txtTotalProxyServers);

        txtTotalConnections = getTextField(5, JTextField.RIGHT);
        pnlHeader.add(getLocalizedLabel("LBL_total_connections", txtTotalConnections));
        pnlHeader.add(txtTotalConnections);

        // special processing for Name Service
        if (model.is1213AndAbove())
            {
            if (model.getClusterVersionAsInt() >= 122100)
                {
                // NameService no longer shows up under ConnectionManagerMBean in
                // 12.2.1 and above so disable checkbox entirely
                cbxIncludeNameService = null;
                }
            else
                {
                // NameService was visible in 12.1.3 as a service in ConnectionManagerMBean
                // so allow user to choose whether to display or not
                cbxIncludeNameService = new JCheckBox(Localization.getLocalText("LBL_include_name_service"));
                cbxIncludeNameService.setMnemonic(KeyEvent.VK_N);
                cbxIncludeNameService.setSelected(false);
                pnlHeader.add(cbxIncludeNameService);
                }
            }

        // create the table
        tmodel = new ProxyTableModel(VisualVMModel.DataType.PROXY.getMetadata());

        table = new ExportableJTable(tmodel);

        table.setPreferredScrollableViewportSize(new Dimension(500, 150));

        // define renderers for the columns
        RenderHelper.setColumnRenderer(table, ProxyData.TOTAL_BYTES_RECEIVED, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, ProxyData.TOTAL_BYTES_SENT, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, ProxyData.TOTAL_MSG_RECEIVED, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, ProxyData.TOTAL_MSG_SENT, new RenderHelper.IntegerRenderer());
        RenderHelper.setHeaderAlignment(table, JLabel.CENTER);

        // Add some space
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        // Create the scroll pane and add the table to it.
        JScrollPane pneScroll = new JScrollPane(table);
        configureScrollPane(pneScroll, table);
        pneScroll.setOpaque(false);

        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setOpaque(false);

        pnlTop.add(pnlHeader, BorderLayout.PAGE_START);
        pnlTop.add(pneScroll, BorderLayout.CENTER);

        JSplitPane pneSplitPlotter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        pneSplitPlotter.setResizeWeight(0.5);
        pneSplitPlotter.setOpaque(false);

        // create a chart for the count of proxy server connections
        proxyGraph      = GraphHelper.createTotalProxyConnectionsGraph();
        proxyStatsGraph = GraphHelper.createProxyServerStatsGraph();


        pneSplitPlotter.add(proxyGraph.getChart());
        pneSplitPlotter.add(proxyStatsGraph.getChart());

        pneSplit.add(pnlTop);
        pneSplit.add(pneSplitPlotter);

        add(pneSplit);
        }

    // ----- AbstractCoherencePanel methods ---------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGUI()
        {
        final String MEM_FORMAT        = "%,d";

        int  cTotalConnections = 0;
        long nSentCount        = 0L;
        long nRecCount         = 0L;

        if (proxyData != null)
            {
            txtTotalProxyServers.setText(String.format("%5d", proxyData.size()));

            for (Entry<Object, Data> entry : proxyData)
                {
                cTotalConnections += (Integer) entry.getValue().getColumn(ProxyData.CONNECTION_COUNT);
                nSentCount        += (Long) entry.getValue().getColumn(ProxyData.TOTAL_BYTES_SENT);
                nRecCount         += (Long) entry.getValue().getColumn(ProxyData.TOTAL_BYTES_RECEIVED);
                }

            txtTotalConnections.setText(String.format(MEM_FORMAT, cTotalConnections));
            }

        else
            {
            txtTotalProxyServers.setText(String.format(MEM_FORMAT, 0));
            txtTotalConnections.setText(String.format(MEM_FORMAT, 0));
            }

        fireTableDataChangedWithSelection(table, tmodel);

        GraphHelper.addValuesToTotalProxyConnectionsGraph(proxyGraph, cTotalConnections);

        long ldtLastUpdate = model.getLastUpdate();
        if (ldtLastUpdate > m_cLastUpdateTime)
            {
            if (m_cLastRecCount == -1L)
                {
                m_cLastRecCount  = nRecCount;
                m_cLastSentCount = nSentCount;
                }

            // get delta values
            long nDeltaRecCount  = nRecCount - m_cLastRecCount;
            long nDeltaSentCount = nSentCount - m_cLastSentCount;

            GraphHelper.addValuesToProxyServerStatsGraph(proxyStatsGraph,
                    nDeltaSentCount < 0 ? 0 : nDeltaSentCount,
                    nDeltaRecCount  < 0 ? 0 : nDeltaRecCount);

            // set the last values to calculate deltas
            m_cLastRecCount   = nRecCount;
            m_cLastSentCount  = nSentCount;
            m_cLastUpdateTime = ldtLastUpdate;
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateData()
        {
        // update the model to indicate if we are going to include the NameService
        model.setIncludeNameService(cbxIncludeNameService != null && cbxIncludeNameService.isSelected());

        proxyData = model.getData(VisualVMModel.DataType.PROXY);

        tmodel.setDataList(proxyData);
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -7612569043492412546L;

    // ----- data members ----------------------------------------------------

    /**
     * The total number of proxy servers (tcp-acceptors) running in the cluster.
     */
    private JTextField txtTotalProxyServers;

    /**
     * The total number of proxy server connections.
     */
    private JTextField txtTotalConnections;

    /**
     * A check-box to indicate if the NameService should be included in the list of proxy servers.
     */
    private JCheckBox cbxIncludeNameService = null;

    /**
     * The graph of proxy server connections.
     */
    private SimpleXYChartSupport proxyGraph;

    /**
     * The graph of proxy server stats.
     */
    private SimpleXYChartSupport proxyStatsGraph;

    /**
     * The proxy statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> proxyData;

    /**
     * The {@link ProxyTableModel} to display proxy data.
     */
    protected ProxyTableModel tmodel;

    /**
     * the {@link ExportableJTable} to use to display data.
     */
    protected ExportableJTable table;

    /**
     * Last sent count.
     */
    private long m_cLastSentCount = -1L;

    /**
     * Last receive count.
     */
    private long m_cLastRecCount = -1L;

    /**
     * Last update time for stats.
     */
    private long m_cLastUpdateTime = -1L;

    }
