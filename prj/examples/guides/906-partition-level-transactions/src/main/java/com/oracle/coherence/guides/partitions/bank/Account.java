/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.partitions.bank;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * A simple representation of a bank account.
 * <p>
 * This class implements {@link com.tangosol.io.pof.EvolvablePortableObject} by
 * extending {@link AbstractEvolvable} so that the class can be changed in a
 * backwards compatible to support rolling upgrades.
 *
 * @author Jonathan Knight 2023.01.14
 * @since 22.06.4
 */
public class Account
        extends AbstractEvolvable
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * A default no-args constructor required for serialization.
     */
    public Account()
        {
        }

    /**
     * Create an account.
     *
     * @param id       the account's identifier
     * @param balance  the account's balance
     */
    public Account(AccountId id, BigDecimal balance)
        {
        this.id = id;
        this.balance = balance;
        }

    /**
     * Returns the account's identifier.
     *
     * @return the account's identifier
     */
    public AccountId getId()
        {
        return id;
        }

    /**
     * Returns the account's balance.
     *
     * @return the account's balance
     */
    public BigDecimal getBalance()
        {
        return balance;
        }

    /**
     * Adjust the balance of this account.
     *
     * @param amount  the amount to adjust the balance by
     */
    public void adjustBalance(BigDecimal amount)
        {
        balance = balance.add(amount);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        id = ExternalizableHelper.readObject(in);
        balance = ExternalizableHelper.readBigDecimal(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, id);
        ExternalizableHelper.writeBigDecimal(out, balance);
        }

    // ----- AbstractEvolvable / PortableObject methods ---------------------

    @Override
    public int getImplVersion()
        {
        return IMPLEMENTATION_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        id = in.readObject(0);
        balance = in.readBigDecimal(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, id);
        out.writeBigDecimal(1, balance);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The evolvable portable object implementation version for this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The account's identifier.
     */
    private AccountId id;

    /**
     * The account balance.
     */
    private BigDecimal balance;
    }
