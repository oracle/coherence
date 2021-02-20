/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Evolvable;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.SafeHashSet;

import javax.inject.Named;

import java.io.IOException;
import java.io.Serializable;

import java.util.Map;
import java.util.Set;


/**
* SafeConfigurablePofContext is an extension of ConfigurablePofContext that can
* serialize and deserialize any valid POF user type, even those that have not
* been explicitly configured, as well as any Java serializable types
* (Serializable, Externalizable, or ExternalizableLite).
* <p>
* <b>Important note:</b> this PofContext has the following limitations:
* <ul>
*   <li> SafeConfigurablePofContext is supported only for Java clients;
*   <li> its performance is less optimal than of the ConfigurablePofContext;
*   <li> the serialized form produced by the SafeConfigurablePofContext will
*        not be recognized by POF aware ValueExtractors.
* </ul>
*
* <p>
* For user types that have been explicitly configured, the
* SafeConfigurablePofContext behaves identical to the ConfigurablePofContext.
*
* @author jh  2007.05.03
* @since Coherence 3.3
*/
@Named("safe-pof")
public class SafeConfigurablePofContext
        extends ConfigurablePofContext
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    * <p>
    * Create a default SafeConfigurablePofContext that will load
    * configuration information from the locator specified in
    * {@link #DEFAULT_RESOURCE}.
    */
    public SafeConfigurablePofContext()
        {
        super();
        f_serializerJava = new JavaPofSerializer();
        }

    /**
    * Create a SafeConfigurablePofContext that will load configuration
    * information from the specified locator.
    *
    * @param sLocator  the locator that specifies the location of the
    *                  PofContext configuration file; the locator is either
    *                  a valid path or a URL
    */
    public SafeConfigurablePofContext(String sLocator)
        {
        super(sLocator);
        f_serializerJava = new JavaPofSerializer();
        }

    /**
     * Create a SafeConfigurablePofContext that serializes instances of
     * non-POF classes with specified serializer.
     *
     * @param serializer  cache config specified serializer to use for non-POF types
     * @param loader      context classloader to use with serializer
     */
    public SafeConfigurablePofContext(Serializer serializer, ClassLoader loader)
        {
        super();
        f_serializerJava = new ExternalSerializer(serializer, /* fWarn */ false);
        setContextClassLoader(loader);
        }

    /**
    * Create a SafeConfigurablePofContext that will use the passed
    * configuration information.
    *
    * @param xml  an XmlElement containing information in the format of a
    *             configuration file used by SafeConfigurablePofContext
    */
    public SafeConfigurablePofContext(XmlElement xml)
        {
        super(xml);
        f_serializerJava = new JavaPofSerializer();
        }

    // ----- Serializer interface -------------------------------------------

    @Override
    public String getName()
        {
        return "safe-pof";
        }


    // ----- PofContext interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public PofSerializer getPofSerializer(int nTypeId)
        {
        switch (nTypeId)
            {
            case TYPE_PORTABLE:
                return f_serializerPof;

            case TYPE_SERIALIZABLE:
                return f_serializerJava;

            default:
                return super.getPofSerializer(nTypeId);
            }
        }

    /**
    * {@inheritDoc}
    */
    public Class getClass(int nTypeId)
        {
        switch (nTypeId)
            {
            case TYPE_PORTABLE:
                // should never get here
                return PortableObject.class;

            case TYPE_SERIALIZABLE:
                // should never get here
                return Serializable.class;

            default:
                return super.getClass(nTypeId);
            }
        }

    /**
    * {@inheritDoc}
    */
    public int getUserTypeIdentifier(Class clz)
        {
        int nTypeId = getUserTypeIdentifierInternal(clz);
        if (nTypeId < 0)
            {
            if (isUserType(clz))
                {
                nTypeId = getGenericTypeId(clz);                    
                Map mapClzToId = getPofConfig().m_mapTypeIdByClass;
                mapClzToId.put(clz, Integer.valueOf(nTypeId));
                }
            else
                {
                throw new IllegalArgumentException("Unknown user type: " + clz);
                }
            }

        return nTypeId;
        }

    /**
    * {@inheritDoc}
    */
    public int getUserTypeIdentifier(String sClass)
        {
        int nTypeId = getUserTypeIdentifierInternal(sClass);
        if (nTypeId < 0)
            {
            if (isUserType(sClass))
                {
                nTypeId = getGenericTypeId(loadClass(sClass));
                Map mapNameToId = getPofConfig().m_mapTypeIdByClassName;
                mapNameToId.put(sClass, Integer.valueOf(nTypeId));
                }
            else
                {
                throw new IllegalArgumentException("Unknown user type: " + sClass);
                }
            }

        return nTypeId;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isUserType(Class clz)
        {
        boolean fUserType = super.isUserType(clz);
        if (!fUserType)
            {
            if (!PofHelper.isIntrinsicPofType(clz))
                {
                fUserType = PortableObject.class.isAssignableFrom(clz) ||
                        Serializable.class.isAssignableFrom(clz);
                }
            }

        return fUserType;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isUserType(String sClass)
        {
        boolean fUserType = super.isUserType(sClass);
        if (!fUserType)
            {
            try
                {
                Class clz = loadClass(sClass);
                if (!PofHelper.isIntrinsicPofType(clz))
                    {
                    fUserType = PortableObject.class.isAssignableFrom(clz) ||
                            Serializable.class.isAssignableFrom(clz);
                    }
                }
            catch (RuntimeException e)
                {
                }
            }

        return fUserType;
        }

    /**
    * For user types that are not registered in the POF configuration used
    * by this PofContext, determine if the user type can be serialized using
    * POF, otherwise determine if the user type can be serialized using the
    * traditional (pre-POF) Coherence Java Serialization format that supports
    * Java <tt>Serializable</tt> and <tt>ExternalizableLite</tt> objects.
    *
    * @param clz  a user type class that is not configured in this PofContext
    *
    * @return a special user type id that indicates that the user type is
    *         supported by "generic" POF serialization or traditional
    *         Java serialization embedded in a POF stream
    */
    protected int getGenericTypeId(Class clz)
        {
        if (PortableObject.class.isAssignableFrom(clz))
            {
            return TYPE_PORTABLE;
            }

        if (Serializable.class.isAssignableFrom(clz))
            {
            return TYPE_SERIALIZABLE;
            }

        throw new IllegalArgumentException("The \"" + clz.getName()
                + "\" class is unsupported by " + getClass().getName());
        }


    // ----- inner class: ExternalSerializer ---------------------------------

    /**
    * Serializer used for Serializable and ExternalizableLite objects.
    */
    public class ExternalSerializer
            extends ExternalizableHelper
            implements PofSerializer
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct {@link ExternalSerializer} with specified non-POF serializer.
         *
         * @param serializer  cache configured serializer
         * @param fWarn       emit a warning if POF not configured for type
         */
        public ExternalSerializer(Serializer serializer, boolean fWarn)
            {
            f_serializer = serializer;
            f_warn       = fWarn;
            }

        // ----- PofSerializer methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        public void serialize(PofWriter out, Object o)
                throws IOException
            {
            out.writeBinary(0, toBinary(o, f_serializer));
            out.writeRemainder(null);
            register(o);
            }

        /**
        * {@inheritDoc}
        */
        public Object deserialize(PofReader in)
                throws IOException
            {
            Object o = fromBinary(in.readBinary(0), f_serializer);
            in.registerIdentity(o);
            in.readRemainder();
            register(o);

            return o;
            }

        // ----- internal -----------------------------------------------

        /**
        * Register a class as having been encountered by the serializer.
        *
        * @param o  an object that is being serialized or has been
        *           deserialized
        */
        protected void register(Object o)
            {
            if (o != null)
                {
                String sClass = o.getClass().getName();
                if (m_setRegisteredClasses.add(sClass) && f_warn)
                    {
                    log("TODO: Add POF support for \"" + sClass + "\".");
                    }
                }
            }

        // ----- data members -------------------------------------------

        /**
        * Serializer used by this PofSerializer.
        */
        private final Serializer f_serializer;

        /**
        * Warn if missing POF type.
        */
        private final boolean    f_warn;

        /**
        * All classes that have been registered.
        */
        private final Set m_setRegisteredClasses = new SafeHashSet();
        }

    // ----- inner class: JavaPofSerializer ---------------------------------

    /**
     * Default Java serializer.
     */
    public class JavaPofSerializer extends ExternalSerializer
        {
        public JavaPofSerializer()
            {
            super(new DefaultSerializer(getContextClassLoader()), /*fWarn*/true);
            }
        }

    // ----- inner class: SafePofSerializer ---------------------------------

    /**
    * Serializer used for objects implementing the PortableObject interface.
    */
    public class SafePofSerializer
            extends ExternalizableHelper
            implements PofSerializer
        {
        // ----- PofSerializer methods ----------------------------------

        /**
        * {@inheritDoc}
        */
        public void serialize(PofWriter out, Object o)
                throws IOException
            {
            BinaryWriteBuffer buffer = new BinaryWriteBuffer(1024*8);
            PofBufferWriter   writer = new PofBufferWriter.UserTypeWriter(
                    buffer.getBufferOutput(), SafeConfigurablePofContext.this,
                    TYPE_PORTABLE, -1);

            // COH-5065: due to the complexity of maintaining references
            // in future data, we won't support them for Evolvable objects
            if (SafeConfigurablePofContext.this.isReferenceEnabled()
                    && !(o instanceof Evolvable))
                {
                writer.enableReference();
                }

            m_serializer.serialize(writer, o);

            String sClass = o.getClass().getName();
            out.writeString(0, sClass);
            out.writeBinary(1, buffer.toBinary());
            out.writeRemainder(null);

            register(sClass);
            }

        /**
        * {@inheritDoc}
        */
        public Object deserialize(PofReader in)
                throws IOException
            {
            String sClass = in.readString(0);
            Binary bin    = in.readBinary(1);
            in.readRemainder();

            ConfigurablePofContext ctx = SafeConfigurablePofContext.this;
            PortableObject po;
            try
                {
                po = (PortableObject) ctx.loadClass(sClass).newInstance();
                }
            catch (Throwable e)
                {
                throw ensureRuntimeException(e,
                        "Unable to instantiate PortableObject class: " + sClass);
                }

            ReadBuffer.BufferInput inInternal = bin.getBufferInput();
            int nType = inInternal.readPackedInt();
            if (nType != TYPE_PORTABLE)
                {
                throw new IOException("Invalid POF type: " + nType
                                      + " (" + TYPE_PORTABLE + " expected)");
                }

            int iVersion = inInternal.readPackedInt();

            PofReader reader = new PofBufferReader.UserTypeReader(
                inInternal, ctx, TYPE_PORTABLE, iVersion);

            m_serializer.initialize(po, reader);

            register(sClass);

            return po;
            }

        // ----- internal -----------------------------------------------

        /**
        * Register a class as having been encountered by the serializer.
        *
        * @param sClass  the name of a class that is being serialized or
        *                deserialized
        */
        protected void register(String sClass)
            {
            if (m_setRegisteredClasses.add(sClass))
                {
                log("TODO: Add the class \"" + sClass
                        + "\" to the POF configuration file.");
                }
            }

        // ----- data members -------------------------------------------

        /**
        * Serializer used by this PofSerializer.
        */
        private final PortableObjectSerializer m_serializer =
            new PortableObjectSerializer(TYPE_PORTABLE);

        /**
        * All classes that have been registered.
        */
        private final Set m_setRegisteredClasses = new SafeHashSet();
        }


    // ----- constants ------------------------------------------------------

    /**
    * The type identifier for objects that implement the PortableObject
    * interface.
    */
    public static final int TYPE_PORTABLE = Integer.MAX_VALUE - 1;

    /**
    * The type identifier for Java Serializable (including the
    * ExternalizableLite format) objects.
    */
    public static final int TYPE_SERIALIZABLE = Integer.MAX_VALUE;


    // ----- data members ---------------------------------------------------

    /**
    * Serializer used for Serializable objects.
    */
    private final PofSerializer f_serializerJava;

    /**
    * Serializer used for [not registered] objects implementing PortableObject
    * interface.
    */
    private final PofSerializer f_serializerPof = new SafePofSerializer();
    }
