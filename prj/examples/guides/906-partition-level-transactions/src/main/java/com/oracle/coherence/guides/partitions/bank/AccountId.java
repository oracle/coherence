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
import com.tangosol.net.cache.KeyAssociation;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/**
 * A account identifier used as a key for Coherence caches.
 *
 * @author Jonathan Knight  2023.01.14
 * @since 22.06.4
 */
public class AccountId
        implements ExternalizableLite, PortableObject, KeyAssociation<CustomerId>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * A default no-args constructor required for serialization.
     */
    public AccountId()
        {
        }

    /**
     * Create a {@link AccountId}.
     *
     * @param id  the id of the customer
     */
    public AccountId(CustomerId customerId, String id)
        {
        this.customerId = customerId;
        this.id = id;
        }

    // ----- KeyAssociation methods -----------------------------------------

    @Override
    public CustomerId getAssociatedKey()
        {
        return customerId;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the id.
     *
     * @return  the customer id
     */
    public CustomerId getCustomerId()
        {
        return customerId;
        }

    /**
     * Return the account id.
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
        AccountId accountId = (AccountId) o;
        return Objects.equals(customerId, accountId.customerId) && Objects.equals(id, accountId.id);
        }

    // Coherence key classes must properly implement hashCode() using
    // all the fields in the class
    @Override
    public int hashCode()
        {
        return Objects.hash(customerId, id);
        }

    @Override
    public String toString()
        {
        return "AccountId{" +
                "customerId=" + customerId +
                ", id='" + id + '\'' +
                '}';
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        customerId = ExternalizableHelper.readObject(in);
        id = ExternalizableHelper.readSafeUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, customerId);
        ExternalizableHelper.writeSafeUTF(out, id);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        customerId = in.readObject(0);
        id = in.readString(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, customerId);
        out.writeString(1, id);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The id of the customer.
     */
    private CustomerId customerId;

    /**
     * The id of the account.
     */
    private String id;
    }
