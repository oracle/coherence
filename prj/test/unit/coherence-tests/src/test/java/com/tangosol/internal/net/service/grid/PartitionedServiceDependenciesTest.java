/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.coherence.config.builder.ActionPolicyBuilder;
import com.tangosol.coherence.config.builder.PartitionAssignmentStrategyBuilder;
import com.tangosol.net.partition.DefaultKeyAssociator;
import com.tangosol.net.partition.DefaultKeyPartitioningStrategy;
import com.tangosol.net.partition.KeyAssociator;
import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.partition.PartitionListener;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.util.AssertionException;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Unit tests for PartitionedServiceDependencies.
 *
 * @author pfm  2011.07.18
 */
public class PartitionedServiceDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultPartitionedServiceDependencies deps1 = new DefaultPartitionedServiceDependencies();

        deps1.validate();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultPartitionedServiceDependencies deps2 = new DefaultPartitionedServiceDependencies(deps1);

        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    /**
     * Method description
     */
    @Test
    public void badBackupCount()
        {
        try
            {
            DefaultPartitionedServiceDependencies deps = new DefaultPartitionedServiceDependencies();

            deps.validate();
            populate(deps);
            deps.setPreferredBackupCount(257);
            deps.validate();
            }
        catch (IllegalArgumentException e)
            {
            return;
            }
        catch (AssertionException e)
            {
            return;
            }

        Assert.fail();
        }

    /**
     * Method description
     */
    @Test
    public void badPartitionCount()
        {
        try
            {
            DefaultPartitionedServiceDependencies deps = new DefaultPartitionedServiceDependencies();

            deps.validate();
            populate(deps);
            deps.setPreferredPartitionCount(0);
            deps.validate();
            }
        catch (IllegalArgumentException e)
            {
            return;
            }
        catch (AssertionException e)
            {
            return;
            }

        Assert.fail();
        }

    /**
     * Method description
     */
    @Test
    public void badTransferThreshold()
        {
        try
            {
            DefaultPartitionedServiceDependencies deps = new DefaultPartitionedServiceDependencies();

            deps.validate();
            populate(deps);
            deps.setTransferThreshold(0);
            deps.validate();
            }
        catch (IllegalArgumentException e)
            {
            return;
            }
        catch (AssertionException e)
            {
            return;
            }

        Assert.fail();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two PartitionedServiceDependencies are equal.
     *
     * @param deps1  the first PartitionedServiceDependencies object
     * @param deps2  the second PartitionedServiceDependencies object
     */
    public static void assertCloneEquals(PartitionedServiceDependencies deps1, PartitionedServiceDependencies deps2)
        {
        GridDependenciesTest.assertCloneEquals(deps1, deps2);

        assertEquals(deps1.getActionPolicyBuilder(), deps2.getActionPolicyBuilder());
        assertEquals(deps1.getDistributionAggressiveness(), deps2.getDistributionAggressiveness());
        assertEquals(deps1.isDistributionSynchronized(), deps2.isDistributionSynchronized());
        assertEquals(deps1.getKeyAssociator(), deps2.getKeyAssociator());
        assertEquals(deps1.getKeyPartitioningStrategy(), deps2.getKeyPartitioningStrategy());
        assertEquals(deps1.isOwnershipCapable(), deps2.isOwnershipCapable());
        assertEquals(deps1.getPartitionAssignmentStrategyBuilder(), deps2.getPartitionAssignmentStrategyBuilder());
        assertEquals(deps1.getPartitionListenerBuilders(), deps2.getPartitionListenerBuilders());
        assertEquals(deps1.getPreferredBackupCount(), deps2.getPreferredBackupCount());
        assertEquals(deps1.getPreferredPartitionCount(), deps2.getPreferredPartitionCount());
        assertEquals(deps1.getTransferThreshold(), deps2.getTransferThreshold());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultPartitionedServiceDependencies to populate
     *
     * @return the DefaultPartitionedServiceDependencies that was passed in
     */
    public static DefaultPartitionedServiceDependencies populate(DefaultPartitionedServiceDependencies deps)
        {
        GridDependenciesTest.populate(deps);

        ActionPolicyBuilder builder = new ActionPolicyBuilder.NullImplementationBuilder();

        deps.setActionPolicyBuilder(builder);
        assertEquals(builder, deps.getActionPolicyBuilder());

        Random random = new Random();
        int    n      = random.nextInt(10);

        deps.setDistributionAggressiveness(n);
        assertEquals(n, deps.getDistributionAggressiveness());

        boolean flag = !deps.isDistributionSynchronized();

        deps.setDistributionSynchronized(flag);
        assertEquals(flag, deps.isDistributionSynchronized());

        KeyAssociator associator = new DefaultKeyAssociator();

        deps.setKeyAssociator(associator);
        assertEquals(associator, deps.getKeyAssociator());

        KeyPartitioningStrategy strategy = new DefaultKeyPartitioningStrategy();

        deps.setKeyPartitioningStrategy(strategy);
        assertEquals(strategy, deps.getKeyPartitioningStrategy());

        deps.setOwnershipCapable(flag = !deps.isOwnershipCapable());
        assertEquals(flag, deps.isOwnershipCapable());

        PartitionAssignmentStrategyBuilder bldrPartitionStrategy = new PartitionAssignmentStrategyBuilder("simple", null);

        deps.setPartitionAssignmentStrategyBuilder(bldrPartitionStrategy);
        assertEquals(bldrPartitionStrategy, deps.getPartitionAssignmentStrategyBuilder());

        List<ParameterizedBuilder<PartitionListener>> bldrsListener = new ArrayList<ParameterizedBuilder<PartitionListener>>(1);

        deps.setPartitionListenerBuilders(bldrsListener);
        assertEquals(bldrsListener, deps.getPartitionListenerBuilders());

        deps.setPreferredBackupCount(n = random.nextInt(5));
        assertEquals(n, deps.getPreferredBackupCount());

        deps.setPreferredPartitionCount(n = deps.getPreferredPartitionCount() + 1);
        assertEquals(n, deps.getPreferredPartitionCount());

        // transfer threshold is set as KB but returned as Bytes.
        deps.setTransferThreshold(n = deps.getTransferThreshold() + 1);
        assertEquals(n * 1024, deps.getTransferThreshold());

        return deps;
        }
    }
