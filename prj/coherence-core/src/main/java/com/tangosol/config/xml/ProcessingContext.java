/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.ExpressionParser;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.run.xml.XmlAttribute;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.ResourceResolver;

import java.lang.reflect.Type;

import java.net.URI;

import java.util.Map;

/**
 * A {@link ProcessingContext} provides contextual information concerning the processing of content in an xml document.
 * <p>
 * {@link ProcessingContext}s additionally provide mechanisms to:
 * <ol>
 *      <li>Request processing of other, typically sub xml content, like child {@link XmlElement}s.
 *
 *      <li>Access the {@link ResourceRegistry} provided to the {@link DocumentProcessor} associated with the
 *          {@link XmlDocument} being processed.
 *
 *      <li>Access the {@link ParameterResolver} provided to the {@link DocumentProcessor} associated with the
 *          {@link XmlDocument} being processed.
 *
 *      <li>Define additional contextual state (such as cookies) that may be used for injecting values into beans
 *          during processing.
 *
 *      <li>Define specialized {@link ElementProcessor}s and {@link AttributeProcessor}s that may be used for
 *          processing and injecting beans in the {@link ProcessingContext}.
 *  </ol>
 *
 * @author bo  2011.06.15
 * @since Coherence 12.1.2
 */
public interface ProcessingContext
        extends ResourceResolver
    {
    /**
     * Obtains the {@link ResourceRegistry} associated with the {@link ProcessingContext}.
     *
     * @return a {@link ResourceRegistry}
     */
    public ResourceRegistry getResourceRegistry();

    /**
     * Obtains the {@link ClassLoader} to use for loading classes in the {@link ProcessingContext}.
     *
     * @return {@link ClassLoader}
     */
    public ClassLoader getContextClassLoader();

    /**
     * Obtains the {@link ParameterResolver} to use for resolving parameters
     * defined externally to the document being processed (ie: the
     * operating system or container)
     *
     * @return the default {@link ParameterResolver}.
     */
    public ParameterResolver getDefaultParameterResolver();

    /**
     * Adds the specified named and typed cookie to the {@link ProcessingContext}.
     * <p>
     * If a cookie with the same name and type exists in the {@link ProcessingContext}, it will be replaced by the
     * specified cookie. If a cookie with the same name and type has been defined in an outer {@link ProcessingContext},
     * the specified cookie will hide the cookie defined in the outer {@link ProcessingContext}.
     *
     * @param <T>          the type of the cookie
     * @param clzCookie    the class of the cookie
     * @param sCookieName  the name of the cookie
     * @param cookie       the cookie value
     */
    public <T> void addCookie(Class<T> clzCookie, String sCookieName, T cookie);

    /**
     * Adds the specified cookie to the {@link ProcessingContext}.
     * <p>
     * If a cookie of the same type and name (being the name of the class of
     * the said cookie) is already registered with the {@link ProcessingContext},
     * it will be replaced by the specified cookie.
     * <p>
     * If a cookie of the same type and name has been defined in an outer
     * {@link ProcessingContext}, the specified cookie will hide the cookie
     * defined in the outer {@link ProcessingContext}.
     * <p>
     * This method is equivalent to calling:
     * <code>addCookie(clz, clz.getName(), cookie);</code>
     *
     * @param <T>          the type of the cookie
     * @param clzCookie    the class of the cookie
     * @param cookie       the cookie value
     */
    public <T> void addCookie(Class<T> clzCookie, T cookie);

    /**
     * Locates and returns the cookie with the specified type and name.
     * <p>
     * Locating the cookie involves searching the current
     * {@link ProcessingContext} for a matching cookie.  If one is not found,
     * the search continues with outer {@link ProcessingContext}s until a
     * cookie is either located or there are no more {@link ProcessingContext}s,
     * in which case <code>null</code> is returned.
     *
     * @param <T>          the type of the cookie
     * @param clzCookie    the class of the cookie
     * @param sCookieName  the name of the cookie
     *
     * @return the cookie or <code>null</code> if not defined
     */
    public <T> T getCookie(Class<T> clzCookie, String sCookieName);

    /**
     * Locates and returns the cookie with the specified type.
     * <p>
     * Locating the cookie involves searching the current
     * {@link ProcessingContext} for a matching cookie.  If one is not found,
     * the search continues with outer {@link ProcessingContext}s until a
     * cookie is either located or there are no more {@link ProcessingContext}s,
     * in which case <code>null</code> is returned.
     * <p>
     * This method is equivalent to calling:
     * <code>getCookie(clz, clz.getName());</code>
     *
     * @param <T>          the type of the cookie
     * @param clzCookie    the class of the cookie
     *
     * @return the cookie or <code>null</code> if not defined
     */
    public <T> T getCookie(Class<T> clzCookie);

    /**
     * Defines the xml path to locate a specific Java Bean property with in an {@link XmlElement} in the
     * {@link ProcessingContext}.
     * <p>
     * This method allows "alias" paths for Java Bean properties to be defined so that {@link #inject(Object, XmlElement)}
     * calls may resolve property values correctly.
     * <p>
     * This is an advanced feature.  Typically this is only used when:
     * <ol>
     *      <li>A Java Bean property name does not match a named value, {@link XmlElement} or {@link XmlAttribute}
     *          with in a {@link ProcessingContext}.
     *      <li>A Java Bean property can not be located with in the {@link ProcessingContext}.
     * </ol>
     *
     * @param sBeanPropertyName  the property name of the bean
     * @param sXmlPath           the xmlPath to the property value
     */
    public void definePropertyPath(String sBeanPropertyName, String sXmlPath);

    /**
     * Registers an {@link AttributeProcessor} that may be used to process specific types of values
     * contained in {@link XmlAttribute}s with in the {@link ProcessingContext}.
     * <p>
     * When an {@link AttributeProcessor} isn't provided by the associated {@link NamespaceHandler} for an
     * {@link XmlAttribute}, an attempt is made to use a type specific {@link AttributeProcessor} to process an
     * {@link XmlAttribute} for injection (with {@link #inject(Object, XmlElement)}).
     *
     * @param <T>        the type
     * @param clzType    the {@link Class} type
     * @param processor  the {@link AttributeProcessor} for the type
     */
    public <T> void registerProcessor(Class<T> clzType, AttributeProcessor<T> processor);

    /**
     * Registers an {@link ElementProcessor} that may be used to process specific types of values
     * contained in {@link XmlElement}s with in the {@link ProcessingContext}.
     * <p>
     * When an {@link ElementProcessor} isn't provided by the associated {@link NamespaceHandler} for an
     * {@link XmlElement}, an attempt is made to use a type specific {@link ElementProcessor} to process an
     * {@link XmlElement} for injection (with {@link #inject(Object, XmlElement)}).
     *
     * @param <T>        the type
     * @param clzType    the {@link Class} type
     * @param processor  the {@link ElementProcessor} for the type
     */
    public <T> void registerProcessor(Class<T> clzType, ElementProcessor<T> processor);

    /**
     * Automatically creates and registers an {@link AttributeProcessor} for the specified type.
     * <p>
     * Note: This assumes the type supports a {@link String}-based or {@link XmlAttribute}-based constructor.
     *
     * @param <T>      the type
     * @param clzType  the type for which to create and register an {@link AttributeProcessor}
     */
    public <T> void registerAttributeType(Class<T> clzType);

    /**
     * Automatically creates and registers an {@link ElementProcessor} for the specified type.
     * <p>
     * Note: This assumes the type supports a {@link String}-based or {@link XmlElement}-based constructor.
     *
     * @param <T>      the type
     * @param clzType  the type for which to create and register an {@link ElementProcessor}
     */
    public <T> void registerElementType(Class<T> clzType);

    /**
     * Obtains the configured {@link ExpressionParser} for this {@link ProcessingContext}.
     *
     * @return the {@link ExpressionParser}
     */
    public ExpressionParser getExpressionParser();

    /**
     * Ensures that an {@link NamespaceHandler} with the specified {@link URI} is available for use in the
     * {@link ProcessingContext} with the specified prefix.  If a {@link NamespaceHandler} with the specified
     * prefix and {@link URI} is not defined by the {@link ProcessingContext}, one is instantiated, registered
     * and returned.
     *
     * @param sPrefix  the prefix of the Xml Namespace to use for the {@link NamespaceHandler}
     * @param uri      the {@link URI} detailing the location of the {@link NamespaceHandler}. Typically this
     *                 will be a java class URI, specified as "class://fully.qualified.class.name"
     *
     * @return an instance of the {@link NamespaceHandler} that is suitable for processing the prefix and {@link URI}
     *
     * @throws ConfigurationException when a configuration problem was encountered
     */
    public NamespaceHandler ensureNamespaceHandler(String sPrefix, URI uri);

    /**
     * Ensures that the specified {@link NamespaceHandler} for the specified prefix is defined in this
     * {@link ProcessingContext}.
     * <p>
     * If a {@link NamespaceHandler} for the prefix does not exist in the {@link ProcessingContext}, it is added.
     * Otherwise the existing {@link NamespaceHandler} for the prefix is returned.
     *
     * @param sPrefix  the prefix of the xml Namespace to be associated with the {@link NamespaceHandler}
     * @param handler  the {@link NamespaceHandler}
     *
     * @return the registered {@link NamespaceHandler} for the {@link ProcessingContext}
     */
    public NamespaceHandler ensureNamespaceHandler(String sPrefix, NamespaceHandler handler);

    /**
     * Load all the custom {@link NamespaceHandler} instances for the specified {@link XmlElement}.
     *
     * @param xmlElement  the {@link XmlElement} to load {@link NamespaceHandler} instances for
     *
     * @throws NullPointerException if the {@code xmlElement} parameter is {@code null}
     */
    public void loadNamespaceHandlers(XmlElement xmlElement);

    /**
     * Obtains the {@link NamespaceHandler} that is capable of processing the namespace defined with the specified
     * {@link URI}.
     *
     * @param uri  the Xml Namespace {@link URI} of the {@link NamespaceHandler} to locate
     *
     * @return <code>null</code> if a {@link NamespaceHandler} could not be located for the specified {@link URI}
     */
    public NamespaceHandler getNamespaceHandler(URI uri);

    /**
     * Obtains the {@link NamespaceHandler} which is capable of processing the namespace with the specified prefix.
     *
     * @param sPrefix  the prefix of the xml namespace
     *
     * @return <code>null</code> if a {@link NamespaceHandler} could not be located for the specified prefix
     */
    public NamespaceHandler getNamespaceHandler(String sPrefix);

    /**
     * Obtains the {@link URI} that is associated with the specified prefix.
     *
     * @param sPrefix  the XML namespace prefix of the {@link URI} to locate
     *
     * @return <code>null</code> if a {@link URI} could not be located for the specified {@link URI}
     */
    public URI getNamespaceURI(String sPrefix);

    /**
     * Obtains the {@link NamespaceHandler}s that are currently in scope
     * for this {@link ProcessingContext}.
     *
     * @return  An {@link Iterable} over the {@link NamespaceHandler}s in scope.
     */
    public Iterable<NamespaceHandler> getNamespaceHandlers();

    /**
     * Request that the document specified by the URI/filename
     * (containing the <strong>root</strong> of an XmlDocument)
     * be processed with appropriate {@link NamespaceHandler}s.
     * <p>
     * Should the document root contain any unrecognized xml namespaces,
     * an attempt will be made to load appropriate {@link NamespaceHandler}s
     * that of which will be used to process said elements in the document.
     *
     * @param uri  the {@link URI} of the XmlDocument to process
     *
     * @return the result of the processing the root element of the document
     *
     * @throws ConfigurationException when a configuration problem was encountered
     */
    public Object processDocumentAt(URI uri)
            throws ConfigurationException;

    /**
     * Request that the document specified by the URI/filename
     * (containing the <strong>root</strong> of an XmlDocument)
     * be processed with appropriate {@link NamespaceHandler}s.
     * <p>
     * Should the document root contain any unrecognized xml namespaces,
     * an attempt will be made to load appropriate {@link NamespaceHandler}s
     * that of which will be used to process said elements in the document.
     *
     * @param sLocation  the URI/filename of the XmlDocument to process
     *
     * @return the result of the processing the root element of the document
     *
     * @throws ConfigurationException when a configuration problem was encountered
     */
    public Object processDocumentAt(String sLocation)
            throws ConfigurationException;

    /**
     * Request that the specified {@link XmlElement}
     * (representing the <strong>root</strong> of an XmlDocument)
     * be processed with appropriate {@link NamespaceHandler}s.
     * <p>
     * Should the document root contain any unrecognized xml namespaces,
     * an attempt will be made to load appropriate {@link NamespaceHandler}s
     * that of which will be used to process said elements in the document.
     *
     * @param xmlElement  the root {@link XmlElement} of the XmlDocument to process
     *
     * @return the result of the processing the root element of the document
     *         represented by the {@link XmlElement}
     *
     * @throws ConfigurationException when a configuration problem was encountered
     */
    public Object processDocument(XmlElement xmlElement)
            throws ConfigurationException;

    /**
     * Request that the specified xml string
     * (representing an xml document) be processed with appropriate
     * {@link NamespaceHandler}s.
     * <p>
     * Should the document root contain any unrecognized xml namespaces,
     * an attempt will be made to load appropriate {@link NamespaceHandler}s
     * that of which will be used to process said elements in the document.
     *
     * @param sXml  a string containing an xml document to process
     *
     * @return the result of processing the root element of the document
     *
     * @throws ConfigurationException when a configuration problem was encountered
     */
    public Object processDocument(String sXml)
            throws ConfigurationException;

    /**
     * Request the specified {@link XmlElement} to be processed with an appropriate {@link NamespaceHandler}
     * known by the {@link ProcessingContext} or outer {@link ProcessingContext}s.
     * <p>
     * Note: Should the element contain any unrecognized xml namespaces, an attempt will be made to load
     * appropriate {@link NamespaceHandler}s that of which will be used to process said elements.
     *
     * @param xmlElement  the {@link XmlElement} to process
     *
     * @return the result of processing the {@link XmlElement} with an
     *         appropriate {@link ElementProcessor}
     *
     * @throws ConfigurationException when a configuration problem was encountered
     */
    public Object processElement(XmlElement xmlElement)
            throws ConfigurationException;

    /**
     * Request the specified xml string be processed with an appropriate {@link NamespaceHandler}
     * known by the {@link ProcessingContext}.
     * <p>
     * Note: Should the element contain any unrecognized xml namespaces, an attempt will be made to load
     * appropriate {@link NamespaceHandler}s that of which will be used to process said elements.
     *
     * @param sXml  the xml to process
     *
     * @return the result of processing the root element of the specified xml
     *
     * @throws ConfigurationException when a configuration problem was encountered
     */
    public Object processElement(String sXml)
            throws ConfigurationException;

    /**
     * Request that all of the child elements contained with in the specified {@link XmlElement} be
     * processed using appropriate {@link NamespaceHandler}s known by the {@link ProcessingContext}.
     * <p>
     * This is a convenience method to aid in the processing of all children of an {@link XmlElement}. The keys
     * of the returned {@link Map} represent the id attributes each child {@link XmlElement}.  If an {@link XmlElement}
     * does not have a specified id attribute, a UUID is generated in it's place.
     *
     * @param xmlElement  the parent {@link XmlElement} of the children to process
     *
     * @return a {@link Map} from identifiable child {@link XmlElement}s (with id="..." attributes)
     *         and their corresponding processed values
     *
     * @throws ConfigurationException when a configuration problem was encountered
     */
    public Map<String, ?> processElementsOf(XmlElement xmlElement)
            throws ConfigurationException;

    /**
     * Request that all of the child elements contained within the specified {@link XmlElement} that do not belong
     * to the namespace of the said {@link XmlElement} are processed using appropriate processes.
     * <p>
     * This is a convenience method to aid in the processing of all children of an {@link XmlElement}. The keys
     * of the returned {@link Map} represent the id attributes each child {@link XmlElement}.  If an {@link XmlElement}
     * does not have a specified id attribute, a UUID is generated in it's place.
     *
     * @param xmlElement  the parent {@link XmlElement} of the children to process
     *
     * @return a {@link Map} from identifiable child {@link XmlElement}s (with id="..." attributes)
     *         and their corresponding processed values
     *
     * @throws ConfigurationException when a configuration problem was encountered
     */
    public Map<String, ?> processForeignElementsOf(XmlElement xmlElement)
            throws ConfigurationException;

    /**
     * Request that the <strong>only</strong> child element contained within the {@link XmlElement} is processed
     * using an appropriate {@link NamespaceHandler} known by the {@link ProcessingContext}.
     *
     * @param <T>         the type
     * @param xmlElement  the {@link XmlElement} in which the child is defined
     *
     * @return the result of processing the child element
     *
     * @throws ConfigurationException when a configuration problem was encountered,
     *         especially if there is zero or more than one child
     */
    public <T> T processOnlyElementOf(XmlElement xmlElement)
            throws ConfigurationException;

    /**
     * Request that the last remaining unprocessed child element contained within the specified {@link XmlElement}
     * is processed using an appropriate {@link ElementProcessor}.
     * <p>
     * This is a convenience method to aid in the processing of an unprocessed child {@link XmlElement} of an element.
     *
     * @param <T>         the type
     * @param xmlElement  the parent {@link XmlElement} of the unprocessed child to process
     *
     * @return the result of processing the child element
     *
     * @throws ConfigurationException if there are zero or more than one unprocessed child in the element, or
     *                                if some other {@link ConfigurationException} occurred
     */
    public <T> T processRemainingElementOf(XmlElement xmlElement)
            throws ConfigurationException;

    /**
     * Request that the last remaining unprocessed children contained within the specified {@link XmlElement}
     * are processed using appropriate {@link ElementProcessor}s.
     * <p>
     * This is a convenience method to aid in the processing of an unprocessed child {@link XmlElement}s of an element.
     * The keys of the returned {@link Map} represent the id attributes each child {@link XmlElement}.  If an
     * {@link XmlElement} does not have a specified id attribute, a UUID is generated in it's place.
     *
     * @param xmlElement  the parent {@link XmlElement} of the unprocessed children to process
     *
     * @return a {@link Map} from identifiable child {@link XmlElement}s (with id="..." attributes)
     *         and their corresponding processed values
     *
     * @throws ConfigurationException when a configuration problem was encountered
     */
    public Map<String, ?> processRemainingElementsOf(XmlElement xmlElement)
            throws ConfigurationException;

    /**
     * Given the information available in the {@link ProcessingContext}, including the cookies, the
     * {@link ResourceRegistry} the {@link XmlElement}, its
     * {@link XmlAttribute}s and/or children, inject appropriately named and typed values into the specified
     * bean (using setter injection).
     * <p>
     * The order in which values are located for injection is as follows; attributed defined by the element, child
     * elements defined by the element, alternative paths to values defined in the {@link ProcessingContext}, cookies
     * defined by the {@link ProcessingContext} and finally the {@link ResourceRegistry} associated with the
     * {@link ProcessingContext}.
     *
     * @param <B>         the bean type
     * @param bean        the bean to be injected
     * @param xmlElement  the {@link XmlElement} from which values will be derived for injection into the bean
     *
     * @return  the provided bean but with properties set based on the available values in the {@link ProcessingContext}
     *
     * @throws ConfigurationException if a configuration is not valid
     */
    public <B> B inject(B bean, XmlElement xmlElement)
            throws ConfigurationException;

    /**
     * Determines if the specified property is defined at the path relative to the specified {@link XmlElement}.
     *
     * @param sPath       the path to the property
     * @param xmlElement  the {@link XmlElement} in which the property should be searched
     *
     * @return <code>true</code> if the property is defined, <code>false</code> otherwise
     *
     * @throws ConfigurationException if a configuration is not valid
     */
    public boolean isPropertyDefined(String sPath, XmlElement xmlElement)
            throws ConfigurationException;

    /**
     * Obtains the strongly typed value for the property defined at the path relative to the specified
     * {@link XmlElement}. If the property is not defined or is of the incorrect type, a {@link ConfigurationException}
     * is thrown.
     *
     * @param <T>           the type of the property
     * @param sPath         the path to the property
     * @param typeProperty  the type of the property
     * @param xmlElement    the {@link XmlElement} containing the properties for the object
     *
     * @return the property value
     *
     * @throws ConfigurationException if a configuration is not valid, the property can't be located or is of the
     *                                wrong type
     */
    public <T> T getMandatoryProperty(String sPath, Type typeProperty, XmlElement xmlElement)
            throws ConfigurationException;

    /**
     * Obtains the strongly typed value for the property defined at the path relative to the specified
     * {@link XmlElement}. If the property is not defined, the defaultValue is returned.
     *
     * @param <T>           the type of the property
     * @param sPath         the path to the property
     * @param typeProperty  the type of the property
     * @param defaultValue  the value to return if the property is not found
     * @param xmlElement    the {@link XmlElement} containing the properties for the object
     *
     * @return the property value
     *
     * @throws ConfigurationException if a configuration is not valid
     */
    public <T> T getOptionalProperty(String sPath, Type typeProperty, T defaultValue, XmlElement xmlElement)
            throws ConfigurationException;
    }
