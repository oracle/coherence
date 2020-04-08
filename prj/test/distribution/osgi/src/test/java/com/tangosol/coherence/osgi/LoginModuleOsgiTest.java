/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi;

import com.tangosol.coherence.osgi.loginmoduleosgitest.LoginModuleOsgiTestBundleActivator;

import com.tangosol.security.KeystoreLogin;
import org.junit.Test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * LoginModuleOsgiTest tests the deployment and execution of the
 * <tt>coherence-login.jar</tt> within an OSGi container.
 *
 * @author hr  2012.01.29
 * @since Coherence 12.1.2
 */
public class LoginModuleOsgiTest
        extends AbstractOsgiTest
    {
    @Test
    public void testDeployBundle() throws BundleException
        {
        Container container = m_container;
        String    sPackage  = LoginModuleOsgiTestBundleActivator.class.getPackage().getName().replace('.','/');

        listBundles();

        // deploy dependencies
        deployDependency(KeystoreLogin.class);

        // deploy this test as a bundle
        container.packageAndDeploy("/" + sPackage + "/loginmodule-osgi-test.properties");

        listBundles();

        // ensure all known bundles are deployed and active
        assertThat(container.getBundle("Coherence Login"), hasState(Bundle.ACTIVE));
        assertThat(container.getBundle("LoginModuleOsgiTest"),      hasState(Bundle.ACTIVE));
        }
    }
