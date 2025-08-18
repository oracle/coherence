/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.graal;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.concurrent.RemoteChannelSerializer;
import com.oracle.bedrock.runtime.concurrent.RemoteEvent;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

import com.oracle.bedrock.runtime.concurrent.callable.RemoteMethodInvocation;
import com.tangosol.coherence.graal.AbstractNativeImageFeature;

import java.lang.annotation.Annotation;

import java.util.Set;

/**
 * A GraalVM native image feature used when building native images
 * for Bedrock testing.
 */
public class BedrockNativeImageFeature
        extends AbstractNativeImageFeature
    {
    @Override
    protected Set<Class<?>> getSupertypes()
        {
        return SUPERTYPES;
        }

    @Override
    protected Set<Class<?>> getSerializableTypes()
        {
        return SERIALIZABLE_TYPES;
        }

    @Override
    protected Set<Class<? extends Annotation>> getAnnotations()
        {
        return ANNOTATIONS;
        }

    @Override
    protected Set<String> getLoadAllClassesFromPackages()
        {
        return Set.of("com.oracle.bedrock", "org.hamcrest");
        }

    // ----- data members ---------------------------------------------------

    /**
     * All subclasses of these types will be included.
     */
    public static final Set<Class<?>> SUPERTYPES = Set.of(
            RemoteRunnable.class,
            RemoteCallable.class,
            RemoteEvent.class,
            RemoteChannelSerializer.class,
            RemoteMethodInvocation.Interceptor.class);

    /**
     * All subclasses of these types will be registered for serialization.
     */
    public static final Set<Class<?>> SERIALIZABLE_TYPES = Set.of(
            RemoteRunnable.class,
            RemoteCallable.class,
            RemoteEvent.class,
            RemoteMethodInvocation.Interceptor.class);

    /**
     * All types with these annotations will be included.
     */
    public static final Set<Class<? extends Annotation>> ANNOTATIONS = Set.of();
    }
