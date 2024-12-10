/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm.misc;

import com.tangosol.util.fsm.Event;
import com.tangosol.util.fsm.ExecutionContext;
import com.tangosol.util.fsm.RollbackTransitionException;
import com.tangosol.util.fsm.TransitionAction;

/**
 * A simple {@link TransitionAction} implementation that just outputs the
 * action information to {@link System#out}.
 *
 * @author Brian Oliver
 */
public class TraceAction<S extends Enum<S>> implements TransitionAction<S>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public void onTransition(String  sTransitionName, S stateFrom, S stateTo,
            Event<S> event, ExecutionContext context)
            throws RollbackTransitionException
        {
        /*
        System.out.printf("(Transition #%d (%s) was from %s to %s due to processing %s)\n\n",
                context.getTransitionCount() + 1,
                sTransitionName,
                stateFrom,
                stateTo,
                event);
        */
        }
    }
