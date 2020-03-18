/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import org.junit.rules.ExternalResource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author jk 2016.03.10
 */
public class PerformanceEnvironment<E extends PerformanceEnvironment>
        extends ExternalResource
    {
    // ----- constructors ---------------------------------------------------

    public PerformanceEnvironment()
        {
        m_classPath = new ArrayList<>();
        m_listKeys  = new ArrayList<>();
        }

    // ----- PerformanceEnvironment methods ---------------------------------

    public E withJar(File... files)
        {
        m_classPath.addAll(Arrays.asList(files));

        return (E) this;
        }

    public List<File> getClassPath()
        {
        return Collections.unmodifiableList(m_classPath);
        }

    public E withKeys(String... keys)
        {
        m_listKeys.addAll(Arrays.asList(keys));

        return (E) this;
        }

    public List<String> getKeys()
        {
        return Collections.unmodifiableList(m_listKeys);
        }

    /**
     * Obtain the folder containing the different Coherence version to test.
     *
     * @return  the folder containing the different Coherence version to test
     */
    protected String getLibFolder()
        {
        String sFolder = System.getProperty("test.lib.folder");

        if (sFolder == null || sFolder.isEmpty())
            {
            throw new RuntimeException("The test.lib.folder property must be set to the location of " +
                                               "the lib folder on the remote host");
            }

        return sFolder;
        }

    // ----- data members ---------------------------------------------------

    private List<File> m_classPath;

    private List<String> m_listKeys;
    }
