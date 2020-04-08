/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler.java;


import com.tangosol.util.ClassHelper;
import com.tangosol.util.ErrorList;
import com.tangosol.util.Resources;


/**
* Java compiler constants.
*
* @version 1.00, 12/02/98
* @author  Cameron Purdy
*/
public interface Constants
        extends ErrorList.Constants
    {
    /**
    * The package resources.
    */
    public static final Resources RESOURCES =
            ClassHelper.getPackageResources("com.tangosol.dev.compiler.java.");


    // ----- tokenizer options ----------------------------------------------

    public static final String OPT_PARSE_COMMENTS   = "java.tokenizer.comments";
    public static final String OPT_PARSE_EXTENSIONS = "java.tokenizer.extensions";


    // ----- error codes ----------------------------------------------------

    public static final String ERR_INTERNAL       = "JT-001";
    public static final String ERR_UNEXPECTED_EOF = "JT-002";
    public static final String ERR_UNICODE_ESCAPE = "JT-003";
    public static final String ERR_INVALID_CHAR   = "JT-004";
    public static final String ERR_CHAR_ESCAPE    = "JT-005";
    public static final String ERR_S_QUOTE_EXP    = "JT-006";
    public static final String ERR_UNESC_S_QUOTE  = "JT-007";
    public static final String ERR_NEWLINE_IN_LIT = "JT-008";
    public static final String ERR_NUMERIC_RANGE  = "JT-009";
    public static final String ERR_NUMERIC_FORMAT = "JT-010";
    public static final String ERR_UNEXPECTED_IO  = "JT-011";

    public static final String LABEL_DUPLICATE  = "JC-001";
    public static final String LABEL_MISSING    = "JC-002";
    public static final String BREAK_TARGET     = "JC-003";
    public static final String CONTINUE_TARGET  = "JC-004";
    public static final String RETURN_MISSING   = "JC-005";
    public static final String RETURN_ISVOID    = "JC-006";
    public static final String RETURN_NOTVOID   = "JC-007";
    public static final String RETURN_TYPE      = "JC-008";
    public static final String VAR_DUPLICATE    = "JC-009";
    public static final String DFT_DUPLICATE    = "JC-010";
    public static final String CASE_DUPLICATE   = "JC-011";
    public static final String CATCH_INVALID    = "JC-012";
    public static final String STMT_NOT_REACHED = "JC-013";
    public static final String DECL_NOT_IMMED   = "JC-014";
    public static final String DECL_BAD_NAME    = "JC-015";
    public static final String EXCEPT_UNCAUGHT  = "JC-016";
    public static final String TOKEN_EXPECTED   = "JC-017";
    public static final String TOKEN_UNEXPECTED = "JC-018";
    public static final String TOKEN_PANIC      = "JC-019";
    public static final String TOKEN_UNSUPP     = "JC-020";
    public static final String TOKEN_ILLEGAL    = "JC-021";
    public static final String TOKEN_UNKNOWN    = "JC-022";
    public static final String ELSE_NO_IF       = "JC-023";
    public static final String CATCH_NO_TRY     = "JC-024";
    public static final String FINALLY_NO_TRY   = "JC-025";
    public static final String LABEL_NO_SWITCH  = "JC-026";
    public static final String INTEGRAL_RANGE   = "JC-027";
    public static final String EXPR_NOT_STMT    = "JC-028";
    public static final String UNEXPECTED_EOF   = "JC-029";
    public static final String IMPORT_NOT_FOUND = "JC-030";
    public static final String NOT_METHOD_NAME  = "JC-031";
    public static final String NOT_TYPE_NAME    = "JC-032";
    public static final String FINAL_REASSIGN   = "JC-033";
    public static final String FINAL_IN_LOOP    = "JC-034";
    public static final String VAR_UNASSIGNED   = "JC-035";
    public static final String THIS_ILLEGAL     = "JC-036";
    public static final String SUPER_ILLEGAL    = "JC-037";
    public static final String NULL_ILLEGAL     = "JC-038";
    public static final String NOT_CONSTANT     = "JC-039";
    public static final String NOT_NUMERIC      = "JC-040";
    public static final String NOT_INTEGRAL     = "JC-041";
    public static final String ILLEGAL_INTEGRAL = "JC-042";
    public static final String ILLEGAL_DIVISOR  = "JC-043";
    public static final String NOT_BOOLEAN      = "JC-044";
    public static final String NOT_REFERENCE    = "JC-045";
    public static final String NOT_CASTABLE     = "JC-046";
    public static final String NOT_COMPARABLE   = "JC-047";
    public static final String NOT_ASSIGNABLE   = "JC-048";
    public static final String NOT_VARIABLE     = "JC-049";
    public static final String BAD_INITIALIZER  = "JC-050";
    public static final String ARRAY_UNEXPECTED = "JC-051";
    public static final String NOT_ARRAY        = "JC-052";
    public static final String ARRAY_DIMENSIONS = "JC-053";
    public static final String IMPORT_DUPLICATE = "JC-054";
    public static final String TYPE_NOT_FOUND   = "JC-055";
    public static final String PKG_NOT_FOUND    = "JC-056";
    public static final String TYPE_NO_ACCESS   = "JC-057";
    public static final String FIELD_NO_ACCESS  = "JC-058";
    public static final String METHOD_NO_ACCESS = "JC-059";
    public static final String VAR_NOT_FOUND    = "JC-060";
    public static final String NOT_VALUE        = "JC-061";
    public static final String FIELD_NOT_FOUND  = "JC-062";
    public static final String REF_REQUIRED     = "JC-063";
    public static final String NOT_SUPER_METHOD = "JC-064";
    public static final String NO_SUPER_METHOD  = "JC-065";
    public static final String SUPER_MISMATCH   = "JC-066";
    public static final String METHOD_NOT_FOUND = "JC-067";
    public static final String AMBIGUOUS_METHOD = "JC-068";
    public static final String ABSTRACT_TYPE    = "JC-069";
    public static final String STATIC_TYPE      = "JC-070";
    public static final String NOT_THROWABLE    = "JC-071";
    public static final String NOT_CATCHABLE    = "JC-072";
    public static final String UNBALANCED_BRACE = "JC-073";
    public static final String NOT_EXPRESSION   = "JC-074";
    public static final String KEYWORD_UNEXP    = "JC-075";
    public static final String DEFAULT_UNSUPP   = "JC-076";
    public static final String STATIC_UNSUPP    = "JC-077";
    }
