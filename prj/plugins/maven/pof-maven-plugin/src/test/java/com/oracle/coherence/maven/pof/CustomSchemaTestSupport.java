/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.pof;

import com.tangosol.io.pof.PortableTypeSerializer;
import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.io.pof.reflect.PofValue;
import com.tangosol.io.pof.reflect.PofValueParser;
import com.tangosol.io.pof.schema.annotation.internal.Instrumented;
import com.tangosol.io.pof.schema.annotation.internal.PofIndex;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.File;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Isolated inputs and serialization assertions for the two instrumentation goals.
 *
 * @author Vaso Putica  2026.11.05
 * @since 22.06
 */
final class CustomSchemaTestSupport
    {
    static void prepare(File directory)
            throws Exception
        {
        copyClass(directory, CLASS_RESOURCE);
        assertNull(load(directory, CLASS_RESOURCE).getAnnotation(Instrumented.class));

        Path schema = directory.toPath().resolve("META-INF/schema.xml");
        Files.createDirectories(schema.getParent());

        try (InputStream input = CustomSchemaTestSupport.class.getResourceAsStream("/custom-schema.xml"))
            {
            assertNotNull(input);
            Files.copy(input, schema);
            }
        }

    static void copyClass(File directory, String resource)
            throws Exception
        {
        Path fileClass = directory.toPath().resolve(resource);
        Files.createDirectories(fileClass.getParent());

        try (InputStream input = CustomSchemaTestSupport.class.getResourceAsStream("/" + resource))
            {
            assertNotNull(input);
            Files.copy(input, fileClass);
            }
        }

    static void assertInstrumented(File directory)
            throws Exception
        {
        Class<?> clz = load(directory, CLASS_RESOURCE);

        assertNotNull(clz.getAnnotation(Instrumented.class));
        assertEquals(0, clz.getDeclaredField("m_sSecond").getAnnotation(PofIndex.class).value());
        assertEquals(1, clz.getDeclaredField("m_sFirst").getAnnotation(PofIndex.class).value());

        SimplePofContext context = new SimplePofContext();
        context.registerUserType(40259, clz, new PortableTypeSerializer(40259, clz));

        Object value  = clz.getConstructor(String.class, String.class).newInstance("first & value", "second value");
        Binary binary = ExternalizableHelper.toBinary(value, context);

        assertOrder(binary, context);

        Object result = ExternalizableHelper.fromBinary(binary, context);

        assertEquals(clz, result.getClass());
        assertOrder(ExternalizableHelper.toBinary(result, context), context);
        }

    private static void assertOrder(Binary binary, SimplePofContext context)
        {
        PofValue value = PofValueParser.parse(binary, context).getChild(40259);

        // the XML reverses the default alphabetical field order
        assertEquals("second value", value.getChild(0).getValue());
        assertEquals("first & value", value.getChild(1).getValue());
        }

    static Class<?> load(File directory, String resource)
            throws Exception
        {
        byte[] bytes = Files.readAllBytes(directory.toPath().resolve(resource));

        // define a fresh copy so instrumentation never mutates a loaded test fixture
        return new ClassLoader(CustomSchemaTestSupport.class.getClassLoader())
            {
            Class<?> define()
                {
                return defineClass(null, bytes, 0, bytes.length);
                }
            }.define();
        }

    private static final String CLASS_RESOURCE = "com/oracle/coherence/maven/pof/SchemaValue.class";
    }
