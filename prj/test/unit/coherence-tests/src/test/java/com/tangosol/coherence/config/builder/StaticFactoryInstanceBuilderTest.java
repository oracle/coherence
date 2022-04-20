/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;

import com.tangosol.config.TestSerializableHelper;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import java.io.Serializable;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link StaticFactoryInstanceBuilderTest} contains unit tests for {@link StaticFactoryInstanceBuilder}s.
 *
 * @author pfm  2012.06.07
 */
public class StaticFactoryInstanceBuilderTest
    {
    /**
     * Test the realizes method works.
     */
    @Test
    public void testRealizes()
        {
        ParameterResolver resolver = new NullParameterResolver();
        ClassLoader       loader   = null;

        StaticFactoryInstanceBuilder<Point> bldr = new StaticFactoryInstanceBuilder<Point>();
        bldr.setFactoryClassName(new LiteralExpression<String>(Factory.class.getName()));
        bldr.setFactoryMethodName(new LiteralExpression<String>("instantiate"));

        // test realizes
        assertTrue(bldr.realizes(Point.class, resolver, loader));
        assertTrue(bldr.realizes(Point2D.class, resolver, loader));
        assertTrue(bldr.realizes(Serializable.class, resolver, loader));
        assertFalse(bldr.realizes(Rectangle.class, resolver, loader));
        }

    /**
     * Ensure that we can realize a classes using a Factory.
     */
    @Test
    public void testRealize()
        {
        ParameterResolver                   resolver = new NullParameterResolver();
        ClassLoader                         loader   = null;
        ParameterList                       params   = null;
        Point                               point    = null;

        StaticFactoryInstanceBuilder<Point> bldr     = new StaticFactoryInstanceBuilder<Point>();
        bldr.setFactoryClassName(new LiteralExpression<String>(Factory.class.getName()));
        bldr.setFactoryMethodName(new LiteralExpression<String>("instantiate"));

        // test realize with no parameters
        point = bldr.realize(resolver, loader, params);
        assertEquals(0, point.x);
        assertEquals(0, point.y);

        // test realize with X,Y parameters
        params = new ResolvableParameterList();
        params.add(new Parameter("x", Integer.valueOf(1)));
        params.add(new Parameter("y", Integer.valueOf(2)));
        point = bldr.realize(resolver, loader, params);
        assertEquals(1, point.x);
        assertEquals(2, point.y);
        }

    /**
     * Ensure that null parameters can be used.
     */
    @Test
    public void testNullParams()
        {
        ParameterResolver                   resolver = new NullParameterResolver();

        StaticFactoryInstanceBuilder<Point> bldr     = new StaticFactoryInstanceBuilder<Point>();
        bldr.setFactoryClassName(new LiteralExpression<String>(Factory.class.getName()));
        bldr.setFactoryMethodName(new LiteralExpression<String>("instantiate"));

        // test realize with no parameters
        Point point = bldr.realize(resolver, null, null);
        assertEquals(0, point.x);
        assertEquals(0, point.y);
        }

    /**
     * Test basic get/set.
     */
    @Test
    public void testGetSet()
        {
        validate(createAndPopulate());
        }

    /**
     * Test POF serialization.
     */
    @Test
    public void testPof()
        {
        validate(TestSerializableHelper.<StaticFactoryInstanceBuilder<Point>>convertPof(createAndPopulate()));
        }

    /**
     * Test ExternalizableLite serialization.
     */
    @Test
    public void testExternalizableLite()
        {
        validate(TestSerializableHelper.<StaticFactoryInstanceBuilder<Point>>convertEL(createAndPopulate()));
        }

    // ----- helpers  -------------------------------------------------------

    /*
     * Create and populate the builder.
     *
     * @return the populated  builder
     */
    protected StaticFactoryInstanceBuilder<Point> createAndPopulate()
        {
        StaticFactoryInstanceBuilder<Point> bldr = new StaticFactoryInstanceBuilder<Point>();
        bldr.setFactoryClassName(new LiteralExpression<String>(Factory.class.getName()));
        bldr.setFactoryMethodName(new LiteralExpression<String>("instantiate"));

        return bldr;
        }

    /*
    * Validate the builder.
    *
    * @param  list the populated builder
    */
    protected void validate(StaticFactoryInstanceBuilder<Point> bldr)
        {
        ParameterResolver resolver = new NullParameterResolver();
        ClassLoader       loader   = null;
        ParameterList     params   = null;

        // test realizes
        assertTrue(bldr.realizes(Point.class, resolver, loader));
        assertTrue(bldr.realizes(Point2D.class, resolver, loader));
        assertTrue(bldr.realizes(Serializable.class, resolver, loader));
        assertFalse(bldr.realizes(Rectangle.class, resolver, loader));

        // test realize
        Object obj = bldr.realize(resolver, loader, params);
        assertTrue(Point.class.isAssignableFrom(obj.getClass()));
        assertTrue(Serializable.class.isAssignableFrom(obj.getClass()));
        }

    // ----- inner classes --------------------------------------------------

    /**
     * The Factory inner class creates Point objects.
     */
    public static class Factory
        {
        public static Point instantiate()
            {
            return new Point();
            }

        public static Point instantiate(int x, int y)
            {
            return new Point(x,y);
            }
        }
    }
