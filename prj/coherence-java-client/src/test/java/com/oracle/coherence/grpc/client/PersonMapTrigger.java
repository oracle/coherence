/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.MapTrigger;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A test {@link com.tangosol.util.MapTrigger} that converts a person's last name
 * to upper case.
 *
 * @author Jonathan Knight  2019.12.17
 * @since 20.06
 */
public class PersonMapTrigger
        implements MapTrigger<String, Person>, ExternalizableLite, PortableObject
    {

    // ----- MapTrigger interface -------------------------------------------

    @Override
    public void process(Entry<String, Person> entry)
        {
        if (entry.isPresent())
            {
            Person person = entry.getValue();
            person.setLastName(String.valueOf(person.getLastName()).toUpperCase());
            entry.setValue(person);
            }
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public int hashCode()
        {
        return PersonMapTrigger.class.hashCode();
        }

    @Override
    public boolean equals(Object obj)
        {
        return obj != null && PersonMapTrigger.class.equals(obj.getClass());
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }
    }
