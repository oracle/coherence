/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.tangosol.net.security.AccessController;

import javax.security.auth.callback.CallbackHandler;

/**
 * The StandardDependencies interface provides a Standard security object
 * with external dependencies.
 *
 * @author der  2011.12.01
 * @since Coherence 12.1.2
 */
public interface StandardDependencies
        extends SecurityDependencies
    {
    /**
     * Return the AccessController.
     *
     * @return the AccessController
     */
    public AccessController getAccessController();

    /**
     * Return the default CallbackHandler.
     *
     * @return the default CallbackHandler
     */
    public CallbackHandler getCallbackHandler();

    /**
     * Return the login module name.
     *
     * @return login module name
     */
    public String getLoginModuleName();
    }
