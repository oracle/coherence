/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;

import java.io.File;

import java.nio.file.Path;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for {@link Files}.
 */
public class FilesTest
    {
    @Test
    public void shouldConvertNativePaths()
        {
        Path pathRelative = Path.of("credentials", "identity.p12");
        Path pathRelativeWithSpace = Path.of("credential files", "member identity.p12");
        Path pathAbsolute = Path.of(System.getProperty("java.io.tmpdir"))
                .toAbsolutePath().resolve("credential file.p12");

        assertEquals(pathRelative, Files.toPath(pathRelative.toString()));
        assertEquals(pathRelativeWithSpace, Files.toPath(pathRelativeWithSpace.toString()));
        assertEquals(pathAbsolute, Files.toPath(pathAbsolute.toString()));
        }

    @Test
    public void shouldConvertLocalFileUri()
        {
        Path path = Path.of(System.getProperty("java.io.tmpdir"))
                .toAbsolutePath().resolve("credential file.p12");

        assertEquals(path, Files.toPath(path.toUri().toString()));
        }

    @Test
    public void shouldRejectUnsupportedUriForms()
        {
        for (String sPath : Arrays.asList(
                "http://credential.invalid/identity.p12",
                "classpath:identity.p12",
                "jar:file:/credential.jar!/identity.p12",
                "file://credential.invalid/identity.p12",
                "file:/identity.p12?version=1",
                "file:/identity.p12#certificate"))
            {
            assertThrows(IllegalArgumentException.class, () -> Files.toPath(sPath), sPath);
            }
        }

    @Test
    public void shouldRecognizeWindowsDrivePathBeforeUriParsing()
        {
        assumeTrue(File.separatorChar == '\\');

        String sDrive  = System.getenv().getOrDefault("SystemDrive", "C:");
        Path   path    = Path.of(sDrive + "\\coherence\\identity.p12");
        Path   pathDriveRelative = Path.of(sDrive + "coherence\\identity.p12");
        Path   pathUnc = Path.of("\\\\credential.invalid\\identity\\member-one.p12");

        assertTrue(path.isAbsolute());
        assertEquals(path, Files.toPath(path.toString()));
        assertFalse(pathDriveRelative.isAbsolute());
        assertEquals(pathDriveRelative, Files.toPath(pathDriveRelative.toString()));
        assertTrue(pathUnc.isAbsolute());
        assertEquals(pathUnc, Files.toPath(pathUnc.toString()));
        }
    }
