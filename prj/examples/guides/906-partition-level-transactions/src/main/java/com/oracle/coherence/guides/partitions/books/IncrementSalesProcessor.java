/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.partitions.books;

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
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SubSet;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An {@link InvocableMap.EntryProcessor} to update sales figures.
 *
 * @author Jonathan Knight 2023.01.14
 * @since 22.06.4
 */
public class IncrementSalesProcessor
        extends AbstractEvolvable
        implements InvocableMap.EntryProcessor<SalesId, BookSales, Void>,
                   ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default no-args constructor required for serialization.
     */
    public IncrementSalesProcessor()
        {
        }

    /**
     * Create a {@link IncrementSalesProcessor}.
     *
     * @param eBook  the e-book sales
     * @param audio  the audiobook sales
     * @param paper  the paper book sales
     */
    public IncrementSalesProcessor(long eBook, long audio, long paper)
        {
        this.eBook = eBook;
        this.audio = audio;
        this.paper = paper;
        }

    // ----- EntryProcessor methods -----------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public Void process(InvocableMap.Entry<SalesId, BookSales> entry)
        {
        // update the entry sales data
        BookSales sales;
        if (entry.isPresent())
            {
            // the entry is present
            sales = entry.getValue();
            }
        else
            {
            // The parent entry is not present, so create a new BookSales value
            sales = new BookSales();
            }

        sales.incrementEBookSales(eBook);
        sales.incrementAudioSales(audio);
        sales.incrementPaperSales(paper);
        // set the updated sale value back into the entry so that Coherence updates the cache
        entry.setValue(sales);

        // Obtain a BinaryEntry from the entry being processes
        BinaryEntry<SalesId, BookSales> binaryEntry = entry.asBinaryEntry();
        // Obtain the BackingMapContext for the entry
        BackingMapContext backingMapContext = binaryEntry.getBackingMapContext();
        // Obtain a sorted set of the Binary keys of the parents of the entry being processed
        SortedSet<Binary> parentKeys = getParentKeys(entry.getKey(), backingMapContext);

        // Iterate over the parent keys, enlisting and updating each parent entry
        for (Binary binaryKey : parentKeys)
            {
            InvocableMap.Entry<SalesId, BookSales> parentEntry = backingMapContext.getBackingMapEntry(binaryKey);
            if (parentEntry.isPresent())
                {
                // the parent entry is present
                sales = parentEntry.getValue();
                }
            else
                {
                // The parent entry is not present, so create a new BookSales value
                sales = new BookSales();
                parentEntry.setValue(sales);
                }
            // update the parent sales data
            sales.incrementEBookSales(eBook);
            sales.incrementAudioSales(audio);
            sales.incrementPaperSales(paper);
            // set the updated sale value back into the entry so that Coherence updates the cache
            parentEntry.setValue(sales);
            }

        return null;
        }

    @SuppressWarnings("unchecked")
    private SortedSet<Binary> getParentKeys(SalesId key, BackingMapContext backingMapContext)
        {
        TreeSet<Binary> parents = new TreeSet<>();
        Converter<Binary, SalesId> converter = backingMapContext.getManagerContext().getKeyFromInternalConverter();

        Map.Entry<Binary, Binary> parent = getParent(key, backingMapContext);
        while (parent != null)
            {
            Binary parentKey = parent.getKey();
            parents.add(parentKey);
            parent = getParent(converter.convert(parentKey), backingMapContext);
            }

        return parents;
        }

    @SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
    private Map.Entry<Binary, Binary> getParent(SalesId id, BackingMapContext backingMapContext)
        {
        ObservableMap<Binary, Binary> backingMap = backingMapContext.getBackingMap();
        Map<ValueExtractor, MapIndex> indexMap = backingMapContext.getIndexMap();
        Converter<Binary, SalesId>    converter = backingMapContext.getManagerContext().getKeyFromInternalConverter();

        String bookId = id.getBookId();
        String region = id.getParentRegionCode();

        Filter<?> filter = Filters.equal(BinaryValueExtractor.of(SalesId::getBookId, converter).fromKey(), bookId)
                               .and(Filters.equal(BinaryValueExtractor.of(SalesId::getRegionCode, converter).fromKey(), region));

        Set<Map.Entry<Binary, Binary>> setEntries = InvocableMapHelper.query(backingMap, indexMap, filter, true, false, null);

        // there should only ever be zero or one matching entry
        return setEntries.stream()
                .findFirst()
                .orElse(null);
        }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private SortedSet<Binary> getParentKeysFromIndex(SalesId key, BackingMapContext backingMapContext)
        {
        BackingMapManagerContext managerContext = backingMapContext.getManagerContext();
        Converter<Binary, SalesId> converter = managerContext.getKeyFromInternalConverter();
        Map<ValueExtractor, MapIndex> indexMap = backingMapContext.getIndexMap();

        // Get the two indexes from the index Map
        MapIndex indexBookId = indexMap.get(ValueExtractor.of(SalesId::getBookId).fromKey());
        Map<String, Set<Binary>> indexBookIdContents = indexBookId.getIndexContents();
        MapIndex indexRegion = indexMap.get(ValueExtractor.of(SalesId::getRegionCode).fromKey());
        Map<String, Set<Binary>> indexRegionContents = indexRegion.getIndexContents();

        // Obtain the set of Binary keys that have the required BookId
        Set<Binary> setBookId = indexBookIdContents.get(key.getBookId());

        SortedSet<Binary> parents = new TreeSet<>();

        SalesId parent = key;
        while (parent != null)
            {
            String region = parent.getParentRegionCode();
            if (region == null)
                {
                // we're finished, the key has no parent region
                break;
                }
            // Obtain the set of Binary keys that have the parent region
            // and wrap them in a Coherence SubSet, so we do not mutate the real set
            Set<Binary> setRegion = new SubSet<>(indexRegionContents.get(region));
            // remove any values from the set that are not in the BookId key set
            setRegion.retainAll(setBookId);
            // setRegion "should" now contain zero or one entry
            Binary binaryKey = setRegion.stream().findFirst().orElse(null);
            if (binaryKey == null)
                {
                // we're finished, there was no parent
                break;
                }
            // add the parent to the result set
            parents.add(binaryKey);
            // set the next parent
            parent = converter.convert(binaryKey);
            }

        return parents;
        }

    // ----- serialization methods ------------------------------------------

    @Override
    public int getImplVersion()
        {
        return IMPLEMENTATION_VERSION;
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        eBook = in.readLong();
        audio = in.readLong();
        paper = in.readLong();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeLong(eBook);
        out.writeLong(audio);
        out.writeLong(paper);
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        eBook = in.readLong(0);
        audio = in.readLong(1);
        paper = in.readLong(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, eBook);
        out.writeLong(1, audio);
        out.writeLong(2, paper);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The evolvable POF implementation version of this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    /**
     * The number of e-books sold.
     */
    private long eBook;

    /**
     * The number of audiobooks sold.
     */
    private long audio;

    /**
     * The number of paper sold.
     */
    private long paper;
    }
