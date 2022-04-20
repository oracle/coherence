/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.generator.data;

import com.tangosol.io.pof.schema.annotation.PortableType;

/**
 * A class to test PortableType generation and serialization with no id.
 *
 * @author tam 2021.07.28
 */
@PortableType
public class TestClassWithNoId {

    public TestClassWithNoId()
        {
        }

    public TestClassWithNoId(String sValue)
        {
        m_sValue = sValue;
        }

    public String getValue()
        {
        return m_sValue;
        }

    public void setValue(String m_sValue)
        {
        this.m_sValue = m_sValue;
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        TestClassWithNoId that = (TestClassWithNoId) o;

        return m_sValue != null ? m_sValue.equals(that.m_sValue) : that.m_sValue == null;
        }

    @Override
    public int hashCode()
        {
        return m_sValue != null ? m_sValue.hashCode() : 0;
        }

    private String m_sValue;
    }
