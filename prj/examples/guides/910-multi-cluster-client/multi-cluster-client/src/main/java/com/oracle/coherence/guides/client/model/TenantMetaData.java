/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client.model;


import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import javax.json.bind.annotation.JsonbProperty;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/**
 * Simple tenant meta-data.
 */
public class TenantMetaData
        implements ExternalizableLite, PortableObject {

    /**
     * The name of the tenant.
     */
    @JsonbProperty("tenant")
    private String tenant;

    /**
     * The type of Coherence client to use
     */
    @JsonbProperty("type")
    private String type;

    /**
     * The host name of the cluster.
     */
    @JsonbProperty("hostName")
    private String hostName;

    /**
     * The port to connect to the cluster on.
     */
    @JsonbProperty("port")
    private int port;

    /**
     * The name of the serializer to use
     */
    @JsonbProperty("serializer")
    private String serializer;

    /**
     * A default constructor required by Coherence serialization.
     */
    public TenantMetaData() {
    }

    /**
     * Create tenant meta-data.
     *
     * @param tenant      the name of the tenant
     * @param type        the type of Coherence client to use
     * @param hostName    the host name of the cluster
     * @param port        the port to use to connect to the cluster
     * @param serializer  the name of the serializer to use
     */
    public TenantMetaData(String tenant, String type, String hostName, int port, String serializer) {
        this.tenant = tenant;
        this.type = type;
        this.hostName = hostName;
        this.port = port;
        this.serializer = serializer;
    }

    /**
     * Returns the tenant name.
     *
     * @return the tenant name
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * Returns the type of Coherence client.
     *
     * @return the type of Coherence client
     */
    public String getType() {
        return type;
    }

    /**
     * Return {@code true} if this tenant uses Coherence Extend,
     * or {@code false} if the tenant uses gRPC.
     *
     * @return {@code true} if this tenant uses Coherence Extend,
     *         or {@code false} if the tenant uses gRPC
     */
    public boolean isExtend() {
        return "extend".equalsIgnoreCase(type);
    }

    /**
     * Set the type of Coherence client.
     *
     * @param type  the type of Coherence client
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the host name of the Coherence cluster.
     *
     * @return the host name of the Coherence cluster
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Set the host name of the Coherence cluster.
     *
     * @param hostName  the host name of the Coherence cluster
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Returns the port name to connect to the cluster on.
     *
     * @return the port name to connect to the cluster on
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the port name to connect to the cluster on.
     *
     * @param port  the port name to connect to the cluster on
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the name of the serializer to use.
     *
     * @return the name of the serializer to use
     */
    public String getSerializer() {
        return serializer == null || serializer.isBlank()
               ? "java" : serializer;
    }

    /**
     * Sets the name of the serializer to use.
     *
     * @param serializer  the name of the serializer to use
     */
    public void setSerializer(String serializer) {
        this.serializer = serializer;
    }

    /**
     * Deserialize a {@link TenantMetaData} when Java serialization is being used.
     *
     * @param in  the DataInput stream to read data from in order to restore
     *            the state of this object
     *
     * @throws IOException if serialization fails
     */
    @Override
    public void readExternal(DataInput in) throws IOException {
        tenant = ExternalizableHelper.readSafeUTF(in);
        type = ExternalizableHelper.readSafeUTF(in);
        hostName = ExternalizableHelper.readSafeUTF(in);
        port = in.readInt();
        serializer = ExternalizableHelper.readSafeUTF(in);
    }

    /**
     * Serialize a {@link TenantMetaData} when Java serialization is being used.
     *
     * @param out  the DataOutput stream to write the state of this object to
     *
     * @throws IOException if serialization fails
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException {
        ExternalizableHelper.writeSafeUTF(out, tenant);
        ExternalizableHelper.writeSafeUTF(out, type);
        ExternalizableHelper.writeSafeUTF(out, hostName);
        out.writeInt(port);
        ExternalizableHelper.writeSafeUTF(out, serializer);
    }

    /**
     * Deserialize a {@link TenantMetaData} when POF serialization is being used.
     *
     * @param in  the PofReader from which to read the object's state
     *
     * @throws IOException if serialization fails
     */
    @Override
    public void readExternal(PofReader in) throws IOException {
        tenant = in.readString(0);
        type = in.readString(1);
        hostName = in.readString(2);
        port = in.readInt(3);
        serializer = in.readString(4);
    }

    /**
     * Serialize a {@link TenantMetaData} when POF serialization is being used.
     *
     * @param out  the PofWriter to which to write the object's state
     *
     * @throws IOException if serialization fails
     */
    @Override
    public void writeExternal(PofWriter out) throws IOException {
        out.writeString(0, tenant);
        out.writeString(1, type);
        out.writeString(2, hostName);
        out.writeInt(3, port);
        out.writeString(4, serializer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TenantMetaData that = (TenantMetaData) o;
        return port == that.port && Objects.equals(tenant, that.tenant)
               && Objects.equals(type, that.type)
               && Objects.equals(hostName, that.hostName)
               && Objects.equals(serializer, that.serializer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenant, type, hostName, port, serializer);
    }

    @Override
    public String toString() {
        return "TenantMetaData{" +
               "tenant='" + tenant + '\'' +
               ", type='" + type + '\'' +
               ", hostName='" + hostName + '\'' +
               ", port=" + port +
               ", serializer=" + serializer +
               '}';
    }
}
