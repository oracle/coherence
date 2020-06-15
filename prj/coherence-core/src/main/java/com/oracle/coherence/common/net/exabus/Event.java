/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus;


import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.common.io.BufferSequence;


/**
 * An Event indicates that a special condition has occurred on a Bus.
 *
 * @author mf/cp 2010.10.04
 */
public interface Event
         extends Disposable
    {
    /**
     * Enumeration of event types.
     */
    enum Type
        {
        /**
         * The OPEN Event is emitted once the Bus is ready to exchange
         * information.
         */
        OPEN,

        /**
         * The CLOSE Event is emitted once the Bus will no longer exchange
         * information. No more input will be accepted and no further output
         * will be added to the collectors.
         */
        CLOSE,

        /**
         * The CONNECT Event is emitted at the start of a sequence of
         * communication between two EndPoints on a Bus. The event may either
         * be solicited as a result of a local {@link Bus#connect connect},
         * unsolicited due to to a peer initiating the connection.
         */
        CONNECT,

        /**
         * The DISCONNECT Event is emitted at the end of a sequence of
         * communication between two EndPoints on a Bus.
         * <p>
         * The DISCONNECT event may either be solicited as the result of
         * a local {@link Bus#disconnect disconnect}, or unsolicited due to
         * either a remote disconnect or a lower level protocol termination.
         * <p>
         * Note that except in the case of a local call to {@link Bus#disconnect}
         * the Bus is not required to detect a logical or physical
         * disconnection, or to emit this event. Therefore "death detection"
         * is ultimately the responsibility of the Bus consumer, and not the
         * Bus itself.
         * <p>
         * Any receipt for the specified EndPoint added to the event collector
         * after this event, indicates an operation that is not known to have
         * completed; in other words, the operation may or may not have
         * completed.
         * <p>
         * For an unsolicited DISCONNECT event the Content may contain an
         * optional {@link Throwable} indicating the reason for the DISCONNECT.
         * <p>
         * Invoking asynchronous operations against a disconnected EndPoint
         * will have no effect except for the receipt being emitted to the
         * event collector.
         */
        DISCONNECT,

        /**
         * The RELEASE Event is emitted by the Bus only in response to a local
         * {@link Bus#release release}. Prior to being released, a logical
         * connection exists even after the DISCONNECT event has been emitted.
         * Once released, the Bus is ready for new connections for the
         * corresponding EndPoint.
         */
        RELEASE,

        /**
         * The BACKLOG_EXCESSIVE Event is emitted when the Bus has reached
         * a state where it is having difficulty in keeping up with the load
         * placed upon it. While in this state, the Bus will not reject work
         * but may eventually disconnect if the backlog continues to increase.
         * Upon receiving this event, it is the responsibility of the Bus
         * consumer to modulate its use of Bus resources.
         * <p>
         * The event may be emitted for a specific remote EndPoint, indicating
         * that throttling is only desired for that EndPoint, or <tt>null</tt>
         * if the backlog is not EndPoint specific. The event may also be
         * emitted for the local EndPoint to indicate that the caller needs to
         * service the Bus output faster, such as when a Bus consumer falls
         * behind in disposing of Events.
         */
        BACKLOG_EXCESSIVE,

        /**
         * The BACKLOG_NORMAL Event is emitted to signify the end of a
         * backlog state. This event is only emitted as a follow up to a
         * BACKLOG_EXCESSIVE event, and will use the same EndPoint
         * information as was used in the BACKLOG_EXCESSIVE event.
         */
        BACKLOG_NORMAL,

        /**
         * A RECEIPT Event is emitted by the Bus to indicate the completion of
         * an asynchronous operation.
         * <p>
         * The content of a RECEIPT event is the "receipt" object that was
         * supplied to the asynchronous operation. Note <tt>null</tt> receipts
         * will not result in a RECEIPT event.
         */
        RECEIPT,

        /**
         * The SIGNAL Event is emitted on the MemoryBus as a result of a peer's
         * call to the {@link MemoryBus#signal signal()} method specifying this
         * EndPoint.
         */
        SIGNAL,

        /**
         * The MESSAGE Event is emitted by the MessageBus in order to deliver the
         * contents of a message that the Bus received from a peer.
         * <p>
         * For a MessageBus, the Content of a MESSAGE Event is a {@link
         * BufferSequence}.
         */
        MESSAGE,
        }

    /**
     * Determine the event type.
     *
     * @return the Type of event
     */
    public Type getType();

    /**
     * Return the EndPoint associated with the event, if any.
     *
     * @return the associated EndPoint, or <tt>null</tt> if the event is not
     *         related to a specific EndPoint
     */
    public EndPoint getEndPoint();

    /**
     * Obtain the content associated with this event.
     * <p>
     * See {@link Type} for details regarding the content type for different
     * events.
     *
     * @return the content associated with this event, or <tt>null</tt>
     */
    public Object getContent();

    /**
     * Dispose of the event and optionally return a decoupled version its content.
     * <p>
     * If <tt>fTakeContent</tt> is <tt>true</tt> then the object returned from this
     * method is independent of the disposed event allowing the content to remain
     * valid for application usage. The returned content will be equivalent to the
     * object returned from {@link #getContent}, but may or may not be the same
     * object instance. If <tt>false</tt> is specified then the event and its
     * content will be disposed and <tt>null</tt> will be returned.
     *
     * @param fTakeContent  true if a decoupled version of the content should be
     *                      returned
     *
     * @return  a the content associated with the event, or <tt>null</tt>
     */
    public Object dispose(boolean fTakeContent);

    /**
     * Dispose of the event, releasing any resources associated with it. An
     * Event Collector must ensure that this method is called exactly once for
     * each Event that is emitted. Once this method has been called, any
     * subsequent use of the Event or contents previously obtained from it is
     * illegal. Specifically, this method indicates to the Bus that the event
     * has been received, processed, and that any resources associated with
     * the Event can be reused.
     */
    public void dispose();
    }
