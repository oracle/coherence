/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;


import com.tangosol.util.Gate;
import com.tangosol.util.ThreadGateLite;

import com.oracle.coherence.common.base.Blocking;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
* QueueFabric contains the abstract elements of a specialized 2-dimensional
* queue-like data-structure that is used internally by the PartitionedCache
* service.
* <p>
* A QueueFabric holds data elements (nodes) in a flexible lattice-like
* structure where each node is contained in 2 queues (one along each dimension).
* There is no implicit ordering between queues within a dimension, nor is there
* an expectation that the content of the queues would form a "flat" matrix.
* <p>
* For example, consider an embodiment of this data-structure that is used to
* track trades made by various brokerages on various stock tickers.  Along one
* dimension (e.g. rows) we have queues that represent the trades executed by
* a specific brokerage.  Along the other dimension (e.g. columns) we have queues
* of trades relating to a specific stock symbol.  Now suppose we have the
* following sequence of trades that are inserted into the fabric:
* <ul>
* <li>broker1 BUY  500 ORCL
* <li>broker1 BUY  500 GOOG
* <li>broker2 SELL 500 ORCL
* <li>broker3 BUY  500 GOOG
* <li>broker2 BUY  500 ORCL
* </ul>
*
* Along one dimension (rows): <br>
* The queue for broker1 would contain: 2 trades (BUY 500 ORCL, BUY 500 GOOG)
* The queue for broker2 would contain: 2 trades (SELL 500 ORCL, BUY 500 ORCL)
* The queue for broker3 would contain: 1 trade  (BUY 500 GOOG)
* <p>
* Along the other dimension (columns) <br>
* The queue for ORCL would contain: 3 trades (BUY 500, SELL 500, BUY 500)
* The queue for GOOG would contain: 2 trades (BUY 500, BUY 500)
* <p>
* Note: this data structure is not simply a matrix, as there is no uniqueness
*       identified by the intersection of the 2 dimensions, and in fact there
*       may be multiple intersections of the 2 dimensions (e.g. multiple trades
*       by the same broker on the same symbol).  In a matrix, the intersection
*       of the dimensions identifies a distinct entity (e.g. square e4 on a
*       chess-board)
*
* @since  Coherence 3.7
* @author rhl 2010.08.09
*/
public class QueueFabric
    {
    // ----- constructors -------------------------------------------------

    /**
    * Default constructor
    */
    public QueueFabric()
        {
        }


    // ----- QueueFabric methods ------------------------------------------

    /**
    * Attempts to lock all queues in this LinkedMatrix, waiting at most the
    * specified amount of time.
    *
    * @param cMillis  the amount of time in ms to wait, or -1 for forever
    *
    * @return true iff the lock was acquired
    */
    public boolean lockAll(long cMillis)
        {
        return getQueueControl().close(cMillis);
        }

    /**
    * Unlock all queues in this LinkedMatrix.
    */
    public void unlockAll()
        {
        getQueueControl().open();
        }

    /**
    * Add the specified node to the specified queues.
    *
    * @param node      the node to add
    * @param queueRow  the "row" queue to add the node to
    * @param queueCol  the "column" queue to add the node to
    */
    public void add(LinkedNode node, LinkedQueue queueRow, LinkedQueue queueCol)
        {
        queueRow.add(node);
        queueCol.add(node);
        }

    /**
    * Remove the specified node from the fabric.
    *
    * @param node  the node to remove
    */
    public void remove(LinkedNode node)
        {
        node.getRowQueue()   .remove(node);
        node.getColumnQueue().remove(node);
        }


    // ----- accessors ----------------------------------------------------

    /**
    * Return the Gate controlling access to the LinkedQueues.
    *
    * @return the Gate controlling access to the LinkedQueues
    */
    protected Gate getQueueControl()
        {
        return m_gateQueue;
        }


    // ----- inner class: LinkedNode --------------------------------------

    /**
    * LinkedNode represents a node in the LinkedMatrix.
    */
    public static class LinkedNode
        {
        // ----- accessors ------------------------------------------------

        /**
        * Return the next LinkedNode in the "row".
        *
        * @return the next LinkedNode in the "row"
        */
        public LinkedNode getNextInRow()
            {
            return m_nodeNextInRow;
            }

        /**
        * Set the next LinkedNode in the "row" to the specified node.
        *
        * @param nodeNext  the LinkedNode to set as the next in the "row"
        */
        protected void setNextInRow(LinkedNode nodeNext)
            {
            m_nodeNextInRow = nodeNext;
            }

        /**
        * Return the previous LinkedNode in the "row".
        *
        * @return the previous LinkedNode in the "row"
        */
        public LinkedNode getPrevInRow()
            {
            return m_nodePrevInRow;
            }

        /**
        * Set the previous LinkedNode in the "row" to the specified node.
        *
        * @param nodePrev  the LinkedNode to set as the previous in the "row"
        */
        protected void setPrevInRow(LinkedNode nodePrev)
            {
            m_nodePrevInRow = nodePrev;
            }

        /**
        * Return the next LinkedNode in the "column".
        *
        * @return the next LinkedNode in the "column"
        */
        public LinkedNode getNextInCol()
            {
            return m_nodeNextInCol;
            }

        /**
        * Set the next LinkedNode in the "column" to the specified node.
        *
        * @param nodeNext  the LinkedNode to set as the next in the "column"
        */
        protected void setNextInCol(LinkedNode nodeNext)
            {
            m_nodeNextInCol = nodeNext;
            }

        /**
        * Return the previous LinkedNode in the "column".
        *
        * @return the previous LinkedNode in the "column"
        */
        public LinkedNode getPrevInCol()
            {
            return m_nodePrevInCol;
            }

        /**
        * Set the previous LinkedNode in the "column" to the specified node.
        *
        * @param nodePrev  the LinkedNode to set as the previous in the "column"
        */
        protected void setPrevInCol(LinkedNode nodePrev)
            {
            m_nodePrevInCol = nodePrev;
            }

        /**
        * Return the LinkedQueue representing the "row" of this LinkedNode.
        *
        * @return the LinkedQueue representing the "row" of this LinkedNode
        */
        public LinkedQueue getRowQueue()
            {
            return m_queueRow;
            }

        /**
        * Set the LinkedQueue representing the "row" of this LinkedNode.
        *
        * @param queueRow  the LinkedQueue representing the "row" of this LinkedNode
        */
        protected void setRowQueue(LinkedQueue queueRow)
            {
            if (m_queueRow == null)
                {
                m_queueRow = queueRow;
                }
            else
                {
                throw new IllegalStateException(
                        "Attempt to add a node to multiple queues along the same dimension");
                }
            }

        /**
        * Return the LinkedQueue representing the "column" of this LinkedNode.
        *
        * @return the LinkedQueue representing the "column" of this LinkedNode
        */
        public LinkedQueue getColumnQueue()
            {
            return m_queueCol;
            }

        /**
        * Set the LinkedQueue representing the "column" of this LinkedNode.
        *
        * @param queueCol  the LinkedQueue representing the "col" of this LinkedNode
        */
        protected void setColumnQueue(LinkedQueue queueCol)
            {
            if (m_queueCol == null)
                {
                m_queueCol = queueCol;
                }
            else
                {
                throw new IllegalStateException(
                        "Attempt to add a node to multiple queues along the same dimension");
                }
            }

        // ----- data members ---------------------------------------------
        /**
        * The "next" node in the "row".
        */
        protected LinkedNode m_nodeNextInRow;

        /**
        * The "previous" node in the "row".
        */
        protected LinkedNode m_nodePrevInRow;

        /**
        * The "next" node in the "column".
        */
        protected LinkedNode m_nodeNextInCol;

        /**
        * The "previous" node in "column".
        */
        protected LinkedNode m_nodePrevInCol;

        /**
        * The "row" that this LinkedNode is in, or null.
        */
        protected LinkedQueue m_queueRow;

        /**
        * The "column" that this LinkedNode is in, or null.
        */
        protected LinkedQueue m_queueCol;
        }


    // ----- inner class: LinkedQueue -------------------------------------

    /**
    * A LinkedQueue represents a "row" or a "column" in the LinkedMatrix.
    */
    public class LinkedQueue
        {
        // ----- constructors ---------------------------------------------

        /**
        * Construct a LinkedQueue.
        *
        * @param fRow  true iff the constructed queue should represent a "row"
        */
        public LinkedQueue(boolean fRow)
            {
            m_fRow = fRow;
            }

        // ----- LinkedQueue operations -----------------------------------

        /**
        * Add the specified node to this queue.
        *
        * @param nodeNew  the new LinkedNode to add
        */
        public synchronized void add(LinkedNode nodeNew)
            {
            LinkedNode nodeFirst = getFirstNode();
            LinkedNode nodeLast  = getLastNode();

            if (nodeFirst == null)
                {
                setFirstNode(nodeNew);
                }
            if (nodeLast != null)
                {
                link(nodeLast, nodeNew);
                }

            setLastNode(nodeNew);

            if (isRow())
                {
                nodeNew.setRowQueue(this);
                }
            else
                {
                nodeNew.setColumnQueue(this);
                }
            m_cSize++;
            }

        /**
        * Remove the specified node from this LinkedQueue.
        *
        * @param node  the LinkedNode to remove
        *
        * @return true iff the node was successfully removed
        */
        public synchronized boolean remove(LinkedNode node)
            {
            boolean fByRow = isRow();

            if ((fByRow  && node.getRowQueue() != this) ||
                (!fByRow && node.getColumnQueue() != this))
                {
                return false;
                }

            LinkedNode nodePrev;
            LinkedNode nodeNext;

            if (fByRow)
                {
                nodePrev = node.getPrevInRow();
                nodeNext = node.getNextInRow();
                }
            else
                {
                nodePrev = node.getPrevInCol();
                nodeNext = node.getNextInCol();
                }

            if (nodePrev == null)
                {
                setFirstNode(nodeNext);
                }

            link(nodePrev, nodeNext);

            if (nodeNext == null)
                {
                setLastNode(nodePrev);
                }

            m_cSize--;
            return true;
            }

        /**
        * Return true iff this LinkedQueue is empty.
        *
        * @return true iff this LinkedQueue is empty
        */
        public boolean isEmpty()
            {
            return getFirstNode() == null;
            }

        /**
        * Return the size of this Queue. The returned value could be "stalled";
        * to get the precise value the caller needs to synchronize on the queue.
        *
        * @return the size of this Queue
        */
        public int getSize()
            {
            return m_cSize;
            }

        /**
        * Link the specified nodes in the dimension of this queue.
        *
        * @param node1  the "predecessor" node (may be null)
        * @param node2  the "successor" node (may be null)
        */
        protected void link(LinkedNode node1, LinkedNode node2)
            {
            if (isRow())
                {
                if (node1 != null)
                    {
                    node1.setNextInRow(node2);
                    }
                if (node2 != null)
                    {
                    node2.setPrevInRow(node1);
                    }
                }
            else
                {
                if (node1 != null)
                    {
                    node1.setNextInCol(node2);
                    }
                if (node2 != null)
                    {
                    node2.setPrevInCol(node1);
                    }
                }
            }

        /**
        * Attempt to lock this queue, waiting at most the specified amount of
        * time.  Return true iff the lock was successfully acquired.
        *
        * @param cMillis  the amount of time in ms to wait, or -1 for forever
        *
        * @return true iff this queue was successfully locked
        */
        public boolean lock(long cMillis)
            {
            Gate    gateQueue = getQueueControl();
            boolean fLocked   = false;

            if (gateQueue.enter(cMillis))
                {
                try
                    {
                    if (cMillis >= 0)
                        {
                        fLocked = Blocking.tryLock(ensureLock(), cMillis, TimeUnit.MILLISECONDS);
                        }
                    else
                        {
                        Blocking.lockInterruptibly(ensureLock());
                        fLocked = true;
                        }
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    }
                finally
                    {
                    if (!fLocked)
                        {
                        gateQueue.exit();
                        }
                    }
                }

            return fLocked;
            }

        /**
        * Unlock this queue.
        */
        public void unlock()
            {
            ensureLock().unlock();
            getQueueControl().exit();
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return true iff this LinkedQueue is a "row".
        *
        * @return true iff this LinkedQueue is a "row"
        */
        public boolean isRow()
            {
            return m_fRow;
            }

        /**
        * Return the first node in the queue.
        *
        * @return the first node in the queue
        */
        public LinkedNode getFirstNode()
            {
            return m_nodeFirst;
            }

        /**
        * Set the first node in the queue.
        *
        * @param nodeFirst  the node to set to be the first in the queue
        */
        protected void setFirstNode(LinkedNode nodeFirst)
            {
            m_nodeFirst = nodeFirst;
            }

        /**
        * Return the last node in the queue.
        *
        * @return the last node in the queue
        */
        public LinkedNode getLastNode()
            {
            return m_nodeLast;
            }

        /**
        * Set the last node in the queue.
        *
        * @param nodeLast  the node to set to be the last in the queue
        */
        protected void setLastNode(LinkedNode nodeLast)
            {
            m_nodeLast = nodeLast;
            }

        /**
        * Return the Lock protecting this Queue.
        *
        * @return the Lock protecting this Queue
        */
        protected Lock ensureLock()
            {
            Lock lock = m_lock;
            if (lock == null)
                {
                synchronized (this)
                    {
                    lock = m_lock;
                    if (lock == null)
                        {
                        lock = m_lock = new ReentrantLock();
                        }
                    }
                }
            return lock;
            }

        // ----- data members ---------------------------------------------
        /**
        * The first node in the LinkedList
        */
        protected LinkedNode m_nodeFirst;

        /**
        * The last node in the LinkedList
        */
        protected LinkedNode m_nodeLast;

        /**
         * Lock protecting this LinkedQueue.
         */
        protected Lock m_lock;

        /**
        * Is this queue a "row"?
        */
        protected boolean m_fRow;

        /**
        * The size of the linked list.
        */
        private int m_cSize;
        }


    // ----- data members -------------------------------------------------

    /**
    * Gate controlling lock access.
    */
    protected Gate m_gateQueue = new ThreadGateLite();
    }
