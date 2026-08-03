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
import java.util.Objects;

/**
 * Passive canonical senior-metadata proof payload.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 26.07
 */
public final class SeniorMetadataProofPayload
    {
    /**
     * Construct a senior-metadata proof payload.
     */
    public SeniorMetadataProofPayload(int nPayloadVersion, byte nMessageKind, String sAlgorithmId,
            String sKeyId, String sIssuerId, String sClusterName, String sServiceName, int nServiceId,
            int nMessageType, String sSenderId, String sSeniorId, String sTargetId, String sCulpritId,
            byte nKillDirection, boolean fZombie, long lSeniorEpoch, long lLastJoinTime,
            int nMemberSetDigestVersion, int cMembers, byte[] abMemberSetDigest, long lReplayEpoch,
            long lNonce, long lIssuedAtMillis, long lExpiresAtMillis)
        {
        f_nPayloadVersion          = nPayloadVersion;
        f_nMessageKind             = nMessageKind;
        f_sAlgorithmId             = normalize(sAlgorithmId, "algorithm");
        f_sKeyId                   = normalize(sKeyId, "key");
        f_sIssuerId                = normalize(sIssuerId, "issuer");
        f_sClusterName             = normalize(sClusterName, "cluster");
        f_sServiceName             = normalize(sServiceName, "service");
        f_nServiceId               = nServiceId;
        f_nMessageType             = nMessageType;
        f_sSenderId                = normalize(sSenderId, "sender");
        f_sSeniorId                = normalize(sSeniorId, "senior");
        f_sTargetId                = normalize(sTargetId, "target");
        f_sCulpritId               = normalize(sCulpritId, "culprit");
        f_nKillDirection           = nKillDirection;
        f_fZombie                  = fZombie;
        f_lSeniorEpoch             = lSeniorEpoch;
        f_lLastJoinTime            = lLastJoinTime;
        f_nMemberSetDigestVersion  = nMemberSetDigestVersion;
        f_cMembers                 = cMembers;
        f_abMemberSetDigest        = copyBounded(abMemberSetDigest, "member-set digest", MAX_DIGEST_BYTES);
        f_lReplayEpoch             = lReplayEpoch;
        f_lNonce                   = lNonce;
        f_lIssuedAtMillis          = lIssuedAtMillis;
        f_lExpiresAtMillis         = lExpiresAtMillis;
        }

    /**
     * Return a deterministic binary representation.
     *
     * @return the payload bytes
     */
    public byte[] toByteArray()
        {
        try
            {
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            DataOutputStream      out      = new DataOutputStream(outBytes);

            out.writeInt(f_nPayloadVersion);
            out.writeByte(f_nMessageKind);
            out.writeUTF(f_sAlgorithmId);
            out.writeUTF(f_sKeyId);
            out.writeUTF(f_sIssuerId);
            out.writeUTF(f_sClusterName);
            out.writeUTF(f_sServiceName);
            out.writeInt(f_nServiceId);
            out.writeInt(f_nMessageType);
            out.writeUTF(f_sSenderId);
            out.writeUTF(f_sSeniorId);
            out.writeUTF(f_sTargetId);
            out.writeUTF(f_sCulpritId);
            out.writeByte(f_nKillDirection);
            out.writeBoolean(f_fZombie);
            out.writeLong(f_lSeniorEpoch);
            out.writeLong(f_lLastJoinTime);
            out.writeInt(f_nMemberSetDigestVersion);
            out.writeInt(f_cMembers);
            writeBytes(out, f_abMemberSetDigest);
            out.writeLong(f_lReplayEpoch);
            out.writeLong(f_lNonce);
            out.writeLong(f_lIssuedAtMillis);
            out.writeLong(f_lExpiresAtMillis);
            out.flush();

            byte[] ab = outBytes.toByteArray();
            if (ab.length > MAX_ENCODED_BYTES)
                {
                throw new IllegalStateException("senior-metadata payload bytes exceed maximum");
                }
            return ab;
            }
        catch (IOException e)
            {
            throw new UncheckedIOException(e);
            }
        }

    /**
     * Read a payload from its deterministic binary representation.
     *
     * @param abPayload  the payload bytes
     *
     * @return the payload
     *
     * @throws IOException if the payload is malformed
     */
    public static SeniorMetadataProofPayload fromByteArray(byte[] abPayload)
            throws IOException
        {
        byte[] ab = abPayload == null ? EMPTY : abPayload;
        if (ab.length > MAX_ENCODED_BYTES)
            {
            throw new IOException("oversized senior-metadata payload length");
            }

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(ab));
        SeniorMetadataProofPayload payload = new SeniorMetadataProofPayload(in.readInt(), in.readByte(),
                readUtf(in, "algorithm"), readUtf(in, "key"), readUtf(in, "issuer"), readUtf(in, "cluster"),
                readUtf(in, "service"), in.readInt(), in.readInt(), readUtf(in, "sender"),
                readUtf(in, "senior"), readUtf(in, "target"), readUtf(in, "culprit"), in.readByte(),
                in.readBoolean(), in.readLong(), in.readLong(), in.readInt(), in.readInt(),
                readBytes(in, "member-set digest", MAX_DIGEST_BYTES), in.readLong(), in.readLong(),
                in.readLong(), in.readLong());
        if (in.read() >= 0)
            {
            throw new IOException("trailing senior-metadata payload bytes");
            }
        return payload;
        }

    public int getPayloadVersion()
        {
        return f_nPayloadVersion;
        }

    public byte getMessageKind()
        {
        return f_nMessageKind;
        }

    public String getAlgorithmId()
        {
        return f_sAlgorithmId;
        }

    public String getKeyId()
        {
        return f_sKeyId;
        }

    public String getIssuerId()
        {
        return f_sIssuerId;
        }

    public String getClusterName()
        {
        return f_sClusterName;
        }

    public String getServiceName()
        {
        return f_sServiceName;
        }

    public int getServiceId()
        {
        return f_nServiceId;
        }

    public int getMessageType()
        {
        return f_nMessageType;
        }

    public String getSenderId()
        {
        return f_sSenderId;
        }

    public String getSeniorId()
        {
        return f_sSeniorId;
        }

    public String getTargetId()
        {
        return f_sTargetId;
        }

    public String getCulpritId()
        {
        return f_sCulpritId;
        }

    public byte getKillDirection()
        {
        return f_nKillDirection;
        }

    public boolean isZombie()
        {
        return f_fZombie;
        }

    public long getSeniorEpoch()
        {
        return f_lSeniorEpoch;
        }

    public long getLastJoinTime()
        {
        return f_lLastJoinTime;
        }

    public int getMemberSetDigestVersion()
        {
        return f_nMemberSetDigestVersion;
        }

    public int getMemberSetCount()
        {
        return f_cMembers;
        }

    public byte[] getMemberSetDigest()
        {
        return Arrays.copyOf(f_abMemberSetDigest, f_abMemberSetDigest.length);
        }

    public long getReplayEpoch()
        {
        return f_lReplayEpoch;
        }

    public long getNonce()
        {
        return f_lNonce;
        }

    public long getIssuedAtMillis()
        {
        return f_lIssuedAtMillis;
        }

    public long getExpiresAtMillis()
        {
        return f_lExpiresAtMillis;
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof SeniorMetadataProofPayload))
            {
            return false;
            }
        SeniorMetadataProofPayload that = (SeniorMetadataProofPayload) o;
        return f_nPayloadVersion == that.f_nPayloadVersion
                && f_nMessageKind == that.f_nMessageKind
                && f_nServiceId == that.f_nServiceId
                && f_nMessageType == that.f_nMessageType
                && f_nKillDirection == that.f_nKillDirection
                && f_fZombie == that.f_fZombie
                && f_lSeniorEpoch == that.f_lSeniorEpoch
                && f_lLastJoinTime == that.f_lLastJoinTime
                && f_nMemberSetDigestVersion == that.f_nMemberSetDigestVersion
                && f_cMembers == that.f_cMembers
                && f_lReplayEpoch == that.f_lReplayEpoch
                && f_lNonce == that.f_lNonce
                && f_lIssuedAtMillis == that.f_lIssuedAtMillis
                && f_lExpiresAtMillis == that.f_lExpiresAtMillis
                && f_sAlgorithmId.equals(that.f_sAlgorithmId)
                && f_sKeyId.equals(that.f_sKeyId)
                && f_sIssuerId.equals(that.f_sIssuerId)
                && f_sClusterName.equals(that.f_sClusterName)
                && f_sServiceName.equals(that.f_sServiceName)
                && f_sSenderId.equals(that.f_sSenderId)
                && f_sSeniorId.equals(that.f_sSeniorId)
                && f_sTargetId.equals(that.f_sTargetId)
                && f_sCulpritId.equals(that.f_sCulpritId)
                && Arrays.equals(f_abMemberSetDigest, that.f_abMemberSetDigest);
        }

    @Override
    public int hashCode()
        {
        int nResult = Objects.hash(f_nPayloadVersion, f_nMessageKind, f_sAlgorithmId, f_sKeyId, f_sIssuerId,
                f_sClusterName, f_sServiceName, f_nServiceId, f_nMessageType, f_sSenderId, f_sSeniorId,
                f_sTargetId, f_sCulpritId, f_nKillDirection, f_fZombie, f_lSeniorEpoch, f_lLastJoinTime,
                f_nMemberSetDigestVersion, f_cMembers, f_lReplayEpoch, f_lNonce, f_lIssuedAtMillis,
                f_lExpiresAtMillis);
        return 31 * nResult + Arrays.hashCode(f_abMemberSetDigest);
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
            throw new IOException("negative senior-metadata " + sField + " length");
            }
        if (cb > cbMax)
            {
            throw new IOException("oversized senior-metadata " + sField + " length");
            }
        if (cb > in.available())
            {
            throw new IOException("truncated senior-metadata " + sField);
            }
        byte[] ab = new byte[cb];
        in.readFully(ab);
        return ab;
        }

    private static String readUtf(DataInputStream in, String sField)
            throws IOException
        {
        int cb = in.readUnsignedShort();
        if (cb > MAX_UTF_BYTES)
            {
            throw new IOException("oversized senior-metadata " + sField + " string");
            }
        if (cb > in.available())
            {
            throw new IOException("truncated senior-metadata " + sField + " string");
            }

        byte[] ab = new byte[cb + 2];
        ab[0] = (byte) (cb >>> 8);
        ab[1] = (byte) cb;
        in.readFully(ab, 2, cb);
        return new DataInputStream(new ByteArrayInputStream(ab)).readUTF();
        }

    private static byte[] copyBounded(byte[] ab, String sField, int cbMax)
        {
        byte[] abValue = ab == null ? EMPTY : ab;
        if (abValue.length > cbMax)
            {
            throw new IllegalArgumentException("senior-metadata " + sField + " bytes exceed maximum");
            }
        return Arrays.copyOf(abValue, abValue.length);
        }

    private static String normalize(String sValue, String sField)
        {
        String sNormalized = sValue == null ? "" : sValue;
        if (getUtfLength(sNormalized) > MAX_UTF_BYTES)
            {
            throw new IllegalArgumentException("senior-metadata " + sField + " string exceeds maximum");
            }
        return sNormalized;
        }

    private static int getUtfLength(String sValue)
        {
        int cb = 0;
        for (int of = 0, cch = sValue.length(); of < cch; of++)
            {
            char ch = sValue.charAt(of);
            cb += ch >= 0x0001 && ch <= 0x007F ? 1 : ch <= 0x07FF ? 2 : 3;
            }
        return cb;
        }

    public static final int PAYLOAD_VERSION = 1;

    public static final byte MESSAGE_KIND_HEARTBEAT    = 1;
    public static final byte MESSAGE_KIND_PANIC_REPORT = 2;
    public static final byte MESSAGE_KIND_PANIC_TOKEN  = 3;
    public static final byte MESSAGE_KIND_KILL_TOKEN   = 4;

    public static final byte KILL_DIRECTION_NONE                    = 0;
    public static final byte KILL_DIRECTION_SENIOR_TO_JUNIOR        = 1;
    public static final byte KILL_DIRECTION_JUNIOR_TO_DOOMED_SENIOR = 2;
    public static final byte KILL_DIRECTION_DOOMED_SENIOR_TO_JUNIOR = 3;

    static final int MAX_ENCODED_BYTES = SeniorMetadataProof.MAX_PAYLOAD_BYTES;

    /**
     * Maximum encoded UTF bytes for one senior-metadata string field.
     */
    static final int MAX_UTF_BYTES = 4096;

    /**
     * Maximum member-set digest bytes.
     */
    static final int MAX_DIGEST_BYTES = 1024;

    private static final byte[] EMPTY = new byte[0];

    private final int     f_nPayloadVersion;
    private final byte    f_nMessageKind;
    private final String  f_sAlgorithmId;
    private final String  f_sKeyId;
    private final String  f_sIssuerId;
    private final String  f_sClusterName;
    private final String  f_sServiceName;
    private final int     f_nServiceId;
    private final int     f_nMessageType;
    private final String  f_sSenderId;
    private final String  f_sSeniorId;
    private final String  f_sTargetId;
    private final String  f_sCulpritId;
    private final byte    f_nKillDirection;
    private final boolean f_fZombie;
    private final long    f_lSeniorEpoch;
    private final long    f_lLastJoinTime;
    private final int     f_nMemberSetDigestVersion;
    private final int     f_cMembers;
    private final byte[]  f_abMemberSetDigest;
    private final long    f_lReplayEpoch;
    private final long    f_lNonce;
    private final long    f_lIssuedAtMillis;
    private final long    f_lExpiresAtMillis;
    }
