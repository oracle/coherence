/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jk  2017.07.19
 */
public class ElementProcessorHelperTest
    {

    public static final String XML_CLASS_SCHEME_CUSTOM
            = "<class-scheme>"
            + "  <foo:custom/>"
            + "</class-scheme>";

    public static final String XML_CLASS_SCHEME_NAME_AND_CUSTOM
            = "<class-scheme>"
            + "  <scheme-name>custom-scheme</scheme-name>"
            + "  <foo:custom/>"
            + "</class-scheme>";

    public static final String XML_CLASS_SCHEME_CUSTOM_AND_NAME
            = "<class-scheme>"
            + "  <foo:custom/>"
            + "  <scheme-name>custom-scheme</scheme-name>"
            + "</class-scheme>";

    public static final String XML_CACHE_STORE_CUSTOM_AND_FEDERATED_LOADING
            = "<cachestore-scheme>"
            + "  <federated-loading>true</federated-loading>"
            + "  <spring:bean>${bean-name}</spring:bean>"
            + "</cachestore-scheme>";

    public static final String XML_CACHE_STORE_FEDERATED_LOADING_AND_CUSTOM
            = "<cachestore-scheme>"
              + "  <federated-loading>true</federated-loading>"
              + "  <spring:bean>${bean-name}</spring:bean>"
              + "</cachestore-scheme>";

    /**
     * Make sure that a class-scheme element that contains a custom namespace
     * element is processed correctly.
     *
     * @throws Exception  if the test fails
     */
    @Test
    public void shouldProcessClassSchemeWithCustomElement() throws Exception
        {
        ProcessingContext    context  = mock(ProcessingContext.class);
        XmlElement           xml      = XmlHelper.loadXml(XML_CLASS_SCHEME_CUSTOM);
        ParameterizedBuilder builder  = mock(ParameterizedBuilder.class);

        when(context.processOnlyElementOf(any(XmlElement.class))).thenReturn(builder);

        ParameterizedBuilder<?> result = ElementProcessorHelper.processParameterizedBuilder(context, xml);

        assertThat(result, is(sameInstance(builder)));
        }

    /**
     * Make sure that a class-scheme element that contains both a custom namespace
     * element and a scheme-name is processed correctly.
     *
     * @throws Exception  if the test fails
     */
    @Test
    public void shouldProcessClassSchemeWithCustomElementAndSchemeName() throws Exception
        {
        ProcessingContext    context  = mock(ProcessingContext.class);
        XmlElement           xml      = XmlHelper.loadXml(XML_CLASS_SCHEME_NAME_AND_CUSTOM);
        ParameterizedBuilder builder  = mock(ParameterizedBuilder.class);

        when(context.processElement(any(XmlElement.class))).thenReturn(builder);

        ParameterizedBuilder<?> result = ElementProcessorHelper.processParameterizedBuilder(context, xml);

        assertThat(result, is(sameInstance(builder)));
        }

    /**
     * Make sure that a class-scheme element that contains both a custom namespace
     * element and a scheme-name is processed correctly.
     *
     * @throws Exception  if the test fails
     */
    @Test
    public void shouldProcessClassSchemeWithCustomElementAndSchemeNameReversedOrder() throws Exception
        {
        ProcessingContext    context  = mock(ProcessingContext.class);
        XmlElement           xml      = XmlHelper.loadXml(XML_CLASS_SCHEME_CUSTOM_AND_NAME);
        ParameterizedBuilder builder  = mock(ParameterizedBuilder.class);

        when(context.processElement(any(XmlElement.class))).thenReturn(builder);

        ParameterizedBuilder<?> result = ElementProcessorHelper.processParameterizedBuilder(context, xml);

        assertThat(result, is(sameInstance(builder)));
        }

    /**
     * Make sure that a cachestore-scheme element that contains both
     * a custom namespace element and a federated-loading element is
     * processed correctly.
     *
     * @since 14.1.2.0.2
     */
    @Test
    public void shouldProcessCacheStoreSchemeWithCustomElementAndFederatedLoading()
        {
        ProcessingContext       context  = mock(ProcessingContext.class);
        XmlElement              xml      = XmlHelper.loadXml(XML_CACHE_STORE_CUSTOM_AND_FEDERATED_LOADING);
        ParameterizedBuilder<?> builder  = mock(ParameterizedBuilder.class);

        when(context.processElement(any(XmlElement.class))).thenReturn(builder);

        ParameterizedBuilder<?> result = ElementProcessorHelper.processParameterizedBuilder(context, xml);

        assertThat(result, is(sameInstance(builder)));
        }

    /**
     * Make sure that a cachestore-scheme element that contains both
     * a custom namespace element and a federated-loading element is
     * processed correctly.
     *
     * @since 14.1.2.0.2
     */
    @Test
    public void shouldProcessClassSchemeWithCustomElementAndFederatedLoadingReversedOrder() throws Exception
        {
        ProcessingContext    context  = mock(ProcessingContext.class);
        XmlElement           xml      = XmlHelper.loadXml(XML_CACHE_STORE_FEDERATED_LOADING_AND_CUSTOM);
        ParameterizedBuilder builder  = mock(ParameterizedBuilder.class);

        when(context.processElement(any(XmlElement.class))).thenReturn(builder);

        ParameterizedBuilder<?> result = ElementProcessorHelper.processParameterizedBuilder(context, xml);

        assertThat(result, is(sameInstance(builder)));
        }

    }
