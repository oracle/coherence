/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.helper;

import com.sun.tools.visualvm.modules.coherence.Localization;

import java.awt.Color;
import java.awt.Component;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JTable;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Various methods to help in rendering table rows and columns.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 *
 */
public class RenderHelper
    {
    // ----- helpers --------------------------------------------------------

    /**
     * Set the renderer for a particular table and column.
     *
     * @param table    the {@link JTable} to set the renderer for
     * @param col      the column, index of 0, to set the renderer for
     * @param renderer the {@link TableCellRenderer} to apply to the table and column
     */
    public static void setColumnRenderer(JTable table, int col, TableCellRenderer renderer)
        {
        TableColumn column = table.getColumnModel().getColumn(col);

        if (column == null)
            {
            throw new IllegalArgumentException("No column number " + col + " for table");
            }

        column.setCellRenderer(renderer);
        }

    /**
     * Set the renderer to a MillisRenderer() for a particular table and column
     *
     * @param table the {@link JTable} to set the renderer for
     * @param col   the column, index of 0, to set the renderer for
     */
    public static void setMillisRenderer(JTable table, int col)
        {
        TableColumn column = table.getColumnModel().getColumn(col);

        if (column == null)
            {
            throw new IllegalArgumentException("No column number " + col + " for table");
            }

        column.setCellRenderer(new DecimalRenderer());
        }

    /**
     * Set the renderer to a IntegerRenderer() for a particular table and column
     *
     * @param table the {@link JTable} to set the renderer for
     * @param col   the column, index of 0, to set the renderer for
     */
    public static void setIntegerRenderer(JTable table, int col)
        {
        TableColumn column = table.getColumnModel().getColumn(col);

        if (column == null)
            {
            throw new IllegalArgumentException("No column number " + col + " for table");
            }

        column.setCellRenderer(new IntegerRenderer());
        }

    /**
     * Sets the default table header alignment for all columns for a table.
     *
     * @param table the {@link JTable} to set the alignment for
     * @param align the alignment, either {@link JLabel}.RIGHT or {@link JLabel}.LEFT.
     */
    public static void setHeaderAlignment(JTable table, int align)
        {
        DefaultTableCellRenderer renderer = ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer());

        renderer.setHorizontalAlignment(align);
        table.getTableHeader().setDefaultRenderer(renderer);
        }

    /**
     * Sets the default table header alignment for a particular column for a table.
     *
     * @param table the {@link JTable} to set the alignment for
     * @param col   the column to set alignment for
     * @param align the alignment, either {@link JLabel}.RIGHT or {@link JLabel}.LEFT.
     */
    public static void setHeaderAlignment(JTable table, int col, int align)
        {
        DefaultTableCellRenderer renderer = ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer());

        renderer.setHorizontalAlignment(align);
        table.getColumn(table.getColumnName(col)).setHeaderRenderer(renderer);
        }

    /**
     * Renderer for cache hit rate.
     */
    @SuppressWarnings("serial")
    public static class CacheHitProbabilityRateRenderer
            extends DefaultTableCellRenderer
        {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column)
            {
            Component c      = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            float     fValue = new Float(getText());

            if (fValue <= 0.500)
                {
                setBackground(Color.red);
                setForeground(Color.white);
                }
            else if (fValue <= 0.75)
                {
                setBackground(Color.orange);
                setForeground(Color.white);
                }
            else
                {
                setBackground(Color.white);
                setForeground(Color.black);
                }

            if (c instanceof JLabel && value instanceof Number)
                {
                JLabel renderedLabel = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                                           row, column);

                renderedLabel.setHorizontalAlignment(JLabel.RIGHT);

                String text = MILLIS_FORMAT.format((Number) value);

                renderedLabel.setText(text);
                }

            return c;
            }
        }

    /**
     * Render for a milliseconds column of type Float.
     *
     */
    @SuppressWarnings("serial")
    public static class DecimalRenderer
            extends DefaultTableCellRenderer
        {
        /**
         * Construct an MillisRenderer with a default alignment of RIGHT and
         * default millis renderer.
         */
        public DecimalRenderer()
            {
            super();
            setHorizontalAlignment(JLabel.RIGHT);
            }

        /**
         * Construct an MillisRenderer with a default alignment of RIGHT and
         * supplied {@link NumberFormat}.
         *
         * @param sFormat  the {@link NumberFormat} to use to render
         */
        public DecimalRenderer(NumberFormat sFormat)
            {
            super();
            this.numberFormat = sFormat;
            setHorizontalAlignment(JLabel.RIGHT);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column)
            {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (c instanceof JLabel && value instanceof Number)
                {
                JLabel renderedLabel = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                                           row, column);

                renderedLabel.setHorizontalAlignment(JLabel.RIGHT);

                String text = numberFormat.format((Number) value);

                renderedLabel.setText(text);
                }

            return c;
            }

        /**
         * Default the format to millis.
         */
        private NumberFormat numberFormat = MILLIS_FORMAT;
        }

    /**
     * Renderer for the free memory percent.
     */
    public static class FreeMemoryRenderer
            extends DefaultTableCellRenderer
        {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column)
            {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!"".equals(getText()) && getText() != null)
                {
                float fValue = new Float(getText());

                if (fValue < .15)
                    {
                    setBackground(Color.red);
                    setForeground(Color.white);
                    setToolTipText(MEMORY_15_TOOLTIP);
                    }
                else if (fValue < 0.25)
                    {
                    setBackground(Color.orange);
                    setForeground(Color.black);
                    setToolTipText(MEMORY_25_TOOLTIP);
                    }
                else
                    {
                    setBackground(Color.white);
                    setForeground(Color.black);
                    setToolTipText(null);
                    }

                if (c instanceof JLabel && value instanceof Number)
                    {
                    JLabel renderedLabel = (JLabel) super.getTableCellRendererComponent(table, value, isSelected,
                                               hasFocus, row, column);

                    renderedLabel.setHorizontalAlignment(JLabel.RIGHT);

                    String text = PERCENT_FORMAT.format((Number) value);

                    renderedLabel.setText(text);
                    }
                }
            else
                {
                setBackground(Color.white);
                setForeground(Color.black);
                }

            return c;
            }
        }

    /**
     * Render for a standard Integer.
     *
     */
    @SuppressWarnings("serial")
    public static class IntegerRenderer
            extends DefaultTableCellRenderer
        {
        /**
         * Construct an IntegerRenderer with a default alignment of RIGHT.
         */
        public IntegerRenderer()
            {
            super();
            setHorizontalAlignment(JLabel.RIGHT);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column)
            {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (c instanceof JLabel && (value instanceof Integer || value instanceof Long))
                {
                JLabel label = (JLabel) c;

                label.setHorizontalAlignment(JLabel.RIGHT);

                String text = INTEGER_FORMAT.format((Number) value);

                label.setText(text);
                }

            return c;
            }
        }

    /**
     * Renderer for memory/bytes to display KB/MB/GB/TB
     */
    public static class BytesRenderer extends IntegerRenderer
       {
       public BytesRenderer()
           {
           super();
           }

       /**
        * {@inheritDoc}
        */
       @Override
       public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                      boolean hasFocus, int row, int column)
           {
           Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

           if (c instanceof JLabel && (value instanceof Integer || value instanceof Long))
               {
               JLabel label = (JLabel) c;

               label.setHorizontalAlignment(JLabel.RIGHT);
               long nLongValue = value instanceof Integer ? ((Integer)value) * 1L : ((Long)value).longValue();

               label.setText(getRenderedBytes(nLongValue));
               }

           return c;
           }
       }

    /**
     * Renderer for a statusHA column.
     */
    @SuppressWarnings("serial")
    public static class StatusHARenderer
            extends DefaultTableCellRenderer
        {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column)
            {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if ("ENDANGERED".equals(getText()))
                {
                setBackground(Color.red);
                setForeground(Color.white);
                setToolTipText(ENDANGERED_TOOLTIP);
                }
            else if ("NODE-SAFE".equals(getText()))
                {
                setBackground(Color.orange);
                setForeground(Color.black);
                setToolTipText(NODE_SAFE_TOOLTIP);
                }
            else if ("n/a".equals(getText()))
                {
                setBackground(Color.white);
                setForeground(Color.black);
                setToolTipText(null);
                }
            else
                {
                setBackground(Color.green);
                setForeground(Color.black);

                setToolTipText((getText().equals("MACHINE-SAFE")
                                ? MACHINE_SAFE_TOOLTIP
                                : (getText().equals("RACK-SAFE") ? RACK_SAFE_TOOLTIP : SITE_SAFE_TOOLTIP)));
                }

            return c;
            }
        }

    /**
     * Renderer for a Unit Calculator column.
     */
    @SuppressWarnings("serial")
    public static class UnitCalculatorRenderer
            extends DefaultTableCellRenderer
        {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column)
            {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if ("FIXED".equals(getText()))
                {
                setBackground(Color.orange);
                setForeground(Color.black);
                setToolTipText(FIXED_UNIT_CALCULATOR_TOOLTIP);
                }
            else
                {
                setBackground(Color.white);
                setForeground(Color.black);
                setToolTipText("");
                }

            return c;
            }
        }

    /**
     * Renderer to display tool tip of the cell contents.
     */
    public static class ToolTipRenderer
            extends DefaultTableCellRenderer
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column)
            {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String sText = getText();
            setToolTipText(sText == null || sText.isEmpty()? null : sText);

            return c;
            }
        }

    /**
     * Renderer for a publisher or receiver success rate.
     */
    @SuppressWarnings("serial")
    public static class SuccessRateRenderer
            extends DefaultTableCellRenderer
        {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column)
            {
            Component c      = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            float     fValue = new Float(getText());

            if (fValue <= 0.900)
                {
                setBackground(Color.red);
                setForeground(Color.white);
                setToolTipText(PUBLISHER_TOOLTIP);
                }
            else if (fValue <= 0.950)
                {
                setBackground(Color.orange);
                setForeground(Color.white);
                setToolTipText(RECEIVER_TOOLTIP);
                }
            else
                {
                setBackground(Color.white);
                setForeground(Color.black);
                setToolTipText(null);
                }

            if (c instanceof JLabel && value instanceof Number)
                {
                JLabel renderedLabel = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                                           row, column);

                renderedLabel.setHorizontalAlignment(JLabel.RIGHT);

                String text = MILLIS_FORMAT.format((Number) value);

                renderedLabel.setText(text);
                }

            return c;
            }
        }

    /**
     * Renderer for the thread utilization.
     */
    public static class ThreadUtilRenderer
            extends DefaultTableCellRenderer
        {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column)
            {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!"".equals(getText()) && getText() != null)
                {
                float fValue = new Float(getText());

                if (fValue >= 0.90)
                    {
                    setBackground(Color.red);
                    setForeground(Color.white);
                    }
                else if (fValue >= 0.60)
                    {
                    setBackground(Color.orange);
                    setForeground(Color.black);
                    }
                else
                    {
                    setBackground(Color.white);
                    setForeground(Color.black);
                    }

                if (c instanceof JLabel && value instanceof Number)
                    {
                    JLabel renderedLabel = (JLabel) super.getTableCellRendererComponent(table, value, isSelected,
                                               hasFocus, row, column);

                    renderedLabel.setHorizontalAlignment(JLabel.RIGHT);

                    String text = PERCENT_FORMAT.format((Number) value);

                    renderedLabel.setText(text);
                    }
                }
            else
                {
                setBackground(Color.white);
                setForeground(Color.black);
                }

            return c;
            }
        }

    /**
     * Render for node state attrubute in federation tab.
     */
    @SuppressWarnings("serial")
    public static class FedNodeStateRenderer
            extends DefaultTableCellRenderer
        {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column)
            {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if ("ERROR".equals(getText()))
                {
                setBackground(Color.red);
                setForeground(Color.white);
                }
            else if ("BACKLOG_EXCESSIVE".equals(getText()))
                {
                setBackground(Color.orange);
                setForeground(Color.black);
                setToolTipText(BACKLOG_EXCESSIVE_TOOLTIP);
                }
            else
                {
                setBackground(Color.green);
                setForeground(Color.black);
                }

            return c;
            }
        }

    /**
     * Render for service state attribute in federation tab.
     */
    @SuppressWarnings("serial")
    public static class FedServiceStateRenderer
            extends DefaultTableCellRenderer
        {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column)
            {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (c instanceof JLabel)
                {
                JLabel label = (JLabel) c;

                if ("0".equals(getText()))
                    {
                    setBackground(Color.green);
                    setForeground(Color.black);
                    label.setText("HEALTHY");
                    }
                else if ("1".equals(getText()))
                    {
                    setBackground(Color.orange);
                    setForeground(Color.black);
                    label.setText("WARNING");
                    }
                else if ("2".equals(getText()))
                    {
                    setBackground(Color.red);
                    setForeground(Color.white);
                    label.setText("ERROR");
                    }
                else
                    {
                    label.setText("NO OUTBOUND CONNECTIONS");
                    setBackground(Color.white);
                    setForeground(Color.black);
                    }
                }

            return c;
            }
        }

    /**
     * Return a formatted byte value with appropriate suffix such as KB/MB/GB/TB etc.
     *
     * @param nValue the byte value
     *
     * @return the formatted String
     */
    public static String getRenderedBytes(long nValue)
        {
        // inspired by http://stackoverflow.com/questions/3263892/format-file-size-as-mb-gb-etc

        int nUnit = 0;
        for (;nValue > GraphHelper.KB * GraphHelper.KB; nValue >>= 10)
            {
            nUnit++;
            }

        if (nValue > GraphHelper.KB)
            {
            nUnit++;
            }

        return String.format("%.1f %cB", nValue / (GraphHelper.KB * 1.0f), " KMGTPE".charAt(nUnit));
        }

    // ----- constants ------------------------------------------------------

    /**
     * Format for millis renderer.
     */
    public static NumberFormat MILLIS_FORMAT = new DecimalFormat("#,##0.0000");

    /**
     * Format for load average renderer.
     */
    public static NumberFormat LOAD_AVERAGE_FORMAT = new DecimalFormat("#,##0.00");

    /**
     * Format for % renderer.
     */
    public static NumberFormat PERCENT_FORMAT = new DecimalFormat("##0%");

    /**
     * Format for integer renderer.
     */
    public static NumberFormat INTEGER_FORMAT = new DecimalFormat("###,###,###,###,###");

    /**
     * Node safe tool tip.
     */
    public static final String NODE_SAFE_TOOLTIP = Localization.getLocalText("TTIP_node_safe");

    /**
     * Machine safe tool tip.
     */
    public static final String MACHINE_SAFE_TOOLTIP = Localization.getLocalText("TTIP_machine_safe");

    /**
     * Rack safe tool tip.
     */
    public static final String RACK_SAFE_TOOLTIP = Localization.getLocalText("TTIP_rack_safe");

    /**
     * Site safe tool tip.
     */
    public static final String SITE_SAFE_TOOLTIP = Localization.getLocalText("TTIP_site_safe");

    /**
     * Endangered tool tip.
     */
    public static final String ENDANGERED_TOOLTIP = Localization.getLocalText("TTIP_endangered");

    /**
     * Publisher tool tip.
     */
    public static final String PUBLISHER_TOOLTIP = Localization.getLocalText("TTIP_publisher");

    /**
     * Receiver tool tip.
     */
    public static final String RECEIVER_TOOLTIP = Localization.getLocalText("TTIP_receiver");

    /**
     * 15% memory tool tip.
     */
    public static final String MEMORY_15_TOOLTIP = Localization.getLocalText("TTIP_mem_15");

    /**
     * 25% memory tool tip.
     */
    public static final String MEMORY_25_TOOLTIP = Localization.getLocalText("TTIP_mem_25");

    /**
     * Fixed Unit calculator memory tool tip.
     */
    public static final String FIXED_UNIT_CALCULATOR_TOOLTIP = Localization.getLocalText("TTIP_fixed_unit_calculator");

    /**
     * Backlog excessive tool tip.
     */
    public static final String BACKLOG_EXCESSIVE_TOOLTIP = Localization.getLocalText("TTIP_backlog_excessive");
    }
