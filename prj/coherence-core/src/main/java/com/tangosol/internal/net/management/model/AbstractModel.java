/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management.model;

import com.oracle.coherence.common.base.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import javax.management.openmbean.OpenMBeanConstructorInfoSupport;

import java.util.LinkedHashMap;
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
            synchronized (this)
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

                    m_mBeanInfo = new MBeanInfo(this.getClass().getName(),
                                                f_sDescription,
                                                aAttributeInfo,
                                                new OpenMBeanConstructorInfoSupport[0],
                                                aOperation,
                                                new MBeanNotificationInfo[0]);
                    }
                }
            }
        return m_mBeanInfo;
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
    }
