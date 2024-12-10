/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class URLPasswordProviderTest
    {
    @Test
    public void shouldGetPassword() throws Exception
        {
        Path   path    = Files.createTempFile("test", ".txt");
        String sSecret = "some\nsecret";

        try
            {
            try (PrintWriter writer = new PrintWriter(path.toFile()))
                {
                writer.print(sSecret);
                }

            URLPasswordProvider provider = new URLPasswordProvider(path.toUri().toURL().toExternalForm());

            assertThat(provider.get(), is(sSecret.toCharArray()));
            }
        finally
            {
            Files.deleteIfExists(path);
            }
        }

    @Test
    public void shouldGetFirstLineOfPassword() throws Exception
        {
        Path   path    = Files.createTempFile("test", ".txt");
        String sSecret = "some\nsecret";

        try
            {
            try (PrintWriter writer = new PrintWriter(path.toFile()))
                {
                writer.print(sSecret);
                }

            URLPasswordProvider provider = new URLPasswordProvider(path.toUri().toURL().toExternalForm(), true);

            assertThat(provider.get(), is("some".toCharArray()));
            }
        finally
            {
            Files.deleteIfExists(path);
            }
        }

    @Test
    public void shouldGetNullPassword() throws Exception
        {
        Path path = Files.createTempFile("test", ".txt");

        try
            {
            URLPasswordProvider provider = new URLPasswordProvider(path.toUri().toURL().toExternalForm());
            assertThat(provider.get(), is(new char[0]));
            }
        finally
            {
            Files.deleteIfExists(path);
            }
        }

    @Test
    public void shouldGetEmptyPassword() throws Exception
        {
        Path path = Files.createTempFile("test", ".txt");

        try
            {
            URLPasswordProvider provider   = new URLPasswordProvider(path.toUri().toURL().toExternalForm(), false);
            char[]              acPassword = provider.get();

            assertThat(acPassword, is(notNullValue()));
            assertThat(acPassword.length, is(0));
            }
        finally
            {
            Files.deleteIfExists(path);
            }
        }

    @Test
    public void shouldFailIfURLNotFound() throws Exception
        {
        Path path = Files.createTempDirectory("test");
        File fileBad = new File(path.toFile(), "missing.txt");

        try
            {
            URLPasswordProvider provider = new URLPasswordProvider(fileBad.toURI().toURL().toExternalForm(), false);
            Exception           ex       = assertThrows(Exception.class, provider::get);
            assertThat(ex.getCause(), is(instanceOf(FileNotFoundException.class)));
            }
        finally
            {
            Files.deleteIfExists(path);
            }
        }

    }
