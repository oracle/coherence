/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;

import java.util.Iterator;

public class ConverterNamedMapDeque<KF, F, KT, T>
        extends ConverterNamedMapQueue<KF, F, KT, T>
        implements NamedMapDeque<KT, T>
    {
    public ConverterNamedMapDeque(NamedMapDeque<KF, F> deque, Converter<KF, KT> convKeyUp, Converter<F, T> convUp,
                                  Converter<KT, KF> convKeyDown, Converter<T, F> convDown)
        {
        super(deque, convKeyUp, convUp, convKeyDown, convDown);
        f_deque = deque;
        }

    @Override
    public long prepend(T t)
        {
        return f_deque.prepend(getConverterDown().convert(t));
        }

    @Override
    public void addFirst(T t)
        {
        f_deque.addFirst(getConverterDown().convert(t));
        }

    @Override
    public void addLast(T t)
        {
        f_deque.addLast(getConverterDown().convert(t));
        }

    @Override
    public boolean offerFirst(T t)
        {
        return f_deque.offerFirst(getConverterDown().convert(t));
        }

    @Override
    public boolean offerLast(T t)
        {
        return f_deque.offerLast(getConverterDown().convert(t));
        }

    @Override
    public T removeFirst()
        {
        return getConverterUp().convert(f_deque.removeFirst());
        }

    @Override
    public T removeLast()
        {
        return getConverterUp().convert(f_deque.removeLast());
        }

    @Override
    public T pollFirst()
        {
        return getConverterUp().convert(f_deque.pollFirst());
        }

    @Override
    public T pollLast()
        {
        return getConverterUp().convert(f_deque.pollLast());
        }

    @Override
    public T getFirst()
        {
        return getConverterUp().convert(f_deque.getFirst());
        }

    @Override
    public T getLast()
        {
        return getConverterUp().convert(f_deque.getLast());
        }

    @Override
    public T peekFirst()
        {
        return getConverterUp().convert(f_deque.peekFirst());
        }

    @Override
    public T peekLast()
        {
        return getConverterUp().convert(f_deque.peekLast());
        }

    @Override
    public boolean removeFirstOccurrence(Object o)
        {
        return f_deque.removeFirstOccurrence(o);
        }

    @Override
    public boolean removeLastOccurrence(Object o)
        {
        return f_deque.removeLastOccurrence(o);
        }

    @Override
    public void push(T t)
        {
        f_deque.push(getConverterDown().convert(t));
        }

    @Override
    public T pop()
        {
        return getConverterUp().convert(f_deque.pop());
        }

    @Override
    public Iterator<T> descendingIterator()
        {
        return ConverterCollections.getIterator(f_deque.descendingIterator(), getConverterUp());
        }

    // ----- factory methods ------------------------------------------------

    @SuppressWarnings("unchecked")
    public static <K, E> NamedMapDeque<K, E> createDeque(NamedMapDeque<Binary, Binary> deque)
        {
        BackingMapManagerContext context       = deque.getService().getBackingMapManager().getContext();
        Converter<Binary, K>     convKeyUp   = context.getKeyFromInternalConverter();
        Converter<K, Binary>     convKeyDown = context.getKeyToInternalConverter();
        Converter<Binary, E>     convValUp   = context.getKeyFromInternalConverter();
        Converter<E, Binary>     convValDown = context.getKeyToInternalConverter();
        return new ConverterNamedMapDeque<>(deque, convKeyUp, convValUp, convKeyDown, convValDown);
        }

    // ----- data members ---------------------------------------------------

    private final NamedMapDeque<KF, F> f_deque;
    }
