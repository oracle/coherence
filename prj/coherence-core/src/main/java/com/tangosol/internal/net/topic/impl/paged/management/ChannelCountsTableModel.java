/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.management;

import com.tangosol.internal.net.management.model.ModelAttribute;
import com.tangosol.internal.net.management.model.SimpleModelAttribute;
import com.tangosol.internal.net.management.model.TabularModel;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The model for arbitrary counts by channel.
 * <p>
 * The table model has a row with two integer attributes.
 * The first attribute is the channel id, and the second
 * attribute is the count.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 22.06.4
 */
public class ChannelCountsTableModel<M>
        extends TabularModel<Integer[], M>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link ChannelCountsTableModel}.
     *
     * @param fnRowCount  the function that returns the row count for a given model
     * @param fnCount     the function that returns the count value from a model for a given channel
     */
    @SuppressWarnings("unchecked")
    public ChannelCountsTableModel(Function<M, Integer> fnRowCount, BiFunction<M, Integer, Integer> fnCount)
        {
        super("Channels", "Counts by channel", ATTRIBUTE_CHANNEL.getName(),
              new ModelAttribute[]{ATTRIBUTE_CHANNEL, ATTRIBUTE_COUNT},
              fnRowCount, createRowFunction(fnCount));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create the {@link BiFunction} that returns the data for a row in the table.
     *
     * @param fnCount  the {@link Function} that returns a count for a specified channel
     *
     * @return the {@link BiFunction} that returns the data for a row in the table
     */
    private static <M> BiFunction<M, Integer, Integer[]> createRowFunction(BiFunction<M, Integer, Integer> fnCount)
        {
        return (m, c) -> new Integer[]{c, fnCount.apply(m, c)};
        }

    // ----- constants ------------------------------------------------------

    /**
     * The channel attribute.
     */
    protected static final ModelAttribute<Integer[]> ATTRIBUTE_CHANNEL =
            SimpleModelAttribute.intBuilder("Channel", Integer[].class)
                    .withDescription("The channel")
                    .withFunction(an -> an[0])
                    .build();

    /**
     * The channel attribute.
     */
    protected static final ModelAttribute<Integer[]> ATTRIBUTE_COUNT =
            SimpleModelAttribute.intBuilder("Count", Integer[].class)
                    .withDescription("The count")
                    .withFunction(an -> an[1])
                    .build();
    }
