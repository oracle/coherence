/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package rest;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.net.NamedCache;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

/**
 * A collection of functional tests for Coherence*Extend-REST that use the
 * default embedded HttpServer with Basic HTTP authentication enabled.
 *
 * @author lh 2013.05.28
 */
public class BasicAuthRestTests
        extends AbstractRestTests
    {

    // ----- constructors ---------------------------------------------------

    public BasicAuthRestTests()
        {
        super(FILE_SERVER_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
            throws IOException
        {
        // generate a temporary login.config to be used by the HTTP server for the tests
        ClassLoader loader   = BasicAuthRestTests.class.getClassLoader();
        String      fileName = loader.getResource("login.config").getFile();
        String      resDir   = fileName.substring(0, fileName.lastIndexOf("/") + 1);
        fileName = fileName + "BA";

        loginConfig = new File(fileName);
        String config = "Coherence { com.tangosol.security.KeystoreLogin required"
            + " keyStorePath=\"" + resDir + "client.jks\"; };"
            + " CoherenceREST { com.tangosol.security.KeystoreLogin required "
            + " keyStorePath=\"" + resDir + "client.jks\"; };\n";
        BufferedWriter output = new BufferedWriter(new FileWriter(loginConfig));
        output.write(config);
        output.close();

        System.setProperty("java.security.auth.login.config", fileName);

        CoherenceClusterMember clusterMember = startCacheServer("BasicAuthRestTests", "rest", FILE_SERVER_CFG_CACHE);
        Eventually.assertDeferred(() -> clusterMember.isServiceRunning("ExtendHttpProxyService"), is(true));
        }

    @Before
    public void setupTest()
        {
        NamedCache cache = getNamedCache("dist-test");
        cache.put("test", "secret");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("BasicAuthRestTests");

        // delete the temporary login.config
        loginConfig.delete();
        }

    @Override
    protected ClientBuilder createClient()
        {
        return super.createClient()
                .register(HttpAuthenticationFeature.basicBuilder()
                                  .credentials("client", "private").build());
        }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public  static  String FILE_SERVER_CFG_CACHE = "server-cache-config-basic-auth.xml";
    private static  File   loginConfig;    // temporary login.config file
    }
