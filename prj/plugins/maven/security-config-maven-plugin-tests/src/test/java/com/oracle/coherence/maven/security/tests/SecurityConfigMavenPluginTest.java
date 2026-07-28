/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.security.tests;

import com.oracle.coherence.maven.security.tests.dual.DualThing;
import com.oracle.coherence.maven.security.tests.executable.AllowedExecutableThing;
import com.oracle.coherence.maven.security.tests.executable.ExecutableThing;
import com.oracle.coherence.maven.security.tests.lambdatarget.AbstractExecutableThing;
import com.oracle.coherence.maven.security.tests.lambdatarget.InheritedLambdaTargetThing;
import com.oracle.coherence.maven.security.tests.lambdatarget.LocalLambdaTargetThing;
import com.oracle.coherence.maven.security.tests.lambdatarget.MissingFunctionalInterfaceThing;
import com.oracle.coherence.maven.security.tests.lambdatarget.NonSerializableLambdaTargetThing;
import com.oracle.coherence.maven.security.tests.pkgnonrecursive.sub.SubPackageExcludedClass;
import com.oracle.coherence.maven.security.tests.pkgrecursive.sub.SubPackageAllowedClass;
import com.oracle.coherence.maven.security.tests.plain.PlainThing;
import com.oracle.coherence.maven.security.tests.portable.PortableThing;
import com.oracle.coherence.maven.security.tests.type.NonRecursiveAllowedType;
import com.oracle.coherence.maven.security.tests.type.RecursiveAllowedType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the security-config Maven plugin.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class SecurityConfigMavenPluginTest
    {
    @Test
    public void shouldGenerateSecurityConfigFromProjectClasses()
            throws Exception
        {
        String sXml = read("target/classes/META-INF/coherence/security-config.xml");

        assertEntry(sXml, RecursiveAllowedType.class.getName(), "@Remote.Allowed");
        assertEntry(sXml, RecursiveAllowedType.Nested.class.getName(), "@Remote.Allowed");
        assertEntry(sXml, NonRecursiveAllowedType.class.getName(), "@Remote.Allowed");
        assertFalse(sXml.contains(NonRecursiveAllowedType.Nested.class.getName()));

        assertEntry(sXml, "com.oracle.coherence.maven.security.tests.pkgrecursive.PackageAllowedClass",
                "@Remote.Allowed(package)");
        assertEntry(sXml, SubPackageAllowedClass.class.getName(), "@Remote.Allowed(package)");
        assertEntry(sXml, "com.oracle.coherence.maven.security.tests.pkgnonrecursive.PackageAllowedClass",
                "@Remote.Allowed(package)");
        assertFalse(sXml.contains(SubPackageExcludedClass.class.getName()));

        assertEntry(sXml, PortableThing.class.getName(), "@PortableType");
        assertEntry(sXml, DualThing.class.getName(), "@PortableType");
        assertExecutableEntry(sXml, ExecutableThing.class.getName());
        assertExecutableEntry(sXml, AllowedExecutableThing.class.getName());
        assertLambdaTargetEntry(sXml, LocalLambdaTargetThing.class.getName());
        assertLambdaTargetEntry(sXml, InheritedLambdaTargetThing.class.getName());
        assertFalse(sXml.contains(MissingFunctionalInterfaceThing.class.getName()));
        assertFalse(sXml.contains(NonSerializableLambdaTargetThing.class.getName()));
        assertFalse(sXml.contains(AbstractExecutableThing.class.getName()));
        assertFalse(sXml.contains(PlainThing.class.getName()));
        }

    @Test
    public void shouldSkipEmptyClassesDirectory()
            throws Exception
        {
        Path path = Paths.get("target/empty-classes/META-INF/coherence/security-config.xml");

        assertFalse("security-config.xml should not be generated for empty classes dir: "
                + path.toAbsolutePath(), Files.exists(path));
        }

    private static void assertEntry(String sXml, String sName, String sSource)
        {
        assertThat(sXml, containsString("<class name=\"" + sName + "\" source=\"" + sSource + "\"/>"));
        }

    private static void assertExecutableEntry(String sXml, String sName)
        {
        assertThat(sXml, containsString("<class name=\"" + sName
                + "\" source=\"@Remote.Executable\" executable=\"true\"/>"));
        }

    private static void assertLambdaTargetEntry(String sXml, String sName)
        {
        assertThat(sXml, containsString("<class name=\"" + sName
                + "\" source=\"@Remote.Executable\" lambda-target=\"true\"/>"));
        }

    private static String read(String sPath)
            throws Exception
        {
        Path path = Paths.get(sPath);
        assertTrue("missing generated file " + path.toAbsolutePath(), Files.exists(path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }
    }
