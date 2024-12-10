/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

/**
 * @author jk  2013.12.19
 */
public class DummyValue
    {
    public DummyValue(String id, String fieldOne)
        {
        this.id       = id;
        this.fieldOne = fieldOne;
        }

    public String getId()
        {
        return id;
        }

    public String getFieldOne()
        {
        return fieldOne;
        }

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

        DummyValue that = (DummyValue) o;

        if (fieldOne != null
            ? !fieldOne.equals(that.fieldOne)
            : that.fieldOne != null)
            {
            return false;
            }

        if (id != null
            ? !id.equals(that.id)
            : that.id != null)
            {
            return false;
            }

        return true;
        }

    @Override
    public int hashCode()
        {
        int result = id != null
                     ? id.hashCode()
                     : 0;

        result = 31 * result + (fieldOne != null
                                ? fieldOne.hashCode()
                                : 0);

        return result;
        }

    private String id;
    private String fieldOne;
    }
