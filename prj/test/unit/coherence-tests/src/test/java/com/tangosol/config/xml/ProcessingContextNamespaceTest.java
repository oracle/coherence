/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.AbstractNamespaceHandler;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.DocumentProcessor.Dependencies;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlDocumentReference;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.Test;

import static org.junit.Assert.*;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;

/**
 * The unit tests for the {@link ConfigurationContext}.
 *
 * @author bo
 * @author dr
 */
public class ProcessingContextNamespaceTest
    {
    /**
     * Ensure that namespaces with inner-class-based processors are registered.
     *
     * @throws Exception When a test fails.
     */
    @Test
    public void testInnerClassNamespaceRegistration()
            throws Exception
        {
        String                   sXml = "<cache-config/>";
        XmlElement               xml  = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx  = new DefaultProcessingContext(xml);
        URI uri = new URI(String.format("class:%s", NamespaceHandlerWithInnerClassXmlProcessors.class.getName()));

        ctx.ensureNamespaceHandler("innerclass", uri);

        assertTrue(ctx.getNamespaceURI("innerclass").equals(uri));
        assertTrue(ctx.getNamespaceHandler("innerclass") instanceof NamespaceHandlerWithInnerClassXmlProcessors);
        assertTrue(ctx.getNamespaceHandler(uri) instanceof NamespaceHandlerWithInnerClassXmlProcessors);
        assertTrue(ctx.getNamespaceHandler("innerclass") == ctx.getNamespaceHandler(uri));

        AbstractNamespaceHandler handler = (AbstractNamespaceHandler) ctx.getNamespaceHandler("innerclass");

        assertNull(handler.getAttributeProcessor("IAnonmousAttributeProcessor"));
        assertNull(handler.getElementProcessor("IAnonmousElementProcessor"));
        assertNull(handler.getAttributeProcessor("ap"));
        assertNull(handler.getElementProcessor("ep"));
        assertNull(handler.getAttributeProcessor("Bar"));
        assertNull(handler.getElementProcessor("Bar"));
        assertNull(handler.getAttributeProcessor("Foo"));
        assertNull(handler.getElementProcessor("Foo"));
        assertNull(handler.getAttributeProcessor("NonStaticAnonymousAttributeProcessor"));
        assertNull(handler.getElementProcessor("NonStaticAnonymousElementProcessor"));
        assertNull(handler.getAttributeProcessor("StaticAnonymousAttributeHandler"));
        assertNull(handler.getElementProcessor("StaticAnonymousElementHandler"));

        assertNotNull(handler.getAttributeProcessor("non-static-named-ap"));
        assertNotNull(handler.getElementProcessor("non-static-named-ep"));
        assertNotNull(handler.getAttributeProcessor("static-named-ap"));
        assertNotNull(handler.getElementProcessor("static-named-eap"));
        assertNotNull(handler.getElementProcessor("static-named-ep"));
        }

    /**
     * Ensure that namespaces with externally defined handlers can be registered.
     *
     * @throws Exception When a test fails.
     */
    @Test
    public void testExternalClassNamespaceRegistration()
            throws Exception
        {
        String                   sXml = "<cache-config/>";
        XmlElement               xml  = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx  = new DefaultProcessingContext(xml);
        URI                      uri = new URI(String.format("class:%s",
                                           ExternalClassNamespaceHandler.class.getName()));

        ctx.ensureNamespaceHandler("externalclass", uri);

        assertTrue(ctx.getNamespaceURI("externalclass").equals(uri));
        assertTrue(ctx.getNamespaceHandler("externalclass") instanceof ExternalClassNamespaceHandler);
        assertTrue(ctx.getNamespaceHandler(uri) instanceof ExternalClassNamespaceHandler);
        assertTrue(ctx.getNamespaceHandler("externalclass") == ctx.getNamespaceHandler(uri));
        }

    /**
     * Ensure that it's ok to use non-class-based namespaces.
     *
     * @throws ConfigurationException
     */
    @Test
    public void testOverridingDefaultNamespaceRegistration()
            throws ConfigurationException
        {
        // define an element with a default non-class-based namespace uri
        String sXml = "<element xmlns=\"http://some.domain.name\"/>";

        // create a namespace on the fly with a mock ElementProcessor we can assert with.
        AbstractNamespaceHandler          handler = new AbstractNamespaceHandler()
            {
            } ;
        RecordingElementProcessor<String> ep      = new RecordingElementProcessor<String>("Hello");

        handler.registerProcessor("element", ep);

        // establish a document processor
        Dependencies      dep       = new DocumentProcessor.DefaultDependencies(handler);
        DocumentProcessor processor = new DocumentProcessor(dep);

        // process the document
        processor.process(new XmlDocumentReference(sXml));
        assertEquals(1, ep.getProcessedCount());
        }
    }
