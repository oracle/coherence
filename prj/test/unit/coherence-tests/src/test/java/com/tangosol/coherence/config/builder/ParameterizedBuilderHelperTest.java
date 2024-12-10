/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder.ReflectionSupport;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import junit.framework.Assert;

import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;

import java.awt.geom.Point2D;

/**
 * Unit tests for the {@link ParameterizedBuilderHelper}s
 *
 * @author bo  2012.11.2
 * @since Coherence 12.1.2
 */
public class ParameterizedBuilderHelperTest
    {
    /**
     * Ensure that we can reflect on a {@link ParameterizedBuilder} that
     * implements {@link ReflectionSupport}.
     */
    @Test
    public void testRealizesWithReflectionSupport()
        {
        InstanceBuilder<Point> bldr    = new InstanceBuilder<Point>(Point.class);

        ParameterResolver      resolve = new NullParameterResolver();

        Assert.assertFalse(ParameterizedBuilderHelper.realizes(bldr, Rectangle.class, resolve, null));

        Assert.assertTrue(ParameterizedBuilderHelper.realizes(bldr, Point.class, resolve, null));

        Assert.assertTrue(ParameterizedBuilderHelper.realizes(bldr, Point2D.class, resolve, null));
        }

    /**
     * Ensure that we can reflect on a {@link ParameterizedBuilder} that uses
     * generics.
     */
    @Test
    public void testRealizesWithReifiedTypes()
        {
        StringParameterizedBuilder bldr    = new StringParameterizedBuilder("Gudday");

        ParameterResolver          resolve = new NullParameterResolver();

        Assert.assertFalse(ParameterizedBuilderHelper.realizes(bldr, Rectangle.class, resolve, null));

        Assert.assertTrue(ParameterizedBuilderHelper.realizes(bldr, Object.class, resolve, null));

        Assert.assertTrue(ParameterizedBuilderHelper.realizes(bldr, String.class, resolve, null));
        }

    /**
     * Ensure that realizes returns false for a null builder.
     */
    @Test
    public void testRealizesWithNullBuilder()
        {
        Assert.assertFalse(ParameterizedBuilderHelper.realizes(null, Object.class, new NullParameterResolver(),null));
        }

    /**
     * A simple {@link ParameterizedBuilder} for a {@link String}.
     */
    public static class StringParameterizedBuilder
            implements ParameterizedBuilder<String>
        {
        /**
         * Constructs a {@link StringBuilder}.
         *
         * @param sValue  the value to return when constructing the {@link String}
         */
        public StringParameterizedBuilder(String sValue)
            {
            m_sValue = sValue;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
            {
            throw new UnsupportedOperationException("This method should never be called as we never want to build the String ["
                + m_sValue + "]");
            }

        private String m_sValue;
        }
    }
