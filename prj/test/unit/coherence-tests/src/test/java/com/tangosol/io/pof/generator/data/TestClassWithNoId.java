/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.generator.data;

import com.tangosol.io.pof.schema.annotation.PortableType;

import java.util.Objects;

/**
 * A class to test PortableType generation and serialization with no id.
 *
 * @author tam 2021.07.28
 */
@PortableType
public class TestClassWithNoId
    {
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

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        TestClassWithNoId that = (TestClassWithNoId) o;
        return Objects.equals(m_sValue, that.m_sValue);
        }

    public int hashCode()
        {
        return Objects.hash(m_sValue);
        }

    public String toString()
        {
        return "TestClassWithNoId{" +
               "value='" + m_sValue + '\'' +
               "}";
        }

    private String m_sValue;
    }
