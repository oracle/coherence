/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.inject;

import com.oracle.coherence.common.schema.util.AsmUtils;

import java.util.Map;
import java.util.ServiceLoader;

/**
 * A {@link Injector} provider used by {@link Injectable}.
 *
 * @author Jonathan Knight  2020.11.19
 * @since 20.12
 */
class InjectorProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Private constructor for utility class.
     */
    private InjectorProvider()
        {
        }

    // ----- InjectorProvider methods ---------------------------------------

    /**
     * Obtain the {@link Injector} to use.
     *
     * @return  the {@link Injector} to use
     */
    public static Injector getInstance()
        {
        return LazyHolder.SINGLETON;
        }

    // ----- helper methods -------------------------------------------------

    static Injector getInjector()
        {
        Injector injector  = INSTANCE;
        int      nPriority = Integer.MIN_VALUE;

        for (Injector instance : ServiceLoader.load(Injector.class))
            {
            Integer nPriorityFromAnno = getPriority(instance);
            if (nPriorityFromAnno != null && nPriorityFromAnno > nPriority)
                {
                injector  = instance;
                nPriority = nPriorityFromAnno;
                }
            else if (nPriority == Integer.MIN_VALUE)
                {
                injector = instance;
                }
            }
        return injector;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Get the value of the {@code Priority} annotation (either {@code javax}
     * or {@code jakarta}) from the provided {@link Injector}.
     *
     * @param instance  the {@link Injector} instance to scan for the
     *                  {@code Priority} annotation
     *
     * @return  the priority of the {@link Injector} or {@code null} if the
     *          {@code Priority} annotation is not found
     *
     * @since 22.06.10
     */
    static protected Integer getPriority(Injector instance)
        {
        Map<String, Object> attributes = AsmUtils.getAnnotationValues(instance.getClass(),
                ANNOTATIONS, ANNOTATION_ATTRIBUTE_ARRAY);

        return (Integer) (attributes.isEmpty() ? null : attributes.get(VALUE_ATTRIBUTE));
        }

    // ----- inner class: LazyHolder ----------------------------------------

    /**
     * A holder for the singleton {@link Injector} instance.
     */
    private static class LazyHolder
        {
        static final Injector SINGLETON = InjectorProvider.getInjector();
        }

    // ----- constants ------------------------------------------------------

    /**
     * A no-op {@link Injector}.
     */
    private static final Injector INSTANCE = (target) -> {};

    /**
     * The array of {@code Priority} annotations to scan for.
     *
     * @since 22.06.10
     */
    private static final String[] ANNOTATIONS =
            {
            "javax.annotation.Priority",
            "jakarta.annotation.Priority"
            };

    /**
     * The {@code value} attribute on either the {@code javax} or
     * {@code jakarta} annotations.
     *
     * @since 22.06.10
     */
    private static final String VALUE_ATTRIBUTE = "value";

    /**
     * The annotation attributes of interest.
     *
     * @since 22.06.10
     */
    private static final String[] ANNOTATION_ATTRIBUTE_ARRAY = { VALUE_ATTRIBUTE };


    }
