/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.util.Arrays;

/**
 * Passive senior-metadata proof record containing a canonical payload and
 * provider proof bytes.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 26.07
 */
public final class SeniorMetadataProof
    {
    /**
     * Construct a senior-metadata proof.
     *
     * @param payload  the canonical payload
     * @param abProof  the provider proof bytes
     */
    public SeniorMetadataProof(SeniorMetadataProofPayload payload, byte[] abProof)
        {
        if (payload == null)
            {
            throw new IllegalArgumentException("payload is required");
            }
        f_payload = payload;
        f_abProof = abProof == null ? EMPTY : Arrays.copyOf(abProof, abProof.length);
        if (f_abProof.length > MAX_PROOF_BYTES)
            {
            throw new IllegalArgumentException("senior-metadata proof bytes exceed maximum");
            }
        }

    /**
     * Return a deterministic binary representation.
     *
     * @return the proof bytes
     */
    public byte[] toByteArray()
        {
        try
            {
            byte[] abPayload = f_payload.toByteArray();
            if (abPayload.length > MAX_PAYLOAD_BYTES)
                {
                throw new IllegalStateException("senior-metadata payload bytes exceed maximum");
                }

            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            DataOutputStream      out      = new DataOutputStream(outBytes);
            out.writeInt(MAGIC);
            out.writeInt(FORMAT_VERSION);
            writeBytes(out, abPayload);
            writeBytes(out, f_abProof);
            out.flush();

            byte[] ab = outBytes.toByteArray();
            if (ab.length > MAX_ENCODED_BYTES)
                {
                throw new IllegalStateException("senior-metadata proof encoded bytes exceed maximum");
                }
            return ab;
            }
        catch (IOException e)
            {
            throw new UncheckedIOException(e);
            }
        }

    /**
     * Read a senior-metadata proof from bytes.
     *
     * @param abProof  the proof bytes
     *
     * @return the proof
     *
     * @throws IOException if the proof bytes are malformed
     */
    public static SeniorMetadataProof fromByteArray(byte[] abProof)
            throws IOException
        {
        byte[] ab = abProof == null ? EMPTY : abProof;
        if (ab.length > MAX_ENCODED_BYTES)
            {
            throw new IOException("oversized senior-metadata proof encoded length");
            }

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(ab));
        if (in.readInt() != MAGIC)
            {
            throw new IOException("invalid senior-metadata proof magic");
            }
        if (in.readInt() != FORMAT_VERSION)
            {
            throw new IOException("unsupported senior-metadata proof format");
            }

        byte[] abPayload = readBytes(in, "payload", MAX_PAYLOAD_BYTES);
        byte[] abMac     = readBytes(in, "proof", MAX_PROOF_BYTES);
        if (in.read() >= 0)
            {
            throw new IOException("trailing senior-metadata proof bytes");
            }
        return new SeniorMetadataProof(SeniorMetadataProofPayload.fromByteArray(abPayload), abMac);
        }

    /**
     * Return the canonical payload.
     *
     * @return the canonical payload
     */
    public SeniorMetadataProofPayload getPayload()
        {
        return f_payload;
        }

    /**
     * Return provider proof bytes.
     *
     * @return provider proof bytes
     */
    public byte[] getProofBytes()
        {
        return Arrays.copyOf(f_abProof, f_abProof.length);
        }

    private static void writeBytes(DataOutputStream out, byte[] ab)
            throws IOException
        {
        out.writeInt(ab.length);
        out.write(ab);
        }

    private static byte[] readBytes(DataInputStream in, String sField, int cbMax)
            throws IOException
        {
        int cb = in.readInt();
        if (cb < 0)
            {
            throw new IOException("negative senior-metadata proof " + sField + " length");
            }
        if (cb > cbMax)
            {
            throw new IOException("oversized senior-metadata proof " + sField + " length");
            }
        if (cb > in.available())
            {
            throw new IOException("truncated senior-metadata proof " + sField);
            }
        byte[] ab = new byte[cb];
        in.readFully(ab);
        return ab;
        }

    public static final int FORMAT_VERSION = 1;

    /**
     * Maximum encoded senior-metadata proof bytes. This mirrors the 64 KiB D2
     * carrier payload limit in {@code Grid} without coupling this core package
     * to generated component classes.
     */
    static final int MAX_ENCODED_BYTES = 64 * 1024;

    /**
     * Fixed proof record bytes outside the payload and proof fields.
     */
    static final int FIXED_ENCODED_LENGTH = Integer.BYTES  // magic
            + Integer.BYTES                                // format version
            + Integer.BYTES                                // payload length
            + Integer.BYTES;                               // proof length

    /**
     * Maximum provider proof/signature bytes.
     */
    static final int MAX_PROOF_BYTES = 8 * 1024;

    /**
     * Maximum encoded canonical payload bytes while reserving space for the
     * fixed proof header and maximum provider proof bytes.
     */
    static final int MAX_PAYLOAD_BYTES = MAX_ENCODED_BYTES - FIXED_ENCODED_LENGTH - MAX_PROOF_BYTES;

    private static final int MAGIC = 0x534d4631; // SMF1

    private static final byte[] EMPTY = new byte[0];

    private final SeniorMetadataProofPayload f_payload;
    private final byte[]                     f_abProof;
    }
