/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management.model;

import com.oracle.coherence.common.base.Exceptions;

import com.tangosol.net.management.OpenMBeanHelper;

import com.tangosol.net.management.annotation.MetricsLabels;
import com.tangosol.net.management.annotation.MetricsScope;
import com.tangosol.net.management.annotation.MetricsValue;

import com.tangosol.net.metrics.MBeanMetric;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;

import javax.management.modelmbean.DescriptorSupport;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import java.io.PrintStream;

import java.util.Arrays;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A base class for tabular attributes in an {@link AbstractModel MBean model}.
 *
 * @param <R> the type of the model representing rows in the table
 * @param <M> the type of the parent model
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public abstract class TabularModel<R, M>
        implements ModelAttribute<M>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a tabular model.
     * <p>
     * The {@code sKeyName} attribute is the name of the attribute used to
     * uniquely identify a row in the table. The name must match one of the
     * attributes in the {@code aAttribute} array.
     *
     * @param sName          the attribute name
     * @param sDescription   the attribute description
     * @param sKeyName       the name of the table row key attribute
     * @param aAttribute     the attributes making up the table row
     * @param fnRowCount     the function to obtain the row count from the parent model
     * @param fnRow          the function to obtain a specific row model from the parent model
     */
    protected TabularModel(String                    sName,
                           String                    sDescription,
                           String                    sKeyName,
                           ModelAttribute<R>[]       aAttribute,
                           Function<M, Integer>      fnRowCount,
                           BiFunction<M, Integer, R> fnRow)
        {
        int cAttribute = aAttribute.length;

        f_sName            = sName;
        f_sDescription     = sDescription;
        f_aAttribute       = aAttribute;
        f_asAttributeNames = new String[cAttribute];
        f_fnRowCount       = fnRowCount;
        f_fnRow            = fnRow;

        String[]          asDescr     = new String[cAttribute];
        OpenType<?>[]     aoType      = new OpenType[cAttribute];
        boolean           fHasMetrics = false;
        MBeanMetric.Scope scope       = MBeanMetric.Scope.VENDOR;
        boolean           fKeyFound   = false;

        for (int i = 0; i < cAttribute; i++)
            {
            ModelAttribute<R> attribute      = aAttribute[i];
            String            sAttributeName = attribute.getName();

            f_asAttributeNames[i] = sAttributeName;

            if (attribute.isMetric())
                {
                fHasMetrics  = true;
                scope = attribute.getMetricScope();
                }
            asDescr[i]  = sAttributeName;
            aoType[i]   = attribute.getType();
            fKeyFound   = fKeyFound || sAttributeName.equals(sKeyName);
            }

        if (!fKeyFound)
            {
            throw new IllegalArgumentException("The key attribute \"" + sKeyName
                    + "\" could not be found in the attributes array " + Arrays.toString(f_asAttributeNames));
            }

        f_fHasMetrics = fHasMetrics;
        f_metricScope = scope;
        f_rowType     = OpenMBeanHelper.createCompositeType(sKeyName, sDescription, f_asAttributeNames, asDescr, aoType);
        f_tableType   = OpenMBeanHelper.createTabularType(sKeyName, sDescription, f_rowType, new String[] {sKeyName});
        }

    // ----- ModelAttribute methods -----------------------------------------

    @Override
    public String getName()
        {
        return f_sName;
        }

    @Override
    public String getDescription()
        {
        return f_sDescription;
        }

    @Override
    public Function<M, ?> getFunction()
        {
        return this::getTabularData;
        }

    @Override
    public TabularType getType()
        {
        return f_tableType;
        }

    @Override
    public MBeanAttributeInfo getMBeanAttributeInfo()
        {
        MBeanAttributeInfo info = m_attribute;
        if (info == null)
            {
            DescriptorSupport descriptor = new DescriptorSupport();
            descriptor.setField(MetricsScope.KEY, f_metricScope.name());

            if (f_fHasMetrics)
                {
                descriptor.setField(MetricsScope.KEY, MBeanMetric.Scope.VENDOR.name());
                descriptor.setField(MetricsValue.DESCRIPTOR_KEY, f_sName);

                for (ModelAttribute<?> attribute : f_aAttribute)
                    {
                    if (attribute.isMetric())
                        {
                        String     sName = attribute.getName();
                        Descriptor desc  = attribute.getMBeanAttributeInfo().getDescriptor();
                        descriptor.setField(sName + '.' + MetricsScope.KEY, desc.getFieldValue(MetricsScope.KEY));
                        descriptor.setField(sName + '.' + MetricsValue.DESCRIPTOR_KEY, desc.getFieldValue(MetricsValue.DESCRIPTOR_KEY));
                        descriptor.setField(sName + '.' + MetricsLabels.DESCRIPTOR_KEY, desc.getFieldValue(MetricsLabels.DESCRIPTOR_KEY));
                        }
                    }

                String[] asMetricNames = Arrays.stream(f_aAttribute)
                        .filter(ModelAttribute::isMetric)
                        .map(ModelAttribute::getName)
                        .toArray(String[]::new);

                descriptor.setField("metrics.columns", asMetricNames);
                }

            info = m_attribute = new OpenMBeanAttributeInfoSupport(f_sName, f_sDescription, f_tableType, true, false, false, descriptor);
            }
        return info;
        }

    @Override
    public boolean isMetric()
        {
        return f_fHasMetrics;
        }

    @Override
    public MBeanMetric.Scope getMetricScope()
        {
        return f_metricScope;
        }

    // ----- TabularModel methods -------------------------------------------

    /**
     * Returns a {@link TabularData table} of all the channel attributes for all channels.
     *
     * @return a {@link TabularData table} of all the channel attributes for all channels
     */
    public TabularData getTabularData(M m)
        {
        try
            {
            TabularDataSupport table      = new TabularDataSupport(f_tableType);
            int                cRow       = f_fnRowCount.apply(m);
            CompositeData[]    rows       = new CompositeData[cRow];
            int                cAttribute = f_aAttribute.length;

            for (int nRow = 0; nRow < cRow; nRow++)
                {
                Object[] aoValue = new Object[cAttribute];
                for (int a = 0; a < cAttribute; a++)
                    {
                    R oRow = f_fnRow.apply(m, nRow);
                    aoValue[a] = f_aAttribute[a].getFunction().apply(oRow);
                    }
                rows[nRow] = new CompositeDataSupport(f_rowType, f_asAttributeNames, aoValue);
                }
            table.putAll(rows);

            return table;
            }
        catch (OpenDataException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Return the row type.
     *
     * @return the row type
     */
    public CompositeType getRowType()
        {
        return f_rowType;
        }

    /**
     * Return the attribute names.
     *
     * @return the attribute names
     */
    public String[] getAttributeNames()
        {
        return f_asAttributeNames;
        }

    /**
     * Print the model's attributes to the specified {@link PrintStream}.
     *
     * @param out      the {@link PrintStream} to print the attributes to
     * @param m        the parent model
     * @param sPrefix  the prefix to print at the start of the data
     */
    public void dumpRows(PrintStream out, M m, String sPrefix)
        {
        int cRow = f_fnRowCount.apply(m);
        for (int nRow = 0; nRow < cRow; nRow++)
            {
            StringBuilder sb = new StringBuilder(sPrefix)
                    .append(" row=")
                    .append(nRow);

            for (ModelAttribute<R> attribute : f_aAttribute)
                {
                R      oRow   = f_fnRow.apply(m, nRow);
                Object oValue = attribute.getFunction().apply(oRow);
                sb.append(", ").append(attribute.getName()).append("=").append(oValue);
                }
            out.println(sb);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * That attribute name.
     */
    private final String f_sName;

    /**
     * The attribute description.
     */
    private final String f_sDescription;

    /**
     * The table row attributes.
     */
    private final ModelAttribute<R>[] f_aAttribute;

    /**
     * The attrbute names.
     */
    private final String[] f_asAttributeNames;

    /**
     * The function to obtain the row count from the parent model.
     */
    private final Function<M, Integer> f_fnRowCount;

    /**
     * The function to obtain a row from the parent model
     */
    private final BiFunction<M, Integer, R> f_fnRow;

    /**
     * The data types in a row.
     */
    private final TabularType f_tableType;

    /**
     * The composite row type.
     */
    private final CompositeType f_rowType;

    /**
     * A flag indicating whether this table contains metrics attributes.
     */
    private final boolean f_fHasMetrics;

    /**
     * The scope for any metrics in the table.
     */
    private final MBeanMetric.Scope f_metricScope;

    /**
     * The lazily created {@link MBeanAttributeInfo} attribute definition.
     */
    private volatile MBeanAttributeInfo m_attribute;
    }
