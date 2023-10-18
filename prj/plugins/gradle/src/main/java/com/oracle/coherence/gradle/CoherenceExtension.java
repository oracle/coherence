/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.provider.Property;

/**
 * A Gradle extension object, that contains all settings and properties for the Coherence Gradle plugin.
 *
 * @author Gunnar Hillert  2023.03.16
 * @since 22.06.05
 */
public abstract class CoherenceExtension
    {
    //----- constructors ----------------------------------------------------

    /**
     * Default constructor for the CoherenceExtension.
     */
    public CoherenceExtension()
        {
        }

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
     * Shall test classes be instrumented by the underlying PortableTypeGenerator? Set the property to {@code true},
     * in order to instrument test classes. If not specified, this property ultimately defaults to {@code false}.
     *
     * @return Gradle container object wrapping a Boolean property
     */
    public abstract Property<Boolean> getInstrumentTestClasses();

    /**
     * Shall an existing POF XML Schema file be used for instrumentation? If not specified, this property
     * defaults to {@code false}.
     *
     * @return Gradle container object wrapping a Boolean property
     */
    public abstract Property<Boolean> getUsePofSchemaXml();

    /**
     * If {@link CoherenceExtension#getUsePofSchemaXml} is true, then use the path specified by this property to determine
     * the XML file containing Portable Type definitions.
     * @return the relative path to the POF XML definition file, defaults to {@code META-INF/schema.xml}
     */
    public abstract Property<String> getPofSchemaXmlPath();

    }
