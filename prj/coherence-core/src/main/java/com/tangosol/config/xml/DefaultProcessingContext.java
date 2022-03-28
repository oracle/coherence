/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ExpressionParser;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.Value;
import com.tangosol.config.xml.DocumentProcessor.Dependencies;

import com.tangosol.run.xml.QualifiedName;
import com.tangosol.run.xml.SimpleAttribute;
import com.tangosol.run.xml.XmlAttribute;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.UUID;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.net.URI;
import java.net.URISyntaxException;

import java.text.ParseException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import java.util.Map.Entry;

import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * The default implementation of a {@link ProcessingContext}.
 *
 * @author bo  2011.06.14
 * @since Coherence 12.1.2
 */
public class DefaultProcessingContext
        implements ProcessingContext, AutoCloseable
    {
    // ----- constructors ----------------------------------------------------

    /**
     * Constructs a root {@link ProcessingContext} using default {@link DocumentProcessor} {@link DocumentProcessor.Dependencies}.
     */
    public DefaultProcessingContext()
        {
        this((Dependencies) null);
        }

    /**
     * Constructs a root {@link ProcessingContext} with the specified {@link DocumentProcessor} {@link DocumentProcessor.Dependencies}.
     *
     * @param dependencies  the {@link DocumentProcessor.Dependencies} for the {@link ProcessingContext}
     */
    public DefaultProcessingContext(Dependencies dependencies)
        {
        m_dependencies = dependencies == null ? new DocumentProcessor.DefaultDependencies() : dependencies;
        m_ctxParent                    = null;
        m_xmlElement                   = null;
        m_mapNamespaceURIsByPrefix     = new LinkedHashMap<>();
        m_mapNamespaceHandlersByURI    = new LinkedHashMap<>();
        m_mapCookiesByType             = new HashMap<>();
        m_mapPropertyPaths             = new HashMap<>();
        m_mapAttributeProcessorsByType = new HashMap<>();
        m_mapElementProcessorsByType   = new HashMap<>();
        m_setProcessedChildElements    = new HashSet<>();
        }

    /**
     * Constructs a root {@link ProcessingContext} for a given {@link XmlElement} using default DocumentProcessor
     * dependencies.
     *
     * @param xmlElement  the {@link XmlElement} for the {@link ProcessingContext}
     */
    public DefaultProcessingContext(XmlElement xmlElement)
        {
        this(new DocumentProcessor.DefaultDependencies());
        m_ctxParent  = null;
        m_xmlElement = xmlElement;
        }

    /**
     * Constructs a sub-{@link ProcessingContext} of another {@link ProcessingContext}.
     *
     * @param ctxParent   the parent {@link ProcessingContext} for this {@link ProcessingContext}
     * @param xmlElement  the {@link XmlElement} for the sub-{@link ProcessingContext}
     */
    public DefaultProcessingContext(DefaultProcessingContext ctxParent, XmlElement xmlElement)
        {
        this(ctxParent.getDependencies());
        m_ctxParent  = ctxParent;
        m_xmlElement = xmlElement;
        }

    /**
     * Constructs a root {@link ProcessingContext} for a given {@link XmlElement}.
     *
     * @param dependencies  the {@link DocumentProcessor.Dependencies} for the {@link ProcessingContext}
     * @param xmlElement    the {@link XmlElement} for the {@link ProcessingContext}
     */
    public DefaultProcessingContext(Dependencies dependencies, XmlElement xmlElement)
        {
        this(dependencies);
        m_ctxParent  = null;
        m_xmlElement = xmlElement;
        if (m_xmlElement != null)
            {
            loadNamespaceHandlers(m_xmlElement);
            }
        }

    // ----- ResourceResolver interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R getResource(Class<R> clsResource)
        {
        R resource = getCookie(clsResource);

        if (resource == null && getResourceRegistry() != null)
            {
            resource = getResourceRegistry().getResource(clsResource);
            }

        return resource;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R getResource(Class<R> clsResource, String sResourceName)
        {
        R resource = getCookie(clsResource, sResourceName);

        if (resource == null && getResourceRegistry() != null)
            {
            resource = getResourceRegistry().getResource(clsResource, sResourceName);
            }

        return resource;
        }

    // ----- ProcessingContext interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceRegistry getResourceRegistry()
        {
        return m_dependencies.getResourceRegistry();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterResolver getDefaultParameterResolver()
        {
        return m_dependencies.getDefaultParameterResolver();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getContextClassLoader()
        {
        return m_dependencies.getContextClassLoader();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void addCookie(Class<T> clzCookie, String sName, T value)
        {
        HashMap<String, Object> mapCookiesByName = m_mapCookiesByType
                .computeIfAbsent(clzCookie, k -> new HashMap<>());

        mapCookiesByName.put(sName, value);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void addCookie(Class<T> clzCookie, T cookie)
        {
        addCookie(clzCookie, clzCookie.getName(), cookie);
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCookie(Class<T> clzCookie, String sName)
        {
        HashMap<String, Object> mapCookiesByName = m_mapCookiesByType.get(clzCookie);
        T                       cookie           = mapCookiesByName == null ? null : (T) mapCookiesByName.get(sName);

        return cookie == null ? (isRootContext() ? null : m_ctxParent.getCookie(clzCookie, sName)) : cookie;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getCookie(Class<T> clzCookie)
        {
        return clzCookie == null ? null : getCookie(clzCookie, clzCookie.getName());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void definePropertyPath(String sBeanPropertyName, String sXmlPath)
        {
        m_mapPropertyPaths.put(sBeanPropertyName, sXmlPath);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerProcessor(Class<T> clzType, AttributeProcessor<T> processor)
        {
        m_mapAttributeProcessorsByType.put(clzType, processor);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerProcessor(Class<T> clzType, ElementProcessor<T> processor)
        {
        m_mapElementProcessorsByType.put(clzType, processor);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerAttributeType(Class<T> clzType)
        {
        registerProcessor(clzType, new SimpleAttributeProcessor<>(clzType));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerElementType(Class<T> clzType)
        {
        registerProcessor(clzType, new SimpleElementProcessor<>(clzType));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExpressionParser getExpressionParser()
        {
        return m_dependencies.getExpressionParser();
        }

    /**
     * {@inheritDoc}
     */
    public Object processDocument(XmlElement xmlElement)
            throws ConfigurationException
        {
        return processElement(xmlElement);
        }

    /**
     * {@inheritDoc}
     */
    public Object processDocumentAt(URI uri)
            throws ConfigurationException
        {
        return processDocument(XmlHelper.loadFileOrResource(uri.toString(), "cache configuration",
            m_dependencies.getContextClassLoader()));
        }

    /**
     * {@inheritDoc}
     */
    public Object processDocumentAt(String sLocation)
            throws ConfigurationException
        {
        return processDocument(XmlHelper.loadFileOrResource(sLocation, "cache configuration",
            m_dependencies.getContextClassLoader()));
        }

    /**
     * {@inheritDoc}
     */
    public Object processDocument(String sXml)
            throws ConfigurationException
        {
        return processDocument(XmlHelper.loadXml(sXml));
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Object processElement(XmlElement xmlElement)
            throws ConfigurationException
        {
        // NOTE: Seven sequential tasks are required to "process" an XmlElement.  These must
        // be performed in a specific order as they are dependent on each other.
        Object oResult = null;

        // we need a list of XmlAttributes for the XmlElement as we may use these in multiple places.
        ArrayList<XmlAttribute> lstAttributes = new ArrayList<>(xmlElement.getAttributeMap().size());

        for (String sAttributeName : ((Set<String>) xmlElement.getAttributeMap().keySet()))
            {
            lstAttributes.add(new SimpleAttribute(xmlElement, sAttributeName, xmlElement.getAttribute(sAttributeName)));
            }

        // we need to know if a new namespace was loaded or if this is the root
        // element to determine if the element requires preprocessing
        boolean fElementRequiresPreprocessing = isRootContext() || xmlElement.getParent() == null;

        // TASK 1: Start a new sub context for the element we're about to process.
        DefaultProcessingContext context = new DefaultProcessingContext(this, xmlElement);

        // TASK 2: Ensure that all of NamespaceHandler(s) declared in the element (using xmlns) are defined
        // in the sub-context.
        for (XmlAttribute attribute : lstAttributes)
            {
            QualifiedName qnAttribute = attribute.getQualifiedName();

            // only process "xmlns" declarations
            if (qnAttribute.getLocalName().equals("xmlns"))
                {
                String sURI = attribute.getXmlValue().getString();

                try
                    {
                    // ensure that the declared Xml Namespaces are available in the context of this element.
                    context.ensureNamespaceHandler(qnAttribute.getPrefix(), new URI(sURI));

                    // a new namespace was loaded so we must perform pre-processing
                    fElementRequiresPreprocessing = true;
                    }
                catch (URISyntaxException uriSyntaxException)
                    {
                    throw new ConfigurationException(String.format("Invalid URI '%s' specified for Xml Namespace '%s'",
                        sURI, qnAttribute.getPrefix()), "You must specify a valid URI for the Xml Namespace.",
                            uriSyntaxException);
                    }
                }
            }

        // TASK 3: Pre-process the element using all visible NamespaceHandler
        // DocumentPreprocessors until no more pre-precessing is required
        if (fElementRequiresPreprocessing)
            {
            Iterable<NamespaceHandler> namespaceHandlers = context.getNamespaceHandlers();
            boolean                    fRevisit;

            do
                {
                fRevisit = false;

                for (NamespaceHandler namespaceHandler : namespaceHandlers)
                    {
                    DocumentPreprocessor preprocessor = namespaceHandler.getDocumentPreprocessor();

                    if (preprocessor != null)
                        {
                        //noinspection ConstantConditions
                        fRevisit = fRevisit || preprocessor.preprocess(context, xmlElement);

                        if (fRevisit)
                            {
                            break;
                            }
                        }
                    }
                }
            while (fRevisit);
            }

        // TASK 4: Find an appropriate ElementProcessor for the element we're about to process
        QualifiedName       qnElement   = xmlElement.getQualifiedName();
        NamespaceHandler    nsElement   = context.getNamespaceHandler(qnElement.getPrefix());
        ElementProcessor<?> procElement = nsElement == null ? null : nsElement.getElementProcessor(xmlElement);

        if (nsElement == null)
            {
            throw new ConfigurationException(String.format(
                "A NamespaceHandler could not be located for the namespace [%s] in the element [%s]",
                qnElement.getPrefix(),
                xmlElement), "A NamespaceHandler implementation for the namespace must be defined.");
            }
        else if (procElement == null)
            {
            throw new ConfigurationException(String.format("An ElementProcessor could not be located for the element [%s]",
                qnElement), "The specified element is unknown to the NamespaceHandler implementation. " + "Perhaps the xml element is foreign to the Xml Namespace?");
            }
        else
            {
            // TASK 5: Process all the xml attributes declared in the element.
            for (XmlAttribute attribute : lstAttributes)
                {
                QualifiedName    qnAttribute = attribute.getQualifiedName();
                NamespaceHandler nsAttribute;

                // locate the AttributeProcessor for the XmlAttribute using the appropriate NamespaceHandler
                if (qnAttribute.hasPrefix())
                    {
                    // when an attribute is defined in a specific namespace, use the namespace of the attribute
                    nsAttribute = getNamespaceHandler(qnAttribute.getPrefix());
                    }
                else
                    {
                    // when an attribute is not defined in a specific namespace, use the namespace of the element.
                    nsAttribute = nsElement;
                    }

                AttributeProcessor<?> procAttribute = nsAttribute == null
                                                      ? null : nsAttribute.getAttributeProcessor(attribute);

                //noinspection StatementWithEmptyBody
                if (nsAttribute == null || procAttribute == null)
                    {
                    // SKIP: when we don't have an NamespaceHandler or AttributeProcessor for the attribute
                    // we simply ignore the request because the dependency injection framework may/can try to access
                    // the content directly.
                    }
                else
                    {
                    procAttribute.process(context, attribute);
                    }
                }

            // TASK 6: Use the located ElementProcessor to process the element in its context.
            ConditionalElementProcessor<?> procConditional = procElement instanceof ConditionalElementProcessor
                                                             ? ((ConditionalElementProcessor<?>) procElement) : null;

            if (procConditional == null || procConditional.accepts(context, xmlElement))
                {
                oResult = procElement.process(context, xmlElement);
                }
            }

        // TASK 7: Terminate the current context as it's now out of scope.
        context.terminate();

        // remember that we've processed this element so that if a call to
        // processRemainingElements is made, we can skip this one
        m_setProcessedChildElements.add(xmlElement);

        return oResult;
        }

    /**
     * {@inheritDoc}
     */
    public Object processElement(String sXml)
            throws ConfigurationException
        {
        return processElement(XmlHelper.loadXml(sXml));
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T processOnlyElementOf(XmlElement xmlElement)
            throws ConfigurationException
        {
        // the xmlElement must only have a single child
        if (xmlElement.getElementList().size() == 1)
            {
            // process the child element
            return (T) processElement((XmlElement) xmlElement.getElementList().get(0));
            }
        else
            {
            // expected only a single element in custom-provider
            throw new ConfigurationException(String.format("Only a single element is permitted in the %s element.",
                xmlElement), String.format("Please consult the documentation regarding use of the '%s' namespace",
                                           new QualifiedName(xmlElement).getPrefix()));
            }
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Map<String, ?> processElementsOf(XmlElement xmlElement)
            throws ConfigurationException
        {
        // process all the children of the xmlElement
        LinkedHashMap<String, Object> mapResult = new LinkedHashMap<>();

        for (XmlElement xmlChild : (Iterable<XmlElement>) xmlElement.getElementList())
            {
            String sId = xmlChild.getAttributeMap().containsKey("id")
                    ? xmlChild.getAttribute("id").getString() : new UUID().toString();

            if (sId.trim().length() == 0)
                {
                sId = new UUID().toString();
                }

            mapResult.put(sId, processElement(xmlChild));
            }

        return mapResult;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> processForeignElementsOf(XmlElement xmlElement)
            throws ConfigurationException
        {
        String sPrefix = xmlElement.getQualifiedName().getPrefix();

        // process all the children of the xmlElement
        LinkedHashMap<String, Object> mapResult = new LinkedHashMap<>();

        for (XmlElement xmlChild : (Iterable<XmlElement>) xmlElement.getElementList())
            {
            if (!xmlChild.getQualifiedName().getPrefix().equals(sPrefix))
                {
                String sId = xmlChild.getAttributeMap().containsKey("id")
                        ? xmlChild.getAttribute("id").getString() : new UUID().toString();

                if (sId.trim().length() == 0)
                    {
                    sId = new UUID().toString();
                    }

                mapResult.put(sId, processElement(xmlChild));
                }
            }

        return mapResult;
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ?> processRemainingElementsOf(XmlElement xmlElement)
            throws ConfigurationException
        {
        // process all the children of the xmlElement that aren't in the set of elements we've already processed
        LinkedHashMap<String, Object> mapResult = new LinkedHashMap<>();

        for (XmlElement xmlChild : (Iterable<XmlElement>) xmlElement.getElementList())
            {
            if (!m_setProcessedChildElements.contains(xmlChild))
                {
                String sId = xmlChild.getAttributeMap().containsKey("id")
                        ? xmlChild.getAttribute("id").getString() : new UUID().toString();

                if (sId.trim().length() == 0)
                    {
                    sId = new UUID().toString();
                    }

                mapResult.put(sId, processElement(xmlChild));
                }
            }

        return mapResult;
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T processRemainingElementOf(XmlElement xmlElement)
            throws ConfigurationException
        {
        Map<String, ?> mapResults = processRemainingElementsOf(xmlElement);
        int            cSize      = mapResults.size();

        if (cSize == 1)
            {
            return (T) mapResults.values().iterator().next();
            }
        else
            {
            throw new ConfigurationException(String.format(
                "Expected a single remaining element to process in %s after processing the elements %s but there were %d elements remaining",
                xmlElement, xmlElement.getQualifiedName(), m_setProcessedChildElements.size()), String.format(
                    "The ElementProcessor implementation for %s makes an incorrect assumption about the number of remaining elements.",
                    xmlElement.getQualifiedName()));
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPropertyDefined(String sPropertyName, XmlElement xmlParent)
            throws ConfigurationException
        {
        if (sPropertyName == null || xmlParent == null)
            {
            return false;
            }

        // assume that a property in "this context" (ie: .) is always found
        boolean fPropertyFound = sPropertyName.equals(".");

        // STEP 1: Attempt to find the property value using an XmlAttribute defined by the XmlElement
        if (!fPropertyFound)
            {
            fPropertyFound = getPropertyAttribute(xmlParent, sPropertyName) != null;
            }

        // STEP 2: Attempt to find the property value using content defined by the XmlElement
        if (!fPropertyFound)
            {
            fPropertyFound = getPropertyElement(xmlParent, sPropertyName) != null;
            }

        return fPropertyFound;
        }

    /**
 * {@inheritDoc}
 */
    @Override
    public <B> B inject(B bean, XmlElement xmlElement)
            throws ConfigurationException
        {
        for (Method method : bean.getClass().getMethods())
            {
            Type[] aParameterTypes = method.getGenericParameterTypes();

            // we can only inject into methods annotated with @Injectable
            Injectable annInjectable = method.getAnnotation(Injectable.class);

            if (annInjectable != null)
                {
                if (aParameterTypes.length == 0 && !method.getReturnType().equals(Void.TYPE))
                    {
                    // perform getter injection (ie: inject into the value returned by the getter)
                    Object oInjectable = null;

                    try
                        {
                        // determine the injectable from the getter
                        oInjectable = method.invoke(bean);

                        // we can only inject into an object that's not a bean
                        if (oInjectable != null && oInjectable != bean)
                            {
                            // decide how to locate the XmlElement containing the content to use for injection
                            String     sPropertyName = annInjectable.value();

                            XmlElement xmlProperty;

                            if (sPropertyName.isEmpty())
                                {
                                // when no name is specified we (automatically)
                                // determine the property name based on the method name
                                sPropertyName = getPropertyName(method);
                                }

                            if (sPropertyName.equals("."))
                                {
                                // when a "." we use "this" context for injection content
                                xmlProperty = xmlElement;
                                }
                            else
                                {
                                // find the element based on the name of the property
                                xmlProperty = getPropertyElement(xmlElement, sPropertyName);
                                }

                            if (xmlProperty != null)
                                {
                                inject(oInjectable, xmlProperty);
                                }
                            }
                        }
                    catch (ConfigurationException e)
                        {
                        throw e;
                        }
                    catch (Exception e)
                        {
                        throw new ConfigurationException(String.format(
                            "Could not inject a value into the instance '%s' using reflection "
                            + " produced by the annotated method '%s' of '%s'", oInjectable, method,
                                bean.getClass().getName()), "Please resolve the causing exception.", e);
                        }
                    }
                else if (aParameterTypes.length == 1)
                    {
                    // perform setter injection (ie: call a setter with a value)

                    // use the method name (or @Injectable) to determine the property name
                    String sPropertyName = getPropertyName(method);

                    // use the setter parameter type to determine the property type

                    // the type of the property
                    Type typeProperty = aParameterTypes[0];

                    // the value of the property
                    Value value = getPropertyValue(sPropertyName, typeProperty, xmlElement, false);

                    if (value != null)
                        {
                        // use the defined value to inject
                        Object oPropertyValue = value.get();

                            {
                            try
                                {
                                method.invoke(bean, oPropertyValue);
                                }
                            catch (Exception e)
                                {
                                throw new ConfigurationException(String.format(
                                    "Could not inject the property '%s' using reflection "
                                    + "with the annotated method '%s' of '%s'", sPropertyName, method,
                                        bean.getClass().getName()), "Please resolve the causing exception.", e);
                                }
                            }
                        }
                    }
                }
            }

        return bean;
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T getMandatoryProperty(String sPath, Type typeProperty, XmlElement xmlParent)
            throws ConfigurationException
        {
        Value value = getPropertyValue(sPath, typeProperty, xmlParent, true);

        if (value == null)
            {
            // when we can't find a value as an attribute or element we must throw an exception.
            throw new ConfigurationException(String.format(
                "The expected property [%s] is not defined in element [%s].", sPath, xmlParent), String.format(
                "Please consult the documentation for the use of the %s namespace",
                m_xmlElement.getQualifiedName().getPrefix()));

            }
        else
            {
            return (T) value.get();
            }
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOptionalProperty(String sPropertyName, Type typeProperty, T defaultValue, XmlElement xmlElement)
            throws ConfigurationException
        {
        Value value = getPropertyValue(sPropertyName, typeProperty, xmlElement, true);

        if (value == null)
            {
            return defaultValue;
            }
        else
            {
            return (T) value.get();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void loadNamespaceHandlers(XmlElement xmlElement)
        {
        int cAttribute = xmlElement.getAttributeMap().size();
        if (cAttribute == 0)
            {
            // nothing to load
            return;
            }

        ArrayList<XmlAttribute> lstAttributes = new ArrayList<>(cAttribute);

        for (String sAttributeName : ((Set<String>) xmlElement.getAttributeMap().keySet()))
            {
            lstAttributes.add(new SimpleAttribute(xmlElement, sAttributeName, xmlElement.getAttribute(sAttributeName)));
            }

        for (XmlAttribute attribute : lstAttributes)
            {
            QualifiedName qnAttribute = attribute.getQualifiedName();

            // only process "xmlns" declarations
            if (qnAttribute.getLocalName().equals("xmlns"))
                {
                String sURI = attribute.getXmlValue().getString();

                try
                    {
                    // ensure that the declared Xml Namespaces are available in the context of this element.
                    ensureNamespaceHandler(qnAttribute.getPrefix(), new URI(sURI));
                    }
                catch (URISyntaxException uriSyntaxException)
                    {
                    throw new ConfigurationException(String.format("Invalid URI '%s' specified for Xml Namespace '%s'",
                        sURI, qnAttribute.getPrefix()), "You must specify a valid URI for the Xml Namespace.",
                            uriSyntaxException);
                    }
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public NamespaceHandler ensureNamespaceHandler(String sPrefix, NamespaceHandler handler)
            throws ConfigurationException
        {
        URI uri = m_mapNamespaceURIsByPrefix.get(sPrefix);

        if (uri == null)
            {
            try
                {
                uri = new URI("class://" + handler.getClass().getName());
                }
            catch (URISyntaxException e)
                {
                throw new ConfigurationException(String.format(
                    "Failed to create a valid URI for the specified namespace class [%s] with prefix [%s]",
                    handler.getClass().getName(), sPrefix), "The implemented URI encoding is invalid", e);
                }

            m_mapNamespaceURIsByPrefix.put(sPrefix, uri);
            m_mapNamespaceHandlersByURI.put(uri, handler);

            // call-back the NamespaceHandler
            handler.onStartNamespace(this, m_xmlElement, sPrefix, uri);

            return handler;
            }
        else
            {
            return m_mapNamespaceHandlersByURI.get(uri);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public NamespaceHandler ensureNamespaceHandler(String sPrefix, URI uri)
            throws ConfigurationException
        {
        // determine if the NamespaceHandler for the specified URI is already defined
        NamespaceHandler namespaceHandler = getNamespaceHandler(uri);

        if (namespaceHandler == null || !getNamespaceURI(sPrefix).equals(uri))
            {
            String sScheme = uri.getScheme();

            // ensure that we don't already have a NamespaceHandler with the specified prefix in the context
            if (m_mapNamespaceURIsByPrefix.containsKey(sPrefix))
                {
                throw new ConfigurationException(String.format(
                    "Duplicate definition for the namespace prefix [%s] and URI [%s] encountered in the element [%s]",
                    sPrefix, uri, m_xmlElement), "Duplicate definitions of namespaces is not permitted.");
                }
            else if (sScheme.equals("class"))
                {
                String sClassName = (uri.getHost() == null) ? uri.getSchemeSpecificPart() : uri.getHost();

                try
                    {
                    Class<?> clzNamespaceHandler = ExternalizableHelper.loadClass(sClassName,
                                                       m_dependencies.getContextClassLoader(), null);

                    // ensure that the class is a NamespaceHandler
                    if (NamespaceHandler.class.isAssignableFrom(clzNamespaceHandler))
                        {
                        try
                            {
                            // find the no args constructor for the NamespaceHandler
                            Constructor<NamespaceHandler> constructor =
                                (Constructor<NamespaceHandler>) clzNamespaceHandler.getConstructor();

                            // instantiate the NamespaceHandler
                            namespaceHandler = constructor.newInstance();

                            // register the NamespaceHandler with this context
                            m_mapNamespaceHandlersByURI.put(uri, namespaceHandler);

                            // register the prefix for the NamespaceHandler
                            m_mapNamespaceURIsByPrefix.put(sPrefix, uri);

                            // call-back the NamespaceHandler
                            namespaceHandler.onStartNamespace(this, m_xmlElement, sPrefix, uri);

                            return namespaceHandler;
                            }
                        catch (Exception exception)
                            {
                            throw new ConfigurationException(String.format("Can't instantiate the NamespaceHandler [%s]\n",
                                sClassName), "Please ensure that the specified class is public and has a no-args constructor", exception);
                            }
                        }
                    else
                        {
                        throw new ConfigurationException(String
                            .format("The declared class [%s] does not implement the %s interface", sClassName, NamespaceHandler.class
                                .getName()), "To use a class as a NamespaceHandler it must implement the appropriate interface.");
                        }
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new ConfigurationException(String.format(
                        "Can't instantiate the NamespaceHandler [%s] as the class is not found\n",
                        sClassName), "Please ensure that the specified class is an instance of NamespaceHandler interface", e);
                    }
                }
            else if (sScheme.equalsIgnoreCase("http") || sScheme.equalsIgnoreCase("https"))
                {
                // for http/https-based schemes, we assume there is a NamespaceHandler already registered
                return getNamespaceHandler(sPrefix);
                }
            else
                {
                throw new ConfigurationException(String.format(
                    "Can't instantiate a suitable NamespaceHandler as the URI [%s] scheme is unknown.\n",
                    uri), "Please ensure that the specified URI refers to a class that implements the NamespaceHandler interface");
                }
            }
        else
            {
            return namespaceHandler;
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public NamespaceHandler getNamespaceHandler(String sPrefix)
        {
        URI uri = getNamespaceURI(sPrefix);

        return uri == null ? null : getNamespaceHandler(uri);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public NamespaceHandler getNamespaceHandler(URI uri)
        {
        NamespaceHandler namespaceHandler = m_mapNamespaceHandlersByURI.get(uri);

        return namespaceHandler == null
               ? (isRootContext() ? null : m_ctxParent.getNamespaceHandler(uri)) : namespaceHandler;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getNamespaceURI(String sPrefix)
        {
        URI uri = m_mapNamespaceURIsByPrefix.get(sPrefix);

        return uri == null ? (isRootContext() ? null : m_ctxParent.getNamespaceURI(sPrefix)) : uri;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<NamespaceHandler> getNamespaceHandlers()
        {
        LinkedHashMap<URI, NamespaceHandler> mapNamespaceHandlersByURI = new LinkedHashMap<>();
        DefaultProcessingContext             ctx                       = this;

        while (ctx != null)
            {
            for (Entry<URI, NamespaceHandler> e : ctx.m_mapNamespaceHandlersByURI.entrySet())
                {
                if (!mapNamespaceHandlersByURI.containsKey(e.getKey()))
                    {
                    mapNamespaceHandlersByURI.put(e.getKey(), e.getValue());
                    }
                }

            ctx = ctx.m_ctxParent;
            }

        return mapNamespaceHandlersByURI.values();
        }

    // ----- DefaultProcessingContext methods -------------------------------

    /**
     * Obtains the {@link DocumentProcessor} {@link DocumentProcessor.Dependencies} for the {@link ProcessingContext}.
     *
     * @return the {@link DocumentProcessor.Dependencies}
     */
    public Dependencies getDependencies()
        {
        return m_dependencies;
        }

    /**
     * Attempts to resolve the named property of the specified type in the current context and
     * if required will parse the specified {@link XmlElement} in order to do so.
     *
     * @param sPropertyName         the name or xml path to the property
     * @param typeProperty          the required type of the property value
     * @param xmlParent             the parent element in which the property may be found
     * @param fOnlyUsePropertyName  when <code>true</code> the specified property name must be
     *                              used resolve the property value.  when <code>false</code>
     *                              attempts may be made to resolve the property just the type
     *                              name if the specified property name doesn't resolve a property
     *
     * @return The {@link Value} representing the property or <code>null</code> if the property
     *         could not be located
     * @throws ConfigurationException  if the property was but could not be processed or is
     *                                 of the incorrect type
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Value getPropertyValue(String sPropertyName, Type typeProperty, XmlElement xmlParent,
                                  boolean fOnlyUsePropertyName)
            throws ConfigurationException
        {
        if (sPropertyName == null)
            {
            throw new NullPointerException("Property Name can't be null");
            }

        if (typeProperty == null)
            {
            throw new NullPointerException("Property Type can't be null");
            }

        if (xmlParent == null)
            {
            throw new NullPointerException("XmlElement in which to locate a property can't be null");
            }

        // assume we haven't resolved the property value
        Object  oValue         = null;
        boolean fValueResolved = false;

        // determine concrete class for the property
        Class<?> clzProperty = ClassHelper.getClass(typeProperty);

        // STEP 1: Attempt to resolve the property value using an XmlAttribute defined by the XmlElement
        if (!sPropertyName.equals("."))
            {
            XmlAttribute xmlValue = getPropertyAttribute(xmlParent, sPropertyName);

            if (xmlValue != null)
                {
                // no matter what happens now we assume we can and have resolved the property value
                fValueResolved = true;

                // determine the AttributeProcessor to process the XmlAttribute content into the property value
                NamespaceHandler nsAttribute = getNamespaceHandler(xmlValue.getQualifiedName().getPrefix());
                AttributeProcessor<?> procAttribute = nsAttribute == null
                                                      ? null : nsAttribute.getAttributeProcessor(xmlValue);

                // when there is no AttributeProcessor, attempt to locate a type-based one
                if (procAttribute == null)
                    {
                    procAttribute = getAttributeProcessor(clzProperty);
                    }

                if (procAttribute == null)
                    {
                    // when we can't find a suitable AttributeProcessor, assume we can use the attribute as a String
                    oValue = xmlValue.getXmlValue().getString();
                    }
                else
                    {
                    oValue = procAttribute.process(this, xmlValue);
                    }
                }
            }

        // STEP 2: Attempt to resolve the property value using content defined by the XmlElement
        if (!fValueResolved)
            {
            XmlElement xmlValue = sPropertyName.equals(".") ? xmlParent : getPropertyElement(xmlParent, sPropertyName);

            if (xmlValue != null)
                {
                // remember that we've processed this element (when it's not the parent!)
                if (xmlValue != xmlParent)
                    {
                    m_setProcessedChildElements.add(xmlValue);
                    }

                // determine the ElementProcessor to process the XmlElement content into the property value
                NamespaceHandler    nsElement   = getNamespaceHandler(xmlValue.getQualifiedName().getPrefix());
                ElementProcessor<?> procElement = nsElement == null ? null : nsElement.getElementProcessor(xmlValue);

                // when there is no ElementProcessor, attempt to locate a type-based one
                if (procElement == null)
                    {
                    procElement = getElementProcessor(clzProperty);
                    }

                // when we can't find a suitable ElementProcessor, try to resolve the value another way
                // (perhaps it's a collection, an array or just plain xml)
                if (procElement == null)
                    {
                    // determine if we are dealing with a collection (or array) of some type?
                    Class<?> clzComponent = ClassHelper.getComponentType(typeProperty);

                    // the style of processing is somewhat dependent on the number of children in the element
                    int cChildren = xmlValue.getElementList().size();

                    if (clzComponent == null)
                        {
                        if (cChildren == 0)
                            {
                            // when there is no processor for the element and the element value is simply an XmlValue
                            // assume we can use the value as a String (if it's not empty)
                            if (!XmlHelper.isEmpty(xmlValue))
                                {
                                oValue = xmlValue.getString();

                                // we have now resolved the property value
                                fValueResolved = true;
                                }
                            }
                        else if (cChildren == 1)
                            {
                            // when there is no processor for the property but the property value contains a single
                            // element, process the element itself
                            oValue = processOnlyElementOf(xmlValue);

                            // we have now resolved the property value
                            fValueResolved = true;
                            }
                        else
                            {
                            // when there is no processor and there's a collection, we assume the required value is
                            // just the element (xml).
                            oValue = xmlValue;

                            // we have now resolved the property value
                            fValueResolved = true;
                            }
                        }
                    else
                        {
                        // create a collection from the element based on the children

                        // we have now resolved the property value
                        fValueResolved = true;

                        // instantiate the collection for the property (or something that is assignment compatible)
                        boolean fArray = clzProperty.isArray();

                        if (fArray)
                            {
                            oValue = Array.newInstance(clzComponent, cChildren);
                            }
                        else if (clzProperty.isInterface())
                            {
                            if (clzProperty.equals(List.class))
                                {
                                oValue = new ArrayList(cChildren);
                                }
                            else if (clzProperty.equals(Set.class))
                                {
                                oValue = new LinkedHashSet(cChildren);
                                }
                            else if (clzProperty.equals(Queue.class))
                                {
                                oValue = new ArrayDeque(cChildren);
                                }
                            else if (clzProperty.equals(SortedSet.class))
                                {
                                oValue = new TreeSet();
                                }
                            else
                                {
                                throw new ConfigurationException(String.format(
                                    "Unsupported collection type [%s] encountered with the property [%s] in [%s]",
                                    typeProperty, sPropertyName,
                                    xmlValue), "The specified interface type is not supported. Please use a more specific type.");
                                }
                            }
                        else
                            {
                            try
                                {
                                oValue = clzProperty.getDeclaredConstructor().newInstance();
                                }
                            catch (Exception e)
                                {
                                throw new ConfigurationException(String.format(
                                    "Failed to instantiate the required type [%s] for the property [%s] in [%s]",
                                    typeProperty, sPropertyName,
                                    xmlValue), "Ensure that the specified type has a public no-args constructor", e);
                                }
                            }

                        // process each of the children, adding them to the collection and ensuring they
                        // are of the correct type
                        int idx = 0;

                        for (XmlElement xmlChild : (Iterable<XmlElement>) xmlValue.getElementList())
                            {
                            Object oChild = processElement(xmlChild);

                            // ensure the child value is compatible with the component type
                            if (clzComponent.isInstance(oChild))
                                {
                                if (fArray)
                                    {
                                    ((Object[]) oValue)[idx] = oChild;
                                    }
                                else
                                    {
                                    ((Collection<Object>) oValue).add(oChild);
                                    }
                                }
                            else
                                {
                                throw new ConfigurationException(String.format("Incompatible types"
                                                                                       + "The value [%s] is not assignable to a collection of type [%s]"
                                                                                       + "as expected by the property [%s] in [%s]", oChild, clzComponent, sPropertyName,
                                                                               xmlValue), "Please ensure assignment compatible values are provided");
                                }

                            idx++;
                            }
                        }
                    }
                else
                    {
                    ConditionalElementProcessor<?> procConditional = procElement instanceof ConditionalElementProcessor
                                                                     ? ((ConditionalElementProcessor) procElement)
                                                                     : null;

                    DefaultProcessingContext ctxValue = new DefaultProcessingContext(this, xmlValue);

                    if (procConditional == null || procConditional.accepts(ctxValue, xmlValue))
                        {
                        oValue = procElement.process(ctxValue, xmlValue);

                        // we have now resolved the property value
                        fValueResolved = true;
                        }

                    ctxValue.terminate();
                    }
                }
            }

        // STEP 3: Attempt to resolve the property value using a Cookie defined by the ProcessingContext
        if (!fValueResolved)
            {
            oValue = getCookie(clzProperty, sPropertyName);

            if (oValue == null && !fOnlyUsePropertyName)
                {
                oValue = getCookie(clzProperty);
                }

            fValueResolved = oValue != null;
            }

        // STEP 4: Attempt to resolve the property value using the ResourceRegistry defined by the ProcessingContext
        if (!fValueResolved)
            {
            ResourceRegistry registry = getResourceRegistry();

            if (registry != null)
                {
                oValue = registry.getResource(clzProperty, sPropertyName);

                if (oValue == null && !fOnlyUsePropertyName)
                    {
                    oValue = registry.getResource(clzProperty);
                    }

                fValueResolved = oValue != null;
                }
            }

        // STEP 5: Perform necessary type checking and value coercion
        if (fValueResolved)
            {
            // attempt to convert the raw value into the required type
            if (oValue == null)
                {
                // we assume null is compatible with all (object-based) types
                return new Value();
                }
            else if (clzProperty.isInstance(oValue))
                {
                // assignable types don't need converting
                return new Value(oValue);
                }
            else if (clzProperty.equals(Expression.class))
                {
                // determine the resulting type of the expression (assume it's just Object.class)
                Class<?> clzResultType = Object.class;

                if (typeProperty instanceof ParameterizedType)
                    {
                    ParameterizedType typeParameterized = (ParameterizedType) typeProperty;
                    Type              typeRaw           = typeParameterized.getRawType();

                    if (typeRaw instanceof Class<?>)
                        {
                        clzResultType = ClassHelper.getClass(typeParameterized.getActualTypeArguments()[0]);
                        }
                    }

                // we need to build an expression using the value
                if (oValue instanceof String)
                    {
                    ExpressionParser parser = m_dependencies.getExpressionParser();

                    if (parser == null)
                        {
                        throw new ConfigurationException(String.format(
                            "Failed to parse the expression [%s] defined for the property [%s] in [%s] as an ExpressionParser is not defined.",
                            oValue, sPropertyName,
                            xmlParent), "An ExpressionParser must be set for the DocumentProcessor.");
                        }
                    else
                        {
                        // attempt to parse the expression
                        try
                            {
                            return new Value(parser.parse((String) oValue, clzResultType));
                            }
                        catch (ParseException e)
                            {
                            throw new ConfigurationException(String.format(
                                "Failed to parse the expression [%s] defined for the property [%s] in [%s].", oValue,
                                sPropertyName, xmlParent), "Please correct the expression syntax error.", e);
                            }
                        }
                    }
                else
                    {
                    // use a LiteralExpression to represent non-string values
                    return new Value(new LiteralExpression(oValue));
                    }
                }
            else
                {
                // use a Value as a means to convert the raw into the required value
                try
                    {
                    Value value = oValue instanceof Value ? (Value) oValue : new Value(oValue);

                    return new Value(value.as(clzProperty));
                    }
                catch (Exception e)
                    {
                    throw new ConfigurationException(String.format(
                        "Failed to convert the property [%s] value [%s] into the required type [%s] in [%s].",
                        sPropertyName, oValue, typeProperty,
                        xmlParent), "The namespace implementation for the property will need to programmatically configure the said property.", e);
                    }
                }
            }
        else
            {
            return null;
            }
        }

    /**
     * Locates the {@link XmlElement} relative to the specified base {@link XmlElement} using a '/'-delimited path
     * <p>
     * The format of the path parameter is based on a subset of the XPath specification, supporting:
     * <ul>
     *      <li>Use a leading '/' to specify the root element
     *      <li>Use '/' to specify a child path delimiter
     *      <li>Use '..' to specify a parent
     * </ul>
     * <p>
     * Elements of the path may optionally be fully qualified in a namespace.  When they are not qualified the
     * specified prefix is used as their qualifier.
     * <p>
     * If multiple child elements exist that have the specified name, then the behavior of this method is undefined, and
     * it is permitted to return any one of the matching elements, to return null, or to throw an arbitrary runtime
     * exception.
     *
     * @param xmlElement  the base {@link XmlElement} from which to locate the desired element
     * @param sPath       the path to follow from the base {@link XmlElement} to find the desired {@link XmlElement}
     * @param sPrefix     the namespace prefix to use for path elements that are not fully qualified
     *
     * @return the {@link XmlElement} with the specified path or <code>null<code> if no such a {@link XmlElement} exists
     */
    private XmlElement getElementAt(XmlElement xmlElement, String sPath, String sPrefix)
        {
        // determine the starting point for the search
        XmlElement xml;

        // strip leading and trailing white spaces from the path as they have no meaning in xml paths
        sPath = sPath == null ? "" : sPath.trim();

        if (sPath.startsWith("/"))
            {
            // a leading / means the root of the base element
            xml = xmlElement.getRoot();
            }
        else
            {
            // anything else (including an empty path) means starting at the base element
            xml = xmlElement;
            }

        // tokenize the path based on the path separator
        StringTokenizer tokens = new StringTokenizer(sPath, "/");

        // apply each of the path elements in the tokenized path
        while (xml != null && tokens.hasMoreTokens())
            {
            String sName = tokens.nextToken().trim();

            if (sName.equals(".."))
                {
                xml = xml.getParent();
                }
            else
                {
                // determine the QualifiedName of the element
                QualifiedName qName = sName.contains(":")
                                      ? new QualifiedName(sName) : new QualifiedName(sPrefix, sName);

                // locate the child with the qualified name
                xml = xml.getElement(qName.getName());
                }
            }

        return xml;
        }

    /**
     * Obtains the property content defined as an {@link XmlAttribute} based on a path relative to a specified
     * base {@link XmlElement}.
     * <p>
     * This is the primary method by which {@link XmlAttribute}-based content for properties is located.  Typically, the
     * provided path is simply the name of the attribute.  However, there are circumstances where it may be a path, in
     * which case the last defined path element (name) in the path (after a / or ..) is used as the attribute name.
     *
     * @param xmlElement  the {@link XmlElement} from which to locate the content
     * @param sPath       the path to the property content relative to the base {@link XmlElement}
     *
     * @return an {@link XmlAttribute} representing the content or <code>null</code> if the content is not found.
     */
    private XmlAttribute getPropertyAttribute(XmlElement xmlElement, String sPath)
        {
        sPath = sPath == null ? null : sPath.trim();

        if (sPath == null || sPath.isEmpty())
            {
            return null;
            }
        else
            {
            // determine the attribute name and parent element given the path
            XmlElement xmlParent;
            String     sAttributeName;
            int        iAttribute = Math.max(sPath.lastIndexOf("/"), sPath.lastIndexOf(".."));

            if (iAttribute >= 0)
                {
                sAttributeName = sPath.substring(iAttribute + (sPath.charAt(iAttribute) == '/' ? 1 : 2));
                sPath          = sPath.substring(0, iAttribute);
                xmlParent = getElementAt(xmlElement, sPath.substring(0, iAttribute),
                                         xmlElement.getQualifiedName().getPrefix());
                }
            else
                {
                sAttributeName = sPath;
                xmlParent      = m_xmlElement;
                }

            // determine the attribute value
            XmlValue value;

            if (xmlParent == null)
                {
                value = null;
                }
            else
                {
                value = xmlParent.getAttribute(sAttributeName);
                }

            return value == null ? null : new SimpleAttribute(xmlParent, sAttributeName, value);
            }
        }

    /**
     * Obtains the property content defined as an {@link XmlElement} based on a path relative to a specified
     * base {@link XmlElement}.
     * <p>
     * This is the primary method by which {@link XmlElement}-based content for properties is located.
     *
     * @param xmlElement  the {@link XmlElement} from which to locate the content
     * @param sPath       the path to the property content relative to the base {@link XmlElement}
     *
     * @return an {@link XmlElement} representing the content or <code>null</code> if the content is not found.
     */
    private XmlElement getPropertyElement(XmlElement xmlElement, String sPath)
        {
        String sPrefix = xmlElement.getQualifiedName().getPrefix();
        XmlElement element = getElementAt(xmlElement, sPath, sPrefix);
        if (element == null && sPrefix != null && !sPrefix.isEmpty())
            {
            // we didn't find the element with the parent's prefix, try with the empty prefix
            // just in case the parent has is a custom namespace, but the child is from the
            // default Coherence namespace
            // e.g. a custom namespace with a Coherence instance (class-name) in it,
            // we need to look up class-name with an empty prefix not the parent's "jk" prefix.
            // <jk:foo>
            //   <class-name>com.oracle.Foo</class-name>
            // </jk:foo>
            element = getElementAt(xmlElement, sPath, "");
            }
        return element;
        }

    /**
     * Obtain the {@link AttributeProcessor} defined for the specific type.
     *
     * @param clzType  the type of {@link AttributeProcessor} we're looking for
     *
     * @return an {@link AttributeProcessor} or <code>null</code> if not found
     */
    @SuppressWarnings("unchecked")
    <T> AttributeProcessor<T> getAttributeProcessor(Class<T> clzType)
        {
        AttributeProcessor<T> procAttribute = (AttributeProcessor<T>) m_mapAttributeProcessorsByType.get(clzType);

        if (procAttribute == null)
            {
            return isRootContext() ? null : m_ctxParent.getAttributeProcessor(clzType);
            }
        else
            {
            return procAttribute;
            }
        }

    /**
     * Obtain the {@link ElementProcessor} defined for the specific type.
     *
     * @param clzType  the type of {@link ElementProcessor} we're looking for
     *
     * @return an {@link ElementProcessor} or <code>null</code> if not found
     */
    @SuppressWarnings("unchecked")
    <T> ElementProcessor<T> getElementProcessor(Class<T> clzType)
        {
        ElementProcessor<T> procElement = (ElementProcessor<T>) m_mapElementProcessorsByType.get(clzType);

        if (procElement == null)
            {
            return isRootContext() ? null : m_ctxParent.getElementProcessor(clzType);
            }
        else
            {
            return procElement;
            }
        }

    /**
     * Determines if the {@link ProcessingContext} is the root.  ie: has no parent.
     *
     * @return <code>true</code> if the {@link ProcessingContext} is the root scope, <code>false</code> otherwise
     */
    public boolean isRootContext()
        {
        return m_ctxParent == null;
        }

    /**
     * Obtains the property name for a method, either from the @Injectable
     * annotation or the method name.
     *
     * @param method  the method which will used to form the property name
     *
     * @return the property name
     */
    String getPropertyName(Method method)
        {
        StringBuilder sbPropertyName = new StringBuilder();

        if (method.isAnnotationPresent(Injectable.class) && !method.getAnnotation(Injectable.class).value().isEmpty())
            {
            sbPropertyName.append((method.getAnnotation(Injectable.class)).value());
            }
        else
            {
            // convert method name from camelCase to hyphen format (Example setModelName/getModelName to model-name)
            String sn = method.getName().substring("set".length());

            if (!sn.isEmpty())
                {
                sbPropertyName.append(sn.substring(0, 1).toLowerCase());
                }

            // insert "-" and convert to lower case when encounter upper case
            for (int i = 1; i < sn.length(); i++)
                {
                char ch = sn.charAt(i);
                if (Character.isUpperCase(ch))
                    {
                    sbPropertyName.append("-");
                    sbPropertyName.append(Character.toLowerCase(ch));
                    }
                else
                    {
                    sbPropertyName.append(ch);
                    }
                }
            }

        // use the property path if one is defined
        String sName         = sbPropertyName.toString();
        String sPropertyPath = m_mapPropertyPaths.get(sName);

        return sPropertyPath == null ? sName : sPropertyPath;
        }

    /**
     * Terminates and end's all the {@link NamespaceHandler}s established in the {@link ProcessingContext}.
     */
    void terminate()
        {
        // dispose each of the NamespaceHandler(s) loaded in this context
        for (String sPrefix : m_mapNamespaceURIsByPrefix.keySet())
            {
            URI              uri     = m_mapNamespaceURIsByPrefix.get(sPrefix);
            NamespaceHandler handler = m_mapNamespaceHandlersByURI.get(uri);

            // ignore then case where we have a non-class uri
            if (handler != null)
                {
                handler.onEndNamespace(this, m_xmlElement, sPrefix, uri);
                }
            }
        }

    // ----- AutoCloseable interface ----------------------------------------

    @Override
    public void close()
        {
        terminate();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link DocumentProcessor} {@link DocumentProcessor.Dependencies} to be when processing content in
     * this {@link ProcessingContext} implementation.
     */
    private final Dependencies m_dependencies;

    /**
     * The parent {@link ProcessingContext}.
     */
    private DefaultProcessingContext m_ctxParent;

    /**
     * The map of {@link AttributeProcessor}s by type for this {@link ProcessingContext}.
     */
    private final HashMap<Class<?>, AttributeProcessor<?>> m_mapAttributeProcessorsByType;

    /**
     * The cookies defined in the {@link ProcessingContext} by type and name.
     */
    private final HashMap<Class<?>, HashMap<String, Object>> m_mapCookiesByType;

    /**
     * The map of {@link ElementProcessor}s by type for this {@link ProcessingContext}.
     */
    private final HashMap<Class<?>, ElementProcessor<?>> m_mapElementProcessorsByType;

    /**
     * The {@link NamespaceHandler}s for the xml namespaces registered in this {@link ProcessingContext}.
     */
    private final LinkedHashMap<URI, NamespaceHandler> m_mapNamespaceHandlersByURI;

    /**
     * The xml namespaces registered in this {@link ProcessingContext}.  A mapping from prefix to URIs
     */
    private final LinkedHashMap<String, URI> m_mapNamespaceURIsByPrefix;

    /**
     * A mapping of Property Names to an {@link XmlElement} paths.  This is used to
     * override the default behavior used to locate property values for this {@link ProcessingContext}.
     */
    private final HashMap<String, String> m_mapPropertyPaths;

    /**
     * The set of child {@link XmlElement} names with in the {@link #m_xmlElement} that have been processed thus
     * far by the {@link ProcessingContext}.
     */
    private final HashSet<XmlElement> m_setProcessedChildElements;

    /**
     * The {@link XmlElement} for which this {@link ProcessingContext} was created.
     */
    private XmlElement m_xmlElement;
    }
