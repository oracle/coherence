/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client.model;


import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

// # tag::src[]

/**
 * A simple user entity.
 */
public class User
        implements PortableObject, ExternalizableLite
    {
    /**
     * The user's identifier.
     */
    private String id;

    /**
     * The user's first name.
     */
    private String firstName;

    /**
     * The user's last name.
     */
    private String lastName;

    /**
     * The user's email address.
     */
    private String email;

    /**
     * A default constructor, required for Coherence serialization.
     */
    public User()
        {
        }

    /**
     * Create a user.
     *
     * @param id         the user's identifier
     * @param firstName  the user's first name
     * @param lastName   the user's last name
     * @param email      the user's email address
     */
    public User(String id, String firstName, String lastName, String email)
        {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        }

    /**
     * Returns the user's identifier.
     *
     * @return the user's identifier
     */
    public String getId()
        {
        return id;
        }

    /**
     * Returns the user's first name.
     *
     * @return the user's first name
     */
    public String getFirstName()
        {
        return firstName;
        }

    /**
     * Set the user's first name.
     *
     * @param firstName  the user's first name
     */
    public void setFirstName(String firstName)
        {
        this.firstName = firstName;
        }

    /**
     * Returns the user's last name.
     *
     * @return the user's last name
     */
    public String getLastName()
        {
        return lastName;
        }

    /**
     * Set the user's last name.
     *
     * @param lastName  the user's last name
     */
    public void setLastName(String lastName)
        {
        this.lastName = lastName;
        }

    /**
     * Returns the user's email address.
     *
     * @return the user's email address
     */
    public String getEmail()
        {
        return email;
        }

    /**
     * Set the user's email address.
     *
     * @param email  the user's email address
     */
    public void setEmail(String email)
        {
        this.email = email;
        }
    // # end::src[]

    /**
     * Deserialize a {@link User} when Java serialization is being used.
     *
     * @param in  the DataInput stream to read data from in order to restore
     *            the state of this object
     *
     * @throws IOException if serialization fails
     */
    @Override
    public void readExternal(DataInput in) throws IOException
        {
        id = ExternalizableHelper.readSafeUTF(in);
        firstName = ExternalizableHelper.readSafeUTF(in);
        lastName = ExternalizableHelper.readSafeUTF(in);
        email = ExternalizableHelper.readSafeUTF(in);
        }

    /**
     * Serialize a {@link User} when Java serialization is being used.
     *
     * @param out  the DataOutput stream to write the state of this object to
     *
     * @throws IOException if serialization fails
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, id);
        ExternalizableHelper.writeSafeUTF(out, firstName);
        ExternalizableHelper.writeSafeUTF(out, lastName);
        ExternalizableHelper.writeSafeUTF(out, email);
        }

    /**
     * Deserialize a {@link User} when POF serialization is being used.
     *
     * @param in  the PofReader from which to read the object's state
     *
     * @throws IOException if serialization fails
     */
    @Override
    public void readExternal(PofReader in) throws IOException
        {
        id = in.readString(0);
        firstName = in.readString(1);
        lastName = in.readString(2);
        email = in.readString(3);
        }

    /**
     * Serialize a {@link User} when POF serialization is being used.
     *
     * @param out  the PofWriter to which to write the object's state
     *
     * @throws IOException if serialization fails
     */
    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, id);
        out.writeString(1, firstName);
        out.writeString(2, lastName);
        out.writeString(3, email);
        }
    }
