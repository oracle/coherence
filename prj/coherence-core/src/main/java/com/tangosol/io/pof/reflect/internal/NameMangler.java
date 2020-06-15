/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

/**
 * NameMangler implementations provide the ability to transform a
 * string to the string convention employed by the mangler implementation.
 * An example of this would be to convert a non-camel case string to a camel
 * case string.
 * <p>
 *
 * @author hr
 *
 * @since 3.7.1
 */
public interface NameMangler
    {
    /**
     * Convert the given string to a new string using a convention determined
     * by the implementer.
     * 
     * @param sName  original string
     *
     * @return mangled string
     */
    public String mangle(String sName);
    }
