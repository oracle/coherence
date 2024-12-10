/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package tcmp;


import com.oracle.coherence.common.base.Timeout;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.CacheFactory;
import com.tangosol.util.Resources;

import com.tangosol.util.WrapperException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;

public class SecuredProductionHostnameVerificationTests
        extends AbstractFunctionalTest
    {
    // ----- FunctionalTest overrides ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public String getProject()
        {
        return System.getProperty("test.project","securedProduction");
        }


    @BeforeClass
    public static void _startup()
        {
        URL url = Resources.findFileOrResource("localhost.jks", getContextClassLoader());

        System.setProperty("coherence.security.keystore",   url.toExternalForm());
        System.setProperty("coherence.security.truststore", url.toExternalForm());
        System.setProperty("coherence.socketprovider",      "ssl");
        System.setProperty("coherence.security.password",   "password");
        System.setProperty("coherence.wka",                 "127.0.0.1");
        System.setProperty("coherence.wka.port",            "8888");
        }

    /**
     * A negative test to make sure that hostname verification rejects
     * when the hostname in the certificate (server) does not match the
     * given host (localhost).
     */
    @Test
    public void testHostNameVerification()
        {
        try (Timeout t = Timeout.after(1, TimeUnit.MINUTES))
            {
            CacheFactory.ensureCluster();
            }
        catch (Exception e)
            {
            if (e instanceof WrapperException)
                {
                if (((WrapperException) e).getOriginalException() instanceof InterruptedException)
                    {
                    fail("Failed to start cluster with exception: " + e);
                    }
                }
            }
        }
    }
