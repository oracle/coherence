/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.generator.data;


import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;
import java.util.Objects;


@PortableType(id = 1001)
public class FinalFieldValue {

    // ----- constructors ---------------------------------------------------

    public FinalFieldValue(String sFinalField)
        {
        m_sFinalField = sFinalField;
        }


    // ----- accessors ------------------------------------------------------

    public String getFinalField()
        {
        return m_sFinalField;
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
        FinalFieldValue value = (FinalFieldValue) o;
        return Objects.equals(m_sFinalField, value.m_sFinalField);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_sFinalField);
        }

    @Override
    public String toString()
        {
        return "FinalFieldValue{finalField='" + m_sFinalField + "'}";
        }

    // ----- data members ---------------------------------------------------

    @Portable
    final String m_sFinalField;
}
