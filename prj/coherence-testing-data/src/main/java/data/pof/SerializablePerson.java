/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.pof;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.Date;


public class SerializablePerson
    extends Person
        implements Serializable
    {
    public SerializablePerson()
        {
        }

    public SerializablePerson(String sName, Date dtDOB)
        {
        super(sName, dtDOB);
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