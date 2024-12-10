/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.generator;

import com.tangosol.io.pof.RawDate;
import com.tangosol.io.pof.RawDateTime;
import com.tangosol.io.pof.RawDayTimeInterval;
import com.tangosol.io.pof.RawQuad;
import com.tangosol.io.pof.RawTime;
import com.tangosol.io.pof.RawTimeInterval;
import com.tangosol.io.pof.RawYearMonthInterval;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.oracle.coherence.common.schema.ClassFileSchemaSource;
import com.oracle.coherence.common.schema.Property;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.SchemaBuilder;
import com.oracle.coherence.common.schema.util.AsmUtils;

import com.tangosol.io.pof.DateMode;
import com.tangosol.io.pof.EvolvableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.io.pof.schema.PofArray;
import com.tangosol.io.pof.schema.PofCollection;
import com.tangosol.io.pof.schema.PofDate;
import com.tangosol.io.pof.schema.PofMap;
import com.tangosol.io.pof.schema.PofProperty;
import com.tangosol.io.pof.schema.PofType;

import com.tangosol.io.pof.schema.annotation.PortableType;
import com.tangosol.io.pof.schema.annotation.internal.Instrumented;
import com.tangosol.io.pof.schema.annotation.internal.PofIndex;

import com.tangosol.util.Binary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.oracle.coherence.common.schema.ClassFileSchemaSource.Filters.hasAnnotation;

import static com.oracle.coherence.common.schema.util.AsmUtils.addAnnotation;
import static com.oracle.coherence.common.schema.util.AsmUtils.getAnnotation;
import static com.oracle.coherence.common.schema.util.AsmUtils.internalName;
import static com.oracle.coherence.common.schema.util.AsmUtils.javaName;

import static org.objectweb.asm.Opcodes.ACC_ENUM;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_TRANSIENT;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.F_SAME1;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;

/**
 * This class instruments classes annotated with {@link PortableType} to make
 * them implement {@link PortableObject} and {@link EvolvableObject}.
 *
 * @author as  2013.07.18
 * @author Gunnar Hillert  2024.08.15
 */
