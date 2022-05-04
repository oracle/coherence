/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package memcached;


import java.io.IOException;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;


public class PofUser implements PortableObject
    {

    public PofUser()
        {
        }

    public PofUser(String sName, int nAge)
        {
        m_sName = sName;
        m_nAge = nAge;
        }

    public String getName()
        {
        return m_sName;
        }

    public int getAge()
        {
        return m_nAge;
        }

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_sName = in.readString(0);
        m_nAge = in.readInt(1);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(0, m_sName);
        out.writeInt(1, m_nAge);
        }

    public String toString()
        {
        return new String("Name: " + m_sName + " Age: " + m_nAge);
        }

    protected String m_sName;
    protected int    m_nAge;

    }
