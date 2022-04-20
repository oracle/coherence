/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.bedrock;


import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;

import com.oracle.coherence.testing.CheckJDK;


/**
 * Jdk version to use for Profiles specific to a JDK.
 *
 * @see IllegalaccessProfile
 *
 * @author jf  2021.03.26
 * @author jk  2021.03.26
 */
public class Jdk implements Option
    {
    // ----- constructors ------------------------------------------------------

    /**
     * Target Jdk option.
     *
     * @param version  target java jvm version
     */
    private Jdk(int version)
        {
        m_nVersion = version;
        }

    /**
     * Specify target remote Jdk when it differs from bedrock test client.
     *
     * @param sJavaHome  JAVA_HOME file path
     */
    private Jdk(String sJavaHome)
        {
        m_nVersion = CheckJDK.computeVersionFromJavaHome(sJavaHome);
        }

    /**
     * Return target jdk version
     *
     * @return jdk version
     */
    public int getVersion()
        {
        return m_nVersion;
        }

    @OptionsByType.Default
    public static Jdk auto()
        {
        // lookup JDK version for current Jvm
        return new Jdk(CheckJDK.computeVersion(System.getProperty("java.version")));
        }

    /**
     * Specify this option when target remote Jdk differs from bedrock test client.
     *
     * @param sJavaHome  JAVA_HOME file path
     *
     * @return Jdk option
     */
    public static Jdk of(String sJavaHome)
        {
        return new Jdk(sJavaHome);
        }

    // ----- data members ------------------------------------------------------

    /**
     * Jdk version of 5-11, 15 to latest version
     */
    private int m_nVersion;
    }
