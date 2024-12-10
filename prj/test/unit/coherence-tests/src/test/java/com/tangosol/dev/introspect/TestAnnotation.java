/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * TestAnnotation is used to test annotation scanning.
 *
 * @author hr  2011.10.18
 *
 * @since Coherence 12.1.2
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TestAnnotation
    {
    String  sParam()  default "";
    byte    bParam()  default 0x0;
    boolean fParam()  default false;
    short   shParam() default -1;
    char    chParam() default 'a';
    int     nParam()  default -1;
    long    lParam()  default -1;
    float   flParam() default 1.1f;
    double  dParam()  default 1.1d;

    Class<? extends Number> clzParam() default Integer.class;
    Type enumType() default Type.FOO;

    // arrays
    int[]   anParam() default {};
    Type[]  aEnumType() default {};

    InnerAnnotation   value();

    InnerAnnotation[] aInnerAnno() default {};

    public enum Type
        {
        FOO, BAR
        }
    }
