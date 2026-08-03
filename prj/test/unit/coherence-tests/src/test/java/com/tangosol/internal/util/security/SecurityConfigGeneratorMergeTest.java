/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.tangosol.internal.util.security.fixtures.executable.ExecutableFixture;
import com.tangosol.internal.util.security.fixtures.lambdatarget.InheritedLambdaTargetFixture;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.net.URI;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

/**
 * Integration tests for {@link SecurityConfigGenerator} merging scanner output
 * with a pre-existing module security config.
 *
 * @author Aleks Seovic  2026.05.02
 * @since 26.07
 */
public class SecurityConfigGeneratorMergeTest
    {
    @Rule
    public TemporaryFolder m_folder = new TemporaryFolder();

    @Test
    public void shouldMergeExistingConfigWithGeneratedEntries()
            throws Exception
        {
        Path   dir     = classesDir();
        Path   path    = dir.resolve(SecurityConfigGenerator.OUTPUT_RESOURCE);
        String sManual = "com.example.ManualAllowed";
        String sExec   = "com.example.ManualExecutable";
        String sLambda = "com.example.ManualLambdaTarget";
        String sMerged = ExecutableFixture.class.getName();
        String sScan   = InheritedLambdaTargetFixture.class.getName();

        copyClass(dir, ExecutableFixture.class);
        copyClass(dir, InheritedLambdaTargetFixture.class);
        write(path,
                entry(sManual, "manual", null, null),
                entry(sExec, "manual", "true", null),
                entry(sLambda, "manual", null, "true"),
                entry(sMerged, "manual", null, "true"));

        SecurityConfigGenerator.generateAndWrite(dir);

        byte[]      abXml      = Files.readAllBytes(path);
        List<Entry> listEntries = entries(abXml);
        Map<String, Entry> mapEntries = byName(listEntries);

        validate(abXml);
        assertThat(count(listEntries, sManual), is(1));
        assertThat(count(listEntries, sExec), is(1));
        assertThat(count(listEntries, sLambda), is(1));
        assertThat(count(listEntries, sMerged), is(1));
        assertThat(count(listEntries, sScan), is(1));

        assertThat(mapEntries.get(sManual).getAttributes(), is(attributes("name", sManual, "source", "manual")));
        assertThat(mapEntries.get(sExec).getAttributes(),
                is(attributes("name", sExec, "source", "manual", "executable", "true")));
        assertThat(mapEntries.get(sLambda).getAttributes(),
                is(attributes("name", sLambda, "source", "manual", "lambda-target", "true")));
        assertThat(mapEntries.get(sMerged).getAttributes(),
                is(attributes("name", sMerged, "source", "manual", "lambda-target", "true")));
        assertThat(mapEntries.get(sScan).getAttributes(), is(attributes("name", sScan,
                "source", SecurityConfigGenerator.SOURCE_REMOTE_EXECUTABLE, "lambda-target", "true")));

        List<String> listNames = names(listEntries);
        List<String> listSorted = new ArrayList<>(listNames);
        Collections.sort(listSorted);

        assertThat(listNames, is(listSorted));
        }

    @Test
    public void shouldRejectInvalidExistingConfig()
            throws Exception
        {
        Path dir  = classesDir();
        Path path = dir.resolve(SecurityConfigGenerator.OUTPUT_RESOURCE);

        copyClass(dir, ExecutableFixture.class);
        write(path, "<class source=\"manual\"/>");

        Exception e = assertThrows(Exception.class, () -> SecurityConfigGenerator.generateAndWrite(dir));

        assertThat(e.getMessage(), containsString(SecurityConfigGenerator.OUTPUT_RESOURCE));
        assertThat(e.getMessage(), containsString(path.toString()));
        }

    @Test
    public void shouldOverwriteReadOnlyExistingConfig()
            throws Exception
        {
        Path   dir     = classesDir();
        Path   path    = dir.resolve(SecurityConfigGenerator.OUTPUT_RESOURCE);
        String sManual = "com.example.ManualAllowed";
        String sScan   = ExecutableFixture.class.getName();

        copyClass(dir, ExecutableFixture.class);
        write(path, entry(sManual, "manual", null, null));

        assumeTrue("test requires writable bit updates", path.toFile().setWritable(false, false));

        try
            {
            SecurityConfigGenerator.generateAndWrite(dir);
            }
        finally
            {
            path.toFile().setWritable(true, false);
            }

        byte[]            abXml      = Files.readAllBytes(path);
        Map<String, Entry> mapEntries = byName(entries(abXml));

        validate(abXml);
        assertThat(mapEntries.get(sManual).getAttributes(), is(attributes("name", sManual, "source", "manual")));
        assertThat(mapEntries.get(sScan).getAttributes(),
                is(attributes("name", sScan, "source", SecurityConfigGenerator.SOURCE_REMOTE_EXECUTABLE,
                        "executable", "true")));
        }

    // ----- helper methods -------------------------------------------------

    private Path classesDir()
            throws Exception
        {
        return m_folder.newFolder().toPath();
        }

    private static void copyClass(Path dir, Class<?> clz)
            throws Exception
        {
        copyResource(dir, clz.getName().replace('.', '/') + ".class");
        }

    private static void copyResource(Path dir, String sResource)
            throws Exception
        {
        URI  uri  = SecurityConfigGeneratorMergeTest.class.getClassLoader().getResource(sResource).toURI();
        Path from = java.nio.file.Paths.get(uri);
        Path to   = dir.resolve(sResource);

        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }

    private static void write(Path path, String... asEntries)
            throws Exception
        {
        Files.createDirectories(path.getParent());
        Files.write(path, xml(asEntries).getBytes(StandardCharsets.UTF_8));
        }

    private static String entry(String sName, String sSource, String sExecutable, String sLambdaTarget)
        {
        return "<class name=\"" + sName + "\" source=\"" + sSource + "\""
                + (sExecutable == null ? "" : " executable=\"" + sExecutable + "\"")
                + (sLambdaTarget == null ? "" : " lambda-target=\"" + sLambdaTarget + "\"")
                + "/>";
        }

    private static String xml(String... asEntries)
        {
        return "<?xml version=\"1.0\"?>\n"
                + "<security-config xmlns=\"" + SecurityConfigGenerator.NAMESPACE + "\"\n"
                + "                 version=\"1.0\">\n"
                + "  <allowed-classes>\n"
                + String.join("\n", asEntries)
                + "\n  </allowed-classes>\n"
                + "</security-config>\n";
        }

    private static void validate(byte[] abXml)
            throws Exception
        {
        try (InputStream inSchema = SecurityConfigGenerator.class.getResourceAsStream("/coherence-security-config.xsd"))
            {
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                    .newSchema(new StreamSource(inSchema))
                    .newValidator()
                    .validate(new StreamSource(new ByteArrayInputStream(abXml)));
            }
        }

    private static List<Entry> entries(byte[] abXml)
            throws Exception
        {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(abXml));
        NodeList listClass = document.getElementsByTagNameNS(SecurityConfigGenerator.NAMESPACE, "class");
        List<Entry> listEntries = new ArrayList<>();

        for (int i = 0; i < listClass.getLength(); i++)
            {
            Element element = (Element) listClass.item(i);
            NamedNodeMap mapNodes = element.getAttributes();
            Map<String, String> mapAttributes = new LinkedHashMap<>();

            for (int j = 0; j < mapNodes.getLength(); j++)
                {
                Node node = mapNodes.item(j);
                mapAttributes.put(node.getNodeName(), node.getNodeValue());
                }
            listEntries.add(new Entry(element.getAttribute("name"), mapAttributes));
            }
        return listEntries;
        }

    private static Map<String, Entry> byName(List<Entry> listEntries)
        {
        Map<String, Entry> mapEntries = new LinkedHashMap<>();
        for (Entry entry : listEntries)
            {
            mapEntries.put(entry.getName(), entry);
            }
        return mapEntries;
        }

    private static int count(List<Entry> listEntries, String sName)
        {
        int cCount = 0;
        for (Entry entry : listEntries)
            {
            if (sName.equals(entry.getName()))
                {
                cCount++;
                }
            }
        return cCount;
        }

    private static List<String> names(List<Entry> listEntries)
        {
        List<String> listNames = new ArrayList<>();
        for (Entry entry : listEntries)
            {
            listNames.add(entry.getName());
            }
        return listNames;
        }

    private static Map<String, String> attributes(String... asNameValue)
        {
        Map<String, String> mapAttributes = new LinkedHashMap<>();
        for (int i = 0; i < asNameValue.length; i += 2)
            {
            mapAttributes.put(asNameValue[i], asNameValue[i + 1]);
            }
        return mapAttributes;
        }

    // ----- inner class: Entry --------------------------------------------

    private static class Entry
        {
        private Entry(String sName, Map<String, String> mapAttributes)
            {
            m_sName         = sName;
            m_mapAttributes = mapAttributes;
            }

        private String getName()
            {
            return m_sName;
            }

        private Map<String, String> getAttributes()
            {
            return m_mapAttributes;
            }

        private final String m_sName;

        private final Map<String, String> m_mapAttributes;
        }
    }
