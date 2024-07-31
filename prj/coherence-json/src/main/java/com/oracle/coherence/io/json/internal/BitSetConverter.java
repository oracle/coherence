/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.common.base.Formatting;
import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;
import jakarta.json.bind.adapter.JsonbAdapter;

import java.util.BitSet;

/**
 * A Genson converter for a {@link BitSet}.
 */
public class BitSetConverter
        implements JsonbAdapter<BitSet, byte[]>, Converter<BitSet>
    {
    @Override
    public void serialize(BitSet bitSet, ObjectWriter writer, Context context) throws Exception
        {
        writer.writeString(Formatting.toHexEscape(bitSet.toByteArray()));
        }

    @Override
    public BitSet deserialize(ObjectReader reader, Context context) throws Exception
        {
        byte[] bytes = Formatting.parseHex(reader.valueAsString());
        return BitSet.valueOf(bytes);
        }

    @Override
    public byte[] adaptToJson(BitSet bits)
        {
        return bits.toByteArray();
        }

    @Override
    public BitSet adaptFromJson(byte[] bytes) throws Exception
        {
        return BitSet.valueOf(bytes);
        }

    public static final BitSetConverter INSTANCE = new BitSetConverter();
    }
