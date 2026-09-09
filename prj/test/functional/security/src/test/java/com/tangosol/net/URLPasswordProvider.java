/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

/**
 * Test compatibility adapter for branches that predate the product
 * {@code URLPasswordProvider} implementation.
 */
public class URLPasswordProvider
        extends FileBasedPasswordProvider
    {
    /**
     * Construct a provider for the specified password file.
     *
     * @param sFile  the password file path
     */
    public URLPasswordProvider(String sFile)
        {
        super(sFile);
        }
    }
