/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.util.DaemonPoolDependencies;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.net.GuardSupport;
import com.tangosol.net.PriorityTask;

import com.tangosol.net.cache.KeyAssociation;

import com.tangosol.util.SafeHashSet;

import com.tangosol.util.fsm.Instruction.ProcessEvent;
import com.tangosol.util.fsm.Instruction.TransitionTo;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.EnumMap;

import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An {@link NonBlockingFiniteStateMachine} is a specialized {@link
 * FiniteStateMachine} implementation that performs transitions
 * asynchronously to the threads that request state changes.  That is,
 * threads that request state transitions are never blocked.  Instead their
 * requests are queued for a single thread to later perform the appropriate
 * transition to the requested state.
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
public class NonBlockingFiniteStateMachine<S extends Enum<S>>
        implements FiniteStateMachine<S>, ExecutionContext
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an {@link NonBlockingFiniteStateMachine} given a {@link Model}.
     *
     * @param sName              the name of the {@link NonBlockingFiniteStateMachine}
     * @param model              the {@link Model} of the {@link NonBlockingFiniteStateMachine}
     * @param stateInitial       the initial state
     * @param daemonPoolDeps     Optional {@link DaemonPoolDependencies} for Daemon Pool that will be used
     *                           for scheduling {@link Transition}s
     * @param fIgnoreExceptions  when <code>true</code> {@link RuntimeException}s will be ignored,
     *                           when <code>false</code> {@link RuntimeException}s will immediately
     *                           stop the {@link NonBlockingFiniteStateMachine}
     * @param deps               the {@link TaskDependencies} specifies the event processing configs.
     */
    public NonBlockingFiniteStateMachine(String sName, Model<S> model, S stateInitial,
            DaemonPoolDependencies daemonPoolDeps, boolean fIgnoreExceptions, TaskDependencies deps)
        {
        // TODO: we should prove that the model is valid
        // ie: no isolated/unreachable states
        // ie: no multiple paths from one state to another state, ensuring that two or more transitions
        // from A to B are not defined ie: that there are no cycles formed by potential "synchronous"
        // state transitions, ensuring that A and B don't have any cycles formed by synchronous state
        // transitions between them, and thus deadlocks in the finite state machine can't occur.

        f_sName               = sName;
        m_stateInitial        = stateInitial;
        m_fAllowTransitions   = true;
        f_atomicTransitions   = new AtomicLong();
        f_atomicPendingEvents = new AtomicLong();
        m_fIgnoreExceptions   = fIgnoreExceptions;
        m_setListeners        = new SafeHashSet();
        f_dependencies        = new DefaultTaskDependencies(deps);

        // force single thread pool to control processing order of state transitions
        DefaultDaemonPoolDependencies depsPool = new DefaultDaemonPoolDependencies(daemonPoolDeps);
        depsPool.setThreadCount(1);
        depsPool.setThreadCountMax(1);
        f_daemonPool = Daemons.newDaemonPool(depsPool);

        // build the transitions table based on the model
        S[]      states   = model.getStates();
        Class<S> clzState = model.getStateClass();

        f_mapTransitions = new EnumMap<>(clzState);

        for (S stateFrom : states)
            {
            f_mapTransitions.put(stateFrom, new EnumMap<>(clzState));
            }

        for (Transition<S> transition : model.getTransitions())
            {
            for (S stateFrom : states)
                {
                if (transition.isStartingState(stateFrom))
                    {
                    f_mapTransitions.get(stateFrom).put(transition.getEndingState(), transition);
                    }
                }
            }

        // create the state entry and exit action tables based on the model
        f_mapEntryActions = new EnumMap<>(clzState);
        f_mapExitActions  = new EnumMap<>(clzState);

        for (S state : states)
            {
            f_mapEntryActions.put(state, model.getStateEntryActions().get(state));
            f_mapExitActions.put(state, model.getStateExitActions().get(state));
            }

        // set the current state
        m_state = null;
        }

    // ----- FiniteStateMachine interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(FiniteStateMachineListener<S> listener)
        {
        m_setListeners.add(listener);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(FiniteStateMachineListener<S> listener)
        {
        m_setListeners.remove(listener);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
        {
        return f_sName;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public S getState()
        {
        return m_state;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTransitionCount()
        {
        return f_atomicTransitions.get();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean start()
        {
        if (!m_fAllowTransitions)
            {
            throw new IllegalStateException("The FiniteStateMachine cannot be started because it was stopped");
            }

        // return true if this is the first start
        boolean fStarting = false;

        if (!m_fStarted)
            {
            f_daemonPool.start();
            m_fAcceptEvents = true;

            process(new TransitionTo<>(m_stateInitial));

            m_fStarted = true;
            fStarting  = true;
            }

        return fStarting;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean stop()
        {
        if (!m_fStarted)
            {
            throw new IllegalStateException("The FiniteStateMachine cannot be stopped " +
                    "because it has never been started");
            }

        boolean fStopping = false;

        if (m_fAcceptEvents)
            {
            f_daemonPool.stop();
            m_fAcceptEvents     = false;
            m_fAllowTransitions = false;
            fStopping           = true;
            }

        return fStopping;
        }

    // ----- NonBlockingFiniteStateMachine methods --------------------------

    /**
     * Requests the {@link FiniteStateMachine} to stop accepting new {@link
     * Event}s to process, wait for any existing queued {@link Event}s to be
     * processed and then stop.
     * <p>
     * Note: Once stopped a {@link FiniteStateMachine} can't be restarted.
     * Instead a new {@link FiniteStateMachine} should be created.
     *
     * @return <code>true</code> if the {@link FiniteStateMachine} was
     *         stopped or <code>false</code> if it was already stopped
     *
     * @throws IllegalStateException  if the FiniteStateMachine was never started
     */
    public synchronized boolean quiesceThenStop()
        {
        if (!m_fStarted)
            {
            throw new IllegalStateException("The FiniteStateMachine cannot be stopped " +
                    "because it has never been started");
            }

        boolean fStopped = false;

        if (m_fAcceptEvents)
            {
            m_fAcceptEvents = false;

            while (f_atomicPendingEvents.get() > 0)
                {
                try
                    {
                    // wait for half a second to see if there are no more pending transitions (this
                    // non-infinite wait is to protect us against the possibility that we miss being notified)
                    Blocking.wait(this, 500);
                    }
                catch (InterruptedException e)
                    {
                    Thread.interrupted();
                    Logger.log(String.format(
                            "[%s]: Thread interrupted while quiescing; stopping immediately", f_sName), Logger.ALWAYS);
                    Logger.log(toString(), Logger.ALWAYS);
                    break;
                    }
                }
            m_fAllowTransitions = false;
            f_daemonPool.stop(); // COH-21710 - stop the worker thread even if there are more pending events;
                                 //             it will no longer process pending events anyway

            fStopped = f_atomicPendingEvents.get() == 0;
            }

        return fStopped;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "NonBlockingFSM status {name:" + f_sName + " isStarted:" + m_fStarted + " isAcceptingEvents:" +
                            m_fAcceptEvents + " PendingEvents:" + f_atomicPendingEvents.get() + "}";
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Event<S> event)
        {
        processLater(event, 0, TimeUnit.SECONDS);
        }

    /**
     * Request the {@link FiniteStateMachine} to process the specified {@link
     * Event} as soon as possible.
     * <p>
     * Note: This method is semantically equivalent to {@link #process(Event)}.
     *
     * @param event  the {@link Event} for the {@link FiniteStateMachine} to process
     */
    public void processLater(Event<S> event)
        {
        processLater(event, 0, TimeUnit.SECONDS);
        }

    /**
     * Request the {@link FiniteStateMachine} to process the specified {@link
     * Event} at some point in the future (represented as a duration to wait
     * from the moment the method is called).
     * <p>
     * Note: There's no guarantee that the {@link Event} will processed
     * because:
     * <ol>
     *     <li>the {@link Transition} to be performed for the
     *         {@link Event} is invalid as the {@link FiniteStateMachine} is not in the
     *         required starting state.
     *     <li>the {@link FiniteStateMachine} may have been stopped.
     * </ol>
     *
     * @param event     the {@link Event} for the {@link FiniteStateMachine} to process
     * @param duration  the amount of the {@link TimeUnit} to wait before the {@link Event} is processed
     * @param timeUnit  the {@link TimeUnit}
     */
    public void processLater(Event<S> event, long duration, TimeUnit timeUnit)
        {
        if (m_fAcceptEvents)
            {
            final Event<S> preparedEvent = prepareEvent(event);

            if (preparedEvent == null)
                {
                // uncomment log when need to troubleshoot problem with FSM
                // Logger.finest(String.format("[%s]: Ignoring event %s as it vetoed being prepared",
                //        f_sName, event));
                }
            else
                {
                f_daemonPool.schedule(new Task(preparedEvent, f_dependencies), timeUnit.toMillis(duration));
                }
            }
        else
            {
            // uncomment log when need to troubleshoot problem with FSM
            // Logger.finest(String.format("[%s]: Ignoring request to process the event %s in %d %s as the " +
            //                 "machine is no longer accepting new transitions",
            //                f_sName, event, duration, timeUnit));
            }
        }

    /**
     * A PriorityTask implementation to process a requested event.
     */
    protected class Task
            implements Runnable, PriorityTask, KeyAssociation
        {
        /**
         * Create a Task with given event.
         *
         * @param event  the event that needs to be processed
         * @param deps   the task dependencies
         */
        public Task(Event<S> event, TaskDependencies deps)
            {
            f_event        = event;
            f_dependencies = deps;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getAssociatedKey()
            {
            return f_dependencies.getAssociatedKey();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run()
            {
            processEvent(f_event);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getSchedulingPriority()
            {
            return PriorityTask.SCHEDULE_STANDARD;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getExecutionTimeoutMillis()
            {
            return f_dependencies.getExecutionTimeoutMillis();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getRequestTimeoutMillis()
            {
            // this is not used in event processing
            return 0;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void runCanceled(boolean fAbandoned)
            {

            }

        /**
         * An event needs to be processed.
         */
        private final Event<S> f_event;

        /**
         * The dependencies to configure the Task.
         */
        private final TaskDependencies f_dependencies;
        }

    /**
     * Dependencies for Task.
     */
    public interface TaskDependencies
        {
        /**
         * Return the execution timeout for the task in millisecond.
         * @return the execution timeout
         */
        public long getExecutionTimeoutMillis();

        /**
         * Return the associated key for the task.
         * @return the associated key
         */
        public Object getAssociatedKey();
        }


    /**
     * Implementation of Dependencies for Task
     */
    public static class DefaultTaskDependencies
            implements TaskDependencies
        {
        /**
         * Default constructor.
         */
        public DefaultTaskDependencies()
            {

            }

        /**
         * Create a DefaultTaskDependencies with provided {@link TaskDependencies}.
         *
         * @param  deps  the TaskDependencies
         */
        public DefaultTaskDependencies(TaskDependencies deps)
            {
            if (deps != null)
                {
                m_cExecutionTimeout = deps.getExecutionTimeoutMillis();
                m_oAssociatedKey    = deps.getAssociatedKey();
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getExecutionTimeoutMillis()
            {
            return m_cExecutionTimeout;
            }

        /**
         * Configure the execution timeout for Task.
         *
         * @param  timeout  execution timeout in millisecond
         *
         * @return this object
         */
        public DefaultTaskDependencies setExecutionTimeoutMillis(long timeout)
            {
            m_cExecutionTimeout = timeout;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getAssociatedKey()
            {
            return m_oAssociatedKey;
            }

        /**
         * Configure the associated key for Task.
         *
         * @param  key  the associated key
         *
         * @return this object.
         */
        public DefaultTaskDependencies setAssociatedKey(Object key)
            {
            m_oAssociatedKey = key;
            return this;
            }

        /**
         * The execution timeout for Task.
         */
        private long m_cExecutionTimeout = 5000L;

        /**
         * The associated key for Task.
         */
        private Object m_oAssociatedKey = "FSM-Task";
        }

    /**
     * Prepares an {@link Event} to be accepted for processing.
     *
     * @param event  the {@link Event} to prepare
     *
     * @return the prepared {@link Event} (or <code>null</code> if the event
     *         should not be processed)
     */
    private Event<S> prepareEvent(Event<S> event)
        {
        // assume the worst - no event is prepared
        Event<S> prepared = null;

        if (m_fAcceptEvents)
            {
            // ensure lifecycle aware events are notified
            if (event instanceof LifecycleAwareEvent)
                {
                LifecycleAwareEvent<S> lifecycleAwareEvent = (LifecycleAwareEvent<S>) event;

                prepared = lifecycleAwareEvent.onAccept(this) ? lifecycleAwareEvent : null;
                }
            else
                {
                prepared = event;
                }
            }

        if (prepared != null)
            {
            // increase the number of events that are now pending
            f_atomicPendingEvents.incrementAndGet();
            }

        return prepared;
        }

    /**
     * Determines if there are any pending {@link Event}s for the {@link
     * FiniteStateMachine} to process.
     * <p>
     * Note: If the {@link FiniteStateMachine} can no longer process {@link
     * Event}s false will be returned.
     *
     * @return <code>true</code> if there are pending {@link Event}s and
     *         {@link Event}s can be processed, <code>false</code> otherwise
     */
    public boolean hasPendingEvents()
        {
        return m_fAllowTransitions && f_atomicPendingEvents.get() > 0;
        }

    /**
     * Processes the specified {@link Event}, causing the {@link
     * FiniteStateMachine} to {@link Transition} to a new state if required.
     *
     * @param event  the {@link Event} to process
     */
    @SuppressWarnings("unchecked")
    private void processEvent(Event<S> event)
        {
        // loop through to handle subsequent events, if any
        while (event != null && m_fAllowTransitions)
            {
            // notify the lifecycle aware event of the commencement of processing
            if (event instanceof LifecycleAwareEvent)
                {
                LifecycleAwareEvent<S> lifecycleAwareEvent = (LifecycleAwareEvent<S>) event;

                lifecycleAwareEvent.onProcessing(this);
                }

            // determine the desired state from the event
            S stateCurrent = getState();
            S stateDesired = event.getDesiredState(stateCurrent, this);

            boolean fIsInitialState = stateCurrent == null;

            // as we're processing an event, decrease the counter of
            // pending events
            f_atomicPendingEvents.decrementAndGet();

            // if there's no desired state, we do nothing
            if (stateDesired == null)
                {
                // do nothing for the event
                // uncomment log when need to troubleshoot problem with FSM
                // Logger.finest(String.format("[%s]: Ignoring event %s as it produced a null desired state.",
                //                f_sName, event));

                // no more events to process
                event = null;
                }
            else
                {
                // assume no transition will be made
                Transition<S> transition = null;

                // when we have a current and desired state, we can
                // perform a transition
                if (!fIsInitialState)
                    {
                    // determine the appropriate transition from the
                    // current state to the desired state (using the transition table)
                    transition = f_mapTransitions.get(stateCurrent).get(stateDesired);

                    if (transition == null)
                        {
                        // there's no transition from the current state to the
                        // desired state, so ignore the request
                        // uncomment log when need to troubleshoot problem with FSM
                        // Logger.finest(String.format("[%s]: Can't find a valid transition from %s to %s. " +
                        //     "Ignoring event %s.", f_sName, stateCurrent, stateDesired, event));

                        event = null;
                        }
                    else
                        {
                        // fetch the action to execute for the transition
                        TransitionAction<S> actionTransition = transition.getAction();

                        // attempt to execute the action for the transition
                        // (if we have one)
                        if (actionTransition != null)
                            {
                            try
                                {
                                // perform the action
                                actionTransition.onTransition(
                                        transition.getName(),
                                        stateCurrent,
                                        transition.getEndingState(),
                                        event,
                                        this);
                                }
                            catch (RollbackTransitionException e)
                                {
                                Logger.log(String.format("[%s]: Transition for event %s from " +
                                        "%s to %s has been rolled back due to:\n%s", f_sName, event,
                                                stateCurrent, stateDesired, e), Logger.ALWAYS);

                                event = null;
                                }
                            catch (RuntimeException e)
                                {
                                // todo: temporary, redo this when we redo all logging
                                Logger.log(e, Logger.ALWAYS);

                                if (m_fIgnoreExceptions)
                                    {
                                    Logger.finest(String.format("[%s]: Transition Action %s for event %s " +
                                            "from %s to %s raised runtime exception (continuing with " +
                                            "transition and ignoring the exception):\n%s", f_sName,
                                            actionTransition, event, stateCurrent, stateDesired, e));
                                    }
                                else
                                    {
                                    m_fAcceptEvents     = false;
                                    m_fAllowTransitions = false;

                                    StringWriter writerString = new StringWriter();
                                    PrintWriter  writerPrint  = new PrintWriter(writerString);

                                    e.printStackTrace(writerPrint);
                                    writerPrint.close();

                                    Logger.log(String.format("[%s]: Stopping the machine as the " +
                                            "Transition Action %s for event %s from %s to %s raised "+
                                            "runtime exception %s:\n%s", f_sName, actionTransition, event,
                                            stateCurrent, stateDesired, e, writerString .toString()), Logger.ALWAYS);

                                    break;
                                    }
                                }
                            }
                        }
                    }

                // now perform exit and entry actions
                if (event != null)
                    {
                    // notify the lifecycle aware event of the completion of processing
                    if (event instanceof LifecycleAwareEvent)
                        {
                        LifecycleAwareEvent<S> lifecycleAwareEvent = (LifecycleAwareEvent<S>) event;

                        lifecycleAwareEvent.onProcessed(this);
                        }

                    // perform the exit action
                    if (!fIsInitialState)
                        {
                        StateExitAction<S> actionExit = f_mapExitActions.get(stateCurrent);

                        if (actionExit != null)
                            {
                            try
                                {
                                actionExit.onExitState(stateCurrent, event,  this);
                                }
                            catch (RuntimeException e)
                                {
                                if (m_fIgnoreExceptions)
                                    {
                                    Logger.warn(String
                                            .format("[%s]: State Exit Action %s for event %s from %s to %s " +
                                                    "raised runtime exception (continuing with transition " +
                                                    "and ignoring the exception):\n%s", f_sName, actionExit,
                                                    event, stateCurrent, stateDesired, e));
                                    }
                                else
                                    {
                                    m_fAcceptEvents     = false;
                                    m_fAllowTransitions = false;

                                    StringWriter writerString = new StringWriter();
                                    PrintWriter  writerPrint  = new PrintWriter(writerString);

                                    e.printStackTrace(writerPrint);
                                    writerPrint.close();

                                    Logger.err(String
                                            .format("[%s]: Stopping the machine as the State Exit Action %s "+
                                                    "for event %s from %s to %s raised runtime exception %s:\n%s",
                                                    f_sName, actionExit, event, stateCurrent,
                                                    stateDesired, e, writerString.toString()));

                                    break;
                                    }
                                }
                            }
                        else
                            {
                            // uncomment log when need to troubleshoot problem with FSM
                            //   Logger.finest(String.format("[%s]: No Exit Action defined for %s",
                            //           f_sName, stateCurrent));
                            }
                        }

                    // we're now in the desired state so set it
                    m_state = stateDesired;

                    // as we've made a transition, count it
                    if (!fIsInitialState)
                        {
                        f_atomicTransitions.incrementAndGet();
                        }

                    // notify the listeners of the transition
                    for (FiniteStateMachineListener<S> listener : m_setListeners)
                        {
                        try
                            {
                            listener.onTransition(stateCurrent, stateDesired);
                            }
                        catch (RuntimeException e)
                            {
                            Logger.warn("Exception occurred in FiniteStateMachineListener", e);
                            }
                        }

                    // the instruction to perform after setting the state
                    Instruction instruction = Instruction.NOTHING;

                    // perform the entry action
                    StateEntryAction<S> actionEntry = f_mapEntryActions.get(stateDesired);

                    if (actionEntry != null)
                        {
                        try
                            {
                            // execute the enter state action and determine what to do next
                            instruction = actionEntry.onEnterState(stateCurrent, stateDesired, event, this);
                            }
                        catch (RuntimeException e)
                            {
                            if (m_fIgnoreExceptions)
                                {
                                Logger.finest(String
                                        .format("[%s]: State Entry Action %s for event %s from %s to %s raised runtime " +
                                                "exception (continuing and ignoring the exception):\n%s",
                                                f_sName, actionEntry, event, stateCurrent, stateDesired, e));
                                }
                            else
                                {
                                m_fAcceptEvents     = false;
                                m_fAllowTransitions = false;

                                StringWriter writerString = new StringWriter();
                                PrintWriter writerPrint   = new PrintWriter(writerString);

                                e.printStackTrace(writerPrint);
                                writerPrint.close();

                                Logger.log(String
                                        .format("[%s]: Stopping the machine as the State Entry Action %s for event %s " +
                                                "from %s to %s raised runtime exception %s:\n%s",
                                                f_sName, actionEntry, event, stateCurrent,
                                                stateDesired, e, writerString.toString()), Logger.ALWAYS);

                                break;
                                }
                            }

                        }
                    else
                        {
                        // uncomment log when need to troubleshoot problem with FSM
                        // Logger.fine(String.format("[%s]: No Entry Action defined for %s",
                        //        f_sName, stateDesired));
                        }

                    // now perform the appropriate instruction based on the entry action
                    if (instruction == null || instruction == Instruction.NOTHING)
                        {
                        // nothing to do for the next instruction
                        event = null;
                        }
                    else if (instruction == Instruction.STOP)
                        {
                        // stop the machine immediately (don't wait for scheduled transitions to complete)
                        stop();
                        }
                    else if (instruction instanceof TransitionTo)
                        {
                        // when the instruction is to "transition", we execute the transition
                        // immediately as this prevents the possible race-condition where
                        // asynchronously scheduled events can become "interleaved"
                        // between the completion of a state change and a move to another the desired state
                        TransitionTo<S> eventTransitionTo = (TransitionTo<S>) instruction;

                        event = prepareEvent(eventTransitionTo);
                        }
                    else if (instruction instanceof DelayedTransitionTo)
                        {
                        DelayedTransitionTo<S> eventDelayedTransitionTo = (DelayedTransitionTo<S>) instruction;

                        // schedule the transition event to be processed (and prepared) in the future
                        processLater(eventDelayedTransitionTo,
                                eventDelayedTransitionTo.getDuration(),
                                eventDelayedTransitionTo.getTimeUnit());

                        event = null;
                        }
                    else if (instruction instanceof ProcessEvent)
                        {
                        ProcessEvent<S> eventDelegating = (ProcessEvent<S>) instruction;

                        event = prepareEvent(eventDelegating.getEvent());
                        }
                    else if (instruction instanceof ProcessEventLater)
                        {
                        ProcessEventLater<S> eventDelayedInstruction = (ProcessEventLater<S>) instruction;

                        // schedule the event to be processed (and prepared) in the future
                        processLater(eventDelayedInstruction.getEvent(),
                                eventDelayedInstruction.getDuration(),
                                eventDelayedInstruction.getTimeUnit());

                        event = null;
                        }
                    else
                        {
                        Logger.warn(String.format("[%s]: Ignoring Instruction [%s] returned as part of "
                                + "transition to %s as it an unknown type for this Finite State Machine.",
                                   f_sName, instruction, stateDesired));
                        }
                    }
                }

            GuardSupport.heartbeat();
            }

        // when this is the last pending transition and we're not accepting any more,
        // notify waiting threads that we're done
        if (!m_fAcceptEvents && f_atomicPendingEvents.get() == 0)
            {
            synchronized(this)
                {
                notifyAll();
                }
            }
        }

    // ----- inner class CoalescedEvent -------------------------------------

    /**
     * A {@link CoalescedEvent} is a {@link LifecycleAwareEvent} that
     * coalesces other (wrapped) {@link Event}s with the same discriminator
     * so that only one {@link Event} actually executes.
     * <p>
     * For example:  Given 10 {@link Event}s submitted to a {@link
     * NonBlockingFiniteStateMachine} with the same discriminator, only one
     * of the said {@link Event}s will be processed.  All others will be
     * discarded.  Once the {@link CoalescedEvent} has been processed, a new
     * batch may be created when another {@link CoalescedEvent} of the same
     * discriminator is submitted.
     * <p>
     * The actual {@link Event} processed depends on the mode of coalescing
     * required.  The first {@link CoalescedEvent} submitted to a {@link
     * NonBlockingFiniteStateMachine} for a specific discriminator
     * effectively starts the coalescing of {@link Event}s for the said
     * discriminator.  When the mode is set to {@link CoalescedEvent.Process#FIRST}, then
     * the first {@link Event} (starting the coalescing) will be processed
     * and others will be discarded. When the mode is set of {@link
     * CoalescedEvent.Process#MOST_RECENT} then the most recently submitted {@link Event}
     * will be processed and likewise, all others for the same discriminator
     * will be discarded.
     *
     * @param <S> the type of the state for the {@link FiniteStateMachine}
     */
    public static class CoalescedEvent<S extends Enum<S>>
            implements LifecycleAwareEvent<S>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link CoalescedEvent} of the specified {@link Event}
         * type using {@link CoalescedEvent.Process#FIRST}.
         *
         * @param event the {@link Event} to be executed when coalesced
         */
        public CoalescedEvent(Event<S> event)
            {
            this(event, Process.FIRST, event.getClass());
            }

        /**
         * Constructs a {@link CoalescedEvent} of the specified {@link Event} type.
         *
         * @param event  the {@link Event} to be coalesced
         * @param mode   which {@link CoalescedEvent}s to process
         */
        public CoalescedEvent(Event<S> event, Process mode)
            {
            this(event, mode, event.getClass());
            }

        /**
         * Constructs a {@link CoalescedEvent} with the specified discriminator and {@link Event}.
         *
         * @param event         the {@link Event} to be coalesced
         * @param mode          which {@link CoalescedEvent}s to process
         * @param discriminator the descriminator used to uniquely coalesce
         *                      the {@link Event}
         */
        public CoalescedEvent(Event<S> event, Process mode, Object discriminator)
            {
            m_oDiscriminator = discriminator == null ? Void.class : discriminator;
            m_event          = event;
            m_mode           = mode;
            m_eventChosen    = null;
            }

        // ----- Event interface --------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public S getDesiredState(S state, ExecutionContext context)
            {
            return m_eventChosen.getDesiredState(state, context);
            }

        // ----- LifecycleAwareEvent interface ------------------------------

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("rawtypes")
        @Override
        public boolean onAccept(ExecutionContext context)
            {
            Event<S> event = m_event;

            // CoalescingEvents may only be accepted by NonBlockingFiniteStateMachines
            if (context instanceof NonBlockingFiniteStateMachine)
                {
                // ensure that the actual event is accepted
                // (there's no reason to accept unacceptable events)
                boolean fIsAccepted = event instanceof LifecycleAwareEvent
                        ? ((LifecycleAwareEvent<S>) event).onAccept(context)
                        : true;

                // replace the provided discriminator with one that is scoped
                // by the NonBlockingFiniteStateMachine;
                Discriminator discriminator = new Discriminator((NonBlockingFiniteStateMachine) context,
                        m_oDiscriminator);

                m_oDiscriminator = discriminator;

                if (fIsAccepted)
                    {
                    fIsAccepted = m_mode == Process.FIRST
                            ? s_mapEventsByDiscriminator.putIfAbsent(discriminator, event) == null
                            : s_mapEventsByDiscriminator.put(discriminator, event) == null;
                    }

                return fIsAccepted;
                }
            else
                {
                throw new UnsupportedOperationException(String.format(
                        "CoalescingEvents may only be used with %s instance",
                        NonBlockingFiniteStateMachine.class.getName()));
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onProcessed(ExecutionContext context)
            {
            if (m_eventChosen instanceof LifecycleAwareEvent)
                {
                ((LifecycleAwareEvent<S>) m_eventChosen).onProcessed(context);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onProcessing(ExecutionContext context)
            {
            // remove the actual event to be processed for the discriminator
            // (we do this because this event that we're processing may have
            // been replaced ie: coalesced by another event)
            Event<S> event = m_eventChosen = (Event<S>) s_mapEventsByDiscriminator.remove(m_oDiscriminator);

            if (event instanceof LifecycleAwareEvent)
                {
                ((LifecycleAwareEvent<S>) event).onProcessing(context);
                }
            }

        // ----- Object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return String.format("CoalescedEvent{%s, discriminator=%s, mode=%s}",
                            m_event, m_oDiscriminator, m_mode);
            }

        // ----- inner class Discriminator ----------------------------------

        /**
         * A {@link Discriminator} is an object that is used to uniquely
         * differentiate events to be coalesced, scoped by a {@link
         * NonBlockingFiniteStateMachine}.
         */
        public static class Discriminator
            {
            /**
             * Constructs a {@link Discriminator} for the specified {@link
             * NonBlockingFiniteStateMachine}.
             *
             * @param machine         the {@link NonBlockingFiniteStateMachine}
             * @param oDiscriminator  the discriminator
             */
            public Discriminator(NonBlockingFiniteStateMachine<?> machine, Object oDiscriminator)
                {
                m_machine        = machine;
                m_oDiscriminator = oDiscriminator;
                }

            /**
             * {@inheritDoc}
             */
            @Override
            public int hashCode()
                {
                final int prime = 31;
                int result = 1;

                result = prime * result + ((m_oDiscriminator == null) ? 0 : m_oDiscriminator.hashCode());
                result = prime * result + ((m_machine == null) ? 0 : m_machine.hashCode());

                return result;
                }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean equals(Object obj)
                {
                if (this == obj)
                    {
                    return true;
                    }

                if (obj == null)
                    {
                    return false;
                    }

                if (getClass() != obj.getClass())
                    {
                    return false;
                    }

                Discriminator other = (Discriminator) obj;

                if (m_oDiscriminator == null)
                    {
                    if (other.m_oDiscriminator != null)
                        {
                        return false;
                        }
                    }
                else if (!m_oDiscriminator.equals(other.m_oDiscriminator))
                    {
                    return false;
                    }

                if (m_machine == null)
                    {
                    if (other.m_machine != null)
                        {
                        return false;
                        }
                    }
                else if (!m_machine.equals(other.m_machine))
                    {
                    return false;
                    }

                return true;
                }

            /**
             * The {@link NonBlockingFiniteStateMachine} to which the
             * discriminator applies.
             */
            private NonBlockingFiniteStateMachine<?> m_machine;

            /**
             * The actual discriminator (not null).
             */
            private Object m_oDiscriminator;
            }

        /**
         * Initialization of shared state.
         */
        static
            {
            s_mapEventsByDiscriminator = new ConcurrentHashMap<Discriminator, Event<?>>();
            }

        // ----- inner enum Process -----------------------------------------

        /**
         * The {@link CoalescedEvent} to process.
         */
        public static enum Process
            {
            /**
             * FIRST indicates that the first submitted {@link Event}
             * for a specific discriminator will be the one which is
             * processed. All other submitted {@link CoalescedEvent}s of the
             * same discriminator will be discarded.
             */
            FIRST,

            /**
             * MOST_RECENT indicates that the most recently
             * submitted {@link Event} for a specified discriminator will be
             * processed. All other previously submitted {@link Event}s of
             * the same discriminator will be discarded.
             */
            MOST_RECENT
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Event}s to be processed, arranged by discriminator.
         */
        private static ConcurrentHashMap<Discriminator, Event<?>> s_mapEventsByDiscriminator;

        /**
         * The discriminator/identifier that is used to coalesce {@link
         * Event}s of the same "type".
         */
        private Object m_oDiscriminator;

        /**
         * The {@link Event} to be coalesced.
         */
        private Event<S> m_event;

        /**
         * The mode of coalescing to use for the {@link Event}.
         */
        private Process m_mode;

        /**
         * The {@link Event} that is eventually chosen to process (from all
         * of those submitted and coalesced).
         */
        private Event<S> m_eventChosen;
        }

    // ----- inner class DelayedTransitionTo --------------------------------

    /**
     * A {@link DelayedTransitionTo} is a specialized {@link Instruction} for
     * {@link NonBlockingFiniteStateMachine}s that enables a {@link
     * StateEntryAction} to request a delayed transition to another state,
     * unlike a {@link TransitionTo} {@link Instruction} which occurs
     * immediately.
     *
     * @see TransitionTo
     */
    public static class DelayedTransitionTo<S extends Enum<S>>
            implements Instruction, Event<S>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link DelayedTransitionTo} without a specified time
         * (to be schedule as soon as possible).
         *
         * @param desiredState the desired state to which to transition
         */
        public DelayedTransitionTo(S desiredState)
            {
            this(desiredState, 0, TimeUnit.MILLISECONDS);
            }

        /**
         * Constructs a {@link DelayedTransitionTo} with the specified time.
         *
         * @param desiredState  the desired state to which to transition
         * @param lDuration     the amount of time to wait before the desired
         *                      transition should occur
         * @param timeUnit      the unit of time measure
         */
        public DelayedTransitionTo(S desiredState, long lDuration, TimeUnit timeUnit)
            {
            m_desiredState = desiredState;
            m_lDuration    = lDuration;
            m_timeUnit     = timeUnit;
            }

        // ----- Event interface --------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public S getDesiredState(S currentState, ExecutionContext context)
            {
            return m_desiredState;
            }

        // ----- DelayedTransitionTo methods --------------------------------

        /**
         * Obtains the amount of time to wait before the transition to the
         * desired state should occur
         *
         * @return the amount of time in the {@link #getTimeUnit()}
         */
        public long getDuration()
            {
            return m_lDuration;
            }

        /**
         * Obtains the {@link TimeUnit} for the {@link #getDuration()}
         *
         * @return the {@link TimeUnit}
         */
        public TimeUnit getTimeUnit()
            {
            return m_timeUnit;
            }

        // ----- data members -----------------------------------------------

        /**
         * The desired state.
         */
        private S m_desiredState;

        /**
         * The amount of time to wait before the transition should occur.
         */
        private long m_lDuration;

        /**
         * The {@link TimeUnit} for the delay time.
         */
        private TimeUnit m_timeUnit;
        }

    // ----- inner class ProcessEventLater ----------------------------------

    /**
     * A specialized {@link Instruction} for {@link NonBlockingFiniteStateMachine}s
     * that enables a {@link StateEntryAction} to request an {@link Event} to
     * be processed at some point in the future.
     * <p>
     * This is the same as calling {@link NonBlockingFiniteStateMachine#processLater(Event,
     * long, TimeUnit)}
     *
     * @see ProcessEvent
     */
    public static class ProcessEventLater<S extends Enum<S>>
            implements Instruction
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link ProcessEventLater} without a specified time
         * (to be schedule as soon as possible).
         *
         * @param event the {@link Event} to process later
         */
        public ProcessEventLater(Event<S> event)
            {
            this(event, 0, TimeUnit.MILLISECONDS);
            }

        /**
         * Constructs a {@link ProcessEventLater} with the specified delay time.
         *
         * @param event     the {@link Event} to process later
         * @param duration  the amount of time to wait before processing the {@link Event}
         * @param timeUnit  the unit of time measure
         */
        public ProcessEventLater(Event<S> event, long duration, TimeUnit timeUnit)
            {
            m_event     = event;
            m_lDuration = duration;
            m_timeUnit  = timeUnit;
            }

        // ----- ProcessEventLater methods ----------------------------------

        /**
         * Obtain the {@link Event} to process later.
         *
         * @return the {@link Event} to process
         */
        public Event<S> getEvent()
            {
            return m_event;
            }

        /**
         * Obtains the amount of time to wait before the transition to the
         * desired state should occur.
         *
         * @return the amount of time in the {@link #getTimeUnit()}
         */
        public long getDuration()
            {
            return m_lDuration;
            }

        /**
         * Obtains the {@link TimeUnit} for the {@link #getDuration()}.
         *
         * @return the {@link TimeUnit}
         */
        public TimeUnit getTimeUnit()
            {
            return m_timeUnit;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Event} to process later.
         */
        private Event<S> m_event;

        /**
         * The amount of time to wait before the processing the {@link Event}.
         */
        private long m_lDuration;

        /**
         * The {@link TimeUnit} for the delay time.
         */
        private TimeUnit m_timeUnit;
        }

    // ----- inner class SubsequentEvent ------------------------------------

    /**
     * A {@link SubsequentEvent} is an {@link Event} that ensures that
     * another (wrapped) {@link Event} to occur if an only if the {@link
     * FiniteStateMachine} is at a certain transition count.  Should an
     * attempt to process the wrapped {@link Event} occur at another
     * transition count, processing of the said event is ignored.
     * <p>
     * {@link SubsequentEvent}s are designed to provide the ability for
     * future scheduled {@link Event}s to be skipped if another {@link Event}
     * has been processed between the time when the {@link SubsequentEvent}
     * was requested to be processed and when it was actually processed. That
     * is, the purpose of this is to allow an {@link Event} to be skipped if
     * other {@link Event}s interleave between the time when the said {@link
     * Event} was actually scheduled and when it was actually meant to be
     * processed.
     *
     * @param <S> the state type of the {@link FiniteStateMachine}
     */
    public static class SubsequentEvent<S extends Enum<S>>
            implements LifecycleAwareEvent<S>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link SubsequentEvent}.
         *
         * @param event  the actual event to process
         */
        public SubsequentEvent(Event<S> event)
            {
            m_cTransitions = -1;
            m_event        = event;
            }

        // ----- LifecycleAwareEvent interface ------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onAccept(ExecutionContext context)
            {
            // when being accepted use context to determine the transition count
            // at which the event should be processed
            m_cTransitions = context.getTransitionCount();

            // ensure the event can be accepted (if it's a lifecycle aware event)
            // otherwise always accept it
            return m_event instanceof LifecycleAwareEvent ?
                    ((LifecycleAwareEvent<S>) m_event).onAccept(context) :
                    true;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onProcessed(ExecutionContext context)
            {
            if (m_event instanceof LifecycleAwareEvent)
                {
                ((LifecycleAwareEvent<S>) m_event).onProcessed(context);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onProcessing(ExecutionContext context)
            {
            if (m_event instanceof LifecycleAwareEvent)
                {
                ((LifecycleAwareEvent<S>) m_event).onProcessing(context);
                }
            }

        // ----- Event interface --------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public S getDesiredState(S currentState, ExecutionContext context)
            {
            if (context.getTransitionCount() == m_cTransitions)
                {
                return m_event.getDesiredState(currentState, context);
                }
            else
                {
                // uncomment log when need to troubleshoot problem with FSM
                // Logger.finest(String.format("[%s]: Skipping event %s since another event " +
                //        "was interleaved between when it was scheduled and when it was processed",
                //         context.getName(), this));


                // by returning null we skip the processing of the event
                return null;
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return String.format("SubsequentEvent{%s, @Transition #%d}", m_event,
                            m_cTransitions + 1);
            }

        // ----- data members -----------------------------------------------

        /**
         * The transition count that the {@link FiniteStateMachine} must be
         * at in order for the wrapped {@link Event} to be processed.
         */
        private long m_cTransitions;

        /**
         * The actual {@link Event}.
         */
        private Event<S> m_event;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The dependencies used to configure event processing Task.
     */
    private final TaskDependencies f_dependencies;

    /**
     * The name of the {@link NonBlockingFiniteStateMachine}.
     */
    private final String f_sName;

    /**
     * The state of the {@link FiniteStateMachine}.
     */
    private volatile S m_state;

    /**
     * The initial state.
     */
    private final S m_stateInitial;

    /**
     * The {@link Transition} table (by starting and ending states).
     */
    private final EnumMap<S, EnumMap<S, Transition<S>>> f_mapTransitions;

    /**
     * The {@link StateEntryAction} table (by state).
     */
    private final EnumMap<S, StateEntryAction<S>> f_mapEntryActions;

    /**
     * The {@link StateExitAction} table (by state).
     */
    private final EnumMap<S, StateExitAction<S>> f_mapExitActions;

    /**
     * The number of transitions that have occurred in the {@link FiniteStateMachine}.
     */
    private final AtomicLong f_atomicTransitions;

    /**
     * Is the {@link FiniteStateMachine} accepting {@link Event}s to trigger
     * {@link Transition}s?
     * <p>
     * This flag allows us to stop the {@link FiniteStateMachine} from
     * accepting {@link Event}s (that may cause {@link Transition}s), but
     * allows the {@link FiniteStateMachine} to continue processing
     * previously accepted {@link Event}s.
     */
    private volatile boolean m_fAcceptEvents;

    /**
     * True if the FiniteStateMachine has been started.
     */
    private volatile boolean m_fStarted;

    /**
     * Is the {@link FiniteStateMachine} allowed to perform {@link
     * Transition}s?
     * <p>
     * This flag determines if the {@link FiniteStateMachine} is operational.
     * Once it can no longer perform {@link Transition}s, the {@link
     * FiniteStateMachine} is "dead" and can no longer be used.
     */
    private volatile boolean m_fAllowTransitions;

    /**
     * The number of pending, ie: queued, {@link Event}s to be processed.
     */
    private final AtomicLong f_atomicPendingEvents;

    /**
     * A {@link ScheduledExecutorService} that will be used to schedule
     * {@link Transition}s for the {@link FiniteStateMachine}.
     * <p>
     * Note: Only threads on this {@link ScheduledExecutorService} may apply
     * a {@link Transition}.
     */
    private final DaemonPool f_daemonPool;

    /**
     * When <code>true</code> {@link RuntimeException}s be ignored (will not
     * stop the {@link FiniteStateMachine}).
     * <p>
     * When <code>false</code> {@link RuntimeException}s be will immediately
     * stop the {@link FiniteStateMachine}.
     */
    private final boolean m_fIgnoreExceptions;

    /**
     * The set of {@link FiniteStateMachineListener}s.
     */
    private final Set<FiniteStateMachineListener<S>> m_setListeners;
    }
