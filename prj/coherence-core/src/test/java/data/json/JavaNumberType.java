/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.json;

public class JavaNumberType
    {
    @SuppressWarnings("unused")
    public JavaNumberType()
        {
        }

    public JavaNumberType(Number number)
        {
        numberProperty = number;
        objectProperty = number;
        }

    public Number numberProperty;
    public Object objectProperty;

    @Override
    public boolean equals(Object o)
        {
        if (o instanceof JavaNumberType)
            {
            JavaNumberType that = (JavaNumberType) o;

            return numberProperty.longValue() == that.numberProperty.longValue();
            }
        return false;
        }
    }
