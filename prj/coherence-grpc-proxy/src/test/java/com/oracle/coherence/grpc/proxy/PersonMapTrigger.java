/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.MapTrigger;

import java.io.DataInput;
import java.io.DataOutput;

/**
 * A test {@link MapTrigger} that converts a person's last name
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
    public void readExternal(DataInput in)
        {
        }

    @Override
    public void writeExternal(DataOutput out)
        {
        }

    @Override
    public void readExternal(PofReader in)
        {
        }

    @Override
    public void writeExternal(PofWriter out)
        {
        }
    }
