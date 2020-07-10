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
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;
import com.sun.tools.visualvm.modules.coherence.panel.util.AbstractMenuOption;
import com.sun.tools.visualvm.modules.coherence.panel.util.ExportableJTable;
import com.sun.tools.visualvm.modules.coherence.panel.util.MenuOption;
import com.sun.tools.visualvm.modules.coherence.tablemodel.MemberTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ClusterData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.MemberData;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.graalvm.visualvm.charts.SimpleXYChartSupport;

/**
 * An implementation of an {@link AbstractCoherencePanel} to
 * view summarized member data.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class CoherenceMemberPanel
        extends AbstractCoherencePanel
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create the layout for the {@link CoherenceMemberPanel}.
     *
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherenceMemberPanel(VisualVMModel model)
        {
        super(new BorderLayout(), model);

        // create a split pane for resizing
        JSplitPane pneSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pneSplit.setOpaque(false);

        // Create the header panel
        JPanel     pnlHeader = new JPanel();

        GridLayout layHeader = new GridLayout(5, 5);

        layHeader.setHgap(30);
        layHeader.setVgap(2);
        pnlHeader.setLayout(layHeader);
        pnlHeader.setOpaque(false);

        // row 1
        txtClusterName = getTextField(10, JTextField.LEFT);
        pnlHeader.add(getLocalizedLabel("LBL_cluster_name", txtClusterName));
        pnlHeader.add(txtClusterName);

        pnlHeader.add(getFiller());

        txtLicenseMode = getTextField(5, JTextField.LEFT);
        pnlHeader.add(getLocalizedLabel("LBL_license_mode", txtLicenseMode));
        pnlHeader.add(txtLicenseMode);

        // row 2
        txtVersion = getTextField(10, JTextField.LEFT);
        pnlHeader.add(getLocalizedLabel("LBL_version", txtVersion));
        pnlHeader.add(txtVersion);
        
        pnlHeader.add(getFiller());

        txtEdition = getTextField(5, JTextField.LEFT);
        pnlHeader.add(getLocalizedLabel("LBL_edition", txtEdition));
        pnlHeader.add(txtEdition);

        // row 3
        txtTotalMembers = getTextField(4);
        pnlHeader.add(getLocalizedLabel("LBL_total_members"));
        pnlHeader.add(txtTotalMembers);

        pnlHeader.add(getFiller());

        txtTotalMemory = getTextField(6);
        pnlHeader.add(getLocalizedLabel("LBL_total_cluster_memory", txtTotalMemory));
        pnlHeader.add(txtTotalMemory);

        // row 4
        txtTotalStorageMembers = getTextField(4);
        pnlHeader.add(getLocalizedLabel("LBL_total_storage_members"));
        pnlHeader.add(txtTotalStorageMembers);

        pnlHeader.add(getFiller());

        txtTotalMemoryUsed = getTextField(6);
        pnlHeader.add(getLocalizedLabel("LBL_total_cluster_memory_used", txtTotalMemoryUsed));
        pnlHeader.add(txtTotalMemoryUsed);

        // row 5
        txtDepartureCount = getTextField(5);
        pnlHeader.add(getLocalizedLabel("LBL_member_departure_count", txtDepartureCount));
        pnlHeader.add(txtDepartureCount);
        
        pnlHeader.add(getFiller());

        txtTotalMemoryAvail = getTextField(6);
        pnlHeader.add(getLocalizedLabel("LBL_total_cluster_memory_avail", txtTotalMemoryAvail));
        pnlHeader.add(txtTotalMemoryAvail);

        pnlHeader.setBorder(new CompoundBorder(new TitledBorder(getLocalizedText("LBL_overview")),
            new EmptyBorder(10, 10, 10, 10)));

        // create the table
        tmodel = new MemberTableModel(VisualVMModel.DataType.MEMBER.getMetadata());

        table = new ExportableJTable(tmodel);

        table.setPreferredScrollableViewportSize(new Dimension(500, table.getRowHeight() * 4));

        // define renderers for the columns
        RenderHelper.setColumnRenderer(table, MemberData.PUBLISHER_SUCCESS, new RenderHelper.SuccessRateRenderer());
        RenderHelper.setColumnRenderer(table, MemberData.RECEIVER_SUCCESS, new RenderHelper.SuccessRateRenderer());
        RenderHelper.setColumnRenderer(table, MemberData.SENDQ_SIZE, new RenderHelper.IntegerRenderer());

        RenderHelper.setHeaderAlignment(table, JLabel.CENTER);

        // Add some space
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        MenuOption menuDetail = new ShowDetailMenuOption(model, table, SELECTED_NODE);

        // reportNodeDetails only available in 12.2.1 and above
        if (model.getClusterVersionAsInt() >= 122100)
            {
            table.setMenuOptions(new MenuOption[] { menuDetail, new ReportNodeStateMenuOption(model, requestSender, table) });
            }
        else
            {
            table.setMenuOptions(new MenuOption[] { menuDetail });
            }

        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
        configureScrollPane(scrollPane, table);

        JPanel      topPanel   = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        topPanel.add(pnlHeader, BorderLayout.PAGE_START);
        topPanel.add(scrollPane, BorderLayout.CENTER);

        // create a chart for the total cluster memory
        memoryGraph = GraphHelper.createClusterMemoryGraph();

        JPanel pnlPlotter = new JPanel(new GridLayout(1, 1));

        pnlPlotter.add(memoryGraph.getChart());

        pneSplit.add(topPanel);
        pneSplit.add(pnlPlotter);

        add(pneSplit);
        }

    // ----- AbstractCoherencePanel methods ----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGUI()
        {
        final  String MEM_FORMAT = "%,d";
        int    cTotalMemory      = 0;
        int    cTotalMemoryUsed  = 0;
        int    cStorageCount     = 0;
        String sEdition          = "";

        Map<String, AtomicInteger> mapEditionCount = new TreeMap<>();

        if (memberData != null)
            {
            txtTotalMembers.setText(String.format("%5d", memberData.size()));

            for (Entry<Object, Data> entry : memberData)
                {
                // only include memory if node is storage enabled
                if (isNodeStorageEnabled((Integer)  entry.getValue().getColumn(MemberData.NODE_ID)))
                    {
                    cStorageCount++;
                    cTotalMemory     += (Integer) entry.getValue().getColumn(MemberData.MAX_MEMORY);
                    cTotalMemoryUsed += (Integer) entry.getValue().getColumn(MemberData.USED_MEMORY);
                    }

                String sThisEdition = (String) entry.getValue().getColumn(MemberData.PRODUCT_EDITION);
                mapEditionCount.computeIfAbsent(sThisEdition, k-> new AtomicInteger(0)).incrementAndGet();
                }

            // check most common case
            if (mapEditionCount.size() == 1)
                {
                sEdition = mapEditionCount.keySet().iterator().next();
                }
            else
                {
                // we have one or more editions (unlikely) so get the most popular one
                int nMax = -1;
                sEdition = "";
                for (Entry<String, AtomicInteger> entry : mapEditionCount.entrySet())
                    {
                    int count = entry.getValue().get();
                    if (count > nMax)
                        {
                        nMax     = count;
                        sEdition = entry.getKey();
                        }
                    }
                }

            txtTotalMemory.setText(String.format(MEM_FORMAT, cTotalMemory));
            txtTotalMemoryUsed.setText(String.format(MEM_FORMAT, cTotalMemoryUsed));
            txtTotalMemoryAvail.setText(String.format(MEM_FORMAT, cTotalMemory - cTotalMemoryUsed));
            }
        else
            {
            txtTotalMembers.setText("");
            txtTotalMemory.setText(String.format(MEM_FORMAT, 0));
            txtTotalMemoryUsed.setText(String.format(MEM_FORMAT, 0));
            txtTotalMemoryAvail.setText(String.format(MEM_FORMAT, 0));
            }

        if (clusterData != null)
            {
            for (Entry<Object, Data> entry : clusterData)
                {
                txtClusterName.setText(entry.getValue().getColumn(ClusterData.CLUSTER_NAME).toString());
                txtLicenseMode.setText(entry.getValue().getColumn(ClusterData.LICENSE_MODE).toString());
                txtVersion.setText(entry.getValue().getColumn(ClusterData.VERSION).toString().replaceFirst(" .*$", ""));
                txtDepartureCount.setText(entry.getValue().getColumn(ClusterData.DEPARTURE_COUNT).toString());
                }
            }

        txtEdition.setText(sEdition);
        txtTotalStorageMembers.setText(String.format("%5d", cStorageCount));
        
        fireTableDataChangedWithSelection(table, tmodel);

        // update the memory graph
        if (cTotalMemory != 0)
            {
            GraphHelper.addValuesToClusterMemoryGraph(memoryGraph, cTotalMemory, cTotalMemoryUsed);
            }

        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateData()
        {
        List<Entry<Object, Data>> tempList = new ArrayList<>();

        // go through and set storage enabled column
        for (Entry<Object, Data> entry : model.getData(VisualVMModel.DataType.MEMBER))
            {
            Data data   = entry.getValue();
            int  nodeId = (Integer) entry.getKey();
            if (!isNodeStorageEnabled(nodeId))
                {
                data.setColumn(MemberData.STORAGE_ENABLED,"false");
                }

            tempList.add(entry);
            }

        memberData  = tempList;
        clusterData = model.getData(VisualVMModel.DataType.CLUSTER);

        if (memberData != null)
            {
            tmodel.setDataList(memberData);
            }

        }

    // ----- inner classes ReportNodeDetailsMenuOption ----------------------

    /**
     * A class to call the reportNodeState operation on the selected
     * ClusterNode MBean and display the details.
     */
    private class ReportNodeStateMenuOption extends AbstractMenuOption
        {

        // ----- constructors -----------------------------------------------

        /**
         * {@inheritDoc}
         */
        public ReportNodeStateMenuOption(VisualVMModel model, RequestSender requestSender,
                                  ExportableJTable jtable)
            {
            super(model, requestSender, jtable);
            }

        // ----- MenuOptions methods ----------------------------------------

        @Override
        public String getMenuItem()
            {
            return getLocalizedText("LBL_report_node_state");
            }

        @Override
        public void actionPerformed(ActionEvent e)
            {
            int     nRow    = getSelectedRow();
            Integer nNodeId = null;

            if (nRow == -1)
                {
                JOptionPane.showMessageDialog(null, getLocalizedText("LBL_must_select_row"));
                }
            else
                {
                try
                    {
                    nNodeId = (Integer) getJTable().getModel().getValueAt(nRow, 0);

                    String sResult = requestSender.getNodeState(nNodeId);

                    showMessageDialog(getLocalizedText("LBL_state_for_node") + " " +  nNodeId,sResult, JOptionPane.INFORMATION_MESSAGE);
                    }
                catch (Exception ee)
                    {
                    showMessageDialog("Error running reportNodeState for Node "+ nNodeId, ee.getMessage(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -7612569043492412546L;

    // ----- data members ---------------------------------------------------

    /**
     * The total number of members in the cluster.
     */
    private JTextField txtTotalMembers;

    /**
     * Total number of storage-enabled members.
     */
    private JTextField txtTotalStorageMembers;
    
    /**
     * The total amount of memory allocated in the cluster by all
     * storage-enabled members.
     */
    private JTextField txtTotalMemory;

    /**
     * The total amount of memory available in the cluster by all storage-enabled members.
     */
    private JTextField txtTotalMemoryAvail;

    /**
     * The total amount of memory used in the cluster by all storage-enabled members.
     */
    private JTextField txtTotalMemoryUsed;

    /**
     * The name of the cluster.
     */
    private JTextField txtClusterName;

    /**
     * The license mode of the cluster.
     */
    private JTextField txtLicenseMode;

    /**
     * The edition of the cluster.
     */
    private JTextField txtEdition;

    /**
     * The total number of members departed.
     */
    private JTextField txtDepartureCount;

    /**
     * The Coherence version of the cluster.
     */
    private JTextField txtVersion;

    /**
     * The graph of cluster memory.
     */
    private SimpleXYChartSupport memoryGraph;

    /**
     * The member statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> memberData;

    /**
     * The cluster statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> clusterData;

    /**
     * The {@link MemberTableModel} to display member data.
     */
    protected MemberTableModel tmodel;

    /**
     * the {@link ExportableJTable} to use to display data.
     */
    protected ExportableJTable table;
    }
