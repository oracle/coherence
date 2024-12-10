/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ParameterMacroExpression;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.SimpleParameterList;

import com.tangosol.config.TestSerializableHelper;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import org.junit.Assert;

import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;

import java.io.Serializable;

/**
 * {@link InstanceBuilderTest} contains unit tests for {@link InstanceBuilder}s.
 *
 * @author bo  2012.02.03
 */
public class InstanceBuilderTest
    {
    /**
     * Ensure that we can create a {@link InstanceBuilder} using a {@link String} and an {@link Expression}.
     */
    @Test
    public void testBuilderConstruction()
        {
        ParameterResolver resolver = new NullParameterResolver();
        ClassLoader       loader   = null;

        InstanceBuilder<Point> bldr = new InstanceBuilder<Point>("java.awt.Point");

        Assert.assertTrue(bldr.realizes(Point.class, resolver, loader));

        bldr = new InstanceBuilder<Point>(new LiteralExpression<String>("java.awt.Point"));
        Point pnt = bldr.realize(resolver, loader, null);

        Assert.assertTrue(bldr.realizes(Point.class, resolver, loader));
        }

    /**
     * Ensure that we can check the type a {@link InstanceBuilder} will realize works as expected for classes,
     * including inheritance.
     */
    @Test
    public void testRealizes()
        {
        InstanceBuilder<Point> bldr     = new InstanceBuilder<Point>(Point.class);

        ParameterResolver      resolver = new NullParameterResolver();
        ClassLoader            loader   = null;

        Assert.assertTrue(bldr.realizes(Point.class, resolver, loader));
        Assert.assertTrue(bldr.realizes(Serializable.class, resolver, loader));
        Assert.assertFalse(bldr.realizes(Rectangle.class, resolver, loader));
        }

    /**
     * Ensure that we can realize a class with a default no args constructor.
     */
    @Test
    public void testRealizeWithNoArgsConstructor()
        {
        InstanceBuilder<String> bldr       = new InstanceBuilder<String>(String.class);

        ParameterResolver       resolver   = new NullParameterResolver();
        ClassLoader             loader     = null;
        ParameterList           parameters = null;

        Assert.assertEquals("", bldr.realize(resolver, loader, parameters));
        }

    /**
     * Ensure that we can realize a class using the realize specified parameters.
     */
    @Test
    public void testRealizeUsingSpecifiedParameters()
        {
        InstanceBuilder<Point> bldr       = new InstanceBuilder<Point>(Point.class);

        ParameterResolver      resolver   = new NullParameterResolver();
        ClassLoader            loader     = null;
        ParameterList          parameters = new SimpleParameterList(1, 1);

        Assert.assertEquals(new Point(1, 1), bldr.realize(resolver, loader, parameters));
        }

    /**
     * Ensure that we can realize a class using the constructor parameters.
     */
    @Test
    public void testRealizeUsingConstructorParameters()
        {
        InstanceBuilder<Point> bldr       = new InstanceBuilder<Point>(Point.class, 1, 1);

        ParameterResolver      resolver   = new NullParameterResolver();
        ClassLoader            loader     = null;
        ParameterList          parameters = null;

        Assert.assertEquals(new Point(1, 1), bldr.realize(resolver, loader, parameters));
        }

    /**
     * Ensure that we fail using the incorrect number of parameters.
     */
    @Test(expected = Exception.class)
    public void testRealizeUsingIncorrectParameterCount()
        {
        InstanceBuilder<Point> bldr       = new InstanceBuilder<Point>(Point.class);

        ParameterResolver      resolver   = new NullParameterResolver();
        ClassLoader            loader     = null;
        ParameterList          parameters = new SimpleParameterList(1234, "cat", "mouse", "dog", false);

        bldr.realize(resolver, loader, parameters);
        }

    /**
     * Ensure that we can realize a class using the parameterized constructor parameters.
     */
    @Test
    public void testRealizeUsingParameterizedParameters()
        {
        InstanceBuilder<String> bldr     = new InstanceBuilder<String>(String.class);

        ResolvableParameterList resolver = new ResolvableParameterList();

        resolver.add(new Parameter("greeting", "hello world"));

        ClassLoader   loader     = null;

        ParameterList parameters = new SimpleParameterList();

        parameters.add(new Parameter("param-1", new ParameterMacroExpression<String>("{greeting}", String.class)));

        Assert.assertEquals("hello world", bldr.realize(resolver, loader, parameters));
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
        validate(TestSerializableHelper.<InstanceBuilder<Point>>convertPof(createAndPopulate()));
        }

    /**
     * Test ExternalizableLite serialization.
     */
    @Test
    public void testExternalizableLite()
        {
        validate(TestSerializableHelper.<InstanceBuilder<Point>>convertEL(createAndPopulate()));
        }

    // ----- helpers  -------------------------------------------------------

    /*
     * Create and populate the builder.
     *
     * @return the populated  builder
     */
    protected InstanceBuilder<Point> createAndPopulate()
        {
        InstanceBuilder<Point> bldr       = new InstanceBuilder<Point>(Point.class);
        ParameterList          parameters = new SimpleParameterList(1, 1);

        bldr.setConstructorParameterList(parameters);

        return bldr;
        }

    /*
    * Validate the builder.
    *
    * @param  list the populated builder
    */
    protected void validate(InstanceBuilder<Point> bldr)
        {
        ParameterResolver      resolver   = new NullParameterResolver();
        ClassLoader            loader     = null;

        Assert.assertEquals(new Point(1, 1), bldr.realize(resolver, loader, null));
        }
    }
