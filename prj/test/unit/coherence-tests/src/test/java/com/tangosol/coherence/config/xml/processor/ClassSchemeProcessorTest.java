/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.scheme.ClassScheme;

import com.tangosol.config.ConfigurationException;

import java.awt.Point;
import java.awt.Rectangle;

import java.net.URISyntaxException;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

/**
 * Unit Tests for {@link CustomizableBuilderProcessor<ClassScheme>}s
 *
 * @author bo  2011.06.24
 */
public class ClassSchemeProcessorTest
        extends AbstractClassSchemeProcessorTest
    {
    /**
     * Ensure that we can create a {@link ParameterizedBuilder} based on a <class-scheme>
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testRegularClassSchemeProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml = "<class-scheme>" + "<class-name>java.awt.Point</class-name>" + "<init-params>"
                      + "<init-param><param-value>100</param-value></init-param>"
                      + "<init-param><param-value>100</param-value></init-param>" + "</init-params>"
                      + "</class-scheme>";

        assertThat(createAndInvokeBuilder(sXml, Point.class), is(new Point(100, 100)));
        }

    /**
     * Ensure that we can create a {@link ParameterizedBuilder} based on a <class-scheme>
     * using a <class-factory-name>.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testStaticFactoryClassSchemeProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml = "<class-scheme>" + "<class-factory-name>java.lang.System</class-factory-name>"
                      + "<method-name>getProperty</method-name>" + "<init-params>"
                      + "<init-param><param-value>java.class.path</param-value></init-param>" + "</init-params>"
                      + "</class-scheme>";

        assertThat(createAndInvokeBuilder(sXml, String.class), is(System.getProperty("java.class.path")));
        }

    /**
     * Ensure that we can't create a {@link ParameterizedBuilder} based on a <class-scheme>
     * when there we can't find a matching constructor.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test(expected = Exception.class)
    public void testInvalidClassSchemeProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml = "<class-scheme>" + "<class-name>java.awt.Point</class-name>" + "<init-params>"
                      + "<init-param><param-type>double</param-type><param-value>100</param-value></init-param>"
                      + "<init-param><param-value>100</param-value></init-param>" + "</init-params>"
                      + "</class-scheme>";

        assertThat(createAndInvokeBuilder(sXml, Point.class), is(new Point(100, 100)));
        }

    /**
     * Ensure that we can create a {@link ParameterizedBuilder} based on a <class-scheme>
     * that contains other nested <class-schemes>
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testNestedClassSchemeProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml =
            "<class-scheme>" + "<class-name>java.awt.Rectangle</class-name>" + "<init-params>"
            + "<init-param><param-value><class-scheme>" + "<class-name>java.awt.Point</class-name>"
            + "<init-params><init-param><param-type>int</param-type><param-value>100</param-value></init-param>"
            + "<init-param><param-type>int</param-type><param-value>100</param-value></init-param>"
            + "</init-params></class-scheme></param-value></init-param>" + "</init-params></class-scheme>";

        assertThat(createAndInvokeBuilder(sXml, Rectangle.class), is(new Rectangle(new Point(100, 100))));
        }
    }
