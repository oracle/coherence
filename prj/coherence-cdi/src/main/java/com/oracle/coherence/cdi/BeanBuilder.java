/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.CDI;

/**
 * Element processor for {@code <cdi:bean>} XML element.
 *
 * @author Aleks Seovic  2019.10.02
 * @since 20.06
 */
public class BeanBuilder
        implements ParameterizedBuilder<Object>,
                   ParameterizedBuilder.ReflectionSupport
    {
    private Expression<String> m_exprBeanName;

    /**
     * Construct {@code BeanBuilder} instance.
     *
     * @param exprBeanName  the name of the CDI bean
     */
    BeanBuilder(String exprBeanName)
        {
        m_exprBeanName = new LiteralExpression<>(exprBeanName);
        }

    // ---- ParameterizedBuilder interface ----------------------------------

    @Override
    public Object realize(ParameterResolver resolver, ClassLoader loader, ParameterList parameterList)
        {
        String beanName = m_exprBeanName.evaluate(resolver);
        Instance<Object> instance = CDI.current().select(NamedLiteral.of(beanName));
        if (instance.isResolvable())
            {
            return instance.get();
            }
        else
            {
            throw new ConfigurationException(String.format("CDI bean [%s] cannot be resolved", beanName),
                                             "Please ensure that a bean with that name exists and can be discovered by CDI.");
            }
        }

    // ---- ParameterizedBuilder.ReflectionSupport interface ----------------

    @Override
    public boolean realizes(Class<?> aClass, ParameterResolver resolver, ClassLoader loader)
        {
        String beanName = m_exprBeanName.evaluate(resolver);
        Instance<?> instance = CDI.current().select(aClass, NamedLiteral.of(beanName));

        return instance.isResolvable();
        }
    }
