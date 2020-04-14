/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.Collection;


/**
* Extends SafeLinkedList and adds recycling of Node objects.
*
* @author cp 2002.01.06
*/
public class RecyclingLinkedList
        extends SafeLinkedList
        implements Cloneable, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an empty List.
    */
    public RecyclingLinkedList()
        {
        initRecycling(DEFAULT_RECYCLE_MAX);
        }

    /**
    * Construct an empty List.
    *
    * @param cMaxRecycle  the maximum number of Nodes to recycle at any
    *                     given time
    */
    public RecyclingLinkedList(int cMaxRecycle)
        {
        initRecycling(cMaxRecycle);
        }

    /**
    * Construct a List containing the elements of the specified Collection.
    *
    * @param collection  the Collection to fill this List from
    */
    public RecyclingLinkedList(Collection collection)
        {
        super(collection);
        initRecycling(DEFAULT_RECYCLE_MAX);
        }

    /**
    * Construct a List containing the elements of the specified Collection.
    *
    * @param collection   the Collection to fill this List from
    * @param cMaxRecycle  the maximum number of Nodes to recycle at any
    *                     given time
    */
    public RecyclingLinkedList(Collection collection, int cMaxRecycle)
        {
        super(collection);
        initRecycling(cMaxRecycle);
        }


    // ----- recycling support ----------------------------------------------

    /**
    * Initializing the recycling data structures.
    *
    * @param cMaxRecycle  the maximum number of Nodes to recycle at any
    *                     given time
    */
    protected void initRecycling(int cMaxRecycle)
        {
        m_anodeRecycled = new Node[cMaxRecycle];
        m_cRecycleMax   = cMaxRecycle;
        m_cRecycleNodes = 0;
        }

    /**
    * Instantiate a Node.  (Factory pattern.)  This method is over-ridden by
    * inheriting implementations if the inheriting implementation extends the
    * inner Node class.
    *
    * @param o  the value for the Node
    */
    protected SafeLinkedList.Node instantiateNode(Object o)
        {
        Node node;
        int  cRecycleNodes = m_cRecycleNodes;
        if (cRecycleNodes > 0)
            {
            m_cRecycleNodes = --cRecycleNodes;
            Node[] anodeRecycled = m_anodeRecycled;
            node = anodeRecycled[cRecycleNodes];
            anodeRecycled[cRecycleNodes] = null;
            node.setObject(o);
            }
        else
            {
            node = new Node(o);
            }
        return node;
        }

    /**
    * Recycle the passed Node.
    *
    * @param node  the Node object to recycle
    */
    protected void recycleNode(Node node)
        {
        int cRecycleNodes = m_cRecycleNodes;
        if (cRecycleNodes < m_cRecycleMax)
            {
            m_anodeRecycled[cRecycleNodes] = node;
            m_cRecycleNodes = cRecycleNodes + 1;
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Create a deep clone of the list.
    *
    * @return a clone of this list
    */
    public Object clone()
        {
        RecyclingLinkedList that = (RecyclingLinkedList) super.clone();
        that.initRecycling(this.m_cRecycleMax);
        return that;
        }


    // ----- Serializable interface -----------------------------------------

    /**
    * The writeObject method is responsible for writing the state of the
    * object for its particular class so that the corresponding
    * readObject method can restore it.
    *
    * @param out the stream to write the object to
    *
    * @exception IOException Includes any I/O exceptions that may occur
    */
    private void writeObject(ObjectOutputStream out)
            throws IOException
        {
        out.writeInt(m_cRecycleMax);
        }

    /**
    * The readObject method is responsible for reading from the stream
    * and restoring the classes fields.
    *
    * @param in the stream to read data from in order to restore the object
    *
    * @exception IOException if I/O errors occur
    * @exception ClassNotFoundException If the class for an object being
    *              restored cannot be found.
    */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
        m_cRecycleMax = in.readInt();
        initRecycling(m_cRecycleMax);
        }


    // ----- inner classes --------------------------------------------------

    /**
    * A Node in the List.  Nodes are doubly-linked and store a value.
    */
    protected class Node
            extends SafeLinkedList.Node
        {
        // ----- constructors -------------------------------------

        /**
        * Construct a blank Node.
        */
        public Node()
            {
            }

        /**
        * Construct a Node with a value.
        *
        * @param o  the value to store in the Node
        */
        public Node(Object o)
            {
            super(o);
            }


        // ----- methods ------------------------------------------

        /**
        * Delink this Node and discard its value.
        *
        * @return the value of this Node before it was discarded
        */
        protected Object discard()
            {
            Object o = super.discard();

            // the following call does not need to be synchronized because
            // discard() is only called from within methods that are
            // synchronized on RecyclingLinkedList.this
            RecyclingLinkedList.this.recycleNode(this);

            return o;
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The Nodes to recycle.
    */
    protected transient Node[] m_anodeRecycled;

    /**
    * The current number of Nodes being recycle.
    */
    protected transient int m_cRecycleNodes;

    /**
    * The maximum number of Nodes to recycle.
    */
    protected int m_cRecycleMax;

    /**
    * The default maximum number of Nodes to recycle.
    */
    protected static final int DEFAULT_RECYCLE_MAX = 256;
    }
