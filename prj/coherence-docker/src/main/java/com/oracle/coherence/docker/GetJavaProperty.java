/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.docker;

/**
 * A simple utility that will display the value for a give Java
 * System property.
 */
public class GetJavaProperty
    {
    public static void main(String[] args)
        {
        System.out.print(System.getProperty(args[0]));
        }
    }
