/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import java.util.Arrays;

/**
 * PasswordProvider allows Coherence users to plug in their own mechanism to determine the appropriate password.
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

    /**
     * Resets the contents of a password array
     *
     * @param arrays  an array of character arrays to reset
     */
    static void reset(char[]... arrays)
        {
        for (char[] ac : arrays)
            {
            if (ac != null)
                {
                Arrays.fill(ac, '0');
                }
            }
        }

    /**
     * A singleton implementation of a {@link PasswordProvider} that always returns an empty array.
     */
    PasswordProvider NullImplementation = () -> new char[0];
    }
