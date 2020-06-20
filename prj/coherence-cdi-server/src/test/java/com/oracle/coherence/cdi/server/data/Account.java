/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server.data;


import com.oracle.coherence.cdi.Injectable;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import java.io.IOException;
import java.io.Serializable;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;


/**
 * A simple class representing a bank account that can be used in tests requiring
 * test data.
 *
 * @author Aleks Seovic  2020.04.08
 */
@Dependent
public class Account
        implements PortableObject, Serializable, Injectable
    {
    private String id;
    private long balance;

    @Inject
    private Event<Overdrawn> accountOverdrawnEvent;

    /**
     * Default constructor for serialization.
     */
    public Account()
        {
        }

    /**
     * Create a {@link Account}.
     *
     * @param id       account ID
     * @param balance  initial balance
     */
    public Account(String id, long balance)
        {
        this.id = id;
        this.balance = balance;
        }

    public String getId()
        {
        return id;
        }

    public long getBalance()
        {
        return balance;
        }

    public long withdraw(long amount)
        {
        balance -= amount;
        if (balance < 0)
            {
            accountOverdrawnEvent.fire(new Overdrawn(balance));
            }
        return balance;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        id = in.readString(0);
        balance = in.readLong(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, id);
        out.writeLong(1, balance);
        }

    public static class Overdrawn
        {
        private long amount;

        Overdrawn(long amount)
            {
            this.amount = Math.abs(amount);
            }

        public long getAmount()
            {
            return amount;
            }

        public String toString()
            {
            return "Overdrawn{" +
                   "amount=" + amount +
                   '}';
            }
        }
    }
