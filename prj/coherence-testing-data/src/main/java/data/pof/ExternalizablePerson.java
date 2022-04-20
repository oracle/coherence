/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.pof;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Date;


public class ExternalizablePerson
        extends Person
        implements ExternalizableLite
    {
    public ExternalizablePerson()
        {
        }

    public ExternalizablePerson(String sName, Date dtDOB)
        {
        super(sName, dtDOB);
        }

    public void readExternal(DataInput in)
            throws IOException
        {
        m_sName = ExternalizableHelper.readUTF(in);
        setAddress((Address) ExternalizableHelper.readObject(in));
        m_dtDOB = new Date(ExternalizableHelper.readLong(in));
        setSpouse((Person) ExternalizableHelper.readObject(in));
        setChildren((Person[]) ExternalizableHelper.readObject(in));
        }

    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeUTF(out, m_sName);
        ExternalizableHelper.writeObject(out, getAddress());
        ExternalizableHelper.writeLong(out, m_dtDOB.getTime());
        ExternalizableHelper.writeObject(out, getSpouse());
        ExternalizableHelper.writeObject(out, getChildren());
        }

    private synchronized void writeObject(ObjectOutputStream out)
            throws IOException
        {
        super.serializePerson(out);
        }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
        super.deserializePerson(in);
        }
    }