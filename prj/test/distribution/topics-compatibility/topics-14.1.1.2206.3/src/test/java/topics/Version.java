/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.tangosol.net.Coherence;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public enum Version
    {
    Current(Version::getCurrentClassPath),
    Previous(Version::getPreviousClassPath);

    Version(Supplier<ClassPath> supplier)
        {
        m_supplier = supplier;
        }

    public ClassPath getClassPath()
        {
        return m_supplier.get();
        }


    private static ClassPath getBaseClassPath()
        {
        try
            {
            return ClassPath.ofSystem()
                    .excluding(ClassPath.ofClass(Coherence.class));
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    private static ClassPath getCurrentClassPath()
        {
        File[] aLib = s_libCurrent.listFiles();
        assertThat(aLib, is(notNullValue()));

        ClassPath[] aCP = Arrays.stream(aLib)
                .map(File::getAbsolutePath)
                .filter(s -> s.endsWith(".jar"))
                .map(ClassPath::of)
                .toArray(ClassPath[]::new);


        return ClassPath.of(getBaseClassPath(), ClassPath.of(aCP));
        }

    private static ClassPath getPreviousClassPath()
        {
        File[] aLib = s_libPrevious.listFiles();
        assertThat(aLib, is(notNullValue()));

        ClassPath[] aCP = Arrays.stream(aLib)
                .map(File::getAbsolutePath)
                .filter(s -> s.endsWith(".jar"))
                .map(ClassPath::of)
                .toArray(ClassPath[]::new);

        return ClassPath.of(getBaseClassPath(), ClassPath.of(aCP));
        }

    private static final File s_buildFolder = MavenProjectFileUtils.locateBuildFolder(ClosableCluster.class);

    private static final File s_libCurrent = new File(new File(s_buildFolder, "lib"), "current");

    private static final File s_libPrevious = new File(new File(s_buildFolder, "lib"), "previous");

    private final Supplier<ClassPath> m_supplier;
    }
