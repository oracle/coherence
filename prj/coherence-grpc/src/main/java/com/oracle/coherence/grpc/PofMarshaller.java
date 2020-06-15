/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc;

import com.tangosol.coherence.config.Config;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofContext;

import io.grpc.MethodDescriptor;

import io.helidon.grpc.core.MarshallerSupplier;

import javax.inject.Named;

/**
 * gRPC Marshaller that uses POF for serialization.
 * <p/>
 * The default {@link MarshallerSupplier} implementation is named {@code pof},
 * and will delegate to the default POF serializer.
 * <p/>
 * If you want to use a non-default POF serializer, simply create a custom
 * implementation of a {@link MarshallerSupplier}, give it a name using
 * {@link Named @Named} annotation, and make it discoverable either by CDI or
 * a {@code ServiceLoader}.
 *
 * @author Aleks Seovic  2019.09.10
 * @since 20.06
 */
public class PofMarshaller<T>
        extends SerializerMarshaller<T>
    {
    /**
     * Construct {@code PofMarshaller} instance.
     *
     * @param sPofConfig  the name of POF configuration file to use
     * @param clazz       the class to marshal
     */
    public PofMarshaller(String sPofConfig, Class<? extends T> clazz)
        {
        this(new ConfigurablePofContext(sPofConfig), clazz);
        }

    /**
     * Construct {@code PofMarshaller} instance.
     *
     * @param pofContext the {@link PofContext} to use
     */
    public PofMarshaller(PofContext pofContext, Class<? extends T> clazz)
        {
        super(pofContext, clazz);
        }

    /**
     * A {@link MarshallerSupplier} implementation that supplies instance of
     * a default {@link PofMarshaller}.
     */
    @Named("pof")
    public static class Supplier
            implements MarshallerSupplier
        {
        public Supplier()
            {
            String sPofConfig = Config.getProperty("coherence.pof.config", "pof-config.xml");
            f_pofContext = new ConfigurablePofContext(sPofConfig);
            }

        @Override
        public <T> MethodDescriptor.Marshaller<T> get(Class<T> clazz)
            {
            return new PofMarshaller<T>(f_pofContext, clazz);
            }

        private final ConfigurablePofContext f_pofContext;
        }
    }
