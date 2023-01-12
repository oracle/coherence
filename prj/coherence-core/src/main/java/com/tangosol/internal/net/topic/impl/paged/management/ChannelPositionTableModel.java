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

import com.tangosol.net.topic.Position;

import java.util.Map;

/**
 * The model for positions in channels.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 22.06.4
 */
public class ChannelPositionTableModel
        extends TabularModel<Object[], Map<Integer, Position>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link ChannelPositionTableModel}.
     */
    @SuppressWarnings("unchecked")
    public ChannelPositionTableModel()
        {
        super("Positions", "Positions by channel", ATTRIBUTE_CHANNEL.getName(),
              new ModelAttribute[]{ATTRIBUTE_CHANNEL, ATTRIBUTE_POSITION}, Map::size, ChannelPositionTableModel::getRow);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the table row for a given channel.
     *
     * @param map       the map of data keyed by channel
     * @param nChannel  the channel for the row
     *
     * @return the row data, which is an {@link Object} array, where
     *         element zero is the channel id, and element one is the
     *         {@link Position}
     */
    private static Object[] getRow(Map<Integer, Position> map, int nChannel)
        {
        return new Object[]{nChannel, String.valueOf(map.get(nChannel))};
        }

    // ----- constants ------------------------------------------------------

    /**
     * The channel attribute.
     */
    protected static final ModelAttribute<Object[]> ATTRIBUTE_CHANNEL =
            SimpleModelAttribute.intBuilder("Channel", Object[].class)
                    .withDescription("The channel")
                    .withFunction(ao -> ao[0])
                    .build();

    /**
     * The channel attribute.
     */
    protected static final ModelAttribute<Object[]> ATTRIBUTE_POSITION =
            SimpleModelAttribute.stringBuilder("Position", Object[].class)
                    .withDescription("The position")
                    .withFunction(ao -> ao[1])
                    .build();
    }
