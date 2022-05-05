/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

import java.io.Serializable;

/**
 * @author jk 2014.09.18
 */
public class DenyClusterAccess
        implements RemoteRunnable, Serializable
    {
    @Override
    public void run()
        {
        System.setProperty(SysPropAuthorizedHostFilter.DENY_ACCESS_PROPERTY, "true");
        }
    }
