/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.partitions.books;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.cache.KeyAssociation;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/**
 * An identifier for book sales by region.
 *
 * @author Jonathan Knight 2023.01.14
 * @since 22.06.4
 */
public class SalesId
        implements ExternalizableLite, PortableObject, KeyAssociation<String>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * A default no-args constructor required for serialization.
     */
    public SalesId()
        {
        }

    /**
     * Create a sales identifier.
     *
     * @param bookId            the book identifier
     * @param regionCode        the region identifier
     * @param parentRegionCode  the parent region identifier, or {@code null}
     *                          if this is a top level region
     */
    public SalesId(String bookId, String regionCode, String parentRegionCode)
        {
        this.bookId = bookId;
        this.regionCode = regionCode;
        this.parentRegionCode = parentRegionCode;
        }

    // ----- KeyAssociation methods -----------------------------------------

    @Override
    public String getAssociatedKey()
        {
        return bookId;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the book identifier.
     *
     * @return the book identifier
     */
    public String getBookId()
        {
        return bookId;
        }

    /**
     * Return the region identifier.
     *
     * @return the region identifier
     */
    public String getRegionCode()
        {
        return regionCode;
        }

    /**
     * Return the parent region identifier, or {@code null}
     * if this is a top level region.
     *
     * @return the parent region identifier, or {@code null}
     *         if this is a top level region
     */
    public String getParentRegionCode()
        {
        return parentRegionCode;
        }

    // ----- Object methods -------------------------------------------------

    // Coherence key classes must properly implement hashCode() using
    // all the fields in the class
    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        SalesId salesId = (SalesId) o;
        return Objects.equals(bookId, salesId.bookId)
                && Objects.equals(regionCode, salesId.regionCode)
                && Objects.equals(parentRegionCode, salesId.parentRegionCode);
        }

    // Coherence key classes must properly implement hashCode() using
    // all the fields in the class
    @Override
    public int hashCode()
        {
        return Objects.hash(bookId, regionCode, parentRegionCode);
        }

    @Override
    public String toString()
        {
        return "SalesId{" +
                "bookId='" + bookId + '\'' +
                ", regionCode='" + regionCode + '\'' +
                ", parentRegionCode='" + parentRegionCode + '\'' +
                '}';
        }

    // ----- serialization methods ------------------------------------------


    @Override
    public void readExternal(DataInput in) throws IOException
        {
        bookId = ExternalizableHelper.readSafeUTF(in);
        regionCode = ExternalizableHelper.readSafeUTF(in);
        parentRegionCode = ExternalizableHelper.readSafeUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, bookId);
        ExternalizableHelper.writeSafeUTF(out, regionCode);
        ExternalizableHelper.writeSafeUTF(out, parentRegionCode);
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        bookId = in.readString(0);
        regionCode = in.readString(1);
        parentRegionCode = in.readString(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, bookId);
        out.writeString(0, regionCode);
        out.writeString(0, parentRegionCode);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The identifier for the ook
     */
    private String bookId;

    /**
     * The region code for the sales data.
     */
    private String regionCode;

    /**
     * The parent region code, or {@code null} if this is a top level region.
     */
    private String parentRegionCode;
    }
