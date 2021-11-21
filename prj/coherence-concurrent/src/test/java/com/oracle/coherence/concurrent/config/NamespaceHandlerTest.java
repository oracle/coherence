/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config;

import com.oracle.coherence.concurrent.config.builders.ConcurrentConfigurationBuilder;
import com.oracle.coherence.concurrent.config.builders.ExecutorServiceBuilder;

import com.oracle.coherence.concurrent.config.processors.ConcurrentConfigurationProcessor;
import com.oracle.coherence.concurrent.config.processors.ExecutorServiceProcessor;
import com.oracle.coherence.concurrent.config.processors.ExecutorServicesProcessor;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;

import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.util.List;

import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for {@link com.tangosol.config.xml.NamespaceHandler}
 *
 * @author rl  11.20.21
 * @since 21.12
 */
public class NamespaceHandlerTest
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void initContainer()
        {
        context = new DefaultProcessingContext(
                new DocumentProcessor.DefaultDependencies()
                        .setExpressionParser(new ParameterMacroExpressionParser()));
        context.ensureNamespaceHandler("c", new NamespaceHandler());
        context.ensureNamespaceHandler("", new CacheConfigNamespaceHandler());
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Validate {@link ExecutorServicesProcessor}.
     */
    @Test
    void testExecutorServiceProcessor()
        {
        NamedExecutorService result =
                realizeNamedExecutorService("<c:executor-service>\n"
                                          + "  <name>test</name>\n"
                                          + "  <instance>\n"
                                          + "    <class-factory-name>java.util.concurrent.Executors</class-factory-name>\n"
                                          + "    <method-name>newSingleThreadExecutor</method-name>\n"
                                          + "  </instance>\n"
                                          + "</c:executor-service>");
        assertThat(result, is(notNullValue()));
        }

    /**
     * Validate {@link ExecutorServicesProcessor}.
     */
    @Test
    void testExecutorServicesProcessor()
        {
        List<ExecutorServiceBuilder> result =
                realizeExecutorServiceBuilders("<c:executor-services>\n"
                                               + "  <c:executor-service>\n"
                                               + "    <name>test</name>\n"
                                               + "    <instance>\n"
                                               + "      <class-factory-name>java.util.concurrent.Executors</class-factory-name>\n"
                                               + "      <method-name>newSingleThreadExecutor</method-name>\n"
                                               + "    </instance>\n"
                                               + "  </c:executor-service>\n"
                                               + "  <c:executor-service>\n"
                                               + "    <name>test2</name>\n"
                                               + "    <instance>\n"
                                               + "      <class-factory-name>java.util.concurrent.Executors</class-factory-name>\n"
                                               + "      <method-name>newFixedThreadPool</method-name>\n"
                                               + "        <init-params>\n"
                                               + "          <init-param>\n"
                                               + "            <param-name>nThreads</param-name>\n"
                                               + "            <param-type>int</param-type>\n"
                                               + "            <param-value>5</param-value>\n"
                                               + "          </init-param>\n"
                                               + "        </init-params>"
                                               + "    </instance>\n"
                                               + "  </c:executor-service>\n"
                                               + "</c:executor-services>");
        assertThat(result, is(notNullValue()));

        assertThat(result.size(), is(2));

        validate("test",  result.get(0));
        validate("test2", result.get(1));
        }

    /**
     * Validate {@link ConcurrentConfigurationProcessor}.
     */
    @Test
    void testConcurrentConfigurationProcessorWithMultipleExecutors()
        {
        ConcurrentConfiguration result =
                realizeConcurrentConfiguration("<c:concurrent-config>\n"
                                             + "  <c:executor-services>\n"
                                             + "    <c:executor-service>\n"
                                             + "      <name>test</name>\n"
                                             + "      <instance>\n"
                                             + "        <class-factory-name>java.util.concurrent.Executors</class-factory-name>\n"
                                             + "        <method-name>newSingleThreadExecutor</method-name>\n"
                                             + "      </instance>\n"
                                             + "    </c:executor-service>\n"
                                             + "    <c:executor-service>\n"
                                             + "      <name>test2</name>\n"
                                             + "      <instance>\n"
                                             + "        <class-factory-name>java.util.concurrent.Executors</class-factory-name>\n"
                                             + "        <method-name>newSingleThreadExecutor</method-name>\n"
                                             + "      </instance>\n"
                                             + "    </c:executor-service>\n"
                                             + "  </c:executor-services>\n"
                                             + "</c:concurrent-config>");
        assertThat(result, notNullValue());

        List<NamedExecutorService> services = result.getNamedExecutorServices();
        assertThat(services, is(notNullValue()));
        assertThat(services.size(), is(2));
        }

    /**
     * Validate {@link ConcurrentConfigurationProcessor} when no executors
     * have been defined.
     */
    @Test
    void testConcurrentConfigurationProcessorWithNoExecutors()
        {
        ConcurrentConfiguration result =
                realizeConcurrentConfiguration("<c:concurrent-config>\n"
                                             + "</c:concurrent-config>");
        assertThat(result, is(notNullValue()));

        List<NamedExecutorService> services = result.getNamedExecutorServices();
        assertThat(services, notNullValue());
        assertThat(services.size(), is(0));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Validates the {@link ExecutorServiceBuilder} is configured as expected.
     *
     * @param sExpectedName  the expected executor name
     * @param service        the {@link ExecutorServiceBuilder} to validate
     */
    protected void validate(String sExpectedName, ExecutorServiceBuilder service)
        {
        NamedExecutorService namedService = service.realize(null, null, null);

        assertThat(namedService.getName(), is(sExpectedName));
        assertThat(namedService.f_supplier, is(notNullValue()));
        ExecutorService service1Actual = namedService.getExecutorService();
        assertThat(service1Actual, is(notNullValue()));
        }

    /**
     * Realizes xml content for {@code executor-service} elements.
     *
     * @param sXml  the XML to produce a {@link NamedExecutorService}
     *
     * @return the realized {@link NamedExecutorService}
     */
    protected NamedExecutorService realizeNamedExecutorService(String sXml)
        {
        XmlElement          xml       = XmlHelper.loadXml(sXml).getRoot();
        NamespaceHandler    handler   = new NamespaceHandler();
        ElementProcessor<?> processor = handler.getElementProcessor(xml);

        assertThat(processor, instanceOf(ExecutorServiceProcessor.class));

        ExecutorServiceBuilder builder = ((ExecutorServiceProcessor) processor).process(context, xml);
        return builder.realize(null, null, null);
        }

    /**
     * Realizes xml content for {@code executor-services} elements.
     *
     * @param sXml  the XML to produce a list of {@link ExecutorServiceBuilder}
     *
     * @return the realized list of {@link ExecutorServiceBuilder}
     */
    protected List<ExecutorServiceBuilder> realizeExecutorServiceBuilders(String sXml)
        {
        XmlElement          xml       = XmlHelper.loadXml(sXml).getRoot();
        NamespaceHandler    handler   = new NamespaceHandler();
        ElementProcessor<?> processor = handler.getElementProcessor(xml);

        assertThat(processor, instanceOf(ExecutorServicesProcessor.class));

        return ((ExecutorServicesProcessor) processor).process(context, xml);
        }

    /**
     * Realizes xml content for {@code concurrent-config} elements.
     *
     * @param sXml  the XML to produce a  {@link ConcurrentConfiguration}
     *
     * @return the realized {@link ConcurrentConfiguration}
     */
    protected ConcurrentConfiguration realizeConcurrentConfiguration(String sXml)
        {
        XmlElement          xml       = XmlHelper.loadXml(sXml).getRoot();
        NamespaceHandler    handler   = new NamespaceHandler();
        ElementProcessor<?> processor = handler.getElementProcessor(xml);

        assertThat(processor, instanceOf(ConcurrentConfigurationProcessor.class));

        ConcurrentConfigurationBuilder builder = ((ConcurrentConfigurationProcessor) processor).process(context, xml);
        return builder.realize(null, null, null);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The configuration {@link ProcessingContext}.
     */
    protected static ProcessingContext context;
    }
