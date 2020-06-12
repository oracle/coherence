/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.UnsafeByteOperations;

import com.tangosol.io.MultiBufferReadBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;

import com.tangosol.io.nio.ByteBufferReadBuffer;

import com.tangosol.util.Binary;
import com.tangosol.util.ByteSequence;
import com.tangosol.util.ExternalizableHelper;

import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

/**
 * A helper class of methods to convert between {@link Binary}
 * instances and proto-buffer byte values.
 *
 * @author Mahesh Kannan  2019.11.01
 * @author Jonathan Knight  2019.11.07
 *
 * @since 20.06
 */
public final class BinaryHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Utility class must not have public constructor.
     */
    private BinaryHelper()
        {
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Convert a {@link BytesValue} to a {@link Binary} that is decorated
     * to make the {@link Binary} suitable for use as a cache key.
     *
     * @param bytes  the {@link BytesValue} to convert
     *
     * @return a {@link Binary} that is suitable for use as a cache key
     *
     * @throws NullPointerException if the {@link BytesValue} is {@code null}
     */
    public static Binary toBinaryKey(BytesValue bytes)
        {
        return toBinaryKey(bytes.getValue());
        }

    /**
     * Convert a {@link ByteString} to a {@link Binary} that is decorated
     * to make the {@link Binary} suitable for use as a cache key.
     *
     * @param bytes  the {@link ByteString} to convert
     *
     * @return a {@link Binary} that is suitable for use as a cache key
     *
     * @throws NullPointerException if the {@link ByteString} is {@code null}
     */
    public static Binary toBinaryKey(ByteString bytes)
        {
        Binary bin = BinaryHelper.toBinary(bytes);
        return toBinaryKey(bin);
        }

    /**
     * Convert a {@link Binary} to a {@link Binary} that is decorated
     * to make the {@link Binary} suitable for use as a cache key.
     *
     * @param binary  the {@link Binary} to convert
     *
     * @return a {@link Binary} that is suitable for use as a cache key
     *
     * @throws NullPointerException if the {@link Binary} is {@code null}
     */
    public static Binary toBinaryKey(Binary binary)
        {
        if (!ExternalizableHelper.isIntDecorated((ByteSequence) binary))
            {
            return ExternalizableHelper.decorateBinary(binary, binary.hashCode()).toBinary();
            }
        return binary;
        }

    /**
     * Convert a {@link BytesValue} to a {@link Binary}.
     * <p>
     * The {@link BytesValue} is converted to a list of {@link java.nio.ByteBuffer}s
     * which are then converted to {@link ByteBufferReadBuffer}s and finally to a
     * {@link Binary} to avoid copying byte arrays.
     *
     * @param bytes  the {@link BytesValue} to convert
     *
     * @return a {@link Binary} containing the bytes of the {@link BytesValue}
     *
     * @throws NullPointerException if the {@link BytesValue} is {@code null}
     */
    public static Binary toBinary(BytesValue bytes)
        {
        return toBinary(bytes.getValue());
        }

    /**
     * Convert a {@link ByteString} to a {@link Binary}.
     * <p>
     * The {@link ByteString} is converted to a list of {@link java.nio.ByteBuffer}s
     * which are then converted to {@link ByteBufferReadBuffer}s and finally to a
     * {@link Binary} to avoid copying byte arrays.
     *
     * @param bytes  the {@link ByteString} to convert
     *
     * @return a {@link Binary} containing the bytes of the {@link ByteString}
     *
     * @throws NullPointerException if the {@link ByteString} is {@code null}
     */
    public static Binary toBinary(ByteString bytes)
        {
        return toReadBuffer(bytes).toBinary();
        }

    /**
     * Convert a {@link ByteString} to a {@link ReadBuffer}.
     * <p>
     * The {@link ByteString} is converted to a list of {@link java.nio.ByteBuffer}s
     * which are then converted to {@link ByteBufferReadBuffer}s and finally to a
     * {@link Binary} to avoid copying byte arrays.
     *
     * @param bytes  the {@link ByteString} to convert
     *
     * @return a {@link ReadBuffer} containing the bytes of the {@link ByteString}
     *
     * @throws NullPointerException if the {@link ByteString} is {@code null}
     */
    public static ReadBuffer toReadBuffer(ByteString bytes)
        {
        ReadBuffer[] readBuffers = bytes.asReadOnlyByteBufferList()
                .stream()
                .map(ByteBufferReadBuffer::new)
                .toArray(ReadBuffer[]::new);

        return new MultiBufferReadBuffer(readBuffers);
        }

    /**
     * Deserialize a serialized {@link Binary}.
     *
     * @param binary      the {@link Binary} to deserialize
     * @param serializer  the {@link Serializer} to use
     * @param <T>         the type of the original object the
     *                    {@link Binary} represents
     *
     * @return the deserialized {@link Binary} value or {@code null}
     * if the {@link Binary} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromBinary(Binary binary, Serializer serializer)
        {
        if (binary == null)
            {
            return null;
            }
        return (T) ExternalizableHelper.fromBinary(binary, serializer);
        }

    /**
     * Convert a {@link Binary} to a {@link BytesValue}.
     * <p>
     * We need to create a {@link ByteString} as efficiently as possible from
     * the undecorated {@link Binary} so we use the {@link UnsafeByteOperations} to
     * avoid copying byte arrays.
     *
     * @param binary  the {@link Binary} to convert
     *
     * @return a {@link BytesValue} containing the bytes from the {@link Binary}
     */
    public static BytesValue toBytesValue(Binary binary)
        {
        BytesValue.Builder builder = BytesValue.newBuilder();
        if (binary != null)
            {
            builder.setValue(toByteString(binary));
            }
        return builder.build();
        }

    /**
     * Convert a {@link Binary} to a {@link ByteString}.
     * <p>
     * We need to create a {@link ByteString} as efficiently as possible from
     * the undecorated {@link Binary} so we use the {@link UnsafeByteOperations} to
     * avoid copying byte arrays.
     *
     * @param binary  the {@link Binary} to convert
     *
     * @return a {@link BytesValue} containing the bytes from the {@link Binary}
     * or an empty {@link ByteString} if the {@link Binary} is {@code null}
     */
    public static ByteString toByteString(Binary binary)
        {
        if (binary != null)
            {
            Binary undecorated = ExternalizableHelper.getUndecorated((ReadBuffer) binary).toBinary();
            return UnsafeByteOperations.unsafeWrap(undecorated.toByteBuffer());
            }
        else
            {
            return ByteString.EMPTY;
            }
        }

    /**
     * A utility method to deserialize a {@link BytesValue} to an Object.
     *
     * @param bytes       the {@link BytesValue} to deserialize an Object.
     * @param serializer  the {@link Serializer} to use to convert the binary
     *                    stream to an object
     * @param <T>         the expected value type
     *
     * @return an object from the specified {@link BytesValue}
     */
    public static <T> T fromBytesValue(BytesValue bytes, Serializer serializer)
        {
        if (bytes == null)
            {
            return null;
            }
        return fromByteString(bytes.getValue(), serializer);
        }

    /**
     * A utility method to deserialize a {@link ByteString} to an Object.
     *
     * @param bytes       the {@link ByteString} to deserialize an Object.
     * @param serializer  the {@link Serializer} to use to convert the binary
     *                    stream to an object
     * @param <T>         the expected value type
     *
     * @return an object from the specified {@link ByteString}
     */
    public static <T> T fromByteString(ByteString bytes, Serializer serializer)
        {
        if (bytes != null && !bytes.isEmpty())
            {
            return ExternalizableHelper.fromBinary(BinaryHelper.toBinary(bytes), serializer);
            }

        return null;
        }

    /**
     * Serialize the specified value to a {@link BytesValue}.
     *
     * @param value       the value to serialize
     * @param serializer  the {@link Serializer} to use
     *
     * @return the serialized value as a {@link BytesValue}
     */
    public static BytesValue toBytesValue(Object value, Serializer serializer)
        {
        return BytesValue.of(toByteString(value, serializer));
        }

    /**
     * Serialize the specified value to a {@link ByteString}.
     *
     * @param value       the value to serialize
     * @param serializer  the {@link Serializer} to use
     *
     * @return the serialized value as a {@link ByteString}
     */
    public static ByteString toByteString(Object value, Serializer serializer)
        {
        Binary binary = ExternalizableHelper.toBinary(value, serializer);
        return toByteString(binary);
        }

    /**
     * Create a {@link Map} to a list of {@link Entry} instances.
     *
     * @param map         the {@link Map} to convert to a list of {@link Entry} instances
     * @param serializer  the {@link Serializer} to use
     *
     * @return a list of {@link Entry} instances
     *
     * @throws NullPointerException if the map is {@code null}
     */
    public static List<Entry> toEntryList(Map<?, ?> map, Serializer serializer)
        {
        return map.entrySet()
                .stream()
                .map(e -> toEntry(e, serializer))
                .collect(Collectors.toList());
        }

    /**
     * Create an {@link Entry} from a {@link Map.Entry}.
     *
     * @param entry       the {@link Map.Entry} to convert to an {@link Entry}
     * @param serializer  the {@link Serializer} to use
     *
     * @return an {@link Entry} containing the specified key and value
     *
     * @throws NullPointerException if the entry is {@code null}
     */
    public static Entry toEntry(Map.Entry<?, ?> entry, Serializer serializer)
        {
        Binary binary = ExternalizableHelper.toBinary(entry.getKey(), serializer);
        Binary key    = toBinaryKey(binary);
        return toEntry(toByteString(key), toByteString(entry.getValue(), serializer));
        }

    /**
     * Create an {@link Entry} containing the specified key and value.
     *
     * @param key    the entfinal ry's key
     * @param value  the entry's value
     *
     * @return an {@link Entry} containing the specified key and value
     */
    public static Entry toEntry(ByteString key, ByteString value)
        {
        return Entry.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();
        }

    // ----- constants ------------------------------------------------------

    /**
     * Singleton {@link Empty}.
     */
    public static final Empty EMPTY = Empty.getDefaultInstance();

    /**
     * Singleton empty {@link com.google.protobuf.ByteString}.
     */
    public static final ByteString EMPTY_BYTE_STRING = ByteString.EMPTY;
    }
