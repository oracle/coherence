/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;

import com.oracle.bedrock.runtime.java.JavaApplication;
import com.oracle.bedrock.runtime.java.LocalJavaApplicationLauncher;
import com.oracle.bedrock.runtime.java.LocalProcessBuilder;

import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.JavaHome;

import com.oracle.bedrock.runtime.options.Executable;

import com.oracle.bedrock.table.Table;

import java.util.Arrays;

/**
 * A specialized Java application launcher for launching a native image.
 *
 * @param <A>  the type of Java application to launch
 */
public class LocalNativeImageLauncher<A extends JavaApplication>
        extends LocalJavaApplicationLauncher<A>
    {
    /**
     * Create a {@link LocalNativeImageLauncher}.
     */
    public LocalNativeImageLauncher()
        {
        }


//    @Override
//    public A launch(Platform      platform,
//                    MetaClass<A> metaClass,
//                    OptionsByType optionsByType) {
//    Arrays.stream(optionsByType.asArray()).forEach(System.out::println);
//
//    System.out.println("CLASS=" + optionsByType.get(ClassName.class).getName());
//
//    return super.launch(platform, metaClass, optionsByType);
//    }

    @Override
    protected String getJavaExecutableName(JavaHome javaHome, Executable executable, Table diagnosticsTable)
        {
        return executable.getName();
        }

    @Override
    protected void processClasspath(Platform platform, OptionsByType optionsByType, OptionsByType launchOptions,
            LocalProcessBuilder processBuilder, Table diagnosticsTable)
        {
        // Nothing to do for native images
        }
    }
