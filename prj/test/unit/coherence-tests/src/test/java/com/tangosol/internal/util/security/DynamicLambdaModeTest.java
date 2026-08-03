/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.invoke.AbstractRemotable;
import com.tangosol.internal.util.invoke.RemotableSupport;

import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the DYNAMIC lambda mode gate.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public class DynamicLambdaModeTest
    {
    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        resetMode();
        }

    @Test
    public void shouldRejectDynamicLambdaInProdModeByDefault()
        {
        setMode("prod", null);

        SecurityException e = assertThrows(SecurityException.class, () -> realizeDynamicLambda("Aleks"));

        assertTrue(e.getMessage().contains(LambdaBytecodeGate.REASON_DYNAMIC_REMOTE_DENIED_BY_MODE));
        }

    @Test
    public void shouldAllowDynamicLambdaInProdModeWhenPropertyAllows()
        {
        setMode("prod", "allow");

        long cBefore = LambdaBytecodeGate.counter("allowed", "none",
                LambdaBytecodeGate.Site.CLASS_DEFINITION);
        Remote.Predicate<String> predicate = realizeDynamicLambda("Aleks");
        long cAfter = LambdaBytecodeGate.counter("allowed", "none",
                LambdaBytecodeGate.Site.CLASS_DEFINITION);

        assertTrue(predicate.test("Aleks"));
        assertTrue(cAfter > cBefore);
        }

    @Test
    public void shouldAllowDynamicLambdaInDevModeByDefault()
        {
        setMode("dev", null);

        Remote.Predicate<String> predicate = realizeDynamicLambda("Aleks");

        assertTrue(predicate.test("Aleks"));
        }

    @Test
    public void shouldAllowNonLambdaClassDefinitionInProdMode()
        {
        setMode("prod", null);

        RemotableSupport support = new RemotableSupport(DynamicLambdaModeTest.class.getClassLoader());
        Remote.Predicate<String> predicate = support.realize(
                support.<Remote.Predicate<String>>createRemoteConstructor(AlwaysTruePredicate.class, new Object[0]));

        assertTrue(predicate.test("anything"));
        }

    private static Remote.Predicate<String> realizeDynamicLambda(String sPrefix)
        {
        RemotableSupport support = new RemotableSupport(DynamicLambdaModeTest.class.getClassLoader());
        StringPredicate predicate = s -> s.startsWith(sPrefix);
        return support.realize(support.createRemoteConstructor(predicate));
        }

    private static void setMode(String sMode, String sDynamicLambda)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicLambda);
        resetMode();
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

    private static void resetMode()
        {
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    public static class AlwaysTruePredicate
            extends AbstractRemotable<AlwaysTruePredicate>
            implements Remote.Predicate<String>
        {
        public AlwaysTruePredicate(Object... aoArgs)
            {
            super(aoArgs);
            }

        @Override
        public boolean test(String s)
            {
            return true;
            }
        }

    public interface StringPredicate
            extends Remote.Predicate<String>
        {
        }

    private final String m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
    private final String m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
    }
