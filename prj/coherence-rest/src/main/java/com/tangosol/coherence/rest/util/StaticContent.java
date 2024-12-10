/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;


/**
 * A simple representation of static content, which is used as a pass-through
 * storage format for content other than JSON.
 *
 * @author as  2015.10.25
 */
public class StaticContent
        implements ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public StaticContent()
        {
        }

    /**
     * Construct StaticContent instance.
     *
     * @param binContent  content
     * @param sMediaType  media type
     */
    public StaticContent(Binary binContent, String sMediaType)
        {
        m_binContent = binContent;
        m_sMediaType = sMediaType;
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return content as Binary.
     *
     * @return content
     */
    @JsonIgnore
    public Binary getContent()
        {
        return m_binContent;
        }

    /**
     * Return media type of this content.
     *
     * @return media type
     */
    @JsonProperty
    public String getMediaType()
        {
        return m_sMediaType;
        }

    /**
     * Return content size in bytes.
     *
     * @return content size
     */
    @JsonProperty
    public int getSize()
        {
        return m_binContent.length();
        }

    // ---- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sMediaType = ExternalizableHelper.readSafeUTF(in);

        Binary binContent = new Binary();
        binContent.readExternal(in);

        m_binContent = binContent;
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sMediaType);
        m_binContent.writeExternal(out);
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sMediaType = in.readString(0);
        m_binContent = in.readBinary(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sMediaType);
        out.writeBinary(1, m_binContent);
        }

    // ---- data members ----------------------------------------------------

    protected Binary m_binContent;
    protected String m_sMediaType;
    }
