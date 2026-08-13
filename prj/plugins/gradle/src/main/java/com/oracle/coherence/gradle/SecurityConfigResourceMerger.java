/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

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
 * Merges Coherence {@code META-INF/coherence/security-config.xml} resources
 * from directories and JARs on a Gradle classpath.
 *
 * @author Aleks Seovic  2026.08.10
 * @since 26.1
 */
final class SecurityConfigResourceMerger
    {
    // ----- constructors ---------------------------------------------------

    private SecurityConfigResourceMerger()
        {
        }

    // ----- SecurityConfigResourceMerger methods -------------------------

    /**
     * Merge all security configuration resources found on a classpath.
     *
     * @param files       the classpath files and directories
     * @param pathOutput  the merged output path
     *
     * @return the merge result
     *
     * @throws IOException if a resource cannot be read, validated, or merged
     */
    static MergeResult merge(Iterable<File> files, Path pathOutput)
            throws IOException
        {
        Map<String, ClassEntry> mapEntries = new LinkedHashMap<>();
        int                     cSources   = 0;

        for (File file : files)
            {
            if (file == null)
                {
                continue;
                }

            Path path = file.toPath();
            if (Files.isDirectory(path))
                {
                Path pathResource = path.resolve(RESOURCE_SECURITY_CONFIG);
                if (Files.isRegularFile(pathResource) && !sameFile(pathResource, pathOutput))
                    {
                    try (InputStream in = Files.newInputStream(pathResource))
                        {
                        read(in, pathResource.toString(), mapEntries);
                        cSources++;
                        }
                    }
                }
            else if (Files.isRegularFile(path))
                {
                cSources += readArchive(path, mapEntries);
                }
            }

        if (cSources == 0)
            {
            Files.deleteIfExists(pathOutput);
            return new MergeResult(0, 0);
            }

        byte[] abXml = toXml(mapEntries);
        parse(new ByteArrayInputStream(abXml), pathOutput.toString());

        Files.createDirectories(pathOutput.getParent());
        Files.write(pathOutput, abXml);
        return new MergeResult(cSources, mapEntries.size());
        }

    // ----- helper methods -------------------------------------------------

    private static int readArchive(Path path, Map<String, ClassEntry> mapEntries)
            throws IOException
        {
        try (ZipFile zip = new ZipFile(path.toFile()))
            {
            ZipEntry entry = zip.getEntry(RESOURCE_SECURITY_CONFIG);
            if (entry == null || entry.isDirectory())
                {
                return 0;
                }

            try (InputStream in = zip.getInputStream(entry))
                {
                read(in, path + "!/" + RESOURCE_SECURITY_CONFIG, mapEntries);
                return 1;
                }
            }
        catch (ZipException e)
            {
            String sFileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (sFileName.endsWith(".jar") || sFileName.endsWith(".zip"))
                {
                throw e;
                }

            // Gradle classpaths may contain ordinary files as well as archives
            return 0;
            }
        }

    private static void read(InputStream in, String sSource, Map<String, ClassEntry> mapEntries)
            throws IOException
        {
        Document document = parse(in, sSource);
        NodeList listClass = document.getElementsByTagNameNS(NAMESPACE, "class");

        for (int i = 0; i < listClass.getLength(); i++)
            {
            Element element      = (Element) listClass.item(i);
            String  sName        = element.getAttribute("name");
            String  sEntrySource = element.getAttribute("source");
            boolean fExecutable  = booleanAttribute(element, "executable");
            boolean fLambda      = booleanAttribute(element, "lambda-target");

            ClassEntry entry = mapEntries.get(sName);
            if (entry == null)
                {
                mapEntries.put(sName, new ClassEntry(sName, sEntrySource, fExecutable, fLambda));
                }
            else if (entry.isExecutable() != fExecutable)
                {
                throw new IOException("Conflicting executable values for " + sName + " in " + sSource);
                }
            else if (entry.isLambdaTarget() != fLambda)
                {
                throw new IOException("Conflicting lambda-target values for " + sName + " in " + sSource);
                }
            }
        }

    private static Document parse(InputStream in, String sSource)
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
            throw new IOException("Unable to parse " + RESOURCE_SECURITY_CONFIG + " from " + sSource, e);
            }
        }

    private static byte[] toXml(Map<String, ClassEntry> mapEntries)
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

            List<ClassEntry> listEntries = new ArrayList<>(mapEntries.values());
            listEntries.sort(Comparator.comparing(ClassEntry::getName));

            for (ClassEntry entry : listEntries)
                {
                Element element = document.createElementNS(NAMESPACE, "class");

                element.setAttribute("name", entry.getName());
                if (!entry.getSource().isEmpty())
                    {
                    element.setAttribute("source", entry.getSource());
                    }
                if (entry.isExecutable())
                    {
                    element.setAttribute("executable", "true");
                    }
                if (entry.isLambdaTarget())
                    {
                    element.setAttribute("lambda-target", "true");
                    }
                classes.appendChild(element);
                }

            ByteArrayOutputStream out     = new ByteArrayOutputStream();
            TransformerFactory    factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            Transformer transformer = factory.newTransformer();
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
        try (InputStream in = SecurityConfigResourceMerger.class.getResourceAsStream("/coherence-security-config.xsd"))
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

    private static boolean booleanAttribute(Element element, String sName)
        {
        String sValue = element.getAttribute(sName);
        return "true".equals(sValue) || "1".equals(sValue);
        }

    private static boolean sameFile(Path pathOne, Path pathTwo)
        {
        return pathOne.toAbsolutePath().normalize().equals(pathTwo.toAbsolutePath().normalize());
        }

    // ----- inner class: MergeResult --------------------------------------

    /**
     * The result of merging security configuration resources.
     */
    static final class MergeResult
        {
        private MergeResult(int cSources, int cEntries)
            {
            f_cSources = cSources;
            f_cEntries = cEntries;
            }

        int getSourceCount()
            {
            return f_cSources;
            }

        int getEntryCount()
            {
            return f_cEntries;
            }

        private final int f_cSources;

        private final int f_cEntries;
        }

    // ----- inner class: ClassEntry ---------------------------------------

    private static final class ClassEntry
        {
        private ClassEntry(String sName, String sSource, boolean fExecutable, boolean fLambdaTarget)
            {
            f_sName         = sName;
            f_sSource       = sSource;
            f_fExecutable   = fExecutable;
            f_fLambdaTarget = fLambdaTarget;
            }

        private String getName()
            {
            return f_sName;
            }

        private String getSource()
            {
            return f_sSource;
            }

        private boolean isExecutable()
            {
            return f_fExecutable;
            }

        private boolean isLambdaTarget()
            {
            return f_fLambdaTarget;
            }

        private final String f_sName;

        private final String f_sSource;

        private final boolean f_fExecutable;

        private final boolean f_fLambdaTarget;
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
    }
