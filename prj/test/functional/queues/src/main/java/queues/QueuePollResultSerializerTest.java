/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues;

import com.oracle.coherence.io.json.JsonSerializer;
import com.tangosol.internal.net.queue.model.QueuePollResult;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class QueuePollResultSerializerTest
    {
    @ParameterizedTest
    @MethodSource("serializers")
    public void shouldSerialize(Serializer serializer) throws Exception
        {
        String          sValue   = "foo";
        Binary          binValue = ExternalizableHelper.toBinary(sValue, serializer);
        QueuePollResult result   = new QueuePollResult(19L, binValue);
        assertThat(result.getId(), is(19L));
        assertThat(result.getBinaryElement(), is(binValue));

        Binary          binary       = ExternalizableHelper.toBinary(result, serializer);
        QueuePollResult deserialized = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(deserialized.getId(), is(19L));
        assertThat(deserialized.getBinaryElement(), is(binValue));
        }

    static Stream<Arguments> serializers()
        {
        List<Arguments> list = new ArrayList<>();
        list.add(Arguments.of(new DefaultSerializer()));
        list.add(Arguments.of(new ConfigurablePofContext()));
        return list.stream();
        }
    }