@SuppressWarnings({"unchecked", "WeakerAccess", "unused"})
public class PortableTypeGenerator
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Create a {@link PortableTypeGenerator}.
     *
     * @param schema  the {@link Schema} instance to use
     * @param in      the {@link InputStream} to use to read the class to instrument
     *
     * @throws IOException if there is an error
     */
    public PortableTypeGenerator(Schema schema, InputStream in)
            throws IOException
        {
        this(schema, in, false);
        }

    /**
     * Create a {@link PortableTypeGenerator}.
     *
     * @param schema  the {@link Schema} instance to use
     * @param in      the {@link InputStream} to use to read the class to instrument
     * @param fDebug  flag indicating whether to generate debug code
     *
     * @throws IOException if there is an error
     */
    public PortableTypeGenerator(Schema schema, InputStream in, boolean fDebug)
            throws IOException
        {
        this(schema, in, fDebug, new CoherenceLogger());
        }

    /**
     * Create a {@link PortableTypeGenerator}.
     *
     * @param schema  the {@link Schema} instance to use
     * @param in      the {@link InputStream} to use to read the class to instrument
     * @param fDebug  flag indicating whether to generate debug code
     * @param logger  the {@link Logger} to use
     *
     * @throws IOException if there is an error
     */
    public PortableTypeGenerator(Schema schema, InputStream in, boolean fDebug, Logger logger)
            throws IOException
        {
        this(schema, new ClassReader(in), fDebug, logger);
        }

    /**
     * Create a {@link PortableTypeGenerator}.
     *
     * @param schema   the {@link Schema} instance to use
     * @param bytes    the byte-code of the class to instrument
     * @param nOffset  the offset into the byte-code array of the start of the class
     * @param nLen     the length of the byte-code array
     * @param fDebug   flag indicating whether to generate debug code
     * @param logger   the {@link Logger} to use
     */
    public PortableTypeGenerator(Schema schema, byte[] bytes, int nOffset, int nLen, boolean fDebug, Logger logger)
        {
        this(schema, new ClassReader(bytes, nOffset, nLen), fDebug, logger);
        }

    /**
     * Create a {@link PortableTypeGenerator}.
     *
     * @param schema  the {@link Schema} instance to use
     * @param reader  the {@link ClassReader} containing the class to instrument
     * @param fDebug  flag indicating whether to generate debug code
     * @param logger  the {@link Logger} to use
     */
    public PortableTypeGenerator(Schema schema, ClassReader reader, boolean fDebug, Logger logger)
        {
        m_schema = schema;
        m_fDebug = fDebug;
        m_log    = logger;

        ClassNode cn = new ClassNode();

        reader.accept(cn, 0);

        m_classNode = cn;

        com.oracle.coherence.common.schema.Type type = m_schema.findTypeByJavaName(javaName(cn.name));
        if (type != null)
            {
            m_type = (PofType) type.getExtension(PofType.class);
            }
        }

    // ----- PortableTypeGenerator methods ----------------------------------

    /**
     * Instrument the class.
     *
     * @return  {@code true} if the class was instrumented.
     */
    public boolean instrumentClass()
        {
        String fullName = javaName(m_classNode.name);

        if (m_type != null && m_type.getId() != 0 && !isEnum() && !isInstrumented())
            {
            m_log.info("Instrumenting type " + fullName);

            ensureTypeId();
            populatePropertyMap();
            populateFieldMap();
            implementDeserializationConstructor();
            implementEvolvableObject();

            // mark as instrumented
            addAnnotation(m_classNode, new AnnotationNode(Type.getDescriptor(Instrumented.class)));

            return true;
            }
        else
            {
            String reason = m_type == null
                            ? "Type does not exist in the schema or PofType extension is not defined"
                            : isEnum()
                                ? "Type is an enumeration"
                                : "Type is already instrumented";
            m_log.debug("Skipping type " + fullName + ". " + reason);

            return false;
            }
        }

    /**
     * Obtain the byte code for the current class in {@link #m_classNode}.
     *
     * @return the byte code for the current class in {@link #m_classNode}
     */
    public byte[] getClassBytes()
        {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        m_classNode.accept(writer);
        return writer.toByteArray();
        }

    /**
     * Write the byte code for the current class in {@link #m_classNode}
     * to the specified {@link OutputStream}.
     *
     * @param  out  the {@link OutputStream} to write the byte code to
     *
     * @throws IOException  if an I/O error occurs
     */
    public void writeClass(OutputStream out)
            throws IOException
        {
        out.write(getClassBytes());
        }

    /**
     * Ensure that the instrumented class implements {@link EvolvableObject}.
     */
    private void implementEvolvableObject()
        {
        boolean fDelegateToSuper = isPofType(m_classNode.superName);
        m_classNode.interfaces.add("com/tangosol/io/pof/EvolvableObject");

        FieldNode evolvable = new FieldNode(ACC_PRIVATE | ACC_TRANSIENT, "__evolvable$" + m_type.getId(),
                                            "Lcom/tangosol/io/Evolvable;", null, null);
        addAnnotation(evolvable, JSONB_TRANSIENT);
        m_classNode.fields.add(evolvable);

        FieldNode holder = new FieldNode(ACC_PRIVATE | ACC_TRANSIENT, "__evolvableHolder$",
                                         "Lcom/tangosol/io/pof/EvolvableHolder;", null, null);
        addAnnotation(holder, JSONB_TRANSIENT);
        m_classNode.fields.add(holder);

        implementGetEvolvable(fDelegateToSuper, evolvable, holder);
        if (!fDelegateToSuper)
            {
            implementGetEvolvableHolder(holder);
            }

        implementWriteExternal();
        }

    /**
     * Ensure that the instrumented class has a deserialization constructor.
     */
    private void implementDeserializationConstructor()
        {
        MethodNode ctor = findMethod("<init>", "(Lcom/tangosol/io/pof/PofReader;)V");
        if (ctor == null)
            {
            ctor = new MethodNode(ACC_PUBLIC, "<init>", "(Lcom/tangosol/io/pof/PofReader;)V", null, new String[] {"java/io/IOException"});
            boolean fDelegateToSuper = isPofType(m_classNode.superName);

            ctor.visitCode();
            ctor.visitVarInsn(ALOAD, 0);
            if (fDelegateToSuper)
                {
                ctor.visitVarInsn(ALOAD, 1);
                ctor.visitMethodInsn(INVOKESPECIAL, m_classNode.superName, "<init>", "(Lcom/tangosol/io/pof/PofReader;)V", false);
                }
            else
                {
                MethodNode defaultCtor = findMethod("<init>", "()V");
                String owner = defaultCtor == null ? m_classNode.superName : m_classNode.name;
                ctor.visitMethodInsn(INVOKESPECIAL, owner, "<init>", "()V", false);
                }
            
            // create nested reader for this type
            ctor.visitVarInsn(ALOAD, 1);
            ctor.visitLdcInsn(m_type.getId());
            ctor.visitMethodInsn(INVOKEINTERFACE, "com/tangosol/io/pof/PofReader",
                               "createNestedPofReader", "(I)Lcom/tangosol/io/pof/PofReader;", true);
            ctor.visitVarInsn(ASTORE, 2);

            int cPofFields = 0;

            // create versioned reader for each version and read its properties
            for (int version : m_mapProperties.keySet())
                {
                ctor.visitVarInsn(ALOAD, 2);
                ctor.visitLdcInsn(version);
                ctor.visitMethodInsn(INVOKEINTERFACE, "com/tangosol/io/pof/PofReader",
                                   "version", "(I)Lcom/tangosol/io/pof/PofReader;", true);
                ctor.visitVarInsn(ASTORE, 3);

                SortedSet<PofProperty> properties = m_mapProperties.get(version);
                for (PofProperty property : properties)
                    {
                    FieldNode field = field(property);

                    if (field == null)
                        {
                        m_log.debug("Field for property " + property.getName() + " was not found");
                        continue;
                        }

                    if ((field.access & Opcodes.ACC_STATIC) != 0 || (field.access & Opcodes.ACC_TRANSIENT) != 0)
                        {
                        // skip static or transient fields
                        continue;
                        }

                    // set the POF index only after we know we are processing this field
                    int nPofIndex = cPofFields++;

                    Type type = Type.getType(field.desc);

                    if (isDebugEnabled())
                        {
                        ctor.visitLdcInsn(
                                "reading attribute " + nPofIndex +
                                " (" + property.getName() + ") from the POF stream");
                        ctor.visitMethodInsn(INVOKESTATIC,
                                           "com/tangosol/io/pof/generator/DebugLogger", "log",
                                           "(Ljava/lang/String;)V", false);
                        }
                    ctor.visitVarInsn(ALOAD, 0);
                    ctor.visitVarInsn(ALOAD, 3);
                    ctor.visitLdcInsn(nPofIndex);

                    ReadMethod readMethod = getReadMethod(property, type);
                    readMethod.createTemplate(ctor, property, type);
                    ctor.visitMethodInsn(INVOKEINTERFACE,
                                       "com/tangosol/io/pof/PofReader",
                                       readMethod.getName(),
                                       readMethod.getDescriptor(), true);
                    if (type.getSort() == Type.OBJECT || "readObjectArray".equals(readMethod.getName()))
                        {
                        ctor.visitTypeInsn(CHECKCAST, type.getInternalName());
                        }
                    ctor.visitFieldInsn(PUTFIELD, m_classNode.name, field.name, field.desc);
                    }
                }

            ctor.visitVarInsn(ALOAD, 0);
            ctor.visitVarInsn(ALOAD, 2);
            ctor.visitMethodInsn(INVOKEVIRTUAL, m_classNode.name,
                               "readEvolvable", "(Lcom/tangosol/io/pof/PofReader;)V", false);

            ctor.visitInsn(RETURN);
            ctor.visitMaxs(0, 0);
            ctor.visitEnd();

            m_classNode.methods.add(ctor);
            m_log.debug("Implemented deserialization constructor");
            }
        else if ((ctor.access & ACC_PUBLIC) == 0)
            {
            m_log.debug("Class " + m_classNode.name + " has a non-public deserialization constructor. Making it public.");
            ctor.access = ACC_PUBLIC;
            }
        }

    /**
     * Ensure that the instrumented class has a {@link EvolvableObject#getEvolvable(int)} method.
     *
     * @param fDelegateToSuper  whether to delegate to super class if necessary
     * @param evolvable         backing field for {@code Evolvable}
     * @param holder            backing field for {@code EvolvableHolder}
     */
    @SuppressWarnings("Duplicates")
    private void implementGetEvolvable(boolean fDelegateToSuper, FieldNode evolvable, FieldNode holder)
        {
        MethodNode mn = new MethodNode(ACC_PUBLIC, "getEvolvable",
                            "(I)Lcom/tangosol/io/Evolvable;", null, null);
        addAnnotation(mn, JSONB_TRANSIENT);

        mn.visitCode();
        mn.visitVarInsn(ALOAD, 0);
        mn.visitFieldInsn(GETFIELD, m_classNode.name, evolvable.name, evolvable.desc);
        Label l0 = new Label();
        mn.visitJumpInsn(IFNONNULL, l0);
        mn.visitVarInsn(ALOAD, 0);
        mn.visitTypeInsn(NEW, "com/tangosol/io/SimpleEvolvable");
        mn.visitInsn(DUP);
        mn.visitLdcInsn(m_type.getVersion());
        mn.visitMethodInsn(INVOKESPECIAL, "com/tangosol/io/SimpleEvolvable",
                           "<init>", "(I)V", false);
        mn.visitFieldInsn(PUTFIELD, m_classNode.name, evolvable.name, evolvable.desc);
        mn.visitLabel(l0);
        mn.visitFrame(F_SAME, 0, null, 0, null);
        mn.visitVarInsn(ILOAD, 1);
        mn.visitLdcInsn(m_type.getId());
        Label l1 = new Label();
        mn.visitJumpInsn(IF_ICMPEQ, l1);
        mn.visitVarInsn(ALOAD, 0);
        mn.visitMethodInsn(INVOKEVIRTUAL, m_classNode.name, "getEvolvableHolder",
                               "()Lcom/tangosol/io/pof/EvolvableHolder;", false);
        mn.visitMethodInsn(INVOKEVIRTUAL, "com/tangosol/io/pof/EvolvableHolder", "isEmpty", "()Z", false);
        Label l2 = new Label();
        mn.visitJumpInsn(IFEQ, l2);
        mn.visitLabel(l1);
        mn.visitFrame(F_SAME, 0, null, 0, null);
        mn.visitVarInsn(ALOAD, 0);
        mn.visitFieldInsn(GETFIELD, m_classNode.name, evolvable.name, evolvable.desc);
        Label l3 = new Label();
        mn.visitJumpInsn(GOTO, l3);
        mn.visitLabel(l2);
        mn.visitFrame(F_SAME, 0, null, 0, null);

        if (fDelegateToSuper)
            {
            mn.visitVarInsn(ALOAD, 0);
            mn.visitVarInsn(ILOAD, 1);
            mn.visitMethodInsn(INVOKESPECIAL, m_classNode.superName, "getEvolvable",
                               "(I)Lcom/tangosol/io/Evolvable;", false);
            }
        else
            {
            mn.visitVarInsn(ALOAD, 0);
            mn.visitMethodInsn(INVOKEVIRTUAL, m_classNode.name, "getEvolvableHolder",
                               "()Lcom/tangosol/io/pof/EvolvableHolder;", false);
            mn.visitVarInsn(ILOAD, 1);
            mn.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
                               "(I)Ljava/lang/Integer;", false);
            mn.visitMethodInsn(INVOKEVIRTUAL,
                               "com/tangosol/io/pof/EvolvableHolder",
                               "get",
                               "(Ljava/lang/Integer;)Lcom/tangosol/io/Evolvable;", false);
            }
        mn.visitLabel(l3);
        mn.visitFrame(F_SAME1, 0, null, 1, new Object[] {"com/tangosol/io/Evolvable"});
        mn.visitInsn(ARETURN);
        mn.visitMaxs(0, 0);
        mn.visitEnd();

        if (!hasMethod(mn))
            {
            m_classNode.methods.add(mn);
            }
        m_log.debug("Implemented method: " + mn.name);
        }

    /**
     * Ensure that the instrumented class has a {@link EvolvableObject#getEvolvableHolder()} method.
     *
     * @param holder  backing field for {@code EvolvableHolder}
     */
    @SuppressWarnings("Duplicates")
    private void implementGetEvolvableHolder(FieldNode holder)
        {
        MethodNode mn = new MethodNode(ACC_PUBLIC, "getEvolvableHolder",
                            "()Lcom/tangosol/io/pof/EvolvableHolder;", null,
                            null);
        addAnnotation(mn, JSONB_TRANSIENT);

        mn.visitCode();
        mn.visitVarInsn(ALOAD, 0);
        mn.visitFieldInsn(GETFIELD, m_classNode.name, holder.name, holder.desc);
        Label l0 = new Label();
        mn.visitJumpInsn(IFNONNULL, l0);
        mn.visitVarInsn(ALOAD, 0);
        mn.visitTypeInsn(NEW, "com/tangosol/io/pof/EvolvableHolder");
        mn.visitInsn(DUP);
        mn.visitMethodInsn(INVOKESPECIAL, "com/tangosol/io/pof/EvolvableHolder",
                           "<init>", "()V", false);
        mn.visitFieldInsn(PUTFIELD, m_classNode.name, holder.name, holder.desc);
        mn.visitLabel(l0);
        mn.visitFrame(F_SAME, 0, null, 0, null);
        mn.visitVarInsn(ALOAD, 0);
        mn.visitFieldInsn(GETFIELD, m_classNode.name, holder.name, holder.desc);
        mn.visitInsn(ARETURN);
        mn.visitMaxs(0, 0);
        mn.visitEnd();

        if (!hasMethod(mn))
            {
            m_classNode.methods.add(mn);
            }
        m_log.debug("Implemented method: " + mn.name);
        }

    /**
     * Ensure that the instrumented class has a {@link PortableObject#writeExternal(PofWriter)} method.
     */
    @SuppressWarnings("Duplicates")
    private void implementWriteExternal()
        {
        MethodNode mn = new MethodNode(ACC_PUBLIC, "writeExternal",
                                       "(Lcom/tangosol/io/pof/PofWriter;)V",
                                       null,
                                       new String[] {"java/io/IOException"});
        mn.visitCode();

        Label l0 = new Label();
        Label l1 = new Label();

        boolean fDelegateToSuper = isPofType(m_classNode.superName);

        mn.visitVarInsn(ALOAD, 1);
        mn.visitMethodInsn(INVOKEINTERFACE, "com/tangosol/io/pof/PofWriter",
                           "getUserTypeId", "()I", true);

        mn.visitLdcInsn(m_type.getId());
        mn.visitJumpInsn(IF_ICMPNE, l0);

        int cPofFields = 0;

        for (int version : m_mapProperties.keySet())
            {
            SortedSet<PofProperty> properties = m_mapProperties.get(version);
            for (PofProperty property : properties)
                {
                FieldNode field = field(property);

                if ((field.access & Opcodes.ACC_STATIC) != 0 || (field.access & Opcodes.ACC_TRANSIENT) != 0)
                    {
                    // skip static or transient fields
                    continue;
                    }

                // set the POF index only after we know we are processing this field
                int nPofIndex = cPofFields++;
                addPofIndex(field, nPofIndex);

                Type type = Type.getType(field.desc);

                if (isDebugEnabled())
                    {
                    mn.visitLdcInsn("writing attribute " + nPofIndex +
                                    " (" + property.getName() + ") to POF stream");
                    mn.visitMethodInsn(INVOKESTATIC,
                                       "com/tangosol/io/pof/generator/DebugLogger", "log",
                                       "(Ljava/lang/String;)V", false);
                    }
                mn.visitVarInsn(ALOAD, 1);
                mn.visitLdcInsn(nPofIndex);
                mn.visitVarInsn(ALOAD, 0);
                mn.visitFieldInsn(GETFIELD, m_classNode.name, field.name, field.desc);

                // push raw encoding flag on stack if the array type supports it
                if (isRawEncodingSupported(type))
                    {
                    if (property.isArray())
                        {
                        PofArray pofArray = property.asArray();
                        mn.visitLdcInsn(pofArray.isUseRawEncoding());
                        }
                    else
                        {
                        mn.visitLdcInsn(false);
                        }
                    }
                
                WriteMethod writeMethod = getWriteMethod(property, type);
                writeMethod.pushUniformTypes(mn);
                mn.visitMethodInsn(INVOKEINTERFACE,
                                   "com/tangosol/io/pof/PofWriter",
                                   writeMethod.getName(),
                                   writeMethod.getDescriptor(), true);
                }
            }

        if (fDelegateToSuper)
            {
            mn.visitJumpInsn(GOTO, l1);
            }
        mn.visitLabel(l0);
        mn.visitFrame(F_SAME, 0, null, 0, null);

        if (fDelegateToSuper)
            {
            mn.visitVarInsn(ALOAD, 0);
            mn.visitVarInsn(ALOAD, 1);
            mn.visitMethodInsn(INVOKESPECIAL, m_classNode.superName, "writeExternal",
                               "(Lcom/tangosol/io/pof/PofWriter;)V", false);
            mn.visitLabel(l1);
            mn.visitFrame(F_SAME, 0, null, 0, null);
            }

        mn.visitInsn(RETURN);
        mn.visitMaxs(0, 0);
        mn.visitEnd();

        if (!hasMethod(mn))
            {
            m_classNode.methods.add(mn);
            }
        m_log.debug("Implemented method: " + mn.name);
        }

    /**
     * Return {@code true} if the specified type represents an array type that
     * supports raw encoding.
     *
     * @param type  the array type to check
     *
     * @return {@code true} if the specified type represents an array type that
     *         supports raw encoding
     */
    private boolean isRawEncodingSupported(Type type)
        {
        return type.getSort() == Type.ARRAY &&
               RAW_ENCODING_TYPES.contains(type.getElementType().getSort());
        }

    // ---- static entry points ---------------------------------------------

    /**
     * Instrument the classes in the specified directory.
     *
     * @param classDir  the directory containing the classes to instrument
     * @param schema    the {@link Schema} to use
     *
     * @throws IOException  if an error occurs
     */
    public static void instrumentClasses(File classDir, Schema schema)
            throws IOException
        {
        instrumentClasses(classDir, schema, false, new ConsoleLogger());
        }

    /**
     * Instrument the classes in the specified directory.
     *
     * @param classDir  the directory containing the classes to instrument
     * @param schema    the {@link Schema} to use
     * @param fDebug    {@code true} to include debug instrumentation
     * @param logger    the {@link Logger} to use
     *
     * @throws IOException  if an error occurs
     */
    public static void instrumentClasses(File classDir, Schema schema, boolean fDebug, Logger logger)
            throws IOException
        {
        if (!classDir.exists())
            {
            throw new IllegalArgumentException(
                    "Specified path [" + classDir.getAbsolutePath()
                    + "] does not exist");
            }
        if (!classDir.isDirectory())
            {
            throw new IllegalArgumentException(
                    "Specified path [" + classDir.getAbsolutePath()
                    + "] is not a directory");
            }

        File[] files = classDir.listFiles();
        if (files != null)
            {
            for (File file : files)
                {
                if (file.isDirectory())
                    {
                    instrumentClasses(file, schema, fDebug, logger);
                    }
                else if (file.getName().endsWith(".class"))
                    {
                    PortableTypeGenerator gen;

                    try (FileInputStream in = new FileInputStream(file))
                        {
                        gen = new PortableTypeGenerator(schema, in, fDebug, logger);
                        }

                    if (gen.instrumentClass())
                        {
                        try (FileOutputStream out = new FileOutputStream(file))
                            {
                            gen.writeClass(out);
                            out.flush();
                            }
                         }
                    }
                }
            }
        }

    /**
     * Instrument a class to add POF functionality.
     *
     * @param fileClass   the location of the class file
     * @param abByteCode  the class byte-code to instrument
     * @param nOffset     the offset into the byte array of the start of the byte code
     * @param nLen        the length of the byte code
     * @param properties  the {@link Properties} controlling code generation
     *
     * @return  the instrumented byte code or null if the class was not instrumented
     */
    // NOTE: This method is called via reflection from the IdeaPofGenerator class
    // so any changes to the method signature must be changed there too
    public static byte[] instrumentClass(File       fileClass,
                                         byte[]     abByteCode,
                                         int        nOffset,
                                         int        nLen,
                                         Properties properties)
        {
        return instrumentClass(fileClass, abByteCode, nOffset, nLen, properties, Collections.emptyMap());
        }

    /**
     * Instrument a class to add POF functionality.
     *
     * @param fileClass   the location of the class file
     * @param abByteCode  the class byte-code to instrument
     * @param nOffset     the offset into the byte array of the start of the byte code
     * @param nLen        the length of the byte code
     * @param properties  the {@link Properties} controlling code generation
     * @param env         extra parameters for code generation
     *
     * @return  the instrumented byte code or null if the class was not instrumented
     */
    // NOTE: This method is called via reflection from the IdeaPofGenerator class
    // so any changes to the method signature must be changed there too
    public static byte[] instrumentClass(File           fileClass,
                                         byte[]         abByteCode,
                                         int            nOffset,
                                         int            nLen,
                                         Properties     properties,
                                         Map<String, ?> env)
        {
        Schema schema = (Schema) env.get("schema");

        if (schema == null)
            {
            schema = createSchema(fileClass, env);
            }

        boolean fDebug = Boolean.parseBoolean(properties.getProperty("debug", "false"));

        PortableTypeGenerator generator = new PortableTypeGenerator(schema,
                                                                    abByteCode,
                                                                    nOffset,
                                                                    nLen,
                                                                    fDebug,
                                                                    new NullLogger());

        if (generator.instrumentClass())
            {
            return generator.getClassBytes();
            }

        return null;
        }

    /**
     * Create schema for a single class file.
     *
     * @param fileClass  class file to create schema for
     * @param env        environment variables
     *
     * @return a generated schema
     */
    // NOTE: This method is called via reflection from the IdeaPofGenerator class
    // so any changes to the method signature must be changed there too
    public static Schema createSchema(File fileClass, Map<String, ?> env)
        {
        ClassFileSchemaSource schemaSource = new ClassFileSchemaSource()
                .withClassFile(fileClass)
                .withTypeFilter(hasAnnotation(PortableType.class))
                .withMissingPropertiesAsObject();

        List<File> listLibs = (List<File>) env.get("libs");

        if (listLibs != null)
            {
            listLibs.stream()
                    .filter(f -> f.isFile() && f.getName().endsWith(".jar"))
                    .forEach(schemaSource::withClassesFromJarFile);

            listLibs.stream()
                    .filter(File::isDirectory)
                    .forEach(schemaSource::withClassesFromDirectory);
            }

        return new SchemaBuilder()
                .addSchemaSource(schemaSource)
                .build();
    }

    // ---- command line utility implementation -----------------------------------------------------------------------

    /**
     * Main method which allows users to run PortableTypeGenerator from
     * the command line.
     *
     * @param args  the name of the class directory to process
     *
     * @throws Exception if an error occurs
     */
    public static void main(String[] args)
            throws Exception
        {
        if (args.length < 1)
            {
            System.out.println("Usage: PortableTypeGenerator <classDir> [-debug]");
            System.exit(0);
            }

        File classDir = new File(args[0]);
        boolean fDebug = args.length >= 2 && args[1].equals("-debug");

        Schema schema = new SchemaBuilder()
                .addSchemaSource(new ClassFileSchemaSource().withClassesFromDirectory(classDir))
                .build();
        instrumentClasses(classDir, schema, fDebug, new ConsoleLogger());
        }

    // ----- inner class: ReadMethod ----------------------------------------

    /**
     * A {@link Method} implementation representing
     * a read method from {@link PofReader}.
     */
    private static class ReadMethod
            extends Method
        {
        /**
         * Create a new {@link ReadMethod}.
         *
         * @param name  the method's name.
         * @param desc  the method's descriptor.
         */
        ReadMethod(String name, String desc)
            {
            super(name, desc);
            }

        /**
         * Create template instance to read values from the POF stream into.
         *
         * @param mn        read method to create template instance in
         * @param property  POF property to create template for
         * @param type      property type
         */
        public void createTemplate(MethodNode mn, PofProperty property, Type type)
            {
            }
        }

    // ----- inner class: WriteMethod ---------------------------------------

    /**
     * A {@link Method} implementation representing
     * a write method from {@link PofWriter}.
     */
    private static class WriteMethod
            extends Method
        {
        /**
         * Create a new {@link WriteMethod}.
         *
         * @param name  the method's name.
         * @param desc  the method's descriptor.
         */
        WriteMethod(String name, String desc)
            {
            super(name, desc);
            }

        /**
         * Push the type of uniform collection/array elements or map keys
         * and values when using uniform POF encoding.
         *
         * @param mn  write method to add uniform type(s) to
         */
        public void pushUniformTypes(MethodNode mn)
            {
            }
        }

    // ----- inner class: CollectionReadMethod ------------------------------

    /**
     * A {@link ReadMethod} implementation for reading a collection.
     */
    private class CollectionReadMethod
            extends ReadMethod
        {
        CollectionReadMethod()
            {
            this("readCollection",
                 "(ILjava/util/Collection;)Ljava/util/Collection;");
            }

        CollectionReadMethod(String name, String desc)
            {
            super(name, desc);
            }

        @Override
        public void createTemplate(MethodNode mn, PofProperty property, Type type)
            {
            Type clazz = getCollectionType(property);
            if (clazz.equals(Type.getType(Object.class)))
                {
                clazz = getDefaultClass(property, type);
                }
            mn.visitLdcInsn(clazz);
            mn.visitMethodInsn(INVOKESTATIC,
                               Type.getInternalName(AsmUtils.class),
                               "createInstance",
                               "(Ljava/lang/Class;)Ljava/lang/Object;",
                               false);
            }

        protected Type getCollectionType(PofProperty property)
            {
            PofCollection col = property.asCollection();
            return type(col.getCollectionClass());
            }

        protected Type getDefaultClass(PofProperty property, Type type)
            {
            if (property.isSet())
                {
                return Type.getType(HashSet.class);
                }
            if (property.isList())
                {
                return Type.getType(ArrayList.class);
                }
            if (property.isMap())
                {
                return Type.getType(HashMap.class);
                }
            throw new IllegalStateException(
                    "Property " + m_classNode.name + "." + property.getName()
                    + " must have explicitly defined class or factory");
            }
        }

    // ----- inner class: CollectionWriteMethod -----------------------------

    /**
     * A {@link WriteMethod} implementation for writing a collection.
     */
    private static class CollectionWriteMethod
            extends WriteMethod
        {
        private Type m_elementClass;

        CollectionWriteMethod(Type elementClass)
            {
            this("writeCollection", createDescriptor(elementClass), elementClass);
            }

        CollectionWriteMethod(String name, String desc, Type elementClass)
            {
            super(name, desc);
            m_elementClass = elementClass;
            }

        private static String createDescriptor(Type elementClass)
            {
            String desc = "(ILjava/util/Collection;";
            if (isUniform(elementClass))
                {
                desc += "Ljava/lang/Class;";
                }
            return desc + ")V";
            }

        static boolean isUniform(Type elementClass)
            {
            return !OBJECT_TYPE.equals(elementClass);
            }

        @Override
        public void pushUniformTypes(MethodNode mn)
            {
            if (isUniform(m_elementClass))
                {
                mn.visitLdcInsn(m_elementClass);
                }
            }
        }

    // ----- inner class: MapReadMethod -------------------------------------

    /**
     * A {@link ReadMethod} implementation for reading a map.
     */
    private class MapReadMethod
            extends CollectionReadMethod
        {
        MapReadMethod()
            {
            super("readMap", "(ILjava/util/Map;)Ljava/util/Map;");
            }

        protected Type getCollectionType(PofProperty property)
            {
            PofMap map = property.asMap();
            return type(map.getMapClass());
            }
        }

    // ----- inner class: MapWriteMethod ------------------------------------

    /**
     * A {@link WriteMethod} implementation for writing a map.
     */
    private static class MapWriteMethod
            extends WriteMethod
        {
        private Type m_keyClass;
        private Type m_valueClass;

        MapWriteMethod(Type keyClass, Type valueClass)
            {
            super("writeMap", createDescriptor(keyClass, valueClass));
            m_keyClass = keyClass;
            m_valueClass = valueClass;
            }

        private static String createDescriptor(Type keyClass, Type valueClass)
            {
            String desc = "(ILjava/util/Map;";
            if (isUniform(keyClass))
                {
                desc += "Ljava/lang/Class;";
                if (isUniform(valueClass))
                    {
                    desc += "Ljava/lang/Class;";
                    }
                }
            return desc + ")V";
            }

        static boolean isUniform(Type elementClass)
            {
            return !OBJECT_TYPE.equals(elementClass);
            }

        @Override
        public void pushUniformTypes(MethodNode mn)
            {
            if (isUniform(m_keyClass))
                {
                mn.visitLdcInsn(m_keyClass);
                if (isUniform(m_valueClass))
                    {
                    mn.visitLdcInsn(m_valueClass);
                    }
                }
            }
        }

    // ----- inner class: ObjectArrayReadMethod -----------------------------

    /**
     * A {@link ReadMethod} implementation for reading an object array.
     */
    private static class ObjectArrayReadMethod
            extends ReadMethod
        {
        ObjectArrayReadMethod()
            {
            super("readObjectArray", "(I[Ljava/lang/Object;)[Ljava/lang/Object;");
            }

        @Override
        public void createTemplate(MethodNode mn, PofProperty field, Type type)
            {
            mn.visitInsn(ICONST_0);
            mn.visitTypeInsn(ANEWARRAY, type.getElementType().getInternalName());
            }
        }

    // ----- inner class: ObjectArrayWriteMethod ----------------------------

    /**
     * A {@link WriteMethod} implementation for writing an object array.
     */
    private static class ObjectArrayWriteMethod
            extends CollectionWriteMethod
        {
        ObjectArrayWriteMethod(Type elementClass)
            {
            super("writeObjectArray", createDescriptor(elementClass), elementClass);
            }

        private static String createDescriptor(Type elementClass)
            {
            String desc = "(I[Ljava/lang/Object;";
            if (isUniform(elementClass))
                {
                desc += "Ljava/lang/Class;";
                }
            return desc + ")V";
            }
        }

    // ----- inner class: ConsoleLogger -------------------------------------

    /**
     * A logger used by the {@link PortableTypeGenerator}.
     */
    public interface Logger
        {
        /**
         * Write a log message at debug level.
         *
         * @param message  the message to write
         */
        void debug(String message);

        /**
         * Write a log message at info level.
         *
         * @param message  the message to write
         */
        void info(String message);
        }

    // ----- inner class: NullLogger ----------------------------------------

    /**
     * A {@link Logger} implementation that does not log anything.
     */
    public static class NullLogger
            implements Logger
        {
        @Override
        public void debug(String message)
            {
            }

        @Override
        public void info(String message)
            {
            }
        }

    // ----- inner class: CoherenceLogger -------------------------------------

    /**
     * A {@link Logger} implementation that logs using the Coherence
     * {@link com.oracle.coherence.common.base.Logger}.
     */
    public static class CoherenceLogger
                implements Logger
        {
        @Override
        public void debug(String message)
            {
            com.oracle.coherence.common.base.Logger.finer(message);
            }

        @Override
        public void info(String message)
            {
            com.oracle.coherence.common.base.Logger.info(message);
            }
        }

    // ----- inner class: ConsoleLogger -------------------------------------

    /**
     * A {@link Logger} implementation that logs to the console.
     */
    public static class ConsoleLogger
            implements Logger
        {
        @Override
        public void debug(String message)
            {
            System.out.println("[DEBUG] " + message);
            }

        @Override
        public void info(String message)
            {
            System.out.println("[INFO] " + message);
            }
        }

    // ----- inner class: JavaLogger ----------------------------------------

    /**
     * A {@link Logger} implementation that logs to the Java logger.
     */
    public static class JavaLogger
            implements Logger
        {
        @Override
        public void debug(String message)
            {
            LOG.fine(message);
            }

        @Override
        public void info(String message)
            {
            LOG.info(message);
            }

        // ---- constants ---------------------------------------------------

        /**
         * The logger object to use.
         */
        private static final java.util.logging.Logger LOG =
                java.util.logging.Logger.getLogger(PortableTypeGenerator.class.getName());
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Add calculated type ID to @PortableType annotation if not present.
     */
    private void ensureTypeId()
        {
        AnnotationNode pt = getAnnotation(m_classNode, PortableType.class);
        if (pt.values == null)
            {
            pt.values = new ArrayList<>();
            }
        int nIdxId = pt.values.indexOf("id");
        if (nIdxId == -1)
            {
            pt.values.add("id");
            pt.values.add(m_type.getId());
            }
        }

    /**
     * Populate map of fields, keyed by name.
     */
    private void populateFieldMap()
        {
        List<FieldNode> fields = m_classNode.fields;
        m_mapFields = new HashMap<>(fields.size());
        for (FieldNode fn : fields)
            {
            m_mapFields.put(fn.name, fn);
            }
        }

    /**
     * Populate property map, sorting them in the process first by class version
     * and then by explicit or implicit property order.
     */
    private void populatePropertyMap()
        {
        int count = 0;
        for (PofProperty p : m_type.getProperties())
            {
            addProperty(p.getSince(), p);
            count++;
            }
        m_log.debug("Found " + count + " properties across " + m_mapProperties.size() + " class version(s)");
        }

    /**
     * Add single property to the property map.
     *
     * @param version   class version property was added in
     * @param property  property to add
     */
    private void addProperty(int version, PofProperty property)
        {
        SortedSet<PofProperty> properties = m_mapProperties.computeIfAbsent(version, k -> new TreeSet<>());
        properties.add(property);
        }

    /**
     * Return true if the specified class is a {@code PofType}.
     *
     * @param sInternalName  internal name of the class to check
     *
     * @return true if the specified class is a {@code PofType}
     */
    private boolean isPofType(String sInternalName)
        {
        com.oracle.coherence.common.schema.Type type = m_schema.findTypeByJavaName(javaName(sInternalName));
        PofType pofType = type == null ? null : (PofType) type.getExtension(PofType.class);
        return pofType != null && pofType.getId() > 0;
        }

    /**
     * Return field node for the specified name.
     *
     * @param name  field name
     *
     * @return field with the specified name; {@code null} if not present
     */
    private FieldNode field(String name)
        {
        return m_mapFields.get(name);
        }

    /**
     * Return backing field node for the specified property.
     *
     * @param property  property to get the backing field for
     *
     * @return backing field for the specified property; {@code null} if not present
     */
    private FieldNode field(Property property)
        {
        return field(property.getName());
        }

    /**
     * Return Type instance for the specified Java class name.
     *
     * @param sJavaName  Java class name
     *
     * @return ASM Type for the specified Java class name
     */
    private static Type type(String sJavaName)
        {
        return sJavaName == null ? OBJECT_TYPE : Type.getType("L" + internalName(sJavaName) + ";");
        }

    /**
     * Determine whether the class being instrumented is an {@link Enum}.
     *
     * @return  {@code true} if the class being instrumented is an {@link Enum}
     */
    private boolean isEnum()
        {
        return (m_classNode.access & ACC_ENUM) == ACC_ENUM;
        }

    /**
     * Determine whether the class has already been instrumented.
     *
     * @return  {@code true} if the class has already been instrumented
     */
    private boolean isInstrumented()
        {
        return AsmUtils.hasAnnotation(m_classNode, Instrumented.class);
        }

    /**
     * Find method with the specified name and description.
     *
     * @param sName  method name
     * @param sDesc  method description
     *
     * @return method with the specified name and description, or {@code null}
     *         if such a method doesn't exist
     */
    @SuppressWarnings("SameParameterValue")
    private MethodNode findMethod(String sName, String sDesc)
        {
        for (MethodNode node : m_classNode.methods)
            {
            if (node.name.equals(sName) && node.desc.equals(sDesc))
                {
                return node;
                }
            }
        return null;
        }

    /**
     * Return {@code true} if the instrumented class contains a method with
     * the same signature as the specified method.
     *
     * @param mn  method to look for
     *
     * @return {@code true} if the instrumented class contains a method with
     *         the same signature as the specified method; {@code false} otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean hasMethod(MethodNode mn)
        {
        for (MethodNode node : m_classNode.methods)
            {
            if (mn.name.equals(node.name) && mn.desc.equals(node.desc))
                {
                return true;
                }
            }
        return false;
        }

    /**
     * Return {@code true} if debug output is enabled.
     *
     * @return {@code true} if debug output is enabled; {@code false} otherwise
     */
    private boolean isDebugEnabled()
        {
        return m_fDebug;
        }

    /**
     * Add {@link PofIndex} annotation to backing field.
     *
     * @param fn      backing field to add the annotation to
     * @param nIndex  POF index value to define within the annotation
     */
    protected void addPofIndex(FieldNode fn, int nIndex)
        {
        AnnotationNode an = new AnnotationNode(Type.getDescriptor(PofIndex.class));
        an.values = Arrays.asList("value", nIndex);
        addAnnotation(fn, an);
        }

    /**
     * Return {@link ReadMethod} for the specified property.
     *
     * @param property  the property to create a read method for
     * @param type      the type of the property
     *
     * @return read method for the specified property
     */
    private ReadMethod getReadMethod(PofProperty property, Type type)
        {
        switch (type.getSort())
            {
            case Type.BOOLEAN:
                return new ReadMethod("readBoolean", "(I)Z");
            case Type.BYTE:
                return new ReadMethod("readByte", "(I)B");
            case Type.CHAR:
                return new ReadMethod("readChar", "(I)C");
            case Type.SHORT:
                return new ReadMethod("readShort", "(I)S");
            case Type.INT:
                return new ReadMethod("readInt", "(I)I");
            case Type.LONG:
                return new ReadMethod("readLong", "(I)J");
            case Type.FLOAT:
                return new ReadMethod("readFloat", "(I)F");
            case Type.DOUBLE:
                return new ReadMethod("readDouble", "(I)D");

            case Type.ARRAY:
                if ("[Z".equals(type.getDescriptor()))
                    {
                    return new ReadMethod(
                            "readBooleanArray", "(I)[Z");
                    }
                if ("[B".equals(type.getDescriptor()))
                    {
                    return new ReadMethod(
                            "readByteArray", "(I)[B");
                    }
                if ("[C".equals(type.getDescriptor()))
                    {
                    return new ReadMethod(
                            "readCharArray", "(I)[C");
                    }
                if ("[S".equals(type.getDescriptor()))
                    {
                    return new ReadMethod(
                            "readShortArray", "(I)[S");
                    }
                if ("[I".equals(type.getDescriptor()))
                    {
                    return new ReadMethod(
                            "readIntArray", "(I)[I");
                    }
                if ("[J".equals(type.getDescriptor()))
                    {
                    return new ReadMethod(
                            "readLongArray", "(I)[J");
                    }
                if ("[F".equals(type.getDescriptor()))
                    {
                    return new ReadMethod(
                            "readFloatArray", "(I)[F");
                    }
                if ("[D".equals(type.getDescriptor()))
                    {
                    return new ReadMethod(
                            "readDoubleArray", "(I)[D");
                    }
                return new ObjectArrayReadMethod();

            default:
                String sClassName = type.getClassName();
                if (sClassName.equals(String.class.getName()))
                    {
                    return new ReadMethod("readString", "(I)Ljava/lang/String;");
                    }
                if (sClassName.equals(Date.class.getName()))
                    {
                    return new ReadMethod("readDate", "(I)Ljava/util/Date;");
                    }
                if (sClassName.equals(LocalDate.class.getName()))
                    {
                    return new ReadMethod("readLocalDate", "(I)Ljava/time/LocalDate;");
                    }
                if (sClassName.equals(LocalDateTime.class.getName()))
                    {
                    return new ReadMethod("readLocalDateTime", "(I)Ljava/time/LocalDateTime;");
                    }
                if (sClassName.equals(LocalTime.class.getName()))
                    {
                    return new ReadMethod("readLocalTime", "(I)Ljava/time/LocalTime;");
                    }
                if (sClassName.equals(OffsetDateTime.class.getName()))
                    {
                    return new ReadMethod("readOffsetDateTime", "(I)Ljava/time/OffsetDateTime;");
                    }
                if (sClassName.equals(OffsetTime.class.getName()))
                    {
                    return new ReadMethod("readOffsetTime", "(I)Ljava/time/OffsetTime;");
                    }
                if (sClassName.equals(ZonedDateTime.class.getName()))
                    {
                    return new ReadMethod("readZonedDateTime", "(I)Ljava/time/ZonedDateTime;");
                    }
                if (sClassName.equals(RawDate.class.getName()))
                    {
                    return new ReadMethod("readRawDate", "(I)Lcom/tangosol/io/pof/RawDate;");
                    }
                if (sClassName.equals(RawDateTime.class.getName()))
                    {
                    return new ReadMethod("readRawDateTime", "(I)Lcom/tangosol/io/pof/RawDateTime;");
                    }
                if (sClassName.equals(RawDayTimeInterval.class.getName()))
                    {
                    return new ReadMethod("readRawDayTimeInterval", "(I)Lcom/tangosol/io/pof/RawDayTimeInterval;");
                    }
                if (sClassName.equals(RawQuad.class.getName()))
                    {
                    return new ReadMethod("readRawQuad", "(I)Lcom/tangosol/io/pof/RawQuad;");
                    }
                if (sClassName.equals(RawTime.class.getName()))
                    {
                    return new ReadMethod("readRawTime", "(I)Lcom/tangosol/io/pof/RawTime;");
                    }
                if (sClassName.equals(RawTimeInterval.class.getName()))
                    {
                    return new ReadMethod("readRawTimeInterval", "(I)Lcom/tangosol/io/pof/RawTimeInterval;");
                    }
                if (sClassName.equals(RawYearMonthInterval.class.getName()))
                    {
                    return new ReadMethod("readRawYearMonthInterval", "(I)Lcom/tangosol/io/pof/RawYearMonthInterval;");
                    }
                if (sClassName.equals(BigDecimal.class.getName()))
                    {
                    return new ReadMethod("readBigDecimal", "(I)Ljava/math/BigDecimal;");
                    }
                if (sClassName.equals(BigInteger.class.getName()))
                    {
                    return new ReadMethod("readBigInteger", "(I)Ljava/math/BigInteger;");
                    }
                if (sClassName.equals(Binary.class.getName()))
                    {
                    return new ReadMethod("readBinary", "(I)Lcom/tangosol/util/Binary;");
                    }
                if (property.isCollection())
                    {
                    return new CollectionReadMethod();
                    }
                if (property.isMap())
                    {
                    return new MapReadMethod();
                    }
                return new ReadMethod("readObject", "(I)Ljava/lang/Object;");
            }
        }

    /**
     * Return {@link WriteMethod} for the specified property.
     *
     * @param property  the property to create a write method for
     * @param type      the type of the property
     *
     * @return write method for the specified property
     */
    private WriteMethod getWriteMethod(PofProperty property, Type type)
        {
        switch (type.getSort())
            {
            case Type.BOOLEAN:
                return new WriteMethod("writeBoolean", "(IZ)V");
            case Type.BYTE:
                return new WriteMethod("writeByte", "(IB)V");
            case Type.CHAR:
                return new WriteMethod("writeChar", "(IC)V");
            case Type.DOUBLE:
                return new WriteMethod("writeDouble", "(ID)V");
            case Type.FLOAT:
                return new WriteMethod("writeFloat", "(IF)V");
            case Type.INT:
                return new WriteMethod("writeInt", "(II)V");
            case Type.LONG:
                return new WriteMethod("writeLong", "(IJ)V");
            case Type.SHORT:
                return new WriteMethod("writeShort", "(IS)V");

            case Type.ARRAY:
                if ("[Z".equals(type.getDescriptor()))
                    {
                    return new WriteMethod("writeBooleanArray", "(I[Z)V");
                    }
                if ("[B".equals(type.getDescriptor()))
                    {
                    return new WriteMethod("writeByteArray", "(I[B)V");
                    }
                if ("[C".equals(type.getDescriptor()))
                    {
                    return new WriteMethod("writeCharArray", "(I[CZ)V");
                    }
                if ("[D".equals(type.getDescriptor()))
                    {
                    return new WriteMethod("writeDoubleArray", "(I[DZ)V");
                    }
                if ("[F".equals(type.getDescriptor()))
                    {
                    return new WriteMethod("writeFloatArray", "(I[FZ)V");
                    }
                if ("[I".equals(type.getDescriptor()))
                    {
                    return new WriteMethod("writeIntArray", "(I[IZ)V");
                    }
                if ("[J".equals(type.getDescriptor()))
                    {
                    return new WriteMethod("writeLongArray", "(I[JZ)V");
                    }
                if ("[S".equals(type.getDescriptor()))
                    {
                    return new WriteMethod("writeShortArray", "(I[SZ)V");
                    }
                return getObjectArrayWriteMethod(property, type);

            default: // Type.OBJECT
                String sClassName = type.getClassName();
                if (sClassName.equals(String.class.getName()))
                    {
                    return new WriteMethod("writeString",
                                           "(ILjava/lang/String;)V");
                    }
                if (sClassName.equals(BigDecimal.class.getName()))
                    {
                    return new WriteMethod("writeBigDecimal",
                                           "(ILjava/math/BigDecimal;)V");
                    }
                if (sClassName.equals(BigInteger.class.getName()))
                    {
                    return new WriteMethod("writeBigInteger",
                                           "(ILjava/math/BigInteger;)V");
                    }
                if (sClassName.equals(Binary.class.getName()))
                    {
                    return new WriteMethod("writeBinary",
                                           "(ILcom/tangosol/util/Binary;)V");
                    }
                if (sClassName.equals(LocalDate.class.getName()))
                    {
                    return new WriteMethod("writeDate",
                                           "(ILjava/time/LocalDate;)V");
                    }
                if (sClassName.equals(LocalDateTime.class.getName()))
                    {
                    return new WriteMethod("writeDateTime",
                                           "(ILjava/time/LocalDateTime;)V");
                    }
                if (sClassName.equals(LocalTime.class.getName()))
                    {
                    return new WriteMethod("writeTime",
                                           "(ILjava/time/LocalTime;)V");
                    }
                if (sClassName.equals(OffsetDateTime.class.getName()))
                    {
                    return new WriteMethod("writeDateTimeWithZone",
                                           "(ILjava/time/OffsetDateTime;)V");
                    }
                if (sClassName.equals(OffsetTime.class.getName()))
                    {
                    return new WriteMethod("writeTimeWithZone",
                                           "(ILjava/time/OffsetTime;)V");
                    }
                if (sClassName.equals(ZonedDateTime.class.getName()))
                    {
                    return new WriteMethod("writeDateTimeWithZone",
                                           "(ILjava/time/ZonedDateTime;)V");
                    }
                if (sClassName.equals(Date.class.getName()))
                    {
                    return getDateWriteMethod(property, type);
                    }
                if (sClassName.equals("java.sql.Timestamp"))
                    {
                    return getDateWriteMethod(property, type);
                    }
                if (property.isCollection())
                    {
                    return getCollectionWriteMethod(property, type);
                    }
                if (property.isMap())
                    {
                    return getMapWriteMethod(property, type);
                    }
                if (sClassName.equals(RawDate.class.getName()))
                    {
                    return new WriteMethod("writeRawDate",
                                           "(ILcom/tangosol/io/pof/RawDate;)V");
                    }
                if (sClassName.equals(RawDateTime.class.getName()))
                    {
                    return new WriteMethod("writeRawDateTime",
                                           "(ILcom/tangosol/io/pof/RawDateTime;)V");
                    }
                if (sClassName.equals(RawDayTimeInterval.class.getName()))
                    {
                    return new WriteMethod("writeRawDayTimeInterval",
                                           "(ILcom/tangosol/io/pof/RawDayTimeInterval;)V");
                    }
                if (sClassName.equals(RawQuad.class.getName()))
                    {
                    return new WriteMethod("writeRawQuad",
                                           "(ILcom/tangosol/io/pof/RawQuad;)V");
                    }
                if (sClassName.equals(RawTime.class.getName()))
                    {
                    return new WriteMethod("writeRawTime",
                                           "(ILcom/tangosol/io/pof/RawTime;)V");
                    }
                if (sClassName.equals(RawTimeInterval.class.getName()))
                    {
                    return new WriteMethod("writeRawTimeInterval",
                                           "(ILcom/tangosol/io/pof/RawTimeInterval;)V");
                    }
                if (sClassName.equals(RawYearMonthInterval.class.getName()))
                    {
                    return new WriteMethod("writeRawYearMonthInterval",
                                           "(ILcom/tangosol/io/pof/RawYearMonthInterval;)V");
                    }
                return new WriteMethod("writeObject", "(ILjava/lang/Object;)V");
            }
        }

    /**
     * Return write method for a date/time value.
     *
     * @param property  property to create write method for
     * @param type      property type
     *
     * @return write method
     */
    private WriteMethod getDateWriteMethod(PofProperty property, Type type)
        {
        String name = "writeDateTime";
        String desc = "(I" + type.getDescriptor() + ")V";

        PofDate pd = property.asDate();
        if (pd != null)
            {
            DateMode mode = pd.getMode();
            switch (mode)
                {
                case DATE:
                    name = "writeDate";
                    break;
                case TIME:
                    name = "writeTime";
                    break;
                case DATE_TIME:
                    name = "writeDateTime";
                    break;
                }
            if (mode != DateMode.DATE)
                {
                if (pd.isIncludeTimezone())
                    {
                    name += "WithZone";
                    }
                }
            }

        return new WriteMethod(name, desc);
        }

    /**
     * Return write method for a collection.
     *
     * @param property  property to create write method for
     * @param type      property type
     *
     * @return write method
     */
    private WriteMethod getCollectionWriteMethod(PofProperty property, Type type)
        {
        Type elementClass = OBJECT_TYPE;

        PofCollection col = property.asCollection();
        if (col != null)
            {
            elementClass = type(col.getElementClass());
            }

        return new CollectionWriteMethod(elementClass);
        }

    /**
     * Return write method for a map.
     *
     * @param property  property to create write method for
     * @param type      property type
     *
     * @return write method
     */
    private WriteMethod getMapWriteMethod(PofProperty property, Type type)
        {
        Type keyClass   = OBJECT_TYPE;
        Type valueClass = OBJECT_TYPE;

        PofMap map = property.asMap();
        if (map != null)
            {
            keyClass = type(map.getKeyClass());
            if (!OBJECT_TYPE.equals(keyClass))
                {
                valueClass = type(map.getValueClass());
                }
            }

        return new MapWriteMethod(keyClass, valueClass);
        }

    /**
     * Return write method for an object array.
     *
     * @param property  property to create write method for
     * @param type      property type
     *
     * @return write method
     */
    private WriteMethod getObjectArrayWriteMethod(PofProperty property, Type type)
        {
        Type elementClass = OBJECT_TYPE;

        PofArray array = property.asArray();
        if (array != null)
            {
            elementClass = type(array.getElementClass());
            }

        return new ObjectArrayWriteMethod(elementClass);
        }

    // ---- constants -------------------------------------------------------

    /**
     * Object type constant.
     */
    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    /**
     * The set of array types that support raw encoding.
     */
    private static final Set<Integer> RAW_ENCODING_TYPES =
            Set.of(Type.CHAR, Type.SHORT, Type.INT, Type.LONG, Type.FLOAT, Type.DOUBLE);

    /**
     * JsonbTransient annotation.
     */
    private static final AnnotationNode JSONB_TRANSIENT =
            new AnnotationNode("Ljavax/json/bind/annotation/JsonbTransient;");

    // ---- data members ----------------------------------------------------

    /**
     * The {@link Schema} instance holding the types.
     */
    private Schema m_schema;

    /**
     * The {@link Logger} to use.
     */
    private Logger m_log;

    /**
     * Flag indicating whether debug output is enabled.
     */
    private boolean m_fDebug;

    /**
     * A class annotated with {@link PortableType} that is being instrumented.
     */
    private ClassNode m_classNode;

    /**
     * POF type metadata for the class being instrumented.
     */
    private PofType m_type;

    /**
     * A map of POF property metadata for the class being instrumented,
     * keyed by class version when the property was first introduced, and then
     * sorted by property order/index within each version.
     */
    private TreeMap<Integer, SortedSet<PofProperty>> m_mapProperties = new TreeMap<>();

    /**
     * A map of all the fields of the class being instrumented, keyed by field name.
     */
    private Map<String, FieldNode> m_mapFields;
    }
