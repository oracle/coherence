/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.partitions.bank;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A simple representation of a customer.
 * <p>
 * This class implements {@link com.tangosol.io.pof.EvolvablePortableObject} by
 * extending {@link AbstractEvolvable} so that the class can be changed in a
 * backwards compatible to support rolling upgrades.
 *
 * @author Jonathan Knight 2023.01.14
 * @since 22.06.4
 */
public class Customer
        extends AbstractEvolvable
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * A default no-args constructor required for serialization.
     */
    public Customer()
        {
        }

    /**
     * Create a customer.
     *
     * @param id         the customer's identifier
     * @param firstName  the customer's first name
     * @param lastName   the customer's last name
     */
    public Customer(CustomerId id, String firstName, String lastName)
        {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the customer's identifier.
     *
     * @return the customer's identifier
     */
    public CustomerId getId()
        {
        return id;
        }

    /**
     * Returns the customer's first name.
     *
     * @return the customer's first name
     */
    public String getFirstName()
        {
        return firstName;
        }

    /**
     * Set the customer's first name.
     *
     * @param firstName  the customer's first name
     */
    public void setFirstName(String firstName)
        {
        this.firstName = firstName;
        }

    /**
     * Returns the customer's last name.
     *
     * @return the customer's last name
     */
    public String getLastName()
        {
        return lastName;
        }

    /**
     * Set the customer's last name.
     *
     * @param lastName  the customer's last name
     */
    public void setLastName(String lastName)
        {
        this.lastName = lastName;
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        id = ExternalizableHelper.readObject(in);
        firstName = ExternalizableHelper.readSafeUTF(in);
        lastName = ExternalizableHelper.readSafeUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, id);
        ExternalizableHelper.writeSafeUTF(out, firstName);
        ExternalizableHelper.writeSafeUTF(out, lastName);
        }

    // ----- AbstractEvolvable / PortableObject methods ---------------------

    @Override
    public int getImplVersion()
        {
        return IMPLEMENTATION_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        id = in.readObject(0);
        firstName = in.readString(1);
        lastName = in.readString(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, id);
        out.writeString(1, firstName);
        out.writeString(2, lastName);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The evolvable portable object implementation version for this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The customer's identifier.
     */
    private CustomerId id;

    /**
     * The customer's first name.
     */
    private String firstName;

    /**
     * The customer's last name.
     */
    private String lastName;
    }
