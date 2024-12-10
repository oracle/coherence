/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.pof;


import java.io.IOException;

import java.util.Date;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.Binary;


public class EvolvablePortablePerson
        extends PortablePerson
        implements EvolvablePortableObject
    {
    public EvolvablePortablePerson()
        {
        }

    public EvolvablePortablePerson(String sName, Date dtDOB)
        {
        super(sName, dtDOB);
        }

    public int getImplVersion()
        {
        return 1;
        }

    public int getDataVersion()
        {
        return m_nVersion;
        }

    public void setDataVersion(int nVersion)
        {
        m_nVersion = nVersion;
        }

    public Binary getFutureData()
        {
        return m_binFuture;
        }

    public void setFutureData(Binary binFuture)
        {
        m_binFuture = binFuture;
        }

    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sName = reader.readString(0);
        setAddress((Address) reader.readObject(1));
        m_dtDOB = reader.readDate(2);
        setSpouse((Person) reader.readObject(3));
        setChildren((Person[]) reader.readObjectArray(4, new EvolvablePortablePerson[0]));
        }

    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeString(0, m_sName);
        writer.writeObject(1, getAddress());
        writer.writeDateTime(2, m_dtDOB);
        writer.writeObject(3, getSpouse());
        writer.writeObjectArray(4, getChildren(),
                EvolvablePortablePerson.class);
        }

    public Binary m_binFuture;
    public int    m_nVersion;
    }