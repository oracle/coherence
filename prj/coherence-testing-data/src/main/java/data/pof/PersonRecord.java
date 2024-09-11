/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package data.pof;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

public record PersonRecord(String name, int age, String city, Address address)
    {
    public static class PofSerializer implements com.tangosol.io.pof.PofSerializer<PersonRecord>
        {
        public void serialize(PofWriter out, PersonRecord value)
                throws IOException
            {
            out.writeString(NAME, value.name());
            out.writeInt(AGE, value.age());
            out.writeString(CITY, value.city());
            out.writeObject(ADDRESS, value.address());
            out.writeRemainder(null);
            }

        public PersonRecord deserialize(PofReader in) throws IOException
            {
            String name = in.readString(NAME);
            int age = in.readInt(AGE);
            String city = in.readString(CITY);
            Address addr = in.readObject(ADDRESS);

            in.readRemainder();
            return new PersonRecord(name, age, city, addr);
            }
        }

    public static final int NAME    = 0;
    public static final int AGE     = 1;
    public static final int CITY    = 2;
    public static final int ADDRESS = 3;
    }
