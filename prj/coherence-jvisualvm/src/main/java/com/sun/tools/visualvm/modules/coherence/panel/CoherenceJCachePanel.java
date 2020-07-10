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
import com.sun.tools.visualvm.modules.coherence.panel.util.MenuOption;
import com.sun.tools.visualvm.modules.coherence.tablemodel.JCacheConfigurationTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.JCacheConfigurationData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.JCacheStatisticsData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Pair;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.graalvm.visualvm.charts.SimpleXYChartSupport;

/**
 * An implementation of an {@link AbstractCoherencePanel} to
 * view summarized JCache statistics.
 *
 * @author tam  2014.09.22
 * @since  12.1.3
 */
public class CoherenceJCachePanel
        extends AbstractCoherencePanel
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create the layout for the {@link CoherenceJCachePanel}.
     *
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherenceJCachePanel(VisualVMModel model)
        {
        super(new BorderLayout(), model);

        // create a split pane for resizing of top and bottom components
        JSplitPane pneSplitMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        tmodel = new JCacheConfigurationTableModel(VisualVMModel.DataType.JCACHE_CONFIG.getMetadata());

        final ExportableJTable table = new ExportableJTable(tmodel);

        RenderHelper.setColumnRenderer(table, JCacheConfigurationData.CACHE_MANAGER,
                                       new RenderHelper.ToolTipRenderer());

        table.setPreferredScrollableViewportSize(new Dimension(300, table.getRowHeight() * 5));

        // Add some space
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        MenuOption optionShowDetails = new ShowDetailMenuOption(model, table, SELECTED_JCACHE);

        optionShowDetails.setMenuLabel(Localization.getLocalText("LBL_show_jcache_details"));

        // add menu option to show details
        table.setMenuOptions(new MenuOption[] {optionShowDetails});

        // Add top panel to screen
        JPanel      pnlTop    = new JPanel(new BorderLayout());
        JScrollPane pneScroll = new JScrollPane(table);

        pnlTop.add(pneScroll, BorderLayout.CENTER);
        pnlTop.setOpaque(true);

        pneSplitMain.add(pnlTop);
        pneSplitMain.setOpaque(false);

        // add bottom panel to screen
        pnlBottom = new JPanel(new BorderLayout());

        JPanel pnlBottomHeader = new JPanel();
        pnlBottomHeader.setOpaque(false);

        txtSelectedCache = getTextField(40, JTextField.LEFT);
        pnlBottomHeader.add(getLocalizedLabel("LBL_selected_config_cache", txtSelectedCache));
        pnlBottomHeader.add(txtSelectedCache);
        pnlBottom.add(pnlBottomHeader, BorderLayout.PAGE_START);

        addSplitPaneAndGraph();
        pneSplitMain.add(pnlBottom);

        add(pneSplitMain);

        // add a listener for the selected row
        ListSelectionModel rowSelectionModel = table.getSelectionModel();

        listener = new SelectRowListSelectionListener(table);
        rowSelectionModel.addListSelectionListener(listener);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Add the split panel and a new graph on the bottom.
     */
    protected void addSplitPaneAndGraph()
        {
        if (pneSplitBottom != null)
            {
            pnlBottom.remove(pneSplitBottom);
            }

        Pair<String, String> selectedJCache = model.getSelectedJCache();
        String               sCacheName     = selectedJCache == null ? "None Selected" : selectedJCache.getY();

        pneSplitBottom        = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        operationAverageGraph = GraphHelper.createJCacheAverageGraph(sCacheName);
        hitRateGraph          = GraphHelper.createJCacheHitPercentageGraph(sCacheName);
        pneSplitBottom.setResizeWeight(0.3);

        pneSplitBottom.add(operationAverageGraph.getChart());
        pneSplitBottom.add(hitRateGraph.getChart());

        pnlBottom.add(pneSplitBottom, BorderLayout.CENTER);

        }

    // ----- AbstractCoherencePanel methods ---------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGUI()
        {
        tmodel.fireTableDataChanged();

        Pair<String, String> selectedJCache = model.getSelectedJCache();

        if (selectedJCache != null)
            {
            listener.updateRowSelection();

            // update the graph
            if (statsData != null)
                {
                float cAveragePut    = 0.0f;
                float cAverageGet    = 0.0f;
                float cAverageRemove = 0.0f;
                float cHitRate       = 0.0f;

                for (Map.Entry<Object, Data> entry : statsData)
                    {
                    if (entry.getKey().equals(selectedJCache))
                        {
                        cAverageGet = (Float) entry.getValue().getColumn(JCacheStatisticsData.AVERAGE_GET_TIME) * 1000f;
                        cAveragePut = (Float) entry.getValue().getColumn(JCacheStatisticsData.AVERAGE_PUT_TIME) * 1000f;
                        cAverageRemove = (Float) entry.getValue().getColumn(JCacheStatisticsData.AVERAGE_REMOVE_TIME)
                                         * 1000f;
                        cHitRate = (Float) entry.getValue().getColumn(JCacheStatisticsData.CACHE_HIT_PERCENTAGE);
                        break;    // should only be one entry
                        }
                    }

                GraphHelper.addValuesToJCacheAverageGraph(operationAverageGraph, cAveragePut, cAverageGet,
                    cAverageRemove);
                GraphHelper.addValuesToJCacheHitPercentagGraph(hitRateGraph, (long) cHitRate);
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateData()
        {
        configData = model.getData(VisualVMModel.DataType.JCACHE_CONFIG);
        statsData  = model.getData(VisualVMModel.DataType.JCACHE_STATS);

        if (configData == null && statsData != null)
            {
            // populate the configData with the stats data if none exists
            SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();

            for (Map.Entry<Object, Data> entry : statsData)
                {
                Data data = new JCacheConfigurationData();
                data.setColumn(JCacheConfigurationData.CACHE_MANAGER, entry.getKey());
                mapData.put(entry.getKey(), data);
                }
                configData = new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());
            }

        if (configData != null)
            {
            tmodel.setDataList(configData);
            }
        }

    // ---- inner classes ---------------------------------------------------

    /**
     * Inner class to change the the information displayed on the graphs
     * table when the master changes.
     */
    private class SelectRowListSelectionListener
            implements ListSelectionListener
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a new listener to changes the detail table.
         *
         * @param table  the table that is to be selected
         */
        public SelectRowListSelectionListener(ExportableJTable table)
            {
            this.table = table;
            }

        // ----- ListSelectionListener methods ------------------------------

        /**
         * Change and clear the detailModel on selection of a cache.
         *
         * @param e  the {@link ListSelectionEvent} to respond to
         */
        @SuppressWarnings("unchecked")
        public void valueChanged(ListSelectionEvent e)
            {
            if (e.getValueIsAdjusting())
                {
                return;
                }

            ListSelectionModel selectionModel = (ListSelectionModel) e.getSource();

            if (!selectionModel.isSelectionEmpty())
                {
                nSelectedRow = selectionModel.getMinSelectionIndex();

                // get the service at the selected row, which is the first column
                Pair<String, String> selectedCache = (Pair<String, String>) table.getValueAt(nSelectedRow, 0);

                if (!selectedCache.equals(model.getSelectedJCache()))
                    {
                    String sSelectedCache = selectedCache.toString();

                    model.setSelectedJCache(selectedCache);
                    txtSelectedCache.setText(sSelectedCache);
                    txtSelectedCache.setToolTipText(sSelectedCache);

                    // Update the Graphs
                    addSplitPaneAndGraph();
                    }
                }
            }

        /**
         * Re-select the last selected row.
         */
        public void updateRowSelection()
            {
            table.addRowSelectionInterval(nSelectedRow, nSelectedRow);
            }

        private ExportableJTable table;

        /**
         * The currently selected row.
         */
        private int nSelectedRow;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Selected Cache.
     */
    private JTextField txtSelectedCache;

    /**
     * The {@link JCacheConfigurationTableModel} to display JCache data.
     */
    protected JCacheConfigurationTableModel tmodel;

    /**
     * The JCache caches summary.
     */
    private List<Map.Entry<Object, Data>> summaryData;

    /**
     * The JCache configuration data.
     */
    private List<Map.Entry<Object, Data>> configData;

    /**
     * The JCache statistics data.
     */
    private List<Map.Entry<Object, Data>> statsData;

    /**
     * The row selection listener.
     */
    private SelectRowListSelectionListener listener;

    /**
     * The graph of average put/get/remove times.
     */
    private SimpleXYChartSupport operationAverageGraph = null;

    /**
     * The graph of hit rate percentage.
     */
    private SimpleXYChartSupport hitRateGraph = null;

    /**
     * Bottom panel.
     */
    private JPanel pnlBottom = null;

    /**
     * Bottom split pane.
     */
    JSplitPane pneSplitBottom = null;
    }
