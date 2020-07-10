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
import com.sun.tools.visualvm.modules.coherence.panel.util.ExportableJTable;
import com.sun.tools.visualvm.modules.coherence.tablemodel.HttpProxyMemberTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.HttpProxyTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.ServiceMemberTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.HttpProxyData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.HttpProxyMemberData;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import org.graalvm.visualvm.charts.SimpleXYChartSupport;

/**
 * An implementation of an {@link AbstractCoherencePanel} to
 * view summarized http proxy server statistics.
 *
 * @author tam  2015.08.28
 * @since 12.2.1.1
 */
public class CoherenceHttpProxyPanel
        extends AbstractCoherencePanel
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create the layout for the {@link CoherenceServicePanel}.
     *
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherenceHttpProxyPanel(VisualVMModel model)
        {
        super(new BorderLayout(), model);

        // create a split pane for resizing
        JSplitPane pneSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pneSplit.setOpaque(false);

        tmodel       = new HttpProxyTableModel(VisualVMModel.DataType.HTTP_PROXY.getMetadata());
        tmodelDetail = new HttpProxyMemberTableModel(VisualVMModel.DataType.HTTP_PROXY_DETAIL.getMetadata());

        table       = new ExportableJTable(tmodel);
        tableDetail = new ExportableJTable(tmodelDetail);

        // set renderers
        RenderHelper.setIntegerRenderer(table, HttpProxyData.MEMBER_COUNT);
        RenderHelper.setIntegerRenderer(table, HttpProxyData.TOTAL_ERROR_COUNT);
        RenderHelper.setIntegerRenderer(table, HttpProxyData.TOTAL_REQUEST_COUNT);

        RenderHelper.setMillisRenderer(table, HttpProxyData.AVERAGE_REQ_TIME);
        RenderHelper.setMillisRenderer(table, HttpProxyData.AVERAGE_REQ_PER_SECOND);

        RenderHelper.setIntegerRenderer(tableDetail, HttpProxyMemberData.NODE_ID);
        RenderHelper.setIntegerRenderer(tableDetail, HttpProxyMemberData.TOTAL_ERROR_COUNT);
        RenderHelper.setIntegerRenderer(tableDetail, HttpProxyMemberData.TOTAL_REQUEST_COUNT);

        RenderHelper.setMillisRenderer(tableDetail, HttpProxyMemberData.AVG_REQ_TIME);
        RenderHelper.setMillisRenderer(tableDetail, HttpProxyMemberData.REQ_PER_SECOND);

        RenderHelper.setHeaderAlignment(table, JLabel.CENTER);
        RenderHelper.setHeaderAlignment(tableDetail, JLabel.CENTER);
        table.setPreferredScrollableViewportSize(new Dimension(500, table.getRowHeight() * 5));
        tableDetail.setPreferredScrollableViewportSize(new Dimension(500, 125));

        // Add some space
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        tableDetail.setIntercellSpacing(new Dimension(6, 3));
        tableDetail.setRowHeight(table.getRowHeight() + 4);

        // Create the scroll pane and add the table to it.
        JScrollPane pneScroll       = new JScrollPane(table);
        JScrollPane pneScrollDetail = new JScrollPane(tableDetail);
        configureScrollPane(pneScroll, table);
        configureScrollPane(pneScrollDetail, tableDetail);

        pneSplit.add(pneScroll);

        // create the detail pane
        JPanel pneDetail = new JPanel();
        pneDetail.setOpaque(false);

        pneDetail.setLayout(new BorderLayout());

        JPanel detailHeaderPanel = new JPanel();
        detailHeaderPanel.setOpaque(false);

        txtSelectedService = getTextField(22, JTextField.LEFT);
        detailHeaderPanel.add(getLocalizedLabel("LBL_selected_service", txtSelectedService));
        detailHeaderPanel.add(txtSelectedService);

        final JSplitPane pneSplitDetail = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        pneSplitDetail.setResizeWeight(0.5);

        pneDetail.add(detailHeaderPanel, BorderLayout.PAGE_START);
        pneSplitDetail.add(pneScrollDetail);

        final JTabbedPane pneDetailTabs = new JTabbedPane();
        populateTabs(pneDetailTabs, getLocalizedText("LBL_none_selected"));

        pneSplitDetail.add(pneDetailTabs);

        pneDetail.add(pneSplitDetail, BorderLayout.CENTER);

        pneSplit.add(pneDetail);
        add(pneSplit);

        // add a listener for the selected row
        ListSelectionModel rowSelectionModel = table.getSelectionModel();

        listener = new SelectRowListSelectionListener(table, pneDetailTabs);
        rowSelectionModel.addListSelectionListener(listener);
        }

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
         * @param table         the {@link ExportableJTable} that is to be selected
         * @param pneDetailTabs the {@link JTabbedPane} to attach to
         */
        public SelectRowListSelectionListener(ExportableJTable table, JTabbedPane pneDetailTabs)
            {
            this.table = table;
            this.pneDetailTabs = pneDetailTabs;
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

            if (!selectionModel.isSelectionEmpty())
                {
                nSelectedRow = selectionModel.getMinSelectionIndex();

                // get the service at the selected row, which is the first column
                String sSelectedService = (String) table.getValueAt(nSelectedRow, 0);

                if (!sSelectedService.equals(model.getSelectedHttpProxyService()))
                    {
                    model.setSelectedHttpProxyService(sSelectedService);
                    model.eraseServiceMemberData();
                    tmodelDetail.setDataList(null);
                    tmodelDetail.fireTableDataChanged();

                    txtSelectedService.setText(sSelectedService);
                    txtSelectedService.setToolTipText(sSelectedService);

                    populateTabs(pneDetailTabs, sSelectedService);

                    m_cLastErrorCount       = -1L;
                    m_cLastRequestCount     = -1L;
                    m_cLastUpdateTime       = -1L;
                    m_cLastResponse1xxCount = -1L;
                    m_cLastResponse2xxCount = -1L;
                    m_cLastResponse3xxCount = -1L;
                    m_cLastResponse4xxCount = -1L;
                    m_cLastResponse5xxCount = -1L;
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

        /**
         * ExportableJTable that this listener applies to.
         */
        private ExportableJTable table;

        /**
         * The JTabbedPane that this listener applies to.
         */
        private JTabbedPane pneDetailTabs;

        /**
         * The currently selected row.
         */
        private int nSelectedRow;
        }

    /**
     * Populate the tabs on a change of service name.
     *
     * @param pneDetailTabs the {@link JTabbedPane} to update
     * @param sServiceName  the service name to display
     */
    private void populateTabs(JTabbedPane pneDetailTabs, String sServiceName)
        {
        // don't recreate tabs if we are updating GUI
        synchronized (this)
            {
            // remove any existing tabs
            int cTabs = pneDetailTabs.getTabCount();

            for (int i = 0; i < cTabs; i++)
                {
                pneDetailTabs.removeTabAt(0);
                }

            requestTimeGraph = GraphHelper.createAverageRequestTimeGraph(sServiceName);
            pneDetailTabs.addTab(getLocalizedText("LBL_average_request_time"),
                    requestTimeGraph.getChart());

            requestsPerSecondGraph = GraphHelper.createAverageRequestsPerSecondGraph(sServiceName);
            pneDetailTabs.addTab(getLocalizedText("LBL_average_request_per_second"),
                    requestsPerSecondGraph.getChart());

            requestsGraph = GraphHelper.createHttpRequestGraph(sServiceName);
            pneDetailTabs.addTab(getLocalizedText("LBL_request_history"),
                    requestsGraph.getChart());

            responseGraph = GraphHelper.createHttpResponseGraph(sServiceName);
            pneDetailTabs.addTab(getLocalizedText("LBL_response_history"),
                    responseGraph.getChart());
            }
        }

    @Override
    public void updateGUI()
        {
        tmodel.fireTableDataChanged();
        fireTableDataChangedWithSelection(tableDetail, tmodelDetail);

        if (model.getSelectedHttpProxyService() != null)
            {
            listener.updateRowSelection();
            }
        }

    @Override
    public void updateData()
        {
        httpProxyData = model.getData(VisualVMModel.DataType.HTTP_PROXY);

        if (httpProxyData != null)
            {
            tmodel.setDataList(httpProxyData);
            }

        httpProxyMemberData = model.getData(
                VisualVMModel.DataType.HTTP_PROXY_DETAIL);

        synchronized (this)
            {
            if (httpProxyMemberData != null)
                {
                tmodelDetail.setDataList(httpProxyMemberData);

                float nTotalRequestTime  = 0.0f;
                float nMaxRequestTime    = 0.0f;
                int   nCount             = 0;
                float nTotalReqPerSecond = 0.0f;
                float nMaxReqPerSecond   = 0.0f;
                long  nErrorCount        = 0L;
                long  nRequestCount      = 0L;
                long  nResponse1xxCount  = 0L;
                long  nResponse2xxCount  = 0L;
                long  nResponse3xxCount  = 0L;
                long  nResponse4xxCount  = 0L;
                long  nResponse5xxCount  = 0L;

                for (Map.Entry<Object, Data> entry : httpProxyMemberData)
                    {
                    float cMillisAverage = (Float) entry.getValue().getColumn(
                            HttpProxyMemberData.AVG_REQ_TIME);
                    nTotalRequestTime += cMillisAverage;
                    nCount++;
                    nErrorCount += (Long) entry.getValue().getColumn(HttpProxyMemberData.TOTAL_ERROR_COUNT);
                    nRequestCount += (Long) entry.getValue().getColumn(HttpProxyMemberData.TOTAL_REQUEST_COUNT);
                    nResponse1xxCount += (Long) entry.getValue().getColumn(HttpProxyMemberData.RESPONSE_COUNT_1);
                    nResponse2xxCount += (Long) entry.getValue().getColumn(HttpProxyMemberData.RESPONSE_COUNT_2);
                    nResponse3xxCount += (Long) entry.getValue().getColumn(HttpProxyMemberData.RESPONSE_COUNT_3);
                    nResponse4xxCount += (Long) entry.getValue().getColumn(HttpProxyMemberData.RESPONSE_COUNT_4);
                    nResponse5xxCount += (Long) entry.getValue().getColumn(HttpProxyMemberData.RESPONSE_COUNT_5);

                    if (cMillisAverage > nMaxRequestTime)
                        {
                        nMaxRequestTime = cMillisAverage;
                        }

                    float nRequestsPerSecond = (Float) entry.getValue().getColumn(HttpProxyMemberData.REQ_PER_SECOND);
                    nTotalReqPerSecond += nRequestsPerSecond;

                    if (nRequestsPerSecond > nMaxReqPerSecond)
                        {
                        nMaxReqPerSecond = nRequestsPerSecond;
                        }
                    }

                if (requestTimeGraph != null)
                    {
                    GraphHelper.addValuesToAverageRequestTimeGraph(
                            requestTimeGraph,
                            nMaxRequestTime,
                            (nCount == 0 ? 0 : nTotalRequestTime / nCount));
                    }

                if (requestsPerSecondGraph != null)
                    {
                    GraphHelper.addValuesToAverageRequestsPerSecondGraph(
                            requestsPerSecondGraph,
                            nMaxReqPerSecond,
                            (nCount == 0 ? 0 : nTotalReqPerSecond / nCount));
                    }

                if (requestsGraph != null)
                    {
                    // only update the graph if the value from the model
                    // has been changed
                    long ldtLastUpdate = model.getLastUpdate();
                    if (ldtLastUpdate > m_cLastUpdateTime)
                        {
                        if (m_cLastRequestCount == -1L)
                            {
                            m_cLastRequestCount = nRequestCount;
                            m_cLastErrorCount = nErrorCount;
                            }

                        if (m_cLastResponse1xxCount == -1L)
                            {
                            m_cLastResponse1xxCount = nResponse1xxCount;
                            m_cLastResponse2xxCount = nResponse2xxCount;
                            m_cLastResponse3xxCount = nResponse3xxCount;
                            m_cLastResponse4xxCount = nResponse4xxCount;
                            m_cLastResponse5xxCount = nResponse5xxCount;
                            }

                        // get delta values
                        long nDeltaErrorCount       = nErrorCount - m_cLastErrorCount;
                        long nDeltaRequestCount     = nRequestCount - m_cLastRequestCount;
                        long nDeltaResponse1xxCount = nResponse1xxCount - m_cLastResponse1xxCount;
                        long nDeltaResponse2xxCount = nResponse2xxCount - m_cLastResponse2xxCount;
                        long nDeltaResponse3xxCount = nResponse3xxCount - m_cLastResponse3xxCount;
                        long nDeltaResponse4xxCount = nResponse4xxCount - m_cLastResponse4xxCount;
                        long nDeltaResponse5xxCount = nResponse5xxCount - m_cLastResponse5xxCount;

                        GraphHelper.addValuesToHttpRequestGraph(requestsGraph,
                                nDeltaRequestCount < 0 ? 0 : nDeltaRequestCount,
                                nDeltaErrorCount < 0 ? 0 : nDeltaErrorCount);

                        GraphHelper.addValuesToHttpResponseGraph(responseGraph,
                                nDeltaResponse1xxCount <
                                0 ? 0 : nDeltaResponse1xxCount,
                                nDeltaResponse2xxCount <
                                0 ? 0 : nDeltaResponse2xxCount,
                                nDeltaResponse3xxCount <
                                0 ? 0 : nDeltaResponse3xxCount,
                                nDeltaResponse4xxCount <
                                0 ? 0 : nDeltaResponse4xxCount,
                                nDeltaResponse5xxCount <
                                0 ? 0 : nDeltaResponse5xxCount);

                        // set the last values to calculate deltas
                        m_cLastErrorCount = nErrorCount;
                        m_cLastRequestCount = nRequestCount;
                        m_cLastUpdateTime = ldtLastUpdate;
                        m_cLastResponse1xxCount = nResponse1xxCount;
                        m_cLastResponse2xxCount = nResponse2xxCount;
                        m_cLastResponse3xxCount = nResponse3xxCount;
                        m_cLastResponse4xxCount = nResponse4xxCount;
                        m_cLastResponse5xxCount = nResponse5xxCount;
                        }
                    }
                }
            }

        String sSelectedService = model.getSelectedHttpProxyService();

        if (sSelectedService == null)
            {
            txtSelectedService.setText("");
            txtSelectedService.setToolTipText("");
            }
        else
            {
            txtSelectedService.setText(sSelectedService);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * the {@link ExportableJTable} to use to display data.
     */
    protected ExportableJTable table;

    /**
     * The {@link HttpProxyTableModel} to display http proxy data.
     */
    protected HttpProxyTableModel tmodel;

    /**
     * the {@link ExportableJTable} to use to display detail data.
     */
    final ExportableJTable tableDetail;

    /**
     * The {@link ServiceMemberTableModel} to display service member data.
     */
    protected HttpProxyMemberTableModel tmodelDetail;

    /**
     * The row selection listener.
     */
    private SelectRowListSelectionListener listener;

    /**
     * The http proxy statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Map.Entry<Object, Data>> httpProxyData;

    /**
     * The proxy member statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Map.Entry<Object, Data>> httpProxyMemberData;

    /**
     * The currently selected service from the service table.
     */
    private JTextField txtSelectedService;

    /**
     * The graph of request time.
     */
    private SimpleXYChartSupport requestTimeGraph;

    /**
     * The graph of average requests per second time.
     */
    private SimpleXYChartSupport requestsPerSecondGraph;

    /**
     * The graph of requests and errors
     */
    private SimpleXYChartSupport requestsGraph;

    /**
     * The graphs of response codes
     */
    private SimpleXYChartSupport responseGraph;

    /**
     * Last error count.
     */
    private long m_cLastErrorCount = -1L;

    /**
     * Last request count.
     */
    private long m_cLastRequestCount = -1L;

    /**
     * Last update time for stats.
     */
    private long m_cLastUpdateTime = -1L;

    /**
     * Last status 100-199 count.
     */
    private long m_cLastResponse1xxCount = -1L;

    /**
     * Last status 200-299 count.
     */
    private long m_cLastResponse2xxCount = -1L;

    /**
     * Last status 300-399 count.
     */
    private long m_cLastResponse3xxCount = -1L;

    /**
     * Last status 400-499 count.
     */
    private long m_cLastResponse4xxCount = -1L;

    /**
     * Last status 500-599 count.
     */
    private long m_cLastResponse5xxCount = -1L;
    }
