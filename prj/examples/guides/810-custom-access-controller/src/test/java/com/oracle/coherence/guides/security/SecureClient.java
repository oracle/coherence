/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.partition.SimplePartitionKey;
import com.tangosol.net.security.Security;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;

/**
 * A simple class that runs {@link Coherence} as an Extend client
 * with a {@link Subject} created from a JAAS login.
 *
 * @author Jonathan Knight 2025.04.11
 */
@SuppressWarnings("resource")
public class SecureClient
    {
    /**
     * Perform a JAAS login and run Coherence within the context
     * of the logged in {@link Subject}.
     *
     * @param args  the program arguments
     */
    public static void main(String[] args)
        {
        Subject subject = Security.login(new TestCallBackHandler());
        Subject.doAs(subject, (PrivilegedAction<Void>) () ->
            {
            try (Coherence coherence = Coherence.client().start().join();
                 Session   session   = coherence.getSession())
                {
                NamedMap<String, String> map = session.getMap("test");

                map.put("key-1", "value-1");
                }
            catch (Throwable t)
                {
                Logger.err("Caught exception in SecureClient", t);
                }
            return null;
            });
        }
    }
