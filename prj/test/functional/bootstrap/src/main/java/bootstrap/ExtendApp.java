/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package bootstrap;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

public class ExtendApp
    {
    public ExtendApp(String sTenant, String sHostName, int nPort)
        {
        m_sTenant = sTenant;
        m_sHostName = sHostName;
        m_nPort = nPort;
        }

    public void setValue(String key, String value, String cacheBucket)
        {
        NamedCache<String, String> namedCache = ensureCoherenceSession().getCache(cacheBucket);
        namedCache.put(key, value);
        }

    public String getValue(String key, String cacheBucket)
        {
        NamedCache<String, String> namedCache = ensureCoherenceSession().getCache(cacheBucket);
        return namedCache.get(key);
        }

    private Session ensureCoherenceSession()
        {
        if (m_coherence == null)
            {
            synchronized (this)
                {
                SessionConfiguration sessionConfig = SessionConfiguration.builder()
                        .named(m_sTenant)
                        .withScopeName(m_sTenant)
                        .withParameter("coherence.extend.address", m_sHostName)
                        .withParameter("coherence.extend.port", m_nPort)
                        .build();

                CoherenceConfiguration config = CoherenceConfiguration.builder()
                        .named(m_sTenant)
                        .withSession(sessionConfig)
                        .build();

                m_coherence = Coherence.fixedClient(config)
                        .start().join();
                }
            }

        return m_coherence.getSession(m_sTenant);
        }

    // ----- data members ---------------------------------------------------

    private final String m_sTenant;

    private final String m_sHostName;

    private final int m_nPort;

    private volatile Coherence m_coherence;
    }
