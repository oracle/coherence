/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management;


import com.tangosol.net.management.annotation.Description;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.StandardEmitterMBean;


/**
 * AnnotatedStandardEmitterMBean is an extension of a {@link StandardEmitterMBean}
 * that uses the {@link Description} and {@link
 * com.tangosol.net.management.annotation.Notification Notification} annotations
 * to describe the MBean and any attributes, operations and notifications it
 * declares.
 * <p>
 * The implementation of this class is basically identical to the
 * {@link AnnotatedStandardMBean} class.
 *
 * @since Coherence 12.1.2
 */
public class AnnotatedStandardEmitterMBean
        extends StandardEmitterMBean
    {
    // ----- constructors ---------------------------------------------------

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
    public <T> AnnotatedStandardEmitterMBean(T impl, Class<T> clzIface)
            throws NotCompliantMBeanException
        {
        super(impl, clzIface, new SilentEmitter(clzIface));
        }


    // ----- StandardMBean methods ------------------------------------------

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


    // ----- NotificationBroadcaster interface ------------------------------

    @Override
    public MBeanNotificationInfo[] getNotificationInfo()
        {
        return MBeanHelper.getNotificationInfo(getMBeanInterface());
        }


    // ----- inner class: SilentEmitter -------------------------------------

    /**
     * A silent {@link NotificationEmitter} implementation for all NotificationEmitter
     * methods except {@link NotificationEmitter#getNotificationInfo()
     * getNotificationInfo()}.
     */
    protected static class SilentEmitter
            implements NotificationEmitter
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a SilentEmitter.
         *
         * @param clzMBean  the MBean class
         */
        public SilentEmitter(Class clzMBean)
            {
            f_clzMBean = clzMBean;
            }

        // ----- NotificationEmitter interface ------------------------------

        @Override
        public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
                throws ListenerNotFoundException
            {
            }

        @Override
        public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
                throws IllegalArgumentException
            {
            }

        @Override
        public void removeNotificationListener(NotificationListener listener)
                throws ListenerNotFoundException
            {
            }

        @Override
        public MBeanNotificationInfo[] getNotificationInfo()
            {
            return MBeanHelper.getNotificationInfo(f_clzMBean);
            }

        // ----- data members -----------------------------------------------

        /**
         * The MBean interface class this emitter provides notification info
         * for.
         */
        protected final Class f_clzMBean;
        }
    }