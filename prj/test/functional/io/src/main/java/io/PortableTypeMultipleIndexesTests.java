/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package io;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.tangosol.io.FileHelper;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.schema.annotation.PortableType;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.util.Resources;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.net.URLClassLoader;

import java.nio.file.Files;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Tests for {@link ConfigurablePofContext} auto-generation POF Config for {@link PortableType}
 * with index files in multiple indexes.
 *
 * @author tam  2020.08.21
 */
public class PortableTypeMultipleIndexesTests
    extends AbstractPortableTypeTests
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public PortableTypeMultipleIndexesTests()
        {
        super(getCacheConfig());
        }

    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.pof.enabled", "true");
        System.setProperty("coherence.log.level", "9");

        try {
            // create temporary directories for each of the Jars
            s_dirPersonIndex = FileHelper.createTempDir();
            s_dirAddressIndex = FileHelper.createTempDir();
            s_dirCountryIndex = FileHelper.createTempDir();

            // get the locations of the indexes
            URL urlAddressIndex = Resources.findFileOrResource("META-INF/address-index/custom.idx", null);
            URL urlPersonIndex  = Resources.findFileOrResource("META-INF/person-index/custom.idx", null);
            URL urlCountryIndex = Resources.findFileOrResource("META-INF/country-index/custom.idx", null);

            assertThat(urlAddressIndex, is(notNullValue()));
            assertThat(urlPersonIndex, is(notNullValue()));
            assertThat(urlCountryIndex, is(notNullValue()));

            File filePerson = new File(urlPersonIndex.getFile());
            File fileAddress = new File(urlAddressIndex.getFile());
            File fileCountry = new File(urlAddressIndex.getFile());

            assertThat(filePerson.exists(), is(true));
            assertThat(fileAddress.exists(), is(true));
            assertThat(fileCountry.exists(), is(true));

            // create the META-INF directory
            File dirPersonMetaInf = new File(s_dirPersonIndex, "META-INF");
            File dirAddressMetaInf = new File(s_dirAddressIndex, "META-INF");
            File dirCountryMetaInf = new File(s_dirCountryIndex, "META-INF");

            assertThat(dirAddressMetaInf.mkdir(), is(true));
            assertThat(dirPersonMetaInf.mkdir(), is(true));
            assertThat(dirCountryMetaInf.mkdir(), is(true));

            // copy the files
            assertThat(Files.copy(filePerson.toPath(), new File(dirPersonMetaInf, "pof.idx").toPath()), is(notNullValue()));
            assertThat(Files.copy(fileAddress.toPath(), new File(dirAddressMetaInf, "pof.idx").toPath()), is(notNullValue()));
            assertThat(Files.copy(fileCountry.toPath(), new File(dirCountryMetaInf, "pof.idx").toPath()), is(notNullValue()));

            // create the Jar files
            s_dirJarBase = FileHelper.createTempDir();
            File filePersonJar = new File(s_dirJarBase, "person-idx.jar");
            File fileAddressJar = new File(s_dirJarBase, "address-idx.jar");
            File fileCountryJar = new File(s_dirJarBase, "country-idx.jar");

            createJarFile(s_dirPersonIndex, filePersonJar);
            createJarFile(s_dirAddressIndex, fileAddressJar);
            createJarFile(s_dirCountryIndex, fileCountryJar);

            URL[]          urls              = new URL[] { filePersonJar.toURI().toURL(),
                                                           fileAddressJar.toURI().toURL(),
                                                           fileCountryJar.toURI().toURL()};
            URLClassLoader newURLClassLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
            Thread.currentThread().setContextClassLoader(newURLClassLoader);

            AbstractFunctionalTest._startup();
            }
        catch (Exception e)
            {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            }
        }

    @After
    public void teardown()
        {
        try
            {
            if (s_dirAddressIndex != null)
                {
                FileHelper.deleteDir(s_dirAddressIndex);
                }

            if (s_dirPersonIndex != null)
                {
                FileHelper.deleteDir(s_dirPersonIndex);
                }

            if (s_dirCountryIndex != null)
                {
                FileHelper.deleteDir(s_dirCountryIndex);
                }

            if (s_dirJarBase != null)
                {
                FileHelper.deleteDir(s_dirJarBase);
                }
            }
         catch (IOException e)
             {
             // ignore
             }
        }

    // ----- tests ----------------------------------------------------------

    /**
     * This test validates that classes annotated with PortableType can be
     * used with POF without creating a pof config file.
     *
     * Note: In this particular test we are including two different jars
     * that include an index file called custom-index.idx and test that
     * the indexes can be loaded from multiple locations. 
     * E.g. person-idx.jar should contain just the custom.idx generated from
     * target/classes/data/portabletype/META-INF/person-index/custom.idx
     * address-idx.jar should contain just the custom.idx generated from
     * target/classes/data/portabletype/META-INF/address-index/custom.idx.
     *
     * Both these jars should contain only the custom.idx file in the root.
     */
    @Test
    public void testGenericPortableTypes()
        {
        runTest();
        }

    //----- helpers ---------------------------------------------------------

    /**
     * Use the {@code fileSrc} argument as the root of the jar contents.
     * Package the entire contents using the JAR format to the specified
     * {@code fileDest} {@link File}.
     *
     * @param fileSrc   the root of the jar contents
     * @param fileDest  the location of the destination file
     *
     * @throws IOException  iff an error occurred in packaging the file
     */
    public static void createJarFile(File fileSrc, File fileDest)
            throws IOException
        {
        JarOutputStream jos = null;
        try
            {
            String       sSrc      = fileSrc.getCanonicalPath();
            final int    cSrcName  = sSrc.length() + 1;
            final String sManifest = JarFile.MANIFEST_NAME.replace('/', File.separatorChar);
                         jos       = new JarOutputStream(new FileOutputStream(fileDest));
            List<File> listFiles = new ArrayList<File>();
            listFilesRecursive(fileSrc, listFiles);

            // the JAR file format makes certain assumptions which we honor
            // specifically the priority ordering of META-INF/**
            // resources and the primordial META-INF/MANIFEST.MF
            Collections.sort(listFiles, new Comparator<File>()
                {
                @Override
                public int compare(File fileLHS, File fileRHS)
                    {
                    try
                        {
                        String s1 = fileLHS.getCanonicalPath().substring(cSrcName);
                        String s2 = fileRHS.getCanonicalPath().substring(cSrcName);

                        return s1.equals(sManifest) ? -1
                                : s1.startsWith("META-INF") && !s2.startsWith("META-INF") ? -1
                                    : s2.startsWith("META-INF") && !s1.startsWith("META-INF") ? 1
                                        : s1.compareTo(s2);
                        }
                    catch (IOException e) { }

                    return fileLHS.compareTo(fileRHS);
                    }
                });

            // ensure a manifest is present
            Manifest manifest;
            int      cFiles   = listFiles.size();
            if (cFiles == 0 || !listFiles.get(0).getCanonicalPath().endsWith(sManifest))
                {
                manifest = new Manifest();
                }
            else
                {
                manifest = new Manifest(new FileInputStream(listFiles.remove(0)));
                }

            Attributes            attributes = manifest.getMainAttributes();
            ByteArrayOutputStream bos        = new ByteArrayOutputStream(512);

            if (!attributes.containsKey(new Attributes.Name("Manifest-Version")))
                {
                attributes.putValue("Manifest-Version", "1.0");
                }
            attributes.putValue("Created-By", "Oracle");

            jos.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
            manifest.write(bos);
            jos.write(bos.toByteArray());
            jos.closeEntry();

            // write all the file resources discovered
            for (int i = 0; i < listFiles.size(); ++i)
                {
                File     file      = listFiles.get(i);
                String   sFileName = file.getCanonicalPath();
                String   sResource = sFileName.substring(cSrcName).replace('\\', '/');
                JarEntry entry     = new JarEntry(sResource);

                entry.setTime(file.lastModified());
                jos.putNextEntry(entry);

                if (file.isFile())
                    {
                    InputStream is = new FileInputStream(file);

                    byte[] ab = new byte[4096];
                    int    cb;
                    while ((cb = is.read(ab)) != -1)
                        {
                        jos.write(ab, 0, cb);
                        }
                    }
                jos.closeEntry();
                }
            }
        finally
            {
            if (jos != null)
                {
                jos.close();
                }
            }
        }

    /**
     * From the provided {@code fileSrc}, list all files present in this and
     * all sub directories. This method will only add files to the given
     * {@code listFiles}
     *
     * @param fileSrc    the root directory to query
     * @param listFiles  the files discovered
     */
    protected static void listFilesRecursive(File fileSrc, List<File> listFiles)
        {
        for (File file : fileSrc.listFiles())
            {
            if (file.isDirectory())
                {
                listFilesRecursive(file, listFiles);
                }
            else
                {
                listFiles.add(file);
                }
            }
        }

    //----- constants -------------------------------------------------------

    private static final String INDEX_FILE_NAME = "custom.idx";

    private static final String PATH_SEP = System.getProperty("path.separator");

    private static File s_dirPersonIndex;
    private static File s_dirAddressIndex;
    private static File s_dirCountryIndex;
    private static File s_dirJarBase;
    }
