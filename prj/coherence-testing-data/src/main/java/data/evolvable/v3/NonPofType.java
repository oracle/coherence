/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v3;

/**
 * @author jk  2017.07.06
 */
public class NonPofType
    {
    private String m_sFieldOne;

    public NonPofType()
        {
        }

    public String getFieldOne()
        {
        return m_sFieldOne;
        }

    public void setFieldOne(String fieldOne)
        {
        this.m_sFieldOne = fieldOne;
        }
    }
