/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

import com.tangosol.dev.introspect.TestAnnotation.Type;

/**
 * TestPopulation is a class with an annotation and all permitted value types
 * within the annotation.
 *
 * @author hr  2012.07.30
 *
 * @since Coherence 12.1.2
 */
@TestAnnotation(
    sParam = "abc", bParam = 0x4, fParam = true, shParam = 8,
    chParam = 'c', nParam = 16, lParam = 32L, flParam = 64.2f,
    dParam = 128.2, clzParam = Long.class, enumType = Type.FOO,
    anParam = {8,4,2,1}, aEnumType = {Type.FOO, Type.BAR},
    value = @InnerAnnotation("cba"), aInnerAnno = @InnerAnnotation("abc"))
public class TestPopulation
    {
    public String someMethod(String sParam, int nParam2, boolean fParam3, byte bParam4)
        {
        return "";
        }
    }
