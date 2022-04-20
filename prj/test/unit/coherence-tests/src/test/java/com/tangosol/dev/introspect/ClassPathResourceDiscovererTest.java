/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

import com.tangosol.dev.introspect.ClassPathResourceDiscoverer.FileBasedResourceDiscoverer;
import com.tangosol.dev.introspect.ClassPathResourceDiscoverer.JarFileBasedResourceDiscoverer;

import com.tangosol.io.FileHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Resources;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * ClassPathResourceDiscovererTest tests classes contained with
 * {@link ClassPathResourceDiscoverer}.
 *
 * @author hr  2011.10.18
 *
 * @since Coherence 12.1.2
 */
public class ClassPathResourceDiscovererTest
    {

    @Test
    public void simple()
        {
        ClassPathResourceDiscoverer discoverer = new ClassPathResourceDiscoverer();
        Enumeration<URL> enumResources = discoverer.discover("", "com/tangosol/dev/introspect");

        int i = 0;
        for (; enumResources.hasMoreElements(); ++i)
            {
            enumResources.nextElement();
            }
        assertThat(i, greaterThan(0));
        }

    @Test
    public void fileBasedSearch() throws IOException
        {
        File dirBase = FileHelper.createTempDir();
        try
            {
            File.createTempFile("TestClass", ".class", dirBase);

            Enumeration<URL> enumResources = new FileBasedResourceDiscoverer()
                .discover(".*\\.class", dirBase.toURI().toURL());

            int i = 0;
            for (; enumResources.hasMoreElements(); ++i)
                {
                enumResources.nextElement();
                }
            assertThat(i, greaterThan(0));
            }
        finally
            {
            FileHelper.deleteDir(dirBase);
            }
        }

    @Test
    public void jarFileBasedSearch() throws IOException
        {
        JarFileBasedResourceDiscoverer disco = new JarFileBasedResourceDiscoverer();
        String sUrl = createJarFile(ClassPathResourceDiscoverer.class);
        URL url = new URL("jar:" + sUrl + "!/");

        Enumeration<URL> enumResources = disco.discover(".*\\.class", url);

        int i = 0;
        for (; enumResources.hasMoreElements(); ++i)
            {
            enumResources.nextElement();
            }
        new File(new URL(sUrl).getFile()).delete();
        assertThat(i, greaterThan(0));
        }

    public static String createJarFile(Class<?>...aClz) throws IOException
        {
        File file = File.createTempFile("jar-file-test",".jar");
        createJarFile(file.getAbsolutePath(), aClz);
        return file.toURI().toURL().toExternalForm();
        }

    public static void createJarFile(String sFileName, Class<?>...aClz) throws IOException
        {
        createJarFile(sFileName, Base.getContextClassLoader(), Collections.<String>emptyList(), aClz);
        }

    public static void createJarFile(String sFileName, List<String> listResources, Class<?>...aClz) throws IOException
        {
        createJarFile(sFileName, Base.getContextClassLoader(), listResources, aClz);
        }

    public static void createJarFile(String sFileName, ClassLoader loader, List<String> listResources, Class<?>... aClz) throws IOException
        {
        JarOutputStream jos  = null;
        File            file = new File(sFileName);
        try
            {
            List<String> listJarContents = new ArrayList<String>(listResources);
            for (int i = 0; i < aClz.length; ++i)
                {
                Class<?> clz = aClz[i];
                listJarContents.add(clz.getName().replace('.', '/') + ".class");
                }

            jos = new JarOutputStream(new FileOutputStream(file));
            for (int i = 0; i < listJarContents.size(); ++i)
                {
                String sResourceName, sResource = sResourceName = listJarContents.get(i);
                if (listResources.contains(sResource))
                    {
                    File fileResource = new File(sResource);
                    if (fileResource.exists())
                        {
                        sResourceName = fileResource.getName();
                        }
                    }
                JarEntry entry = new JarEntry(sResourceName);

                entry.setTime(System.currentTimeMillis());
                jos.putNextEntry(entry);

                if (sResource.endsWith(".class") || listResources.contains(sResource))
                    {
                    URL         url = Resources.findFileOrResource(sResource, loader);
                    InputStream is  = url == null ? null : url.openStream();

                    int c = 0;
                    while ((c = is.read()) != -1)
                        {
                        jos.write(c);
                        }
                    }
                jos.closeEntry();
                }
            }
        catch(IOException e)
            {
            throw e;
            }
        finally
            {
            if (jos != null)
                {
                jos.close();
                }
            }
        }
    }