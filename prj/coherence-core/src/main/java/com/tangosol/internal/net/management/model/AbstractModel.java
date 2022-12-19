/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management.model;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.internal.net.topic.impl.paged.management.SubscriberModel;
import com.tangosol.net.management.annotation.MetricsScope;
import com.tangosol.net.metrics.MBeanMetric;
import org.glassfish.hk2.utilities.DescriptorBuilder;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.TabularDataSupport;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A base class for an {@link DynamicMBean} models.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public abstract class AbstractModel<M extends AbstractModel<M>>
        implements DynamicMBean
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create an {@link AbstractModel}.
     *
     * @param sDescription  the MBean description.
     */
    protected AbstractModel(String sDescription)
        {
        f_sDescription = sDescription;
        }

    // ----- AbstractModel methods ------------------------------------------

    /**
     * Add an MBean attribute to this model.
     *
     * @param attribute the {@link ModelAttribute attribute} to add
     */
    protected void addAttribute(ModelAttribute<M> attribute)
        {
        f_mapAttribute.put(attribute.getName(), attribute);
        }

    /**
     * Add an MBean operation to this model.
     *
     * @param operation the {@link ModelOperation operation} to add
     */
    protected void addOperation(ModelOperation<M> operation)
        {
        f_mapOperation.put(operation.getName(), operation);
        }

    /**
     * Set the scope name to use for metrics from this MBean.
     *
     * @param scope the scope of the metrics
     */
    protected void setScope(MBeanMetric.Scope scope)
        {
        m_scope = scope == null ? MBeanMetric.Scope.VENDOR : scope;
        }

    // ----- DynamicMBean methods -------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public Object getAttribute(String sAttribute) throws AttributeNotFoundException, MBeanException, ReflectionException
        {
        ModelAttribute<M> attribute = f_mapAttribute.get(sAttribute);
        if (attribute == null)
            {
            throw new IllegalArgumentException("Invalid attribute name: " + sAttribute);
            }

        Function<M, ?> function = attribute.getFunction();
        if (function == null)
            {
            throw new AttributeNotFoundException(sAttribute);
            }

        return function.apply((M) this);
        }

    @Override
    public void setAttribute(Attribute sAttribute) throws AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException
        {
        throw new InvalidAttributeValueException("Attribute " + sAttribute + " is read-only");
        }

    @Override
    public AttributeList getAttributes(String[] asAttribute)
        {
        AttributeList list = new AttributeList();
        for (String sAttribute : asAttribute)
            {
            try
                {
                Object oValue = getAttribute(sAttribute);
                list.add(new Attribute(sAttribute, oValue));
                }
            catch (AttributeNotFoundException | MBeanException | ReflectionException e)
                {
                Logger.err(e);
                }
            }
        return list;
        }

    @Override
    public AttributeList setAttributes(AttributeList attributes)
        {
        AttributeList list = new AttributeList();
        for (Attribute attribute : attributes.asList())
            {
            try
                {
                setAttribute(attribute);
                String sName = attribute.getName();
                list.add(new Attribute(sName, getAttribute(sName)));
                }
            catch (AttributeNotFoundException | InvalidAttributeValueException | MBeanException | ReflectionException e)
                {
                Logger.err(e);
                }
            }
        return list;
        }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(String sOperation, Object[] aoParam, String[] asSignature)
            throws MBeanException, ReflectionException
        {
        ModelOperation<M> operation = f_mapOperation.get(sOperation);
        if (operation == null)
            {
            throw new UnsupportedOperationException("MBean operation " + sOperation + " is not supported");
            }
        return operation.getFunction().apply((M) this, aoParam);
        }

    @Override
    public MBeanInfo getMBeanInfo()
        {
        if (m_mBeanInfo == null)
            {
            MBeanAttributeInfo[] aAttributeInfo = f_mapAttribute.values().stream()
                    .map(ModelAttribute::getMBeanAttributeInfo)
                    .toArray(MBeanAttributeInfo[]::new);

            MBeanOperationInfo[] aOperation = f_mapOperation.values()
                    .stream()
                    .map(ModelOperation::getOperation)
                    .toArray(MBeanOperationInfo[]::new);

            DescriptorSupport descriptor = new DescriptorSupport();
            descriptor.setField(MetricsScope.KEY, m_scope.name());

            m_mBeanInfo = new MBeanInfo(this.getClass().getName(),
                                        f_sDescription,
                                        aAttributeInfo,
                                        new OpenMBeanConstructorInfoSupport[0],
                                        aOperation,
                                        new MBeanNotificationInfo[0],
                                        descriptor);
            }
        return m_mBeanInfo;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Print the model's attributes to the specified {@link PrintStream}.
     *
     * @param out  the {@link PrintStream} to print the attributes to
     */
    @SuppressWarnings("unchecked")
    public void dumpAttributes(PrintStream out)
        {
        for (Map.Entry<String, ModelAttribute<M>> entry : f_mapAttribute.entrySet())
            {
            ModelAttribute<M> attribute = entry.getValue();
            if (attribute instanceof TabularModel)
                {
                ((TabularModel<?, M>) attribute).dumpRows(out, (M) this, entry.getKey());
                }
            else
                {
                Function<M, ?>    function = attribute.getFunction();
                Object            oValue   = function.apply((M) this);
                out.printf("%s %s\n", entry.getKey(), oValue);
                }
            }
        }

    /**
     * If the specified value is {@code null} then the String "n/a"
     * is returned, otherwise the String value is returned.
     *
     * @param o  the value to return the String of
     *
     * @return the String value of the object or "n/a" if the value is {@code null}
     */
    protected String valueOrNotApplicable(Object o)
        {
        return o == null ? "n/a" : String.valueOf(o);
        }

    // ----- data members ---------------------------------------------------

    /**
     * A map of attributes for this MBean, keyed by attribute name.
     */
    protected final LinkedHashMap<String, ModelAttribute<M>> f_mapAttribute = new LinkedHashMap<>();

    /**
     * A map of operations for this MBean, keyed by operation name.
     */
    protected final LinkedHashMap<String, ModelOperation<M>> f_mapOperation = new LinkedHashMap<>();

    /**
     * The MBean description.
     */
    private final String f_sDescription;

    /**
     * The lazily created MBean definition.
     */
    private volatile MBeanInfo m_mBeanInfo;

    /**
     * The scope of the MBean metrics.
     */
    private MBeanMetric.Scope m_scope = MBeanMetric.Scope.VENDOR;
    }
