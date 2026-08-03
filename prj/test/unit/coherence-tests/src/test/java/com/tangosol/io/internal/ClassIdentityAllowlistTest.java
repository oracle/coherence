/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import org.junit.After;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ClassIdentityAllowlist}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class ClassIdentityAllowlistTest
    {
    @After
    public void cleanup()
        {
        restoreProperty("coherence.mode", m_sModeOld);
        ClassIdentityAllowlist.reset();
        }

    @Test
    public void testDevModeAcceptsBaselinePackages()
        {
        withMode("dev");

        assertTrue(ClassIdentityAllowlist.isAllowed("java/lang", "String"));
        assertTrue(ClassIdentityAllowlist.isAllowed("com/tangosol/util", "Base"));
        assertFalse(ClassIdentityAllowlist.isAllowed("com/evil/attack", "Payload"));
        }

    @Test
    public void testProdModeAcceptsBaselineAndRejectsUnregisteredCustomerPackage()
        {
        withMode("prod");

        assertTrue(ClassIdentityAllowlist.isAllowed("java/lang", "String"));
        assertTrue(ClassIdentityAllowlist.isAllowed("com/tangosol/util", "Base"));
        assertFalse(ClassIdentityAllowlist.isAllowed("com/customer/app", "Task"));
        }

    @Test
    public void testProdModeAcceptsRegisteredPackage()
        {
        withMode("prod");
        ClassIdentityAllowlist.setRegisteredPackageProvider(() -> Set.of("com.customer.app"));

        assertTrue(ClassIdentityAllowlist.isAllowed("com/customer/app", "Task"));
        }

    private void withMode(String sMode)
        {
        restoreProperty("coherence.mode", sMode);
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    private final String m_sModeOld = System.getProperty("coherence.mode");
    }
