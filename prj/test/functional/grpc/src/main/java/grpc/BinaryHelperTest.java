/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;

import com.oracle.coherence.grpc.BinaryHelper;

import com.tangosol.io.DefaultSerializer;

import com.tangosol.io.Serializer;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
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

    // ----- constants ------------------------------------------------------

    private static final Serializer SERIALIZER = new DefaultSerializer();
    }
