/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ExpressionParser;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.SystemPropertyParameterResolver;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.Base;
import com.tangosol.util.ResourceRegistry;

/**
 * A {@link DocumentProcessor} is responsible for processing in an {@link XmlDocument} to produce a resulting
 * configured resource.
 * <p>
 * During the processing of the {@link XmlDocument}, the provided {@link ResourceRegistry} may be
 * accessed/mutated.
 *
 * @author bo  2011.06.15
 * @since Coherence 12.1.2
 */
public class DocumentProcessor
    {
    // ----- constructors ----------------------------------------------------

    /**
     * Construct a {@link DocumentProcessor}.
     *
     * @param dependencies  the {@link Dependencies} for the {@link DocumentProcessor}
     */
    public DocumentProcessor(Dependencies dependencies)
        {
        m_dependencies = dependencies;
        }

    // ----- DocumentProcessor methods --------------------------------------

    /**
     * Processes the {@link XmlDocument} located at the specified {@link XmlDocumentReference}.
     *
     * @param <T>          the resource type
     * @param refDocument  the {@link XmlDocumentReference}
     * @param aOverrides   reference overrides
     *
     * @return a configured resource based on processing the root element (and children when required)
     * of the {@link XmlDocument} specified by the {@link XmlDocumentReference}
     *
     * @throws ConfigurationException when a configuration problem was encountered
     */
    @SuppressWarnings("unchecked")
    public <T> T process(XmlDocumentReference refDocument, XmlDocumentReference... aOverrides)
            throws ConfigurationException
        {
        // load the xml document
        XmlDocument xmlDocument = refDocument.getXmlDocument();

        // establish the root processing context
        DefaultProcessingContext context = new DefaultProcessingContext(m_dependencies, xmlDocument);

        // add the default namespace handler
        NamespaceHandler handler = m_dependencies.getDefaultNamespaceHandler();

        if (handler != null)
            {
            // apply overrides
            for (XmlDocumentReference refOverride : aOverrides)
                {
                XmlDocument xmlOverride = refOverride.getXmlDocument();

                if (handler.getOverrideProcessor() != null)
                    {
                    handler.getOverrideProcessor().process(xmlDocument, xmlOverride);
                    }
                }

            context.ensureNamespaceHandler("", handler);
            }

        // process the document
        Object oResult = context.processDocument(xmlDocument);

        // terminate and tidy up the created context.
        context.terminate();

        return (T) oResult;
        }

    // ----- Dependencies interface -----------------------------------------

    /**
     * The {@link Dependencies} of {@link DocumentProcessor}s.
     */
    public static interface Dependencies
        {
        /**
         * Obtains the {@link ResourceRegistry} for the {@link DocumentProcessor}.
         *
         * @return a {@link ResourceRegistry}
         */
        ResourceRegistry getResourceRegistry();

        /**
         * Obtains the {@link ClassLoader} to use for dynamically loading classes during processing.
         *
         * @return the {@link ClassLoader}
         */
        ClassLoader getContextClassLoader();

        /**
         * Obtains the {@link ExpressionParser} to use for parsing {@link Expression}s during document processing.
         *
         * @return the {@link ExpressionParser}
         */
        ExpressionParser getExpressionParser();

        /**
         * The {@link NamespaceHandler} for the default (ie: unspecified) xml namespace.
         *
         * @return the default {@link NamespaceHandler}
         */
        NamespaceHandler getDefaultNamespaceHandler();

        /**
         * Obtains the default {@link ParameterResolver} that may be used for
         * resolving externally defined configuration parameters, like those
         * from the operating system or container.  This {@link ParameterResolver}
         * is used when one is not provide or one is required during parsing
         * and processing the document.
         *
         * @return  the default {@link ParameterResolver}
         */
        ParameterResolver getDefaultParameterResolver();
        }

    // ----- DefaultDependencies class --------------------------------------

    /**
     * The {@link DefaultDependencies} is the default implementation of the
     * {@link DocumentProcessor} {@link Dependencies} interface.
     */
    public static class DefaultDependencies
            implements Dependencies
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link DefaultDependencies}.
         */
        public DefaultDependencies()
            {
            m_namespaceHandler   = null;
            m_resourceRegistry   = null;
            m_contextClassLoader = Base.ensureClassLoader(null);
            m_exprParser         = null;
            m_parameterResolver  = new SystemPropertyParameterResolver();
            }

        /**
         * Constructs a {@link DefaultDependencies} with a default {@link NamespaceHandler}.
         *
         * @param handler  the default {@link NamespaceHandler}
         */
        public DefaultDependencies(NamespaceHandler handler)
            {
            this();
            m_namespaceHandler = handler;
            }

        // ----- Dependencies methods ---------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public ResourceRegistry getResourceRegistry()
            {
            return m_resourceRegistry;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public ClassLoader getContextClassLoader()
            {
            return m_contextClassLoader;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public NamespaceHandler getDefaultNamespaceHandler()
            {
            return m_namespaceHandler;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExpressionParser getExpressionParser()
            {
            return m_exprParser;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public ParameterResolver getDefaultParameterResolver()
            {
            return m_parameterResolver;
            }

        // ----- DefaultDependencies methods --------------------------------

        /**
         * Sets the {@link ResourceRegistry} that will be used when processing a document.
         *
         * @param registry  the {@link ResourceRegistry}
         *
         * @return the {@link DefaultDependencies} so that fluent-method-chaining may be used
         */
        public DefaultDependencies setResourceRegistry(ResourceRegistry registry)
            {
            m_resourceRegistry = registry;

            return this;
            }

        /**
         * Sets the {@link ClassLoader} that will be used to dynamically load classes.
         *
         * @param classLoader  the {@link ClassLoader}
         *
         * @return the {@link DefaultDependencies} so that fluent-method-chaining may be used
         */
        public DefaultDependencies setClassLoader(ClassLoader classLoader)
            {
            m_contextClassLoader = classLoader;

            return this;
            }

        /**
         * Sets the {@link NamespaceHandler} for the default namespace of documents to be processed
         *
         * @param handler  the default {@link NamespaceHandler}
         *
         * @return the {@link DefaultDependencies} so that fluent-method-chaining may be used
         */
        public DefaultDependencies setDefaultNamespaceHandler(NamespaceHandler handler)
            {
            m_namespaceHandler = handler;

            return this;
            }

        /**
         * Sets the {@link ExpressionParser} to use for parsing {@link Expression}s during document processing.
         *
         * @param parser  the {@link ExpressionParser}
         *
         * @return the {@link DefaultDependencies} so that fluent-method-chaining may be used
         */
        public DefaultDependencies setExpressionParser(ExpressionParser parser)
            {
            m_exprParser = parser;

            return this;
            }

        /**
         * Sets the default {@link ParameterResolver} to use for resolving
         * externally defined (ie: operating system/container) level parameters.
         *
         * @param parameterResolver  the {@link ParameterResolver}
         *
         * @return the {@link DefaultDependencies} so that fluent-method-chaining may be used
         */
        public DefaultDependencies setDefaultParameterResolver(ParameterResolver parameterResolver)
            {
            m_parameterResolver = parameterResolver;

            return this;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link ResourceRegistry} in which to define or access external resources.
         */
        private ResourceRegistry m_resourceRegistry;

        /**
         * The {@link ClassLoader} from which dynamically requested classes should be loaded.
         */
        private ClassLoader m_contextClassLoader;

        /**
         * The {@link ExpressionParser} to use for parsing expressions in the document
         */
        private ExpressionParser m_exprParser;

        /**
         * The {@link NamespaceHandler} for the default xml namespace.
         */
        private NamespaceHandler m_namespaceHandler;

        /**
         * The default {@link ParameterResolver}.
         */
        private ParameterResolver m_parameterResolver;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Dependencies} for this {@link DocumentProcessor}.
     */
    private Dependencies m_dependencies;
    }
