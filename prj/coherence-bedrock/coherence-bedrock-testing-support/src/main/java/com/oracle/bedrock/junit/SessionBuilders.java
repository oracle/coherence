/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.junit;

import com.oracle.bedrock.Option;

public class SessionBuilders
    {
    /**
     * Constructs a {@link SessionBuilder} for a Storage Disabled Member.
     *
     * @return a {@link SessionBuilder}
     */
    public static SessionBuilder storageDisabledMember()
        {
        return new StorageDisabledMember();
        }

    /**
     * Constructs a {@link SessionBuilder} for a Storage Disabled Member.
     *
     * @param options  the additional options to use to create the session
     *
     * @return a {@link SessionBuilder}
     */
    public static SessionBuilder storageDisabledMember(Option... options)
        {
        return new StorageDisabledMember(options);
        }

    /**
     * Constructs a {@link SessionBuilder} for a *Extend Client.
     *
     * @param cacheConfigURI the Cache Configuration URI
     * @return a {@link SessionBuilder}
     */
    public static SessionBuilder extendClient(String cacheConfigURI)
        {
        return new ExtendClient(cacheConfigURI);
        }

    /**
     * Constructs a {@link SessionBuilder} for a *Extend Client.
     *
     * @param cacheConfigURI the Cache Configuration URI
     * @param options        additional options to configure the client
     * @return a {@link SessionBuilder}
     */
    public static SessionBuilder extendClient(String cacheConfigURI, Option... options)
        {
        return new ExtendClient(cacheConfigURI, options);
        }
    }
