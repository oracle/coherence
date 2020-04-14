/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;

import com.tangosol.run.xml.XmlAttribute;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import java.net.URI;

import java.util.HashMap;

/**
 * An {@link AbstractNamespaceHandler} provides a base implementation of a {@link NamespaceHandler}
 * with support for implicit and explicit registration of {@link ElementProcessor}s and
 * {@link AttributeProcessor}s for those xml attributes and elements occurring in the associated xml namespace.
 *
 * @author bo 2011.06.20
 * @since Coherence 12.1.2
 */
public abstract class AbstractNamespaceHandler
        implements NamespaceHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an {@link AbstractNamespaceHandler}.
     */
    public AbstractNamespaceHandler()
        {
        m_documentPreprocessor   = null;
        m_mapElementProcessors   = new HashMap<String, ElementProcessor<?>>();
        m_mapAttributeProcessors = new HashMap<String, AttributeProcessor<?>>();

        // attempt to automatically register all Element and Attribute Processors
        // that are declared as non-abstract static inner classes
        autoRegisterInnerClassProcessors();
        }

    // ----- NamespaceHandler interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    public DocumentPreprocessor getDocumentPreprocessor()
        {
        return m_documentPreprocessor;
        }

    /**
     * {@inheritDoc}
     */
    public AttributeProcessor<?> getAttributeProcessor(XmlAttribute attribute)
        {
        AttributeProcessor<?> processor = m_mapAttributeProcessors.get(attribute.getQualifiedName().getLocalName());

        return (processor == null) ? onUnknownAttribute(attribute) : processor;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ElementProcessor<?> getElementProcessor(XmlElement element)
        {
        ElementProcessor<?> processor = m_mapElementProcessors.get(element.getQualifiedName().getLocalName());

        return (processor == null) ? onUnknownElement(element) : processor;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartNamespace(ProcessingContext context, XmlElement element, String prefix, URI uri)
        {
        /**
         * Override this method to provide specific NamespaceHandler declaration semantics.
         */
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEndNamespace(ProcessingContext context, XmlElement element, String prefix, URI uri)
        {
        /**
         * Override this method to provide specific NamespaceHandler disposal semantics
         */
        }

    // ----- AbstractNamespaceHandler interface -----------------------------

    /**
     * Sets the {@link DocumentPreprocessor} for the {@link NamespaceHandler}.
     *
     * @param preprocessor  the {@link DocumentPreprocessor}
     */
    public void setDocumentPreprocessor(DocumentPreprocessor preprocessor)
        {
        m_documentPreprocessor = preprocessor;
        }

    /**
     * Registers the specified processor as an {@link ElementProcessor} or 
     * {@link AttributeProcessor} (based on the interfaces it implements) 
     * using the {@link XmlSimpleName} annotation to determine the localName
     * of the said processor.
     * <p>
     * Note: The specified class must have a no-args constructor.
     *
     * @param clzProcessor  A class that implements either (or both) 
     * 						{@link ElementProcessor} or {@link AttributeProcessor}
     *                      that is annotated with @{@link XmlSimpleName} to 
     *                      specify it's localName.
     */
    public void registerProcessor(Class<?> clzProcessor)
        {
        if (clzProcessor.isAnnotationPresent(XmlSimpleName.class)
            && (ElementProcessor.class.isAssignableFrom(clzProcessor)
                || AttributeProcessor.class.isAssignableFrom(clzProcessor)))
            {
            try
                {
                String sLocalName = clzProcessor.getAnnotation(XmlSimpleName.class).value();
                Object oProcessor = clzProcessor.newInstance();

                if (oProcessor instanceof ElementProcessor)
                    {
                    registerProcessor(sLocalName, (ElementProcessor<?>) oProcessor);
                    }

                if (oProcessor instanceof AttributeProcessor)
                    {
                    registerProcessor(sLocalName, (AttributeProcessor<?>) oProcessor);
                    }
                }
            catch (InstantiationException e)
                {
                Base.ensureRuntimeException(e, "Specified processor class requires a no-args constructor.");
                }
            catch (IllegalAccessException e)
                {
                Base.ensureRuntimeException(e);
                }
            }
        }

    /**
     * Registers an {@link ElementProcessor} for {@link XmlElement}s with a name with in the context of
     * the {@link NamespaceHandler} namespace.
     *
     * @param sLocalName  The local name of the {@link XmlElement} to be processed with the {@link ElementProcessor}
     * @param processor   The {@link ElementProcessor}
     */
    public void registerProcessor(String sLocalName, ElementProcessor<?> processor)
        {
        m_mapElementProcessors.put(sLocalName, processor);
        }

    /**
     * Registers an {@link AttributeProcessor} for {@link XmlAttribute}s with the specified name.
     *
     * @param sLocalName  The local name of the {@link XmlAttribute} to be processed with the {@link AttributeProcessor}
     * @param processor   The {@link AttributeProcessor}
     */
    public void registerProcessor(String sLocalName, AttributeProcessor<?> processor)
        {
        m_mapAttributeProcessors.put(sLocalName, processor);
        }

    /**
     * Registers (internally creates) an appropriate {@link ElementProcessor} for {@link XmlElement}s with
     * the specified local name so that they produce the specified type of value when processed.
     *
     * @param <T>         the type of value the registered {@link ElementProcessor} will produce
     * @param sLocalName  The local name of the {@link XmlElement}s to be associated with the type
     * @param clzType     The {@link Class} of value that should be produced for the {@link XmlElement}
     */
    @SuppressWarnings("unchecked")
    public <T> void registerElementType(String sLocalName, Class<T> clzType)
        {
        if (ElementProcessor.class.isAssignableFrom(clzType))
            {
            try
                {
                ElementProcessor<T> processor = (ElementProcessor<T>) clzType.newInstance();

                registerProcessor(sLocalName, processor);
                }
            catch (Exception exception)
                {
                throw new RuntimeException(String.format("Can't instantiate the ElementProcessor [%s]\n", clzType),
                                           exception);
                }
            }
        else
            {
            // default to the SimpleElementHandler with specified type
            registerProcessor(sLocalName, new SimpleElementProcessor<T>(clzType));
            }
        }

    /**
     * Registers (internally creates) an appropriate {@link AttributeProcessor} for {@link XmlAttribute}s with
     * the specified local name so that they produce the specified type of value when processed.
     *
     * @param <T>         the type of value the registered {@link AttributeProcessor} will produce
     * @param sLocalName  The local name of the {@link XmlElement}s to be associated with the type
     * @param clzType     The {@link Class} of value that should be produced for the {@link XmlAttribute}
     */
    @SuppressWarnings("unchecked")
    public <T> void registerAttributeType(String sLocalName, Class<T> clzType)
        {
        if (AttributeProcessor.class.isAssignableFrom(clzType))
            {
            try
                {
                AttributeProcessor<T> processor = (AttributeProcessor<T>) clzType.newInstance();

                registerProcessor(sLocalName, processor);
                }
            catch (Exception exception)
                {
                throw new RuntimeException(String.format("Can't instantiate the AttributeProcessor [%s]\n", clzType),
                                           exception);
                }
            }
        else
            {
            registerProcessor(sLocalName, new SimpleAttributeProcessor<T>(clzType));
            }
        }

    /**
     * A call-back to handle when an {@link XmlAttribute} is unknown to the {@link NamespaceHandler}.
     * <p>
     * Override this method to provide specialized foreign {@link XmlAttribute} processing.  By default,
     * <code>null</code> will be returned for unknown {@link XmlAttribute}s.
     *
     * @param attribute  The {@link XmlAttribute} that was unknown.
     *
     * @return An appropriate {@link AttributeProcessor} that may be used to process the unknown {@link XmlAttribute}
     *         or <code>null</code> if no special processing should occur.
     */
    protected AttributeProcessor<?> onUnknownAttribute(XmlAttribute attribute)
        {
        // SKIP: by default we don't do anything if the attribute is unknown
        return null;
        }

    /**
     * A call-back to handle when an {@link XmlElement} is unknown to the {@link NamespaceHandler}.
     * <p>
     * Override this method to provide specialized foreign {@link XmlElement} processing.
     * By default, unknown {@link XmlElement} will return an {@link ElementProcessor} that when attempting
     * to process the said element, will throw a {@link ConfigurationException}.
     *
     * @param element  The {@link XmlElement} that was unknown.
     *
     * @return An appropriate {@link ElementProcessor} that may be used to process the unknown {@link XmlElement}
     *         or <code>null</code> if no special processing should occur.
     */
    protected ElementProcessor<?> onUnknownElement(XmlElement element)
        {
        // SKIP: by default we don't do anything if the element is unknown
        return null;
        }

    /**
     * Obtains the {@link AttributeProcessor} registered with the specified
     * localName (in the namespace).
     *
     * @param localName  the name of the {@link AttributeProcessor} to return
     *
     * @return the {@link AttributeProcessor} or <code>null</code> if not found
     */
    public AttributeProcessor<?> getAttributeProcessor(String localName)
        {
        return m_mapAttributeProcessors.get(localName);
        }

    /**
     * Obtains the {@link ElementProcessor} registered with the specified
     * localName (in the namespace).
     *
     * @param localName  the name of the {@link ElementProcessor} to return
     *
     * @return the {@link ElementProcessor} or <code>null</code> if not found
     */
    public ElementProcessor<?> getElementProcessor(String localName)
        {
        return m_mapElementProcessors.get(localName);
        }

    /**
     * Using reflection, automatically locates and registers each of the inner-class {@link ElementProcessor}
     * and {@link AttributeProcessor} definitions with the {@link NamespaceHandler} namespace.
     */
    void autoRegisterInnerClassProcessors()
        {
        for (Class<?> clzDeclared : getClass().getDeclaredClasses())
            {
            if (!clzDeclared.isInterface() && !clzDeclared.isAnnotation())
                {
                String sProcessorName = getProcessorName(clzDeclared);

                if (sProcessorName != null && !sProcessorName.isEmpty()
                    && (ElementProcessor.class.isAssignableFrom(clzDeclared)
                        || AttributeProcessor.class.isAssignableFrom(clzDeclared)))
                    {
                    try
                        {
                        Object oProcessor;

                        if (Modifier.isStatic(clzDeclared.getModifiers()))
                            {
                            // static inner class
                            oProcessor = clzDeclared.newInstance();
                            }
                        else
                            {
                            // non-static inner class
                            Constructor<?> constructor = clzDeclared.getConstructor(new Class[] {getClass()});

                            oProcessor = constructor.newInstance(new Object[] {this});
                            }

                        if (ElementProcessor.class.isAssignableFrom(clzDeclared))
                            {
                            registerProcessor(sProcessorName, (ElementProcessor<?>) oProcessor);
                            }

                        if (AttributeProcessor.class.isAssignableFrom(clzDeclared))
                            {
                            registerProcessor(sProcessorName, (AttributeProcessor<?>) oProcessor);
                            }
                        }
                    catch (Exception exception)
                        {
                        throw new RuntimeException(String.format("Can't instantiate the Processor [%s]\n",
                            clzDeclared), exception);
                        }
                    }
                }
            }
        }

    /**
     * Determine xml element/attribute name of the specified {@link ElementProcessor}
     * / {@link AttributeProcessor} non-abstract {@link Class} that has
     * been annotated by {@link XmlSimpleName}.
     * <p>
     * A name for the processor is only returned if the specified class either
     * implements the {@link ElementProcessor} or {@link AttributeProcessor} interfaces
     * and is non-abstract with an {@link XmlSimpleName} annotation.  Should any
     * of these conditions fail, <code>null</code> is returned.
     *
     * @param clzProcessor  The {@link Class} from which to determine an appropriate name.
     *
     * @return The name of the processor
     */
    String getProcessorName(Class<?> clzProcessor)
        {
        if (clzProcessor != null && clzProcessor.isAnnotationPresent(XmlSimpleName.class))
            {
            // use the name as specified by the annotation
            String sProcessorName = ((XmlSimpleName) clzProcessor.getAnnotation(XmlSimpleName.class)).value();

            // now ensure that the class is non-abstract, static with a no-args constructor
            int nModifiers = clzProcessor.getModifiers();

            if (Modifier.isAbstract(nModifiers) || clzProcessor.isAnnotation() || clzProcessor.isInterface())
                {
                return null;
                }
            else
                {
                return sProcessorName;
                }
            }
        else
            {
            return null;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The registered {@link DocumentPreprocessor} for the {@link NamespaceHandler}.
     */
    private DocumentPreprocessor m_documentPreprocessor;

    /**
     * The registered {@link AttributeProcessor}(s) for the {@link NamespaceHandler}.
     */
    private HashMap<String, AttributeProcessor<?>> m_mapAttributeProcessors;

    /**
     * The registered {@link ElementProcessor}(s) for the {@link NamespaceHandler}.
     */
    private HashMap<String, ElementProcessor<?>> m_mapElementProcessors;
    }
