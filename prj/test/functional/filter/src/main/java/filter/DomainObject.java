/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package filter;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;

/**
 * @author jk 2015.08.18
 */
public class DomainObject
        implements PortableObject
    {
    public DomainObject()
        {
        }

    public DomainObject(boolean fOne, boolean fTwo, boolean fThree)
        {
        m_fOne   = fOne;
        m_fTwo   = fTwo;
        m_fThree = fThree;
        }

    public boolean isOne()
        {
        return m_fOne;
        }

    public boolean isTwo()
        {
        return m_fTwo;
        }

    public boolean isThree()
        {
        return m_fThree;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_fOne   = in.readBoolean(0);
        m_fTwo   = in.readBoolean(1);
        m_fThree = in.readBoolean(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeBoolean(0, m_fOne);
        out.writeBoolean(1, m_fTwo);
        out.writeBoolean(2, m_fThree);
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

        DomainObject that = (DomainObject) o;

        return m_fOne == that.m_fOne && m_fTwo == that.m_fTwo && m_fThree == that.m_fThree;
        }

    @Override
    public int hashCode()
        {
        int result = (m_fOne ? 1 : 0);
        result = 31 * result + (m_fTwo ? 1 : 0);
        result = 31 * result + (m_fThree ? 1 : 0);
        return result;
        }

    @Override
    public String toString()
        {
        return "DomainObject(" +
                "one=" + m_fOne +
                ", two=" + m_fTwo +
                ", three=" + m_fThree +
                ')';
        }

    private boolean m_fOne;

    private boolean m_fTwo;

    private boolean m_fThree;
    }
