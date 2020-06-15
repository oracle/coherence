/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package common.data;

import javax.json.bind.annotation.JsonbProperty;

public class JavaNumberType
    {
    // ----- constructors ---------------------------------------------------

    @SuppressWarnings("unused")
    public JavaNumberType()
        {
        }

    public JavaNumberType(Number number)
        {
        m_nNumber   = number;
        m_oProperty = number;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (o instanceof JavaNumberType)
            {
            JavaNumberType that = (JavaNumberType) o;

            return m_nNumber.longValue() == that.m_nNumber.longValue();
            }
        return false;
        }

    // ----- data members ---------------------------------------------------

    @JsonbProperty("numberProperty")
    protected Number m_nNumber;

    @JsonbProperty("objectProperty")
    protected Object m_oProperty;
    }
