/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.pof;

import com.tangosol.io.pof.PortableTypeSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.File;

import java.lang.reflect.Constructor;

import org.apache.maven.project.MavenProject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk  2018.03.27
 */
public class InstrumentClassesMojoTest
    {
    @Test
    public void shouldInstrumentClassWithCustomSchema() throws Exception
        {
        File directory = m_tempFolder.newFolder("classes");
        CustomSchemaTestSupport.prepare(directory);

        InstrumentClassesMojo mojo = new InstrumentClassesMojo();
        mojo.setProject(new MavenProject());
        mojo.setClassesDirectory(directory);
        mojo.execute();

        CustomSchemaTestSupport.assertInstrumented(directory);
        }

    @Test
    public void shouldInstrumentClass()
            throws Exception
        {
        MavenProject project     = new MavenProject();
        File         fileClasses = m_tempFolder.newFolder("classes");
        String       resource    = "com/oracle/coherence/maven/pof/ValueOne.class";

        CustomSchemaTestSupport.copyClass(fileClasses, resource);

        InstrumentClassesMojo mojo = new InstrumentClassesMojo();

        mojo.setProject(project);
        mojo.setClassesDirectory(fileClasses);

        mojo.execute();

        SimplePofContext ctx = new SimplePofContext();
        Class<?>         clz = CustomSchemaTestSupport.load(fileClasses, resource);

        ctx.registerUserType(1000, clz, new PortableTypeSerializer(1000, clz));

        Constructor<?> constructor = clz.getDeclaredConstructor(String.class, String.class);
        Object         oValue      = constructor.newInstance("foo", "bar");
        Binary         binary      = ExternalizableHelper.toBinary(oValue, ctx);
        Object         oResult     = ExternalizableHelper.fromBinary(binary, ctx);

        assertThat(oResult, is(oValue));
        }

    @Rule
    public TemporaryFolder m_tempFolder = new TemporaryFolder();
    }
