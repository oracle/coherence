/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.panel;

import com.sun.tools.visualvm.charts.SimpleXYChartSupport;
import com.sun.tools.visualvm.modules.coherence.VisualVMModel;
import com.sun.tools.visualvm.modules.coherence.helper.GraphHelper;
import com.sun.tools.visualvm.modules.coherence.helper.RenderHelper;
import com.sun.tools.visualvm.modules.coherence.panel.util.ExportableJTable;
import com.sun.tools.visualvm.modules.coherence.tablemodel.MachineTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.MachineData;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import java.util.List;

import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

/**
 * An implementation of an {@link AbstractCoherencePanel} to
 * view summarized machine data.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class CoherenceMachinePanel
        extends AbstractCoherencePanel
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create the layout for the {@link CoherenceMachinePanel}.
     *
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherenceMachinePanel(VisualVMModel model)
        {
        super(new BorderLayout(), model);

        // create a split pane for resizing
        JSplitPane pneSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pneSplit.setOpaque(false);

        // Create the header panel
        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new FlowLayout());
        pnlHeader.setOpaque(false);

        txtTotalMachines = getTextField(5, JTextField.RIGHT);
        pnlHeader.add(getLocalizedLabel("LBL_total_machines", txtTotalMachines));
        pnlHeader.add(txtTotalMachines);

        txtTotalClusterCores = getTextField(5, JTextField.RIGHT);
        pnlHeader.add(getLocalizedLabel("LBL_total_cores", txtTotalClusterCores));
        pnlHeader.add(txtTotalClusterCores);

        // create the table
        tmodel = new MachineTableModel(VisualVMModel.DataType.MACHINE.getMetadata());

        table = new ExportableJTable(tmodel);

        table.setPreferredScrollableViewportSize(new Dimension(500, 150));

        // define renderers for the columns
        RenderHelper.setColumnRenderer(table, MachineData.FREE_PHYSICAL_MEMORY, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, MachineData.TOTAL_PHYSICAL_MEMORY, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, MachineData.SYSTEM_LOAD_AVERAGE,
                                       new RenderHelper.DecimalRenderer(RenderHelper.LOAD_AVERAGE_FORMAT));
        RenderHelper.setColumnRenderer(table, MachineData.PERCENT_FREE_MEMORY, new RenderHelper.FreeMemoryRenderer());
        RenderHelper.setHeaderAlignment(table, JLabel.CENTER);

        // Add some space
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
        configureScrollPane(scrollPane, table);

        JPanel      pnlTop     = new JPanel();

        pnlTop.setLayout(new BorderLayout());
        pnlTop.setOpaque(false);

        pnlTop.add(pnlHeader, BorderLayout.PAGE_START);
        pnlTop.add(scrollPane, BorderLayout.CENTER);

        pneSplit.add(pnlTop);

        // create a chart for the machine load averages
        machineGraph = GraphHelper.createMachineLoadAverageGraph(model);

        JPanel pnlPlotter = new JPanel(new GridLayout(1, 1));

        pnlPlotter.add(machineGraph.getChart());

        pneSplit.add(pnlPlotter);
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

        int          cTotalCores       = 0;
        int          count             = 0;
        double       cLoadAverage      = 0;
        double       cMax              = -1;
        double       cTotalLoadAverage = 0;

        // work out the max and average load averages for the graph
        if (machineData != null)
            {
            txtTotalMachines.setText(String.format("%5d", machineData.size()));

            for (Entry<Object, Data> entry : machineData)
                {
                count++;
                cTotalCores       += (Integer) entry.getValue().getColumn(MachineData.PROCESSOR_COUNT);
                cLoadAverage      = (Double) entry.getValue().getColumn(MachineData.SYSTEM_LOAD_AVERAGE);
                cTotalLoadAverage += cLoadAverage;

                if (cMax == -1 || cLoadAverage > cMax)
                    {
                    cMax = cLoadAverage;
                    }
                }

            txtTotalClusterCores.setText(String.format(MEM_FORMAT, cTotalCores));

            // update graph
            GraphHelper.addValuesToLoadAverageGraph(machineGraph, (float) cMax,
                (float) (cTotalLoadAverage == 0 ? 0 : cTotalLoadAverage / count));
            }
        else
            {
            txtTotalClusterCores.setText(String.format(MEM_FORMAT, 0));
            txtTotalMachines.setText(String.format(MEM_FORMAT, 0));

            }

        fireTableDataChangedWithSelection(table, tmodel);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateData()
        {
        machineData = model.getData(VisualVMModel.DataType.MACHINE);

        if (machineData != null)
            {
            tmodel.setDataList(machineData);
            }
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -7612569043492412546L;

    // ----- data members ---------------------------------------------------

    /**
     * The total number of cores in the cluster.
     */
    private JTextField txtTotalClusterCores;

    /**
     * The total number of machines in the cluster.
     */
    private JTextField txtTotalMachines;

    /**
     * The graph of machine load averages.
     */
    private SimpleXYChartSupport machineGraph;

    /**
     * The machine statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> machineData;

    /**
     * The {@link MachineTableModel} to display machine data.
     */
    protected MachineTableModel tmodel;

    /**
     * the {@link ExportableJTable} to use to display data.
     */
    protected ExportableJTable table;
    }
