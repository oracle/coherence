/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;


/**
* Filter which truncates the results of another filter. This filter is a
* mutable object that is modified by the query processor. Clients are supposed
* to hold a reference to this filter and repetitively pass it to query
* methods after setting a desired page context calling
* {@link #setPage(int nPage)}, {@link #nextPage()}, or {@link #previousPage}.
*
* @author gg 2002.12.06
*/
public class LimitFilter<T>
        extends    AbstractQueryRecorderFilter<T>
        implements EntryFilter<Object, T>, IndexAwareFilter<Object, T>,
                   ExternalizableLite, PortableObject, Cloneable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public LimitFilter()
        {
        }

    /**
    * Construct a limit filter.
    *
    * @param filter     the filter whose results this Filter truncates
    * @param cPageSize  the page size
    */
    public LimitFilter(Filter<T> filter, int cPageSize)
        {
        if (filter == null)
            {
            throw new IllegalArgumentException("Filter must be specified");
            }

        if (filter instanceof LimitFilter)
            {
            throw new UnsupportedOperationException("Limit of limit");
            }

        m_filter = filter;

        setPageSize(cPageSize);
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(T o)
        {
        return m_filter.evaluate(o);
        }


    // ----- EntryFilter interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluateEntry(Map.Entry entry)
        {
        return InvocableMapHelper.evaluateEntry(m_filter, entry);
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        Filter<T> filter = m_filter;
        return filter instanceof IndexAwareFilter
            ? ((IndexAwareFilter) filter).calculateEffectiveness(mapIndexes, setKeys)
            : ExtractorFilter.calculateIteratorEffectiveness(setKeys.size());
        }

    /**
    * {@inheritDoc}
    */
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        Filter<T> filter = m_filter;
        if (filter instanceof IndexAwareFilter)
            {
            return ((IndexAwareFilter) filter).applyIndex(mapIndexes, setKeys);
            }
        else
            {
            return filter;
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the Filter whose results are truncated by this filter.
    *
    * @return the filter whose results are truncated by this filter
    */
    public Filter<T> getFilter()
        {
        return m_filter;
        }

    /**
    * Obtain the page size (expressed as a number of entries per page).
    *
    * @return the page size
    */
    public int getPageSize()
        {
        return m_cPageSize;
        }

    /**
    * Set the page size (expressed as a number of entries per page).
    *
    * @param cPageSize  the page size
    */
    public void setPageSize(int cPageSize)
        {
        if (cPageSize <= 0)
            {
            throw new IllegalArgumentException("Invalid page size");
            }
        m_cPageSize = cPageSize;
        }

    /**
    * Obtain a current page number (zero-based).
    *
    * @return the page number
    */
    public int getPage()
        {
        return m_nPage;
        }

    /**
    * Set the page number (zero-based). Setting the page number to zero will
    * reset the filter's state.
    *
    * @param nPage  the page number
    */
    public void setPage(int nPage)
        {
        if (nPage < 0)
            {
            throw new IllegalArgumentException("Negative page: " + nPage);
            }

        if (nPage == 0) // "reset"
            {
            setTopAnchor   (null);
            setBottomAnchor(null);
            setCookie(null);
            }
        else
            {
            int nPageCurr = m_nPage;
            if (nPage == nPageCurr + 1)
                {
                setTopAnchor(getBottomAnchor());
                setBottomAnchor(null);
                }
            else if (nPage == nPageCurr - 1)
                {
                setBottomAnchor(getTopAnchor());
                setTopAnchor   (null);
                }
            else if (nPage != nPageCurr)
                {
                setTopAnchor   (null);
                setBottomAnchor(null);
                }
            }
        m_nPage = nPage;
        }

    /**
    * Obtain the Comparator used to partition the entry values into pages.
    * <p>
    * This method is intended to be used only by query processors. Clients
    * should not modify the content of this property.
    *
    * @return the Comparator object
    */
    public Comparator getComparator()
        {
        return m_comparator;
        }

    /**
    * Set the Comparator used to partition the values into pages.
    * <p>
    * This method is intended to be used only by query processors. Clients
    * should not modify the content of this property.
    *
    * @param comparator Comparator object
    */
    public void setComparator(Comparator comparator)
        {
        m_comparator = comparator;
        }

    /**
    * Obtain the top anchor object, which is the last value object
    * on a previous page.
    * <p>
    * This method is intended to be used only by query processors. Clients
    * should not modify the content of this property.
    *
    * @return top anchor object
    */
    public Object getTopAnchor()
        {
        return m_oAnchorTop;
        }

    /**
    * Set the top anchor object.
    * <p>
    * This method is intended to be used only by query processors. Clients
    * should not modify the content of this property.
    *
    * @param oAnchor the top anchor object
    */
    public void setTopAnchor(Object oAnchor)
        {
        m_oAnchorTop = oAnchor;
        }

    /**
    * Obtain the bottom anchor object, which is the last value object
    * on the current page.
    * <p>
    * This method is intended to be used only by query processors. Clients
    * should not modify the content of this property.
    *
    * @return bottom anchor object
    */
    public Object getBottomAnchor()
        {
        return m_oAnchorBottom;
        }

    /**
    * Set the bottom anchor object.
    * <p>
    * This method is intended to be used only by query processors. Clients
    * should not modify the content of this property.
    *
    * @param oAnchor the bottom anchor object
    */
    public void setBottomAnchor(Object oAnchor)
        {
        m_oAnchorBottom = oAnchor;
        }

    /**
    * Obtain the cookie object.
    * <p>
    * This method is intended to be used only by query processors. Clients
    * should not modify the content of this property.
    *
    * @return cookie object
    */
    public Object getCookie()
        {
        return m_oCookie;
        }

    /**
    * Set the cookie object.
    * <p>
    * This method is intended to be used only by query processors. Clients
    * should not modify the content of this property.
    *
    * @param oCookie the cookie object
    */
    public void setCookie(Object oCookie)
        {
        m_oCookie = oCookie;
        }

    /**
    *  Return the number of members query concurrently.
    *
    * @return batch size
    */
    public int getBatchSize()
        {
        return m_cBatch;
        }

    /**
    * Set the number of members query concurrently.
    *
    * @param cBatch  batch size
    *
    * @return the set batch size
    */
    public int setBatchSize(int cBatch)
        {
        return m_cBatch = cBatch;
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Switch to the next page.
    */
    public void nextPage()
        {
        setPage(getPage() + 1);
        }

    /**
    * Switch to the previous page.
    */
    public void previousPage()
        {
        setPage(getPage() - 1);
        }

    /**
    * Extract a subset of the specified array to fit the filter's parameters
    * (i.e. page size and page number). If this filter has a comparator, the
    * specified array is presumed to be sorted accordingly.
    * <p>
    * The returned array is guaranteed to iterate exactly in the same
    * order as the original array.
    *
    * @param aEntry  an original array of entries
    *
    * @return an array of entries extracted accordingly to the filter
    *         parameters
    */
    public Object[] extractPage(Object[] aEntry)
        {
        int        cEntries   = aEntry.length;
        int        cPageSize  = getPageSize();
        Comparator comparator = getComparator();

        // no reason to optimize for a small result set
        if (comparator != null && cEntries > cPageSize)
            {
            Object oAnchorTop    = getTopAnchor();
            Object oAnchorBottom = getBottomAnchor();

            if (oAnchorTop != null)
                {
                // if both AnchorTop and AnchorBottom are present;
                // it's a repetitive request for the same page

                int ofAnchor = Arrays.binarySearch(aEntry,
                    new SimpleMapEntry(null, oAnchorTop), comparator);
                int nShift  = oAnchorBottom == null ? 1 : 0;
                int ofFirst = ofAnchor >= 0 ? ofAnchor + nShift : -ofAnchor - 1;
// com.tangosol.net.CacheFactory.log("\n### optimize: " + cPageSize + " out of " + cEntries + " at " + ofFirst, 3);
                if (ofFirst < cEntries)
                    {
                    return extractPage(new SimpleEnumerator(
                        aEntry, ofFirst, Math.min(cPageSize, cEntries - ofFirst)));
                    }
                else
                    {
                    return new Object[0];
                    }
                }
            else if (oAnchorBottom != null)
                {
                int ofAnchor = Arrays.binarySearch(aEntry,
                    new SimpleMapEntry(null, oAnchorBottom), comparator);
                int ofAfterLast = ofAnchor >= 0 ? ofAnchor : -ofAnchor - 1;

                if (ofAfterLast > 0)
                    {
                    int ofFirst = Math.max(0, ofAfterLast - cPageSize);
// com.tangosol.net.CacheFactory.log("\n### optimize: " + cPageSize + " out of " + (ofAfterLast - ofFirst) + " at " + ofFirst);
                    return extractPage(new SimpleEnumerator(
                        aEntry, ofFirst, Math.min(cPageSize, ofAfterLast - ofFirst)));
                    }
                else
                    {
                    return new Object[0];
                    }
                }
            }

        return extractPage(new SimpleEnumerator(aEntry));
        }

    /**
    * Extract a subset of the specified set to fit the filter's parameters
    * (i.e. page size and page number). If this filter has a comparator, the
    * specified Set is presumed to be sorted accordingly.
    * <p>
    * The returned set is guaranteed to iterate exactly in the same order
    * as the original set.
    *
    * @param set  an original set of entries
    *
    * @return a set of entries extracted accordingly to the filter
    *         parameters
    */
    public Set extractPage(Set set)
        {
        // potential TODO: optimize if (set instanceof SortedSet)
        return new ImmutableArrayList(extractPage(set.iterator()));
        }

    /**
    * Extract a subset of the specified iterator to fit the filter's parameters
    * (i.e. page size and page number). The returned array is guaranteed
    * to iterate exactly in the same order as the original iterator.
    *
    * @param iter  an original entry iterator
    *
    * @return an array of entries extracted accordingly to the filter
    *         parameters
    */
    public Object[] extractPage(Iterator iter)
        {
        int        cPageSize     = getPageSize();
        Comparator comparator    = getComparator();
        Object     oAnchorTop    = getTopAnchor();
        Object     oAnchorBottom = getBottomAnchor();
        Object[]   aoEntry       = new Object[cPageSize];
        int        iEntry        = 0;

        if (comparator == null ||
                oAnchorTop == null && oAnchorBottom == null)
            {
            int cSkip = getPage()*cPageSize;

            // THIS IS A HACK: reconsider
            if (comparator == null && oAnchorTop instanceof Integer)
                {
                cSkip = ((Integer) oAnchorTop).intValue();
                }

            while (iter.hasNext())
                {
                Object oEntry = iter.next();

                if (--cSkip >= 0)
                    {
                    continue;
                    }

                aoEntry[iEntry] = oEntry;

                if (++iEntry == cPageSize)
                    {
                    break;
                    }
                }

            if (iEntry < cPageSize)
                {
                // last page is not full
                int      cSize = iEntry;
                Object[] ao    = new Object[cSize];

                if (cSize > 0)
                    {
                    System.arraycopy(aoEntry, 0, ao, 0, cSize);
                    }
                aoEntry = ao;
                }
            }
        else
            {
            boolean   fHeading    = oAnchorTop != null || oAnchorBottom == null;
            boolean   fInclusive  = oAnchorTop != null && oAnchorBottom != null;
            boolean   fSkip       = fHeading;
            boolean   fWrap       = false;
            Map.Entry entryTop    = new SimpleMapEntry(null, oAnchorTop);
            Map.Entry entryBottom = new SimpleMapEntry(null, oAnchorBottom);

            while (iter.hasNext())
                {
                Map.Entry entry = (Map.Entry) iter.next();

                if (fSkip)
                    {
                    int nCompare = comparator.compare(entry, entryTop);

                    fSkip = fInclusive ? (nCompare < 0) : (nCompare <= 0);
                    if (fSkip)
                        {
                        continue;
                        }
                    }

                if (fHeading)
                    {
                    aoEntry[iEntry] = entry;

                    if (++iEntry == cPageSize)
                        {
                        break;
                        }
                    }
                else
                    {
                    if (comparator.compare(entry, entryBottom) >= 0)
                        {
                        break;
                        }

                    aoEntry[iEntry] = entry;

                    if (++iEntry == cPageSize)
                        {
                        fWrap  = true;
                        iEntry = 0;
                        }
                    }
                }

            if (fWrap)
                {
                Object[] ao = new Object[cPageSize];

                System.arraycopy(aoEntry, iEntry, ao, 0, cPageSize - iEntry);
                System.arraycopy(aoEntry, 0, ao, cPageSize - iEntry, iEntry);
                aoEntry = ao;
                }
            else if (iEntry < cPageSize)
                {
                // last page is not full
                int      cSize = iEntry;
                Object[] ao    = new Object[cSize];

                if (cSize > 0)
                    {
                    System.arraycopy(aoEntry, 0, ao, 0, cSize);
                    }
                aoEntry = ao;
                }
            }
        return aoEntry;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        StringBuilder sb = new StringBuilder("LimitFilter: (");
        sb.append(m_filter)
          .append(" [pageSize=")
          .append(m_cPageSize)
          .append(", pageNum=")
          .append(m_nPage);

        if (m_comparator != null)
            {
            sb.append(", top=")
              .append(m_oAnchorTop)
              .append(", bottom=")
              .append(m_oAnchorBottom)
              .append(", comparator=")
              .append(m_comparator);
            }

        sb.append("])");
        return sb.toString();
        }

    /**
    * Clone this filter.
    *
    * @return a clone of this filter
    */
    public Object clone()
        {
        try
            {
            return super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter        = (Filter<T>) readObject(in);
        m_cPageSize     = readInt(in);
        m_nPage         = readInt(in);
        m_comparator    = (Comparator) readObject(in);
        m_oAnchorTop    = readObject(in);
        m_oAnchorBottom = readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_filter);
        writeInt   (out, m_cPageSize);
        writeInt   (out, m_nPage);
        writeObject(out, m_comparator);
        writeObject(out, m_oAnchorTop);
        writeObject(out, m_oAnchorBottom);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_filter        = (Filter<T>) in.readObject(0);
        m_cPageSize     = in.readInt(1);
        m_nPage         = in.readInt(2);
        m_comparator    = (Comparator) in.readObject(3);
        m_oAnchorTop    = in.readObject(4);
        m_oAnchorBottom = in.readObject(5);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        out.writeInt   (1, m_cPageSize);
        out.writeInt   (2, m_nPage);
        out.writeObject(3, m_comparator);
        out.writeObject(4, m_oAnchorTop);
        out.writeObject(5, m_oAnchorBottom);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The Filter whose results are truncated by this filter.
    */
    @JsonbProperty("filter")
    private Filter<T> m_filter;

    /**
    * The number of entries per page.
    */
    @JsonbProperty("pageSize")
    private int m_cPageSize;

    /**
    * The page number.
    */
    @JsonbProperty("page")
    private int m_nPage;

    /**
    * The comparator used to partition the entry values into pages.
    */
    @JsonbProperty("comparator")
    private Comparator m_comparator;

    /**
    * The top anchor object (the last object on a previous page).
    */
    @JsonbProperty("topAnchor")
    private Object m_oAnchorTop;

    /**
    * The bottom anchor object (the last object on the current page).
    */
    @JsonbProperty("bottomAnchor")
    private Object m_oAnchorBottom;

    /**
    * The cookie object used by the query processors to store a transient
    * state of the request (on a client side).
    */
    private transient Object m_oCookie;

    /**
    * The number of members query concurrently.
    */
    private transient int m_cBatch;
    }
