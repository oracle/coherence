/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * A Gradle extension object, that contains all settings and properties for the Coherence Gradle plugin.
 *
 * @author Gunnar Hillert  2023.03.16
 * @since 22.06.05
 */
public abstract class CoherenceExtension
    {
    //----- CoherenceExtension methods --------------------------------------

    /**
     * Returns a Gradle container object wrapping a Boolean property. If set to {@code true} we instruct the underlying
     * PortableTypeGenerator to generate debug code in regards the instrumented classes. If not specified, this property
     * ultimately defaults to {@code false}.
     *
     * @return Gradle container object wrapping a Boolean property
     */
    public abstract Property<Boolean> getDebug();

    /**
     * Shall test classes be instrumented by the the underlying PortableTypeGenerator? Set the property to {@code true},
     * in order to instrument test classes. If not specified, this property ultimately defaults to {@code false}.
     *
     * @return Gradle container object wrapping a Boolean property
     */
    public abstract Property<Boolean> getInstrumentTestClasses();

    /**
     * Provide a DirectoryProperty to a custom test classes directory. If not set, it will default
     * to Gradle's default test output directory.
     * @return the DirectoryProperty specifying the test classes directory
     */
    abstract DirectoryProperty getTestClassesDirectory();

    /**
     * Provide a DirectoryProperty to a custom classes directory. If not set, it will default
     * to Gradle's default output directory.
     * @return the DirectoryProperty specifying the main classes directory
     */
    abstract DirectoryProperty getMainClassesDirectory();
    }
