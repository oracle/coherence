/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;

import javax.enterprise.inject.Instance;

import javax.enterprise.inject.literal.NamedLiteral;

import javax.enterprise.inject.spi.CDI;

import java.util.Objects;

/**
 * Element processor for {@code <cdi:bean>} XML element.
 *
 * @author Aleks Seovic  2019.10.02
 * @since 20.06
 */
@SuppressWarnings("deprecation")
public class BeanBuilder
        implements ParameterizedBuilder<Object>,
                   ParameterizedBuilder.ReflectionSupport
    {
    /**
     * Construct {@code BeanBuilder} instance.
     *
     * @param exprBeanName  the name of the CDI bean
     */
    BeanBuilder(CDI<Object> cdi, String exprBeanName)
        {
        f_cdi          = Objects.requireNonNull(cdi);
        f_exprBeanName = new LiteralExpression<>(exprBeanName);
        }

    // ---- ParameterizedBuilder interface ----------------------------------

    @Override
    public Object realize(ParameterResolver resolver, ClassLoader loader, ParameterList parameterList)
        {
        String beanName = f_exprBeanName.evaluate(resolver);
        Instance<Object> instance = f_cdi.select(NamedLiteral.of(beanName));
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
        String      beanName = f_exprBeanName.evaluate(resolver);
        Instance<?> instance = f_cdi.select(aClass, NamedLiteral.of(beanName));

        return instance.isResolvable();
        }

    // ----- data members ---------------------------------------------------

    private final Expression<String> f_exprBeanName;

    private final CDI<Object> f_cdi;
    }
