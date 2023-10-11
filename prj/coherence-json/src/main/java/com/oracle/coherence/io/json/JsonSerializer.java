/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.io.json.genson.GenericType;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;
import com.oracle.coherence.io.json.genson.JsonBindingException;
import com.oracle.coherence.io.json.genson.Modifier;

import com.oracle.coherence.io.json.genson.convert.NullConverterFactory;

import com.oracle.coherence.io.json.genson.datetime.JavaDateTimeBundle;

import com.oracle.coherence.io.json.genson.ext.GensonBundle;

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
import com.oracle.coherence.io.json.internal.MathContextConverter;
import com.oracle.coherence.io.json.internal.MissingClassConverter;
import com.oracle.coherence.io.json.internal.RoundingModeConverter;
import com.oracle.coherence.io.json.internal.SerializationGate;
import com.oracle.coherence.io.json.internal.SerializationSupportConverter;
import com.oracle.coherence.io.json.internal.ThrowableConverter;
import com.oracle.coherence.io.json.internal.VersionableSerializer;

import com.tangosol.coherence.config.Config;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.SerializationSupport;
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

import java.math.MathContext;
import java.math.RoundingMode;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Named;

/**
 * JSON Serializer using {@code Genson}.
 *
 * @since 20.06
 */
@Named(JsonSerializer.NAME)
@ApplicationScoped
public class JsonSerializer
        implements Serializer, ClassLoaderAware
    {
    // ----- constructors ---------------------------------------------------

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
     * @param fCompatibleMode  {@code true} if consuming JSON from an endpoint
     *                         that doesn't use our JSON serialization library.
     *
     * @see #JsonSerializer(ClassLoader, Modifier, boolean)
     */
    public JsonSerializer(boolean fCompatibleMode)
        {
        this(null, null, fCompatibleMode);
        }

    @Override
    public String getName()
        {
        return NAME;
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
     * @param loader            the {@link ClassLoader} to use during deserialization
     *                          operations
     * @param builderModifier   the {@link Modifier} that will be invoked to allow
     *                          customization of the serialization
     * @param fCompatibleMode   {@code true} if consuming JSON from an endpoint
     *                          that doesn't use our JSON serialization library.
     */
    public JsonSerializer(ClassLoader loader, Modifier<GensonBuilder> builderModifier, boolean fCompatibleMode)
        {

        this.f_fCompatibleMode = fCompatibleMode;

        GensonBuilder builder = new GensonBuilder()
                .withBundle(new BundleProxy("com.fasterxml.jackson.annotation.JacksonAnnotation",
                                            "com.oracle.coherence.io.json.genson.ext.jackson.JacksonBundle"))
                .withBundle(new JavaDateTimeBundle())
                .withBundle(new GensonServiceBundle())
                .setDefaultType(ValueType.OBJECT, JsonObject.class)
                .setDefaultType(ValueType.INTEGER, Number.class)
                .setDefaultType(ValueType.DOUBLE, Number.class)
                .useClassMetadata(true)
                .useClassMetadataWithStaticType(true)
                .useFields(true, VisibilityFilter.PRIVATE)
                .useMethods(false)
                .useConstructorWithArguments(true)
                .setSkipNull(true)
                .withSerializer(VersionableSerializer.INSTANCE, Versionable.class)
                .withConverter(InetAddressConverter.INSTANCE, InetAddress.class)
                .withConverter(InetAddressConverter.INSTANCE, Inet4Address.class)
                .withConverter(InetAddressConverter.INSTANCE, Inet6Address.class)
                .withConverter(InetSocketAddressConverter.INSTANCE, InetSocketAddress.class)
                .withConverter(JsonObjectConverter.INSTANCE, JsonObject.class)
                .withConverter(MathContextConverter.INSTANCE, MathContext.class)
                .withConverter(RoundingModeConverter.INSTANCE, RoundingMode.class);

        addSafeBundle(builder, JsonbBundle.class);
        addSafeBundle(builder, JSR353Bundle.class);

        try
            {
            builder.useUnknownPropertyHandler(new EvolvableHandler());
            }
        catch (Throwable t)
            {
            Logger.warn("Could not add JSON UnknownPropertyHandler " + EvolvableHandler.class + " " + t.getLocalizedMessage());
            }


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

    // ----- ClassLoaderAware interface -------------------------------------

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

    // ----- Serializer interface -------------------------------------------

    @Override
    public void serialize(WriteBuffer.BufferOutput bufferOutput, Object oValue) throws IOException
        {
        //noinspection rawtypes
        GenericType type = OBJECT_TYPE;
        if (oValue != null)
            {
            Class<?> clazz = oValue.getClass();

            if (!SerializationGate.isValid(clazz))
                {
                throw new JsonBindingException("Unable to serialize " + clazz.getName());
                }

            for (Class<?> c : JSON_TYPES)
                {
                if (c.isInstance(oValue))
                    {
                    type = GenericType.of(oValue.getClass());
                    break;
                    }
                }
            }

        if (DEBUG_MODE)
            {
            Class<?> cls = oValue == null ? null : oValue.getClass();
            String serialized = f_genson.serialize(oValue);
            Base.log("\n-------------------- JSON DEBUG: Serializing -------------------- \nType: "
                     + cls + "\nValue: " + oValue + "\nSerialized Result:\n" + serialized
                     + "\nSerialized Bytes:\n" + Base.toHexDump(serialized.getBytes(), 16)
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

        if (!SerializationGate.isValid(clazz))
            {
            throw new JsonBindingException("Unable to de-serialize " + clazz.getName());
            }

        WrapperDataInputStream stream = new WrapperDataInputStream(in);

        if (DEBUG_MODE)
            {
            byte[] abData = new byte[stream.available()];
            int nRead = stream.read(abData);

            if (nRead == -1)
                {
                throw new IOException("BufferInput.available() is non-zero, however, no bytes were read");
                }

            if (nRead != abData.length)
                {
                throw new IOException(String.format("Expected to %s bytes, but only %s were read", abData.length, nRead));
                }

            try
                {
                T oValue = f_genson.deserialize(new String(abData), clazz);

                Class<?> cls = oValue == null ? null : oValue.getClass();

                Base.log("\n-------------------- JSON DEBUG: Deserializing -------------------- \nBytes:\n"
                         + Base.toHexDump(abData, 16)
                         + "\nType: " + cls + "\nValue: " + oValue
                         + "\n---------------------------------------------------------------");

                return oValue;
                }
            catch (Throwable thrown)
                {
                Logger.err("JSON DEBUG: Failed to deserialize " + new String(abData), thrown);
                throw Base.ensureRuntimeException(thrown);
                }
            }
        else
            {
            return f_genson.deserialize(new InputStreamReader(stream), clazz);
            }
        }

    // ----- public methods -------------------------------------------------

    /**
     * Deserialize an object as an instance of the specified class by reading
     * its state using the specified json String.
     * <p>
     * <b>Note:</b> Classes that need to designate an alternative object to be
     * returned by the Serializer after an object is deserialized from the json
     * data should implement the {@link SerializationSupport#readResolve()} method.
     *
     * @param <T>    the class to deserialize to
     * @param sJson  the json to deserialize
     * @param clazz  the type of the object to deserialize
     *
     * @return the deserialized user type instance
     *
     * @throws IOException if an I/O error occurs
     *
     * @since 22.06
     */
    public <T> T deserialize(String sJson, Class<? extends T> clazz)
            throws IOException
        {
        return deserialize(sJson.getBytes(StandardCharsets.UTF_8), clazz);
        }

    /**
     * Deserialize an object as an instance of the specified class by reading
     * its state using the specified json byte array.
     * <p>
     * <b>Note:</b> Classes that need to designate an alternative object to be
     * returned by the Serializer after an object is deserialized from the json
     * data should implement the {@link SerializationSupport#readResolve()} method.
     *
     * @param <T>     the class to deserialize to
     * @param abJson  the json to deserialize
     * @param clazz   the type of the object to deserialize
     *
     * @return the deserialized user type instance
     *
     * @throws IOException if an I/O error occurs
     *
     * @since 22.06
     */
    public <T> T deserialize(byte[] abJson, Class<? extends T> clazz)
            throws IOException
        {
        ByteArrayReadBuffer buf = new ByteArrayReadBuffer(abJson);
        return deserialize(buf.getBufferInput(), clazz);
        }

    /**
     * Serialize a value to a {@link WriteBuffer}.
     *
     * @param o  the value to serialize
     *
     * @return a {@link WriteBuffer} containing the serialized value
     *
     * @throws IOException if an error occurs
     */
    public WriteBuffer serialize(Object o) throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(512);
        serialize(buf.getBufferOutput(), o);
        return buf;
        }

    /**
     * @return this {@code JsonSerializer}'s configured {@link Genson} instance.
     */
    public Genson underlying()
        {
        return f_genson;
        }

    // ----- helper methods -------------------------------------------------

    private static void addSafeBundle(GensonBuilder builder, Class<? extends GensonBundle> clz)
        {
        try
            {
            GensonBundle bundle = clz.getDeclaredConstructor().newInstance();
            builder.withBundle(bundle);
            }
        catch (Throwable t)
            {
            Logger.warn("Failed to add Genson bundle: " + clz + " - " + t.getLocalizedMessage());
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Helper method to allow creating a {@link JsonObject} from a {@link String}. Useful for
     * inserting JSON values from CohQL.
     * <p>
     * Example in CohQL: <code>insert into 'test' key(1) value json('{"id": 1, "name": "Tim"}')</code>
     *
     * @param sJson  JSON to parse
     *
     * @return  a new {@link JsonObject}
     * @since 22.06.7
     */
    public static Object fromJson(String sJson)
        {
        try
            {
            return SERIALIZER.deserialize(sJson, Object.class);
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    // ----- inner class: BundleProxy ---------------------------------------

    /**
     * Proxies a {@link GensonBundle} by checking if the required
     * classes are available prior to instantiation and invocation
     * of the actual bundle.
     *
     * @since 20.12
     */
    protected static final class BundleProxy
            extends GensonBundle
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code BundleProxy} that will attempt to load, instantiate
         * and invoke the {@link GensonBundle} specified by {@code sClzGensonBundle}
         * if the class specified by {@code sClzGuard} can be loaded.
         *
         * @param sClzGuard         the class that must be loadable before instantiating
         *                          and invoking the proxied {@link GensonBundle}
         * @param sClzGensonBundle  the {@link GensonBundle} class (as String) to instantiate
         *                          and invoke if {@code sClzGuard} can be loaded
         */
        private BundleProxy(String sClzGuard, String sClzGensonBundle)
            {
            f_sClzName        = sClzGuard;
            f_sClzGensonBundle = sClzGensonBundle;
            }

        // ----- methods from GensonBundle ----------------------------------

        @SuppressWarnings("unchecked")
        @Override
        public void configure(GensonBuilder builder)
            {
            String      sClzGensonBundle = f_sClzGensonBundle;
            ClassLoader loader           = builder.getClassLoader();
            try
                {
                Class.forName(f_sClzName, false, loader);
                try
                    {
                    Class<? extends GensonBundle> clzGensonBundle = (Class<? extends GensonBundle>)
                            Class.forName(sClzGensonBundle, true, loader);
                    GensonBundle bundle = clzGensonBundle.getDeclaredConstructor().newInstance();
                    bundle.configure(builder);
                    }
                catch (Exception e)
                    {
                    throw Base.ensureRuntimeException(e, String.format("Unexpected error loading bundle [%s]",
                            sClzGensonBundle));
                    }
                }
            catch (ClassNotFoundException ignored)
                {
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The type that must be loadable before the {@link GensonBundle}
         * may be instantiated and invoked.
         */
        private final String f_sClzName;

        /**
         * The {@link GensonBundle} (as String) to instantiate and invoke if the type
         * guard was successfully loaded.
         */
        private final String f_sClzGensonBundle;
        }

    // ----- inner class: Factory -------------------------------------------

    /**
     * The default {@link SerializerFactory} to create a JSON serializer.
     */
    public static class Factory
            implements SerializerFactory
        {
        @Override
        public Serializer createSerializer(ClassLoader loader)
            {
            return new JsonSerializer(loader);
            }

        @Override
        public String getName()
            {
            return JsonSerializer.NAME;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Serializer to be used by fromJson method.
     */
    private static final JsonSerializer SERIALIZER = new JsonSerializer();

    /**
     * The default name for the JSON serializer.
     */
    public static final String NAME = "json";

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
    public static final boolean DEBUG_MODE = Config.getBoolean(PROP_DEBUG_MODE);

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
