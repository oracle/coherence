/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.config;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.pof.PortableObject;

import jakarta.json.bind.annotation.JsonbTransient;
import java.beans.Introspector;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base class for configuration objects used in Coherence RAG.
 * <p/>
 * Supports copying readable properties to target objects via reflection.
 * Handles both JavaBean-style (get/set/is) and fluent-style properties.
 *
 * @param <T> the type of target object this configuration is for
 *
 * @author Aleks Seovic  2025.06.28
 * @since 25.09
 */
public abstract class AbstractConfig<T>
        extends AbstractEvolvable
        implements PortableObject
    {
    @JsonbTransient
    public int getDataVersion()
        {
        return super.getDataVersion();
        }

    @JsonbTransient
    public int getImplVersion()
        {
        return 0;
        }

    /**
     * Applies all readable properties from this config instance to the given target object.
     * <p/>
     * This supports both JavaBean-style and fluent-style setter methods on the target.
     *
     * @param target the object to apply configuration values to (e.g., a POJO or Builder)
     */
    public void apply(T target)
        {
        Map<String, Method> mapSourceProperties = findReadableProperties(this.getClass());
        Map<String, Method> mapTargetProperties = findWritableProperties(target.getClass());

        for (Map.Entry<String, Method> entry : mapSourceProperties.entrySet())
            {
            String sName  = entry.getKey();
            Method getter = entry.getValue();
            Method setter = mapTargetProperties.get(sName);

            if (setter != null)
                {
                try
                    {
                    Object oValue = getter.invoke(this);
                    setter.invoke(target, oValue);
                    }
                catch (Exception e)
                    {
                    throw new RuntimeException("Failed to set property: " + sName, e);
                    }
                }
            else
                {
                Logger.warn("Source property '%s' has no matching property in a target class %s"
                                    .formatted(sName, target.getClass().getName()));
                }
            }
        }

    /**
     * Finds all readable properties (JavaBean-style and record-style) on the given class.
     *
     * @param clazz the class to inspect
     *
     * @return a map of property names to getter methods
     */
    private Map<String, Method> findReadableProperties(Class<?> clazz)
        {
        Map<String, Method> getters = new HashMap<>();
        for (Method method : clazz.getMethods())
            {
            if (method.getParameterCount() != 0 || EXCLUDED_METHODS.contains(method.getName()))
                {
                continue;
                }

            String sName = method.getName();
            Class<?> returnType = method.getReturnType();

            if (sName.startsWith("get") && sName.length() > 3)
                {
                String sProp = Introspector.decapitalize(sName.substring(3));
                getters.put(sProp, method);
                }
            else if (sName.startsWith("is") && sName.length() > 2 && returnType == boolean.class)
                {
                String sProp = Introspector.decapitalize(sName.substring(2));
                getters.put(sProp, method);
                }
            else
                {
                // record-style accessor
                getters.put(sName, method);
                }
            }
        return getters;
        }

    /**
     * Finds all writable properties (JavaBean-style setters and fluent-style setters) on the given class.
     *
     * @param clazz the class to inspect
     *
     * @return a map of property names to setter methods
     */
    private Map<String, Method> findWritableProperties(Class<?> clazz)
        {
        Map<String, Method> setters = new HashMap<>();
        for (Method method : clazz.getMethods())
            {
            if (method.getParameterCount() != 1 || EXCLUDED_METHODS.contains(method.getName()))
                {
                continue;
                }

            String sName = method.getName();

            // JavaBean-style: setFoo
            if (sName.startsWith("set") && sName.length() > 3)
                {
                String sProp = Introspector.decapitalize(sName.substring(3));
                setters.put(sProp, method);
                }
            else
                {
                // Fluent-style: foo(value)
                setters.put(sName, method);
                }
            }
        return setters;
        }

    /**
     * A set of method names that should be excluded from reflective property introspection.
     */
    private static final Set<String> EXCLUDED_METHODS;

    static
        {
        Set<String> set = new HashSet<>();

        // Object methods
        set.add("getClass");
        set.add("equals");
        set.add("hashCode");
        set.add("toString");

        // AbstractEvolvable methods
        for (Method method : AbstractEvolvable.class.getMethods())
            {
            set.add(method.getName());
            }

        // PortableObject methods
        for (Method method : PortableObject.class.getMethods())
            {
            set.add(method.getName());
            }

        EXCLUDED_METHODS = Collections.unmodifiableSet(set);
        }
    }
