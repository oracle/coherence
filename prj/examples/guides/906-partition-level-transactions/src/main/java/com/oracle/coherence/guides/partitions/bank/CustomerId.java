/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.partitions.bank;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/**
 * A customer identifier used as a key for Coherence caches.
 *
 * @author Jonathan Knight  2023.01.14
 * @since 22.06.4
 */
public class CustomerId
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * A default no-args constructor required for serialization.
     */
    public CustomerId()
        {
        }

    /**
     * Create a {@link CustomerId}.
     *
     * @param id  the id of the customer
     */
    public CustomerId(String id)
        {
        this.id = id;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the id.
     *
     * @return  the customer id
     */
    public String getId()
        {
        return id;
        }

    // ----- Object methods -------------------------------------------------

    // Coherence key classes must properly implement equals() using
    // all the fields in the class
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
        CustomerId that = (CustomerId) o;
        return Objects.equals(id, that.id);
        }

    // Coherence key classes must properly implement hashCode() using
    // all the fields in the class
    @Override
    public int hashCode()
        {
        return Objects.hash(id);
        }

    @Override
    public String toString()
        {
        return "CustomerId{" +
                "id='" + id + '\'' +
                '}';
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        id = ExternalizableHelper.readSafeUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, id);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        id = in.readString(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, id);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The id of the customer.
     */
    private String id;
    }
