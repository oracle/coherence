/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.maven.pof;


import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;
import java.util.Objects;


/**
 * A test POF instrumented class.
 *
 * @author jk  2018.03.27
 */
@PortableType(id = 1000)
public class ValueOne
    {
    // ----- constructors ---------------------------------------------------

    public ValueOne(String sFieldOne, String sFieldTwo)
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
        ValueOne valueOne = (ValueOne) o;
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
