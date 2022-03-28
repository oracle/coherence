/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.base.Reads;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ResourcesTest
    {
    @Test
    public void shouldFindInputStreamFromFileOnClasspath() throws Exception
        {
        String sResource = "data/test-1.txt";
        URL    url       = getClass().getClassLoader().getResource(sResource);
        assertThat(url, is(notNullValue()));

        byte[] ab        = Reads.read(url);
        String sExpected = new String(ab, StandardCharsets.UTF_8);

        try (InputStream inputStream = Resources.findInputStream(sResource))
            {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String         sLine  = reader.readLine();
            assertThat(sLine, is(sExpected));
            }
        }

    @Test
    public void shouldFindInputStreamFromURL() throws Exception
        {
        String sResource = "data/test-1.txt";
        URL    url       = getClass().getClassLoader().getResource(sResource);
        assertThat(url, is(notNullValue()));

        byte[] ab        = Reads.read(url);
        String sExpected = new String(ab, StandardCharsets.UTF_8);

        try (InputStream inputStream = Resources.findInputStream(url.toExternalForm()))
            {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String         sLine  = reader.readLine();
            assertThat(sLine, is(sExpected));
            }
        }

    @Test
    public void shouldFindInputStreamFromFile() throws Exception
        {
        Path   path      = Files.createTempFile("test", ".txt");
        String sExpected = "testing...";

        try
            {
            try (PrintWriter writer = new PrintWriter(path.toFile()))
                {
                writer.print(sExpected);
                }

            String sFile = path.toFile().getAbsolutePath().replace("\\", "/");
            try (InputStream inputStream = Resources.findInputStream(sFile))
                {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String         sLine  = reader.readLine();
                assertThat(sLine, is(sExpected));
                }
            }
        finally
            {
            Files.deleteIfExists(path);
            }
        }
    }
