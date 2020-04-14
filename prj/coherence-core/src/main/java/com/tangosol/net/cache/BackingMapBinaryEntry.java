/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.io.Serializer;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;


/**
* An implementation of the BinaryEntry interface that is based on specified
* binary key, binary value and BackingMapManagerContext. Analogous to the
* {@link com.tangosol.util.MapTrigger.Entry}, it represents a pending change to
* an Entry that is about to committed to the underlying Map. For example,
* the original binary value of null indicates pending insert (a non-existing
* entry), while the binary value of null represents a pending remove operation.
* <p>
* It is currently only used by the ReadWriteBackingMap to communicate with
* the BinaryEntryStore.
* <p>
* This implementation is not thread safe.
*
* @author gg 2009.11.22
*/
public class BackingMapBinaryEntry
        extends Base
        implements BinaryEntry, MapTrigger.Entry
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a BackingMapBinaryEntry with the specified binary key, values
    * and BackingMapManagerContext.
    *
    * @param binKey        the Binary key
    * @param binValue      the Binary value; could be null representing a
    *                      non-existing entry
    * @param binValueOrig  an original Binary value; could be null representing
    *                      an insert operation
    * @param ctx           a BackingMapManagerContext
    */
    public BackingMapBinaryEntry(Binary binKey, Binary binValue,
            Binary binValueOrig, BackingMapManagerContext ctx)
        {
        this(binKey, binValue, binValueOrig, CacheMap.EXPIRY_DEFAULT, ctx);
        }

    /**
    * Construct a BackingMapBinaryEntry with the specified binary key, values
    * and BackingMapManagerContext.
    *
    * @param binKey        the Binary key
    * @param binValue      the Binary value; could be null representing a
    *                      non-existing entry
    * @param binValueOrig  an original Binary value; could be null representing
    *                      an insert operation
    * @param cDelay        the expiry delay
    * @param ctx           a BackingMapManagerContext
    */
    public BackingMapBinaryEntry(Binary binKey, Binary binValue,
            Binary binValueOrig, long cDelay, BackingMapManagerContext ctx)
        {
        azzert(binKey != null, "Null key");

        m_ctx          = ctx;
        m_binKey       = binKey;
        m_binValue     = binValue;
        m_binValueOrig = binValueOrig;
        m_ldtExpiry    = calculateExpiry(cDelay);
        }


    // ----- Map.Entry interface --------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Binary getBinaryKey()
        {
        return m_binKey;
        }

    /**
    * {@inheritDoc}
    */
    public Binary getBinaryValue()
        {
        Binary binValue = m_binValue;
        if (binValue == NO_VALUE)
            {
            Object oValue = m_oValue;
            azzert(oValue != NO_VALUE);

            m_binValue = binValue = (Binary) m_ctx
                    .getValueToInternalConverter().convert(oValue);
            }
        return binValue;
        }

    /**
    * {@inheritDoc}
    */
    public Binary getOriginalBinaryValue()
        {
        return m_binValueOrig;
        }

    /**
    * {@inheritDoc}
    */
    public Object getKey()
        {
        Object oKey = m_oKey;
        if (oKey == NO_VALUE)
            {
            m_oKey = oKey = m_ctx.getKeyFromInternalConverter().convert(
                getBinaryKey());
            }
        return oKey;
        }

    /**
    * {@inheritDoc}
    */
    public Object getValue()
        {
        Object oValue = m_oValue;
        if (oValue == NO_VALUE)
            {
            Binary binValue = getBinaryValue();
            azzert(binValue != NO_VALUE);

            m_oValue = oValue = m_ctx.getValueFromInternalConverter().convert(
                binValue);
            }
        return oValue;
        }

    /**
    * {@inheritDoc}
    */
    public Object getOriginalValue()
        {
        Object oValueOrig = m_oValueOrig;
        if (oValueOrig == NO_VALUE)
            {
            Binary binValueOrig = getOriginalBinaryValue();
            azzert(binValueOrig != NO_VALUE);

            m_oValueOrig = oValueOrig = m_ctx.getValueFromInternalConverter()
                    .convert(binValueOrig);
            }
        return oValueOrig;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isOriginalPresent()
        {
        return m_binValueOrig != null;
        }

    /**
    * {@inheritDoc}
    */
    public Object setValue(Object oValue)
        {
        Object oPrev = getValue();

        m_oValue   = oValue;
        m_binValue = NO_VALUE;

        return oPrev;
        }

    /**
    * {@inheritDoc}
    */
    public void setValue(Object oValue, boolean fSynthetic)
        {
        setValue(oValue);
        m_fSynthetic = fSynthetic;
        }

    /**
    * {@inheritDoc}
    */
    public void updateBinaryValue(Binary binValue)
        {
        m_binValue = binValue;
        m_oValue   = NO_VALUE;
        }

    /**
    * {@inheritDoc}
    */
    public void updateBinaryValue(Binary binValue, boolean fSynthetic)
        {
        updateBinaryValue(binValue);
        m_fSynthetic = fSynthetic;
        }

    /**
    * {@inheritDoc}
    */
    public Serializer getSerializer()
        {
        BackingMapManagerContext ctx = m_ctx;
        return ctx == null ? null : ctx.getCacheService().getSerializer();
        }

    /**
    * {@inheritDoc}
    */
    public BackingMapManagerContext getContext()
        {
        return m_ctx;
        }

    /**
    * {@inheritDoc}
    */
    public ObservableMap getBackingMap()
        {
        // don't expose it
        return null;
        }

    /**
    * {@inheritDoc}
    */
    public BackingMapContext getBackingMapContext()
        {
        return null;
        }

    /**
    * {@inheritDoc}
    */
    public void expire(long cMillis)
        {
        m_ldtExpiry = calculateExpiry(cMillis);
        }

    /**
    * {@inheritDoc}
    */
    public long getExpiry()
        {
        long ldtExpiry = m_ldtExpiry;
        return ldtExpiry == CacheMap.EXPIRY_DEFAULT || ldtExpiry == CacheMap.EXPIRY_NEVER
             ? ldtExpiry : Math.max(1L, ldtExpiry - Base.getLastSafeTimeMillis());
        }

    /**
    * {@inheritDoc}
    */
    public boolean isReadOnly()
        {
        return false;
        }

    /**
    * {@inheritDoc}
    */
    public void update(ValueUpdater updater, Object oValue)
        {
        InvocableMapHelper.updateEntry(updater, this, oValue);
        }

    /**
    * {@inheritDoc}
    */
    public boolean isPresent()
        {
        return getOriginalBinaryValue() != null;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isSynthetic()
        {
        return m_fSynthetic;
        }

    /**
     * {@inheritDoc}
     */
    public void remove(boolean fSynthetic)
        {
        updateBinaryValue(null, fSynthetic);
        }

    /**
    * {@inheritDoc}
    */
    public Object extract(ValueExtractor extractor)
        {
        return InvocableMapHelper.extractFromEntry(extractor, this);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compares the specified object with this entry for equality.
    *
    * @param o object to be compared for equality with this entry
    *
    * @return <tt>true</tt> if the specified object is equal to this entry
    */
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (o instanceof BackingMapBinaryEntry)
            {
            BackingMapBinaryEntry that = (BackingMapBinaryEntry) o;

            return equals(this.getBinaryKey(), that.getBinaryKey())
                    && equals(this.getBinaryValue(), that.getBinaryValue());
            }
        return false;
        }

    /**
    * Returns the hash code value for this entry.
    *
    * @return the hash code value for this entry.
    */
    public int hashCode()
        {
        return m_binKey.hashCode();
        }

    /**
    * Render the entry as a String.
    *
    * @return the readable description for this entry
    */
    public String toString()
        {
        StringBuffer sb = new StringBuffer();

        sb.append(ClassHelper.getSimpleName(getClass())).append("{Key=");

        // prevent conversion as a side effect of toString call
        if (m_oKey == NO_VALUE)
            {
            sb.append(m_binKey);
            }
        else
            {
            sb.append(m_oKey);
            }

        sb.append(", Value=");
        if (m_oValue == NO_VALUE)
            {
            sb.append(m_binValue);
            }
        else
            {
            sb.append(m_oValue);
            }

        sb.append(", OrigValue=");
        if (m_oValueOrig == NO_VALUE)
            {
            sb.append(m_binValueOrig);
            }
        else
            {
            sb.append(m_oValueOrig);
            }
        sb.append('}');

        return sb.toString();
        }

    
    // ----- helpers -------------------------------------------------------
    
    /**
     * Calculate the expiry timestamp based on the expiry delay value.
     *
     * @param cDelay  the expiry delay
     *
     * @return the expiry timestamp
     */
    protected static long calculateExpiry(long cDelay)
        {
        return cDelay == 0L              ? CacheMap.EXPIRY_DEFAULT :
               cDelay < 0L               ? CacheMap.EXPIRY_NEVER :
               cDelay > Long.MAX_VALUE/2 ? Long.MAX_VALUE :
                                           Base.getLastSafeTimeMillis() + cDelay;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The backing map context.
    */
    protected BackingMapManagerContext m_ctx;

    /**
    * The Binary key. This object reference will not change for the life of
    * the Entry.
    */
    protected Binary m_binKey;

    /**
    * The Binary value.
    */
    protected Binary m_binValue;

    /**
    * The original Binary value.
    */
    protected Binary m_binValueOrig;

    /**
    * The expiry timestamp for this entry; or
    * {@link com.tangosol.net.cache.CacheMap#EXPIRY_DEFAULT} if the
    * entry uses the default expiry setting; or
    * {@link com.tangosol.net.cache.CacheMap#EXPIRY_NEVER} if the
    * entry never expires
    */
    protected long m_ldtExpiry = CacheMap.EXPIRY_DEFAULT;

    /**
    * Lazily converted key in Object format.
    */
    protected Object m_oKey = NO_VALUE;

    /**
    * Lazily converted value in Object format.
    */
    protected Object m_oValue = NO_VALUE;

    /**
    * Lazily converted original value in Object format.
    */
    protected Object m_oValueOrig = NO_VALUE;

    /**
    * Whether this update is synthetic.
    *
    * @since 12.2.1.4
    */
    protected boolean m_fSynthetic;

    /**
    * Marker object used to indicate that a lazily converted value has not yet
    * been calculated.
    */
    private final static Binary NO_VALUE = new Binary();
    }
