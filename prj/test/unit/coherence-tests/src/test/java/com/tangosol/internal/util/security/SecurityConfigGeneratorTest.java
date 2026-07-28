/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.tangosol.internal.util.security.fixtures.dual.DualFixture;
import com.tangosol.internal.util.security.fixtures.executable.DualExecutableAllowedFixture;
import com.tangosol.internal.util.security.fixtures.executable.ExecutableFixture;
import com.tangosol.internal.util.security.fixtures.executable.ExecutableOuterFixture;
import com.tangosol.internal.util.security.fixtures.lambdatarget.AbstractExecutableFixture;
import com.tangosol.internal.util.security.fixtures.lambdatarget.InheritedLambdaTargetFixture;
import com.tangosol.internal.util.security.fixtures.lambdatarget.LocalLambdaTargetFixture;
import com.tangosol.internal.util.security.fixtures.lambdatarget.MultiMethodLambdaTargetFixture;
import com.tangosol.internal.util.security.fixtures.lambdatarget.NonSerializableLambdaTargetFixture;
import com.tangosol.internal.util.security.fixtures.lambdatarget.SamLambdaTargetFixture;
import com.tangosol.internal.util.security.fixtures.pkgnonrecursive.PackageAllowedClass;
import com.tangosol.internal.util.security.fixtures.pkgnonrecursive.sub.SubPackageExcludedClass;
import com.tangosol.internal.util.security.fixtures.pkgrecursive.sub.SubPackageAllowedClass;
import com.tangosol.internal.util.security.fixtures.plain.PlainFixture;
import com.tangosol.internal.util.security.fixtures.portable.PortableFixture;
import com.tangosol.internal.util.security.fixtures.type.NonRecursiveAllowedType;
import com.tangosol.internal.util.security.fixtures.type.RecursiveAllowedType;

import java.io.InputStream;

