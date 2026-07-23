/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression coverage for the Slice A bytecode deny-list and Slice B OIS
 * deny-list cross-check.
 *
 * @author Aleks Seovic  2026.04.30
 * @since 26.04
 */
public class LambdaBytecodeDenylistResourceTest
    {
    @Test
    public void testClassLevelBytecodeDenylistEntriesAreSerializationDenied()
            throws Exception
        {
        for (String sClass : loadClassLevelEntries())
            {
            Class clz = loadIfPresent(sClass);
            if (clz != null)
                {
                assertTrue(sClass, SerializationAllowlist.isDenied(clz));
                }
            }
        }

    private static Class loadIfPresent(String sClass)
            throws Exception
        {
        try
            {
            return Class.forName(sClass);
            }
        catch (ClassNotFoundException e)
            {
            assertFalse("expected a version-specific JDK class: " + sClass, sClass.startsWith("java."));
            return null;
            }
        }

    private static Set<String> loadClassLevelEntries()
            throws Exception
        {
        try (InputStream in = LambdaBytecodeDenylistResourceTest.class.getClassLoader()
                .getResourceAsStream("META-INF/coherence/lambda-bytecode-denylist.txt"))
            {
            assertTrue("deny-list resource missing", in != null);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                {
                return reader.lines()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .filter(s -> !s.startsWith("#"))
                        .filter(s -> !s.contains("#"))
                        .collect(Collectors.toSet());
                }
            }
        }
    }
