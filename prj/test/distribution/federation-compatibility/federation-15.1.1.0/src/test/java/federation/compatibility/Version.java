/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package federation.compatibility;

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

/**
 * Coherence versions used by the federation compatibility tests.
 *
 * @author Aleks Seovic  2026.05.17
 * @since 15.1.2.0
 */
public enum Version
    {
    Current(Version::getCurrentClassPath),
    Previous(Version::getPreviousClassPath);

    Version(Supplier<ClassPath> supplier)
        {
        f_supplier = supplier;
        }

    /**
     * Return the class path for this version.
     *
     * @return the class path for this version
     */
    public ClassPath getClassPath()
        {
        return f_supplier.get();
        }

    /**
     * Return the class path shared by both current and previous members.
     *
     * @return the shared class path
     */
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

    /**
     * Return the current Coherence member class path.
     *
     * @return the current Coherence member class path
     */
    private static ClassPath getCurrentClassPath()
        {
        return ClassPath.of(getBaseClassPath(), classPathFrom(s_libCurrent));
        }

    /**
     * Return the previous Coherence member class path.
     *
     * @return the previous Coherence member class path
     */
    private static ClassPath getPreviousClassPath()
        {
        return ClassPath.of(getBaseClassPath(), classPathFrom(s_libPrevious));
        }

    /**
     * Return a class path from every jar in a folder.
     *
     * @param dir  the folder containing jars
     *
     * @return the class path
     */
    private static ClassPath classPathFrom(File dir)
        {
        File[] aLib = dir.listFiles();
        assertThat(aLib, is(notNullValue()));

        ClassPath[] aCP = Arrays.stream(aLib)
                .map(File::getAbsolutePath)
                .filter(s -> s.endsWith(".jar"))
                .map(ClassPath::of)
                .toArray(ClassPath[]::new);

        return ClassPath.of(aCP);
        }

    // ----- constants ------------------------------------------------------

    private static final File s_buildFolder = MavenProjectFileUtils.locateBuildFolder(Version.class);

    private static final File s_libCurrent = new File(new File(s_buildFolder, "lib"), "current");

    private static final File s_libPrevious = new File(new File(s_buildFolder, "lib"), "previous");

    // ----- data members ---------------------------------------------------

    private final Supplier<ClassPath> f_supplier;
    }
