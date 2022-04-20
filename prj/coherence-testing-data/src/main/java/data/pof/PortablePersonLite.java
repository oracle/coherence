/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.pof;


import java.io.IOException;

import java.util.Date;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;


public class PortablePersonLite
        extends PersonLite
        implements PortableObject
    {
    public PortablePersonLite()
        {
        }

    public PortablePersonLite(String sName, Date dtDOB)
        {
        super(sName, dtDOB);
        }

    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sName = reader.readString(0);
        m_dtDOB = reader.readDate(2);
        }

    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeString(0, m_sName);
        writer.writeObject(1, null);
        writer.writeDateTime(2, m_dtDOB);
        writer.writeObject(3, null);
        writer.writeObjectArray(4, null, null);
        }
    }