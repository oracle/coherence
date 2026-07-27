/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import com.tangosol.net.Service;
import com.tangosol.net.security.IdentityTransformer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.security.auth.Subject;

/**
 * Identity transformer that returns a custom serialized token for negative
 * Extend-boundary testing.
 *
 * @author OpenAI  2026.05.16
 */
public class MaliciousIdentityTransformer
        implements IdentityTransformer
    {
    @Override
    public Object transformIdentity(Subject subject, Service service)
        {
        return new Token();
        }

    /**
     * Token that records materialization if readObject is reached.
     */
    public static class Token
            implements Serializable
        {
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            String sMarker = System.getProperty("test.malicious.identity.marker");
            if (sMarker != null)
                {
                Files.writeString(Path.of(sMarker), "materialized");
                }
            in.defaultReadObject();
            }

        private static final long serialVersionUID = 1L;
        }
    }
