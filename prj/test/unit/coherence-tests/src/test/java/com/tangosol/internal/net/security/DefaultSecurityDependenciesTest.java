/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for DefaultSecurityDependenciesTest (security-config element).
 *
 * @author der  2012.1.3
 * @since Coherence 12.1.2
 */
public class DefaultSecurityDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default values.
     */
    @Test
    public void testDefaultNoConfigVal()
        {
        DefaultSecurityDependencies deps = new DefaultSecurityDependencies();

        deps.validate();
        System.out.println("DefaultSecurityDependenciesTest.testDefaultNoConfigValidate:");
        System.out.println(deps.toString());

        // test the default values
        assertDefault(deps);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the default SecurityDependencies is set correctly.
     *
     * @param deps  the SecurityDependencies object
     */
    public static void assertDefault(SecurityDependencies deps)
        {
        assertEquals(deps.isEnabled(), DefaultSecurityDependencies.DEFAULT_ENABLED);
        assertEquals(deps.isSubjectScoped(), DefaultSecurityDependencies.DEFAULT_SUBJECT_SCOPED);
        assertNull(deps.getIdentityAsserter());
        assertNull(deps.getIdentityTransformer());
        assertEquals(deps.getModel(), DefaultSecurityDependencies.DEFAULT_MODEL);
        }

    /**
     * Assert that the two SecurityDependencies are equal.
     *
     * @param deps1  the first SecurityDependencies object
     * @param deps2  the second SecurityDependencies object
     */
    public static void assertCloneEquals(SecurityDependencies deps1, SecurityDependencies deps2)
        {
        assertEquals(deps1.isEnabled(),              deps2.isEnabled());
        assertEquals(deps1.isSubjectScoped(),        deps2.isSubjectScoped());
        assertEquals(deps1.getIdentityAsserter(),    deps2.getIdentityAsserter());
        assertEquals(deps1.getIdentityTransformer(), deps2.getIdentityTransformer());
        assertEquals(deps1.getModel(),               deps2.getModel());
        }
    }
