/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

import data.pof.PortablePerson;


/**
 * Tests for JsonJacksonMarshaller.
 *
 * @author as  2011.07.14
 */
public class JsonJacksonMarshallerTest
        extends AbstractMarshallerTest
    {
    protected Marshaller getMarshaller()
        {
        return new JacksonJsonMarshaller(PortablePerson.class);
        }

    protected String getExpectedValue()
        {
        return "{\"@type\":\".PortablePerson\",\"address\":{\"city\":\"Tampa\","
                + "\"state\":\"FL\",\"street\":\"123 Main St\",\"zip\":\"12345\"},"
                + "\"age\":36,\"children\":[{\"@type\":\".PortablePerson\",\"address\":null,"
                + "\"age\":6,\"children\":[],\"dateOfBirth\":\"2004-08-14\",\"name\":\"Ana Maria Seovic\",\"phoneNumbers\":{},"
                + "\"spouse\":null},{\"@type\":\".PortablePerson\",\"address\":null,\"age\":3,\"children\":[],"
                + "\"dateOfBirth\":\"2008-12-28\",\"name\":\"Novak Seovic\",\"phoneNumbers\":{},\"spouse\":null}],"
                + "\"dateOfBirth\":\"1974-08-24\",\"name\":\"Aleksandar Seovic\",\"phoneNumbers\":{},"
                + "\"spouse\":{\"@type\":\".PortablePerson\",\"address\":null,\"age\":33,\"children\":[],"
                + "\"dateOfBirth\":\"1978-02-20\",\"name\":\"Marija Seovic\",\"phoneNumbers\":{},"
                + "\"spouse\":null}}";
        }

    protected String getExpectedFragment()
        {
        return getExpectedValue();
        }
    }
