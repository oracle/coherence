/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.io.Evolvable;

import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;

import com.tangosol.io.pof.reflect.Codec;
import com.tangosol.io.pof.reflect.internal.AnnotationVisitor;
import com.tangosol.io.pof.reflect.internal.ClassMetadataBuilder;
import com.tangosol.io.pof.reflect.internal.TypeMetadata;
import com.tangosol.io.pof.reflect.internal.TypeMetadata.AttributeMetadata;

import com.tangosol.util.Binary;

import java.io.IOException;

import java.util.Iterator;

/**
 * A {@link PofAnnotationSerializer} provides annotation based
 * de/serialization. This serializer must be instantiated with the intended
 * class which is eventually scanned for the presence of the following
 * annotations.
 * <ul>
 *      <li>{@link Portable}</li>
 *      <li>{@link PortableProperty}</li>
 * </ul>
 * <p>
 * This serializer supports classes iff they are annotated with the type
 * level annotation; {@link Portable}. This annotation is a marker annotation
 * with no children.
 * <p>
 * All fields annotated with {@link PortableProperty} are explicitly
 * deemed POF serializable with the option of specifying overrides to
 * provide explicit behaviour such as:
 * <ul>
 *      <li>explicit POF indexes</li>
 *      <li>Custom {@link Codec} &lt;T&gt; to
 *          specify concrete implementations / customizations</li>
 * </ul>
 * <p>
 * The {@link PortableProperty#value()} (POF index) can be omitted iff the
 * auto-indexing feature is enabled. This is enabled by instantiating this
 * class with the {@code fAutoIndex} constructor argument. This feature
 * determines the index based on any explicit indexes specified and the name
 * of the portable properties. Currently objects with multiple versions is
 * not supported. The following illustrates the auto index algorithm:
 * <table border=1>
 *   <caption>Examples on POF index</caption>
 *   <tr><td>Name</td><td>Explicit Index</td><td>Determined Index</td></tr>
 *   <tr><td>c</td><td>1</td><td>1</td>
 *   <tr><td>a</td><td></td><td>0</td>
 *   <tr><td>b</td><td></td><td>2</td>
 * </table>
 * <p>
 * <b>NOTE:</b> This implementation does support objects that implement
 * Evolvable
 *
 * @author hr
 *
 * @since 3.7.1
 *
 * @param <T>  the user type this PofAnnotationSerializer will (de)serialize
 *
 * @see Portable
 */
public class PofAnnotationSerializer<T>
        implements PofSerializer
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a PofAnnotationSerializer.
     *
     * @param nTypeId  the POF type id
     * @param clz      type this serializer is aware of
     */
    public PofAnnotationSerializer(int nTypeId, Class<T> clz)
        {
        this(nTypeId, clz, false);
        }

    /**
     * Constructs a PofAnnotationSerializer.
     *
     * @param nTypeId     the POF type id
     * @param clz         type this serializer is aware of
     * @param fAutoIndex  turns on the auto index feature
     */
    public PofAnnotationSerializer(int nTypeId, Class<T> clz, boolean fAutoIndex)
        {
        if (clz == null)
            {
            throw new IllegalArgumentException("PofAnnotationSerializer requires a class");
            }
        // fail-fast
        initialize(nTypeId, clz, fAutoIndex);
        }

    // ----- PofSerializer interface ----------------------------------------

    /**
     * {@inheritDoc}
     */
    public void serialize(PofWriter out, Object o) throws IOException
        {
        // set the version identifier
        boolean   fEvolvable = o instanceof Evolvable;
        Evolvable evolvable  = null;
        if (fEvolvable)
            {
            evolvable = (Evolvable) o;
            out.setVersionId(Math.max(evolvable.getDataVersion(),
                    evolvable.getImplVersion()));
            }

        for (Iterator<AttributeMetadata<T>> iter = m_tmd.getAttributes(); iter.hasNext(); )
            {
            TypeMetadata.AttributeMetadata<T> attr = iter.next();
            attr.getCodec().encode(out, attr.getIndex(), attr.get((T) o));
            }

        // write out any future properties
        Binary binRemainder = null;
        if (fEvolvable)
            {
            binRemainder = evolvable.getFutureData();
            }
        out.writeRemainder(binRemainder);
        }

    /**
     * {@inheritDoc}
     */
    public Object deserialize(PofReader in) throws IOException
        {
        TypeMetadata<T> tmd = m_tmd;
        T value = tmd.newInstance();

        // set the version identifier
        boolean   fEvolvable = value instanceof Evolvable;
        Evolvable evolvable  = null;
        if (fEvolvable)
            {
            evolvable = (Evolvable) value;
            evolvable.setDataVersion(in.getVersionId());
            }

        for (Iterator<TypeMetadata.AttributeMetadata<T>> iter = tmd.getAttributes(); iter.hasNext(); )
            {
            TypeMetadata.AttributeMetadata<T> attr = iter.next();
            attr.set(value, attr.getCodec().decode(in, attr.getIndex()));
            }

        // read any future properties
        Binary binRemainder = in.readRemainder();
        if (fEvolvable)
            {
            evolvable.setFutureData(binRemainder);
            }
        return value;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return the {@link TypeMetadata} instance that holds structural information
     * regarding the class this serializer (de)serializes.
     *
     * @return a TypeMetadata instance for the class this serializer (de)serializes
     */
    protected TypeMetadata<T> getTypeMetadata()
        {
        return m_tmd;
        }

    /**
     * Initialize this serializer with {@link TypeMetadata} pertaining to the
     * specified class.
     *
     * @param clz         class in question
     * @param nTypeId     POF type id that uniquely identifies this type
     * @param fAutoIndex  turns on the auto index feature
     *
     * @throws {@link IllegalArgumentException}  if annotation is not present
     *         on {@code clz}
     */
    private void initialize(int nTypeId, Class<T> clz, boolean fAutoIndex)
        {
        Portable portable = clz.getAnnotation(Portable.class);
        if (portable == null)
            {
            throw new IllegalArgumentException(String.format(
                    "Attempting to use %s for a class (%s) that has no %s annotation",
                    getClass().getSimpleName(),
                    clz.getName(),
                    Portable.class.getSimpleName()));
            }

        // via the builder create the type metadata
        final ClassMetadataBuilder<T> builder = new ClassMetadataBuilder<T>()
                .setTypeId(nTypeId);
        builder.accept(new AnnotationVisitor<T>(fAutoIndex), clz);
        m_tmd = builder.build();
        }

    // ----- data members ---------------------------------------------------

    /**
     * TypeMetadata representing type information for this serializer
     * instance
     */
    private TypeMetadata<T> m_tmd;
    }
