/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package http.config;

import com.tangosol.coherence.config.xml.processor.HttpAcceptorDependenciesProcessor;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.internal.net.service.peer.acceptor.HttpAcceptorDependencies;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class HttpAcceptorDependenciesProcessorTests
    {
    @Test
    public void shouldUseDiscoveredHttpServer()
        {
        String      sXmlFile = "default-http-acceptor.xml";
        XmlDocument xml      = XmlHelper.loadFileOrResource(sXmlFile, "test");

        ProcessingContext                 context   = new DefaultProcessingContext();
        HttpAcceptorDependenciesProcessor processor = new HttpAcceptorDependenciesProcessor();
        HttpAcceptorDependencies          dependencies = processor.process(context, xml.getElement("http-acceptor"));

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getHttpServer(), is(instanceOf(HttpServerStub.class)));
        }
    }
