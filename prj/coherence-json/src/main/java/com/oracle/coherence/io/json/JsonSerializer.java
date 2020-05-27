/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.oracle.coherence.io.json.genson.GenericType;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;
import com.oracle.coherence.io.json.genson.Modifier;

import com.oracle.coherence.io.json.genson.convert.NullConverterFactory;

import com.oracle.coherence.io.json.genson.datetime.JavaDateTimeBundle;

import com.oracle.coherence.io.json.genson.ext.jackson.JacksonBundle;
import com.oracle.coherence.io.json.genson.ext.jsonb.JsonbBundle;
import com.oracle.coherence.io.json.genson.ext.jsr353.JSR353Bundle;

import com.oracle.coherence.io.json.genson.reflect.EvolvableHandler;
import com.oracle.coherence.io.json.genson.reflect.VisibilityFilter;

import com.oracle.coherence.io.json.genson.stream.ValueType;

import com.oracle.coherence.io.json.internal.ClassConverter;
import com.oracle.coherence.io.json.internal.ComparableConverter;
import com.oracle.coherence.io.json.internal.EnumConverter;
import com.oracle.coherence.io.json.internal.GensonServiceBundle;
import com.oracle.coherence.io.json.internal.InetAddressConverter;
import com.oracle.coherence.io.json.internal.InetSocketAddressConverter;
import com.oracle.coherence.io.json.internal.JsonObjectConverter;
import com.oracle.coherence.io.json.internal.MapConverter;
import com.oracle.coherence.io.json.internal.MissingClassConverter;
import com.oracle.coherence.io.json.internal.SerializationSupportConverter;
import com.oracle.coherence.io.json.internal.ThrowableConverter;
import com.oracle.coherence.io.json.internal.VersionableSerializer;

import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;
import com.tangosol.io.WrapperDataInputStream;
import com.tangosol.io.WrapperDataOutputStream;
import com.tangosol.io.WriteBuffer;

import com.tangosol.util.Base;
import com.tangosol.util.Versionable;

import java.io.IOException;
import java.io.InputStreamReader;

import java.lang.ref.WeakReference;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Named;

/**
 * JSON Serializer using {@code Genson}.
 *
 * @since 14.1.2
 */
