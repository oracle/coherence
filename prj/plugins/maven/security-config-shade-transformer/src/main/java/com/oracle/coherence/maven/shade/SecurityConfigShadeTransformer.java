/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.shade;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ReproducibleResourceTransformer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/**
 * Maven Shade transformer that merges Coherence
 * {@code META-INF/coherence/security-config.xml} resources.
 * <p>
 * The transformer runs in the Maven Shade plugin classloader, so depending on
 * {@code coherence-core} would create a build cycle. The matching
 * {@code coherence-security-config.xsd} is therefore intentionally copied into
 * this module and should be kept in sync whenever the runtime schema changes,
 * including additive attributes such as {@code executable} and
 * {@code lambda-target}.
 *
 * @author Aleks Seovic  2026.05.02
 * @since 26.04
 */
public class SecurityConfigShadeTransformer
        implements ReproducibleResourceTransformer
    {
    @Override
    public boolean canTransformResource(String sResource)
        {
        return RESOURCE_SECURITY_CONFIG.equals(sResource);
        }

    @Override
    @SuppressWarnings("deprecation")
    public void processResource(String sResource, InputStream in, List<Relocator> listRelocators)
            throws IOException
        {
        processResource(sResource, in, listRelocators, 0);
        }

    @Override
    public void processResource(String sResource, InputStream in, List<Relocator> listRelocators, long lTime)
            throws IOException
        {
        if (!canTransformResource(sResource))
            {
            return;
            }

        m_fTransformed = true;

        Document document = parse(in, sResource);
        NodeList listClass = document.getElementsByTagNameNS(NAMESPACE, "class");
        for (int i = 0; i < listClass.getLength(); i++)
            {
            Element element     = (Element) listClass.item(i);
            String  sName       = element.getAttribute("name");
            String  sSource     = element.getAttribute("source");
            boolean fExecutable = executable(element);
            boolean fLambda     = lambdaTarget(element);

            ClassEntry entry = m_mapEntries.get(sName);
            if (entry == null)
                {
                m_mapEntries.put(sName, new ClassEntry(sSource, fExecutable, fLambda));
                }
            else if (entry.isExecutable() != fExecutable)
                {
                throw new IOException("Conflicting executable values for " + sName);
                }
            else if (entry.isLambdaTarget() != fLambda)
                {
                throw new IOException("Conflicting lambda-target values for " + sName);
                }
            }

        m_lTime = Math.max(m_lTime, lTime);
        }

    @Override
    public boolean hasTransformedResource()
        {
        return m_fTransformed;
        }

    @Override
    public void modifyOutputStream(JarOutputStream out)
            throws IOException
        {
        byte[] abXml = toXml();
        parse(new ByteArrayInputStream(abXml), RESOURCE_SECURITY_CONFIG);

        JarEntry entry = new JarEntry(RESOURCE_SECURITY_CONFIG);
        if (m_lTime >= 0)
            {
            entry.setTime(m_lTime);
            }
        out.putNextEntry(entry);
        out.write(abXml);
        out.closeEntry();
        }

    private byte[] toXml()
            throws IOException
        {
        try
            {
            Document document = newDocumentBuilder().newDocument();
            Element  root     = document.createElementNS(NAMESPACE, "security-config");
            Element  classes  = document.createElementNS(NAMESPACE, "allowed-classes");

            root.setAttribute("version", "1.0");
            document.appendChild(root);
            root.appendChild(classes);

            for (Map.Entry<String, ClassEntry> entry : m_mapEntries.entrySet())
                {
                ClassEntry classEntry = entry.getValue();
                Element    element    = document.createElementNS(NAMESPACE, "class");

                element.setAttribute("name", entry.getKey());
                if (!classEntry.getSource().isEmpty())
                    {
                    element.setAttribute("source", classEntry.getSource());
                    }
                if (classEntry.isExecutable())
                    {
                    element.setAttribute("executable", "true");
                    }
                if (classEntry.isLambdaTarget())
                    {
                    element.setAttribute("lambda-target", "true");
                    }
                classes.appendChild(element);
                }

            ByteArrayOutputStream out         = new ByteArrayOutputStream();
            TransformerFactory    factory     = TransformerFactory.newInstance();
            Transformer           transformer = factory.newTransformer();

            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toByteArray();
            }
        catch (ParserConfigurationException | SAXException | TransformerException e)
            {
            throw new IOException("Unable to write " + RESOURCE_SECURITY_CONFIG, e);
            }
        }

    private static Document parse(InputStream in, String sResource)
            throws IOException
        {
        try
            {
            DocumentBuilder builder = newDocumentBuilder();
            builder.setErrorHandler(THROWING_ERROR_HANDLER);
            return builder.parse(in);
            }
        catch (ParserConfigurationException | SAXException e)
            {
            throw new IOException("Unable to parse " + sResource, e);
            }
        }

    private static DocumentBuilder newDocumentBuilder()
            throws ParserConfigurationException, SAXException, IOException
        {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setSchema(schema());
        return factory.newDocumentBuilder();
        }

    private static Schema schema()
            throws SAXException, IOException
        {
        try (InputStream in = SecurityConfigShadeTransformer.class.getResourceAsStream("/coherence-security-config.xsd"))
            {
            if (in == null)
                {
                throw new IOException("Unable to load /coherence-security-config.xsd");
                }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newSchema(new StreamSource(in));
            }
        }

    private static boolean executable(Element element)
        {
        if (!element.hasAttribute("executable"))
            {
            return false;
            }

        String sValue = element.getAttribute("executable");
        return "true".equals(sValue) || "1".equals(sValue);
        }

    private static boolean lambdaTarget(Element element)
        {
        if (!element.hasAttribute("lambda-target"))
            {
            return false;
            }

        String sValue = element.getAttribute("lambda-target");
        return "true".equals(sValue) || "1".equals(sValue);
        }

    // ----- inner class: ClassEntry ---------------------------------------

    private static class ClassEntry
        {
        ClassEntry(String sSource, boolean fExecutable, boolean fLambdaTarget)
            {
            m_sSource       = sSource == null ? "" : sSource;
            m_fExecutable   = fExecutable;
            m_fLambdaTarget = fLambdaTarget;
            }

        String getSource()
            {
            return m_sSource;
            }

        boolean isExecutable()
            {
            return m_fExecutable;
            }

        boolean isLambdaTarget()
            {
            return m_fLambdaTarget;
            }

        private final String m_sSource;

        private final boolean m_fExecutable;

        private final boolean m_fLambdaTarget;
        }

    // ----- constants ------------------------------------------------------

    static final String RESOURCE_SECURITY_CONFIG = "META-INF/coherence/security-config.xml";

    private static final String NAMESPACE = "http://xmlns.oracle.com/coherence/coherence-security-config";

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

    private final Map<String, ClassEntry> m_mapEntries = new LinkedHashMap<>();

    private boolean m_fTransformed;

    private long m_lTime = -1;
    }
