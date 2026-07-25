/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import data.pof.PortablePerson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Regression tests for the default POF context selection.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class DefaultPofContextStrictnessTest
    {
    @Test
    public void testDefaultPofContextIsStrictConfigurablePofContext()
        {
        PofContext context = createDefaultPofContext();

        assertEquals(ConfigurablePofContext.class, context.getClass());
        }

    @Test
    public void testStrictDefaultRejectsSafePofPortablePayloadBeforeLoadingWireClass()
        {
        SafeConfigurablePofContext contextSafe = new SafeConfigurablePofContext();
        Binary bin = ExternalizableHelper.toBinary(PortablePerson.createNoChildren(), contextSafe);

        TrackingConfigurablePofContext contextStrict = new TrackingConfigurablePofContext();
        try
            {
            ExternalizableHelper.fromBinary(bin, contextStrict);
            fail("expected unregistered safe-pof portable payload to be rejected");
            }
        catch (RuntimeException e)
            {
            assertUnknownUserType(e);
            assertFalse(contextStrict.wasLoadClassCalled(PortablePerson.class.getName()));
            }
        }

    private static PofContext createDefaultPofContext()
        {
        return new ConfigurablePofContext();
        }

    private static void assertUnknownUserType(Throwable t)
        {
        while (t != null)
            {
            String sMessage = t.getMessage();
            if (sMessage != null && sMessage.contains("unknown user type"))
                {
                return;
                }
            t = t.getCause();
            }

        fail("expected unknown user type rejection");
        }

    // ----- inner class: TrackingConfigurablePofContext --------------------

    /**
     * ConfigurablePofContext that records attempted wire-name class loading.
     */
    public static class TrackingConfigurablePofContext
            extends ConfigurablePofContext
        {
        @Override
        protected Class loadClass(String sClass)
            {
            m_sClass = sClass;
            return super.loadClass(sClass);
            }

        public boolean wasLoadClassCalled(String sClass)
            {
            return sClass.equals(m_sClass);
            }

        private String m_sClass;
        }
    }
