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
public class InstrumentTestClassesMojoTest
    {
    @Test
    public void shouldInstrumentTestClassWithCustomSchema() throws Exception
        {
        File classes     = m_tempFolder.newFolder("classes");
        File testClasses = m_tempFolder.newFolder("test-classes");
        CustomSchemaTestSupport.prepare(testClasses);

        InstrumentTestClassesMojo mojo = new InstrumentTestClassesMojo();
        mojo.setProject(new MavenProject());
        mojo.setClassesDirectory(classes);
        mojo.setTestClassesDirectory(testClasses);
        mojo.execute();

        CustomSchemaTestSupport.assertInstrumented(testClasses);
        }

    @Test
    public void shouldInstrumentClass()
            throws Exception
        {
        MavenProject              project         = new MavenProject();
        File                      fileClasses     = m_tempFolder.newFolder("classes");
        File                      fileTestClasses = m_tempFolder.newFolder("test-classes");
        InstrumentTestClassesMojo mojo            = new InstrumentTestClassesMojo();
        String                    resource        = "com/oracle/coherence/maven/pof/ValueOne.class";

        CustomSchemaTestSupport.copyClass(fileTestClasses, resource);
        CustomSchemaTestSupport.copyClass(fileClasses, "com/oracle/coherence/maven/pof/ValueTwo.class");

        mojo.setProject(project);
        mojo.setClassesDirectory(fileClasses);
        mojo.setTestClassesDirectory(fileTestClasses);

        mojo.execute();

        SimplePofContext ctx = new SimplePofContext();
        Class<?>         clz = CustomSchemaTestSupport.load(fileTestClasses, resource);

        ctx.registerUserType(1000, clz, new PortableTypeSerializer(1000, clz));

        Constructor<?> constructor = clz.getDeclaredConstructor(String.class, String.class);
        Object         oValue      = constructor.newInstance("foo", "bar");
        Binary         binary      = ExternalizableHelper.toBinary(oValue, ctx);
        Object         oResult     = ExternalizableHelper.fromBinary(binary, ctx);

        assertThat(oResult, is(oValue));

        Class<?> clzMain = CustomSchemaTestSupport.load(fileClasses, "com/oracle/coherence/maven/pof/ValueTwo.class");

        ctx.registerUserType(2000, clzMain, new PortableTypeSerializer(2000, clzMain));

        Object mainValue = clzMain.getConstructor(String.class, String.class).newInstance("main", "value");

        assertThat(ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(mainValue, ctx), ctx), is(mainValue));
        }

    @Rule
    public TemporaryFolder m_tempFolder = new TemporaryFolder();
    }
