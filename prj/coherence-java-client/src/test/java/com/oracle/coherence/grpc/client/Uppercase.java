/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.InvocableMap;

import java.io.IOException;

/**
 * An entry processor to convert a {@link Person}'s
 * last name to upper case.
 *
 * @author Jonathan Knight  2020.06.22
 */
public class Uppercase
        implements InvocableMap.EntryProcessor<String, Person, Object>,
                   PortableObject

    {
    // ----- InvocableMap.EntryProcessor methods ----------------------------

    @Override
    public Object process(InvocableMap.Entry<String, Person> entry)
        {
        Person p = entry.getValue();
        p.setLastName(p.getLastName().toUpperCase());
        entry.setValue(p);
        return null;
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }
    }
