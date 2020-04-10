/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.pof;

import com.tangosol.io.pof.PortableTypeSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.File;

import java.lang.reflect.Constructor;

import org.apache.maven.project.MavenProject;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk  2018.03.27
 */
public class InstrumentTestClassesMojoTest
    {
    @Test
    public void shouldInstrumentClass() throws Exception
        {
        MavenProject              project         = new MavenProject();
        File                      fileClasses     = new File(InstrumentTestClassesMojo.class.getProtectionDomain()
                                                             .getCodeSource().getLocation().toURI());
        File                      fileTestClasses = new File(InstrumentTestClassesMojoTest.class.getProtectionDomain()
                                                             .getCodeSource().getLocation().toURI());
        InstrumentTestClassesMojo mojo            = new InstrumentTestClassesMojo();

        mojo.setProject(project);
        mojo.setClassesDirectory(fileClasses);
        mojo.setTestClassesDirectory(fileTestClasses);

        mojo.execute();

        SimplePofContext ctx = new SimplePofContext();
        Class<?>         clz = Class.forName("com.oracle.coherence.maven.pof.ValueOne");
        
        ctx.registerUserType(1000, clz, new PortableTypeSerializer(1000, clz));

        Constructor<?> constructor = clz.getDeclaredConstructor(String.class, String.class);
        Object         oValue      = constructor.newInstance("foo", "bar");
        Binary         binary      = ExternalizableHelper.toBinary(oValue, ctx);
        Object         oResult     = ExternalizableHelper.fromBinary(binary, ctx);

        assertThat(oResult, is(oValue));
        }
    }
