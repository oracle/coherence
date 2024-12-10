/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.pof;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.bind.annotation.adapters.XmlAdapter;


/**
 * Adapts a java.util.Date type for custom marshaling.
 *
 * @author ic  2011.07.15
 */
public class DateXmlAdapter
        extends XmlAdapter<String, Date>
    {
    public Date unmarshal(String v)
        throws Exception
        {
        return DATE_FORMAT.parse(v);
        }

    public String marshal(Date v)
        throws Exception
        {
        return DATE_FORMAT.format(v);
        }

    // ---- constants -------------------------------------------------------

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    }
