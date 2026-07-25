/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import org.junit.Test;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for default product POF serializer registrations.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class SerializableSerializerDefaultAbsenceTest
    {
    @Test
    public void testProductPofConfigsDoNotRegisterSerializableSerializer()
            throws IOException
        {
        Path root = findRepositoryRoot();
        List<Path> listConfigs;

        try (Stream<Path> stream = Files.walk(root))
            {
            listConfigs = stream
                    .filter(Files::isRegularFile)
                    .filter(this::isProductPofConfig)
                    .collect(Collectors.toList());
            }

        assertTrue("expected product POF configs to be present", listConfigs.size() > 0);

        for (Path path : listConfigs)
            {
            String sXml = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            assertFalse(path.toString(), sXml.contains("com.tangosol.io.pof.SerializableSerializer"));
            assertFalse(path.toString(), sXml.contains("com.tangosol.io.pof.reflect.SerializableSerializer"));
            }
        }

    private boolean isProductPofConfig(Path path)
        {
        String sPath = path.toString().replace('\\', '/');
        String sName = path.getFileName().toString();

        return sPath.contains("/src/main/resources/")
                && !sPath.contains("/target/")
                && (sName.endsWith("pof-config.xml") || sName.equals("pof-config.xml"));
        }

    private static Path findRepositoryRoot()
        {
        Path path = Paths.get("").toAbsolutePath();

        while (path != null)
            {
            if (Files.exists(path.resolve("coherence-core/src/main/resources/coherence-pof-config.xml")))
                {
                return path;
                }
            path = path.getParent();
            }

        throw new IllegalStateException("repository root not found");
        }
    }
