/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.panel.util;

import com.sun.tools.visualvm.modules.coherence.Localization;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.filechooser.FileNameExtensionFilter;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * An implementation of a {@link JTable} that allows exporting table data as CSV
 * as well as addition of additional menu options for right click.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class ExportableJTable
        extends JTable
        implements ActionListener, AdditionalMenuOptions
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Create the table.
     *
     * @param model the {@link TableModel} to base this {@link JTable} on
     */
    public ExportableJTable(TableModel model)
        {
        super(model);

        if  (fUseJTableSorting)
            {
            setAutoCreateRowSorter(true);
            }

        // ensure users can only ever select one row at a time
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setRowSelectionAllowed(true);

        ListSelectionModel rowSelectionModel = this.getSelectionModel();

        f_listener = new DefaultRowListSelectionListener(this);
        rowSelectionModel.addListSelectionListener(f_listener);

        // set more pleasant gird lines view
        setShowGrid(true);
        String sOS = System.getProperty("os.name").toLowerCase();
        if (sOS.indexOf("windows") != -1)
            {
            setGridColor(UIManager.getColor("controlHighlight"));
            }
        else if (sOS.indexOf("mac") != -1)
            {
            setGridColor(Color.LIGHT_GRAY);
            }
        }

    // ---- JTable methods --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public JPopupMenu getComponentPopupMenu()
        {
        if (menu == null)
            {
            menu     = new JPopupMenu("Table Options:");

            menuItem = new JMenuItem(Localization.getLocalText("LBL_save_data_as"));

            menuItem.addActionListener(this);
            menu.add(menuItem);

            // add additional menu options
            if (m_menuOption != null)
                {
                menu.addSeparator();

                for (MenuOption menuOption : m_menuOption)
                    {
                    if (menuOption instanceof SeparatorMenuOption)
                        {
                        menu.addSeparator();
                        }
                    else
                        {
                        if (menuOption != null)
                            {
                            JMenuItem newMenuItem = new JMenuItem(menuOption.getMenuItem());

                            newMenuItem.addActionListener(menuOption);
                            menu.add(newMenuItem);
                            }
                        }
                    }
                }
            }

        return menu;
        }

    // ---- ActionListener methods ------------------------------------------

    /**
     * Respond to a right-click event for saving data to disk.
     *
     * @param event  the event
     */
    public void actionPerformed(ActionEvent event)
        {
        JComponent src = (JComponent) event.getSource();

        if (src.equals(menuItem))
            {
            int result = fileChooser.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION)
                {
                saveTableDataToFile(fileChooser.getSelectedFile());
                }
            }
        }

    // ----- AdditionalMenuOptions methods ----------------------------------

    /**
     * {@inheritDoc}
     */
    public MenuOption[] getMenuOptions()
        {
        return m_menuOption;
        }

    /**
     * {@inheritDoc}
     */
    public void setMenuOptions(MenuOption[] menuOptions)
        {
        m_menuOption = menuOptions;
        }

    // ----- ExportableJTable methods ---------------------------------------

    /**
     * Return the {link ListSelectionListener} that has been setup for this table.
     *
     * @return the {link ListSelectionListener} that has been setup for this table
     */
    public DefaultRowListSelectionListener getListener()
        {
        return f_listener;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Save the data for the table to a CSV file.
     *
     * @param file  the {@link File} to save to
     */
    private void saveTableDataToFile(File file)
        {
        PrintStream fileWriter = null;

        try
            {
            fileWriter = new PrintStream(new FileOutputStream(file));

            AbstractTableModel tableModel = (AbstractTableModel) this.getModel();

            // Get the column headers

            TableColumnModel columnModel = this.getTableHeader().getColumnModel();
            int              columnCount = columnModel.getColumnCount();

            for (int i = 0; i < columnCount; i++)
                {
                fileWriter.print("\"" + columnModel.getColumn(i).getHeaderValue()
                                 + (i < columnCount - 1 ? "\"," : "\""));
                }

            fileWriter.print(LF);

            // output the data line by line
            for (int r = 0; r < tableModel.getRowCount(); r++)
                {
                for (int c = 0; c < columnCount; c++)
                    {
                    Object oValue = tableModel.getValueAt(r, c);

                    fileWriter.print((oValue == null ? "" : oValue.toString()) + (c < columnCount - 1 ? "," : ""));
                    }

                fileWriter.print(LF);
                }

            }
        catch (IOException ioe)
            {
            LOGGER.log(Level.WARNING, Localization.getLocalText("LBL_unable_to_save", new String[] {file.toString(),
                ioe.getMessage()}));
            }
        finally
            {
            if (fileWriter != null)
                {
                fileWriter.close();
                }
            }
        }

    /**
     * An implementation of a {@link JFileChooser} that will confirm overwrite of
     * an existing file.
     *
     */
    public static class CheckExistsFileChooser
            extends JFileChooser
        {
        /**
         * Default constructor.
         */
        public CheckExistsFileChooser()
            {
            super();
            }

        /**
         * Constructor taking a directory name to check for existence.
         *
         * @param sDirectory the directory to check for existence
         */
        public CheckExistsFileChooser(String sDirectory)
            {
            super(sDirectory);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void approveSelection()
            {
            if (!validateFileSelection(this.getSelectedFile()))
                {
                }
            else
                {
                super.approveSelection();
                }
            }

        /**
         * If a file exists, then ensure you ask the user if they want to
         * overwrite it.
         *
         * @param file  the {@link File} that was selected
         *
         * @return true if the file does not exist or the user wants to overwrite it
         */
        private boolean validateFileSelection(File file)
            {
            if (file.exists())
                {
                String sQuestion = Localization.getLocalText("LBL_file_already_exists",
                                       new String[] {file.getAbsolutePath()});

                if (JOptionPane.showConfirmDialog(null, sQuestion, Localization.getLocalText("LBL_confirm"),
                                                  JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    {
                    return true;
                    }

                return false;
                }

            return true;
            }
        }

    // ----- inner classes --------------------------------------------------

    /**
     * A {@link ListSelectionListener} that allows us to re-select a row after the
     * table has been redrawn.
     *
     */
    public class DefaultRowListSelectionListener
            implements ListSelectionListener
        {
        /**
         * Create a new listener to allow us to re-select the row if it was selected before
         * the refresh.
         *
         * @param table the {@link JTable} that this listener applies to
         */
        public DefaultRowListSelectionListener(JTable table)
            {
            m_table = table;
            }

        @Override
        public void valueChanged(ListSelectionEvent listSelectionEvent)
            {
            if (listSelectionEvent.getValueIsAdjusting())
                {
                return;
                }

            ListSelectionModel selectionModel = (ListSelectionModel) listSelectionEvent.getSource();

            if (!selectionModel.isSelectionEmpty())
                {
                m_nSelectedRow = selectionModel.getMinSelectionIndex();
                }
            else
                {
                m_nSelectedRow = -1;
                }
            }

        /**
         * Re-select the last selected row.
         */
        public void updateRowSelection()
            {
            if (m_nSelectedRow != -1)
                {
                m_table.addRowSelectionInterval(m_nSelectedRow, m_nSelectedRow);
                }
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the currently selected row.
         *
         * @return the currently selected row
         */
        public int getSelectedRow()
            {
            return m_nSelectedRow;
            }

        /**
         * Set the currently selected row.
         *
         * @param nSelectedRow the row to select
         */
        public void setSelectedRow(int nSelectedRow)
            {
            m_nSelectedRow = nSelectedRow;
            updateRowSelection();
            }

        // ----- data members -----------------------------------------------

        /**
         * The selected row.
         */
        private int m_nSelectedRow = -1;

        /**
         * The {@link JTable} that this applies to.
         */
        private JTable m_table;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 5999795232769091368L;

    /**
     * The line separator for the platform this process is running on.
     */
    private static String LF = System.getProperty("line.separator");

    /**
     * File chooser to select a file.
     */
    private static JFileChooser fileChooser = null;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(ExportableJTable.class.getName());

    /**
     * Provide ability to turn off JTable sorting via hidden option - on by default.
     */
    private static final boolean fUseJTableSorting =
            "true".equals(System.getProperty("coherence.jvisualvm.sorting.enabled", "true"));

    /**
     * Initialize so that we only get one instance.
     */
    static
        {
        fileChooser = new CheckExistsFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(Localization.getLocalText("LBL_csv_file"), "csv"));
        }

    // ----- data members ---------------------------------------------------

    private MenuOption[] m_menuOption = null;

    /**
     * Right-click menu.
     */
    private JPopupMenu menu = null;

    /**
     * Menu item for "Save As".
     */
    private JMenuItem menuItem;

    /**
     * The row selection listener.
     */
    private final DefaultRowListSelectionListener f_listener;
    }
