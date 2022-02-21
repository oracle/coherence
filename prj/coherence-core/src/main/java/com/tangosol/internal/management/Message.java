/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes a message to be sent along with the response.
 *
 * @author sr  2017.08.21
 * @since 12.2.1.4.0
 */
public class Message
    {
    // ----- constructors -----------------------------------------------------------------

    /**
     * Construct an instance of Message.
     *
     * @param severity  the severity of the message
     * @param sMessage  the message
     */
    public Message(Severity severity, String sMessage)
        {
        f_sSeverity = severity;
        f_sMessage  = sMessage;
        f_sField    = null;
        }

    /**
     * Construct an instance of Message.
     *
     * @param severity  the severity of the message
     * @param sField    the field related to the message
     * @param sMessage  the message
     */
    public Message(Severity severity, String sField, String sMessage)
        {
        f_sSeverity = severity;
        f_sField    = sField;
        f_sMessage  = sMessage;
        }


    /**
     * Convert the message to a json object.
     *
     * @return  the JSON object
     */
    public Map<String, Object> toJson()
        {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(PROP_MESSAGE_MESSAGE, f_sMessage);

        if (f_sSeverity != null)
            {
            map.put(PROP_MESSAGE_SEVERITY, f_sSeverity);
            }

        String sField = f_sField;
        if (sField != null && !sField.equals(""))
            {
            map.put(PROP_MESSAGE_FIELD, sField);
            }
        return map;
        }

    // ----- enum -------------------------------------------------------------

    /**
     * Severity of the message, only failure is used right now.
     */
    public enum Severity
        {
        FAILURE, ERROR
        }

    // ----- data members ------------------------------------------------------

    /**
     * The severity of the message.
     */
    protected final Severity f_sSeverity;

    /**
     * The fields for which the message corresponds to.
     * Used in cases when a put call is made to a field.
     */
    protected final String f_sField;

    /**
     * The message itself.
     */
    protected final String f_sMessage;

    // ----- constants ---------------------------------------------------------

    /**
     * The key used for populating severity in the response.
     */
    public static final String PROP_MESSAGE_SEVERITY = "severity";

    /**
     * The key used for populating field in the response.
     */
    public static final String PROP_MESSAGE_FIELD    = "field";

    /**
     * The key used for populating message in the response.
     */
    public static final String PROP_MESSAGE_MESSAGE  = "message";
    }
