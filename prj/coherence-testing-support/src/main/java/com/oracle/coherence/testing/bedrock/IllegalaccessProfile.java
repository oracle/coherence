/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.bedrock;


import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.java.options.Freeform;
import com.oracle.bedrock.runtime.java.options.Freeforms;
import com.oracle.bedrock.runtime.java.options.JvmOptions;


/**
 * Enable configuring module clauses via Java commandline using this profile for JDK 9 and greater.
 *
 * @author jf  2021.03.26
 * @author jk  2021.03.26
 */
public class IllegalaccessProfile
        implements Profile, Option
    {
    // ----- constructors ------------------------------------------------------

    /**
     * Constructs a {@link IllegalaccessProfile}.
     *
     * @param sParameters the parameters provided to the {@link IllegalaccessProfile}
     */
    @OptionsByType.Default
    public IllegalaccessProfile(String sParameters)
        {
        m_sValue = sParameters;
        }

    // ----- Profile methods ---------------------------------------------------

    @Override
    public void onLaunching(
            Platform platform,
            MetaClass metaClass,
            OptionsByType optionsByType)
        {
        Jdk jdk = optionsByType.get(Jdk.class);

        if (jdk.getVersion() > 8)
            {
            // options introduced in jdk 9
            Freeforms jvmOptions = JvmOptions.include();

            // Support was removed in JDK 17 so avoid warning message by not including in JDK 17 and greater
            if (jdk.getVersion() <= 16)
                {
                jvmOptions = jvmOptions.with(new Freeform("--illegal-access=" + m_sValue));
                }

            if (jdk.getVersion() >= 22 && m_sValue.equals("strict"))
                {
                jvmOptions = JvmOptions.include("-Djavax.xml.catalog.resolve=strict",
                                                "-Djdk.xml.jdkcatalog.resolve=strict");
                }

            Freeforms freeforms = optionsByType.get(Freeforms.class);
            for (Freeform freeform : freeforms)
                {
                jvmOptions = jvmOptions.with(freeform);
                }
            optionsByType.add(jvmOptions);
            }

        }

    @Override
    public void onLaunched(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        // there's nothing to do after an application has been realized
        }

    @Override
    public void onClosing(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        // there's nothing to do after an application has been closed
        }

    // ----- data members ------------------------------------------------------

    /**
     * Java option --illegal-access value of "permit", "deny", "warn", "debug"
     */
    String m_sValue;
    }
