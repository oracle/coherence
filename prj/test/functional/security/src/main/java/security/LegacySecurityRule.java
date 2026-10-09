/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import com.oracle.coherence.testing.SystemPropertyResource;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.net.CacheFactory;

import org.junit.rules.ExternalResource;

/**
 * Scope for tests of the legacy Subject identity and SignedObject permission
 * exchange. These fixtures deliberately retain that compatibility contract;
 * hardened identity and peer-proof tests use their own configuration.
 *
 * @author phf  2026.09.30
 * @since 26.10
 */
public class LegacySecurityRule
        extends ExternalResource
    {
    @Override
    protected void before()
        {
        m_scope = CoherenceModeHelper.securityCompatibility();
        m_cluster = new SystemPropertyResource("coherence.cluster", "LegacySecurity-" + System.nanoTime());
        m_invocation = new SystemPropertyResource("coherence.invocation.enabled", "true");
        }

    @Override
    protected void after()
        {
        try
            {
            CacheFactory.shutdown();
            }
        finally
            {
            m_invocation.close();
            m_cluster.close();
            m_scope.close();
            }
        }

    private CoherenceModeHelper.ModeScope m_scope;
    private SystemPropertyResource m_cluster;
    private SystemPropertyResource m_invocation;
    }
