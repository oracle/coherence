/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.serialization;

import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Cross-release variant of {@link PofDeserializationTelemetryBenchmark}.
 *
 * <p>This benchmark uses the identical complex POF graph and deserialization
 * operation, but deliberately has no compile-time dependency on the
 * post-hardening telemetry APIs. It can therefore be launched with an older
 * Coherence jar first on the class path to establish a pre-hardening
 * baseline.</p>
 *
 * @author Aleks Seovic  2026.09.01
 * @since 26.10
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xms1g", "-Xmx1g"})
public class PofDeserializationCompatibilityBenchmark
    {
    // ----- benchmark methods ---------------------------------------------

    @Benchmark
    @Threads(1)
    public PofDeserializationTelemetryBenchmark.OrderPayload deserializeOneThread(BenchmarkState state)
        {
        return ExternalizableHelper.fromBinary(state.m_binary, state.m_context);
        }

    @Benchmark
    @Threads(8)
    public PofDeserializationTelemetryBenchmark.OrderPayload deserializeEightThreads(BenchmarkState state)
        {
        return ExternalizableHelper.fromBinary(state.m_binary, state.m_context);
        }

    @Benchmark
    @Threads(32)
    public PofDeserializationTelemetryBenchmark.OrderPayload deserializeThirtyTwoThreads(BenchmarkState state)
        {
        return ExternalizableHelper.fromBinary(state.m_binary, state.m_context);
        }

    // ----- inner class: BenchmarkState -----------------------------------

    /**
     * POF context and serialized payload shared by all benchmark workers.
     */
    @State(Scope.Benchmark)
    public static class BenchmarkState
        {
        @Setup(Level.Trial)
        public void setup()
            {
            m_context = new SimplePofContext();
            register(TYPE_ORDER, PofDeserializationTelemetryBenchmark.OrderPayload.class);
            register(TYPE_CUSTOMER, PofDeserializationTelemetryBenchmark.Customer.class);
            register(TYPE_ADDRESS, PofDeserializationTelemetryBenchmark.Address.class);
            register(TYPE_LINE_ITEM, PofDeserializationTelemetryBenchmark.LineItem.class);
            register(TYPE_PRODUCT, PofDeserializationTelemetryBenchmark.Product.class);
            register(TYPE_MONEY, PofDeserializationTelemetryBenchmark.Money.class);
            register(TYPE_ATTRIBUTE, PofDeserializationTelemetryBenchmark.AttributeValue.class);
            register(TYPE_AUDIT, PofDeserializationTelemetryBenchmark.AuditInfo.class);

            PofDeserializationTelemetryBenchmark.OrderPayload payload =
                    PofDeserializationTelemetryBenchmark.BenchmarkState.createPayload();
            m_binary = ExternalizableHelper.toBinary(payload, m_context);

            if (ExternalizableHelper.fromBinary(m_binary, m_context) == null)
                {
                throw new IllegalStateException("POF payload did not round-trip");
                }
            }

        private void register(int nType, Class<?> clz)
            {
            m_context.registerUserType(nType, clz, new PortableObjectSerializer(nType));
            }

        private SimplePofContext m_context;

        private Binary m_binary;
        }

    // ----- constants ------------------------------------------------------

    private static final int TYPE_ORDER = 1000;
    private static final int TYPE_CUSTOMER = 1001;
    private static final int TYPE_ADDRESS = 1002;
    private static final int TYPE_LINE_ITEM = 1003;
    private static final int TYPE_PRODUCT = 1004;
    private static final int TYPE_MONEY = 1005;
    private static final int TYPE_ATTRIBUTE = 1006;
    private static final int TYPE_AUDIT = 1007;
    }
