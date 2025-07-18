/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.profiles;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.*;
import com.oracle.bedrock.runtime.coherence.LocalNativeImageLauncher;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.Argument;
import com.oracle.bedrock.runtime.options.Executable;
import com.tangosol.coherence.config.Config;

import java.io.File;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class NativeImageProfile
        implements Profile, Option
    {
    @Override
    public <A extends Application> ApplicationLauncher<A> getLauncher(MetaClass<A> metaClass)
        {
        return new LocalNativeImageLauncher();
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

    // ----- data members ---------------------------------------------------

    private final String f_nativeImage;
    }
