/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.provider.Property;

/**
 * A Gradle extension object for security-config generation.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public abstract class SecurityConfigExtension
    {
    //----- constructors ----------------------------------------------------

    /**
     * Default constructor for the SecurityConfigExtension.
     */
    public SecurityConfigExtension()
        {
        }

    //----- SecurityConfigExtension methods --------------------------------

    /**
     * Shall security-config generation be skipped? If not specified, this property
     * ultimately defaults to {@code false}.
     *
     * @return Gradle container object wrapping a Boolean property
     */
    public abstract Property<Boolean> getSkip();

    /**
     * Shall security-config.xml be generated for test classes? Set the property to
     * {@code true}, in order to process test classes. If not specified, this property
     * ultimately defaults to {@code false}.
     *
     * @return Gradle container object wrapping a Boolean property
     */
    public abstract Property<Boolean> getProcessTestClasses();
    }
