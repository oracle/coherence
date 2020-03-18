/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

/**
 * PasswordProvider allows Coherence users to plugin their own mechanism to determine the appropriate password.
 *
 * @author spuneet
 * @since 12.2.1.4
 */
public interface PasswordProvider
    {
    /**
     * Returns the password to be used in clear format.
     *
     * The char[] returned from the get() method is not retained by the provider, and it is expected that the
     * consumer zero's out the array once it is done with the password.
     *
     * @return password as char[]
     */
    char[] get();
    }
