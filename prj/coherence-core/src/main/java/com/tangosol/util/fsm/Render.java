/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

/**
 * Provides Render capabilities for {@link FiniteStateMachine}s, in particular their {@link Model}s.
 *
 * @author bko
 * @since Coherence 12.2.1
 */
public class Render
    {
    /**
     * Produces a GraphViz representation of a Model.
     *
     * @param <S>       the type of state of the FiniteStateMachine
     * @param model     the Model to render
     * @param fVerbose  if true, label transitions
     *                  (this results in a larger graph)
     *
     * @return a String representing a GraphViz model
     */
    public static <S extends Enum<S>> String asGraphViz(Model<S> model, boolean fVerbose)
        {
        StringBuilder builder = new StringBuilder();

        builder.append("digraph finite_state_machine {\n");
        builder.append("    rankdir=LR;\n");
        builder.append("    node [shape=circle];\n");

        for (S state : model.getStates())
            {
            builder.append("    ").append(state.name()).append(" [ label=\"").append(state.name()).append("\" ];\n");
            }

        for (S state : model.getStates())
            {
            for (Transition<S> transition : model.getTransitions())
                {
                if (transition.isStartingState(state))
                    {
                    builder.append("    ").append(state.name()).append(" -> ")
                            .append(transition.getEndingState().name());

                    if (fVerbose)
                        {
                        builder.append("[ label=\"").append(transition.getName()).append("\" ];\n");
                        }
                    else
                        {
                        builder.append(";\n");
                        }

                    }
                }
            }
        builder.append("}\n");

        return builder.toString();
        }
    }
