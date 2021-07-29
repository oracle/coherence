/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.generator;

import com.oracle.coherence.common.schema.Schema;

import com.tangosol.io.pof.EvolvableObject;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.io.pof.PortableTypeSerializer;
import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.io.pof.generator.data.TestClassWithNoId;
import com.tangosol.io.pof.schema.annotation.internal.Instrumented;

import com.tangosol.util.Base;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.io.File;

import java.net.URL;
import java.net.URLClassLoader;

import java.nio.file.Files;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2020.07.10
 */
public class PortableTypeGeneratorTest
    {
    @Test
    public void shouldCreateSchemaForPackagedClass() throws Exception
        {
        String         sClassName   = PackagedType.class.getName();
        URL            url          = getClass().getResource("/" + sClassName.replaceAll("\\.", "/") + ".class");
        File           fileClass    = new File(url.toURI());
        Map<String, ?> env           = new HashMap<>();

        Schema schema = PortableTypeGenerator.createSchema(fileClass, env);
        assertThat(schema, is(notNullValue()));
        }

    @Test
    public void shouldInstrumentPackagedClass() throws Exception
        {
        String         sClassName   = PackagedType.class.getName();
        URL            url          = getClass().getResource("/" + sClassName.replaceAll("\\.", "/") + ".class");
        File           fileClass    = new File(url.toURI());
        byte[]         abBytes      = Files.readAllBytes(fileClass.toPath());
        Properties     properties   = new Properties();
        Map<String, ?> env          = new HashMap<>();
        byte[]         instrumented = PortableTypeGenerator.instrumentClass(fileClass, abBytes, 0, abBytes.length, properties, env);

        assertThat(instrumented, is(notNullValue()));

        ByteArrayClassLoader loader = new ByteArrayClassLoader(Collections.singletonMap(sClassName, instrumented));
        Class<?> instrumentedClass = loader.findClass(PackagedType.class.getName());

        assertThat(instrumentedClass.isAnnotationPresent(Instrumented.class), is(true));
        assertThat(PortableObject.class.isAssignableFrom(instrumentedClass), is(true));
        assertThat(EvolvableObject.class.isAssignableFrom(instrumentedClass), is(true));
        }

    @Test
    public void shouldCreateSchemaForPackagelessClass() throws Exception
        {
        String         sClassName   = "NonPackagedType";
        URL            url          = getClass().getResource("/" + sClassName + ".class");
        File           fileClass    = new File(url.toURI());
        Map<String, ?> env           = new HashMap<>();

        Schema schema = PortableTypeGenerator.createSchema(fileClass, env);
        assertThat(schema, is(notNullValue()));
        }

    @Test
    @SuppressWarnings("rawtypes")
    public void shouldInstrumentClassWithNoId() throws Exception
        {
        String         sClassName   = TestClassWithNoId.class.getName();
        URL            url          = getClass().getResource("/" + sClassName.replaceAll("\\.", "/") + ".class");
        File           fileClass    = new File(url.toURI());
        byte[]         abBytes      = Files.readAllBytes(fileClass.toPath());
        Properties     properties   = new Properties();
        Map<String, ?> env          = new HashMap<>();
        byte[]         instrumented = PortableTypeGenerator.instrumentClass(fileClass, abBytes, 0, abBytes.length, properties, env);

        SimplePofContext ctx = new SimplePofContext();
        
        ByteArrayClassLoader loader = new ByteArrayClassLoader(Collections.singletonMap(sClassName, instrumented));
        Class<?> instrumentedClass = loader.findClass(TestClassWithNoId.class.getName());

        ctx.registerUserType(1, instrumentedClass, new PortableTypeSerializer(1, instrumentedClass));

        Object testClass = instrumentedClass.getDeclaredConstructor(String.class).newInstance("value");
        
        Binary            binTestClass  = ExternalizableHelper.toBinary(testClass, ctx);
        Object            result        = ExternalizableHelper.fromBinary(binTestClass, ctx);
        assertThat(result.equals(testClass), is(true));
        }

    @Test
    public void shouldInstrumentPackagelessClass() throws Exception
        {
        String         sClassName   = "NonPackagedType";
        URL            url          = getClass().getResource("/" + sClassName + ".class");
        File           fileClass    = new File(url.toURI());
        byte[]         abBytes      = Files.readAllBytes(fileClass.toPath());
        Properties     properties   = new Properties();
        Map<String, ?> env          = new HashMap<>();
        byte[]         instrumented = PortableTypeGenerator.instrumentClass(fileClass, abBytes, 0, abBytes.length, properties, env);

        assertThat(instrumented, is(notNullValue()));

        ByteArrayClassLoader loader = new ByteArrayClassLoader(Collections.singletonMap(sClassName, instrumented));
        Class<?> instrumentedClass = loader.findClass(sClassName);

        assertThat(instrumentedClass.isAnnotationPresent(Instrumented.class), is(true));
        assertThat(PortableObject.class.isAssignableFrom(instrumentedClass), is(true));
        assertThat(EvolvableObject.class.isAssignableFrom(instrumentedClass), is(true));
        }

    // ----- inner class: ByteArrayClassLoader ------------------------------

    public static class ByteArrayClassLoader
            extends URLClassLoader
        {
        public ByteArrayClassLoader(Map<String, byte[]> mapClasses)
            {
            super(new URL[0], Base.getContextClassLoader());
            m_mapClasses = mapClasses;
            }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException
            {
            byte[] classBytes = m_mapClasses.get(name);
            if (classBytes != null)
                {
                return defineClass(name, classBytes, 0, classBytes.length);
                }
            return super.findClass(name);
            }

        private final Map<String, byte[]> m_mapClasses;
        }
    }
