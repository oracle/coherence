/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi.loginmoduleosgitest;

import com.tangosol.security.KeystoreLogin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * LoginModuleOsgiTestBundleActivator is a part of a bundle that has a
 * dependency on the Coherence Login Module and will exercise some basic
 * functionality. The failure of this bundle will result in this bundle
 * being in an inactive state. Subsequently this will result in the failure
 * of the {@link com.tangosol.coherence.osgi.LoginModuleOsgiTest} as it
 * asserts the state of this bundle.
 *
 * @author hr  2012.09.02
 * @since Coherence 12.1.2
 */
public class LoginModuleOsgiTestBundleActivator
        implements BundleActivator
    {

    // ----- BundleActivator methods ----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(BundleContext context) throws Exception
        {
        new KeystoreLogin();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(BundleContext context) throws Exception
        {
        }
    }
