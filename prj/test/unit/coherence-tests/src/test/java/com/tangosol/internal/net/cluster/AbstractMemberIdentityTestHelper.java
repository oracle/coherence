/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.cluster;

import com.tangosol.net.MemberIdentity;

import static org.junit.Assert.assertEquals;

/**
 * Helper class to run MemberIdentity tests.
 *
 * @author pfm  2011.07.18
 */
public abstract class AbstractMemberIdentityTestHelper
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters to make sure the value that was set was returned.
     */
    public abstract void testAccess();

     /**
     * Test the copy constructor.
     */
    public abstract void testClone();

    // ----- helpers --------------------------------------------------------

    /**
     * Load the DefaultMemberIdentity with sample data.
     *
     * @param identity  the member identity
     */
    protected void loadIdentity(DefaultMemberIdentity identity)
        {
        identity.setClusterName("myCluster");
        identity.setMachineId(100);
        identity.setMachineName("myMachine");
        identity.setMemberName("myMember");
        identity.setPriority(Thread.NORM_PRIORITY);
        identity.setProcessName("myProcess");
        identity.setRackName("myRack");
        identity.setRoleName("myRole");
        identity.setSiteName("mySite");
        }

    /**
     * Test the setters to make sure the value that was set was returned.  This will also
     * be called by DefaultClusterDependencies to test its inner class MemberIdentity
     *
     * @param identity  the member identity
     */
    protected void runAccessTest(DefaultMemberIdentity identity)
        {
        identity.validate();
        System.out.println(identity.toString());

        identity.setClusterName("myCluster");
        assertEquals(identity.getClusterName(),"myCluster");

        identity.setMachineId(100);
        assertEquals(identity.getMachineId(), 100);

        identity.setMachineName("myMachine");
        assertEquals(identity.getMachineName(), "myMachine");

        identity.setMemberName("myMember");
        assertEquals(identity.getMemberName(), "myMember");

        identity.setPriority(Thread.NORM_PRIORITY);
        assertEquals(identity.getPriority(), Thread.NORM_PRIORITY);

        identity.setProcessName("myProcess");
        assertEquals(identity.getProcessName(), "myProcess");

        identity.setRackName("myRack");
        assertEquals(identity.getRackName(),"myRack");

        identity.setRoleName("myRole");
        assertEquals(identity.getRoleName(),"myRole");

        identity.setSiteName("mySite");
        assertEquals(identity.getSiteName(), "mySite");

        identity.validate();
        System.out.println(identity.toString());
        }

    /**
     * Assert that the two MemberIdentity are equal.
     *
     * @param identity1  the first MemberIdentity object
     * @param identity2  the second MemberIdentity object
     */
    protected void assertCloneEquals(MemberIdentity identity1, MemberIdentity identity2)
        {
        assertEquals(identity1.getClusterName(), identity2.getClusterName());
        assertEquals(identity1.getMachineId(),   identity2.getMachineId());
        assertEquals(identity1.getMachineName(), identity2.getMachineName());
        assertEquals(identity1.getMemberName(),  identity2.getMemberName());
        assertEquals(identity1.getPriority(),    identity2.getPriority());
        assertEquals(identity1.getProcessName(), identity2.getProcessName());
        assertEquals(identity1.getRackName(),    identity2.getRackName());
        assertEquals(identity1.getRoleName(),    identity2.getRoleName());
        assertEquals(identity1.getSiteName(),    identity2.getSiteName());
        }
    }
