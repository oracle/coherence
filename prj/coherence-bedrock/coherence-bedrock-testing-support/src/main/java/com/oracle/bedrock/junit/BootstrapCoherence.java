/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.junit;

import com.tangosol.net.Coherence;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A JUnit 5 extension annotation that bootstraps a local Coherence  cluster member
 * in the{@link BeforeAllCallback#beforeAll(ExtensionContext)} method and shuts-down
 * Coherence in the {@link AfterAllCallback#afterAll(ExtensionContext)} method.
 *
 * @author Jonathan Knight 2022.06.23
 * @since 22.06
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(BootstrapCoherenceExtension.class)
public @interface BootstrapCoherence
    {
    /**
     * The cache configuration to use.
     *
     * @return the cache configuration to use
     */
    String config() default "";

    /**
     * The mode to bootstrap Coherence in.
     *
     * @return the mode to bootstrap Coherence in
     */
    Coherence.Mode mode() default Coherence.Mode.ClusterMember;

    /**
     * Properties to pass to the default session builder,
     * as key value pairs in the format {code "key=value"}.
     *
     * @return properties to pass to the default session builder
     */
    String[] properties() default {};

    /**
     * The maximum amount of time to wait for Coherence to start.
     *
     * @return the maximum amount of time to wait for Coherence to start
     */
    int timeout() default DEFAULT_TIMEOUT;

    /**
     * The default maximum amount of time to wait for Coherence to start.
     */
    int DEFAULT_TIMEOUT = 300000;

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Session
        {
        }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Cache
        {
        String name();
        }
    }
