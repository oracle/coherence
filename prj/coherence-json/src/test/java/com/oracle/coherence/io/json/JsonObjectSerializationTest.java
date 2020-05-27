/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.util.ArrayList;
import java.util.List;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link JsonObject} serialization.
 *
 * @since 14.1.2
 */
class JsonObjectSerializationTest
    {
    // ----- test cases -----------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void testRoundTripSerialization(String sName, Serializer serializer)
        {
        JsonObject o = new JsonObject()
                .setVersion(2)
                .set("name", "Aleks")
                .set("age", 41)
                .set("minor", false)
                .set("address", new JsonObject().set("city", "Tampa"));

        assertEquals(o, roundTrip(sName, o, serializer));
        }

    // ----- helper methods -------------------------------------------------

    protected Object roundTrip(String sName, JsonObject mapJson, Serializer serializer)
        {
        Binary bin = ExternalizableHelper.toBinary(mapJson, serializer);
        System.out.println(sName + " (" + bin.length() + " bytes):\n" + new String(bin.toByteArray()));
        return ExternalizableHelper.fromBinary(bin, serializer);
        }

    protected static Stream<Arguments> serializers()
        {
        List<Arguments> args = new ArrayList<>();

        args.add(Arguments.of("java", new DefaultSerializer()));
        args.add(Arguments.of("pof", new ConfigurablePofContext("json-pof-config.xml")));
        args.add(Arguments.of("json", new JsonSerializer()));

        return args.stream();
        }
    }
