/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.injection;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.util.ResourceResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A simple implementation of an {@link Injector} that resolves and
 * appropriately calls setter methods annotated with {@link Injectable} with
 * required values.
 *
 * @author bo 2012.09.17
 * @since Coherence 12.1.2
 */
public class SimpleInjector
        implements Injector
    {

    // ----- Injector methods -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T inject(T object, ResourceResolver resolver)
        {
        if (object != null && resolver != null)
            {
            Class<?> clsObject = object.getClass();

            for (Method method : clsObject.getMethods())
                {
                int modifiers = method.getModifiers();

                if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers) && !Modifier.isAbstract(modifiers)
                    && method.getParameterTypes().length == 1)
                    {
                    Class<?>   clsResource = method.getParameterTypes()[0];

                    Injectable injectable  = method.getAnnotation(Injectable.class);

                    if (injectable != null)
                        {
                        String sResourceName = injectable.value().trim();

                        Object oResource;

                        if (sResourceName.isEmpty())
                            {
                            oResource = resolver.getResource(clsResource);
                            }
                        else
                            {
                            try
                                {
                                oResource = resolver.getResource(clsResource, sResourceName);
                                }
                            catch (Exception e)
                                {
                                oResource = null;
                                Logger.warn(String.format(
                                        "Failed to lookup resource %s resource-type %s for method %s due to %s",
                                        sResourceName, clsResource.getCanonicalName(),
                                        method, e));
                                }
                            }

                        // null resources are not injectable
                        if (oResource != null)
                            {
                            try
                                {
                                method.invoke(object, oResource);
                                }
                            catch (Exception e)
                                {
                                Logger.warn(String.format(
                                    "Failed to inject resource %s into %s using method %s due to %s", oResource, object,
                                    method, e));
                                }
                            }
                        }
                    }
                }
            }

        return object;
        }
    }
