/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.internal.json;

import jakarta.json.stream.JsonParser;

abstract class AbstractDeserializer
    {
    protected int getIntValue(JsonParser parser)
        {
        return parser.next() == JsonParser.Event.VALUE_NUMBER
               ? parser.getInt()
               : 0;
        }

    protected boolean getBooleanValue(JsonParser parser)
        {
        return parser.next() == JsonParser.Event.VALUE_TRUE;
        }

    protected String getStringValue(JsonParser parser)
        {
        return parser.next() == JsonParser.Event.VALUE_STRING
               ? parser.getString()
               : null;
        }
    }
