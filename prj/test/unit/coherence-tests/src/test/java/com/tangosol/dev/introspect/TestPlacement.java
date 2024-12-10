/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

/**
 * TestPlacement is a class with annotations present in all permitted areas
 * by the Java class file format.
 *
 * @author hr  2012.07.30
 *
 * @since Coherence 12.1.2
 */
@TestAnnotation(anParam = {1,2,4,8}, sParam = "cba", value = @InnerAnnotation("bca"))
public class TestPlacement
    {
    @TestAnnotation(@InnerAnnotation(""))
    public String someMethod(@TestAnnotation(@InnerAnnotation("")) String[] asParam, int nParam2, boolean fParam3, byte bParam4)
        {
        int i = 10;
        return "abx";
        }

    // ----- data members ---------------------------------------------------

    @TestAnnotation(@InnerAnnotation(""))
    private String m_sParam;
    }