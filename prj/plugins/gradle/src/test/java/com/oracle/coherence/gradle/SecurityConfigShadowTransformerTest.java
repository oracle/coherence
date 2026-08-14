/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import com.oracle.coherence.gradle.support.TestUtils.SecurityConfigEntry;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for Gradle Shadow security configuration merging.
 *
 * @author Aleks Seovic  2026.08.10
 * @since 26.07
 */
class SecurityConfigShadowTransformerTest
    {
    @Test
    void shouldMergeSecurityConfigForCurrentShadowPluginId()
            throws IOException
        {
        assertShadowMerge("com.gradleup.shadow", false);
        }

    @Test
    void shouldMergeSecurityConfigForLegacyShadowPluginId()
            throws IOException
        {
        assertShadowMerge("com.github.johnrengelman.shadow", false);
        }

    @Test
    void shouldMergeSecurityConfigWhenShadowIsAppliedFirst()
            throws IOException
        {
        assertShadowMerge("com.gradleup.shadow", true);
        }

    private void assertShadowMerge(String sPluginId, boolean fShadowFirst)
            throws IOException
        {
        createFakeShadowPlugin(sPluginId);
        createDependency("one.jar", config(
                clazz("com.example.One", "manual", true, false),
                clazz("com.example.Duplicate", "manual", false, false)));
        createDependency("two.jar", config(
                clazz("com.example.Two", "@Remote.Executable", false, true),
                clazz("com.example.Duplicate", "scan", false, false)));
        createApplication(sPluginId, fShadowFirst);

        BuildResult result = GradleRunner.create()
                .withProjectDir(f_pathProject.toFile())
                .withArguments("shadowJar", "--stacktrace", "--info")
                .withPluginClasspath()
                .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":securityConfigShadow").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":shadowJar").getOutcome());

        Path pathJar = f_pathProject.resolve("build/libs/application-all.jar");
        assertTrue(Files.isRegularFile(pathJar));

        try (JarFile jar = new JarFile(pathJar.toFile()))
            {
            assertEquals(1, countSecurityConfigEntries(jar));
            assertThat(readEntries(jar), containsInAnyOrder(
                    new SecurityConfigEntry("com.example.One", "manual", true, false),
                    new SecurityConfigEntry("com.example.Two", "@Remote.Executable", false, true),
                    new SecurityConfigEntry("com.example.Duplicate", "manual", false, false)));
            }
        }

    private void createApplication(String sPluginId, boolean fShadowFirst)
            throws IOException
        {
        Files.writeString(f_pathProject.resolve("settings.gradle"), "rootProject.name = 'application'\n");
        String sPlugins = fShadowFirst
                ? "plugins {\n"
                        + "    id 'java'\n"
                        + "    id '" + sPluginId + "'\n"
                        + "    id 'com.oracle.coherence.ce'\n"
                        + "}\n"
                : "plugins {\n"
                        + "    id 'java'\n"
                        + "    id 'com.oracle.coherence.ce'\n"
                        + "}\n"
                        + "apply plugin: '" + sPluginId + "'\n";

        Files.writeString(f_pathProject.resolve("build.gradle"), sPlugins
                + "dependencies {\n"
                + "    implementation files('libs/one.jar', 'libs/two.jar')\n"
                + "}\n");

        Path pathJava = f_pathProject.resolve("src/main/java/com/example/Application.java");
        Files.createDirectories(pathJava.getParent());
        Files.writeString(pathJava,
                "package com.example;\npublic class Application {}\n", StandardCharsets.UTF_8);
        }

    private void createFakeShadowPlugin(String sPluginId)
            throws IOException
        {
        Path pathJava = f_pathProject.resolve("buildSrc/src/main/java/com/example/FakeShadowPlugin.java");
        Files.createDirectories(pathJava.getParent());
        Files.writeString(pathJava, """
                package com.example;

                import org.gradle.api.Plugin;
                import org.gradle.api.Project;
                import org.gradle.api.file.DuplicatesStrategy;
                import org.gradle.api.plugins.JavaPluginExtension;
                import org.gradle.api.tasks.SourceSet;
                import org.gradle.api.tasks.bundling.Jar;

                import java.io.File;
                import java.util.ArrayList;
                import java.util.List;

                public class FakeShadowPlugin implements Plugin<Project>
                    {
                    @Override
                    public void apply(Project project)
                        {
                        project.getTasks().register("shadowJar", Jar.class, task ->
                            {
                            task.getArchiveFileName().set("application-all.jar");
                            task.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE);

                            SourceSet sourceSet = project.getExtensions().getByType(JavaPluginExtension.class)
                                    .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                            task.from(sourceSet.getOutput());
                            task.from(project.provider(() ->
                                {
                                List<Object> list = new ArrayList<>();
                                for (File file : project.getConfigurations().getByName("runtimeClasspath").getFiles())
                                    {
                                    list.add(file.isDirectory() ? file : project.zipTree(file));
                                    }
                                return list;
                                }));
                            });
                        }
                    }
                """, StandardCharsets.UTF_8);

        Path pathDescriptor = f_pathProject.resolve("buildSrc/src/main/resources/META-INF/gradle-plugins/")
                .resolve(sPluginId + ".properties");
        Files.createDirectories(pathDescriptor.getParent());
        Files.writeString(pathDescriptor, "implementation-class=com.example.FakeShadowPlugin\n");
        }

    private void createDependency(String sName, String sXml)
            throws IOException
        {
        Path pathJar = f_pathProject.resolve("libs").resolve(sName);
        Files.createDirectories(pathJar.getParent());

        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(pathJar)))
            {
            out.putNextEntry(new JarEntry(SecurityConfigResourceMerger.RESOURCE_SECURITY_CONFIG));
            out.write(sXml.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            }
        }

    private static int countSecurityConfigEntries(JarFile jar)
        {
        int                   cEntries = 0;
        Enumeration<JarEntry> entries  = jar.entries();
        while (entries.hasMoreElements())
            {
            if (SecurityConfigResourceMerger.RESOURCE_SECURITY_CONFIG.equals(entries.nextElement().getName()))
                {
                cEntries++;
                }
            }
        return cEntries;
        }

    private static Set<SecurityConfigEntry> readEntries(JarFile jar)
            throws IOException
        {
        try
            {
            JarEntry entry = jar.getJarEntry(SecurityConfigResourceMerger.RESOURCE_SECURITY_CONFIG);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(jar.getInputStream(entry));
            NodeList listClass = document.getElementsByTagNameNS("*", "class");
            Set<SecurityConfigEntry> setEntries = new HashSet<>();

            for (int i = 0; i < listClass.getLength(); i++)
                {
                Element element = (Element) listClass.item(i);
                setEntries.add(new SecurityConfigEntry(element.getAttribute("name"), element.getAttribute("source"),
                        Boolean.parseBoolean(element.getAttribute("executable")),
                        Boolean.parseBoolean(element.getAttribute("lambda-target"))));
                }
            return setEntries;
            }
        catch (Exception e)
            {
            throw new IOException(e);
            }
        }

    private static String config(String... asClass)
        {
        return "<?xml version=\"1.0\"?>\n"
                + "<security-config xmlns=\"http://xmlns.oracle.com/coherence/coherence-security-config\""
                + " version=\"1.0\">\n"
                + "  <allowed-classes>\n"
                + String.join("", asClass)
                + "  </allowed-classes>\n"
                + "</security-config>\n";
        }

    private static String clazz(String sName, String sSource, boolean fExecutable, boolean fLambdaTarget)
        {
        return "    <class name=\"" + sName + "\" source=\"" + sSource + "\""
                + (fExecutable ? " executable=\"true\"" : "")
                + (fLambdaTarget ? " lambda-target=\"true\"" : "")
                + "/>\n";
        }

    @TempDir
    private Path f_pathProject;
    }
