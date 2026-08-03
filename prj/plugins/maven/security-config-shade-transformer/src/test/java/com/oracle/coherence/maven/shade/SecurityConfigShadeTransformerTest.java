/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.shade;

import org.junit.Test;

import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SecurityConfigShadeTransformer}.
 *
 * @author Aleks Seovic  2026.05.02
 * @since 26.07
 */
public class SecurityConfigShadeTransformerTest
    {
    @Test
    public void shouldMergeTwoInputsIntoOneOutput()
            throws Exception
        {
        SecurityConfigShadeTransformer transformer = new SecurityConfigShadeTransformer();

        process(transformer, config(
                clazz("com.example.First", "@Remote.Allowed", false),
                clazz("com.example.Second", "@PortableType", false)));
        process(transformer, config(
                clazz("com.example.Third", "manual", false)));

        String sXml = output(transformer);

        assertThat(sXml, containsString("com.example.First"));
        assertThat(sXml, containsString("com.example.Second"));
        assertThat(sXml, containsString("com.example.Third"));
        assertEquals(3, count(sXml, "<class "));
        assertValid(sXml);
        }

    @Test
    public void shouldDeduplicateByClassNameAndKeepFirstSource()
            throws Exception
        {
        SecurityConfigShadeTransformer transformer = new SecurityConfigShadeTransformer();

        process(transformer, config(
                clazz("com.example.Duplicate", "@Remote.Allowed", false)));
        process(transformer, config(
                clazz("com.example.Duplicate", "manual", false)));

        String sXml = output(transformer);

        assertEquals(1, count(sXml, "com.example.Duplicate"));
        assertThat(sXml, containsString("source=\"@Remote.Allowed\""));
        assertThat(sXml, not(containsString("source=\"manual\"")));
        assertValid(sXml);
        }

    @Test
    public void shouldPreserveExecutable()
            throws Exception
        {
        SecurityConfigShadeTransformer transformer = new SecurityConfigShadeTransformer();

        process(transformer, config(
                clazz("com.example.Command", "@Remote.Executable", true)));

        String sXml = output(transformer);

        assertThat(sXml, containsString("name=\"com.example.Command\""));
        assertThat(sXml, containsString("executable=\"true\""));
        assertValid(sXml);
        }

    @Test
    public void shouldPreserveLambdaTarget()
            throws Exception
        {
        SecurityConfigShadeTransformer transformer = new SecurityConfigShadeTransformer();

        process(transformer, config(
                clazz("com.example.Command", "@Remote.Executable", false, true)));

        String sXml = output(transformer);

        assertThat(sXml, containsString("name=\"com.example.Command\""));
        assertThat(sXml, containsString("lambda-target=\"true\""));
        assertThat(sXml, not(containsString("executable=\"true\"")));
        assertValid(sXml);
        }

    @Test(expected = IOException.class)
    public void shouldFailOnConflictingExecutableValues()
            throws Exception
        {
        SecurityConfigShadeTransformer transformer = new SecurityConfigShadeTransformer();

        process(transformer, config(
                clazz("com.example.Command", "@Remote.Executable", true)));
        process(transformer, config(
                clazz("com.example.Command", "@Remote.Allowed", false)));
        }

    @Test(expected = IOException.class)
    public void shouldFailOnConflictingLambdaTargetValues()
            throws Exception
        {
        SecurityConfigShadeTransformer transformer = new SecurityConfigShadeTransformer();

        process(transformer, config(
                clazz("com.example.Command", "@Remote.Executable", false, true)));
        process(transformer, config(
                clazz("com.example.Command", "@Remote.Executable", false, false)));
        }

    @Test(expected = IOException.class)
    public void shouldFailOnMalformedXml()
            throws Exception
        {
        process(new SecurityConfigShadeTransformer(), "<security-config>");
        }

    @Test(expected = IOException.class)
    public void shouldFailOnSchemaInvalidXml()
            throws Exception
        {
        process(new SecurityConfigShadeTransformer(), config(
                "<class name=\"NoPackage\"/>"));
        }

    @Test
    public void shouldOnlyTransformSecurityConfigResource()
        {
        SecurityConfigShadeTransformer transformer = new SecurityConfigShadeTransformer();

        assertTrue(transformer.canTransformResource(SecurityConfigShadeTransformer.RESOURCE_SECURITY_CONFIG));
        assertFalse(transformer.canTransformResource("META-INF/services/example.Service"));
        }

    @Test
    public void shouldEmitEmptyMergedConfigWhenInputHasNoClasses()
            throws Exception
        {
        SecurityConfigShadeTransformer transformer = new SecurityConfigShadeTransformer();

        process(transformer, config());

        String sXml = output(transformer);

        assertThat(sXml, containsString("<allowed-classes/>"));
        assertValid(sXml);
        }

    private static void process(SecurityConfigShadeTransformer transformer, String sXml)
            throws IOException
        {
        transformer.processResource(SecurityConfigShadeTransformer.RESOURCE_SECURITY_CONFIG,
                new ByteArrayInputStream(sXml.getBytes(StandardCharsets.UTF_8)), null, 0);
        }

    private static String output(SecurityConfigShadeTransformer transformer)
            throws IOException
        {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(out))
            {
            transformer.modifyOutputStream(jar);
            }

        try (JarInputStream jar = new JarInputStream(new ByteArrayInputStream(out.toByteArray())))
            {
            assertEquals(SecurityConfigShadeTransformer.RESOURCE_SECURITY_CONFIG, jar.getNextJarEntry().getName());

            ByteArrayOutputStream xml = new ByteArrayOutputStream();
            byte[]                ab  = new byte[1024];
            int                   cb;

            while ((cb = jar.read(ab)) >= 0)
                {
                xml.write(ab, 0, cb);
                }

            return new String(xml.toByteArray(), StandardCharsets.UTF_8);
            }
        }

    private static void assertValid(String sXml)
            throws Exception
        {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        factory.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(new StreamSource(SecurityConfigShadeTransformerTest.class
                        .getResourceAsStream("/coherence-security-config.xsd"))));

        Document document = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(sXml.getBytes(StandardCharsets.UTF_8)));

        assertEquals("security-config", document.getDocumentElement().getLocalName());
        }

    private static String config(String... asClass)
        {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<security-config xmlns=\"http://xmlns.oracle.com/coherence/coherence-security-config\" version=\"1.0\">\n");
        sb.append("  <allowed-classes>\n");
        for (String sClass : asClass)
            {
            sb.append("    ").append(sClass).append('\n');
            }
        sb.append("  </allowed-classes>\n");
        sb.append("</security-config>\n");
        return sb.toString();
        }

    private static String clazz(String sName, String sSource, boolean fExecutable)
        {
        return clazz(sName, sSource, fExecutable, false);
        }

    private static String clazz(String sName, String sSource, boolean fExecutable, boolean fLambdaTarget)
        {
        return "<class name=\"" + sName + "\" source=\"" + sSource + "\""
                + (fExecutable ? " executable=\"true\"" : "")
                + (fLambdaTarget ? " lambda-target=\"true\"" : "")
                + "/>";
        }

    private static int count(String sText, String sNeedle)
        {
        int cCount = 0;
        int of     = 0;

        while ((of = sText.indexOf(sNeedle, of)) >= 0)
            {
            cCount++;
            of += sNeedle.length();
            }

        return cCount;
        }
    }
