/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;

import com.tangosol.dev.introspect.ClassAnnotationSeeker;
import com.tangosol.dev.introspect.ClassPathResourceDiscoverer;

import com.tangosol.io.pof.schema.annotation.PortableType;

import com.tangosol.io.pof.testdata.PortableTypesInnerClass;

import com.tangosol.util.Base;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URI;
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

import java.util.stream.Collectors;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

/**
* Test for the {@link PofIndexer} class.
* <p/>
* @author Gunnar Hillert  2024.03.10
*/
public class PofIndexerTest
        extends Base
    {

    /**
     * This test should be eventually moved to {@link com.tangosol.dev.introspect.ClassAnnotationSeekerTest}.
     */
    @Test
    public void testFindClassNamesInJar() throws Exception
        {
        final File jarFile = copyJarFileToTempDirectory("/pof-indexer-test.jar");
        final URI uri = new URI("jar", jarFile.toURI() + "!/", null);

        final List<URL> urls = List.of(uri.toURL());

        final ClassAnnotationSeeker.Dependencies simpleDeps = new ClassAnnotationSeeker.Dependencies();
        simpleDeps.setDiscoverer(new ClassPathResourceDiscoverer.InformedResourceDiscoverer(
                    urls.toArray(new URL[urls.size()])));

        final ClassAnnotationSeeker simpleSeeker  = new ClassAnnotationSeeker(simpleDeps);
        final Set<String> setClassNames = simpleSeeker.findClassNames(PortableType.class);
        assertTrue(setClassNames.size() == 5);
        }

    @Test
    public void testFindClassNamesInClassFiles() throws Exception
        {
        final List<File> classFiles = extractJarFile("/pof-indexer-test.jar");

        final ClassAnnotationSeeker.Dependencies simpleDeps = new ClassAnnotationSeeker.Dependencies();
        simpleDeps.setDiscoverer(new ClassPathResourceDiscoverer.InformedResourceDiscoverer(
                classFiles.stream().map(file ->
                    {
                    try
                        {
                        return file.toURI().toURL();
                        }
                    catch (MalformedURLException e)
                        {
                        throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toSet()).toArray(new URL[classFiles.size()])));

        final ClassAnnotationSeeker simpleSeeker  = new ClassAnnotationSeeker(simpleDeps);
        final Set<String> setClassNames = simpleSeeker.findClassNames(PortableType.class);
        assertTrue(setClassNames.size() == 5);
        }

    @Test
    public void testFindClassNamesInClassDirectory() throws Exception
        {
        Set<File> roots = getRoots();

        final ClassAnnotationSeeker.Dependencies simpleDeps = new ClassAnnotationSeeker.Dependencies();
        simpleDeps.setDiscoverer(new ClassPathResourceDiscoverer.InformedResourceDiscoverer(
                roots.stream().map(file ->
                    {
                    try
                        {
                        return file.toURI().toURL();
                        }
                    catch (MalformedURLException e)
                        {
                        throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toSet()).toArray(new URL[roots.size()])));
        simpleDeps.setPackages(Set.of("com.tangosol.io.pof.testdata.pkg"));
        final ClassAnnotationSeeker simpleSeeker  = new ClassAnnotationSeeker(simpleDeps);
        final Set<String> setClassNames = simpleSeeker.findClassNames(PortableType.class);
        assertEquals(5, setClassNames.size());
        }

    @Test
    public void testTheDiscoveryOfPortableTypesInInnerClass() throws Exception
        {
        final PofIndexer pofIndexer = new PofIndexer();
        pofIndexer.withClasses(List.of(PortableTypesInnerClass.PortableTypeTest1.class));
        Map<String, Integer> portableTypes = pofIndexer.discoverPortableTypes();
        assertTrue(portableTypes.size() == 1);
        }

    @Test
    public void testTheDiscoveryOfPortableTypesInASingleClass() throws Exception
        {
        final PofIndexer pofIndexer = new PofIndexer();
        pofIndexer.withClasses(List.of(PortableTypesInnerClass.class.getDeclaredClasses()));
        Map<String, Integer> portableTypes = pofIndexer.discoverPortableTypes();
        assertTrue(portableTypes.size() == 5);
        }

    @Test
    public void testTheDiscoveryOfPortableForPackage() throws Exception
        {
        final PofIndexer pofIndexer = new PofIndexer();
        pofIndexer.setPackagesToScan(Set.of("com.tangosol.io.pof.testdata.pkg"));
        final Map<String, Integer> portableTypes = pofIndexer.discoverPortableTypes();
        assertTrue(portableTypes.size() == 5);
        }

    @Test
    public void testTheDiscoveryOfPortableForPackageFiltered() throws Exception
        {
        final PofIndexer pofIndexer = new PofIndexer();
        pofIndexer.setPackagesToScan(Set.of("com.tangosol.io.pof.testdata.pkg"));
        pofIndexer.setIncludeFilterPatterns(Set.of(".*TestEnum$"));
        final Map<String, Integer> portableTypes = pofIndexer.discoverPortableTypes();
        assertTrue(portableTypes.size() == 1);
        }

    @Test
    public void testTheDiscoveryOfPortableTypesInAJarFile() throws Exception
        {
        final File jarFile = copyJarFileToTempDirectory("/pof-indexer-test.jar");
        final PofIndexer pofIndexer = new PofIndexer().ignoreClasspath(true);
        pofIndexer.withClassesFromJarFile(List.of(jarFile));
        final Map<String, Integer> portableTypes = pofIndexer.discoverPortableTypes();
        assertTrue(portableTypes.size() == 5);
        }

    @Test
    public void testCreatePofIndexWithInnerClass() throws Exception
        {
        final File tempDirectory = createTempDirectory();
        final File pofIndexFile = new File(tempDirectory, "META-INF/pof.idx");
        final PofIndexer pofIndexer = new PofIndexer();

        pofIndexer.withClasses(List.of(PortableTypesInnerClass.PortableTypeTest1.class));
        pofIndexer.createIndexInDirectory(tempDirectory);

        assertTrue(pofIndexFile.exists());
        }

    @Test
    public void testCreatePofIndexWithJarFile() throws Exception
        {
        final File jarFile = copyJarFileToTempDirectory("/pof-indexer-test.jar");
        final File pofIndexFile = new File(jarFile.getParentFile(), "META-INF/pof.idx");

        final PofIndexer pofIndexer = new PofIndexer().ignoreClasspath(true);
        pofIndexer.withClassesFromJarFile(List.of(jarFile));
        pofIndexer.createIndexInDirectory(jarFile.getParentFile());

        assertTrue(pofIndexFile.exists());
        final Properties properties = new Properties();
        properties.load(new FileInputStream(pofIndexFile));

        assertFalse(properties.isEmpty());
        assertTrue(properties.entrySet().size() == 5);
        }

    @Test
    public void testCreatePofIndexWithClassesDirectory() throws Exception
        {
        final Set<File> classesDirectories = extractJarFileAndReturnParentDirectories("/pof-indexer-test.jar");
        assertTrue(classesDirectories.size() == 1);

        final File classesDirectoryToUse = classesDirectories.iterator().next();
        final File pofIndexFile = new File(classesDirectoryToUse, "META-INF/pof.idx");
        assertFalse(pofIndexFile.exists());
        final PofIndexer pofIndexer = new PofIndexer().ignoreClasspath(true);
        pofIndexer.withClassesFromDirectory(classesDirectories);
        pofIndexer.createIndexInDirectory(classesDirectoryToUse);

        assertTrue(pofIndexFile.exists());
        final Properties properties = new Properties();
        properties.load(new FileInputStream(pofIndexFile));

        assertFalse(properties.isEmpty());
        assertTrue(properties.entrySet().size() == 5);
        }

    public File copyJarFileToTempDirectory(String resourceLocation)
        {
        final Path resourcePath = Paths.get(resourceLocation);
        final String fileName = resourcePath.getFileName().toString();

        final File tempDirectory = createTempDirectory();
        final File outputFile = new File(tempDirectory, fileName);
        final InputStream inputStream = getClass().getResourceAsStream(resourceLocation);

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

    private Set<File> extractJarFileAndReturnParentDirectories(String resourceLocation)
        {
        final List<File> classFiles = extractJarFile(resourceLocation);
        return classFiles.stream().map(File::getParentFile).collect(Collectors.toSet());
        }

    private List<File> extractJarFile(String resourceLocation)
        {
        final File tempDirectory = createTempDirectory();
        final List<File> files = new ArrayList<>();
        final ZipInputStream zin = new ZipInputStream(getClass().getResourceAsStream(resourceLocation));

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
                final File file = new File(tempDirectory, entry.getName());
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
        final File temporaryDirectory;
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

    private Set<File> getRoots() throws IOException
        {
        Enumeration<URL> roots = this.getClass().getClassLoader().getResources("");

        final Set<File> rootDirs = new HashSet<>();

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
