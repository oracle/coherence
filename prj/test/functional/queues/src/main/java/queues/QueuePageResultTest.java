/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues;

import com.tangosol.internal.net.queue.model.QueuePageResult;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"unchecked", "rawtypes"})
public class QueuePageResultTest
    {
    @Test
    public void shouldHavePOFBinaryList()
        {
        List<String>            list       = Arrays.asList("one", "two", "three");
        List<Binary>    listBinary = list.stream().map(m_converterToPof::convert).collect(Collectors.toList());
        QueuePageResult result     = new QueuePageResult(1234L, listBinary);

        assertThat(result.getKey(), is(1234L));
        assertThat(result.getBinaryList(), is(listBinary));
        }

    @Test
    public void shouldHaveJavaBinaryList()
        {
        List<String>            list       = Arrays.asList("one", "two", "three");
        List<Binary>    listBinary = list.stream().map(m_converterToJava::convert).collect(Collectors.toList());
        QueuePageResult result     = new QueuePageResult(1234L, listBinary);

        assertThat(result.getKey(), is(1234L));
        assertThat(result.getBinaryList(), is(listBinary));
        }

    @Test
    public void shouldRoundTripAsPOF()
        {
        List<String>            list       = Arrays.asList("one", "two", "three");
        List<Binary>    listBinary = list.stream().map(m_converterToPof::convert).collect(Collectors.toList());
        QueuePageResult original   = new QueuePageResult(1234L, listBinary);

        Binary          binary = ExternalizableHelper.toBinary(original, m_pofSerializer);
        QueuePageResult result = ExternalizableHelper.fromBinary(binary, m_pofSerializer);

        assertThat(result.getKey(), is(1234L));
        assertThat(result.getBinaryList(), is(listBinary));
        }

    @Test
    public void shouldRoundTripAsJava()
        {
        List<String>            list       = Arrays.asList("one", "two", "three");
        List<Binary>    listBinary = list.stream().map(m_converterToJava::convert).collect(Collectors.toList());
        QueuePageResult original   = new QueuePageResult(1234L, listBinary);

        Binary          binary = ExternalizableHelper.toBinary(original, m_javaSerializer);
        QueuePageResult result = ExternalizableHelper.fromBinary(binary, m_javaSerializer);

        assertThat(result.getKey(), is(1234L));
        assertThat(result.getBinaryList(), is(listBinary));
        }

    @Test
    public void shouldRoundTripAsPofJavaPof()
        {
        List<String>            list       = Arrays.asList("one", "two", "three");
        List<Binary>    listBinary = list.stream().map(m_converterToPof::convert).collect(Collectors.toList());
        QueuePageResult original   = new QueuePageResult(1234L, listBinary);

        Binary          binary = ExternalizableHelper.toBinary(original, m_pofSerializer);
        QueuePageResult result = ExternalizableHelper.fromBinary(binary, m_pofSerializer);

        binary = ExternalizableHelper.toBinary(result, m_javaSerializer);
        result = ExternalizableHelper.fromBinary(binary, m_javaSerializer);
        binary = ExternalizableHelper.toBinary(result, m_pofSerializer);
        result = ExternalizableHelper.fromBinary(binary, m_pofSerializer);

        assertThat(result.getKey(), is(1234L));
        assertThat(result.getBinaryList(), is(listBinary));
        }

    // ----- data members ---------------------------------------------------

    public static final Serializer m_pofSerializer = new ConfigurablePofContext("coherence-pof-config.xml");

    public static final Converter<Binary, ?> m_converterFromPof = bin -> ExternalizableHelper.fromBinary(bin, m_pofSerializer);

    public static final Converter<Object, Binary> m_converterToPof = bin -> ExternalizableHelper.toBinary(bin, m_pofSerializer);

    public static final Serializer m_javaSerializer = new DefaultSerializer();

    public static final Converter<Binary, Object> m_converterFromJava = bin -> ExternalizableHelper.fromBinary(bin, m_javaSerializer);

    public static final Converter<Object, Binary> m_converterToJava = bin -> ExternalizableHelper.toBinary(bin, m_javaSerializer);
    }