@Named("json")
@ApplicationScoped
public class JsonSerializer
        implements Serializer, ClassLoaderAware
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor.
     */
    public JsonSerializer()
        {
        this(null, null, false);
        }

    /**
     * Create a {@link JsonSerializer}.
     *
     * @param compatibleMode  {@code true} if consuming JSON from an endpoint
     *                        that doesn't use our JSON serialization library.
     *
     * @see #JsonSerializer(ClassLoader, Modifier, boolean)
     */
    public JsonSerializer(boolean compatibleMode)
        {
        this(null, null, compatibleMode);
        }

    @Override
    public String getName()
        {
        return "json";
        }

    /**
     * Constructs a {@code GensonJsonSerializer} that will use the
     * provided {@code ClassLoader}.
     *
     * @param loader  the {@link ClassLoader} to use during deserialization operations
     */
    public JsonSerializer(ClassLoader loader)
        {
        this(loader, null, false);
        }

    /**
     * Constructs a {@code GensonJsonSerializer} that will use the
     * provided {@code ClassLoader}.
     *
     * @param loader           the {@link ClassLoader} to use during deserialization
     *                         operations
     * @param builderModifier  the {@link Modifier} that will be invoked to allow
     *                         customization of the serialization
     * @param compatibleMode   {@code true} if consuming JSON from an endpoint
     *                         that doesn't use our JSON serialization library.
     */
    public JsonSerializer(ClassLoader loader, Modifier<GensonBuilder> builderModifier, boolean compatibleMode)
        {

        this.f_fCompatibleMode = compatibleMode;

        GensonBuilder builder = new GensonBuilder()
                .withBundle(new JacksonBundle())
                .withBundle(new JsonbBundle())
                .withBundle(new JSR353Bundle())
                .withBundle(new JavaDateTimeBundle())
                .withBundle(new GensonServiceBundle())
                .setDefaultType(ValueType.OBJECT, JsonObject.class)
                .setDefaultType(ValueType.INTEGER, Number.class)
                .setDefaultType(ValueType.DOUBLE, Number.class)
                .useClassMetadata(true)
                .useClassMetadataWithStaticType(false)
                .useFields(true, VisibilityFilter.PRIVATE)
                .useMethods(false)
                .useConstructorWithArguments(true)
                .useUnknownPropertyHandler(new EvolvableHandler())
                .setSkipNull(true)
                .withSerializer(VersionableSerializer.INSTANCE, Versionable.class)
                .withConverter(InetAddressConverter.INSTANCE, InetAddress.class)
                .withConverter(InetAddressConverter.INSTANCE, Inet4Address.class)
                .withConverter(InetAddressConverter.INSTANCE, Inet6Address.class)
                .withConverter(InetSocketAddressConverter.INSTANCE, InetSocketAddress.class)
                .withConverter(JsonObjectConverter.INSTANCE, JsonObject.class);

        if (!this.f_fCompatibleMode)
            {
            builder.withConverterFactory(MapConverter.Factory.INSTANCE);
            }

        builder.withConverterFactory(ComparableConverter.Factory.INSTANCE)
                .withConverterFactory(ClassConverter.Factory.INSTANCE)
                .withConverterFactory(EnumConverter.Factory.INSTANCE)
                .withConverterFactory(ThrowableConverter.Factory.INSTANCE)
                .withConverterFactory(factory ->
                                              factory.find(NullConverterFactory.class)
                                                      .withNext(new SerializationSupportConverter.Factory(this))
                                                      .withNext(MissingClassConverter.Factory.INSTANCE));

        if (builderModifier != null)
            {
            builder = builderModifier.apply(builder);
            }

        f_genson = builder.create();

        setContextClassLoader(loader);
        }

    // ---- ClassLoaderAware interface --------------------------------------

    @Override
    public ClassLoader getContextClassLoader()
        {
        WeakReference<ClassLoader> refLoader = this.m_refLoader;
        return refLoader == null ? null : refLoader.get();
        }

    @Override
    public void setContextClassLoader(ClassLoader classLoader)
        {
        assert m_refLoader == null;
        if (classLoader != null)
            {
            m_refLoader = new WeakReference<>(classLoader);
            f_genson.setLoader(classLoader);
            }
        }

    // ---- Serializer interface --------------------------------------------

    @Override
    @SuppressWarnings("RedundantThrows")
    public void serialize(WriteBuffer.BufferOutput bufferOutput, Object oValue) throws IOException
        {
        GenericType type = OBJECT_TYPE;
        if (oValue != null)
            {
            for (Class<?> c : JSON_TYPES)
                {
                if (c.isInstance(oValue))
                    {
                    type = GenericType.of(oValue.getClass());
                    }
                }
            }

        if (DEBUG_MODE)
            {
            Class<?> cls = oValue == null ? null : oValue.getClass();
            Base.log("-------------------- JSON DEBUG: Serializing -------------------- \nType: "
                     + cls + "\nValue: " + oValue + "\n" + f_genson.serialize(oValue)
                     + "\n---------------------------------------------------------------");
            }

        f_genson.serialize(oValue, type, new WrapperDataOutputStream(bufferOutput));
        }

    @Override
    public <T> T deserialize(ReadBuffer.BufferInput in, Class<? extends T> clazz) throws IOException
        {
        if (in.available() == 0)
            {
            return null;
            }

        WrapperDataInputStream stream = new WrapperDataInputStream(in);

        if (DEBUG_MODE)
            {
            byte[] abData = new byte[stream.available()];
            stream.read(abData);

            try
                {
                T oValue = f_genson.deserialize(new String(abData), clazz);

                Class<?> cls = oValue == null ? null : oValue.getClass();

                Base.log("-------------------- JSON DEBUG: Deserializing -------------------- \nType: "
                         + cls + "\nValue: " + oValue + "\n" + f_genson.serialize(oValue)
                         + "\n---------------------------------------------------------------");

                return oValue;
                }
            catch (Throwable thrown)
                {
                Base.err("JSON DEBUG: Failed to deserialize " + new String(abData));
                throw Base.ensureRuntimeException(thrown);
                }
            }
        else
            {
            return f_genson.deserialize(new InputStreamReader(stream), clazz);
            }
        }

    // ---- public methods ---------------------------------------------------

    /**
     * @return this {@code JsonSerializer}'s configured {@link Genson} instance.
     */
    public Genson underlying()
        {
        return f_genson;
        }

    // ---- nested class: Factory --------------------------------------------

    /**
     * Factory for default serializer.
     */
    public static class Factory
            implements SerializerFactory
        {
        @Override
        public Serializer createSerializer(ClassLoader loader)
            {
            return new JsonSerializer(loader);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Generic type for {@link Object}.
     */
    private static final GenericType<Object> OBJECT_TYPE = GenericType.of(Object.class);

    /**
     * An array of core JSON data types.
     */
    private static final Class<?>[] JSON_TYPES = {String.class, Number.class, Boolean.class};

    /**
     * The System property to use to dump json being serialized or deserialized to the log.
     */
    public static final String PROP_DEBUG_MODE = "coherence.io.json.debug";

    /**
     * A flag indicating whether to dump json to the log.
     * <p>
     * This will impact performance and should only be used in testing.
     */
    public static final boolean DEBUG_MODE = Boolean.getBoolean(PROP_DEBUG_MODE);

    // ----- data members ---------------------------------------------------

    /**
     * {@link Genson} runtime for serialization/deserialization operations.
     */
    protected final Genson f_genson;

    /**
     * Flag indicating Maps should be serialized in a JSON compatible fashion.  This is useful
     * for the cases when JSON produced by this library needs to be consumed by another JSON parsing
     * library other than {@code Genson}.
     */
    protected final boolean f_fCompatibleMode;

    /**
     * The optional ClassLoader.
     */
    protected WeakReference<ClassLoader> m_refLoader;
    }
