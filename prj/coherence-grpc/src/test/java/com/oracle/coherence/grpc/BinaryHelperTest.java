/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;

import com.tangosol.io.DefaultSerializer;

import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;

import com.tangosol.util.Binary;
import com.tangosol.util.ByteSequence;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.SimpleMapEntry;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2019.11.26
 */
@SuppressWarnings("ConstantConditions")
class BinaryHelperTest
    {
    // ----- test methods ---------------------------------------------------

    @Test
    void shouldDeserializeBinary()
        {
        String value  = "foo";
        Binary binary = ExternalizableHelper.toBinary(value, SERIALIZER);
        Object result = BinaryHelper.fromBinary(binary, SERIALIZER);

        assertThat(result, is(value));
        }

    @Test
    void shouldDeserializeNullBinary()
        {
        Object result = BinaryHelper.fromBinary(null, SERIALIZER);
        assertThat(result, is(nullValue()));
        }

    @Test
    void shouldConvertFromByteString()
        {
        String     value  = "foo";
        Binary     binary = ExternalizableHelper.toBinary(value, SERIALIZER);
        ByteString bytes  = ByteString.copyFrom(binary.toByteArray());
        Object     result = BinaryHelper.fromByteString(bytes, SERIALIZER);

        assertThat(result, is(value));
        }

    @Test
    void shouldConvertFromEmptyByteString()
        {
        Object result = BinaryHelper.fromByteString(ByteString.EMPTY, SERIALIZER);
        assertThat(result, is(nullValue()));
        }

    @Test
    void shouldConvertFromNullByteString()
        {
        Object result = BinaryHelper.fromByteString(null, SERIALIZER);
        assertThat(result, is(nullValue()));
        }

    @Test
    void shouldConvertFromBytesValue()
        {
        String     value  = "foo";
        Binary     binary = ExternalizableHelper.toBinary(value, SERIALIZER);
        BytesValue bytes  = BytesValue.of(ByteString.copyFrom(binary.toByteArray()));
        Object     result = BinaryHelper.fromBytesValue(bytes, SERIALIZER);

        assertThat(result, is(value));
        }

    @Test
    void shouldConvertFromEmptyBytesValue()
        {
        Object result = BinaryHelper.fromBytesValue(BytesValue.of(ByteString.EMPTY), SERIALIZER);
        assertThat(result, is(nullValue()));
        }

    @Test
    void shouldConvertFromNullBytesValue()
        {
        Object result = BinaryHelper.fromBytesValue(null, SERIALIZER);
        assertThat(result, is(nullValue()));
        }

    @Test
    void shouldConvertBinaryToBinaryKey()
        {
        Binary binary = new Binary(new byte[] {1, 2, 3, 4});
        Binary key    = BinaryHelper.toBinaryKey(binary);

        assertThat(ExternalizableHelper.isIntDecorated((ByteSequence) key), is(true));
        assertThat(ExternalizableHelper.extractIntDecoration(key),          is(binary.hashCode()));
        }

    @Test
    void shouldConvertBinaryKeyToBinaryKey()
        {
        Binary     binary          = new Binary(new byte[] {1, 2, 3, 4});
        ReadBuffer readBuffer      = ExternalizableHelper.decorateBinary(binary, 19);
        Binary     decoratedBinary = readBuffer.toBinary();
        Binary     key             = BinaryHelper.toBinaryKey(decoratedBinary);

        assertThat(key, is(sameInstance(decoratedBinary)));
        }

    @Test
    void shouldConvertByteStringToBinary()
        {
        ByteString bytes  = ByteString.copyFrom(new byte[] {1, 2, 3, 4});
        Binary     binary = BinaryHelper.toBinary(bytes);

        assertThat(binary.toByteArray(), is(bytes.toByteArray()));
        }

    @Test
    void shouldConvertBytesValueToBinary()
        {
        ByteString bytes      = ByteString.copyFrom(new byte[] {1, 2, 3, 4});
        BytesValue bytesValue = BytesValue.of(bytes);
        Binary     binary     = BinaryHelper.toBinary(bytesValue);

        assertThat(binary.toByteArray(), is(bytes.toByteArray()));
        }

    @Test
    void shouldConvertByteStringToBinaryKey()
        {
        ByteString bytes  = ByteString.copyFrom(new byte[] {1, 2, 3, 4});
        Binary     binary = BinaryHelper.toBinary(bytes);
        Binary     key    = BinaryHelper.toBinaryKey(bytes);

        assertThat(ExternalizableHelper.isIntDecorated((ByteSequence) key), is(true));
        assertThat(ExternalizableHelper.extractIntDecoration(key),          is(binary.hashCode()));
        }

    @Test
    void shouldConvertBytesValueToBinaryKey()
        {
        ByteString bytes      = ByteString.copyFrom(new byte[] {1, 2, 3, 4});
        BytesValue bytesValue = BytesValue.of(bytes);
        Binary     binary     = BinaryHelper.toBinary(bytesValue);
        Binary     key        = BinaryHelper.toBinaryKey(bytesValue);

        assertThat(ExternalizableHelper.isIntDecorated((ByteSequence) key), is(true));
        assertThat(ExternalizableHelper.extractIntDecoration(key),          is(binary.hashCode()));
        }

    @Test
    void shouldConvertBinaryToByteString()
        {
        Binary     binary = new Binary(new byte[] {1, 2, 3, 4});
        ByteString bytes  = BinaryHelper.toByteString(binary);

        assertThat(bytes.toByteArray(), is(binary.toByteArray()));
        }

    @Test
    void shouldConvertNullBinaryToByteString()
        {
        ByteString bytes = BinaryHelper.toByteString(null);
        assertThat(bytes.isEmpty(), is(true));
        }

    @Test
    void shouldConvertBinaryToBytesValue()
        {
        Binary     binary = new Binary(new byte[] {1, 2, 3, 4});
        BytesValue bytes  = BinaryHelper.toBytesValue(binary);

        assertThat(bytes.getValue().toByteArray(), is(binary.toByteArray()));
        }

    @Test
    void shouldConvertNullBinaryToBytesValue()
        {
        BytesValue bytes = BinaryHelper.toBytesValue(null);
        assertThat(bytes.getValue().isEmpty(), is(true));
        }

    @Test
    void shouldConvertObjectToByteString()
        {
        ByteString bytes = BinaryHelper.toByteString("foo", SERIALIZER);
        assertThat(bytes, is(notNullValue()));

        Object value = ExternalizableHelper.fromBinary(new Binary(bytes.toByteArray()), SERIALIZER);
        assertThat(value, is("foo"));
        }

    @Test
    void shouldConvertObjectToBytesValue()
        {
        BytesValue bytes = BinaryHelper.toBytesValue("foo", SERIALIZER);
        assertThat(bytes, is(notNullValue()));

        Object value = ExternalizableHelper.fromBinary(new Binary(bytes.getValue().toByteArray()), SERIALIZER);
        assertThat(value, is("foo"));
        }

    @Test
    void shouldCreateEntry()
        {
        ByteString key   = ByteString.copyFrom(new byte[] {1, 2, 3, 4});
        ByteString value = ByteString.copyFrom(new byte[] {11, 12, 13, 14});
        Entry      entry = BinaryHelper.toEntry(key, value);

        assertThat(entry,            is(notNullValue()));
        assertThat(entry.getKey(),   is(key));
        assertThat(entry.getValue(), is(value));
        }

    @Test
    void shouldCreateEntryFromMapEntry()
        {
        SimpleMapEntry<String, String> mapEntry = new SimpleMapEntry<>("key-1", "value-1");
        Entry                          entry    = BinaryHelper.toEntry(mapEntry, SERIALIZER);

        assertThat(entry, is(notNullValue()));
        assertThat(ExternalizableHelper.fromBinary(new Binary(entry.getKey().toByteArray())),   is("key-1"));
        assertThat(ExternalizableHelper.fromBinary(new Binary(entry.getValue().toByteArray())), is("value-1"));
        }


    @Test
    void shouldCreateEntryFromMap()
        {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("key-1", "value-1");
        map.put("key-2", "value-2");
        map.put("key-3", "value-3");

        List<Entry> list = BinaryHelper.toEntryList(map, SERIALIZER);
        assertThat(list,        is(notNullValue()));
        assertThat(list.size(), is(map.size()));

        assertThat(ExternalizableHelper.fromBinary(new Binary(list.get(0).getKey().toByteArray())),   is("key-1"));
        assertThat(ExternalizableHelper.fromBinary(new Binary(list.get(0).getValue().toByteArray())), is("value-1"));
        assertThat(ExternalizableHelper.fromBinary(new Binary(list.get(1).getKey().toByteArray())),   is("key-2"));
        assertThat(ExternalizableHelper.fromBinary(new Binary(list.get(1).getValue().toByteArray())), is("value-2"));
        assertThat(ExternalizableHelper.fromBinary(new Binary(list.get(2).getKey().toByteArray())),   is("key-3"));
        assertThat(ExternalizableHelper.fromBinary(new Binary(list.get(2).getValue().toByteArray())), is("value-3"));
        }

    // ----- constants ------------------------------------------------------

    private static final Serializer SERIALIZER = new DefaultSerializer();
    }
