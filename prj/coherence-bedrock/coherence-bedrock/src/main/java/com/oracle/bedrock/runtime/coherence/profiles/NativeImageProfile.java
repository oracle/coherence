/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.profiles;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.ApplicationLauncher;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;

import com.oracle.bedrock.runtime.Profiles;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.LocalNativeImageLauncher;

import com.oracle.bedrock.runtime.java.ClassPath;

import com.oracle.bedrock.runtime.java.JavaApplication;
import com.oracle.bedrock.runtime.options.Executable;

/**
 * A Bedrock profile to enable running GraalVM native images
 */
public class NativeImageProfile
        implements Profile, Option
    {
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <A extends Application> ApplicationLauncher<A> getLauncher(MetaClass<A> metaClass)
        {
        if (metaClass instanceof JavaApplication.MetaClass || metaClass instanceof CoherenceClusterMember.MetaClass)
            {
            return new LocalNativeImageLauncher();
            }
        return null;
        }

    /**
     * Create a {@link NativeImageProfile}.
     */
    @OptionsByType.Default
    public NativeImageProfile(String nativeImage)
        {
        f_nativeImage = nativeImage;
        }

    @Override
    public void onLaunching(Platform platform, MetaClass metaClass, OptionsByType options)
        {
        options.add(Executable.named(f_nativeImage));
        options.remove(ClassPath.class);
        }

    @Override
    public void onLaunched(Platform platform, Application application, OptionsByType optionsByType)
        {
        }

    @Override
    public void onClosing(Platform platform, Application application, OptionsByType optionsByType)
        {
        }

    /**
     * Return {@code true} if the {@link NativeImageProfile} is enabled.
     *
     * @return  {@code true} if the {@link NativeImageProfile} is enabled
     */
    public static boolean isEnabled()
        {
        return Profiles.getProfiles().get(NativeImageProfile.class) != null;
        }

    // ----- data members ---------------------------------------------------

    private final String f_nativeImage;
    }
