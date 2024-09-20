/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;

import com.tangosol.io.pof.testdata.PortableTypesInnerClass;

import com.tangosol.util.Base;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
* Test for the {@link PofIndexer} class.
* <p/>
* @author Gunnar Hillert  2024.03.10
*/
public class PofIndexerTest
        extends Base
    {

    @Test
    public void testTheDiscoveryOfPortableTypesInInnerClass() throws Exception
        {
        PofIndexer pofIndexer = new PofIndexer();
        pofIndexer.withClasses(List.of(PortableTypesInnerClass.PortableTypeTest1.class));
        Map<String, Integer> portableTypes = pofIndexer.discoverPortableTypes();
        assertEquals(1, portableTypes.size());
        assertEquals(Integer.valueOf(1000), portableTypes.get("com.tangosol.io.pof.testdata.PortableTypesInnerClass$PortableTypeTest1"));
        }

    @Test
    public void testTheDiscoveryOfPortableTypesInASingleClass() throws Exception
        {
        PofIndexer pofIndexer = new PofIndexer();
        List<Class> classesToIndex = List.of(PortableTypesInnerClass.class.getDeclaredClasses())
                .stream().map(clazz->(Class) clazz).toList();
        pofIndexer.withClasses(classesToIndex);
        Map<String, Integer> portableTypes = pofIndexer.discoverPortableTypes();
        assertEquals(4, portableTypes.size());

        assertEquals(Integer.valueOf(1),    portableTypes.get("com.tangosol.io.pof.testdata.PortableTypesInnerClass$PortableTypeTestConflicting"));
        assertEquals(Integer.valueOf(1000), portableTypes.get("com.tangosol.io.pof.testdata.PortableTypesInnerClass$PortableTypeTest1"));
        assertEquals(Integer.valueOf(2000), portableTypes.get("com.tangosol.io.pof.testdata.PortableTypesInnerClass$PortableTypeTestInterface"));
        assertEquals(Integer.valueOf(1234), portableTypes.get("com.tangosol.io.pof.testdata.PortableTypesInnerClass$TestEnum"));
        }

    @Test
    public void testTheDiscoveryOfPortableForPackage() throws Exception
        {
        PofIndexer pofIndexer = new PofIndexer();
        pofIndexer.setPackagesToScan(Set.of("com.tangosol.io.pof.testdata.pkg"));
        Map<String, Integer> portableTypes = pofIndexer.discoverPortableTypes();

        assertEquals(4, portableTypes.size());
        assertEquals(Integer.valueOf(1),    portableTypes.get("com.tangosol.io.pof.testdata.pkg.PortableTypeTestConflicting"));
        assertEquals(Integer.valueOf(1000), portableTypes.get("com.tangosol.io.pof.testdata.pkg.PortableTypeTest1"));
        assertEquals(Integer.valueOf(2000), portableTypes.get("com.tangosol.io.pof.testdata.pkg.PortableTypeTestInterface"));
        assertEquals(Integer.valueOf(1234), portableTypes.get("com.tangosol.io.pof.testdata.pkg.TestEnum"));
        }

    @Test
    public void testTheDiscoveryOfPortableForPackageFiltered() throws Exception
        {
        PofIndexer pofIndexer = new PofIndexer();
        pofIndexer.setPackagesToScan(Set.of("com.tangosol.io.pof.testdata.pkg"));
        pofIndexer.setIncludeFilterPatterns(Set.of(".*TestEnum$"));
        Map<String, Integer> portableTypes = pofIndexer.discoverPortableTypes();

        assertEquals(1, portableTypes.size());
        assertEquals(Integer.valueOf(1234), portableTypes.get("com.tangosol.io.pof.testdata.pkg.TestEnum"));
        }

    @Test
    public void testTheDiscoveryOfPortableTypesInAJarFile() throws Exception
        {
        File jarFile = copyJarFileToTempDirectory("/pof-indexer-test.jar");
        PofIndexer pofIndexer = new PofIndexer().ignoreClasspath(true);
        pofIndexer.withClassesFromJarFile(List.of(jarFile));
        Map<String, Integer> portableTypes = pofIndexer.discoverPortableTypes();

        assertEquals(4, portableTypes.size());
        assertEquals(Integer.valueOf(1),    portableTypes.get("com.tangosol.io.pof.testdata.pkg.PortableTypeTestConflicting"));
        assertEquals(Integer.valueOf(1000), portableTypes.get("com.tangosol.io.pof.testdata.pkg.PortableTypeTest1"));
        assertEquals(Integer.valueOf(2000), portableTypes.get("com.tangosol.io.pof.testdata.pkg.PortableTypeTestInterface"));
        assertEquals(Integer.valueOf(1234), portableTypes.get("com.tangosol.io.pof.testdata.pkg.TestEnum"));
        }

    @Test
    public void testCreatePofIndexWithInnerClass()
        {
        File tempDirectory = createTempDirectory();
        File pofIndexFile = new File(tempDirectory, "META-INF/pof.idx");
        PofIndexer pofIndexer = new PofIndexer();

        pofIndexer.withClasses(List.of(PortableTypesInnerClass.PortableTypeTest1.class));
        pofIndexer.createIndexInDirectory(tempDirectory);

        assertTrue(pofIndexFile.exists());

        Properties properties = getProperties(pofIndexFile);
        assertEquals("1000", properties.getProperty("com.tangosol.io.pof.testdata.PortableTypesInnerClass$PortableTypeTest1"));
        }

    @Test
    public void testCreatePofIndexWithJarFile()
        {
        File jarFile = copyJarFileToTempDirectory("/pof-indexer-test.jar");
        File pofIndexFile = new File(jarFile.getParentFile(), "META-INF/pof.idx");

        PofIndexer pofIndexer = new PofIndexer().ignoreClasspath(true);
        pofIndexer.withClassesFromJarFile(List.of(jarFile));
        pofIndexer.createIndexInDirectory(jarFile.getParentFile());

        assertTrue(pofIndexFile.exists());
        Properties properties = getProperties(pofIndexFile);

        assertFalse(properties.isEmpty());
        assertEquals(4, properties.entrySet().size());

        assertEquals(properties.getProperty("com.tangosol.io.pof.testdata.pkg.PortableTypeTestConflicting"), "1");
        assertEquals(properties.getProperty("com.tangosol.io.pof.testdata.pkg.PortableTypeTest1"), "1000");
        assertEquals(properties.getProperty("com.tangosol.io.pof.testdata.pkg.PortableTypeTestInterface"), "2000");
        assertEquals(properties.getProperty("com.tangosol.io.pof.testdata.pkg.TestEnum"), "1234");
        }

    @Test
    public void testCreatePofIndexWithClassesDirectory() throws Exception
        {
        File classesBaseDirectory = extractJarFileAndReturnParentDirectories("/pof-indexer-test.jar");
        assertTrue(classesBaseDirectory.isDirectory());

        File pofIndexFile = new File(classesBaseDirectory, "META-INF/pof.idx");
        assertFalse(pofIndexFile.exists());
        PofIndexer pofIndexer = new PofIndexer().ignoreClasspath(true);
        pofIndexer.withClassesFromDirectory(List.of(classesBaseDirectory));
        pofIndexer.createIndexInDirectory(classesBaseDirectory);

        assertTrue(pofIndexFile.exists());
        Properties properties = getProperties(pofIndexFile);

        assertFalse(properties.isEmpty());
        assertEquals(4, properties.entrySet().size());

        assertEquals(properties.getProperty("com.tangosol.io.pof.testdata.pkg.PortableTypeTestConflicting"), "1");
        assertEquals(properties.getProperty("com.tangosol.io.pof.testdata.pkg.PortableTypeTest1"), "1000");
        assertEquals(properties.getProperty("com.tangosol.io.pof.testdata.pkg.PortableTypeTestInterface"), "2000");
        assertEquals(properties.getProperty("com.tangosol.io.pof.testdata.pkg.TestEnum"), "1234");
        }

    public File copyJarFileToTempDirectory(String resourceLocation)
        {
        Path resourcePath = Paths.get(resourceLocation);
        String fileName = resourcePath.getFileName().toString();

        File tempDirectory = createTempDirectory();
        File outputFile = new File(tempDirectory, fileName);
        InputStream inputStream = getClass().getResourceAsStream(resourceLocation);

        try (FileOutputStream os = new FileOutputStream(outputFile))
            {
            for (int c = inputStream.read(); c != -1; c = inputStream.read())
                {
                os.write(c);
                }
            }
        catch (IOException e)
            {
            throw new IllegalStateException(
                    String.format("Unable to read InputStream from resource '%s'.", resourceLocation));
            }
        return outputFile;
        }

    private File extractJarFileAndReturnParentDirectories(String resourceLocation)
        {
        File tempDirectory = createTempDirectory();
        List<File> classFiles = extractJarFile(resourceLocation, tempDirectory);
        return tempDirectory;
        }

    private List<File> extractJarFile(String resourceLocation)
        {
        return extractJarFile(resourceLocation, createTempDirectory());
        }

    private List<File> extractJarFile(String resourceLocation, File tempDirectory)
        {
        List<File> files = new ArrayList<>();
        ZipInputStream zin = new ZipInputStream(getClass().getResourceAsStream(resourceLocation));

        try
            {
            ZipEntry entry = null;
            while((entry = zin.getNextEntry()) != null)
                {
                if (entry.isDirectory())
                    {
                    new File(tempDirectory, entry.getName()).mkdir();
                    continue;
                    }
                File file = new File(tempDirectory, entry.getName());
                try (FileOutputStream os = new FileOutputStream(file))
                    {
                    for (int c = zin.read(); c != -1; c = zin.read())
                        {
                        os.write(c);
                        }
                    }
                files.add(file);
                }
            }
        catch (IOException e)
            {
            throw new IllegalStateException("Unable to get the next ZIP file entry.", e);
            }
        return files;
        }

    private File createTempDirectory()
        {
        File temporaryDirectory;
        try
            {
            temporaryDirectory = Files.createTempDirectory("pofindexer_" + System.currentTimeMillis()).toFile();
            }
        catch (IOException e)
            {
            throw new IllegalStateException("Unable to create temporary directory.", e);
            }
        return temporaryDirectory;
        }

    private Properties getProperties(File pofIndexFile)
        {
        Properties properties = new Properties();

        try (InputStream is = new FileInputStream(pofIndexFile))
            {
            properties.load(is);
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        return properties;
        }

    private Set<File> getRoots() throws IOException
        {
        Enumeration<URL> roots = this.getClass().getClassLoader().getResources("");

        Set<File> rootDirs = new HashSet<>();

        while (roots.hasMoreElements())
            {
            URL url = roots.nextElement();
            if (url.getProtocol().equals("jar"))
                {
                continue;
                }

            File root = new File(url.getPath());
            rootDirs.add(root);
            }

        return rootDirs;
        }
    }
