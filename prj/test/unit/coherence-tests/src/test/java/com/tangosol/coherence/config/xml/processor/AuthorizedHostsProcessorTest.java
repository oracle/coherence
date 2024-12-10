/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.DocumentProcessor.DefaultDependencies;

import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Unit test for {@link AuthorizedHostsProcessor}
 *
 * @author cl  2014.07.29
 */
public class AuthorizedHostsProcessorTest
	{
	/**
     * Test the customized host-filter is created.
     *
	 * see Bug 19279409
	 */
	@Test
	public void testAuthorizedHostsFilterProcessing()
		{
		String sXml =
                "<tcp-acceptor><authorized-hosts>" + "<host-address>localhost</host-address>" +
                "<host-filter><class-name>com.tangosol.coherence.config.xml.processor.AuthorizedHostsProcessorTest$AuthorizedHostsFilter</class-name></host-filter>" +
                "</authorized-hosts></tcp-acceptor>";

        ResourceRegistry resourceRegistry = new SimpleResourceRegistry();

        DefaultDependencies
                dep = new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        DocumentProcessor processor = new DocumentProcessor(dep);

        DefaultTcpAcceptorDependencies tcp = (DefaultTcpAcceptorDependencies) processor.process(new XmlDocumentReference(sXml));

        assertTrue(tcp.getAuthorizedHostFilterBuilder() != null);
        assertTrue(tcp.getAuthorizedHostFilterBuilder().realize(new NullParameterResolver(), Base.getContextClassLoader(), null) instanceof AuthorizedHostsFilter);
		}

    /**
     * A Filter implementation for host-filter.
     */
	public static class AuthorizedHostsFilter implements Filter
		{
    	@Override
    	public boolean evaluate(Object o)
    		{
        	return true;
    		}
		}
	}