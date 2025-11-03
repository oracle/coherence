/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.util;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import java.util.Set;

/**
 * Utility class for programmatic CDI bean lookup and retrieval.
 * <p/>
 * This helper class provides static methods for retrieving CDI managed beans
 * programmatically, which is useful in scenarios where dependency injection
 * is not available or when dynamic bean lookup is required based on runtime
 * configuration.
 * <p/>
 * The class supports two primary lookup strategies:
 * <ul>
 *   <li>Type-based lookup: Retrieving beans by their class type</li>
 *   <li>Name-based lookup: Retrieving named beans with type filtering</li>
 * </ul>
 * <p/>
 * All methods handle CDI bean resolution, ambiguity checking, and proper
 * creation context management to ensure beans are properly initialized
 * and managed by the CDI container.
 * <p/>
 * Usage examples:
 * <pre>
 * // Get bean by type
 * EmbeddingModel embeddingModel = CdiHelper.getBean(EmbeddingModel.class);
 * 
 * // Get named bean with type filtering
 * ModelProvider provider = CdiHelper.getNamedBean(ModelProvider.class, "OpenAI");
 * </pre>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@SuppressWarnings("unchecked")
public class CdiHelper
    {
    /**
     * Retrieves a CDI managed bean by its class type.
     * <p/>
     * This method performs type-based bean lookup using the CDI BeanManager.
     * If multiple beans of the same type exist, CDI's resolution mechanism
     * is used to select the appropriate bean (based on qualifiers, priorities, etc.).
     * <p/>
     * The method handles proper creation context management to ensure the
     * returned bean is fully initialized and managed by the CDI container.
     * 
     * @param <T> the type of the bean to retrieve
     * @param type the Class object representing the bean type
     * 
     * @return the bean instance of the specified type, or null if no bean found
     */
    public static <T> T getBean(Class<T> type)
        {
        BeanManager beanManager = CDI.current().getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(type);
        if (beans == null || beans.isEmpty())
            {
            return null;
            }

        // Resolve the bean (handles ambiguities, etc.)
        Bean<?> bean = beanManager.resolve(beans);
        CreationalContext<?> ctx = beanManager.createCreationalContext(bean);

        return (T) beanManager.getReference(bean, type, ctx);
        }

    /**
     * Retrieves a named CDI managed bean with type filtering.
     * <p/>
     * This method performs name-based bean lookup and then filters the results
     * to ensure the bean is assignable to the specified type. This is particularly
     * useful for retrieving named implementations of interfaces (such as model
     * providers identified by name).
     * <p/>
     * The method includes ambiguity checking to ensure only one bean matches
     * both the name and type criteria, throwing an exception if multiple
     * matching beans are found.
     * 
     * @param <T> the type of the bean to retrieve
     * @param type the Class object representing the expected bean type
     * @param name the name qualifier of the bean to retrieve
     * 
     * @return the named bean instance of the specified type, or null if no matching bean found
     * 
     * @throws IllegalStateException if multiple beans match the name and type criteria
     */
    public static <T> T getNamedBean(Class<T> type, String name)
        {
        BeanManager bm = CDI.current().getBeanManager();
        Set<Bean<?>> beans = bm.getBeans(name);

        if (beans == null || beans.isEmpty())
            {
            return null;
            }

        // Narrow down to beans of expected type
        Bean<?> matchingBean = null;
        for (Bean<?> bean : beans)
            {
            if (type.isAssignableFrom(bean.getBeanClass()))
                {
                if (matchingBean != null)
                    {
                    throw new IllegalStateException("Ambiguous beans found for name '" + name + "' and type " + type.getName());
                    }
                matchingBean = bean;
                }
            }

        if (matchingBean == null)
            {
            return null;
            }

        CreationalContext<?> ctx = bm.createCreationalContext(matchingBean);
        return (T) bm.getReference(matchingBean, type, ctx);
        }
    }
