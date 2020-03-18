/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.coverage;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.io.FileHelper;
import com.oracle.bedrock.jacoco.Dump;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.JavaApplication;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.tangosol.util.Base;
import org.jacoco.agent.rt.RT;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author jk  2019.12.05
 */
public class CoverageProfile
        implements Profile, Option
    {
    /**
     * Constructs a {@link CoverageProfile}.
     *
     * @param parameters the parameters provided to the {@link CoverageProfile}
     */
    @OptionsByType.Default
    public CoverageProfile(String parameters)
        {
        this(parameters, true);
        }

    /**
     * Constructs a {@link CoverageProfile}.
     *
     * @param sParameters the parameters provided to the {@link CoverageProfile}
     * @param fEnabled    whether coverage should be enabled
     */
    public CoverageProfile(String sParameters, boolean fEnabled)
        {
        if ("auto".equalsIgnoreCase(sParameters))
            {
            CoverageProfile profile = fromSystemProperty();
            this.m_sParameters = profile.m_sParameters;
            this.m_fEnabled = true;
            }
        else
            {
            this.m_sParameters = sParameters;
            this.m_fEnabled = fEnabled;
            }
        }

    @Override
    public void onLaunching(
            Platform platform,
            MetaClass metaClass,
            OptionsByType optionsByType)
        {
        String sJacocoFolder = System.getProperty("jacoco.dest.folder");
        String jacocoFile = sJacocoFolder + "/" + UUID.randomUUID().toString() + ".exec";
        optionsByType.add(SystemProperty.of("jacoco-agent.destfile", jacocoFile));
        optionsByType.add(SystemProperty.of("jacoco-agent.dumponexit", "false"));

        try
            {
            // ensure that Jacoco RT jar is on the classpath
            ClassPath classPath = optionsByType.getOrDefault(ClassPath.class, ClassPath.ofSystem());
            ClassPath jacocoClassPath = ClassPath.ofClass(RT.class);
            optionsByType.add(ClassPath.of(classPath, jacocoClassPath));
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    @Override
    public void onLaunched(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        // there's nothing to after an application has been realized
        }

    @Override
    public void onClosing(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        // prior to closing a JavaApplication we request the JaCoCo telemetry to be dumped
        if (m_fEnabled && application instanceof JavaApplication)
            {
            try
                {
                JavaApplication javaApplication = (JavaApplication) application;

                CompletableFuture<Void> future = javaApplication.submit(new Dump());

                // Wait for the Dump to complete
                future.join();
                }
            catch (IllegalStateException e)
                {
                System.err.println("Could not call Jacoco dump() on remote process: " + e.getMessage());
                }
            }
        }

    /**
     * Create a {@link CoverageProfile}.
     *
     * @param parameters the Jacoco parameters
     * @return a Jacoco profile with the specified parameters
     */
    public static CoverageProfile enabled(String parameters)
        {
        return new CoverageProfile(parameters, true);
        }

    /**
     * Create a {@link CoverageProfile} with coverage disabled.
     *
     * @return a Jacoco profile with coverage disabled
     */
    public static CoverageProfile disabled()
        {
        return new CoverageProfile("", false);
        }

    /**
     * Create a {@link CoverageProfile} with the destination configured from System properties.
     *
     * @return a Jacoco profile with the destination configured from System properties
     */
    public static CoverageProfile fromSystemProperty()
        {
        return fromSystemProperty(null);
        }

    /**
     * Create a {@link CoverageProfile} with the destination configured from System properties.
     *
     * @return a Jacoco profile with the destination configured from System properties
     */
    public static CoverageProfile fromSystemProperty(String extraParams)
        {
        String sJacocoFolder = System.getProperty("jacoco.dest.folder");
        boolean fEnabled = Boolean.getBoolean("jacoco.enabled");

        if (sJacocoFolder == null || sJacocoFolder.trim().isEmpty())
            {
            try
                {
                sJacocoFolder = FileHelper.createTemporaryFolder("jacoco").getCanonicalPath();
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }
            }

        String jacocoFile = sJacocoFolder + "/" + UUID.randomUUID().toString() + ".exec";
        String params = "destfile=" + jacocoFile;

        if (extraParams != null && !extraParams.trim().isEmpty())
            {
            params = params + extraParams.trim();
            }

        return new CoverageProfile(params, fEnabled);
        }
    /**
     * The parameters provided to the {@link CoverageProfile}.
     */
    private final String m_sParameters;
    /**
     * Whether to enabled Jacoco coverage.
     */
    private final boolean m_fEnabled;
    }
