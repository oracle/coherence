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


public class EvolvablePortablePerson2
        extends EvolvablePortablePerson
    {
    public EvolvablePortablePerson2()
        {}

    public EvolvablePortablePerson2(String sName, Date dtDOB)
        {
        super(sName, dtDOB);
        }

    public int getImplVersion()
        {
        return 2;
        }

    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sName = reader.readString(0);
        setAddress((Address) reader.readObject(1));
        m_dtDOB = reader.readDate(2);
        setSpouse((Person) reader.readObject(3));
        setChildren((Person[]) reader.readObjectArray(4, new EvolvablePortablePerson2[0]));
        m_sNationality = reader.readString(5);
        m_addrPOB = (Address) reader.readObject(6);
        }

    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeString(0, m_sName);
        writer.writeObject(1, getAddress());
        writer.writeDateTime(2, m_dtDOB);
        writer.writeObject(3, getSpouse());
        writer.writeObjectArray(4, getChildren(), EvolvablePortablePerson2.class);
        writer.writeString(5, m_sNationality);
        writer.writeObject(6, m_addrPOB);
        }

    public String  m_sNationality;
    public Address m_addrPOB;
    }