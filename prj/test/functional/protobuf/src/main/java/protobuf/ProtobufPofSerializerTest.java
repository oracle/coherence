/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package protobuf;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Message;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProtobufPofSerializerTest
    {
    @ParameterizedTest(name = "{index} message={0}")
    @MethodSource("messages")
    public void shouldSerializeMessage(Class<?> type, Message message) throws Exception
        {
        Binary  binary = ExternalizableHelper.toBinary(message, SERIALIZER);
        Message result = ExternalizableHelper.fromBinary(binary, SERIALIZER);
        assertThat(result, is(message));
        }

    protected static Stream<Arguments> messages()
        {
        List<Arguments> list = new ArrayList<>();

        ByteString byteString = ByteString.copyFromUtf8("Hello World");
        BytesValue bytesValue = BytesValue.of(byteString);
        list.add(Arguments.of(BytesValue.class, bytesValue));

        DoubleValue doubleValue = DoubleValue.newBuilder().setValue(1.0).build();
        list.add(Arguments.of(DoubleValue.class, doubleValue));

        Any any = Any.pack(bytesValue);
        list.add(Arguments.of(Any.class, any));

        return list.stream();
        }

    public static final ConfigurablePofContext SERIALIZER = new ConfigurablePofContext();
    }
