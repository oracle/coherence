/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests.search;

import com.oracle.coherence.ai.Vector;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A simple test holder for a vector and a text value.
 */
public class ValueWithVector
        implements ExternalizableLite, PortableObject
    {
    public ValueWithVector()
        {
        }

    public ValueWithVector(Vector<float[]> vector, String text)
        {
        this.vector = vector;
        this.text   = text;
        }

    public Vector<float[]> getVector()
        {
        return vector;
        }

    public String getText()
        {
        return text;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        vector = in.readObject(0);
        text   = in.readString(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, vector);
        out.writeString(1, text);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        vector = ExternalizableHelper.readObject(in);
        text   = ExternalizableHelper.readSafeUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, vector);
        ExternalizableHelper.writeSafeUTF(out, text);
        }

    // ----- data members ---------------------------------------------------

    private Vector<float[]> vector;

    private String text;
    }
