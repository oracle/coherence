/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.util.Resources;


/**
*
*/
public class PackageResources extends Resources implements Constants
    {
    public Object[][] getContents()
        {
        return resources;
        }

    static final Object[][] resources =
        {
        // Java Tokenizer resources
        {ERR_INTERNAL       , "An internal error has occurred in the Java tokenizer."},
        {ERR_UNEXPECTED_EOF , "Unexpected End-Of-File."},
        {ERR_UNICODE_ESCAPE , "Invalid unicode escape sequence."},
        {ERR_INVALID_CHAR   , "Invalid character:  {0}."},
        {ERR_CHAR_ESCAPE    , "Invalid character escape sequence:  {0}."},
        {ERR_S_QUOTE_EXP    , "Single quote expected."},
        {ERR_UNESC_S_QUOTE  , "Character literal is either empty or contains an unescaped single quote."},
        {ERR_NEWLINE_IN_LIT , "Literal contains a new-line character."},
        {ERR_NUMERIC_RANGE  , "Numeric value out-of-range:  {0}."},
        {ERR_NUMERIC_FORMAT , "Invalid numeric format:  {0}."},
        {ERR_UNEXPECTED_IO  , "Unexpected I/O error:  {0}."},

        {LABEL_DUPLICATE , "Duplicate label:  \"{0}\"."},
        {LABEL_MISSING   , "Missing label:  \"{0}\"."},
        {BREAK_TARGET    , "No enclosing switch, while, do, or for statement."},
        {CONTINUE_TARGET , "No enclosing while, do, or for statement."},
        {RETURN_MISSING  , "A return statement expected."},
        {RETURN_ISVOID   , "The return statement must not return a value."},
        {RETURN_NOTVOID  , "The return statement must return a value."},
        {RETURN_TYPE     , "Incompatible type for return.  Cannot assign {0} to {1}."},
        {VAR_DUPLICATE   , "Variable \"{0}\" is already defined in this method."},
        {DFT_DUPLICATE   , "Duplicate default label."},
        {CASE_DUPLICATE  , "Duplicate case label:  \"{0}\"."},
        {CATCH_INVALID   , "Invalid exception parameter declaration."},
        {STMT_NOT_REACHED, "Statement is not reachable."},
        {DECL_NOT_IMMED  , "Local variable declarations must be immediately contained by a block."},
        {DECL_BAD_NAME   , "Illegal variable name."},
        {EXCEPT_UNCAUGHT , "Uncaught or undeclared exception:  \"{0}\"."},
        {TOKEN_EXPECTED  , "Token expected:  \"{0}\"."},
        {TOKEN_UNEXPECTED, "Unexpected token:  \"{0}\"."},
        {TOKEN_PANIC     , "Unexpected token:  \"{0}\"."},
        {TOKEN_UNSUPP    , "Unsupported token in a script context:  \"{0}\"."},
        {TOKEN_ILLEGAL   , "Illegal token:  \"{0}\"."},
        {TOKEN_UNKNOWN   , "Internal error; unkown token:  \"{0}\"."},
        {ELSE_NO_IF      , "\"{0}\" without \"if\"."},
        {CATCH_NO_TRY    , "\"{0}\" without \"try\"."},
        {FINALLY_NO_TRY  , "\"{0}\" without \"try\"."},
        {LABEL_NO_SWITCH , "\"{0}\" without \"switch\"."},
        {INTEGRAL_RANGE  , "The integral value is out of range; the specified value must follow a minus sign."},
        {EXPR_NOT_STMT   , "The expression cannot be used as a statement."},
        {UNEXPECTED_EOF  , "Unexpected End-Of-File; the script may have an extra opening brace (\"{\") or a missing closing brace (\"}\")."},
        {IMPORT_NOT_FOUND, "Import class not found:  \"{0}\"."},
        {NOT_METHOD_NAME , "Illegal method invocation."},
        {NOT_TYPE_NAME   , "Illegal type specifier:  \"{0}\"."},
        {FINAL_REASSIGN  , "Final variable \"{0}\" cannot be reassigned."},
        {FINAL_IN_LOOP   , "Final variable \"{0}\" cannot be assigned within a loop."},
        {VAR_UNASSIGNED  , "Variable \"{0}\" is not definitely assigned."},
        {THIS_ILLEGAL    , "The keyword \"this\" cannot be used in a static method."},
        {SUPER_ILLEGAL   , "The keyword \"super\" cannot be used as a reference type."},
        {NULL_ILLEGAL    , "The null type cannot be used as a reference type."},
        {NOT_CONSTANT    , "The expression is not constant."},
        {NOT_NUMERIC     , "The expression type is not numeric."},
        {NOT_INTEGRAL    , "The expression type is not integer."},
        {ILLEGAL_INTEGRAL, "The integer value is illegal."},
        {ILLEGAL_DIVISOR , "Division by zero is illegal."},
        {NOT_BOOLEAN     , "The expression type is not boolean."},
        {NOT_REFERENCE   , "The expression type is not a reference type."},
        {NOT_CASTABLE    , "The expression is of type \"{0}\" and cannot be cast to \"{1}\"."},
        {NOT_COMPARABLE  , "The expression is of type \"{0}\" and cannot be compared with \"{1}\"."},
        {NOT_ASSIGNABLE  , "The expression is of type \"{0}\" and cannot be automatically converted to \"{1}\"."},
        {NOT_VARIABLE    , "The expression cannot be assigned to."},
        {BAD_INITIALIZER , "The initializer expression cannot be assigned to \"{0}\"."},
        {ARRAY_UNEXPECTED, "Array constants can only be used in initializers."},
        {NOT_ARRAY       , "The expression type is not an array type."},
        {ARRAY_DIMENSIONS, "Array constants cannot be used if array dimensions are specified."},
        {IMPORT_DUPLICATE, "Duplicate import alias:  \"{0}\"."},
        {TYPE_NOT_FOUND  , "Unable to locate type \"{0}\" in package \"{1}\"."},
        {PKG_NOT_FOUND   , "Unable to locate package \"{0}\"."},
        {TYPE_NO_ACCESS  , "The type \"{0}\" is not accessible."},
        {FIELD_NO_ACCESS , "The field \"{0}\" on type \"{1}\" is not accessible."},
        {METHOD_NO_ACCESS, "The method \"{0}\" on type \"{1}\" is not accessible."},
        {VAR_NOT_FOUND   , "Undefined variable:  \"{0}\"."},
        {NOT_VALUE       , "The expression does not result in a value."},
        {FIELD_NOT_FOUND , "No field \"{0}\" exists on type \"{1}\"."},
        {REF_REQUIRED    , "A reference is required to access the instance member \"{0}\"."},
        {NOT_SUPER_METHOD, "The method is not a super method of the current method."},
        {NO_SUPER_METHOD , "The current method, \"{0}\", does not have a super method."},
        {SUPER_MISMATCH  , "Super method parameter mismatch:  {0} passed, {1} expected."},
        {METHOD_NOT_FOUND, "No method \"{0}\" matching these parameters exists on type \"{1}\"."},
        {AMBIGUOUS_METHOD, "Ambiguous call to method \"{0}\" on type \"{1}\"."},
        {ABSTRACT_TYPE   , "Type \"{0}\" cannot be instantiated; it is abstract."},
        {STATIC_TYPE     , "Type \"{0}\" cannot be instantiated; it is static."},
        {NOT_THROWABLE   , "Type \"{0}\" cannot be thrown; it does not derive from Throwable."},
        {NOT_CATCHABLE   , "Type \"{0}\" cannot be caught; it is not thrown by the try block."},
        {UNBALANCED_BRACE, "The script contains an unmatched closing brace; it may have a missing opening brace (\"{\") or an extra closing brace (\"}\")."},
        {NOT_EXPRESSION  , "Expression expected."},
        {KEYWORD_UNEXP   , "Unexpected keyword:  \"{0}\"."},
        {DEFAULT_UNSUPP  , "The \"super\" call is not currently supported for default interface methods"},
        {STATIC_UNSUPP   , "Static interface method calls are not currently supported"},
        };
    }
