/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

/**
 * InnerAnnotation used to test annotation scanning.
 *
 * @author hr  2011.10.20
 *
 * @since Coherence 12.1.2
 */
public @interface InnerAnnotation
    {
    String value() default "";
    }
