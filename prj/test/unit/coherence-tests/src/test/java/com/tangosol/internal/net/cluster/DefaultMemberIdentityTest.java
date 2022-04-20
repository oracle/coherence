/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.cluster;

import org.junit.Test;

/**
 * Unit tests for DefaultMemberIdentity.
 *
 * @author pfm  2011.07.18
 */
public class DefaultMemberIdentityTest
    extends AbstractMemberIdentityTestHelper
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters to make sure the value that was set was returned.
     */
    @Test
    @Override
    public void testAccess()
        {
        DefaultMemberIdentity identity = new DefaultMemberIdentity();
        identity.validate();
        runAccessTest(identity);
        }

     /**
     * Test the copy constructor.
     */
    @Test
    @Override
    public void testClone()
        {
        DefaultMemberIdentity identity = new DefaultMemberIdentity();
        loadIdentity(identity);
        identity.validate();

        DefaultMemberIdentity identity2 = new DefaultMemberIdentity(identity);
        assertCloneEquals(identity, identity2);
        identity2.validate();
        }
    }
