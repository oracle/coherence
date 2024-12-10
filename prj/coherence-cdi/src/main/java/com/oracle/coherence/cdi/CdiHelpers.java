/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.CacheName;

import com.tangosol.internal.cdi.MethodKey;

import com.tangosol.net.Coherence;

import java.lang.annotation.Annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.DeploymentException;

/**
 * A helper class providing CDI helper methods.
 */
final class CdiHelpers
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Utility class must not have public constructor.
     */
    private CdiHelpers()
        {
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create key producing function based on method parameters.
     *
     * @param method  the annotated method
     *
     * @return key generator function
     */
    static Function<Object[], Object> cacheKeyFunction(AnnotatedMethod<?> method)
        {
        List<? extends AnnotatedParameter<?>> parameters = method.getParameters();
        List<Integer> keyIndices = new ArrayList<>();
        List<Integer> allIndices = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++)
            {
            AnnotatedParameter<?> annotatedParameter = parameters.get(i);
            if (annotatedParameter.getAnnotation(CacheKey.class) != null)
                {
                keyIndices.add(i);
                }
            if (annotatedParameter.getAnnotation(CacheValue.class) == null)
                {
                allIndices.add(i);
                }
            }
        if (keyIndices.isEmpty())
            {
            // all parameters except for @CacheValue are keys
            keyIndices = allIndices;
            }
        if (keyIndices.size() == 1)
            {
            int index = keyIndices.iterator().next();
            return paramValues -> paramValues[index];
            }

        Integer[] indices = keyIndices.toArray(new Integer[0]);
        return paramValues -> new MethodKey(paramValues, indices);
        }

    /**
     * Extract cache name from the annotated method or class.
     *
     * @param annotatedType  the type potentially annotated with {@link CacheName} annotation
     * @param callable       the method potentially annotated with {@link CacheName} annotation
     *
     * @return a cache name
     */
    static String cacheName(Annotated annotatedType, Annotated callable)
        {
        CacheName found = annotatedType == null
                          ? null
                          : annotatedType.getAnnotation(CacheName.class);

        if (callable != null)
            {
            CacheName cacheName = callable.getAnnotation(CacheName.class);
            if (cacheName != null)
                {
                found = cacheName;
                }
            }

        if (found == null)
            {
            throw new DeploymentException("CacheName must be defined either on type, method/constructor, or field/parameter."
                                          + " Type " + annotatedType
                                          + ", callable: " + callable);
            }
        return found.value();
        }

    /**
     * Returns position of method parameter annotated with specified annotation.
     *
     * @param parameters  the method parameters
     * @param annotation  the annotation to search for
     * @param <T>         the declared type of annotated class
     *
     * @return  annotated parameter position
     */
    static <T> Integer annotatedParameterIndex(List<? extends AnnotatedParameter<? super T>> parameters, Class<? extends Annotation> annotation)
        {
        return parameters.stream()
                .filter(annotatedParameter -> annotatedParameter.isAnnotationPresent(annotation))
                .findFirst()
                .map(AnnotatedParameter::getPosition)
                .orElse(null);
        }

    /**
     * Extract session name from the annotated method or class.
     *
     * @param annotatedType  the target type
     * @param callable       the target method
     *
     * @return  the session name
     */
    static String sessionName(Annotated annotatedType, Annotated callable)
        {
        SessionName sessionName = null;
        if (callable != null)
            {
            sessionName = callable.getAnnotation(SessionName.class);
            }
        if (sessionName == null && annotatedType != null)
            {
            sessionName = annotatedType.getAnnotation(SessionName.class);
            }
        if (sessionName != null)
            {
            return sessionName.value();
            }
        return Coherence.DEFAULT_NAME;
        }
    }
