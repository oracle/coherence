/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

/**
 * A RegistrationBehavior is used by numerous registries for the
 * purpose of specifying the required behavior when registering an artifact.
 *
 * @author bo  2012.10
 * @since Coherence 12.1.2
 */
public enum RegistrationBehavior
    {
    /**
     * Specifies that registration should be ignored and skipped (without
     * raising an error or exception) if the artifact to be registered is
     * already known.
     */
    IGNORE,

    /**
     * Specifies that registration should replace an existing identified
     * artifact with that of which was specified.
     */
    REPLACE,

    /**
     * Specifies that registration should fail (by raising an exception) if
     * the identified artifact is already registered.
     */
    FAIL,

    /**
     * Specifies that registration must always occur. If an identifiable
     * artifact is already registered, a new identity is generated (based
     * on the provided identity) and the specified artifact is registered.
     */
    ALWAYS;
    }
