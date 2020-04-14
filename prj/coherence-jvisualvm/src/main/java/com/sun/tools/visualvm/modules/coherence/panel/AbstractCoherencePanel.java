/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.panel;

import com.sun.tools.visualvm.modules.coherence.Localization;
import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import com.sun.tools.visualvm.modules.coherence.helper.RenderHelper;
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;

import com.sun.tools.visualvm.modules.coherence.panel.util.AbstractMenuOption;
import com.sun.tools.visualvm.modules.coherence.panel.util.ExportableJTable;

import com.sun.tools.visualvm.modules.coherence.tablemodel.model.AbstractData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.NodeStorageData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Pair;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * An abstract implementation of a {@link JPanel} which provides basic support
 * to be displayed as JVisualVM plug-in.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public abstract class AbstractCoherencePanel
        extends JPanel
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create an instance of the panel which will be used to display
     * data retrieved from JMX.
     *
     * @param  manager the {@link LayoutManager} to use
     * @param  model   the {@link VisualVMModel} to interrogate for data
     */
    public AbstractCoherencePanel(LayoutManager manager, VisualVMModel model)
        {
        super(manager);
        this.model = model;
        this.setOpaque(false);
        }

    // ----- AbstractCoherencePanel methods ----------------------------------

    /**
     * Called to update any GUI related artifacts. Called from done().
     */
    public abstract void updateGUI();

    /**
     * Update any data. Called from doInBackground().
     */
    public abstract void updateData();

    // ----- accessors -------------------------------------------------------

    /**
     * Set the {@link MBeanServerConnection} to get JMX data from.
     *
     * @param requestSender  the {@link RequestSender} to get JMX data from
     */
    public void setRequestSender(RequestSender requestSender)
        {
        this.requestSender = requestSender;
        }

    /**
     * Create a {@link JLabel} with the specified localized text and set the
     * {@link Component} that the label is for to help with accessibility.
     *
     * @param sKey      the key to look up in Bundle.properties
     * @param component the {@link Component} that the label is for or null
     *
     * @return a {@link JLabel} with the specified text
     */
    protected JLabel getLocalizedLabel(String sKey, Component component)
        {
        JLabel label = new JLabel();
        label.setOpaque(false);

        label.setText(Localization.getLocalText(sKey) + ":");

        if (component != null)
            {
            label.setLabelFor(component);
            }

        return label;
        }

    /**
     * Create a {@link JLabel} with the specified localized text.
     *
     * @param sKey  the key to look up in Bundle.properties
     *
     * @return a {@link JLabel} with the specified text
     */
    protected JLabel getLocalizedLabel(String sKey)
        {
        return getLocalizedLabel(sKey, (Component) null);
        }

    /**
     * Return localized text given a key.
     *
     * @param sKey  the key to look up in Bundle.properties
     *
     * @return localized text given a key
     */
    protected String getLocalizedText(String sKey)
        {
        return Localization.getLocalText(sKey);
        }

    /**
     * Return a label which is just a filler.
     *
     * @return a label which is just a filler
     *
     */
    protected JLabel getFiller()
        {
        JLabel label = new JLabel();

        label.setText(FILLER);

        return label;
        }

    /**
     * Create a {@link JTextField} with the specified width and make it
     * right aligned.
     *
     * @param width  the width for the {@link JTextField}
     *
     * @return the newly created text field
     */
    protected JTextField getTextField(int width)
        {
        return getTextField(width, JTextField.RIGHT);
        }

    /**
     * Create a {@link JTextField} with the specified width and specified
     * alignment.
     *
     * @param  width  the width for the {@link JTextField}
     * @param  align  either {@link JTextField}.RIGHT or LEFT
     *
     * @return the newly created text field
     */
    protected JTextField getTextField(int width, int align)
        {
        JTextField textField = new JTextField();

        textField.setEditable(false);
        textField.setColumns(width);
        textField.setHorizontalAlignment(align);

        textField.setOpaque(false);

        return textField;
        }

    /**
     * Fire a tableDataChanged but save and re-apply any selection.
     *
     * @param table  the {@link ExportableJTable} to save selection for
     * @param model  the {@link AbstractTableModel} to refresh
     */
    protected void fireTableDataChangedWithSelection(ExportableJTable table, AbstractTableModel model)
        {
        int nSelectedRow = table.getListener().getSelectedRow();

        model.fireTableDataChanged();
        table.getListener().setSelectedRow(nSelectedRow);
        }

    /**
     * Configure a {@link javax.swing.JScrollPane} with common settings.
     *
     * @param pneScroll the scroll pane to configure
     * @param table the table to get background from
     */
    protected void configureScrollPane(JScrollPane pneScroll, JTable table)
        {
        pneScroll.getViewport().setBackground(table.getBackground());
        }

    /**
     * Get a full qualified MBean name.
     *
     * @param requestSender  the {@link RequestSender} to perform additional queries
     * @param sQuery         the query to execute
     *
     * @return the fully qualified MBean name
     *
     * @throws Exception  the relevant exception
     */
    protected String getFullyQualifiedName(RequestSender requestSender, String sQuery)
            throws Exception
        {
        // look up the full name of the MBean in case we are in container
        Set<ObjectName> setResult = requestSender.getCompleteObjectName(new ObjectName(sQuery));

        for (Object oResult : setResult)
            {
            return oResult.toString();
            }

        return null;
        }

    /**
     * Returns true if the node is storage-enabled.
     *
     * @param nodeId   the node id to check
     * @return true if the node is storage-enabled
     */
    protected boolean isNodeStorageEnabled(int nodeId)
        {
        for (Map.Entry<Object, Data> entry : model.getData(VisualVMModel.DataType.NODE_STORAGE))
            {
            if ((Integer) entry.getValue().getColumn(NodeStorageData.NODE_ID) == nodeId)
                {
                return (Boolean) entry.getValue().getColumn(NodeStorageData.STORAGE_ENABLED);
                }
            }

        // no node id found
        return false;
        }

    // ----- inner classes --------------------------------------------------

    /**
     * A menu option to display a detailed list of attributes from the
     * currently selected row.
     */
    protected class ShowDetailMenuOption
            extends AbstractMenuOption
        {
        /**
         * {@inheritDoc}
         */
        public ShowDetailMenuOption(VisualVMModel model, ExportableJTable jtable, int nSelectedItem)
            {
            super(model, requestSender, jtable);
            f_nSelectedItem = nSelectedItem;

            // setup the table
            tmodel = new DefaultTableModel(new Object[] {Localization.getLocalText("LBL_name") ,
                Localization.getLocalText("LBL_value")}, 2)
                    {
                    @Override
                    public boolean isCellEditable(int row, int column)
                        {
                        return false;
                        }
                    };
            table = new ExportableJTable(tmodel);
            RenderHelper.setHeaderAlignment(table, JLabel.CENTER);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            table.setPreferredScrollableViewportSize(new Dimension((int) (Math.max((int) (screenSize.getWidth() * 0.5),
                800)), table.getRowHeight() * 20));

            table.setIntercellSpacing(new Dimension(6, 3));
            table.setRowHeight(table.getRowHeight() + 4);

            pneMessage = new JScrollPane(table);
            configureScrollPane(pneMessage, table);
            AbstractMenuOption.setResizable(pneMessage);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMenuItem()
            {
            return m_sMenuLabel;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(ActionEvent actionEvent)
            {
            int    nRow   = getSelectedRow();
            Object oValue = null;
            String sQuery = null;

            if (nRow == -1)
                {
                JOptionPane.showMessageDialog(null, Localization.getLocalText("LBL_must_select_row"));
                }
            else
                {
                try
                    {
                    oValue = getJTable().getModel().getValueAt(nRow, 0);

                    // determine any specific values to substitute
                    if (f_nSelectedItem == SELECTED_NODE)
                        {
                        sQuery = "Coherence:type=Node,nodeId=" + oValue + ",*";
                        }
                    else if (f_nSelectedItem == SELECTED_SERVICE)
                        {
                        String sSelectedService = model.getSelectedService();

                        // extract domainPartition and service name if we have one
                        String[] asServiceDetails = AbstractData.getDomainAndService(sSelectedService);
                        String   sDomainPartition = asServiceDetails[0];
                        sSelectedService = asServiceDetails[1];

                        sQuery = "Coherence:type=Service,name=" + sSelectedService +
                                (sDomainPartition != null ? ",domainPartition=" + sDomainPartition : "") + ",nodeId=" + oValue + ",*";
                        }
                    else if (f_nSelectedItem == SELECTED_CACHE)
                        {
                        Pair<String, String> selectedCache = model.getSelectedCache();
                        sQuery = "Coherence:type=Cache,service=" + getServiceName(selectedCache.getX()) + ",name=" + selectedCache.getY() +
                                 ",tier=back,nodeId=" + oValue + getDomainPartitionKey(selectedCache.getX()) + ",*";
                        }
                    else if (f_nSelectedItem == SELECTED_STORAGE)
                        {
                        Pair<String, String> selectedCache = model.getSelectedCache();
                        sQuery = "Coherence:type=StorageManager,service=" + getServiceName(selectedCache.getX()) + ",cache=" + selectedCache.getY() +
                                 ",nodeId=" + oValue + getDomainPartitionKey(selectedCache.getX()) + ",*";
                        }
                    else if (f_nSelectedItem == SELECTED_JCACHE)
                        {
                        Pair<String, String> selectedCache = model.getSelectedJCache();
                        sQuery = "javax.cache:type=CacheStatistics,CacheManager=" + selectedCache.getX() + ",Cache=" + selectedCache.getY() + ",*";
                        }
                    else if (f_nSelectedItem == SELECTED_FRONT_CACHE)
                        {
                        Pair<String, String> selectedCache = model.getSelectedCache();
                        sQuery = "Coherence:type=Cache,service=" + getServiceName(selectedCache.getX()) + ",name=" + selectedCache.getY() +
                                 ",tier=front,nodeId=" + oValue + getDomainPartitionKey(selectedCache.getX()) + ",*";
                        }

                    // remove any existing rows
                    tmodel.getDataVector().removeAllElements();
                    tmodel.fireTableDataChanged();
                    populateAllAttributes(sQuery);
                    pneMessage.getVerticalScrollBar().setValue(0);
                    tmodel.fireTableDataChanged();

                    JOptionPane.showMessageDialog(null, pneMessage, Localization.getLocalText("LBL_details"),
                                                  JOptionPane.INFORMATION_MESSAGE);

                    }
                catch (Exception e)
                    {
                    showMessageDialog(Localization.getLocalText("LBL_error"), e.getMessage(),
                                      JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    }
                }
            }

        // ----- helpers --------------------------------------------------------

        /**
         * Return the domainPartition key or empty string if the service doesn't contain one.
         *
         * @param sSelectedServiceName  service name to interrogate
         *
         * @return the domain partition key or empty string
         */
        protected String getDomainPartitionKey(String sSelectedServiceName)
            {
            String[] asServiceDetails = AbstractData.getDomainAndService(sSelectedServiceName);
            String   sDomainPartition = asServiceDetails[0];

            return (sDomainPartition != null ? ",domainPartition=" + sDomainPartition : "");
            }

        /**
         * Return the service part of a the selected service.
         *
         * @param sSelectedServiceName  service name to interrogate
         *
         * @return the service name to return
         */
        protected String getServiceName(String sSelectedServiceName)
            {
            String[] asParts = AbstractData.getDomainAndService(sSelectedServiceName);
            return asParts[1];
            }

        /**
         * Populate all of the attributes for the given query.
         *
         * @param  sQuery the query to run
         *
         * @throws Exception if any relevant error
         */
        protected void populateAllAttributes(String sQuery)
                throws Exception
            {
            int row = 0;

            Set<ObjectName> setObjects = requestSender.getCompleteObjectName(new ObjectName(sQuery));

            for (Iterator<ObjectName> iter = setObjects.iterator(); iter.hasNext(); )
                {
                ObjectName objName = (ObjectName) iter.next();
                tmodel.insertRow(row++, new Object[]{"JMX Key", objName.toString()});

                List<Attribute> lstAttr = requestSender.getAllAttributes(objName);

                for (Attribute attr : lstAttr)
                    {
                    Object oValue = attr.getValue();
                    if (oValue instanceof Object[])
                        {
                        oValue = Arrays.toString((Object[]) oValue);
                        }
                    tmodel.insertRow(row++, new Object[] {attr.getName(), oValue});
                    }
                }

            }

        // ----- data members ---------------------------------------------------

        /**
         * The selected item to build query from.
         */
        protected int f_nSelectedItem;

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

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -7607701492285533521L;

    /**
     * Filler for spacing.
     */
    protected static final String FILLER = "   ";

    /**
     * Comma number formatter.
     */
    protected final String COMMA_NUMBER_FORMAT = "%,d";

    /**
     * Indicates to select selected service.
     */
    public static final int SELECTED_NODE = 0;

    /**
     * Indicates to select selected service.
     */
    public static final int SELECTED_SERVICE = 1;

    /**
     * Indicates to select selected cache.
     */
    public static final int SELECTED_CACHE = 2;

    /**
     * Indicates to select selected storage.
     */
    public static final int SELECTED_STORAGE = 3;

    /**
     * Indicates to select JCache.
     */
    public static final int SELECTED_JCACHE = 4;

    /**
     * Indicates to select selected front cache.
     */
    public static final int SELECTED_FRONT_CACHE = 5;

    // ----- data members ---------------------------------------------------

    /**
     * The request sender to use.
     */
    protected RequestSender requestSender;

    /**
     * The visualVM model.
     */
    protected final VisualVMModel model;
    }