import java.net.URI;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link SecurityConfigGenerator}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class SecurityConfigGeneratorTest
    {
    @Rule
    public TemporaryFolder m_folder = new TemporaryFolder();

    @Test
    public void shouldIncludeTypeAllowedByDefault()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, RecursiveAllowedType.class);

        Map<String, String> mapEntries = entries(SecurityConfigGenerator.generate(dir));

        assertThat(mapEntries.get(RecursiveAllowedType.class.getName()), is(SecurityConfigGenerator.SOURCE_REMOTE_ALLOWED));
        }

    @Test
    public void shouldIncludeNestedTypeWhenTypeAllowedIsRecursive()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, RecursiveAllowedType.class);
        copyClass(dir, RecursiveAllowedType.Nested.class);

        Map<String, String> mapEntries = entries(SecurityConfigGenerator.generate(dir));

        assertThat(mapEntries.get(RecursiveAllowedType.class.getName()), is(SecurityConfigGenerator.SOURCE_REMOTE_ALLOWED));
        assertThat(mapEntries.get(RecursiveAllowedType.Nested.class.getName()), is(SecurityConfigGenerator.SOURCE_REMOTE_ALLOWED));
        }

    @Test
    public void shouldExcludeNestedTypeWhenTypeAllowedIsNotRecursive()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, NonRecursiveAllowedType.class);
        copyClass(dir, NonRecursiveAllowedType.Nested.class);

        Map<String, String> mapEntries = entries(SecurityConfigGenerator.generate(dir));

        assertThat(mapEntries.get(NonRecursiveAllowedType.class.getName()), is(SecurityConfigGenerator.SOURCE_REMOTE_ALLOWED));
        assertFalse(mapEntries.containsKey(NonRecursiveAllowedType.Nested.class.getName()));
        }

    @Test
    public void shouldIncludeRecursivePackageAndSubPackages()
            throws Exception
        {
        Path dir = classesDir();
        copyResource(dir, "com/tangosol/internal/util/security/fixtures/pkgrecursive/package-info.class");
        copyClass(dir, com.tangosol.internal.util.security.fixtures.pkgrecursive.PackageAllowedClass.class);
        copyClass(dir, SubPackageAllowedClass.class);

        Map<String, String> mapEntries = entries(SecurityConfigGenerator.generate(dir));

        assertThat(mapEntries.get(com.tangosol.internal.util.security.fixtures.pkgrecursive.PackageAllowedClass.class.getName()),
                is(SecurityConfigGenerator.SOURCE_REMOTE_ALLOWED_PACKAGE));
        assertThat(mapEntries.get(SubPackageAllowedClass.class.getName()),
                is(SecurityConfigGenerator.SOURCE_REMOTE_ALLOWED_PACKAGE));
        }

    @Test
    public void shouldIncludeOnlyDirectPackageWhenPackageAllowedIsNotRecursive()
            throws Exception
        {
        Path dir = classesDir();
        copyResource(dir, "com/tangosol/internal/util/security/fixtures/pkgnonrecursive/package-info.class");
        copyClass(dir, PackageAllowedClass.class);
        copyClass(dir, SubPackageExcludedClass.class);

        Map<String, String> mapEntries = entries(SecurityConfigGenerator.generate(dir));

        assertThat(mapEntries.get(PackageAllowedClass.class.getName()), is(SecurityConfigGenerator.SOURCE_REMOTE_ALLOWED_PACKAGE));
        assertFalse(mapEntries.containsKey(SubPackageExcludedClass.class.getName()));
        }

    @Test
    public void shouldIncludePortableTypeWithPortableSource()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, PortableFixture.class);

        Map<String, String> mapEntries = entries(SecurityConfigGenerator.generate(dir));

        assertThat(mapEntries.get(PortableFixture.class.getName()), is(SecurityConfigGenerator.SOURCE_PORTABLE_TYPE));
        }

    @Test
    public void shouldPreferPortableTypeSourceForDualMatch()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, DualFixture.class);

        Map<String, String> mapEntries = entries(SecurityConfigGenerator.generate(dir));

        assertThat(mapEntries.size(), is(1));
        assertThat(mapEntries.get(DualFixture.class.getName()), is(SecurityConfigGenerator.SOURCE_PORTABLE_TYPE));
        }

    @Test
    public void shouldIncludeExecutableType()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, ExecutableFixture.class);

        Map<String, SecurityConfigGenerator.Entry> mapEntries = entryMap(SecurityConfigGenerator.generate(dir));

        assertThat(mapEntries.size(), is(1));
        assertThat(mapEntries.get(ExecutableFixture.class.getName()).getSource(),
                is(SecurityConfigGenerator.SOURCE_REMOTE_EXECUTABLE));
        assertTrue(mapEntries.get(ExecutableFixture.class.getName()).isExecutable());
        }

    @Test
    public void shouldIncludeInheritedSamInterfaceAsLambdaTarget()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, InheritedLambdaTargetFixture.class);

        Map<String, SecurityConfigGenerator.Entry> mapEntries = entryMap(SecurityConfigGenerator.generate(dir));

        assertThat(mapEntries.size(), is(1));
        assertThat(mapEntries.get(InheritedLambdaTargetFixture.class.getName()).getSource(),
                is(SecurityConfigGenerator.SOURCE_REMOTE_EXECUTABLE));
        assertFalse(mapEntries.get(InheritedLambdaTargetFixture.class.getName()).isExecutable());
        assertTrue(mapEntries.get(InheritedLambdaTargetFixture.class.getName()).isLambdaTarget());
        }

    @Test
    public void shouldIncludeLocalSamInterfaceAsLambdaTarget()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, LocalLambdaTargetFixture.class);

        Map<String, SecurityConfigGenerator.Entry> mapEntries = entryMap(SecurityConfigGenerator.generate(dir));

        assertThat(mapEntries.size(), is(1));
        assertFalse(mapEntries.get(LocalLambdaTargetFixture.class.getName()).isExecutable());
        assertTrue(mapEntries.get(LocalLambdaTargetFixture.class.getName()).isLambdaTarget());
        }

    @Test
    public void shouldExcludeInterfaceWithoutFunctionalInterface()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, SamLambdaTargetFixture.class);

        Map<String, SecurityConfigGenerator.Entry> mapEntries = entryMap(SecurityConfigGenerator.generate(dir));

        assertFalse(mapEntries.containsKey(SamLambdaTargetFixture.class.getName()));
        }

    @Test
    public void shouldExcludeNonSerializableFunctionalInterface()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, NonSerializableLambdaTargetFixture.class);

        Map<String, SecurityConfigGenerator.Entry> mapEntries = entryMap(SecurityConfigGenerator.generate(dir));

        assertFalse(mapEntries.containsKey(NonSerializableLambdaTargetFixture.class.getName()));
        }

    @Test
    public void shouldFailStrictModeForNonSerializableFunctionalInterface()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, NonSerializableLambdaTargetFixture.class);

        String sPrevious = System.getProperty("security.config.strict");
        System.setProperty("security.config.strict", "true");
        try
            {
            SecurityConfigGenerator.generate(dir);
            fail("strict mode should reject non-serializable lambda targets");
            }
        catch (IllegalStateException e)
            {
            assertThat(e.getMessage(), containsString(NonSerializableLambdaTargetFixture.class.getName()));
            }
        finally
            {
            if (sPrevious == null)
                {
                System.clearProperty("security.config.strict");
                }
            else
                {
                System.setProperty("security.config.strict", sPrevious);
                }
            }
        }

    @Test
    public void shouldExcludeMultiMethodExecutableInterface()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, MultiMethodLambdaTargetFixture.class);

        Map<String, SecurityConfigGenerator.Entry> mapEntries = entryMap(SecurityConfigGenerator.generate(dir));

        assertFalse(mapEntries.containsKey(MultiMethodLambdaTargetFixture.class.getName()));
        }

    @Test
    public void shouldExcludeAbstractExecutableClass()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, AbstractExecutableFixture.class);

        Map<String, SecurityConfigGenerator.Entry> mapEntries = entryMap(SecurityConfigGenerator.generate(dir));

        assertFalse(mapEntries.containsKey(AbstractExecutableFixture.class.getName()));
        }

    @Test
    public void shouldPreferExecutableForAllowedExecutableCollision()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, DualExecutableAllowedFixture.class);

        Map<String, SecurityConfigGenerator.Entry> mapEntries = entryMap(SecurityConfigGenerator.generate(dir));

        assertThat(mapEntries.size(), is(1));
        assertThat(mapEntries.get(DualExecutableAllowedFixture.class.getName()).getSource(),
                is(SecurityConfigGenerator.SOURCE_REMOTE_EXECUTABLE));
        assertTrue(mapEntries.get(DualExecutableAllowedFixture.class.getName()).isExecutable());
        }

    @Test
    public void shouldIncludeNestedTypeOnlyWhenNestedTypeIsExecutable()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, ExecutableOuterFixture.class);
        copyClass(dir, ExecutableOuterFixture.Nested.class);

        Map<String, SecurityConfigGenerator.Entry> mapEntries = entryMap(SecurityConfigGenerator.generate(dir));

        assertFalse(mapEntries.containsKey(ExecutableOuterFixture.class.getName()));
        assertThat(mapEntries.get(ExecutableOuterFixture.Nested.class.getName()).getSource(),
                is(SecurityConfigGenerator.SOURCE_REMOTE_EXECUTABLE));
        assertTrue(mapEntries.get(ExecutableOuterFixture.Nested.class.getName()).isExecutable());
        }

    @Test
    public void shouldWriteValidEmptyConfigForEmptyDirectory()
            throws Exception
        {
        Path   dir = classesDir();
        byte[] xml = SecurityConfigGenerator.toXml(SecurityConfigGenerator.generate(dir));

        assertTrue(new String(xml, "UTF-8").contains("<allowed-classes/>"));
        validate(xml);
        }

    @Test
    public void shouldProduceXsdValidOutput()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, RecursiveAllowedType.class);
        copyClass(dir, PlainFixture.class);
        copyClass(dir, ExecutableFixture.class);

        byte[] xml = SecurityConfigGenerator.toXml(SecurityConfigGenerator.generate(dir));

        validate(xml);
        assertFalse(new String(xml, "UTF-8").contains(PlainFixture.class.getName()));
        assertTrue(new String(xml, "UTF-8").contains("executable=\"true\""));
        }

    @Test
    public void shouldWriteLambdaTargetAttribute()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, InheritedLambdaTargetFixture.class);

        byte[] xml = SecurityConfigGenerator.toXml(SecurityConfigGenerator.generate(dir));
        String sXml = new String(xml, "UTF-8");

        validate(xml);
        assertTrue(sXml.contains("lambda-target=\"true\""));
        assertFalse(sXml.contains("executable=\"true\""));
        }

    @Test
    public void shouldProduceDeterministicOutput()
            throws Exception
        {
        Path dir = classesDir();
        copyClass(dir, PortableFixture.class);
        copyClass(dir, RecursiveAllowedType.class);
        copyClass(dir, RecursiveAllowedType.Nested.class);

        byte[] xmlOne = SecurityConfigGenerator.toXml(SecurityConfigGenerator.generate(dir));
        byte[] xmlTwo = SecurityConfigGenerator.toXml(SecurityConfigGenerator.generate(dir));

        assertThat(xmlOne, is(xmlTwo));
        }

    // ----- helper methods -------------------------------------------------

    private Path classesDir()
            throws Exception
        {
        return m_folder.newFolder().toPath();
        }

    private static Map<String, String> entries(SecurityConfigGenerator.GeneratedConfig config)
        {
        Map<String, String> mapEntries = new HashMap<>();
        List<SecurityConfigGenerator.Entry> listEntries = config.getEntries();
        for (SecurityConfigGenerator.Entry entry : listEntries)
            {
            mapEntries.put(entry.getName(), entry.getSource());
            }
        return mapEntries;
        }

    private static Map<String, SecurityConfigGenerator.Entry> entryMap(SecurityConfigGenerator.GeneratedConfig config)
        {
        Map<String, SecurityConfigGenerator.Entry> mapEntries = new HashMap<>();
        List<SecurityConfigGenerator.Entry> listEntries = config.getEntries();
        for (SecurityConfigGenerator.Entry entry : listEntries)
            {
            mapEntries.put(entry.getName(), entry);
            }
        return mapEntries;
        }

    private static void validate(byte[] xml)
            throws Exception
        {
        try (InputStream inSchema = SecurityConfigGenerator.class.getResourceAsStream("/coherence-security-config.xsd"))
            {
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                    .newSchema(new StreamSource(inSchema))
                    .newValidator()
                    .validate(new StreamSource(new java.io.ByteArrayInputStream(xml)));
            }
        }

    private static void copyClass(Path dir, Class<?> clz)
            throws Exception
        {
        copyResource(dir, clz.getName().replace('.', '/') + ".class");
        }

    private static void copyResource(Path dir, String sResource)
            throws Exception
        {
        URI  uri  = SecurityConfigGeneratorTest.class.getClassLoader().getResource(sResource).toURI();
        Path from = java.nio.file.Paths.get(uri);
        Path to   = dir.resolve(sResource);

        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
