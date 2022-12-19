/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management.model;

import com.tangosol.net.management.annotation.MetricsLabels;
import com.tangosol.net.management.annotation.MetricsScope;
import com.tangosol.net.management.annotation.MetricsTag;
import com.tangosol.net.management.annotation.MetricsValue;

import com.tangosol.net.metrics.MBeanMetric;

import javax.management.MBeanAttributeInfo;

import javax.management.modelmbean.DescriptorSupport;

import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import java.util.Set;

import java.util.function.Function;

/**
 * A simple attribute in an MBean model.
 *
 * @param <M> the type of the parent model
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class SimpleModelAttribute<M>
        implements ModelAttribute<M>
    {
    // ----- constructors -------------------------------------------------------

    /**
     * Create a {@link SimpleModelAttribute}.
     *
     * @param builder  the {@link Builder} to create this attribute from
     */
    private SimpleModelAttribute(Builder<M> builder)
        {
        f_sName        = builder.f_sName;
        f_sDescription = builder.m_sDescription;
        f_type         = builder.f_type;
        f_fReadable    = builder.m_fReadable;
        f_fWritable    = builder.m_fWritable;
        f_function     = builder.m_function;
        f_fMetric      = builder.m_fMetric;
        f_fMetricTag   = builder.m_fMetricTag;
        f_sMetricName  = builder.m_sMetricName;
        f_metricScope  = builder.m_metricScope;
        f_asLabels     = builder.m_asLabels;
        }

    // ----- SubscriberAttribute methods ------------------------------------

    @Override
    public String getName()
        {
        return f_sName;
        }

    @Override
    public OpenType<?> getType()
        {
        return f_type;
        }

    @Override
    public boolean isMetric()
        {
        return f_fMetric;
        }

    @Override
    public MBeanMetric.Scope getMetricScope()
        {
        return f_metricScope;
        }

    @Override
    public MBeanAttributeInfo getMBeanAttributeInfo()
        {
        if (m_attribute == null)
            {
            synchronized (this)
                {
                if (m_attribute == null)
                    {
                    DescriptorSupport descriptor = new DescriptorSupport();
                    String            sName      = f_sMetricName == null || f_sMetricName.isBlank() ? f_sName : f_sMetricName;
                    if (f_fMetric)
                        {
                        descriptor.setField(MetricsScope.KEY, MBeanMetric.Scope.VENDOR.name());
                        descriptor.setField(MetricsValue.DESCRIPTOR_KEY, sName);
                        if (f_asLabels.length != 0)
                            {
                            descriptor.setField(MetricsLabels.DESCRIPTOR_KEY, f_asLabels);
                            }
                        }
                    else if (f_fMetricTag)
                        {
                        descriptor.setField(MetricsTag.DESCRIPTOR_KEY, sName);
                        }

                    m_attribute = new OpenMBeanAttributeInfoSupport(f_sName, f_sDescription, f_type, f_fReadable, f_fWritable, false, descriptor);
                    }
                }
            }
        return m_attribute;
        }

    @Override
    public Function<M, ?> getFunction()
        {
        return f_function;
        }

    @Override
    public String getDescription()
        {
        return f_sDescription;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return this attribute as a {@link Builder}.
     *
     * @param cls  the type of the MBean model
     * @param <T>  the type of the MBean model
     *
     * @return this attribute as a {@link Builder}
     */
    @SuppressWarnings("unchecked")
    public <T> Builder<T> asBuilder(Class<? extends T> cls)
        {
        Builder<T> builder = builder(f_sName, f_type, cls);

        builder.m_sMetricName  = f_sMetricName;
        builder.m_asLabels     = f_asLabels;
        builder.m_fMetric      = f_fMetric;
        builder.m_fMetricTag   = f_fMetricTag;
        builder.m_fReadable    = f_fReadable;
        builder.m_function     = (Function<T, ?>) f_function;
        builder.m_fWritable    = f_fWritable;
        builder.m_metricScope  = f_metricScope;
        builder.m_sDescription = f_sDescription;

        return builder;
        }

    /**
     * Create a {@link SimpleModelAttribute} {@link Builder}.
     *
     * @param sName  the attribute name
     * @param type   the attribute type
     * @param <M>    the model type
     *
     * @return a {@link SimpleModelAttribute} {@link Builder}
     */
    public static <M> Builder<M> builder(String sName, OpenType<?> type, Class<? extends M> cls)
        {
        return new Builder<>(sName, type);
        }

    /**
     * Create an Integer typed {@link SimpleModelAttribute} {@link Builder}.
     *
     * @param sName  the attribute name
     * @param <M>    the model type
     *
     * @return a {@link SimpleModelAttribute} {@link Builder}
     */
    public static <M> Builder<M> intBuilder(String sName, Class<? extends M> cls)
        {
        return builder(sName, SimpleType.INTEGER, cls);
        }

    /**
     * Create a Long typed {@link SimpleModelAttribute} {@link Builder}.
     *
     * @param sName  the attribute name
     * @param <M>    the model type
     *
     * @return a {@link SimpleModelAttribute} {@link Builder}
     */
    public static <M> Builder<M> longBuilder(String sName, Class<? extends M> cls)
        {
        return builder(sName, SimpleType.LONG, cls);
        }

    /**
     * Create a Double typed {@link SimpleModelAttribute} {@link Builder}.
     *
     * @param sName  the attribute name
     * @param <M>    the model type
     *
     * @return a {@link SimpleModelAttribute} {@link Builder}
     */
    public static <M> Builder<M> doubleBuilder(String sName, Class<? extends M> cls)
        {
        return builder(sName, SimpleType.DOUBLE, cls);
        }

    /**
     * Create a Boolean typed {@link SimpleModelAttribute} {@link Builder}.
     *
     * @param sName  the attribute name
     * @param <M>    the model type
     *
     * @return a {@link SimpleModelAttribute} {@link Builder}
     */
    public static <M> Builder<M> booleanBuilder(String sName, Class<? extends M> cls)
        {
        return builder(sName, SimpleType.BOOLEAN, cls);
        }

    /**
     * Create a String typed {@link SimpleModelAttribute} {@link Builder}.
     *
     * @param sName  the attribute name
     * @param <M>    the model type
     *
     * @return a {@link SimpleModelAttribute} {@link Builder}
     */
    public static <M> Builder<M> stringBuilder(String sName, Class<? extends M> cls)
        {
        return builder(sName, SimpleType.STRING, cls);
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder to create a {@link SimpleModelAttribute}.
     *
     * @param <M>  the type of the model
     */
    public static class Builder<M>
        {
        /**
         * Create a builder.
         *
         * @param sName  the attribute name
         * @param type   the attribute type
         */
        protected Builder(String sName, OpenType<?> type)
            {
            f_sName = sName;
            f_type  = type;
            }

        /**
         * Set the attribute description.
         *
         * @param sDescription  the attribute description
         *
         * @return this builder
         */
        public Builder<M> withDescription(String sDescription)
            {
            m_sDescription = sDescription;
            return this;
            }

        /**
         * Set whether the attribute is read-only.
         *
         * @param fReadOnly {@code true} if the attribute is read-only
         *
         * @return this builder
         */
        public Builder<M> readOnly(boolean fReadOnly)
            {
            m_fReadable = true;
            m_fWritable = !fReadOnly;
            return this;
            }

        /**
         * Set whether the attribute is readable.
         *
         * @param fReadable {@code true} if the attribute is readable
         *
         * @return this builder
         */
        public Builder<M> readable(boolean fReadable)
            {
            m_fReadable = fReadable;
            return this;
            }

        /**
         * Set whether the attribute is writeable.
         *
         * @param fWriteable {@code true} if the attribute is writeable
         *
         * @return this builder
         */
        public Builder<M> writeable(boolean fWriteable)
            {
            m_fWritable = fWriteable;
            return this;
            }

        /**
         * Set the {@link Function} to use to obtain the attribute
         * value from the model.
         *
         * @param function  the {@link Function} to use to obtain the attribute
         *                  value from the model
         *
         * @return this builder
         */
        public Builder<M> withFunction(Function<M, ?> function)
            {
            m_function = function;
            return this;
            }

        /**
         * Set the attribute metric labels.
         *
         * @param asLabel  the attribute metric labels
         *
         * @return this builder
         */
        public Builder<M> withMetricLabels(String... asLabel)
            {
            m_fMetric  = true;
            m_asLabels = asLabel;
            return this;
            }

        /**
         * Set the attribute metric labels.
         *
         * @param setLabel  the attribute metric labels
         *
         * @return this builder
         */
        public Builder<M> withMetricLabels(Set<String> setLabel)
            {
            m_fMetric  = true;
            m_asLabels = setLabel == null || setLabel.isEmpty() ? new String[0] : setLabel.toArray(new String[0]);
            return this;
            }

        /**
         * Set the attribute metric scope.
         *
         * @param scope  the attribute metric scope
         *
         * @return this builder
         */
        public Builder<M> withMetricScope(MBeanMetric.Scope scope)
            {
            m_metricScope = scope == null ? MBeanMetric.Scope.VENDOR : scope;
            return this;
            }

        /**
         * Set the attribute metric name.
         *
         * @param sName  the metric name
         *
         * @return this builder
         */
        public Builder<M> metric(String sName)
            {
            m_fMetric     = true;
            m_sMetricName = sName;
            return this;
            }

        /**
         * Set whether the attribute is a metric.
         *
         * @param fMetric  {@code true} if the attribute is a metric
         *
         * @return this builder
         */
        public Builder<M> metric(boolean fMetric)
            {
            m_fMetric = fMetric;
            return this;
            }

        /**
         * Set whether the attribute is a metric tag.
         *
         * @param fTag  {@code true} if the attribute is a metric tag
         *
         * @return this builder
         */
        public Builder<M> metricTag(boolean fTag)
            {
            m_fMetricTag = fTag;
            m_fMetric    = false;
            return this;
            }

        /**
         * Build the {@link SimpleModelAttribute}.
         *
         * @return the {@link SimpleModelAttribute}
         */
        public SimpleModelAttribute<M> build()
            {
            return new SimpleModelAttribute<>(this);
            }

        // ----- data members -----------------------------------------------

        /**
         * The attribute's name.
         */
        private final String f_sName;

        /**
         * The attribute's type
         */
        private final OpenType<?> f_type;

        /**
         * The attribute's description.
         */
        private String m_sDescription;

        /**
         * A flag indicating whether the attribute is readable.
         */
        private boolean m_fReadable = true;

        /**
         * A flag indicating whether the attribute is writable.
         */
        private boolean m_fWritable = false;

        /**
         * The function to execute to obtain the attribute value.
         */
        private Function<M, ?> m_function = m -> null;

        /**
         * A flag indicating whether this is a metrics value.
         */
        private boolean m_fMetric = false;

        /**
         * A flag indicating whether this is a metrics tag.
         */
        private boolean m_fMetricTag = false;

        /**
         * The set of metrics labels.
         */
        private String[] m_asLabels = {};

        /**
         * The metric name
         */
        private String m_sMetricName;

        /**
         * The metric scope
         */
        private MBeanMetric.Scope m_metricScope = MBeanMetric.Scope.VENDOR;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The lazily created {@link MBeanAttributeInfo} attribute definition.
     */
    private volatile MBeanAttributeInfo m_attribute;

    /**
     * The attribute's name.
     */
    private final String f_sName;

    /**
     * The attribute's description.
     */
    private final String f_sDescription;

    /**
     * The attribute's type
     */
    private final OpenType<?> f_type;

    /**
     * A flag indicating whether the attribute is readable.
     */
    private final boolean f_fReadable;

    /**
     * A flag indicating whether the attribute is writable.
     */
    private final boolean f_fWritable;

    /**
     * The function to execute to obtain the attribute value.
     */
    private final Function<M, ?> f_function;

    /**
     * A flag indicating whether this is a metrics value.
     */
    private final boolean f_fMetric;

    /**
     * A flag indicating whether this is a metrics tag.
     */
    private final boolean f_fMetricTag;

    /**
     * The set of additional metric labels
     */
    private final String[] f_asLabels;

    /**
     * The name to use for metrics.
     */
    private final String f_sMetricName;

    /**
     * The metric scope.
     */
    private final MBeanMetric.Scope f_metricScope;
    }
