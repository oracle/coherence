/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config;

import com.oracle.coherence.concurrent.config.processors.CachedProcessor;
import com.oracle.coherence.concurrent.config.processors.FixedProcessor;
import com.oracle.coherence.concurrent.config.processors.SingleProcessor;
import com.oracle.coherence.concurrent.config.processors.WorkStealingProcessor;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;

import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;

import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

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

    @Test
    void testFixedThreadPool()
        {
        String sXml = "<e:fixed>\n"
                    + "  <e:name>test</e:name>\n"
                    + "  <e:thread-count>5</e:thread-count>\n"
                    + "</e:fixed>";

        XmlElement          xml       = XmlHelper.loadXml(sXml).getRoot();
        NamespaceHandler    handler   = new NamespaceHandler();
        ElementProcessor<?> processor = handler.getElementProcessor(xml);

        assertThat(processor, instanceOf(FixedProcessor.class));

        NamedExecutorService result = ((FixedProcessor) processor).process(context, xml);

        assertThat(result,                      is(notNullValue()));
        assertThat(result.getName(),            is("test"));
        assertThat(result.getExecutorService(), is(notNullValue()));
        assertThat(result.getDescription(), is("FixedThreadPool(ThreadCount=5, ThreadFactory=default)"));
        }

    @Test
    void testFixedThreadPoolWithThreadFactory()
        {
        String sXml = "<e:fixed>\n"
                    + "  <e:name>{foo test1}</e:name>\n"
                    + "  <e:thread-count>{foo-count 5}</e:thread-count>\n"
                    + "  <e:thread-factory>\n"
                    + "  <instance>\n"
                    + "    <class-factory-name>java.util.concurrent.Executors</class-factory-name>\n"
                    + "    <method-name>defaultThreadFactory</method-name>\n"
                    + "  </instance>\n"
                    + "  </e:thread-factory>\n"
                    + "</e:fixed>";

        XmlElement          xml       = XmlHelper.loadXml(sXml).getRoot();
        NamespaceHandler    handler   = new NamespaceHandler();
        ElementProcessor<?> processor = handler.getElementProcessor(xml);

        assertThat(processor, instanceOf(FixedProcessor.class));

        NamedExecutorService result = ((FixedProcessor) processor).process(context, xml);

        assertThat(result,                      is(notNullValue()));
        assertThat(result.getName(),            is("test1"));
        assertThat(result.getExecutorService(), is(notNullValue()));
        assertThat(result.getDescription(), is("FixedThreadPool(ThreadCount=5, ThreadFactory=java.util.concurrent.Executors$DefaultThreadFactory)"));
        }

    @Test
    void testSingleThreaded()
        {
        String sXml = "<e:single>\n"
                    + "  <e:name>test2</e:name>\n"
                    + "</e:single>";

        XmlElement          xml       = XmlHelper.loadXml(sXml).getRoot();
        NamespaceHandler    handler   = new NamespaceHandler();
        ElementProcessor<?> processor = handler.getElementProcessor(xml);

        assertThat(processor, instanceOf(SingleProcessor.class));

        NamedExecutorService result = ((SingleProcessor) processor).process(context, xml);

        assertThat(result,                      is(notNullValue()));
        assertThat(result.getName(),            is("test2"));
        assertThat(result.getExecutorService(), is(notNullValue()));
        assertThat(result.getDescription(), is("SingleThreaded(ThreadFactory=default)"));
        }

    @Test
    void testSingleThreadedWithThreadFactory()
        {
        String sXml = "<e:single>\n"
                    + "  <e:name>test3</e:name>\n"
                    + "  <e:thread-factory>\n"
                    + "  <instance>\n"
                    + "    <class-factory-name>java.util.concurrent.Executors</class-factory-name>\n"
                    + "    <method-name>defaultThreadFactory</method-name>\n"
                    + "  </instance>\n"
                    + "  </e:thread-factory>\n"
                    + "</e:single>";

        XmlElement          xml       = XmlHelper.loadXml(sXml).getRoot();
        NamespaceHandler    handler   = new NamespaceHandler();
        ElementProcessor<?> processor = handler.getElementProcessor(xml);

        assertThat(processor, instanceOf(SingleProcessor.class));

        NamedExecutorService result = ((SingleProcessor) processor).process(context, xml);

        assertThat(result,                      is(notNullValue()));
        assertThat(result.getName(),            is("test3"));
        assertThat(result.getExecutorService(), is(notNullValue()));
        assertThat(result.getDescription(), is("SingleThreaded(ThreadFactory=java.util.concurrent.Executors$DefaultThreadFactory)"));
        }

    @Test
    void testCached()
        {
        String sXml = "<e:cached>\n"
                    + "  <e:name>test4</e:name>\n"
                    + "</e:cached>";

        XmlElement          xml       = XmlHelper.loadXml(sXml).getRoot();
        NamespaceHandler    handler   = new NamespaceHandler();
        ElementProcessor<?> processor = handler.getElementProcessor(xml);

        assertThat(processor, instanceOf(CachedProcessor.class));

        NamedExecutorService result = ((CachedProcessor) processor).process(context, xml);

        assertThat(result,                      is(notNullValue()));
        assertThat(result.getName(),            is("test4"));
        assertThat(result.getExecutorService(), is(notNullValue()));
        assertThat(result.getDescription(), is("CachedThreadPool(ThreadFactory=default)"));
        }

    @Test
    void testCachedWithThreadFactory()
        {
        String sXml = "<e:cached>\n"
                    + "  <e:name>test5</e:name>\n"
                    + "  <e:thread-factory>\n"
                    + "  <instance>\n"
                    + "    <class-factory-name>java.util.concurrent.Executors</class-factory-name>\n"
                    + "    <method-name>defaultThreadFactory</method-name>\n"
                    + "  </instance>\n"
                    + "  </e:thread-factory>\n"
                    + "</e:cached>";

        XmlElement          xml       = XmlHelper.loadXml(sXml).getRoot();
        NamespaceHandler    handler   = new NamespaceHandler();
        ElementProcessor<?> processor = handler.getElementProcessor(xml);

        assertThat(processor, instanceOf(CachedProcessor.class));

        NamedExecutorService result = ((CachedProcessor) processor).process(context, xml);

        assertThat(result,                      is(notNullValue()));
        assertThat(result.getName(),            is("test5"));
        assertThat(result.getExecutorService(), is(notNullValue()));
        assertThat(result.getDescription(), is("CachedThreadPool(ThreadFactory=java.util.concurrent.Executors$DefaultThreadFactory)"));
        }

    @Test
    void testWorkStealing()
        {
        String sXml = "<e:work-stealing>\n"
                    + "  <e:name>test6</e:name>\n"
                    + "</e:work-stealing>";

        int                 nProcessors = Runtime.getRuntime().availableProcessors();
        XmlElement          xml         = XmlHelper.loadXml(sXml).getRoot();
        NamespaceHandler    handler     = new NamespaceHandler();
        ElementProcessor<?> processor   = handler.getElementProcessor(xml);

        assertThat(processor, instanceOf(WorkStealingProcessor.class));

        NamedExecutorService result = ((WorkStealingProcessor) processor).process(context, xml);

        assertThat(result,                      is(notNullValue()));
        assertThat(result.getName(),            is("test6"));
        assertThat(result.getExecutorService(), is(notNullValue()));
        assertThat(result.getDescription(), is("WorkStealingThreadPool(Parallelism=" + nProcessors + ')'));
        }

    @Test
    void testWorkStealingWithParallelism()
        {
        String sXml = "<e:work-stealing>\n"
                    + "  <e:name>test7</e:name>\n"
                    + "  <e:parallelism>{foo 5}</e:parallelism>\n"
                    + "</e:work-stealing>";

        XmlElement          xml       = XmlHelper.loadXml(sXml).getRoot();
        NamespaceHandler    handler   = new NamespaceHandler();
        ElementProcessor<?> processor = handler.getElementProcessor(xml);

        assertThat(processor, instanceOf(WorkStealingProcessor.class));

        NamedExecutorService result = ((WorkStealingProcessor) processor).process(context, xml);

        assertThat(result,                      is(notNullValue()));
        assertThat(result.getName(),            is("test7"));
        assertThat(result.getExecutorService(), is(notNullValue()));
        assertThat(result.getDescription(), is("WorkStealingThreadPool(Parallelism=5)"));
        }

    // ----- data members ---------------------------------------------------

    /**
     * The configuration {@link ProcessingContext}.
     */
    protected static ProcessingContext context;
    }
