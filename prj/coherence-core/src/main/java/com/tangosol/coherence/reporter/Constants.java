/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;


/**
* Constants used to parse the Report Configuraiton and the Report Batch XML files.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public interface Constants
    {
    /**
    * The Default value for the report if an non-critical error occurs.
    */
    static final Object DEFAULT_VALUE = null;

    /**
     * The domain partition key attribute.
     */
    public static final String DOMAIN_PARTITION = "domainPartition";

    /**
    * The calculation is not valid.
    */
    public static final int CALC_ERR = -1;

    /**
    * The values in the attribute will be summed.
    */
    public static final int CALC_SUM = 0;

    /**
    * The values in the attribute will averaged.
    */
    public static final int CALC_AVG = 1;

    /**
    * The maximum value for the attibutes will be returned.
    */
    public static final int CALC_MAX = 2;

    /**
    * The minimum value for the attibutes will be returned.
    */
    public static final int CALC_MIN = 3;

    /**
    * The column type is invalid.
    */
    public static final int COL_ERR = -1;

    /**
    * The column type is a JMX Attribute.
    */
    public static final int COL_ATTRIB = 0;

    /**
    * The column type is a calculation of a JMX Attribute.
    */
    public static final int COL_CALC = 1;

    /**
    * The column type is a JMX method.
    */
    public static final int COL_METHOD = 2;

    /**
    * The column type is a JMX key part.
    */
    public static final int COL_KEY = 3;

    /**
    * The column type is a Global report value.
    */
    public static final int COL_GLOBAL = 4;

    /**
    * The report type is invalid.
    */
    public static final int REPORT_ERR = -1;

        /**
    * The XML tag Header Flag
    */
    public static final String TAG_HEADERS = "hide-headers";

    /**
    * The XML tag for the output file name.
    */
    public static final String TAG_FILENAME = "file-name";

    /**
    * The XML tag for the pattern and filter elements.
    */
    public static final String TAG_QUERY = "query";

    /**
    * The XML tag for the report description.
    */
    public static final String TAG_DESC= "description";

    /**
    * The XML tag for the JMX Query.
    */
    public static final String TAG_PATTERN = "pattern";

    /**
    * The XML tag for the row information.
    */
    public static final String TAG_ROW = "row";

    /**
    * The XML tag for the Attribute Name.
    */
    public static final String TAG_COLUMNNAME = "name";

    /**
    * The XML value for the correlated column type.
    */
    public static final String VALUE_CORRELATED = "correlated";

    /**
    * The XML tag for the Attribute Name.
    */
    public static final String TAG_PARAMS = "params";

    /**
    * The XML tag for the column header.
    */
    public static final String TAG_COLUMNHEAD = "header";

    /**
    * The XML tag for the column header.
    */
    public static final String TAG_HIDDEN = "hidden";

    /**
    * The XML tag for the query filter
    */
    public static final String TAG_FILTER = "filter";

    /**
    * The XML tag for the query filter
    */
    public static final String TAG_FILTERS = "filters";

    /**
    * The XML tag for the query filter
    */
    public static final String TAG_FILTERTYPE = "type";

    /**
    * The XML tag for the query filter
    */
    public static final String TAG_FILTERCLS = "filter-class";

    /**
     * The XML tag for the query filter
     */
    public static final String TAG_REPORT = "report";

    /**
     * The XML tag for the query filter
     */
    public static final String TAG_FILTERREF= "filter-ref";

    /**
    * The XML tag for the column type.
    */
    public static final String TAG_COLUMNTYPE = "type";

    /**
     * The XML tag for the column value's type.
     */
    public static final String TAG_DATATYPE = "data-type";

    /**
    * The XML tag for the calculation type.
    */
    public static final String TAG_COLUMNFUNC = "function-name";

    /**
    * The XML tag for the column/value delimiter.
    */
    public static final String TAG_DELIM = "delim";

    /**
    * The XML tag for a custom column class
    */
    public static final String TAG_CLASS = "column-class";

    /**
    * The XML tag for the column definition.
    */
    public static final String TAG_COLUMN = "column";

    /**
    * The XML tag for the column definition.
    */
    public static final String TAG_COLUMNREF = "column-ref";

    /**
    * The XML value for a JMX Attribute column
    */
    public static final String TAG_SUBQUERY = "subquery";

    /**
    * The XML value for a sum calculation.
    */
    public static final String VALUE_SUM = "sum";

    /**
    * The XML value for a Count calculation.
    */
    public static final String VALUE_COUNT = "count";

    /**
    * The XML value for a average calculation.
    */
    public static final String VALUE_AVG = "avg";

    /**
    * The XML value for a minimum calculation.
    */
    public static final String VALUE_MIN = "min";

    /**
    * The XML value for a maximum calculation.
    */
    public static final String VALUE_MAX = "max";

    /**
    * The XML value for a maximum calculation.
    */
    public static final String VALUE_DELTA = "delta";

    /**
    * The XML value for a divide calculation.
    */
    public static final String VALUE_DIVIDE = "divide";

    /**
    * The XML value for a divide calculation.
    */
    public static final String VALUE_ADD = "add";

    /**
    * The XML value for a divide calculation.
    */
    public static final String VALUE_SUB = "subtract";

    /**
    * The XML value for a divide calculation.
    */
    public static final String VALUE_MULTI = "multiply";

    /**
    * The XML value for a JMX Attribute column
    */
    public static final String VALUE_ATTRIB = "attribute";

    /**
    * The XML value for a JMX Attribute column
    */
    public static final String VALUE_SUBQUERY = "subquery";

    /**
    * The XML value for a JMX method column.
    */
    public static final String VALUE_METHOD = "method";

    /**
    * The XML value for a custom column.
    */
    public static final String VALUE_CUSTOM = "custom";

    /**
    * The XML value for a reporter global value column.
    */
    public static final String VALUE_CONSTANT = "constant";

    /**
    * The XML value for a system property value
    */
    public static final String VALUE_PROPERTY = "property";

    /**
    * The XML value for a reporter global value column.
    */
    public static final String VALUE_GLOBAL = "global";

    /**
    * The XML value for a reporter global value column.
    */
    public static final String VALUE_FUNC = "function";

    /**
    * The XML value for a reporter global value column.
    */
    public static final String VALUE_DISTINCT = "distinct";

    /**
    * The XML value for a reporter global value column.
    */
    public static final String VALUE_OR = "or";

    /**
    * The XML value for a reporter global value column.
    */
    public static final String VALUE_AND = "and";

    /**
    * The XML value for a reporter global value column.
    */
    public static final String VALUE_EQUALS = "equals";

    /**
    * The XML value for a reporter global value column.
    */
    public static final String VALUE_GREATER = "greater";

    /**
    * The XML value for a reporter global value column.
    */
    public static final String VALUE_LESS = "less";

    /**
    * The XML value for a reporter global value column.
    */
    public static final String VALUE_NOT = "not";

    /**
    * The XML value for a JMX key part column.
    */
    public static final String VALUE_KEY = "key";

    /**
    * The Reporter calculation column.
    */
    public static final String VALUE_COLCALC = "function";

    /**
    * The XML value for a the global number of times the report has been executed.
    */
    public static final String VALUE_BATCH = "{batch-counter}";

    /**
    * The XML value for a the global time a report started.
    */
    public static final String VALUE_TIME = "{report-time}";

    /**
    * The default column type.
    */
    public static final String DEFAULT_COLTYPE = "attribute";

    /**
    * The default column calculation type.
    */
    public static final String DEFAULT_CALC = "sum";

    /**
    * The file "macro" start character.
    */
    public static final String MACRO_START = "\\{";

    /**
    * The file "macro" stop character.
    */
    public static final String MACRO_STOP = "\\}";

    /**
    * The XML value for a tab delimited locator.
    */
    public static final String VALUE_TAB = "{tab}";

    /**
    * The XML value for a comma delimited locator.
    */
    public static final String VALUE_SPACE = "{space}";

    /**
    * The file name "macro" to insert the execution batch number into the filename.
    */
    public static final String MACRO_BATCH = "batch";

    /**
    * The file name "macro" to insert the execution node Id into the filename.
    */
    public static final String MACRO_NODE = "node";

    /**
    * The file name "macro" to insert the execution start date into the filename.
    */
    public static final String MACRO_DATE = "date";

    /**
    * The XML value to indicate if this is a Non-Multitenant Environment.
    */
    public static final String MACRO_NONMT = "non-MT";
    }
