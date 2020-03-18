/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.oracle.coherence.common.collections.AbstractStableIterator;

import com.tangosol.util.Base;
import com.tangosol.util.HashHelper;

import java.net.InetSocketAddress;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.HashSet;


/**
* CompositeAddressProvider is a composite of one or more AddressProviders or
* addresses that also implements the Set interface.  This AddressProvider will
* provide addresses from all registered AddressProviders or Addresses.
* <p>
* This class implements the Set interface, but the contents are not checked to
* determine whether each element is unique. It is the responsibility of the
* user to ensure that the elements are unique if the object is used as a Set.
* <p>
* This implementation is thread-safe for consumers of the Set interface, but
* allows no more than one consumer of the AddressProvider interface.
*
* @author rhl 2008-12-05
* @since Coherence 3.5
*/
public class CompositeAddressProvider
        extends AbstractCollection
        implements DescribableAddressProvider, Set
    {
    // ----- constructors ---------------------------------------------------
    /**
    * Default constructor.
    */
    public CompositeAddressProvider()
        {
        }

    /**
    * Construct a CompositeAddressProvider from the specified AddressProvider.
    *
    * @param provider  the AddressProvider to add
    */
    public CompositeAddressProvider(AddressProvider provider)
        {
        addProvider(provider);
        }

    /**
    * Add an AddressProvider.
    *
    * @param provider  the AddressProvider to add
    */
    public synchronized void addProvider(AddressProvider provider)
        {
        getProviderList().add(provider);
        }

    /**
    * Add an address.
    *
    * @param address  the address to add
    */
    public synchronized void addAddress(InetSocketAddress address)
        {
        AddressProvider providerNew = new SingleProvider(address);

        // check if the address already exists as a single provider; if so,
        // don't add it again.
        for (Iterator iter = getProviderList().iterator(); iter.hasNext(); )
            {
            if (Base.equals(providerNew, iter.next()))
                {
                return;
                }
            }
        addProvider(providerNew);
        }

    /**
    * Return the provider list.
    *
    * @return the provider list
    */
    protected List getProviderList()
        {
        return m_listProviders;
        }


    // ----- AddressProvider interface --------------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized InetSocketAddress getNextAddress()
        {
        Iterator iter = ensureInternalIterator();
        if (iter.hasNext())
            {
            return (InetSocketAddress) iter.next();
            }

        // the iterator was exhausted
        return null;
        }

    /**
    * {@inheritDoc}
    */
    public void accept()
        {
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void reject(Throwable eCause)
        {
        try
            {
            ensureInternalIterator().remove();
            }
        catch (UnsupportedOperationException | IllegalStateException e)
            {
            // could happen from concurrent use, even though our getNextAddress call is sync'd some other
            // thread could call it between our call and the call to reject, leaving the iterator in a
            // state where remove can't function
            }
        }

    /**
    * {@inheritDoc}
    */
    public String[] getAddressDescriptions()
        {
        LinkedList listProviders = (LinkedList) getProviderList();
        if (listProviders.size() == 1)
            {
            // optimize for common case
            try
                {
                AddressProvider provider = (AddressProvider) listProviders.getFirst();
                return provider instanceof DescribableAddressProvider ?
                       ((DescribableAddressProvider) provider).getAddressDescriptions() :
                       InetAddressHelper.getAddressDescriptions(getConfiguredAddresses(provider));
                }
            catch (NoSuchElementException e)
                {
                // the address provider was concurrently removed
                return new String[0];
                }
            }

        synchronized (this)
          {
          listProviders = new LinkedList(listProviders);
          }

        List<String> listAddress = new ArrayList<>();
        for (Iterator iter = listProviders.iterator(); iter.hasNext(); )
            {
            AddressProvider provider = (AddressProvider) iter.next();
            listAddress.addAll(provider instanceof DescribableAddressProvider ?
                        Arrays.asList(((DescribableAddressProvider) provider).getAddressDescriptions()) :
                        Arrays.asList(InetAddressHelper.getAddressDescriptions(getConfiguredAddresses(provider))));
            }

        return listAddress.toArray(new String[listAddress.size()]);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized boolean equals(Object o)
        {
        return (o instanceof CompositeAddressProvider) &&
            ((CompositeAddressProvider) o).m_listProviders.equals(m_listProviders);
        }

    /**
    * Return the hash code for this AddressProvider.
    *
    * @return the hash code for this AddressProvider
    */
    public synchronized int hashCode()
        {
        return 37 * HashHelper.hash(m_listProviders, 0);
        }


    // ----- Collection interface -------------------------------------------

    /**
    * Returns an iterator over the elements contained in this collection.
    *
    * @return an iterator over the elements contained in this collection.
    */
    public Iterator iterator()
        {
        return new AddressIterator(getProviderList());
        }

    /**
    * Returns the number of elements in this collection.  If the collection
    * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
    * <tt>Integer.MAX_VALUE</tt>.
    *
    * @return the number of elements in this collection
    */
    public int size()
        {
        int cAddress = 0;
        for (Iterator iter = iterator(); iter.hasNext(); ++cAddress)
            {
            iter.next();
            }
        return cAddress;
        }

    /**
    * Ensures that this collection contains the specified element (optional
    * operation).  Returns <tt>true</tt> if the collection changed as a
    * result of the call.  (Returns <tt>false</tt> if this collection does
    * not permit duplicates and already contains the specified element.)
    * Collections that support this operation may place limitations on what
    * elements may be added to the collection.  In particular, some
    * collections will refuse to add <tt>null</tt> elements, and others will
    * impose restrictions on the type of elements that may be added.
    * Collection classes should clearly specify in their documentation any
    * restrictions on what elements may be added.<p>
    *
    * This implementation always throws an
    * <tt>UnsupportedOperationException</tt>.
    *
    * @param o element whose presence in this collection is to be ensured.
    * @return <tt>true</tt> if the collection changed as a result of the call.
    *
    * @throws UnsupportedOperationException if the <tt>add</tt> method is not
    *   supported by this collection.
    *
    * @throws NullPointerException if this collection does not permit
    *         <tt>null</tt> elements, and the specified element is
    *         <tt>null</tt>.
    *
    * @throws ClassCastException if the class of the specified element
    *         prevents it from being added to this collection.
    *
    * @throws IllegalArgumentException if some aspect of this element
    *            prevents it from being added to this collection.
    */
    public boolean add(Object o)
        {
        if (o instanceof AddressProvider)
            {
            addProvider((AddressProvider) o);
            }
        else if (o instanceof InetSocketAddress)
            {
            addAddress((InetSocketAddress) o);
            }
        else
            {
            throw new ClassCastException(o == null ? "null" : o.getClass().getName());
            }

        // Note: this isn't exactly right; strictly speaking, we are supposed
        // to return false if the address(es) added were all already contained
        // (set did not change).
        return true;
        }

    /**
    * Removes the specified element from this set if it is present (optional
    * operation).  More formally, removes an element <code>e</code> such that
    * <code>(o==null ?  e==null : o.equals(e))</code>, if the set contains
    * such an element.  Returns <tt>true</tt> if the set contained the
    * specified element (or equivalently, if the set changed as a result of
    * the call).  (The set will not contain the specified element once the
    * call returns.)
    *
    * @param o object to be removed from this set, if present.
    * @return true if the set contained the specified element.
    * @throws ClassCastException if the type of the specified element
    *         is incompatible with this set (optional).
    * @throws NullPointerException if the specified element is null and this
    *         set does not support null elements (optional).
    * @throws UnsupportedOperationException if the <tt>remove</tt> method is
    *         not supported by this set.
    */
    public synchronized boolean remove(Object o)
        {
        if (o instanceof AddressProvider)
            {
            return getProviderList().remove(o);
            }
        else if (o instanceof InetSocketAddress)
            {
            InetSocketAddress address = (InetSocketAddress) o;
            return getProviderList().remove(new SingleProvider(address));
            }

        return false;
        }


    // ----- inner class AddressIterator ------------------------------------

    /**
    * An Iterator over the addresses in this AddressProvider.  The Iterator
    * represents a "snapshot" of the AddressProvider's addresses and may not
    * reflect any concurrent updates to the underlying AddressProvider(s).
    */
    protected static class AddressIterator
            extends AbstractStableIterator
        {
        // ---- constructors ------------------------------------------------

        protected AddressIterator(List listProvider)
            {
            m_iterProvider = new LinkedList(listProvider).iterator();
            }

        /**
        * {@inheritDoc}
        */
        protected void advance()
            {
            while (m_iterAddress == null || !m_iterAddress.hasNext())
                {
                if (m_iterProvider.hasNext())
                    {
                    AddressProvider provider =
                        (AddressProvider) m_iterProvider.next();
                    m_providerCurrent = provider;
                    synchronized (provider)
                        {
                        HashSet           set = new HashSet();
                        InetSocketAddress address;
                        while ((address = provider.getNextAddress()) != null)
                            {
                            set.add(address);
                            }
                        m_iterAddress = set.iterator();
                        }
                    }
                else
                    {
                    // exhausted all of the providers
                    return;
                    }
                }
            setNext(m_iterAddress.next());
            }

        /**
        * {@inheritDoc}
        */
        public void remove(Object oPrev)
            {
            AddressProvider provider = getCurrentProvider();

            // only remove Addresses from the Set iterator if they were added
            // dynamically as single addresses.
            if (provider instanceof SingleProvider)
                {
                // reject will remove the provider from the composite list.
                provider.reject(null);
                }
            }

        /**
        * Return the current AddressProvider.
        *
        * @return the current AddressProvider
        */
        protected AddressProvider getCurrentProvider()
            {
            return m_providerCurrent;
            }

        // ---- data members ------------------------------------------------

        /**
        * The current AddressProvider.
        */
        protected AddressProvider m_providerCurrent;

        /**
        * The iterator of Addresses from the current provider.
        */
        protected Iterator m_iterAddress;

        /**
        * The iterator of AddressProviders.
        */
        protected Iterator m_iterProvider;
        }


    // ----- inner class SingleProvider -------------------------------------

    /**
    * AddressProvider wrapper for a single address dynamically added to this
    * AddressSet.
    */
    protected class SingleProvider
            implements AddressProvider
        {
        // ----- constructors -----------------------------------------------
        /**
        * Constructor
        */
        protected SingleProvider(InetSocketAddress address)
            {
            m_address = address;
            }


        // ---- AddressProvider interface -----------------------------------

        /**
        * {@inheritDoc}
        */
        public InetSocketAddress getNextAddress()
            {
            boolean fReturnAddr = m_fExhausted = !m_fExhausted;
            return fReturnAddr ? m_address : null;
            }

        /**
        * {@inheritDoc}
        */
        public void accept()
            {
            }

        /**
        * {@inheritDoc}
        */
        public void reject(Throwable eCause)
            {
            List listProvider = CompositeAddressProvider.this.getProviderList();
            synchronized (listProvider)
                {
                listProvider.remove(this);
                }
            }


        // ---- Object methods ----------------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean equals(Object o)
            {
            return (o instanceof SingleProvider) &&
                ((SingleProvider) o).m_address.equals(m_address);
            }

        /**
        * {@inheritDoc}
        */
        public int hashCode()
            {
            return m_address.hashCode();
            }


        // ---- data members ------------------------------------------------

        /**
        * The single address that this AddressProvider represents.
        */
        protected InetSocketAddress m_address;

        /**
        * Is this AddressProvider exhausted?
        */
        protected boolean m_fExhausted = false;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Create (if necessary) and return the default iterator.
    *
    * @return the default iterator
    */
    protected AddressIterator ensureInternalIterator()
        {
        AddressIterator iter = m_iterInternal;

        if (iter == null || !iter.hasNext())
            {
            m_iterInternal = (AddressIterator) iterator();
            iter = (iter == null) ? m_iterInternal : iter;
            }
        return iter;
        }

    /**
    * Return a list of configured addresses in the specified AddressProvider.
    *
    * @param provider  the address provider
    *
    * @return the list of addresses
    */
    protected List getConfiguredAddresses(AddressProvider provider)
        {
        List listAddress = new LinkedList();
        InetSocketAddress address;
        while ((address = provider.getNextAddress()) != null)
            {
            listAddress.add(address);
            }
        return listAddress;
        }


    // ----- data members ---------------------------------------------------

    /**
    * Map of Providers
    */
    private final LinkedList m_listProviders = new LinkedList();

    /**
    * The default iterator used to implement the AddressProvider interface.
    */
    protected AddressIterator m_iterInternal;
    }
