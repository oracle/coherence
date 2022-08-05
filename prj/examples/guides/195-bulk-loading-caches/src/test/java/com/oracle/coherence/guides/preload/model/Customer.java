/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.preload.model;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A class representing a customer.
 */
public class Customer
        extends AbstractEvolvable
        implements ExternalizableLite, EvolvablePortableObject
    {
    /**
     * A public no-args constructor required for serialization.
     */
    public Customer()
        {
        }

    /**
     * Create a customer.
     *
     * @param id           the customer's identifier
     * @param name         the customer's name
     * @param address      the customer's address
     * @param creditLimit  the customer's credit limit
     */
    public Customer(int id, String name, String address, int creditLimit)
        {
        this.id = id;
        this.creditLimit = creditLimit;
        this.name = name;
        this.address = address;
        }

    /**
     * Return the identifier for this {@link Customer}.
     *
     * @return the identifier for this {@link Customer}
     */
    public int getId()
        {
        return id;
        }

    /**
     * Set the identifier for this {@link Customer}.
     *
     * @param id  the identifier for this {@link Customer}
     */
    public void setId(int id)
        {
        this.id = id;
        }

    /**
     * Return the {@link Customer customer's} credit limit.
     *
     * @return the {@link Customer customer's} credit limit
     */
    public int getCreditLimit()
        {
        return creditLimit;
        }

    /**
     * Set the {@link Customer customer's} credit limit.
     *
     * @param creditLimit  the {@link Customer customer's} credit limit
     */
    public void setCreditLimit(int creditLimit)
        {
        this.creditLimit = creditLimit;
        }

    /**
     * Return the {@link Customer customer's} name.
     *
     * @return the {@link Customer customer's} name
     */
    public String getName()
        {
        return name;
        }

    /**
     * Set the {@link Customer customer's} name.
     *
     * @param name  the {@link Customer customer's} name
     */
    public void setName(String name)
        {
        this.name = name;
        }

    /**
     * Return the {@link Customer customer's} address.
     *
     * @return the {@link Customer customer's} address
     */
    public String getAddress()
        {
        return address;
        }

    /**
     * Return the {@link Customer customer's} address.
     *
     * @param address  the {@link Customer customer's} address
     */
    public void setAddress(String address)
        {
        this.address = address;
        }

    /**
     * Serialize a {@link Customer} when Coherence is using Java serialization.
     *
     * @param in  the DataInput stream to read data from in order to restore
     *            the state of this object
     *
     * @throws IOException if serialization fails
     */
    @Override
    public void readExternal(DataInput in) throws IOException
        {
        id = in.readInt();
        name = ExternalizableHelper.readSafeUTF(in);
        address = ExternalizableHelper.readSafeUTF(in);
        creditLimit = in.readInt();
        }

    /**
     * Deserialize a {@link Customer} when Coherence is using Java serialization.
     *
     * @param out  the DataOutput stream to write the state of this object to
     *
     * @throws IOException if deserialization fails
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeInt(id);
        ExternalizableHelper.writeSafeUTF(out, name);
        ExternalizableHelper.writeSafeUTF(out, address);
        out.writeInt(creditLimit);
        }

    /**
     * Serialize a {@link Customer} when Coherence is using POF serialization.
     *
     * @param in  the PofReader from which to read the object's state
     *
     * @throws IOException if serialization fails
     */
    @Override
    public void readExternal(PofReader in) throws IOException
        {
        id = in.readInt(0);
        name = in.readString(1);
        address = in.readString(2);
        creditLimit = in.readInt(3);
        }

    /**
     * Deserialize a {@link Customer} when Coherence is using POF serialization.
     *
     * @param out  the PofWriter to which to write the object's state
     *
     * @throws IOException if deserialization fails
     */
    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, id);
        out.writeString(1, name);
        out.writeString(2, address);
        out.writeInt(creditLimit, 3);
        }

    /**
     * Return the evolvable version of the class when using POF serialization.
     *
     * @return the evolvable version of the class when using POF serialization
     */
    @Override
    public int getImplVersion()
        {
        return EVOLVABLE_VERSION;
        }

    // ----- constants ------------------------------------------------------

    public static final int EVOLVABLE_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The customer's identifier.
     */
    private int id;

    /**
     * The customer's name.
     */
    private String name;

    /**
     * The customer's address.
     */
    private String address;

    /**
     * The customer's credit limit.
     */
    private int creditLimit;
    }
