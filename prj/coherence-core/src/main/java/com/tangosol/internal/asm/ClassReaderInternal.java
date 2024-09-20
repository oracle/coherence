/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.asm;

import com.tangosol.util.asm.BaseClassReaderInternal;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

/**
 * This class wraps ASM's ClassReader allowing Coherence to bypass the class
 * version checks performed by ASM when reading a class.
 *
 * @since 15.1.1.0
 */
/*
 * Internal NOTE:  This class is also duplicated in coherence-json and
 *                 coherence-rest.  This is done because each module shades
 *                 ASM within a unique package into the produced JAR and
 *                 thus having to create copes to deal with those package
 *                 differences.
 */
public class ClassReaderInternal
        extends BaseClassReaderInternal<ClassReader, ClassVisitor>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * @see BaseClassReaderInternal#BaseClassReaderInternal(InputStream)
     */
    public ClassReaderInternal(InputStream streamIn) throws IOException
        {
        super(streamIn);
        }

    /**
     * @see BaseClassReaderInternal#BaseClassReaderInternal(byte[])
     */
    public ClassReaderInternal(byte[] abBytes)
        {
        super(abBytes);
        }

    // ----- BaseClassReaderInternal methods --------------------------------

    @Override
    protected ClassReader createReader(byte[] abBytes)
        {
        return new ClassReader(abBytes);
        }

    @Override
    protected void accept(ClassReader classReader, ClassVisitor classVisitor, int nParsingOptions)
        {
        classReader.accept(classVisitor, nParsingOptions);
        }
    }
