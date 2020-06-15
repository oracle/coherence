/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management;


import com.tangosol.net.management.annotation.Description;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;


/**
 * AnnotatedStandardMBean is an extension of a {@link StandardMBean} that uses
 * the {@link Description} annotation for describing the attributes, operations
 * and the bean on the designated MBean interface.
 * <p>
 * This class is an extended version of the implementation from Eamon
 * McManus's java.net article
 * <a href="http://weblogs.java.net/blog/emcmanus/archive/2005/07/adding_informat.html">
 * "Adding information to a Standard MBean interface using annotations."</a>
 *
 * @author cf 2010.12.10
 * @since Coherence 3.7
 */
public class AnnotatedStandardMBean
        extends StandardMBean
    {
    // ----- constructors -----------------------------------------------------

    /**
     * Make a DynamicMBean out of the MBean implementation, using the specified
     * mbeanInterface class.
     *
     * @param <T>       the type of the MBean implementation
     * @param impl      the implementation of the MBean
     * @param clzIface  the Management Interface implemented by the MBean's
     *                  implementation. If null, then this object will use
     *                  standard JMX design pattern to determine the management
     *                  interface associated with the given implementation
     *
     * @throws NotCompliantMBeanException if the mbeanInterface does not follow
     *         JMX design patterns for Management Interfaces, or if the
     *         provided implementation does not implement the specified interface
     */
    public <T> AnnotatedStandardMBean(T impl, Class<T> clzIface)
            throws NotCompliantMBeanException
        {
        super(impl, clzIface);
        }

    /**
     * Make a DynamicMBean out of the MBean implementation, using the specified
     * mbeanInterface class.
     *
     * @param <T>       the type of the MBean implementation
     * @param impl      the implementation of the MBean
     * @param clzIface  the Management Interface implemented by the MBean's
     *                  implementation. If null, then this object will use
     *                  standard JMX design pattern to determine the management
     *                  interface associated with the given implementation
     * @param isMXBean  flag indicating whether clzIface represents an MXBean
     *
     * @throws NotCompliantMBeanException if the mbeanInterface does not follow
     *         JMX design patterns for Management Interfaces, or if the
     *         provided implementation does not implement the specified interface
     */
    public <T> AnnotatedStandardMBean(T impl, Class<T> clzIface, boolean isMXBean)
            throws NotCompliantMBeanException
        {
        super(impl, clzIface, isMXBean);
        }


    // ----- StandardMBean methods -------------------------------------------

    /**
     * Retrieve the description for the MBean from the MBean interface annotation.
     *
     * @param info  the {@link MBeanInfo} for the MBean
     *
     * @return the MBean description
     */
    @Override
    protected String getDescription(MBeanInfo info)
        {
        return MBeanHelper.getDescription(getMBeanInterface(), info);
        }

    /**
     * Retrieve a description for the particular {@link MBeanOperationInfo} by
     * finding a {@link Description} annotation on the corresponding operation.
     *
     * @param info  the {@link MBeanOperationInfo}
     *
     * @return the description for an operation
     */
    @Override
    protected String getDescription(MBeanOperationInfo info)
        {
        return MBeanHelper.getDescription(getMBeanInterface(), info);
        }

    /**
     * Retrieve a description for a particular attribute by finding a
     * {@link Description} annotation on the getter method for the attribute.
     * If a description is not found on the getter method, the setter will
     * be checked.
     *
     * @param info  the {@link MBeanAttributeInfo} for the attribute
     *
     * @return the description for an attribute
     */
    @Override
    protected String getDescription(MBeanAttributeInfo info)
        {
        return MBeanHelper.getDescription(getMBeanInterface(), info);
        }

    /**
     * Retrieve the parameter name for the specified parameter by finding a
     * {@link Description} annotation on the operation.
     *
     * @param op      the {@link MBeanOperationInfo} for the op
     * @param param   the {@link MBeanParameterInfo} for the parameter
     * @param iParam  zero-based sequence number of the parameter
     *
     * @return the name to use for the given MBeanParameterInfo.
     */
    @Override
    protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int iParam)
        {
        return MBeanHelper.getParameterName(getMBeanInterface(), op, param, iParam);
        }
    }
