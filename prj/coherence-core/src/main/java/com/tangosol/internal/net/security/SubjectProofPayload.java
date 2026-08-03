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

import java.security.Principal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import javax.security.auth.Subject;

/**
 * Passive canonical subject-proof payload.
 *
 * @author Aleks Seovic  2026.05.18
 * @since 26.07
 */
public final class SubjectProofPayload
    {
    /**
     * Construct a subject-proof payload.
     */
    public SubjectProofPayload(int nProofVersion, String sAlgorithmId, String sKeyId, String sIssuerId,
            String sSubjectSource, String sServiceName, String sCacheName, long lRequestSuid, long lNonce,
            long lIssuedAtMillis, long lExpiresAtMillis, Collection<String> colPrincipalNames)
        {
        this(nProofVersion, sAlgorithmId, sKeyId, sIssuerId, sSubjectSource, "", "", sServiceName, sCacheName,
                lRequestSuid, 0L, lNonce, lIssuedAtMillis, lExpiresAtMillis, colPrincipalNames);
        }

    /**
     * Construct a subject-proof payload.
     */
    public SubjectProofPayload(int nProofVersion, String sAlgorithmId, String sKeyId, String sIssuerId,
            String sSubjectSource, String sClusterName, String sServiceType, String sServiceName, String sCacheName,
            long lRequestSuid, long lReplayEpoch, long lNonce, long lIssuedAtMillis, long lExpiresAtMillis,
            Collection<String> colPrincipalNames)
        {
        f_nProofVersion   = nProofVersion;
        f_sAlgorithmId    = normalize(sAlgorithmId, "algorithm");
        f_sKeyId          = normalize(sKeyId, "key");
        f_sIssuerId       = normalize(sIssuerId, "issuer");
        f_sSubjectSource  = normalize(sSubjectSource, "subject-source");
        f_sClusterName    = normalize(sClusterName, "cluster");
        f_sServiceType    = normalize(sServiceType, "service-type");
        f_sServiceName    = normalize(sServiceName, "service");
        f_sCacheName      = normalize(sCacheName, "cache");
        f_lRequestSuid    = lRequestSuid;
        f_lReplayEpoch    = lReplayEpoch;
        f_lNonce          = lNonce;
        f_lIssuedAtMillis = lIssuedAtMillis;
        f_lExpiresAtMillis = lExpiresAtMillis;
        List<String> listPrincipalNames = canonicalPrincipalNames(colPrincipalNames);
        if (listPrincipalNames.size() > MAX_PRINCIPAL_COUNT)
            {
            throw new IllegalArgumentException("subject-proof principal count exceeds maximum");
            }
        f_listPrincipalNames = Collections.unmodifiableList(listPrincipalNames);
        }

    /**
     * Return a deterministic binary representation of this payload.
     *
     * @return the payload bytes
     */
    public byte[] toByteArray()
        {
        try
            {
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            DataOutputStream      out      = new DataOutputStream(outBytes);

            out.writeInt(f_nProofVersion);
            out.writeUTF(f_sAlgorithmId);
            out.writeUTF(f_sKeyId);
            out.writeUTF(f_sIssuerId);
            out.writeUTF(f_sSubjectSource);
            out.writeUTF(f_sClusterName);
            out.writeUTF(f_sServiceType);
            out.writeUTF(f_sServiceName);
            out.writeUTF(f_sCacheName);
            out.writeLong(f_lRequestSuid);
            out.writeLong(f_lReplayEpoch);
            out.writeLong(f_lNonce);
            out.writeLong(f_lIssuedAtMillis);
            out.writeLong(f_lExpiresAtMillis);
            out.writeInt(f_listPrincipalNames.size());
            for (String sName : f_listPrincipalNames)
                {
                out.writeUTF(sName);
                }
            out.flush();
            return outBytes.toByteArray();
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
    public static SubjectProofPayload fromByteArray(byte[] abPayload)
            throws IOException
        {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(abPayload == null ? EMPTY : abPayload));
        int nProofVersion = in.readInt();
        SubjectProofPayload payload = new SubjectProofPayload(nProofVersion,
                readUtf(in, "algorithm"), readUtf(in, "key"), readUtf(in, "issuer"),
                readUtf(in, "subject-source"), readUtf(in, "cluster"), readUtf(in, "service-type"),
                readUtf(in, "service"), readUtf(in, "cache"), in.readLong(), in.readLong(), in.readLong(),
                in.readLong(), in.readLong(), readPrincipalNames(in));
        if (in.read() >= 0)
            {
            throw new IOException("trailing subject-proof payload bytes");
            }
        return payload;
        }

    /**
     * Return canonical sorted principal names for a subject.
     *
     * @param subject  the subject, or {@code null}
     *
     * @return canonical principal names
     */
    public static List<String> canonicalPrincipalNames(Subject subject)
        {
        if (subject == null)
            {
            return Collections.emptyList();
            }

        List<String> listNames = new ArrayList<>();
        for (Principal principal : subject.getPrincipals())
            {
            if (principal != null)
                {
                listNames.add(principal.getName());
                }
            }
        return canonicalPrincipalNames(listNames);
        }

    /**
     * Return canonical sorted principal names.
     *
     * @param colNames  the principal names
     *
     * @return canonical principal names
     */
    public static List<String> canonicalPrincipalNames(Collection<String> colNames)
        {
        if (colNames == null || colNames.isEmpty())
            {
            return Collections.emptyList();
            }

        TreeSet<String> setNames = new TreeSet<>();
        for (String sName : colNames)
            {
            if (sName != null)
                {
                setNames.add(normalize(sName, "principal"));
                }
            }
        return new ArrayList<>(setNames);
        }

    public int getProofVersion()
        {
        return f_nProofVersion;
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

    public String getSubjectSource()
        {
        return f_sSubjectSource;
        }

    public String getClusterName()
        {
        return f_sClusterName;
        }

    public String getServiceType()
        {
        return f_sServiceType;
        }

    public String getServiceName()
        {
        return f_sServiceName;
        }

    public String getCacheName()
        {
        return f_sCacheName;
        }

    public long getRequestSuid()
        {
        return f_lRequestSuid;
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

    public List<String> getPrincipalNames()
        {
        return f_listPrincipalNames;
        }

    /**
     * Return true when every receiver-reconstructible non-principal scope
     * field matches the expected payload. Issuer/key/source authority and
     * issuer-selected proof metadata such as nonce and issue/expiry times
     * remain signed inside the proof record, but are not locally reconstructed
     * by the receiver.
     */
    public boolean matchesScope(SubjectProofPayload expected)
        {
        return expected != null
                && f_nProofVersion == expected.f_nProofVersion
                && f_sClusterName.equals(expected.f_sClusterName)
                && f_sServiceType.equals(expected.f_sServiceType)
                && f_sServiceName.equals(expected.f_sServiceName)
                && f_sCacheName.equals(expected.f_sCacheName)
                && f_lRequestSuid == expected.f_lRequestSuid
                && f_lReplayEpoch == expected.f_lReplayEpoch;
        }

    /**
     * Return true when principal names match the expected payload.
     */
    public boolean matchesPrincipals(SubjectProofPayload expected)
        {
        return expected != null && f_listPrincipalNames.equals(expected.f_listPrincipalNames);
        }

    private static List<String> readPrincipalNames(DataInputStream in)
            throws IOException
        {
        int cNames = in.readInt();
        if (cNames < 0)
            {
            throw new IOException("negative subject-proof principal count");
            }
        if (cNames > MAX_PRINCIPAL_COUNT)
            {
            throw new IOException("oversized subject-proof principal count");
            }

        List<String> listNames = new ArrayList<>(cNames);
        for (int i = 0; i < cNames; i++)
            {
            listNames.add(readUtf(in, "principal"));
            }
        return listNames;
        }

    private static String readUtf(DataInputStream in, String sField)
            throws IOException
        {
        int cb = in.readUnsignedShort();
        if (cb > MAX_UTF_BYTES)
            {
            throw new IOException("oversized subject-proof " + sField + " string");
            }
        if (cb > in.available())
            {
            throw new IOException("truncated subject-proof " + sField + " string");
            }

        byte[] ab = new byte[cb + 2];
        ab[0] = (byte) (cb >>> 8);
        ab[1] = (byte) cb;
        in.readFully(ab, 2, cb);
        return new DataInputStream(new ByteArrayInputStream(ab)).readUTF();
        }

    private static String normalize(String sValue, String sField)
        {
        String sNormalized = sValue == null ? "" : sValue;
        if (getUtfLength(sNormalized) > MAX_UTF_BYTES)
            {
            throw new IllegalArgumentException("subject-proof " + sField + " string exceeds maximum");
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

    public static final int PROOF_VERSION = 1;

    /**
     * Maximum encoded UTF bytes for one subject-proof string field.
     */
    static final int MAX_UTF_BYTES = 4096;

    /**
     * Maximum principal names in one subject-proof payload.
     */
    static final int MAX_PRINCIPAL_COUNT = 256;

    private static final byte[] EMPTY = new byte[0];

    private final int          f_nProofVersion;
    private final String       f_sAlgorithmId;
    private final String       f_sKeyId;
    private final String       f_sIssuerId;
    private final String       f_sSubjectSource;
    private final String       f_sClusterName;
    private final String       f_sServiceType;
    private final String       f_sServiceName;
    private final String       f_sCacheName;
    private final long         f_lRequestSuid;
    private final long         f_lReplayEpoch;
    private final long         f_lNonce;
    private final long         f_lIssuedAtMillis;
    private final long         f_lExpiresAtMillis;
    private final List<String> f_listPrincipalNames;
    }
