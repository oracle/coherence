/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.partitions.bank;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * An {@link com.tangosol.util.InvocableMap.EntryProcessor} to transfer
 * an amount between two accounts for the same customer.
 * <p>
 * This EntryProcessor is invoked against the customer cache and uses
 * a partition local transaction to adjust the balance of the two accounts.
 * This ensures that the update of the accounts is atomic and idempotent
 * so that if there is a failure causing Coherence to re-execute the
 * EntryProcessor, the adjustment and result will still be correct.
 * <p>
 * To keep the code simple there is no error handling, for example we do not
 * check the accounts actually exist.
 *
 * @author Jonathan Knight  2023.01.14
 * @since 22.06.4
 */
public class TransferProcessor
        extends AbstractEvolvable
        implements InvocableMap.EntryProcessor<CustomerId, Customer, Void>,
                   ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * A default no-args constructor required for serialization.
     */
    public TransferProcessor()
        {
        }

    /**
     * Create a {@link TransferProcessor}.
     *
     * @param sourceAccount       the identifier of the account to debit
     * @param destinationAccount  the identifier of the account to credit
     * @param amount              the amount to transfer
     */
    public TransferProcessor(AccountId sourceAccount, AccountId destinationAccount, BigDecimal amount)
        {
        this.sourceAccountId      = sourceAccount;
        this.destinationAccountId = destinationAccount;
        this.amount               = amount;
        }

    // ----- InvocableMap.EntryProcessor methods ----------------------------

    @Override
    @SuppressWarnings("unchecked")
    public Void process(InvocableMap.Entry<CustomerId, Customer> entry)
        {
        // Convert the entry to a BinaryEntry
        BinaryEntry<CustomerId, Customer> binaryEntry = entry.asBinaryEntry();
        // Obtain the manager context
        BackingMapManagerContext context = binaryEntry.getContext();

        // Obtain the converter to use to convert the account identifiers
        // into Coherence internal serialized binary format
        // It is important to use the correct key converter for this conversion
        Converter<AccountId, Binary> keyConverter = context.getKeyToInternalConverter();

        // Obtain the backing map context for the accounts cache
        BackingMapContext accountsContext = context.getBackingMapContext(BankCacheNames.ACCOUNTS);

        // Convert the source account id to a binary key and obtain the cache entry for the source account
        Binary sourceKey = keyConverter.convert(sourceAccountId);
        InvocableMap.Entry<AccountId, Account> sourceEntry = accountsContext.getBackingMapEntry(sourceKey);
        // Convert the destination account id to a binary key and obtain the cache entry for the destination account
        Binary destinationKey = keyConverter.convert(destinationAccountId);
        InvocableMap.Entry<AccountId, Account> destinationEntry = accountsContext.getBackingMapEntry(destinationKey);

        // adjust the values for the two accounts
        Account sourceAccount = sourceEntry.getValue();
        sourceAccount.adjustBalance(amount.negate());
        // set the updated source account back into the entry so that the cache is updated
        sourceEntry.setValue(sourceAccount);

        Account destinationAccount = destinationEntry.getValue();
        destinationAccount.adjustBalance(amount);
        // set the updated destination account back into the entry so that the cache is updated
        destinationEntry.setValue(destinationAccount);

        return null;
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        sourceAccountId = ExternalizableHelper.readObject(in);
        destinationAccountId = ExternalizableHelper.readObject(in);
        amount = ExternalizableHelper.readBigDecimal(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, sourceAccountId);
        ExternalizableHelper.writeObject(out, destinationAccountId);
        ExternalizableHelper.writeBigDecimal(out, amount);
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
        sourceAccountId = in.readObject(0);
        destinationAccountId = in.readObject(1);
        amount = in.readBigDecimal(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, sourceAccountId);
        out.writeObject(1, destinationAccountId);
        out.writeBigDecimal(2, amount);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The evolvable portable object implementation version for this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The account to debit.
     */
    AccountId sourceAccountId;

    /**
     * The account to credit.
     */
    AccountId destinationAccountId;

    /**
     * The amount to transfer.
     */
    BigDecimal amount;
    }
