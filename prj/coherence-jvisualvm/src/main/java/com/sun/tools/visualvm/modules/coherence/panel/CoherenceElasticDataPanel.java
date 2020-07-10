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
import com.sun.tools.visualvm.modules.coherence.panel.util.AbstractMenuOption;
import com.sun.tools.visualvm.modules.coherence.panel.util.ExportableJTable;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FlashJournalData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.RamJournalData;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.graalvm.visualvm.charts.SimpleXYChartSupport;

/**
 * An implementation of an {@link AbstractCoherencePanel} to
 * view summarized elastic data statistics.
 *
 * @author tam  2014.04.21
 * @since  12.1.3
 */
public class CoherenceElasticDataPanel
        extends AbstractCoherencePanel
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Create the layout for the {@link CoherenceElasticDataPanel}.
     *
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherenceElasticDataPanel(VisualVMModel model)
        {
        super(new BorderLayout(), model);

        JPanel pnlHeader = new JPanel(new GridLayout(1, 2));
        pnlHeader.setOpaque(false);

        barRamUsage   = createProgressBar();
        barFlashUsage = createProgressBar();

        JPanel pnlRamHeader   = new JPanel(new FlowLayout());
        pnlRamHeader.setOpaque(false);

        JPanel pnlFlashHeader = new JPanel(new FlowLayout());
        pnlFlashHeader.setOpaque(false);

        lblRam   = new JLabel("");
        lblFlash = new JLabel("");
        lblRam.setToolTipText(getLocalizedText("LBL_journal_files_used"));
        lblFlash.setToolTipText(getLocalizedText("LBL_journal_files_used"));

        pnlRamHeader.add(getLocalizedLabel("LBL_ram_journal_files"));
        pnlRamHeader.add(barRamUsage);
        pnlRamHeader.add(lblRam);
        pnlFlashHeader.add(getLocalizedLabel("LBL_flash_journal_files"));
        pnlFlashHeader.add(barFlashUsage);
        pnlFlashHeader.add(lblFlash);

        MouseOverAction mouseOverAction = new MouseOverAction(this);

        barFlashUsage.addMouseListener(mouseOverAction);
        barRamUsage.addMouseListener(mouseOverAction);

        pnlHeader.add(pnlRamHeader);
        pnlHeader.add(pnlFlashHeader);

        JPanel pnlData = new JPanel();

        pnlData.setLayout(new GridLayout(2, 2));

        // create a chart for ram journal memory
        JPanel pnlPlotter = new JPanel(new GridLayout(1, 1));

        ramJournalMemoryGraph = GraphHelper.createRamJournalMemoryGraph();

        // ramJournalMemoryGraph.getChart().setPreferredSize(new Dimension(500, 300));
        pnlData.add(ramJournalMemoryGraph.getChart());

        // create a chart for flash journal memory
        flashJournalMemoryGraph = GraphHelper.createFlashJournalMemoryGraph();
        pnlData.add(flashJournalMemoryGraph.getChart());

        // create a chart for ram journal compactions
        ramJournalCompactionGraph = GraphHelper.createRamJournalCompactionGraph();
        pnlData.add(ramJournalCompactionGraph.getChart());

        // create a chart for flash journal compactions
        flashJournalCompactionGraph = GraphHelper.createFlashJournalCompactionGraph();
        pnlData.add(flashJournalCompactionGraph.getChart());

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
        int  cCurrentRamFileCount   = 0;
        int  cMaxRamFileCount       = 0;
        int  cCurrentFlashFileCount = 0;
        int  cMaxFlashFileCount     = 0;
        long cTotalRamUsed          = 0L;
        long cTotalFlashUsed        = 0L;
        long cCommittedRam          = 0L;
        long cCommittedFlash        = 0L;
        int  cRamCompaction         = 0;
        int  cFlashCompaction       = 0;
        int  cRamExhaustive         = 0;
        int  cFlashExhaustive       = 0;

        if (ramJournalData != null)
            {
            for (Map.Entry<Object, Data> entry : ramJournalData)
                {
                cCurrentRamFileCount += (Integer) entry.getValue().getColumn(RamJournalData.FILE_COUNT);
                cMaxRamFileCount     += (Integer) entry.getValue().getColumn(RamJournalData.MAX_FILES);
                cTotalRamUsed        += (Long) entry.getValue().getColumn(RamJournalData.TOTAL_DATA_SIZE);
                cCommittedRam        += (Long) entry.getValue().getColumn(RamJournalData.TOTAL_COMMITTED_BYTES);
                cRamCompaction       += getNullEntry(entry.getValue().getColumn(RamJournalData.COMPACTION_COUNT));
                cRamExhaustive += getNullEntry(entry.getValue().getColumn(RamJournalData.EXHAUSTIVE_COMPACTION_COUNT));
                }
            }

        if (flashJournalData != null)
            {
            for (Map.Entry<Object, Data> entry : flashJournalData)
                {
                cCurrentFlashFileCount += (Integer) entry.getValue().getColumn(FlashJournalData.FILE_COUNT);
                cMaxFlashFileCount     += (Integer) entry.getValue().getColumn(FlashJournalData.MAX_FILES);
                cTotalFlashUsed        += (Long) entry.getValue().getColumn(FlashJournalData.TOTAL_DATA_SIZE);
                cCommittedFlash        += (Long) entry.getValue().getColumn(FlashJournalData.TOTAL_COMMITTED_BYTES);
                cFlashCompaction       += getNullEntry(entry.getValue().getColumn(FlashJournalData.COMPACTION_COUNT));
                cFlashExhaustive +=
                    getNullEntry(entry.getValue().getColumn(FlashJournalData.EXHAUSTIVE_COMPACTION_COUNT));
                }
            }

        // if last value never set then set to current value
        if (m_cLastRamCompaction == -1)
            {
            m_cLastRamCompaction   = cRamCompaction;
            m_cLastRamExhaustive   = cRamExhaustive;
            m_cLastFlashCompaction = cFlashCompaction;
            m_cLastFlashExhaustive = cFlashExhaustive;
            }

        // initialize every time otherwise changes in membership will cause odd %
        barFlashUsage.setMaximum(cMaxFlashFileCount);
        barRamUsage.setMaximum(cMaxRamFileCount);

        barRamUsage.setValue(cCurrentRamFileCount);
        lblRam.setText(Integer.toString(cCurrentRamFileCount) + " / " + Integer.toString(cMaxRamFileCount));

        barFlashUsage.setValue(cCurrentFlashFileCount);
        lblFlash.setText(Integer.toString(cCurrentFlashFileCount) + " / " + Integer.toString(cMaxFlashFileCount));

        GraphHelper.addValuesToRamJournalMemoryGraph(ramJournalMemoryGraph, cCommittedRam, cTotalRamUsed);
        GraphHelper.addValuesToFlashJournalMemoryGraph(flashJournalMemoryGraph, cCommittedFlash, cTotalFlashUsed);

        // add delta values
        int cRamCompactionDelta   = cRamCompaction - m_cLastRamCompaction;
        int cRamExhaustiveDelta   = cRamExhaustive - m_cLastRamExhaustive;
        int cFlashCompactionDelta = cFlashCompaction - m_cLastFlashCompaction;
        int cFlashExhaustiveDelta = cFlashExhaustive - m_cLastFlashExhaustive;

        GraphHelper.addValuesToRamJournalCompactionGraph(ramJournalCompactionGraph,
            cRamCompactionDelta < 0 ? 0 : cRamCompactionDelta, cRamExhaustiveDelta < 0 ? 0 : cRamExhaustiveDelta);
        GraphHelper.addValuesToFlashJournalCompactionGraph(flashJournalCompactionGraph,
            cFlashCompactionDelta < 0 ? 0 : cFlashCompactionDelta,
            cFlashExhaustiveDelta < 0 ? 0 : cFlashExhaustiveDelta);

        // set the last values to calculate deltas
        m_cLastFlashCompaction = cFlashCompaction;
        m_cLastFlashExhaustive = cFlashExhaustive;
        m_cLastRamCompaction   = cRamCompaction;
        m_cLastRamExhaustive   = cRamExhaustive;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateData()
        {
        ramJournalData   = model.getData(VisualVMModel.DataType.RAMJOURNAL);
        flashJournalData = model.getData(VisualVMModel.DataType.FLASHJOURNAL);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Create a {@link JProgressBar} to display file count usage.
     *
     * @return a {@link JProgressBar} to display file count usage
     */
    private JProgressBar createProgressBar()
        {
        JProgressBar barProgress = new JProgressBar();

        barProgress.setMinimum(0);
        barProgress.setMaximum(1);
        barProgress.setStringPainted(true);

        Dimension dim = new Dimension(200, 20);

        barProgress.setPreferredSize(dim);
        barProgress.setSize(dim);
        barProgress.setToolTipText(Localization.getLocalText("LBL_click_for_detail"));

        return barProgress;
        }

    /**
     * Returns an integer value of zero if the object is null, otherwise the value
     * as an int is returned. This is used because sometimes null values are returned
     * as columns when the version of Coherence doesn't support it.
     *
     * @param oValue the value check may be null or valid Integer
     *
     * @return a value of zero if null otherwise the value as an int
     */
    private int getNullEntry(Object oValue)
        {
        return (oValue == null ? Integer.valueOf(0) : (Integer) oValue);
        }

    // ----- inner classes --------------------------------------------------

    /**
     * A class to provide mouse-click functionality to display details
     * for either flash journal or ram journal data.
     */
    private class MouseOverAction
            implements MouseListener
        {
        /**
         * Create the {@link MouseListener} implementation to show details
         * when a progress bar is hovered over.
         *
         * @param panel the panel to use
         */
        public MouseOverAction(CoherenceElasticDataPanel panel)
            {
            pnlElasticData = panel;
            tmodel         = new DefaultTableModel(new Object[]
                {
                Localization.getLocalText("LBL_node_id"), Localization.getLocalText("LBL_journal_files"),
                Localization.getLocalText("LBL_total_data_size"), Localization.getLocalText("LBL_committed"),
                Localization.getLocalText("LBL_compactions"),
                Localization.getLocalText("LBL_current_collector_load_factor"),
                Localization.getLocalText("LBL_max_file_size")
                }, COLUMN_COUNT)
                    {
                    @Override
                    public boolean isCellEditable(int row, int column)
                        {
                        return false;
                        }
                    };
            table = new ExportableJTable(tmodel);

            RenderHelper.setIntegerRenderer(table, 0);                                      // node id
            setColumnRenderer(table, 1, Localization.getLocalText("TTIP_used_maximum"));    // Journal files
            RenderHelper.setColumnRenderer(table, 2, new RenderHelper.BytesRenderer());     // total data size
            setColumnRenderer(table, 3, Localization.getLocalText("TTIP_used_maximum"));    // Committed
            setColumnRenderer(table, 4, Localization.getLocalText("TTIP_compactions"));     // Compactions
            setColumnRenderer(table, 5, null);                                              // load factor
            RenderHelper.setColumnRenderer(table, 6, new RenderHelper.BytesRenderer());     // max file size

            RenderHelper.setHeaderAlignment(table, JLabel.CENTER);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            table.setPreferredScrollableViewportSize(new Dimension((int) (Math.max((int) (screenSize.getWidth() * 0.5),
                800)), table.getRowHeight() * 10));

            table.setIntercellSpacing(new Dimension(6, 3));
            table.setRowHeight(table.getRowHeight() + 4);

            pneMessage = new JScrollPane(table);
            configureScrollPane(pneMessage, table);
            AbstractMenuOption.setResizable(pneMessage);
            }

        /**
         * Set a column renderer for right aligned and optionally set tool tip text.
         *
         * @param exportableJTable the {@link ExportableJTable} to apply to
         * @param nColumn          column number to right align
         * @param sToolTip         tool tip - null if nothing
         */
        private void setColumnRenderer(ExportableJTable exportableJTable, int nColumn, String sToolTip)
            {
            DefaultTableCellRenderer rndRightAlign = new DefaultTableCellRenderer();

            if (sToolTip != null)
                {
                rndRightAlign.setToolTipText(sToolTip);
                }

            rndRightAlign.setHorizontalAlignment(JLabel.RIGHT);
            exportableJTable.getColumnModel().getColumn(nColumn).setCellRenderer(rndRightAlign);
            }

        // ----- MouseListener methods --------------------------------------

        @Override
        public void mouseEntered(MouseEvent e)
            {
            }

        @Override
        public void mousePressed(MouseEvent e)
            {
            }

        @Override
        public void mouseReleased(MouseEvent e)
            {
            }

        /**
         * Display a dialog box with a table showing detailed information
         * node by node for either ram or flash journal.
         *
         * @param e the {@link MouseEvent} that caused this action
         */
        @Override
        public void mouseClicked(MouseEvent e)
            {
            boolean fisRamBar = e.getSource().equals(pnlElasticData.barRamUsage);
            int     row       = 0;

            // remove any existing rows
            tmodel.getDataVector().removeAllElements();
            tmodel.fireTableDataChanged();

            java.util.List<Map.Entry<Object, Data>> tableData = pnlElasticData.model.getData(fisRamBar
                                                                    ? VisualVMModel.DataType.RAMJOURNAL
                                                                    : VisualVMModel.DataType.FLASHJOURNAL);

            // loop through the model and format nicely
            for (Map.Entry<Object, Data> entry : tableData)
                {
                Data data = entry.getValue();
                String sJournalFiles = data.getColumn(RamJournalData.FILE_COUNT).toString() + " / "
                                       + data.getColumn(RamJournalData.MAX_FILES).toString();
                String sCommitted =
                    RenderHelper.getRenderedBytes((Long) data.getColumn(RamJournalData.TOTAL_COMMITTED_BYTES)) + " / "
                    + RenderHelper.getRenderedBytes((Long) data.getColumn(RamJournalData.MAX_COMMITTED_BYTES));
                String sCompactions = getNullEntry(data.getColumn(RamJournalData.COMPACTION_COUNT)) + " / " +
                                      getNullEntry(data.getColumn(RamJournalData.EXHAUSTIVE_COMPACTION_COUNT));

                tmodel.insertRow(row++, new Object[]
                    {
                    data.getColumn(RamJournalData.NODE_ID), sJournalFiles,
                    data.getColumn(RamJournalData.TOTAL_DATA_SIZE), sCommitted, sCompactions,
                    data.getColumn(RamJournalData.CURRENT_COLLECTION_LOAD_FACTOR),
                    data.getColumn(RamJournalData.MAX_FILE_SIZE)
                    });
                }

            tmodel.fireTableDataChanged();

            JOptionPane.showMessageDialog(null, pneMessage,
                                          Localization.getLocalText(fisRamBar
                                              ? "LBL_ram_journal_detail"
                                              : "LBL_flash_journal_detail"), JOptionPane.INFORMATION_MESSAGE);
            }

        @Override
        public void mouseExited(MouseEvent e)
            {
            }

        // ----- static -----------------------------------------------------

        /**
         * Column count.
         */
        private static final int COLUMN_COUNT = 7;

        // ----- data members -----------------------------------------------

        /**
         * The panel used to display the elastic data information.
         */
        private CoherenceElasticDataPanel pnlElasticData;

        /**
         * The {@link TableModel} to display detail data.
         */
        private DefaultTableModel tmodel;

        /**
         * the {@link ExportableJTable} to use to display detail data.
         */
        private ExportableJTable table;

        /**
         * The scroll pane to display the table in.
         */
        final JScrollPane pneMessage;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Indicates if we have set the bounds for the progress bars.
     */
    private boolean m_fProgressBarInitialized = false;

    /**
     * Indication of how much RAM is used.
     */
    private JProgressBar barRamUsage;

    /**
     * Indication of how much flash is used.
     */
    private JProgressBar barFlashUsage;

    /**
     * Shows files used out of total.
     */
    private JLabel lblRam;

    /**
     * Shows files used out of total.
     */
    private JLabel lblFlash;

    /**
     * The graph of overall ramjournal memory.
     */
    private SimpleXYChartSupport ramJournalMemoryGraph = null;

    /**
     * The graph of overall ramjournal compactions.
     */
    private SimpleXYChartSupport ramJournalCompactionGraph = null;

    /**
     * The ramjournal data retrieved from the {@link VisualVMModel}.
     */
    private java.util.List<Map.Entry<Object, Data>> ramJournalData;

    /**
     * The graph of overall flashjournal memory.
     */
    private SimpleXYChartSupport flashJournalMemoryGraph = null;

    /**
     * The graph of overall flashjournal compactions.
     */
    private SimpleXYChartSupport flashJournalCompactionGraph = null;

    /**
     * The flashjournal data retrieved from the {@link VisualVMModel}.
     */
    private java.util.List<Map.Entry<Object, Data>> flashJournalData;

    /**
     * Last ramjournal compaction count.
     */
    int m_cLastRamCompaction = -1;

    /**
     * Last flashjournal compaction count.
     */
    int m_cLastFlashCompaction = -1;

    /**
     * Last ramjournal exhaustive compaction count.
     */
    int m_cLastRamExhaustive = -1;

    /**
     * Last ramjournal exhaustive compaction count.
     */
    int m_cLastFlashExhaustive = -1;
    }
