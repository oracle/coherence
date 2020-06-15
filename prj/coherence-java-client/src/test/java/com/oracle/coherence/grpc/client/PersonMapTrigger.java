/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
