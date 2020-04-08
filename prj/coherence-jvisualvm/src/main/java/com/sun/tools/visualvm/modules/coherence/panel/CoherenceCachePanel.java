/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.panel;

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;
import com.sun.tools.visualvm.modules.coherence.helper.RenderHelper;
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;
import com.sun.tools.visualvm.modules.coherence.panel.util.AbstractMenuOption;
import com.sun.tools.visualvm.modules.coherence.panel.util.ExportableJTable;
import com.sun.tools.visualvm.modules.coherence.panel.util.MenuOption;
import com.sun.tools.visualvm.modules.coherence.tablemodel.CacheDetailTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.CacheStorageManagerTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.CacheTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheDetailData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheFrontDetailData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheStorageManagerData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Pair;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Tuple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanServerConnection;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * An implementation of an {@link AbstractCoherencePanel} to view summarized cache
 * size statistics.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class CoherenceCachePanel
        extends AbstractCoherencePanel
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create the layout for the {@link CoherenceCachePanel}.
     *
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherenceCachePanel(VisualVMModel model)
        {
        super(new BorderLayout(), model);

        // create a split pane for resizing
        JSplitPane pneSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pneSplit.setOpaque(false);

        // Populate the header panel
        JPanel pnlTop    = new JPanel(new BorderLayout());
        JPanel pnlHeader = new JPanel();

        pnlHeader.setLayout(new FlowLayout());
        pnlHeader.setOpaque(false);

        txtTotalCaches = getTextField(5);
        pnlHeader.add(getLocalizedLabel("LBL_total_caches", txtTotalCaches));
        pnlHeader.add(txtTotalCaches);

        txtTotalMemory = getTextField(10);
        pnlHeader.add(getLocalizedLabel("LBL_total_data", txtTotalMemory));
        pnlHeader.add(txtTotalMemory);
        txtTotalMemory.setToolTipText(getLocalizedText("TTIP_cache_size"));

        pnlTop.add(pnlHeader, BorderLayout.PAGE_START);
        pnlTop.setOpaque(false);

        // create any table models required
        tmodel            = new CacheTableModel(VisualVMModel.DataType.CACHE.getMetadata());
        tmodelDetail      = new CacheDetailTableModel(VisualVMModel.DataType.CACHE_DETAIL.getMetadata());
        tmodelFrontDetail = new CacheDetailTableModel(VisualVMModel.DataType.CACHE_FRONT_DETAIL.getMetadata());
        tmodelStorage     = new CacheStorageManagerTableModel(VisualVMModel.DataType.CACHE_STORAGE_MANAGER.getMetadata());

        final ExportableJTable table = new ExportableJTable(tmodel);
        tableDetail                  = new ExportableJTable(tmodelDetail);
        tableFrontDetail             = new ExportableJTable(tmodelFrontDetail);
        tableStorage                 = new ExportableJTable(tmodelStorage);

        table.setPreferredScrollableViewportSize(new Dimension(500, table.getRowHeight() * 5));
        tableDetail.setPreferredScrollableViewportSize(new Dimension(500, tableDetail.getRowHeight() * 3));
        tableFrontDetail.setPreferredScrollableViewportSize(new Dimension(500, tableFrontDetail.getRowHeight() * 3));
        tableStorage.setPreferredScrollableViewportSize(new Dimension(500, tableStorage.getRowHeight() * 3));

        // define renderers for the columns
        RenderHelper.setColumnRenderer(table, CacheData.CACHE_NAME, new RenderHelper.ToolTipRenderer());
        RenderHelper.setColumnRenderer(table, CacheData.SIZE, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, CacheData.AVG_OBJECT_SIZE, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, CacheData.MEMORY_USAGE_MB, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, CacheData.MEMORY_USAGE_BYTES, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, CacheData.UNIT_CALCULATOR, new RenderHelper.UnitCalculatorRenderer());

        RenderHelper.setHeaderAlignment(table, JLabel.CENTER);
        RenderHelper.setHeaderAlignment(tableDetail, JLabel.CENTER);
        RenderHelper.setHeaderAlignment(tableFrontDetail, JLabel.CENTER);
        RenderHelper.setHeaderAlignment(tableStorage, JLabel.CENTER);

        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.CACHE_HITS, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.CACHE_MISSES, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.MEMORY_BYTES, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.TOTAL_GETS, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.TOTAL_PUTS, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.SIZE, new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.HIT_PROBABILITY,
                                       new RenderHelper.CacheHitProbabilityRateRenderer());

        RenderHelper.setColumnRenderer(tableFrontDetail, CacheFrontDetailData.CACHE_HITS, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableFrontDetail, CacheFrontDetailData.CACHE_MISSES, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableFrontDetail, CacheFrontDetailData.TOTAL_GETS, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableFrontDetail, CacheFrontDetailData.TOTAL_PUTS, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableFrontDetail, CacheFrontDetailData.SIZE, new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableFrontDetail, CacheFrontDetailData.HIT_PROBABILITY,
                                       new RenderHelper.CacheHitProbabilityRateRenderer());

        RenderHelper.setColumnRenderer(tableStorage, CacheStorageManagerData.LOCKS_GRANTED,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableStorage, CacheStorageManagerData.LOCKS_PENDING,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableStorage, CacheStorageManagerData.LISTENER_REGISTRATIONS,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableStorage, CacheStorageManagerData.MAX_QUERY_DURATION,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableStorage, CacheStorageManagerData.NON_OPTIMIZED_QUERY_AVG,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableStorage, CacheStorageManagerData.OPTIMIZED_QUERY_AVG,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableStorage, CacheStorageManagerData.MAX_QUERY_DESCRIPTION,
                                       new RenderHelper.ToolTipRenderer());
        RenderHelper.setColumnRenderer(tableStorage, CacheStorageManagerData.INDEX_TOTAL_UNITS,
                                       new RenderHelper.IntegerRenderer());

        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        // experimental heat map
        if ("true".equals(System.getProperty("coherence.jvisualvm.heatmap.enabled")))
            {
            table.setMenuOptions(
                    new MenuOption[]{new ShowHeatMapMenuOption(model, requestSender,
                            table,
                            ShowHeatMapMenuOption.TYPE_SIZE),
                            new ShowHeatMapMenuOption(model, requestSender, table,
                                    ShowHeatMapMenuOption.TYPE_MEMORY)});
            }

        tableDetail.setIntercellSpacing(new Dimension(6, 3));
        tableDetail.setRowHeight(table.getRowHeight() + 4);
        tableDetail.setMenuOptions(new MenuOption[] {new ShowDetailMenuOption(model, tableDetail, SELECTED_CACHE)});

        tableFrontDetail.setIntercellSpacing(new Dimension(6, 3));
        tableFrontDetail.setRowHeight(table.getRowHeight() + 4);
        tableFrontDetail.setMenuOptions(new MenuOption[] {new ShowDetailMenuOption(model, tableFrontDetail, SELECTED_FRONT_CACHE)});

        tableStorage.setIntercellSpacing(new Dimension(6, 3));
        tableStorage.setRowHeight(table.getRowHeight() + 4);
        tableStorage.setMenuOptions(new MenuOption[] {new ShowDetailMenuOption(model, tableStorage, SELECTED_STORAGE)});

        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane        = new JScrollPane(table);
        JScrollPane scrollPaneDetail  = new JScrollPane(tableDetail);
        JScrollPane scrollPaneStorage = new JScrollPane(tableStorage);

        scrollPaneFrontDetail = new JScrollPane(tableFrontDetail);

        configureScrollPane(scrollPane, table);
        configureScrollPane(scrollPaneDetail, tableDetail);
        configureScrollPane(scrollPaneFrontDetail, tableFrontDetail);
        configureScrollPane(scrollPaneStorage, tableStorage);
        scrollPane.setOpaque(false);
        scrollPaneDetail.setOpaque(false);
        scrollPaneStorage.setOpaque(false);
        scrollPaneFrontDetail.setOpaque(false);

        pnlTop.add(scrollPane, BorderLayout.CENTER);
        pneSplit.add(pnlTop);

        JPanel bottomPanel       = new JPanel(new BorderLayout());
        JPanel detailHeaderPanel = new JPanel();
        bottomPanel.setOpaque(false);
        detailHeaderPanel.setOpaque(false);

        txtSelectedCache = getTextField(30, JTextField.LEFT);
        detailHeaderPanel.add(getLocalizedLabel("LBL_selected_service_cache", txtSelectedCache));
        detailHeaderPanel.add(txtSelectedCache);

        txtMaxQueryDuration = getTextField(5);
        detailHeaderPanel.add(getLocalizedLabel("LBL_max_query_millis", txtMaxQueryDuration));
        detailHeaderPanel.add(txtMaxQueryDuration);

        txtMaxQueryDescription = getTextField(30, JTextField.LEFT);
        detailHeaderPanel.add(getLocalizedLabel("LBL_max_query_desc", txtMaxQueryDescription));
        detailHeaderPanel.add(txtMaxQueryDescription);

        bottomPanel.add(detailHeaderPanel, BorderLayout.PAGE_START);
        bottomPanel.setOpaque(false);

        pneTab = new JTabbedPane();
        pneTab.setOpaque(false);

        pneTab.addTab(getLocalizedText("TAB_cache"), scrollPaneDetail);
        pneTab.addTab(getLocalizedText("TAB_storage"), scrollPaneStorage);

        bottomPanel.add(pneTab, BorderLayout.CENTER);

        pneSplit.add(bottomPanel);

        add(pneSplit);

        // add a listener for the selected row
        ListSelectionModel rowSelectionModel = table.getSelectionModel();

        listener = new SelectRowListSelectionListener(table);
        rowSelectionModel.addListSelectionListener(listener);
        }

    // ---- AbstractCoherencePanel methods ----------------------------------

    /**
      * {@inheritDoc}
      */
    @Override
    public void updateData()
        {
        cacheData            = model.getData(VisualVMModel.DataType.CACHE);
        cacheDetailData      = model.getData(VisualVMModel.DataType.CACHE_DETAIL);
        cacheFrontDetailData = model.getData(VisualVMModel.DataType.CACHE_FRONT_DETAIL);
        cacheStorageData     = model.getData(VisualVMModel.DataType.CACHE_STORAGE_MANAGER);

        // zero out memory if the selected cache is FIXED unit calculator
        Tuple selectedCache = model.getSelectedCache();
        if (selectedCache != null)
            {
            boolean isFixed = false;
            String  sCache  = selectedCache.toString();
            // find the cacheData for this cache
            for (Entry<Object, Data> entry : cacheData)
                {
                if (entry.getKey().toString().equals(sCache) && entry.getValue().getColumn(CacheData.UNIT_CALCULATOR).equals("FIXED"))
                    {
                    isFixed = true;
                    break;
                    }
                }

            if (isFixed)
                {
                List<Entry<Object, Data>> tempList = new ArrayList<>();

                // zero out the values for memory and update the list
                for (Entry<Object, Data> entry : cacheDetailData)
                    {
                    entry.getValue().setColumn(CacheDetailData.MEMORY_BYTES, Integer.valueOf(0));
                    tempList.add(entry);
                    }

                cacheDetailData = tempList;
                }
            }

        tmodel.setDataList(cacheData);
        tmodelDetail.setDataList(cacheDetailData);
        tmodelStorage.setDataList(cacheStorageData);

        // check if near cache is configured
        isNearCacheConfigured = cacheFrontDetailData != null && cacheFrontDetailData.size() > 0;

        if (isNearCacheConfigured)
            {
            tmodelFrontDetail.setDataList(cacheFrontDetailData);
            }

        // if we are currently displaying the heat map then update it
        if (m_currentHeatMap != null)
            {
            m_currentHeatMap.updateData();
            m_currentHeatMap.m_pnlHeatMap.repaint();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGUI()
        {
        // If the near cache is configured, we have to make sure the front cache detail tab
        // is added. Otherwise, we have to remove it.
        if (isNearCacheConfigured)
            {
            if (!isFrontCacheTabAdded && pneTab != null)
                {
                pneTab.addTab(getLocalizedText("TAB_front_cache_detail"), scrollPaneFrontDetail);
                isFrontCacheTabAdded = true;
                }
            }
        else
            {
            if (isFrontCacheTabAdded && pneTab != null)
                {
                pneTab.remove(scrollPaneFrontDetail);
                isFrontCacheTabAdded = false;
                }
            }

        if (cacheData != null)
            {
            txtTotalCaches.setText(String.format("%5d", cacheData.size()));

            float cTotalCacheSize = 0.0f;

            for (Entry<Object, Data> entry : cacheData)
                {
                cTotalCacheSize += new Float((Integer) entry.getValue().getColumn(CacheData.MEMORY_USAGE_MB));
                }

            txtTotalMemory.setText(String.format("%,10.2f", cTotalCacheSize));
            }
        else
            {
            txtTotalCaches.setText("0");
            txtTotalMemory.setText(String.format("%,10.2f", 0.0));
            }

        Tuple selectedCache = model.getSelectedCache();

        if (selectedCache == null)
            {
            txtSelectedCache.setText("");
            }
        else
            {
            txtSelectedCache.setText(selectedCache.toString());
            }

        if (cacheStorageData != null)
            {
            long   lMaxQueryMillis      = 0L;
            String sMaxQueryDescription = "";

            for (Entry<Object, Data> entry : cacheStorageData)
                {
                Object oValue  = entry.getValue().getColumn(CacheStorageManagerData.MAX_QUERY_DURATION);
                long lMaxValue = oValue != null ? (Long) oValue : 0L;

                if (lMaxValue > lMaxQueryMillis)
                    {
                    lMaxQueryMillis      = lMaxValue;
                    oValue               = entry.getValue().getColumn(CacheStorageManagerData.MAX_QUERY_DESCRIPTION);
                    sMaxQueryDescription = oValue != null ? (String) oValue : "";
                    }
                }

            txtMaxQueryDescription.setText(sMaxQueryDescription);
            txtMaxQueryDescription.setToolTipText(sMaxQueryDescription);
            txtMaxQueryDuration.setText(String.format("%5d", lMaxQueryMillis));
            }

        tmodel.fireTableDataChanged();

        fireTableDataChangedWithSelection(tableDetail, tmodelDetail);
        fireTableDataChangedWithSelection(tableFrontDetail, tmodelFrontDetail);
        fireTableDataChangedWithSelection(tableStorage, tmodelStorage);

        if (model.getSelectedCache() != null)
            {
            listener.updateRowSelection();
            }
        }

    // ---- inner classes ---------------------------------------------------

    /**
     * Inner class to change the the information displayed on the detailModel
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

                if (!selectedCache.equals(model.getSelectedCache()))
                    {
                    String sSelectedCache = selectedCache.toString();
                    model.setSelectedCache(selectedCache);
                    txtSelectedCache.setText(sSelectedCache);
                    txtSelectedCache.setToolTipText(sSelectedCache);
                    tmodelDetail.setDataList(null);
                    tmodelDetail.fireTableDataChanged();

                    tmodelFrontDetail.setDataList(null);
                    tmodelFrontDetail.fireTableDataChanged();

                    txtMaxQueryDescription.setText("");
                    txtMaxQueryDuration.setText("");
                    cacheData = null;
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

    /**
     * An experimental menu option to display a heat map for the cache sizes or
     * primary storage used. To enable, set the following system property<br>
     *  coherence.jvisualvm.heatmap.enabled=true
     */
    protected class ShowHeatMapMenuOption extends AbstractMenuOption
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a new menu option for displaying heat map.
         *
         * @param model          the {@link VisualVMModel} to get collected data from
         * @param requestSender  the {@link MBeanServerConnection} to perform additional queries
         * @param jtable         the {@link ExportableJTable} that this applies to
         * @param nType          the type of the heat map either SIZE or MEMORY
         */
        public ShowHeatMapMenuOption (VisualVMModel model, RequestSender requestSender,
                                      ExportableJTable jtable, int nType)
            {
            super(model, requestSender, jtable);
            f_nType = nType;

            f_sMenuItem  = getLocalizedText(nType == TYPE_SIZE ? "LBL_size_heat_map"       : "LBL_memory_heat_map");
            f_sTitle     = getLocalizedText(nType == TYPE_SIZE ? "LBL_title_size_heat_map" : "LBL_title_memory_heat_map");
            }

        // ----- AbstractMenuOption methods ---------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMenuItem()
           {
           return f_sMenuItem;
           }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed (ActionEvent e)
            {
            if (cacheData != null && cacheData.size() > 0)
                {
                updateData();

                if (m_cTotal == 0L)
                    {
                    JOptionPane.showMessageDialog(null, getLocalizedText("LBL_no_caches"));
                    }
                else
                    {
                    JPanel pnlHeatMap = new HeatMapJPanel();

                    try
                        {
                        m_currentHeatMap = this;
                        m_pnlHeatMap     = pnlHeatMap;
                        JOptionPane.showMessageDialog(null, pnlHeatMap,
                                f_sTitle,
                                JOptionPane.INFORMATION_MESSAGE);
                        }
                    finally
                        {
                        m_currentHeatMap = null;
                        m_pnlHeatMap     = null;
                        }
                    }
                }
            else
                {
                JOptionPane.showMessageDialog(null,
                        getLocalizedText("LBL_no_caches"));
                }
            }

        /**
         * Update the data for the heat map, can be called by choosing the
         * right-click or by regular refresh if the JPanel is visible.
         */
        public synchronized void updateData()
            {
            // build a linked list with a Pair<X,Y> where X = cache name (which is Pair<String, String>) and
            // Y is the count of size of memory
            f_listValues.clear();
            m_cTotal = 0L;

            for (Entry<Object, Data> entry : cacheData)
                {
                long cValue;
                if (f_nType == TYPE_SIZE)
                    {
                    cValue = (long) ((Integer) entry.getValue().getColumn(CacheData.SIZE));
                    }
                else
                    {
                    cValue = (Long) entry.getValue().getColumn(CacheData.MEMORY_USAGE_BYTES);
                    }

                m_cTotal += cValue;
                Pair<Pair<String, String>, Long> cache = new Pair<>((Pair<String, String>) entry.getValue().getColumn(CacheData.CACHE_NAME), cValue);

                if (f_listValues.size() == 0)
                    {
                    f_listValues.add(cache);
                    }
                else
                    {
                    int nLocation = 0;
                    boolean fAdded = false;

                    // Find where the value is in the list
                    Iterator<Pair<Pair<String, String>, Long>> iter = f_listValues.iterator();
                    while (iter.hasNext())
                        {
                        Pair<Pair<String, String>, Long> entryHeatMap = iter.next();
                        if (entryHeatMap.getY().longValue() <= cache.getY().longValue())
                            {
                            // add new value at the current position
                            f_listValues.add(nLocation, cache);
                            fAdded = true;
                            break;
                            }
                        else
                            {
                            // value must be added at least after this one
                            nLocation++;
                            }
                        }

                    // if we have not added then add to end of list
                    if (!fAdded)
                        {
                        f_listValues.addLast(cache);
                        }
                    }
                }
            }

        // ----- inner classes ----------------------------------------------

        /**
         * Extension of JPanel to display the HeatMap.
         */
        protected class HeatMapJPanel extends JPanel
            {
            /**
             * Construct a new JPanel for the heap map.
             */
            public HeatMapJPanel()
                {
                super();
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int nWidth           = (int) (screenSize.getWidth()  * 0.75);
                int nHeight          = (int) (screenSize.getHeight() * 0.5);
                AbstractMenuOption.setResizable(this);

                setPreferredSize(new Dimension(nWidth, nHeight));

                // add mouse listeners to display tooltips for cache names
                addMouseMotionListener(new MouseMotionListener()
                    {
                    @Override
                    public void mouseDragged (MouseEvent e)
                        {
                        }

                    @Override
                    public void mouseMoved (MouseEvent e)
                       {
                       mapToolTips.forEach( (k,v) ->
                           {
                           if (k.contains(e.getPoint()))
                               {
                               setToolTipText(v);
                               }
                           });
                       ToolTipManager.sharedInstance().mouseMoved(e);
                       }
                    });
                }

            @Override
            public void paintComponent(Graphics g)
                {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setFont(f_font);
                m_nRange = MIN_RANGE;

                int nMapWidth  = getWidth()  - 10;
                int nMapHeight = getHeight() - 10;
                int nStartX = 5;
                int nStartY = 5;
                int nLastStartX;
                int nLastStartY;

                g2.clearRect(nStartX, nStartY, nMapWidth, nMapHeight);
                g2.draw(new Rectangle2D.Double(nStartX, nStartY, nMapWidth,
                        nMapHeight));

                float   nTotalPercentLeft = 100.0f;
                boolean fVerticalSplit    = true;
                mapToolTips.clear();

                // iterate through all values which will start with largest
                Iterator<Pair<Pair<String, String>, Long>> iter = f_listValues.iterator();
                while (iter.hasNext())
                    {
                    Pair<Pair<String, String>, Long> entryHeatMap = iter.next();
                    long                 nValue = entryHeatMap.getY();
                    Pair<String, String> cache  = entryHeatMap.getX();

                    // get the percent of this value of the total
                    float nPercent = nValue * 1.0f / m_cTotal * 100.0f;

                    if (fVerticalSplit && nTotalPercentLeft <= 50 && nTotalPercentLeft - nPercent > 0)
                        {
                        fVerticalSplit = false;
                        }
                    else if (!fVerticalSplit && nTotalPercentLeft <= 20)
                        {
                        fVerticalSplit = true;
                        }

                    Rectangle2D rectangle;
                    nLastStartX = nStartX;
                    nLastStartY = nStartY;

                    // calculate the size of the rectangle we are going to use to
                    // represent this value
                    if (fVerticalSplit)
                        {
                        int nNewWidth = (int) (nMapWidth * 1.0f * nPercent / nTotalPercentLeft);
                        rectangle     = new Rectangle2D.Double(nStartX, nStartY, nNewWidth, nMapHeight);
                        nStartX      += nNewWidth;
                        nMapWidth    -= nNewWidth;
                        }
                    else
                        {
                        // split horizontal
                        int nNewHeight = (int) (nMapHeight * 1.0f * nPercent / nTotalPercentLeft);
                        rectangle      = new Rectangle2D.Double(nStartX, nStartY, nMapWidth, nNewHeight);
                        nStartY       += nNewHeight;
                        nMapHeight    -= nNewHeight;
                        }

                    if (rectangle != null)
                        {
                        g2.setColor(getColour());
                        g2.fill(rectangle);

                        g2.setColor(Color.black);
                        g2.draw(rectangle);

                        String sCache      = cache.toString();
                        String sCacheShort = cache.getY();

                                StringBuffer sb = new StringBuffer(sCache).append(" - ")
                                .append(RenderHelper.INTEGER_FORMAT.format(nValue))
                                .append(f_nType == TYPE_MEMORY ? " bytes" : " objects")
                                .append(" (")
                                .append(RenderHelper.LOAD_AVERAGE_FORMAT.format(nPercent))
                                .append("%)");

                        String sCaption = sb.toString();

                        mapToolTips.put(rectangle, sCaption);

                        // if we have enough room, add the cache name
                        FontMetrics fm = g.getFontMetrics();
                        if (rectangle.getWidth()  > fm.stringWidth(sCaption) + 20 &&
                            rectangle.getHeight() > fm.getHeight() + 20)
                            {
                            g2.drawString(sCaption, nLastStartX + 10, nLastStartY + 20);
                            }
                        else if (rectangle.getWidth()  > fm.stringWidth(sCache) + 20 &&
                                 rectangle.getHeight() > fm.getHeight() + 20)
                            {
                            g2.drawString(sCache, nLastStartX + 10, nLastStartY + 20);
                            }
                        else if (rectangle.getWidth()  > fm.stringWidth(sCacheShort) + 20 &&
                                 rectangle.getHeight() > fm.getHeight() + 20)
                            {
                            g2.drawString(sCacheShort, nLastStartX + 10, nLastStartY + 20);
                            }
                        }

                    nTotalPercentLeft -= nPercent;
                    }
                }

            /**
             * Return a color that is based upon the incrementing range
             * m_nRange.
             *
             * @return a Color
             */
            private Color getColour()
                {
                m_nRange += INC_RANGE;
                if (m_nRange > MAX_RANGE)
                    {
                    m_nRange = MIN_RANGE;
                    }

                return Color.getHSBColor(m_nRange , 0.8f, 0.8f);
                }

            // ----- constants ----------------------------------------------

            /**
             * Beginning value for heat map Colour.
             */
            private static final float MIN_RANGE = 0.400f;

            /**
             * Max value for heat map Colour.
             */
            private static final float MAX_RANGE = 0.8f;

            /**
             * Incremental value for heat map Colour.
             */
            private static final float INC_RANGE = 0.0333f;

            // ----- data members -------------------------------------------

            /**
             * Tool tips for the caches.
             */
            private final Map<Rectangle2D, String> mapToolTips = new HashMap<>();

            /**
             * The Font to use to
             */
            private final Font f_font = new Font("Arial", Font.PLAIN, 12);

            /**
             * The current range for the heat map Colour.
             */
            private float m_nRange = MIN_RANGE;
            }

        // ----- constants --------------------------------------------------

        /**
         * Indicates a size based heat map.
         */
        protected static final int TYPE_SIZE = 0;

        /**
         * Indicates a memory based heat map.
         */
        protected static final int TYPE_MEMORY = 1;

        // ----- data members -------------------------------------------------

        /**
         * The list of caches and values.
         */
        private final LinkedList<Pair<Pair<String, String>, Long>> f_listValues = new LinkedList<>();

        /**
         * Current total for all caches.
         */
        private long m_cTotal = 0L;

        /**
         * The type of the heat map to display.
         */
        private final int f_nType;

        /**
         * Menu option description.
         */
        private final String f_sMenuItem;

        /**
         * Title for heat map.
         */
        private final String f_sTitle;

        /**
         * Current HeatMapJPanel.
         */
        protected JPanel m_pnlHeatMap;
        }

    // ---- constants -------------------------------------------------------

    private static final long serialVersionUID = -7612569043492412496L;

    // ----- data members ---------------------------------------------------

    /**
     * Flag to indicate if near cache is configured.
     */
    private boolean isNearCacheConfigured = false;

    /**
     * Flag to indicate if we already added the front
     * cache detail tab.
     */
    private boolean isFrontCacheTabAdded = false;

    /**
     * The tabbed panel.
     */
    private JTabbedPane pneTab;

    /**
     * The scroll panel which we use to display front
     * cache details tab.
     */
    private JScrollPane scrollPaneFrontDetail;

    /**
     * Total number of caches.
     */
    private JTextField txtTotalCaches;

    /**
     * Total primary copy memory used by caches.
     */
    private JTextField txtTotalMemory;

    /**
     * Selected Cache.
     */
    private JTextField txtSelectedCache;

    /**
     * Max query duration across nodes.
     */
    private JTextField txtMaxQueryDuration;

    /**
     * Max query description across nodes.
     */
    private JTextField txtMaxQueryDescription;

    /**
     * The {@link CacheTableModel} to display cache data.
     */
    protected CacheTableModel tmodel;

    /**
     * The {@link CacheDetailTableModel} to display detail cache data.
     */
    protected CacheDetailTableModel tmodelDetail;

    /**
     * The {@link CacheDetailTableModel} to display detail front cache data.
     */
    protected CacheDetailTableModel tmodelFrontDetail;

    /**
     * The {@link CacheStorageManagerTableModel} to display cache storage data.
     */
    protected CacheStorageManagerTableModel tmodelStorage;

    /**
     * The cache data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> cacheData = null;

    /**
     * The detailed cache data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> cacheDetailData = null;

    /**
     * The detailed front cache data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> cacheFrontDetailData = null;

    /**
     * The storage cache data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> cacheStorageData = null;

    /**
     * The row selection listener.
     */
    private SelectRowListSelectionListener listener;

    /**
     * The {@link ExportableJTable} to use to display cache detail data.
     */
    final ExportableJTable tableDetail;

    /**
     * The {@link ExportableJTable} to use to display front cache data.
     */
    private ExportableJTable tableFrontDetail;

    /**
     * The {@link ExportableJTable} to use to display storage data.
     */
    final ExportableJTable tableStorage;

    /**
     * Currently displaying heat map.
     */
    private ShowHeatMapMenuOption m_currentHeatMap;
    }
