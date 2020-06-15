/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;

import com.tangosol.util.function.Remote;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.function.Supplier;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link MBeanServerProxy} that wraps a {@link MBeanServer}.
 *
 * @author jk  2019.05.30
 */
public class WrapperMBeanServerProxy
        implements MBeanServerProxy
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link WrapperMBeanServerProxy}.
     *
     * @param server  the {@link MBeanServer} to wrap
     */
    public WrapperMBeanServerProxy(MBeanServer server)
        {
        this(() -> server);
        }

    /**
     * Create a {@link WrapperMBeanServerProxy}.
     *
     * @param supplier  the {@link Supplier} of the {@link MBeanServer} to wrap
     */
    public WrapperMBeanServerProxy(Supplier<MBeanServer> supplier)
        {
        f_supplier = Objects.requireNonNull(supplier);
        }

    // ----- MBeanServerProxy methods ---------------------------------------

    @Override
    public MBeanInfo getMBeanInfo(String sName)
        {
        try
            {
            return getServer().getMBeanInfo(new ObjectName(sName));
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    @Override
    public <R> R execute(Remote.Function<MBeanServer, R> function)
        {
        return function.apply(getServer());
        }

    @Override
    public Map<String, Object> getAttributes(String sName, Filter<String> filter)
        {
        try
            {
            MBeanInfo info = getMBeanInfo(sName);

            String[] attributes = Arrays.stream(info.getAttributes())
                    .filter(a -> evaluateAttributeName(a.getName(), filter))
                    .map(MBeanAttributeInfo::getName)
                    .distinct()
                    .toArray(String[]::new);

            MBeanServer         server         = getServer();
            AttributeList       listAttributes = server.getAttributes(new ObjectName(sName), attributes);
            Map<String, Object> mapValues;

            if (listAttributes != null)
                {
                mapValues = listAttributes.asList()
                                          .stream()
                                          .filter(a -> a.getValue() != null)
                                         .collect(Collectors.toMap(Attribute::getName, Attribute::getValue));
                }
            else
                {
                mapValues = null;
                }

            return mapValues;
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    @Override
    public Object getAttribute(String sName, String sAttr)
        {
        try
            {
            return getServer().getAttribute(new ObjectName(sName), sAttr);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    @Override
    public void setAttribute(String sName, String sAttr, Object oValue)
        {
        try
            {
            getServer().setAttribute(new ObjectName(sName), new Attribute(sAttr, oValue));
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    @Override
    public Object invoke(String sName, String sOpName, Object[] aoParams, String[] asSignature)
        {
        try
            {
            return getServer().invoke(new ObjectName(sName), sOpName, aoParams, asSignature);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    @Override
    public Set<String> queryNames(String sPattern, Filter<ObjectName> filter)
        {
        try
            {
            return queryNames(new ObjectName(sPattern), filter);
            }
        catch (MalformedObjectNameException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    @Override
    public Set<String> queryNames(ObjectName pattern, Filter<ObjectName> filter)
        {
        try
            {
            MBeanServer     server         = getServer();
            Set<ObjectName> setObjectNames = server.queryNames(pattern, null);
            Set<String>     setNames;

            if (setObjectNames != null)
                {
                Stream<ObjectName> stream = setObjectNames.stream();

                if (filter != null)
                    {
                    stream = stream.filter(filter::evaluate);
                    }

                setNames = stream.map(ObjectName::toString)
                                 .collect(Collectors.toSet());
                }
            else
                {
                setNames = null;
                }

            return setNames;
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    @Override
    public boolean isMBeanRegistered(String sName)
        {
        try
            {
            return getServer().isRegistered(new ObjectName(sName));
            }
        catch (MalformedObjectNameException e)
            {
            throw new RuntimeException(e);
            }
        }

    @Override
    public MBeanServerProxy local()
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public void addNotificationListener(String sName, NotificationListener listener, NotificationFilter filter, Object oHandback)
        {
        try
            {
            getServer().addNotificationListener(new ObjectName(sName), listener, filter, oHandback);
            }
        catch (MalformedObjectNameException | InstanceNotFoundException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    @Override
    public void removeNotificationListener(String sName, NotificationListener listener)
        {
        try
            {
            getServer().removeNotificationListener(new ObjectName(sName), listener);
            }
        catch (MalformedObjectNameException | InstanceNotFoundException | ListenerNotFoundException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    @Override
    public void removeNotificationListener(String sName, NotificationListener listener, NotificationFilter filter, Object oHandback)
        {
        try
            {
            getServer().removeNotificationListener(new ObjectName(sName), listener, filter, oHandback);
            }
        catch (MalformedObjectNameException | InstanceNotFoundException | ListenerNotFoundException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    // ----- helper methods -------------------------------------------------

    private MBeanServer getServer()
        {
        return f_supplier.get();
        }

    private boolean evaluateAttributeName(String sName, Filter<String> filter)
        {
        return filter == null
                || filter.evaluate(sName)
                || filter.evaluate(sName.toUpperCase());
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link MBeanServer}.
     */
    private final Supplier<MBeanServer> f_supplier;
    }
