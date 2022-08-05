/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.model;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * A class to represent a simple order for a product by a customer.
 */
public class Order
        extends AbstractEvolvable
        implements ExternalizableLite, EvolvablePortableObject
    {
    public Order()
        {
        }

    public Order(int id, int customerId, int itemId, BigDecimal itemPrice, int quantity)
        {
        this.id = id;
        this.customerId = customerId;
        this.itemId = itemId;
        this.itemPrice = itemPrice;
        this.quantity = quantity;
        }

    public int getId()
        {
        return id;
        }

    public int getCustomerId()
        {
        return customerId;
        }

    public int getItemId()
        {
        return itemId;
        }

    public BigDecimal getItemPrice()
        {
        return itemPrice;
        }

    public int getQuantity()
        {
        return quantity;
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        id = in.readInt();
        customerId = in.readInt();
        itemId = in.readInt();
        itemPrice = ExternalizableHelper.readBigDecimal(in);
        quantity = in.readInt();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeInt(id);
        out.writeInt(customerId);
        out.writeInt(itemId);
        ExternalizableHelper.writeBigDecimal(out, itemPrice);
        out.writeInt(quantity);
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        id = in.readInt(0);
        customerId = in.readInt(1);
        itemId = in.readInt(2);
        itemPrice = in.readBigDecimal(3);
        quantity = in.readInt(4);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, id);
        out.writeInt(1, customerId);
        out.writeInt(2, itemId);
        out.writeBigDecimal(3, itemPrice);
        out.writeInt(4, quantity);
        }

    @Override
    public int getImplVersion()
        {
        return 1;
        }

    // ----- data members ---------------------------------------------------

    private int id;

    private int customerId;

    private int itemId;

    private BigDecimal itemPrice;

    private int quantity;
    }
