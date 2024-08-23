/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.testdata;

import com.tangosol.io.pof.ConfigurablePofContextTest;
import com.tangosol.io.pof.schema.annotation.PortableType;

public class PortableTypesInnerClass
    {

    public static abstract class PortableTypeTestBase
        {
        private int m_nId;
        private String m_sString;

        public PortableTypeTestBase()
            {
            }

        public PortableTypeTestBase(int nId, String sString)
            {
            m_nId     = nId;
            m_sString = sString;
            }

        public int getId()
            {
            return m_nId;
            }

        public void setId(int nId)
            {
            this.m_nId = nId;
            }

        public String getString()
            {
            return m_sString;
            }

        public void setString(String sString)
            {
            this.m_sString = sString;
            }
       }

    @PortableType(id = 1000)
    public static class PortableTypeTest1
            extends ConfigurablePofContextTest.PortableTypeTestBase
        {

        public PortableTypeTest1()
            {
            super();
            }

        public PortableTypeTest1(int nId, String sString)
            {
            super(nId, sString);
           }
        }

    @PortableType(id = 1)
    public static class PortableTypeTestConflicting
            extends ConfigurablePofContextTest.PortableTypeTestBase
        {
        public PortableTypeTestConflicting()
            {
            super();
            }

        public PortableTypeTestConflicting(int nId, String sString)
            {
            super(nId, sString);
            }
        }

    @PortableType(id = 2000)
    public interface PortableTypeTestInterface
        {
        }

    @PortableType(id = 1234)
    public enum TestEnum
        {
        BRONZE, SILVER, GOLD
        }

    }
