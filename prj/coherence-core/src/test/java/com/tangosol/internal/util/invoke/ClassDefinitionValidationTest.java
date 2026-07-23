/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataOutput;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for {@link ClassDefinition} class-file shape validation.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class ClassDefinitionValidationTest
    {
    @Test
    public void testRejectsShortBytecode()
        {
        assertRejected(new byte[9], "invalid-shape");
        }

    @Test
    public void testRejectsWrongMagic()
            throws IOException
        {
        byte[] abClass = validClassBytes();
        abClass[0] = 0;

        assertRejected(abClass, "missing-magic");
        }

    @Test
    public void testRejectsMajorVersionOutOfRange()
            throws IOException
        {
        byte[] abClass = validClassBytes();
        abClass[6] = 0;
        abClass[7] = 71;

        assertRejected(abClass, "invalid-version");
        }

    @Test
    public void testRejectsConstantPoolOverrun()
        {
        byte[] abClass =
                {
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
                0, 0,
                0, 52,
                0, 2,
                1, (byte) 0xFF, (byte) 0xFF
                };

        assertRejected(abClass, "invalid-shape");
        }

    @Test
    public void testRejectsTrailingBytes()
            throws IOException
        {
        byte[] abClass = validClassBytes();
        byte[] abExtra = Arrays.copyOf(abClass, abClass.length + 1);

        assertRejected(abExtra, "trailing-bytes");
        }

    @Test
    public void testAcceptsWellFormedClassFile()
            throws IOException
        {
        byte[]          abClass    = validClassBytes();
        ClassDefinition definition = readDefinition(abClass);

        assertEquals(new ClassIdentity(ClassDefinitionValidationTest.class), definition.getId());
        assertArrayEquals(abClass, definition.getBytes());
        }

    private static void assertRejected(byte[] abClass, String sReason)
        {
        IOException e = assertThrows(IOException.class, () -> readDefinition(abClass));
        assertEquals("Invalid class definition: " + sReason, e.getMessage());
        }

    private static ClassDefinition readDefinition(byte[] abClass)
            throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(0);
        DataOutput out = buffer.getBufferOutput();
        ExternalizableHelper.writeObject(out, new ClassIdentity(ClassDefinitionValidationTest.class));
        ExternalizableHelper.writeByteArray(out, abClass);

        ClassDefinition definition = new ClassDefinition();
        definition.readExternal(new ByteArrayReadBuffer(buffer.toByteArray()).getBufferInput());
        return definition;
        }

    private static byte[] validClassBytes()
            throws IOException
        {
        try (InputStream in = ClassDefinitionValidationTest.class.getResourceAsStream("ClassDefinitionValidationTest.class"))
            {
            return Base.read(in);
            }
        }
    }
