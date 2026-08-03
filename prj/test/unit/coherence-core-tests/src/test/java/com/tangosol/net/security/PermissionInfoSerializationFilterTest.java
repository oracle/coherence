/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.security;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.io.pof.PofPrincipal;

import com.tangosol.net.ClusterPermission;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.Signature;
import java.security.SignedObject;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.Subject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for bridge-filtered {@link PermissionInfo} nested deserialization.
 *
 * @author OpenAI  2026.05.16
 */
public class PermissionInfoSerializationFilterTest
    {
    @Test
    public void shouldRoundTripExternalizablePermissionInfo() throws Exception
        {
        PermissionInfo info = permissionInfo();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes))
            {
            info.writeExternal(out);
            }

        PermissionInfo result = new PermissionInfo();
        try (ObjectInputStream in = ExternalizableHelper.newFilteredObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()), getClass().getClassLoader()))
            {
            result.readExternal(in);
            }

        assertValidRoundTrip(result);
        }

    @Test
    public void shouldRoundTripPofPermissionInfo() throws Exception
        {
        SimplePofContext ctx = pofContext();
        Binary           bin = ExternalizableHelper.toBinary(permissionInfo(), ctx);
        PermissionInfo   result = (PermissionInfo) ExternalizableHelper.fromBinary(bin, ctx);

        assertValidRoundTrip(result);
        }

    @Test
    public void shouldRejectPofSignedPermissionBeforeReadObject() throws Exception
        {
        PofReader reader = reader(binary(new MaterializationProbe()), null, null);

        assertThrows(IOException.class, () -> new PermissionInfo().readExternal(reader));
        assertFalse(MaterializationProbe.wasMaterialized());
        }

    @Test
    public void shouldRejectPofPrincipalSetBeforeReadObject() throws Exception
        {
        Set<Object> setPrincipals = new HashSet<>();
        setPrincipals.add(new MaterializationProbe());

        PofReader reader = reader(binary(signedPermission()), binary(setPrincipals), binary(Set.of()));

        assertThrows(IOException.class, () -> new PermissionInfo().readExternal(reader));
        assertFalse(MaterializationProbe.wasMaterialized());
        }

    @Test
    public void shouldRejectPofPrincipalSetCustomNumberBeforeReadObject() throws Exception
        {
        Set<Object> setPrincipals = new HashSet<>();
        setPrincipals.add(new NumberProbe());

        PofReader reader = reader(binary(signedPermission()), binary(setPrincipals), binary(Set.of()));

        assertThrows(IOException.class, () -> new PermissionInfo().readExternal(reader));
        assertFalse(NumberProbe.wasMaterialized());
        }

    @Test
    public void shouldRejectPofCredentialSetBeforeReadObject() throws Exception
        {
        Set<Object> setCredentials = new HashSet<>();
        setCredentials.add(new MaterializationProbe());

        PofReader reader = reader(binary(signedPermission()), binary(Set.of()), binary(setCredentials));

        assertThrows(IOException.class, () -> new PermissionInfo().readExternal(reader));
        assertFalse(MaterializationProbe.wasMaterialized());
        }

    @Test
    public void shouldRejectPofCredentialSetCustomNumberBeforeReadObject() throws Exception
        {
        Set<Object> setCredentials = new HashSet<>();
        setCredentials.add(new NumberProbe());

        PofReader reader = reader(binary(signedPermission()), binary(Set.of()), binary(setCredentials));

        assertThrows(IOException.class, () -> new PermissionInfo().readExternal(reader));
        assertFalse(NumberProbe.wasMaterialized());
        }

    @Test
    public void shouldRejectExternalizableSignedPermissionBeforeReadObject() throws Exception
        {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes))
            {
            out.writeUTF("DistributedCache");
            out.writeObject(new MaterializationProbe());
            }

        PermissionInfo info = new PermissionInfo();
        try (ObjectInputStream in = ExternalizableHelper.newFilteredObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()), getClass().getClassLoader()))
            {
            assertThrows(IOException.class, () -> info.readExternal(in));
            }
        assertFalse(MaterializationProbe.wasMaterialized());
        }

    @Test
    public void shouldRejectExternalizablePrincipalBeforeReadObject() throws Exception
        {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes))
            {
            out.writeUTF("DistributedCache");
            out.writeObject(signedPermission());
            out.writeBoolean(true);
            out.writeInt(1);
            out.writeObject(new MaterializationProbe());
            out.writeInt(0);
            }

        PermissionInfo info = new PermissionInfo();
        try (ObjectInputStream in = ExternalizableHelper.newFilteredObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()), getClass().getClassLoader()))
            {
            assertThrows(IOException.class, () -> info.readExternal(in));
            }
        assertFalse(MaterializationProbe.wasMaterialized());
        }

    @Test
    public void shouldRejectExternalizablePrincipalCustomNumberBeforeReadObject() throws Exception
        {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes))
            {
            out.writeUTF("DistributedCache");
            out.writeObject(signedPermission());
            out.writeBoolean(true);
            out.writeInt(1);
            out.writeObject(new NumberProbe());
            out.writeInt(0);
            }

        PermissionInfo info = new PermissionInfo();
        NumberProbe.reset();
        try (ObjectInputStream in = ExternalizableHelper.newFilteredObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()), getClass().getClassLoader()))
            {
            assertThrows(IOException.class, () -> info.readExternal(in));
            }
        assertFalse(NumberProbe.wasMaterialized());
        }

    @Test
    public void shouldRejectExternalizableCredentialBeforeReadObject() throws Exception
        {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes))
            {
            out.writeUTF("DistributedCache");
            out.writeObject(signedPermission());
            out.writeBoolean(true);
            out.writeInt(0);
            out.writeInt(1);
            out.writeObject(new MaterializationProbe());
            }

        PermissionInfo info = new PermissionInfo();
        try (ObjectInputStream in = ExternalizableHelper.newFilteredObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()), getClass().getClassLoader()))
            {
            assertThrows(IOException.class, () -> info.readExternal(in));
            }
        assertFalse(MaterializationProbe.wasMaterialized());
        }

    @Test
    public void shouldRejectExternalizableCredentialCustomNumberBeforeReadObject() throws Exception
        {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes))
            {
            out.writeUTF("DistributedCache");
            out.writeObject(signedPermission());
            out.writeBoolean(true);
            out.writeInt(0);
            out.writeInt(1);
            out.writeObject(new NumberProbe());
            }

        PermissionInfo info = new PermissionInfo();
        NumberProbe.reset();
        try (ObjectInputStream in = ExternalizableHelper.newFilteredObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()), getClass().getClassLoader()))
            {
            assertThrows(IOException.class, () -> info.readExternal(in));
            }
        assertFalse(NumberProbe.wasMaterialized());
        }

    private PofReader reader(Binary binSigned, Binary binPrincipals, Binary binCredentials)
            throws IOException
        {
        PofReader reader = mock(PofReader.class);

        when(reader.getPofContext()).thenReturn(pofContext());
        when(reader.readString(0)).thenReturn("DistributedCache");
        when(reader.readBinary(1)).thenReturn(binSigned);
        when(reader.readBoolean(2)).thenReturn(binPrincipals != null || binCredentials != null);
        when(reader.readBinary(3)).thenReturn(binPrincipals);
        when(reader.readBinary(4)).thenReturn(binCredentials);
        return reader;
        }

    private Binary binary(Object o)
        {
        MaterializationProbe.reset();
        NumberProbe.reset();
        return ExternalizableHelper.toBinary(o, new DefaultSerializer());
        }

    private PermissionInfo permissionInfo() throws Exception
        {
        Subject subject = new Subject();

        subject.getPrincipals().add(new PofPrincipal("CN=Manager, OU=MyUnit"));
        return new PermissionInfo(permission(), "DistributedCache", signedPermission(), subject);
        }

    private SignedObject signedPermission() throws Exception
        {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        KeyPair          keyPair   = generator.generateKeyPair();
        Signature        signature = Signature.getInstance("SHA256withRSA");

        return new SignedObject(permission(), keyPair.getPrivate(), signature);
        }

    private ClusterPermission permission()
        {
        return new ClusterPermission("service=DistributedCache,cache=Orders", "join");
        }

    private SimplePofContext pofContext()
        {
        SimplePofContext ctx = new SimplePofContext();

        ctx.registerUserType(1001, PermissionInfo.class, new PortableObjectSerializer(1001));
        return ctx;
        }

    private void assertValidRoundTrip(PermissionInfo info)
        {
        assertNotNull(info.getSignedPermission());
        assertNotNull(info.getSubject());
        assertTrue(info.getSubject().getPrincipals().contains(new PofPrincipal("CN=Manager, OU=MyUnit")));
        }

    /**
     * Serializable probe that records if Java deserialization reaches
     * readObject.
     */
    public static class MaterializationProbe
            implements Serializable
        {
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            MATERIALIZED.set(true);
            in.defaultReadObject();
            }

        static void reset()
            {
            MATERIALIZED.set(false);
            }

        static boolean wasMaterialized()
            {
            return MATERIALIZED.get();
            }

        private static final AtomicBoolean MATERIALIZED = new AtomicBoolean();

        private static final long serialVersionUID = 1L;
        }

    /**
     * Serializable Number probe that records if Java deserialization reaches
     * readObject.
     */
    public static class NumberProbe
            extends Number
            implements Principal
        {
        @Override
        public String getName()
            {
            return "number-probe";
            }

        @Override
        public int intValue()
            {
            return 0;
            }

        @Override
        public long longValue()
            {
            return 0L;
            }

        @Override
        public float floatValue()
            {
            return 0.0f;
            }

        @Override
        public double doubleValue()
            {
            return 0.0d;
            }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
            {
            MATERIALIZED.set(true);
            in.defaultReadObject();
            }

        static void reset()
            {
            MATERIALIZED.set(false);
            }

        static boolean wasMaterialized()
            {
            return MATERIALIZED.get();
            }

        private static final AtomicBoolean MATERIALIZED = new AtomicBoolean();

        private static final long serialVersionUID = 1L;
        }
    }
