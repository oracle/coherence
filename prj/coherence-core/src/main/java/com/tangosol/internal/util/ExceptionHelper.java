/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;

import java.io.InvalidObjectException;

/**
 * Helper class for {@link InvalidObjectException} creation.
 *
 * @author joe fialli  2023.07.28
 * @since 23.09
 */
public class ExceptionHelper
    {
    /**
     * Create an InvalidObjectException.
     *
     * @param message  message for returned exception
     * @param e        cause for returned exception
     *
     * @return an {@link InvalidObjectException}
     */
    public static InvalidObjectException createInvalidObjectException(String message, Exception e)
        {
        return new InvalidObjectException(message, e);
        }
    }
