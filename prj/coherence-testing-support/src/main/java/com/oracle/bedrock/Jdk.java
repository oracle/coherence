/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock;


import com.oracle.bedrock.illegalaccess.IllegalaccessProfile;


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
        return new Jdk(Integer.valueOf(System.getProperty("java.version").split("-|\\.")[0]));
        }

    /**
     * Specify this option when target remote Jdk differs from bedrock test client.
     *
     * @param version jdk version of 5, 6, 7, 8, 9, 11, 15, 16, 17, ...
     *
     * @return Jdk option
     */
    public static Jdk of(int version)
        {
        return new Jdk(version);
        }

    // ----- data members ------------------------------------------------------

    /**
     * Jdk version of 5-11, 15-17
     */
    private int m_nVersion;
    }
