/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.pof;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;

import java.util.Date;


public class SkippingPersonLite
        extends PersonLite
        implements PortableObject
    {
    public SkippingPersonLite()
        {
        }

    public SkippingPersonLite(String sName, Date dtDOB)
        {
        super(sName, dtDOB);
        }

    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sName = reader.readString(0);
        reader.readByteArray(-1);
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