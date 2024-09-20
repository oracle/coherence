/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.maven.pof;


import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;

import java.util.Objects;


/**
 * A test POF instrumented class.
 *
 * @author Gunnar Hillert  2024.05.02
 */
@PortableType(id = 2000)
public class ValueTwo
    {
    // ----- constructors ---------------------------------------------------

    public ValueTwo(String sFieldOne, String sFieldTwo)
        {
        m_sFieldOne = sFieldOne;
        m_sFieldTwo = sFieldTwo;
        }


    // ----- accessors ------------------------------------------------------

    public String getFieldOne()
        {
        return m_sFieldOne;
        }

    public String getFieldTwo()
        {
        return m_sFieldTwo;
        }

    // ----- object methods -------------------------------------------------

    @Override
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
        ValueTwo valueOne = (ValueTwo) o;
        return Objects.equals(m_sFieldOne, valueOne.m_sFieldOne) &&
                Objects.equals(m_sFieldTwo, valueOne.m_sFieldTwo);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_sFieldOne, m_sFieldTwo);
        }

    @Override
    public String toString()
        {
        return "ValueOne{" +
                "fieldOne='" + m_sFieldOne + '\'' +
                ", fieldTwo='" + m_sFieldTwo + '\'' +
                '}';
        }

    // ----- data members ---------------------------------------------------

    @Portable
    private String m_sFieldOne;

    @Portable
    private String m_sFieldTwo;
    }
