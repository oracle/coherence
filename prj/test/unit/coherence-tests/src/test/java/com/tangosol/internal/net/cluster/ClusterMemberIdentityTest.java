/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.cluster;

import org.junit.Test;

/**
 * Unit tests for DefaultClusterDependencies inner MemberIdentity anonymous class.
 *
 * @author pfm  2011.07.18
 */
public class ClusterMemberIdentityTest
    extends AbstractMemberIdentityTestHelper
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters to make sure the value that was set was returned.
     */
    @Test
    public void testAccess()
        {
        DefaultMemberIdentity identity = DefaultClusterDependenciesTest.createDeps().getMemberIdentity();
        identity.validate();
        runAccessTest(identity);
        }

     /**
     * Test the copy constructor.
     */
    @Test
    public void testClone()
        {
        DefaultClusterDependencies deps = DefaultClusterDependenciesTest.createDeps();
        DefaultMemberIdentity identity = deps.getMemberIdentity();
        loadIdentity(identity);
        identity.validate();

        // The DefaultClusterDependencies.setMemberIdentity will clone the identity
        deps.setMemberIdentity(identity);
        DefaultMemberIdentity identity2 = deps.getMemberIdentity();
        assertCloneEquals(identity, identity2);
        identity2.validate();
        }
    }
