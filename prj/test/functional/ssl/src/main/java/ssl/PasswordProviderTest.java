/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package ssl;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.NamedCache;
import com.tangosol.util.WrapperException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Functional tests for PasswordProvider.
 *
 * @author spuneet
 */
public class PasswordProviderTest
    {

    @Test
    public void testSSLWithPasswordProvider()
        {
        thrown.expect(WrapperException.class); // Throws : ConfigurationException
        thrown.expectMessage("There are no local participants configured");

        System.setProperty("coherence.cacheconfig", "tangosol-coherence-cache-config-password-providers-with-ssl.xml");
        System.setProperty("coherence.override", "tangosol-coherence-password-providers-with-ssl.xml");
        CacheFactory.shutdown();
        CacheFactory.ensureCluster();

        // Throws exception at this point, for above given reason. Using federation with SSL,
        // to verify that the SSL password-provider(s) are loaded correctly.
        // The below error is after the SSL config is loaded, so can be safely ignored.
        // Requires: Configure a local participant by setting the participant name to match the cluster name
        NamedCache cache = CacheFactory.getCache("test",
                new ScopedLoader("Scope_0", CacheFactoryBuilder.class.getClassLoader()));

        CacheFactory.shutdown();
        }

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    }

class ScopedLoader
        extends ClassLoader
    {
    /**
     * Create a new class loader with the specified scope.
     *
     * @param sScope  the name of the scope to create a class loader for
     * @param parent  the parent class loader
     */
    ScopedLoader(String sScope, ClassLoader parent)
    {
        super(parent);
        m_sScope = sScope;
    }

    /**
     * Return the scope name of this loader.
     *
     * @return the scope name of this loader
     */
    protected String getScopeName()
    {
        return m_sScope;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return super.toString() + "(Scope: " + m_sScope + ")";
    }

    /**
     * The name of the logical scope that this class-loader defines.
     */
    protected String m_sScope;
    }