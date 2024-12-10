/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management.jmx;


import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Service;

import com.tangosol.net.management.Registry;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

import java.io.ObjectInputStream;

import java.lang.reflect.Method;

import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationListener;
import javax.management.NotificationFilter;
import javax.management.ObjectInstance;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import javax.management.loading.ClassLoaderRepository;


/**
* The WrapperMBeanServer provides grid related optimizations, such
* as the "refresh on query" capability. To enable the wrapper set
* javax.management.builder.initial system property to the builder class name:
* <pre>
*   -Djavax.management.builder.initial=com.tangosol.net.management.jmx.WrapperMBeanServerBuilder
* </pre>
*
* NOTE: This feature requires JMX 1.2 (JDK 1.5) or later.
*
* @see WrapperMBeanServerBuilder
* @since Coherence 3.4
* @author ew 2007.07.18
*/
public class WrapperMBeanServer
    extends    Base
    implements MBeanServer
    {
    /**
    * {@inheritDoc}
    */
    public ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanException, NotCompliantMBeanException
        {
        return getWrappedServer().createMBean(className, name);
        }

    /**
    * {@inheritDoc}
    */
    public ObjectInstance createMBean(String className, ObjectName name,
                                      ObjectName loaderName)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException
        {
        return getWrappedServer().createMBean(className, name, loaderName);
        }

    /**
    * {@inheritDoc}
    */
    public ObjectInstance createMBean(String className, ObjectName name,
                                      Object params[], String signature[])
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanException, NotCompliantMBeanException
        {
        return getWrappedServer().createMBean(className, name, params, signature);
        }

    /**
    * {@inheritDoc}
    */
    public ObjectInstance createMBean(String className, ObjectName name,
                                      ObjectName loaderName, Object params[], String signature[])
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException
        {
        return getWrappedServer().createMBean(className, name, loaderName, params, signature);
        }

    /**
    * {@inheritDoc}
    */
    public ObjectInstance registerMBean(Object object, ObjectName name)
            throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException
        {
        return getWrappedServer().registerMBean(object, name);
        }

    /**
    * {@inheritDoc}
    */
    public void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException
        {
        //To change body of implemented methods use File | Settings | File Templates.
        getWrappedServer().unregisterMBean(name);
        }

    /**
    * {@inheritDoc}
    */
    public ObjectInstance getObjectInstance(ObjectName name)
            throws InstanceNotFoundException
        {
        return getWrappedServer().getObjectInstance(name);
        }

    /**
    * {@inheritDoc}
    */
    public Set queryMBeans(ObjectName name, QueryExp query)
        {
        Set setMBeans = getWrappedServer().queryMBeans(name, query);
        refreshMBeans(setMBeans);
        return setMBeans;
        }

    /**
    * {@inheritDoc}
    */
    public Set queryNames(ObjectName name, QueryExp query)
        {
        Set setNames = getWrappedServer().queryNames(name, query);
        refreshMBeans(setNames);
        return setNames;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isRegistered(ObjectName name)
        {
        return getWrappedServer().isRegistered(name);
        }

    /**
    * {@inheritDoc}
    */
    public Integer getMBeanCount()
        {
        return getWrappedServer().getMBeanCount();
        }

    /**
    * {@inheritDoc}
    */
    public Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException
        {
        return getWrappedServer().getAttribute(name, attribute);
        }

    /**
    * {@inheritDoc}
    */
    public AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException
        {
        return getWrappedServer().getAttributes(name, attributes);
        }

    /**
    * {@inheritDoc}
    */
    public void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
        {
        getWrappedServer().setAttribute(name, attribute);
        }

    /**
    * {@inheritDoc}
    */
    public AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException
        {
        return getWrappedServer().setAttributes(name, attributes);
        }

    /**
    * {@inheritDoc}
    */
    public Object invoke(ObjectName name, String operationName, Object params[],
            String signature[])
            throws InstanceNotFoundException, MBeanException, ReflectionException
        {
        return getWrappedServer().invoke(name, operationName, params, signature);
        }

    /**
    * {@inheritDoc}
    */
    public String getDefaultDomain()
        {
        return getWrappedServer().getDefaultDomain();
        }

    /**
    * {@inheritDoc}
    */
    public String[] getDomains()
        {
        return getWrappedServer().getDomains();
        }

    /**
    * {@inheritDoc}
    */
    public void addNotificationListener(ObjectName name,
            NotificationListener listener, NotificationFilter filter,
            Object handback)
            throws InstanceNotFoundException
        {
        getWrappedServer().addNotificationListener(name, listener, filter, handback);
        }

    /**
    * {@inheritDoc}
    */
    public void addNotificationListener(ObjectName name, ObjectName listener,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException
        {
        getWrappedServer().addNotificationListener(name, listener, filter, handback);
        }

    /**
    * {@inheritDoc}
    */
    public void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException
        {
        getWrappedServer().removeNotificationListener(name, listener);
        }

    /**
    * {@inheritDoc}
    */
    public void removeNotificationListener(ObjectName name, ObjectName listener,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException
        {
        getWrappedServer().removeNotificationListener(name, listener,filter,handback);
        }

    /**
    * {@inheritDoc}
    */
    public void removeNotificationListener(ObjectName name,
            NotificationListener listener)
            throws InstanceNotFoundException, ListenerNotFoundException
        {
        getWrappedServer().removeNotificationListener(name, listener);
        }

    /**
    * {@inheritDoc}
    */
    public void removeNotificationListener(ObjectName name,
            NotificationListener listener, NotificationFilter filter,
            Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException
        {
        getWrappedServer().removeNotificationListener(name, listener, filter, handback);
        }

    /**
    * {@inheritDoc}
    */
    public MBeanInfo getMBeanInfo(ObjectName name)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException
        {
        return getWrappedServer().getMBeanInfo(name);
        }

    /**
    * {@inheritDoc}
    */
    public boolean isInstanceOf(ObjectName name, String className)
            throws InstanceNotFoundException
        {
        return getWrappedServer().isInstanceOf(name, className);
        }

    /**
    * {@inheritDoc}
    */
    public Object instantiate(String className)
            throws ReflectionException, MBeanException
        {
        return getWrappedServer().instantiate(className);
        }

    /**
    * {@inheritDoc}
    */
    public Object instantiate(String className, ObjectName loaderName)
            throws ReflectionException, MBeanException, InstanceNotFoundException
        {
        return getWrappedServer().instantiate(className, loaderName);
        }

    /**
    * {@inheritDoc}
    */
    public Object instantiate(String className, Object params[], String signature[])
            throws ReflectionException, MBeanException
        {
        return getWrappedServer().instantiate(className, params, signature);
        }

    /**
    * {@inheritDoc}
    */
    public Object instantiate(String className, ObjectName loaderName,
            Object params[], String signature[])
            throws ReflectionException, MBeanException, InstanceNotFoundException
        {
        return getWrappedServer().instantiate(className, loaderName, params, signature);
        }

    /**
    * {@inheritDoc}
    */
    public ObjectInputStream deserialize(ObjectName name, byte[] data)
            throws OperationsException
        {
        return getWrappedServer().deserialize(name, data);
        }

    /**
    * {@inheritDoc}
    */
    public ObjectInputStream deserialize(String className, byte[] data)
            throws OperationsException, ReflectionException
        {
        return getWrappedServer().deserialize(className, data);
        }

    /**
    * {@inheritDoc}
    */
    public ObjectInputStream deserialize(String className, ObjectName loaderName,
                                         byte[] data)
            throws OperationsException, ReflectionException
        {
        return getWrappedServer().deserialize(className, loaderName, data);
        }

    /**
    * {@inheritDoc}
    */
   public ClassLoader getClassLoaderFor(ObjectName mbeanName)
            throws InstanceNotFoundException
        {
        return getWrappedServer().getClassLoaderFor(mbeanName);
        }

    /**
    * {@inheritDoc}
    */
    public ClassLoader getClassLoader(ObjectName loaderName)
            throws InstanceNotFoundException
        {
        return getWrappedServer().getClassLoader(loaderName);
        }

    /**
    * {@inheritDoc}
    */
    public ClassLoaderRepository getClassLoaderRepository()
        {
        return getWrappedServer().getClassLoaderRepository();
        }

    /**
    * Refresh all cached data for specified beans.
    *
    * @param setBeans  Set of ObjectNames or ObjectInstances to be refreshed
    */
    protected void refreshMBeans(Set setBeans)
        {
        if (setBeans != null)
            {
            Cluster cluster = CacheFactory.getCluster();
            if (cluster.isRunning())
                {
                Service svcManagement = cluster.getService(Registry.SERVICE_NAME);
                if (svcManagement != null)
                    {
                    Object conn = svcManagement.getUserContext();
                    if (conn != null)
                        {
                        try
                            {
                            METH_REFRESH.invoke(conn, setBeans);
                            }
                        catch (Exception e) // IllegalAccessException, InvocationTargetException
                            {
                            throw ensureRuntimeException(e);
                            }
                        }
                    }
                }
            }
        }

    /**
    * Obtain the wrapped MBeanServer.
    *
    * @return  the wrapped MBeanServer
    */
    public MBeanServer getWrappedServer()
        {
        return m_serverWrapped;
        }

    /**
    * Specify the wrapped MBeanServer.
    *
    * @param serverWrapped  the wrapped (inner) server
    */
    public void setWrappedServer(MBeanServer serverWrapped)
        {
        m_serverWrapped = serverWrapped;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The inner MBeanServer that this MBeanServer wraps.
    */
    private MBeanServer m_serverWrapped;

    /**
    * Connector#refreshRemoteModels method.
    */
    private static Method METH_REFRESH;

    static
        {
        try
            {
            Class   clzConnector = Class.forName(
                    "com.tangosol.coherence.component.net.management.Connector");
            Class[] aclzParam    = new Class[] {Set.class};

            METH_REFRESH = ClassHelper.findMethod(clzConnector,
                    "refreshRemoteModels", aclzParam, false);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }
    }
