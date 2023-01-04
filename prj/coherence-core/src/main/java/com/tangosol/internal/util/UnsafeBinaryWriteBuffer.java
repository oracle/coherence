/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.util.Binary;
import com.tangosol.util.Unsafe;

/**
 * An unsafe WriteBuffer implementation whose primary purpose is to be used to
 * create unsafe Binary objects.
 * <p/>
 * This class should only be used when you want to reuse the same buffer to create
 * multiple Binary instances, and you know exactly what you are doing and why!
 * <p/>
 * This buffer is only safe to reuse if you are certain that the Binary that was
 * created from it is effectively garbage and can be collected by the JVM, as any
 * Binary obtained from this buffer WILL change whenever the buffer itself changes.
 * <p/>
 * Reuse is also only safe from a single thread, so this class is only suitable
 * for use as a local or thread-local variable. Attempting to share instances of
 * this class across multiple threads will almost certainly have unintended
 * consequences.
 *
 * @author Aleks Seovic  2022.11.09
 */
public final class UnsafeBinaryWriteBuffer
        extends ByteArrayWriteBuffer
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an UnsafeBinaryWriteBuffer with a certain initial capacity.
     *
     * @param cbCap initial capacity
     *
     * @throws IllegalArgumentException if <tt>cbCap</tt> is negative
     */
    public UnsafeBinaryWriteBuffer(int cbCap)
        {
        super(cbCap);
        }

    /**
     * Construct an UnsafeBinaryWriteBuffer with a certain initial capacity and a
     * certain maximum capacity.
     *
     * @param cbCap initial capacity
     * @param cbMax maximum capacity
     *
     * @throws IllegalArgumentException if <tt>cbCap</tt> or <tt>cbMax</tt> is
     *                                  negative, or if <tt>cbCap</tt> is
     *                                  greater than
     *                                  <tt>cbMax</tt>
     */
    public UnsafeBinaryWriteBuffer(int cbCap, int cbMax)
        {
        super(cbCap, cbMax);
        }

    // ----- WriteBuffer interface ------------------------------------------

    @Override
    public Binary toBinary()
        {
        return UNSAFE.newBinary(m_ab, 0, m_cb);
        }

    // ----- public API -----------------------------------------------------

    /**
     * Return an existing or create a new write buffer for the current thread.
     *
     * @return an existing or a new write buffer for the current thread
     */
    public static WriteBuffer get()
        {
        return THREAD_BUFFER.get();
        }

    // ----- static members -------------------------------------------------

    /**
     * Cached instance of Unsafe.
     *
     * @see Unsafe#getUnsafe()
     */
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    /**
     * Cached unsafe per-thread buffers.
     */
    private static final ThreadLocal<UnsafeBinaryWriteBuffer> THREAD_BUFFER =
            ThreadLocal.withInitial(() -> new UnsafeBinaryWriteBuffer(4096));
    }
