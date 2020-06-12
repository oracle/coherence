/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import java.lang.reflect.Modifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test serialization of entry processors.
 *
 * @author Jonathan Knight  2019.11.11
 * @since 14.1.2
 */
class ProcessorTest
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        s_pofContext = new ConfigurablePofContext("coherence-grpc-proxy-pof-config.xml");
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Assert that every non-abstract inner class of {@link Processors} that implements
     * {@link com.tangosol.util.InvocableMap.EntryProcessor} is in the POF config file.
     *
     * @param cls the class to test
     */
    @ParameterizedTest
    @MethodSource("getProcessorClasses")
    public void shouldAllBeInPofPofConfiguration(Class cls)
        {
        assertThat(String.format("Class %s is missing from the coherence-grpc-proxy-pof-config.xml POF configuration file", cls),
                   s_pofContext.isUserType(cls), is(true));
        }

    @ParameterizedTest
    @MethodSource("getSerializers")
    public void shouldSerializeContainsValueProcessor(Serializer serializer)
        {
        Binary                            value     = new Binary(new byte[] {1, 2, 3, 4, 5, 6});
        Processors.ContainsValueProcessor processor = new Processors.ContainsValueProcessor(value);
        Binary                            binary    = ExternalizableHelper.toBinary(processor, serializer);
        Object                            oResult   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(oResult, is(instanceOf(Processors.ContainsValueProcessor.class)));
        assertThat(((Processors.ContainsValueProcessor) oResult).getValue(), is(value));
        }

    @ParameterizedTest
    @MethodSource("getSerializers")
    public void shouldSerializeGetProcessor(Serializer serializer)
        {
        Processors.GetProcessor processor = new Processors.GetProcessor();
        Binary                  binary    = ExternalizableHelper.toBinary(processor, serializer);
        Object                  oResult   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(oResult, is(instanceOf(Processors.GetProcessor.class)));
        }

    @ParameterizedTest
    @MethodSource("getSerializers")
    public void shouldSerializePutIfAbsentProcessor(Serializer serializer)
        {
        Binary                          value     = new Binary(new byte[] {1, 2, 3, 4, 5, 6});
        Processors.PutIfAbsentProcessor processor = new Processors.PutIfAbsentProcessor(value, 1234L);
        Binary                          binary    = ExternalizableHelper.toBinary(processor, serializer);
        Object                          oResult   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(oResult, is(instanceOf(Processors.PutIfAbsentProcessor.class)));
        assertThat(((Processors.PutIfAbsentProcessor) oResult).getValue(), is(value));
        assertThat(((Processors.PutProcessor) oResult).getTtl(),           is(1234L));
        }

    @ParameterizedTest
    @MethodSource("getSerializers")
    public void shouldSerializePutProcessor(Serializer serializer)
        {
        Binary                  value     = new Binary(new byte[] {1, 2, 3, 4, 5, 6});
        Processors.PutProcessor processor = new Processors.PutProcessor(value, 1234L);
        Binary                  binary    = ExternalizableHelper.toBinary(processor, serializer);
        Object                  oResult   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(oResult, is(instanceOf(Processors.PutProcessor.class)));
        assertThat(((Processors.PutProcessor) oResult).getValue(), is(value));
        assertThat(((Processors.PutProcessor) oResult).getTtl(),   is(1234L));
        }

    @ParameterizedTest
    @MethodSource("getSerializers")
    public void shouldSerializePutAllProcessor(Serializer serializer)
        {
        Binary              key1   = new Binary(new byte[] {1, 2, 3, 4, 5, 6});
        Binary              value1 = new Binary(new byte[] {11, 12, 13, 14, 15, 16});
        Binary              key2   = new Binary(new byte[] {21, 22, 23, 24, 25, 26});
        Binary              value2 = new Binary(new byte[] {31, 32, 33, 34, 35, 36});
        Map<Binary, Binary> map    = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);

        InvocableMap.EntryProcessor<Binary, Binary, Binary> processor = Processors.putAll(map);

        Binary binary  = ExternalizableHelper.toBinary(processor, serializer);
        Object oResult = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(oResult, is(instanceOf(Processors.PutAllProcessor.class)));
        assertThat(((Processors.PutAllProcessor) oResult).getMap(), is(map));
        }

    @ParameterizedTest
    @MethodSource("getSerializers")
    public void shouldSerializeRemoveBlindProcessor(Serializer serializer)
        {
        Processors.RemoveBlindProcessor processor = new Processors.RemoveBlindProcessor();
        Binary                          binary    = ExternalizableHelper.toBinary(processor, serializer);
        Object                          oResult   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(oResult, is(instanceOf(Processors.RemoveBlindProcessor.class)));
        }

    @ParameterizedTest
    @MethodSource("getSerializers")
    public void shouldSerializeRemoveProcessor(Serializer serializer)
        {
        Processors.RemoveProcessor processor = new Processors.RemoveProcessor();
        Binary                     binary    = ExternalizableHelper.toBinary(processor, serializer);
        Object                     oResult   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(oResult, is(instanceOf(Processors.RemoveProcessor.class)));
        }

    @ParameterizedTest
    @MethodSource("getSerializers")
    public void shouldSerializeReplaceMappingProcessor(Serializer serializer)
        {
        Binary                             newValue  = new Binary(new byte[] {1, 2, 3, 4, 5, 6});
        Binary                             prevValue = new Binary(new byte[] {9, 8, 7, 6, 5});
        Processors.ReplaceMappingProcessor processor = new Processors.ReplaceMappingProcessor(prevValue, newValue);
        Binary                             binary    = ExternalizableHelper.toBinary(processor, serializer);
        Object                             oResult   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(oResult, is(instanceOf(Processors.ReplaceMappingProcessor.class)));
        assertThat(((Processors.ReplaceMappingProcessor) oResult).getValue(),    is(prevValue));
        assertThat(((Processors.ReplaceMappingProcessor) oResult).getNewValue(), is(newValue));
        }

    @ParameterizedTest
    @MethodSource("getSerializers")
    public void shouldSerializeReplaceProcessorReplaceProcessor(Serializer serializer)
        {
        Binary                      value     = new Binary(new byte[] {1, 2, 3, 4, 5, 6});
        Processors.ReplaceProcessor processor = new Processors.ReplaceProcessor(value);
        Binary                      binary    = ExternalizableHelper.toBinary(processor, serializer);
        Object                      oResult   = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(oResult, is(instanceOf(Processors.ReplaceProcessor.class)));
        assertThat(((Processors.ReplaceProcessor) oResult).getValue(), is(value));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain a stream of test arguments that are all of the inner classes of
     * the {@link com.oracle.coherence.grpc.proxy.Processors} class that are
     * not abstract and that implement {@link com.tangosol.util.InvocableMap.EntryProcessor}
     *
     * @return a stream of test arguments
     */
    protected static Stream<Arguments> getProcessorClasses()
        {
        return Arrays.stream(Processors.class.getClasses())
                .filter(InvocableMap.EntryProcessor.class::isAssignableFrom)
                .filter(cls -> Processors.class.equals(cls.getDeclaringClass()))
                .filter(cls -> !Modifier.isAbstract(cls.getModifiers()))
                .map(Arguments::of);
        }

    /**
     * Obtain a stream of JUnit test arguments that are all of the serializers
     * to use to test serialization. This is currently POF and Java.
     *
     * @return a stream of test arguments
     */
    protected static Stream<Arguments> getSerializers()
        {
        return Arrays.stream(new Arguments[] {
                Arguments.of(s_pofContext),
                Arguments.of(new DefaultSerializer())
        });
        }

    // ----- data members ---------------------------------------------------

    private static ConfigurablePofContext s_pofContext;
    }
