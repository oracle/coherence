/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.util.Base;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

/**
 * Runtime union of classpath {@code META-INF/coherence/security-config.xml}
 * resources.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public final class SecurityConfig
    {
    /**
     * Return the current security config.
     *
     * @return the current security config
     */
    public static SecurityConfig current()
        {
        SecurityConfig config = S_CONFIG.get();
        if (config == null)
            {
            config = load();
            if (!S_CONFIG.compareAndSet(null, config))
                {
                config = S_CONFIG.get();
                }
            }
        return config;
        }

    /**
     * Return {@code true} if the specified class name is allowed.
     *
     * @param sFqn  the fully qualified class name
     *
     * @return {@code true} if the specified class name is allowed
     */
    public boolean contains(String sFqn)
        {
        return f_mapSource.containsKey(sFqn);
        }

    /**
     * Return the source attribution for the specified class name.
     *
     * @param sFqn  the fully qualified class name
     *
     * @return the source attribution, or {@code null}
     */
    public String source(String sFqn)
        {
        return f_mapSource.get(sFqn);
        }

    /**
     * Return all allowed fully qualified class names.
     *
     * @return all allowed fully qualified class names
     */
    public Set<String> allowedFqns()
        {
        return f_setAllowed;
        }

    /**
     * Return {@code true} if the specified class name is executable.
     *
     * @param sFqn  the fully qualified class name
     *
     * @return {@code true} if the specified class name is executable
     */
    public boolean isExecutable(String sFqn)
        {
        return sFqn != null && f_setExecutable.contains(sFqn);
        }

    /**
     * Return all executable fully qualified class names.
     *
     * @return all executable fully qualified class names
     */
    public Set<String> executableFqns()
        {
        return f_setExecutable;
        }

    /**
     * Return {@code true} if the specified class name is an allowed lambda target.
     *
     * @param sFqn  the fully qualified class name
     *
     * @return {@code true} if the specified class name is an allowed lambda target
     */
    public boolean isLambdaTarget(String sFqn)
        {
        return sFqn != null && f_setLambdaTarget.contains(sFqn);
        }

    /**
     * Return all allowed lambda-target fully qualified class names.
     *
     * @return all allowed lambda-target fully qualified class names
     */
    public Set<String> lambdaTargetFqns()
        {
        return f_setLambdaTarget;
        }

    /**
     * Reset the singleton for tests.
     */
    static void resetForTesting()
        {
        S_CONFIG.set(null);
        }

    // ----- constructors ---------------------------------------------------

    private SecurityConfig(Map<String, String> mapSource, Set<String> setExecutable, Set<String> setLambdaTarget)
        {
        f_mapSource       = Collections.unmodifiableMap(new LinkedHashMap<>(mapSource));
        f_setAllowed      = Collections.unmodifiableSet(f_mapSource.keySet());
        f_setExecutable   = Collections.unmodifiableSet(new LinkedHashSet<>(setExecutable));
        f_setLambdaTarget = Collections.unmodifiableSet(new LinkedHashSet<>(setLambdaTarget));
        }

    // ----- helper methods -------------------------------------------------

    private static SecurityConfig load()
        {
        Map<String, String> mapSource       = new LinkedHashMap<>();
        Set<String>         setExecutable   = new LinkedHashSet<>();
        Set<String>         setLambdaTarget = new LinkedHashSet<>();
        int                 cSources        = 0;

        try
            {
            Enumeration<URL> resources = classLoader().getResources(RESOURCE_SECURITY_CONFIG);
            while (resources.hasMoreElements())
                {
                URL url = resources.nextElement();
                cSources++;
                read(url, mapSource, setExecutable, setLambdaTarget);
                }
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        Logger.info("SecurityConfig loaded " + mapSource.size() + " entries from " + cSources + " sources");
        return new SecurityConfig(mapSource, setExecutable, setLambdaTarget);
        }

    private static void read(URL url, Map<String, String> mapSource, Set<String> setExecutable,
            Set<String> setLambdaTarget)
        {
        try (InputStream in = url.openStream();
             InputStream inSchema = schema())
            {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                    .newSchema(new StreamSource(inSchema)));

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(THROWING_ERROR_HANDLER);

            Document document = builder.parse(in);
            NodeList listClass = document.getElementsByTagNameNS(NAMESPACE, "class");
            for (int i = 0; i < listClass.getLength(); i++)
                {
                Element element = (Element) listClass.item(i);
                // first-loaded source attribution wins; duplicate FQNs remain valid but do not churn diagnostics.
                String sName = element.getAttribute("name");
                mapSource.putIfAbsent(sName, element.getAttribute("source"));
                if (Boolean.parseBoolean(element.getAttribute("executable")))
                    {
                    setExecutable.add(sName);
                    }
                if (Boolean.parseBoolean(element.getAttribute("lambda-target")))
                    {
                    setLambdaTarget.add(sName);
                    }
                }
            }
        catch (IOException | ParserConfigurationException | SAXException e)
            {
            throw new IllegalStateException("Unable to load " + RESOURCE_SECURITY_CONFIG + " from " + url, e);
            }
        }

    private static InputStream schema()
        {
        InputStream in = SecurityConfig.class.getResourceAsStream("/coherence-security-config.xsd");
        if (in == null)
            {
            throw new IllegalStateException("Unable to load /coherence-security-config.xsd");
            }
        return in;
        }

    private static ClassLoader classLoader()
        {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader == null ? SecurityConfig.class.getClassLoader() : loader;
        }

    // ----- constants ------------------------------------------------------

    public static final String RESOURCE_SECURITY_CONFIG = "META-INF/coherence/security-config.xml";

    private static final String NAMESPACE = "http://xmlns.oracle.com/coherence/coherence-security-config";

    private static final AtomicReference<SecurityConfig> S_CONFIG = new AtomicReference<>();

    private static final ErrorHandler THROWING_ERROR_HANDLER = new ErrorHandler()
        {
        @Override
        public void warning(SAXParseException e)
                throws SAXException
            {
            throw e;
            }

        @Override
        public void error(SAXParseException e)
                throws SAXException
            {
            throw e;
            }

        @Override
        public void fatalError(SAXParseException e)
                throws SAXException
            {
            throw e;
            }
        };

    // ----- data members ---------------------------------------------------

    private final Map<String, String> f_mapSource;

    private final Set<String> f_setAllowed;

    private final Set<String> f_setExecutable;

    private final Set<String> f_setLambdaTarget;
    }
