/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.sun.tools.visualvm.modules.coherence.panel;

import com.sun.tools.visualvm.charts.SimpleXYChartSupport;

import com.sun.tools.visualvm.modules.coherence.Localization;

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import com.sun.tools.visualvm.modules.coherence.helper.GraphHelper;
import com.sun.tools.visualvm.modules.coherence.helper.RenderHelper;

import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;
import com.sun.tools.visualvm.modules.coherence.panel.util.AbstractMenuOption;
import com.sun.tools.visualvm.modules.coherence.panel.util.ExportableJTable;
import com.sun.tools.visualvm.modules.coherence.panel.util.MenuOption;

import com.sun.tools.visualvm.modules.coherence.panel.util.SeparatorMenuOption;
import com.sun.tools.visualvm.modules.coherence.tablemodel.FederationTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.FederationInboundTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.FederationOutboundTableModel;

import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FederationData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FederationDestinationDetailsData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FederationOriginDetailsData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Pair;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import java.awt.event.ActionEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * An implementation of an {@link AbstractCoherencePanel} to
 * view summarized federatrion service statistics.
 *
 * @author cl  2014.02.17
 * @since  12.2.1
 */
public class CoherenceFederationPanel
        extends AbstractCoherencePanel
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create the layout for the {@link CoherenceFederationPanel}.
     *
     * @param model the {@link VisualVMModel} to use for this panel
     */
    public CoherenceFederationPanel(VisualVMModel model)
        {
        super(new BorderLayout(), model);

        // create a split pane for resizing
        JSplitPane pneSplitFed = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pneSplitFed.setOpaque(false);

        // create a tab pane for inbound and outbound tabs
        JTabbedPane pneTabDetail = new JTabbedPane();
        pneTabDetail.setOpaque(false);

        // create two split panes for the inbound and outbound tabs
        JSplitPane pneSplitInbound  = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JSplitPane pneSplitOutbound = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pneSplitInbound.setOpaque(false);
        pneSplitOutbound.setOpaque(false);

        // create two split panes for details inside inbound and outbound panes
        JSplitPane pneSplitInboundDetail  = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JSplitPane pneSplitOutboundDetail = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        pneSplitInboundDetail.setOpaque(false);
        pneSplitOutboundDetail.setOpaque(false);

        // create table models for the inbound and outbound details
        m_tmodelFed      = new FederationTableModel(VisualVMModel.DataType.FEDERATION_DESTINATION.getMetadata());
        m_tmodelInbound  = new FederationInboundTableModel(VisualVMModel.DataType.FEDERATION_ORIGIN_DETAILS.getMetadata());
        m_tmodelOutbound = new FederationOutboundTableModel(VisualVMModel.DataType.FEDERATION_DESTINATION_DETAILS.getMetadata());

        // create exportable JTables for each table models
        final ExportableJTable tableFed      = new ExportableJTable(m_tmodelFed);
        final ExportableJTable tableInbound  = new ExportableJTable(m_tmodelInbound);
        final ExportableJTable tableOutbound = new ExportableJTable(m_tmodelOutbound);

        // create the scroll pane and add the table to it.
        JScrollPane pneScrollFed      = new JScrollPane(tableFed);
        JScrollPane pneScrollInbound  = new JScrollPane(tableInbound);
        JScrollPane pneScrollOutbound = new JScrollPane(tableOutbound);
        configureScrollPane(pneScrollFed, tableFed);
        configureScrollPane(pneScrollInbound, tableInbound);
        configureScrollPane(pneScrollOutbound, tableOutbound);

        // configure menu options
        Set<MenuOption> setMenuOptions = new LinkedHashSet<>();
        setMenuOptions.add(new StartMenuOption(model, requestSender, tableFed, StartMenuOption.START));

        if (model.getClusterVersionAsInt() >= 122103)
            {
            // add startwithSync & StartWIthNoBacklog available in patchset 12.2.1.0.3 and above
            setMenuOptions.add(new StartMenuOption(model, requestSender, tableFed, StartMenuOption.START_WITH_SYNC));
            setMenuOptions.add(new StartMenuOption(model, requestSender, tableFed, StartMenuOption.START_WITH_NO_BACKLOG));
            }

        setMenuOptions.add(new StopMenuOption(model, requestSender, tableFed));
        setMenuOptions.add(new PauseMenuOption(model, requestSender, tableFed));
        setMenuOptions.add(new SeparatorMenuOption(model, requestSender, tableFed));
        setMenuOptions.add(new ReplicateAllMenuOption(model, requestSender, tableFed));
        setMenuOptions.add(new RetrievePendingIncomingMessagesMenuOption(model, requestSender, tableFed));
        setMenuOptions.add(new RetrievePendingOutgoingMessagesMenuOption(model, requestSender, tableFed));

        // add right-click menu options
        tableFed.setMenuOptions(setMenuOptions.toArray(new MenuOption[setMenuOptions.size()]));

        // create a detail pane
        JPanel pneFedDetail = new JPanel(new BorderLayout());
        pneFedDetail.setOpaque(false);

        // create containers for inbound and outbounf details
        JPanel pneInboundDetail = new JPanel(new BorderLayout());
        JPanel pneOutboundDetail = new JPanel(new BorderLayout());
        pneInboundDetail.setOpaque(false);
        pneOutboundDetail.setOpaque(false);

        // create a panel to hold the textfields for outbound
        JPanel txtPanelOutboundDetailTotal = new JPanel();
        txtPanelOutboundDetailTotal.setBackground(Color.white);

        // create several textfields for the detail stats for outbound
        m_txtOutboundMaxBandwidth = getTextField(7, JTextField.LEFT);
        m_txtOutboundSendTimeOut  = getTextField(7, JTextField.LEFT);
        m_txtOutboundGeoIp        = getTextField(7, JTextField.LEFT);
        m_txtOutboundErrorDesp    = getTextField(7, JTextField.LEFT);

        // create labels for the textfields
        JLabel labelOutboundMaxBandwidth = getLocalizedLabel("LBL_max_bandwidth", m_txtOutboundMaxBandwidth);
        JLabel labelOutboundSendTimeOut  = getLocalizedLabel("LBL_send_time_out", m_txtOutboundSendTimeOut);
        JLabel labelOutboundGeoIp        = getLocalizedLabel("LBL_geo_ip", m_txtOutboundGeoIp);
        JLabel labelOutboundErrorDesp    = getLocalizedLabel("LBL_error_description", m_txtOutboundErrorDesp);

        // set group layout for four textfields in outbound tab
        GroupLayout layout = new GroupLayout(txtPanelOutboundDetailTotal);
        txtPanelOutboundDetailTotal.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

        hGroup.addGroup(layout.createParallelGroup().
            addComponent(labelOutboundMaxBandwidth).
            addComponent(labelOutboundSendTimeOut).
            addComponent(labelOutboundGeoIp).
            addComponent(labelOutboundErrorDesp)
            );

        hGroup.addGroup(layout.createParallelGroup().
            addComponent(m_txtOutboundMaxBandwidth).
            addComponent(m_txtOutboundSendTimeOut).
            addComponent(m_txtOutboundGeoIp).
            addComponent(m_txtOutboundErrorDesp)
            );

        layout.setHorizontalGroup(hGroup);

        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

        vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
            addComponent(labelOutboundMaxBandwidth).
            addComponent(m_txtOutboundMaxBandwidth)
            );

        vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
            addComponent(labelOutboundSendTimeOut).
            addComponent(m_txtOutboundSendTimeOut)
            );

        vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
            addComponent(labelOutboundGeoIp).
            addComponent(m_txtOutboundGeoIp)
            );

        vGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
            addComponent(labelOutboundErrorDesp).
            addComponent(m_txtOutboundErrorDesp)
            );

        layout.setVerticalGroup(vGroup);

        // create panes for the outbound and inbound graphs
        final JTabbedPane pneTabInboundGraph = new JTabbedPane();
        final JTabbedPane pneTabOutboundGraph = new JTabbedPane();
        pneTabInboundGraph.setOpaque(false);
        pneTabOutboundGraph.setOpaque(false);

        // create graphs in tabs
        populateOutboundTabs(pneTabOutboundGraph);
        populateInboundTabs(pneTabInboundGraph);
        pneTabOutboundGraph.setOpaque(false);
        pneTabInboundGraph.setOpaque(false);

        // reder
        RenderHelper.setColumnRenderer(tableFed, FederationData.Column.STATUS.ordinal() - 1, new RenderHelper.FedServiceStateRenderer());

        RenderHelper.setColumnRenderer(tableFed, 3, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableFed, 4, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableFed, 5, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableFed, 6, new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableOutbound, FederationDestinationDetailsData.Column.STATE.ordinal(),
            new RenderHelper.FedNodeStateRenderer());

        RenderHelper.setColumnRenderer(tableOutbound, FederationDestinationDetailsData.Column.CURRENT_BANDWIDTH.ordinal(),
            new RenderHelper.DecimalRenderer());

        RenderHelper.setColumnRenderer(tableOutbound, FederationDestinationDetailsData.Column.TOTAL_BYTES_SENT.ordinal(),
            new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableOutbound, FederationDestinationDetailsData.Column.TOTAL_ENTRIES_SENT.ordinal(),
            new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableOutbound, FederationDestinationDetailsData.Column.TOTAL_RECORDS_SENT.ordinal(),
            new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableOutbound, FederationDestinationDetailsData.Column.TOTAL_MSG_SENT.ordinal(),
            new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableOutbound, FederationDestinationDetailsData.Column.TOTAL_MSG_UNACKED.ordinal(),
            new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableInbound, FederationOriginDetailsData.Column.TOTAL_BYTES_RECEIVED.ordinal(),
            new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableInbound, FederationOriginDetailsData.Column.TOTAL_RECORDS_RECEIVED.ordinal(),
            new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableInbound, FederationOriginDetailsData.Column.TOTAL_ENTRIES_RECEIVED.ordinal(),
            new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableInbound, FederationOriginDetailsData.Column.TOTAL_MSG_RECEIVED.ordinal(),
            new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableInbound, FederationOriginDetailsData.Column.TOTAL_MSG_UNACKED.ordinal(),
            new RenderHelper.IntegerRenderer());

        RenderHelper.setHeaderAlignment(tableFed, JLabel.CENTER);
        RenderHelper.setHeaderAlignment(tableInbound, JLabel.CENTER);
        RenderHelper.setHeaderAlignment(tableOutbound, JLabel.CENTER);

        // set sizes
        tableFed.setPreferredScrollableViewportSize(new Dimension(500, tableFed.getRowHeight() * 4));
        tableInbound.setPreferredScrollableViewportSize(new Dimension(500, tableInbound.getRowHeight() * 5));
        tableOutbound.setPreferredScrollableViewportSize(new Dimension(500, tableOutbound.getRowHeight() * 5));

        tableFed.setIntercellSpacing(new Dimension(6, 3));
        tableFed.setRowHeight(tableFed.getRowHeight() + 4);

        tableInbound.setIntercellSpacing(new Dimension(6, 3));
        tableInbound.setRowHeight(tableFed.getRowHeight() + 4);

        tableOutbound.setIntercellSpacing(new Dimension(6, 3));
        tableOutbound.setRowHeight(tableFed.getRowHeight() + 4);

        // adding and nesting
        pneSplitOutboundDetail.add(txtPanelOutboundDetailTotal);

        pneSplitInboundDetail.add(pneTabInboundGraph);
        pneSplitOutboundDetail.add(pneTabOutboundGraph);

        pneInboundDetail.add(pneSplitInboundDetail);
        pneOutboundDetail.add(pneSplitOutboundDetail);

        pneSplitInbound.add(pneScrollInbound);
        pneSplitOutbound.add(pneScrollOutbound);

        pneSplitInbound.add(pneInboundDetail);
        pneSplitOutbound.add(pneOutboundDetail);

        pneTabDetail.addTab(getLocalizedText("TAB_outbound"), pneSplitOutbound);
        pneTabDetail.addTab(getLocalizedText("TAB_inbound"), pneSplitInbound);

        pneFedDetail.add(pneTabDetail, BorderLayout.CENTER);

        pneScrollFed.setOpaque(false);
        pneSplitFed.setOpaque(false);
        pneSplitFed.add(pneScrollFed);
        pneSplitFed.add(pneFedDetail);

        add(pneSplitFed);

        // add listener actions
        m_rowSelectModelFed = tableFed.getSelectionModel();
        m_rowSelectModelOutboundDetails = tableOutbound.getSelectionModel();
        m_rowSelectModelInboundDetails = tableInbound.getSelectionModel();

        m_listener = new SelectRowListSelectionListener(tableFed, tableOutbound, tableInbound, pneTabOutboundGraph, pneTabInboundGraph);

        m_rowSelectModelFed.addListSelectionListener(m_listener);
        m_rowSelectModelOutboundDetails.addListSelectionListener(m_listener);
        m_rowSelectModelInboundDetails.addListSelectionListener(m_listener);
        }

    /**
     * Populate the graphs in the outbound detail tabs.
     *
     * @param pneDetailTabs the {@link JTabbedPane} to update
     */
    private void populateOutboundTabs(JTabbedPane pneDetailTabs)
        {
        // remove any existing tabs
        int cTabs = pneDetailTabs.getTabCount();

        for (int i = 0; i < cTabs; i++)
            {
            pneDetailTabs.removeTabAt(0);
            }

        m_bandwidthUtilGraph = GraphHelper.createBandwidthUtilGraph();
        pneDetailTabs.addTab(getLocalizedText("LBL_bandwidth_utilization"), m_bandwidthUtilGraph.getChart());

        m_recordBacklogDelayGraph = GraphHelper.createOutboundPercentileGraph();
        pneDetailTabs.addTab(getLocalizedText("LBL_replication_percentile_millis"), m_recordBacklogDelayGraph.getChart());
        }

    /**
     * Populate the graphs in the inbound detail tabs.
     *
     * @param pneDetailTabs the {@link JTabbedPane} to update
     */
    private void populateInboundTabs(JTabbedPane pneDetailTabs)
        {
        // remove any existing tabs
        int cTabs = pneDetailTabs.getTabCount();

        for (int i = 0; i < cTabs; i++)
            {
            pneDetailTabs.removeTabAt(0);
            }

        m_graphInboundPercentile = GraphHelper.createInboundPercentileGraph();
        pneDetailTabs.addTab(getLocalizedText("LBL_replication_percentile_millis"), m_graphInboundPercentile.getChart());
        }

    // ----- AbstractCoherencePanel methods ---------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGUI()
        {
        // update the data display in each table
        m_tmodelFed.fireTableDataChanged();
        m_tmodelOutbound.fireTableDataChanged();
        m_tmodelInbound.fireTableDataChanged();

        // re-select the selected rows
        if (model.getSelectedServiceParticipant() != null)
            {
            m_listener.updateRowSelections();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateData()
        {
        // get the merged data for tableFed
        m_federationData = getMergedFederationData();

        if (m_federationData != null)
            {
            // update data for tableFEd
            m_tmodelFed.setDataList(m_federationData);
            m_tmodelFed.fireTableDataChanged();
            }

        // check if there is a row selected in the tableFed
        if (model.getSelectedServiceParticipant() != null)
            {
            // get outbound details data
            m_fedDestinationDetailsData = model.getData(VisualVMModel.DataType.FEDERATION_DESTINATION_DETAILS);

            // get inbound details data
            m_fedOriginDetailData = model.getData(VisualVMModel.DataType.FEDERATION_ORIGIN_DETAILS);

            // update outbound details data
            m_tmodelOutbound.setDataList(m_fedDestinationDetailsData);
            m_tmodelOutbound.fireTableDataChanged();

            // update inbound details data
            m_tmodelInbound.setDataList(m_fedOriginDetailData);
            m_tmodelInbound.fireTableDataChanged();

            // update the outbound tab
            String sSelectedNode = model.getSelectedNodeOutbound();
            if(sSelectedNode != null)
                {
                if (m_fedDestinationDetailsData != null)
                    {
                    for (Entry<Object, Data> entry : m_fedDestinationDetailsData)
                        {
                        String sNodeId = (String) entry.getValue().getColumn(FederationDestinationDetailsData.Column.NODE_ID.ordinal());
                        if (sNodeId.equals(sSelectedNode))
                            {
                            Long backlog           = (Long) entry.getValue().getColumn(FederationDestinationDetailsData.Column.RECORD_BACKLOG_DELAY_TIME_PERCENTILE_MILLIS.ordinal());
                            Long network           = (Long) entry.getValue().getColumn(FederationDestinationDetailsData.Column.MSG_NETWORK_ROUND_TRIP_TIME_PERCENTILE_MILLIS.ordinal());
                            Long apply             = (Long) entry.getValue().getColumn(FederationDestinationDetailsData.Column.MSG_APPLY_TIME_PERCENTILE_MILLIS.ordinal());
                            Float currentBandwidth = (Float) entry.getValue().getColumn(FederationDestinationDetailsData.Column.CURRENT_BANDWIDTH.ordinal());
                            String maxBandwidth    = (String) entry.getValue().getColumn(FederationDestinationDetailsData.Column.MAX_BANDWIDTH.ordinal());
                            String sErrorDesp      = (String) entry.getValue().getColumn(FederationDestinationDetailsData.Column.ERROR_DESCRIPTION.ordinal());

                            // add new values  to graphs
                            GraphHelper.addValuesToOutboundPercentileDelayGraph(m_recordBacklogDelayGraph, backlog, network, apply);
                            GraphHelper.addValuesToBandwidthUtilGraph(m_bandwidthUtilGraph, new Float(maxBandwidth), currentBandwidth);

                            // update the error description in textfield
                            m_txtOutboundErrorDesp.setText(sErrorDesp);
                            m_txtOutboundErrorDesp.setToolTipText(sErrorDesp);
                            }
                        }
                    }
                }

            // update the inbound tab
            sSelectedNode = model.getSelectedNodeInbound();
            if(sSelectedNode != null)
                {
                if (m_fedOriginDetailData != null)
                    {
                    for (Entry<Object, Data> entry : m_fedOriginDetailData)
                        {
                        String sNodeId = (String) entry.getValue().getColumn(FederationOriginDetailsData.Column.NODE_ID.ordinal());
                        if (sNodeId.equals(sSelectedNode))
                            {
                            Long apply   = (Long) entry.getValue().getColumn(FederationOriginDetailsData.Column.MSG_APPLY_TIME_PERCENTILE_MILLIS.ordinal());
                            Long backlog = (Long) entry.getValue().getColumn(FederationOriginDetailsData.Column.RECORD_BACKLOG_DELAY_TIME_PERCENTILE_MILLIS.ordinal());

                            // add the new value to the graph
                            GraphHelper.addValuesToInboundPercentileGraph(m_graphInboundPercentile, backlog, apply);
                            }
                        }
                    }
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Merge the destination and origin data into a list. For each federation service,
     * only those participants which at least have one origin or one destination will
     * show in this list.
     *
     * @return a merged list of entries which contain names, status and some aggregation stats
     *         of service / participant pairs
     */
    private List<Entry<Object, Data>> getMergedFederationData()
        {
        // get data of destinations and origins
        List<Entry<Object, Data>> fedDstData = model.getData(VisualVMModel.DataType.FEDERATION_DESTINATION);
        List<Entry<Object, Data>> fedOriginData = model.getData(VisualVMModel.DataType.FEDERATION_ORIGIN);

        if (fedDstData == null)
            {
            return fedOriginData;
            }
        else if (fedOriginData == null)
            {
            return fedDstData;
            }
        else
            {
            // remove duplicate entries
            for (Entry<Object, Data> entryOrig : fedOriginData)
                {
                Pair key = (Pair) entryOrig.getKey();
                boolean fFound = false;

                // merge destination data and origin data into one entry (in destination data list)
                for (Entry<Object, Data> entryDst : fedDstData)
                    {
                    if (entryDst.getKey().equals(key))
                        {
                        Data dstData = entryDst.getValue();
                        dstData.setColumn(FederationData.Column.TOTAL_BYTES_RECEIVED.ordinal(),
                            entryOrig.getValue().getColumn(FederationData.Column.TOTAL_BYTES_RECEIVED.ordinal()));
                        dstData.setColumn(FederationData.Column.TOTAL_MSGS_RECEIVED.ordinal(),
                            entryOrig.getValue().getColumn(FederationData.Column.TOTAL_MSGS_RECEIVED.ordinal()));
                        fFound = true;
                        break;
                        }
                    }
                if (!fFound)
                    {
                    // add to dst list
                    fedDstData.add(entryOrig);
                    }
                }
            return fedDstData;
            }
        }

    /**
     * Set the text content for several textfields.
     *
     * @param sMaxBandwidth  the maxbandwidth
     * @param sSendTimeout   the send time out
     * @param sGeoIp         the Geo IP
     * @param sErrorDesp     the error description
     */
    private void setTextDetailsValue(String sMaxBandwidth, String sSendTimeout,
                                        String sGeoIp, String sErrorDesp)
        {
        m_txtOutboundMaxBandwidth.setText("0.0".equals(sMaxBandwidth) ? "Not Set" : sMaxBandwidth);
        m_txtOutboundSendTimeOut.setText(sSendTimeout);
        m_txtOutboundGeoIp.setText(sGeoIp);
        m_txtOutboundErrorDesp.setText(sErrorDesp);
        m_txtOutboundErrorDesp.setToolTipText(sErrorDesp);
        }

    // ----- inner classes --------------------------------------------------

    /**
     * MenuOption for start jmx operation in FederationManager.
     */
    private class StartMenuOption
        extends AbstractMenuOption
        {
        // ----- constructors -----------------------------------------------

        /**
         * {@inheritDoc}
         */
        public StartMenuOption(VisualVMModel model, RequestSender requestSender,
            ExportableJTable jtable, int nStartType)
            {
            super(model, requestSender, jtable);
            f_nStartType = nStartType;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMenuItem()
            {
            return getLocalizedText(f_nStartType  == START           ? "LBL_start_menu" :
                                    (f_nStartType == START_WITH_SYNC ? "LBL_start_menu_with_sync" :
                                                                       "LBL_start_menu_with_no_backlog"));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e)
            {
            int nRow = getSelectedRow();
            String sService = null;

            if (nRow == -1)
                {
                JOptionPane.showMessageDialog(null, getLocalizedText("LBL_must_select_row"));
                }
            else
                {
                try
                    {
                    sService            = (String) getJTable().getModel().getValueAt(nRow, 0);
                    String sParticipant = (String) getJTable().getModel().getValueAt(nRow, 1);

                    String sOperation   =  f_nStartType == START ? "start" :
                                          (f_nStartType == START_WITH_SYNC ? "startWithSync" : "startWithNoBacklog");

                    if (confirmOperation(sOperation.toUpperCase(), sParticipant))
                        {

                        requestSender.invokeFederationOperation(sService, sOperation, sParticipant);
                        showMessageDialog(Localization.getLocalText("LBL_details_service", new String[] {sService}),
                                          Localization.getLocalText("LBL_operation_result_done", new String[] {sOperation.toUpperCase(), sParticipant}),
                                          JOptionPane.INFORMATION_MESSAGE, 400, 50);
                        }
                    }
                catch (Exception ee)
                    {
                    showMessageDialog(Localization.getLocalText("ERR_cannot_run", new String[] {sService}),
                                      ee.getMessage(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

        // ---- constants ---------------------------------------------------

        /**
         * Indicates normal start.
         */
        public static final int START = 0;

        /**
         * Indicates start with sync.
         */
        public static final int START_WITH_SYNC = 1;

        /**
         * Indicates start with no backlog.
         */
        public static final int START_WITH_NO_BACKLOG = 2;

        // ---- data members ------------------------------------------------

        /**
         * Type of start operation. 0 = normal start, 1 = start with SYNC, 2 = start  with no backlog
         */
        private final int f_nStartType;
        }

    /**
     * MenuOption for stop jmx operation in FederationManager.
     */
    private class StopMenuOption
        extends AbstractMenuOption
        {
        // ----- constructors -----------------------------------------------

        /**
         * {@inheritDoc}
         */
        public StopMenuOption(VisualVMModel model, RequestSender requestSender,
            ExportableJTable jtable)
            {
            super(model, requestSender, jtable);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMenuItem()
            {
            return getLocalizedText("LBL_stop_menu");
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e)
            {
            int nRow = getSelectedRow();
            String sService = null;

            if (nRow == -1)
                {
                JOptionPane.showMessageDialog(null, getLocalizedText("LBL_must_select_row"));
                }
            else
                {
                try
                    {
                    sService            = (String) getJTable().getModel().getValueAt(nRow, 0);
                    String sParticipant = (String) getJTable().getModel().getValueAt(nRow, 1);

                    if (confirmOperation("STOP", sParticipant))
                        {
                        requestSender.invokeFederationOperation(sService, "stop", sParticipant);

                        showMessageDialog(Localization.getLocalText("LBL_details_service", new String[] {sService}),
                                          Localization.getLocalText("LBL_operation_result_done", new String[] {"STOP", sParticipant}),
                                          JOptionPane.INFORMATION_MESSAGE, 400, 50);
                        }
                    }
                catch (Exception ee)
                    {
                    showMessageDialog(Localization.getLocalText("ERR_cannot_run", new String[] {sService}),
                                      ee.getMessage(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

    /**
     * MenuOption for pasue jmx operation in FederationManager.
     */
    private class PauseMenuOption
        extends AbstractMenuOption
        {
        // ----- constructors -----------------------------------------------

        /**
         * {@inheritDoc}
         */
        public PauseMenuOption(VisualVMModel model, RequestSender requestSender,
            ExportableJTable jtable)
            {
            super(model, requestSender, jtable);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMenuItem()
            {
            return getLocalizedText("LBL_pause_menu");
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e)
            {
            int nRow = getSelectedRow();
            String sService = null;

            if (nRow == -1)
                {
                JOptionPane.showMessageDialog(null, getLocalizedText("LBL_must_select_row"));
                }
            else
                {
                try
                    {
                    sService            = (String) getJTable().getModel().getValueAt(nRow, 0);
                    String sParticipant = (String) getJTable().getModel().getValueAt(nRow, 1);

                    if (confirmOperation("PAUSE", sParticipant))
                        {
                        requestSender.invokeFederationOperation(sService, "pause", sParticipant);

                        showMessageDialog(Localization.getLocalText("LBL_details_service", new String[] {sService}),
                                          Localization.getLocalText("LBL_operation_result_done", new String[] {"PAUSE", sParticipant}),
                                          JOptionPane.INFORMATION_MESSAGE, 400, 50);
                        }
                    }
                catch (Exception ee)
                    {
                    showMessageDialog(Localization.getLocalText("ERR_cannot_run", new String[] {sService}),
                                      ee.getMessage(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

    /**
     * MenuOption for replicateAll jmx operation in FederationManager.
     */
    private class ReplicateAllMenuOption
        extends AbstractMenuOption
        {
        // ----- constructors -----------------------------------------------

        /**
         * {@inheritDoc}
         */
        public ReplicateAllMenuOption(VisualVMModel model, RequestSender requestSender,
            ExportableJTable jtable)
            {
            super(model, requestSender, jtable);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMenuItem()
            {
            return getLocalizedText("LBL_replicate_all_menu");
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e)
            {
            int nRow = getSelectedRow();
            String sService = null;

            if (nRow == -1)
                {
                JOptionPane.showMessageDialog(null, getLocalizedText("LBL_must_select_row"));
                }
            else
                {
                try
                    {
                    sService            = (String) getJTable().getModel().getValueAt(nRow, 0);
                    String sParticipant = (String) getJTable().getModel().getValueAt(nRow, 1);

                    if (confirmOperation("REPLICATE ALL", sParticipant))
                        {
                        requestSender.invokeFederationOperation(sService, "replicateAll", sParticipant);

                        showMessageDialog(Localization.getLocalText("LBL_details_service", new String[]{sService}),
                                          Localization.getLocalText("LBL_operation_result_done", new String[] {"REPLICATE ALL", sParticipant}),
                                          JOptionPane.INFORMATION_MESSAGE, 400, 50);
                        }
                    }
                catch (Exception ee)
                    {
                    showMessageDialog(Localization.getLocalText("ERR_cannot_run", new String[] {sService}),
                                      ee.getMessage(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

    /**
     * MenuOption for retrievePendingIncomingMessages jmx operation in FederationManager.
     */
    private class RetrievePendingIncomingMessagesMenuOption
        extends AbstractMenuOption
        {
        // ----- constructors -----------------------------------------------

        /**
         * {@inheritDoc}
         */
        public RetrievePendingIncomingMessagesMenuOption(VisualVMModel model, RequestSender requestSender,
            ExportableJTable jtable)
            {
            super(model, requestSender, jtable);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMenuItem()
            {
            return getLocalizedText("LBL_retrieve_incoming_menu");
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e)
            {
            int nRow = getSelectedRow();
            String sService = null;

            if (nRow == -1)
                {
                JOptionPane.showMessageDialog(null, getLocalizedText("LBL_must_select_row"));
                }
            else
                {
                try
                    {
                    sService        = (String) getJTable().getModel().getValueAt(nRow, 0);

                    if (JOptionPane.showConfirmDialog(null,
                        Localization.getLocalText("LBL_incoming_msg_result_menu", new String[] {sService}),
                        Localization.getLocalText("LBL_confirm_operation"),
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                        {
                        Integer cResult =  requestSender.retrievePendingIncomingMessages(sService);

                        showMessageDialog(Localization.getLocalText("LBL_details_service", new String[]{sService}),
                                          Localization.getLocalText("LBL_result_is", new String[] {cResult.toString()}),
                                          JOptionPane.INFORMATION_MESSAGE, 400, 50);
                        }
                    }
                catch (Exception ee)
                    {
                    showMessageDialog(Localization.getLocalText("ERR_cannot_run", new String[] {sService}),
                                      ee.getMessage(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

    /**
     * MenuOption for retrievePendingOutgoingMessages jmx operation in FederationManager.
     */
    private class RetrievePendingOutgoingMessagesMenuOption
        extends AbstractMenuOption
        {
        // ----- constructors -----------------------------------------------

        /**
         * {@inheritDoc}
         */
        public RetrievePendingOutgoingMessagesMenuOption(VisualVMModel model, RequestSender requestSender,
            ExportableJTable jtable)
            {
            super(model, requestSender, jtable);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMenuItem()
            {
            return getLocalizedText("LBL_retrieve_outgoing_menu");
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent e)
            {
            int nRow = getSelectedRow();
            String sService = null;

            if (nRow == -1)
                {
                JOptionPane.showMessageDialog(null, getLocalizedText("LBL_must_select_row"));
                }
            else
                {
                try
                    {
                    sService        = (String) getJTable().getModel().getValueAt(nRow, 0);
                    if (JOptionPane.showConfirmDialog(null,
                        Localization.getLocalText("LBL_outgoing_msg_result_menu", new String[] {sService}),
                        Localization.getLocalText("LBL_confirm_operation"),
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                        {

                        Integer cResult =  requestSender.retrievePendingOutgoingMessages(sService);

/*
String sObjName = getFederationManagerObjectName(sService);
Integer cResult = (Integer) requestSender.invoke(new ObjectName(sObjName),
                                                                  "retrievePendingOutgoingMessages",
                                                                  new Object[]{}, new String[]{});*/

                        showMessageDialog(Localization.getLocalText("LBL_details_service", new String[]{sService}),
                                          Localization.getLocalText("LBL_result_is", new String[]{cResult.toString()}),
                                          JOptionPane.INFORMATION_MESSAGE, 400, 50);
                        }
                    }
                catch (Exception ee)
                    {
                    showMessageDialog(Localization.getLocalText("ERR_cannot_run", new String[] {sService}),
                                      ee.getMessage(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

    /**
     * Inner class to monitor and change all data displays when row selection events
     * happen in this tab.
     */
    private class SelectRowListSelectionListener
            implements ListSelectionListener
        {

        // ----- constructors -----------------------------------------------

        /**
         * Create the listener to monitor row selection events.
         *
         * @param  tableFed            the {@link ExportableJTable} that is to be selected
         * @param  tableOutbound       the {@link ExportableJTable} that is to be selected
         * @param  tableInbound        the {@link ExportableJTable} that is to be selected
         * @param  pneTabOutboundGraph the {@link JTabbedPane} to update
         * @param  pneTabInboundGraph  the {@link JTabbedPane} to update
         */
        public SelectRowListSelectionListener(ExportableJTable tableFed, ExportableJTable tableOutbound,
                                              ExportableJTable tableInbound, JTabbedPane pneTabOutboundGraph,
                                              JTabbedPane pneTabInboundGraph)
            {
            m_tableFed            = tableFed;
            m_tableOutbound       = tableOutbound;
            m_tableInbound        = tableInbound;
            m_pneTabOutboundGraph = pneTabOutboundGraph;
            m_pneTabInboundGraph  = pneTabInboundGraph;
            }

        // ----- ListSelectionListener methods ------------------------------

        /**
         * React when a row is selected.
         *
         * @param e {@link ListSelectionEvent} to respond to
         */
        public void valueChanged(ListSelectionEvent e)
            {
            if (e.getValueIsAdjusting())
                {
                return;
                }

            ListSelectionModel selectionModel = (ListSelectionModel) e.getSource();

            if (selectionModel.isSelectionEmpty())
                {
                return;
                }

            // select the destination detail (outbound) table
            if (selectionModel == m_rowSelectModelOutboundDetails && m_fedDestinationDetailsData != null)
                {
                // will update the selected row
                m_updateRowOutbound = true;

                // get the selected row index
                m_nSelectedRowOutbound = selectionModel.getMinSelectionIndex();

                Map.Entry<Object, Data> entry = m_fedDestinationDetailsData.get(m_nSelectedRowOutbound);
                Data data = entry.getValue();
                String sSelectedNode = (String) data.getColumn(FederationDestinationDetailsData.Column.NODE_ID.ordinal());

                // the selected node has changed
                if (!sSelectedNode.equals(model.getSelectedNodeOutbound()))
                    {
                    // update the selected node ID in VisualVMModel
                    model.setSelectedNodeOutbound(sSelectedNode);

                    String sMaxBandwidth = (String) data.getColumn(FederationDestinationDetailsData.Column.MAX_BANDWIDTH.ordinal());
                    String sSendTimeout  = (String) data.getColumn(FederationDestinationDetailsData.Column.SEND_TIMEOUT_MILLIS.ordinal());
                    String sGeoIp        = (String) data.getColumn(FederationDestinationDetailsData.Column.GEO_IP.ordinal());
                    String sErrorDesp    = (String) data.getColumn(FederationDestinationDetailsData.Column.ERROR_DESCRIPTION.ordinal());

                    // set textfields content
                    setTextDetailsValue(sMaxBandwidth, sSendTimeout, sGeoIp, sErrorDesp);

                    // update graphs
                    populateOutboundTabs(m_pneTabOutboundGraph);
                    }

                return;
                }
            // select the origin detail (inbound) table
            else if (selectionModel == m_rowSelectModelInboundDetails && m_fedOriginDetailData != null)
                {
                //will update the selected row
                m_updateRowInbound = true;

                // get the selected row index
                m_nSelectedRowInbound = selectionModel.getMinSelectionIndex();

                Map.Entry<Object, Data> entry = m_fedOriginDetailData.get(m_nSelectedRowInbound);
                String sSelectedNode = (String) entry.getValue().getColumn(FederationOriginDetailsData.Column.NODE_ID.ordinal());

                // the selected node has changed
                if (!sSelectedNode.equals(model.getSelectedNodeInbound()))
                    {
                    // update the selected node ID in VisualVMModel
                    model.setSelectedNodeInbound(sSelectedNode);

                    // update graphs
                    populateInboundTabs(m_pneTabInboundGraph);
                    }

                return;
                }
            // select the federation table
            else if (selectionModel == m_rowSelectModelFed)
                {
                // will update the selected row
                m_updateRowFed = true;

                // get the selected row index
                m_nSelectedRowFed = selectionModel.getMinSelectionIndex();

                // get the service at the selected row, which is the first column
                String sSelectedService = (String) m_tableFed.getValueAt(m_nSelectedRowFed, 0);
                String sParticipant     = (String) m_tableFed.getValueAt(m_nSelectedRowFed, 1);

                Pair<String, String> serviceParticipant = new Pair(sSelectedService, sParticipant);

                // selected service/participant has changed
                if (!serviceParticipant.equals(model.getSelectedServiceParticipant()))
                    {
                    // update the selected service / participant pair in VisualVMModel
                    model.setSelectedServiceParticipant(serviceParticipant);

                    // get rid of old details data for inbound and outbound
                    model.eraseFederationDetailsData();

                    // update destails data display
                    m_tmodelOutbound.setDataList(null);
                    m_tmodelOutbound.fireTableDataChanged();
                    m_tmodelInbound.setDataList(null);
                    m_tmodelInbound.fireTableDataChanged();

                    // update textfields
                    setTextDetailsValue("", "", "", "");

                    // update graphs
                    populateOutboundTabs(m_pneTabOutboundGraph);
                    populateInboundTabs(m_pneTabInboundGraph);
                    }
                return;
                }
            }

        /**
         * Re-select the last selected rows.
         */
        public void updateRowSelections()
            {
            if (m_updateRowFed)
                {
                m_tableFed.addRowSelectionInterval(m_nSelectedRowFed, m_nSelectedRowFed);
                }
            if (m_updateRowOutbound)
                {
                m_tableOutbound.addRowSelectionInterval(m_nSelectedRowOutbound, m_nSelectedRowOutbound);
                }
            if (m_updateRowInbound)
                {
                m_tableInbound.addRowSelectionInterval(m_nSelectedRowInbound, m_nSelectedRowInbound);
                }
            }

        /**
         * The {@link ExportableJTable} that is to be selected.
         */
        private ExportableJTable m_tableFed;

        /**
         * The {@link ExportableJTable} that is to be selected.
         */
        private ExportableJTable m_tableOutbound;

        /**
         * The {@link ExportableJTable} that is to be selected.
         */
        private ExportableJTable m_tableInbound;

        /**
         * The {@link JTabbedPane} to update.
         */
        private JTabbedPane m_pneTabOutboundGraph;

        /**
         * The {@link JTabbedPane} to update.
         */
        private JTabbedPane m_pneTabInboundGraph;

        /**
         * Whether the row in tableFed is selected.
         */
        private boolean m_updateRowFed = false;

        /**
         * Whether the row in tableOutbound is selected.
         */
        private boolean m_updateRowOutbound = false;

        /**
         * Whether the row in tableInbound is selected.
         */
        private boolean m_updateRowInbound = false;

        /**
         * The selected row index in tableFed.
         */
        private int m_nSelectedRowFed;

        /**
         * The selected row index in tableOutbound.
         */
        private int m_nSelectedRowOutbound;

        /**
         * The selected row index in tableInbound.
         */
        private int m_nSelectedRowInbound;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Ask a question to confirm the operation against a participant.
     *
     * @param sOperation    operation to perform
     * @param sParticipant  the participant to execute against
     *
     * @return true if the user selected Yes
     */
    private boolean confirmOperation(String sOperation, String sParticipant)
        {
        return JOptionPane.showConfirmDialog(null,
                Localization.getLocalText("LBL_operation_result_menu", new String[] {sOperation, sParticipant}),
                Localization.getLocalText("LBL_confirm_operation"),
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 6174232358605085951L;

    // ----- data members ---------------------------------------------------

    /**
     * The textfield holds the max bandwidth data from destination.
     */
    private JTextField m_txtOutboundMaxBandwidth;

    /**
     * The textfield holds the send time out data from destination.
     */
    private JTextField m_txtOutboundSendTimeOut;

    /**
     * The textfield holds the Geo-Ip data from destination.
     */
    private JTextField m_txtOutboundGeoIp;

    /**
     * The textfield holds the error description data from destination.
     */
    private JTextField m_txtOutboundErrorDesp;

    /**
     * The {@link FederationTableModel} to display the merged destination and origin data.
     */
    private FederationTableModel m_tmodelFed;

    /**
     * The {@link FederationInboundTableModel} to display the inbound detail data.
     */
    private FederationInboundTableModel m_tmodelInbound;

    /**
     * The {@link FederationOutboundTableModel} to display the outbound detail data.
     */
    private FederationOutboundTableModel m_tmodelOutbound;

    /**
     * The merged fedetation data from the destination data and origin data.
     */
    private List<Entry<Object, Data>> m_federationData;

    /**
     * The destination detail data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> m_fedDestinationDetailsData;

    /**
     * The origin detail data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> m_fedOriginDetailData;

    /**
     * The graph of record backlog delay percentiles.
     */
    private SimpleXYChartSupport m_recordBacklogDelayGraph = null;

    /**
     * The graph of bandwidth utilization percentiles.
     */
    private SimpleXYChartSupport m_bandwidthUtilGraph = null;

    /**
     * The graph of inbound message apply time percentiles.
     */
    private SimpleXYChartSupport m_graphInboundPercentile = null;

    /**
     * The row selection listener.
     */
    private SelectRowListSelectionListener m_listener;

    /**
     * The {@link ListSelectionModel} of tableFed.
     */
    private ListSelectionModel m_rowSelectModelFed;

    /**
     * The {@link ListSelectionModel} of tableOutbound.
     */
    private ListSelectionModel m_rowSelectModelOutboundDetails;

    /**
     * The {@link ListSelectionModel} of tableInbound.
     */
    private ListSelectionModel m_rowSelectModelInboundDetails;
    }
