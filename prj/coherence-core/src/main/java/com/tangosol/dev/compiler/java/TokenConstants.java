/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler.java;


import java.util.Hashtable;


/**
* Constants used by Java language token.
*
* @version 0.10, 11/21/96
* @version 0.50, 09/09/98
* @author 	Cameron Purdy
*/
public interface TokenConstants
    {
    // ----- Java token ID's --------------------------------------------

    public static final int TOK_NONE          =   0; // pseudo (e.g. EOF)
    public static final int KEY_ABSTRACT      =   1;
    public static final int KEY_AS            =   2; // proprietary extension
    public static final int KEY_BOOLEAN       =   3;
    public static final int KEY_BREAK         =   4;
    public static final int KEY_BYTE          =   5;
    public static final int KEY_CASE          =   6;
    public static final int KEY_CATCH         =   7;
    public static final int KEY_CHAR          =   8;
    public static final int KEY_CLASS         =   9;
    public static final int KEY_CONST         =  10;
    public static final int KEY_CONTINUE      =  11;
    public static final int KEY_DEFAULT       =  12;
    public static final int KEY_DO            =  13;
    public static final int KEY_DOUBLE        =  14;
    public static final int KEY_ELSE          =  15;
    public static final int KEY_EXTENDS       =  16;
    public static final int KEY_FINAL         =  17;
    public static final int KEY_FINALLY       =  18;
    public static final int KEY_FLOAT         =  19;
    public static final int KEY_FOR           =  20;
    public static final int KEY_GOTO          =  21;
    public static final int KEY_IF            =  22;
    public static final int KEY_IMPLEMENTS    =  23;
    public static final int KEY_IMPORT        =  24;
    public static final int KEY_INSTANCEOF    =  25;
    public static final int KEY_INT           =  26;
    public static final int KEY_INTERFACE     =  27;
    public static final int KEY_LONG          =  28;
    public static final int KEY_NATIVE        =  29;
    public static final int KEY_NEW           =  30;
    public static final int KEY_PACKAGE       =  31;
    public static final int KEY_PRIVATE       =  32;
    public static final int KEY_PROTECTED     =  33;
    public static final int KEY_PUBLIC        =  34;
    public static final int KEY_RETURN        =  35;
    public static final int KEY_SHORT         =  36;
    public static final int KEY_STATIC        =  37;
    public static final int KEY_STRICTFP      =  38; // 2001.05.22 cp
    public static final int KEY_SUPER         =  39;
    public static final int KEY_SWITCH        =  40;
    public static final int KEY_SYNCHRONIZED  =  41;
    public static final int KEY_THIS          =  42;
    public static final int KEY_THROW         =  43;
    public static final int KEY_THROWS        =  44;
    public static final int KEY_TRANSIENT     =  45;
    public static final int KEY_TRY           =  46;
    public static final int KEY_VOID          =  47;
    public static final int KEY_VOLATILE      =  48;
    public static final int KEY_WHILE         =  49;
    public static final int SEP_LPARENTHESIS  =  50;
    public static final int SEP_RPARENTHESIS  =  51;
    public static final int SEP_LCURLYBRACE   =  52;
    public static final int SEP_RCURLYBRACE   =  53;
    public static final int SEP_LBRACKET      =  54;
    public static final int SEP_RBRACKET      =  55;
    public static final int SEP_SEMICOLON     =  56;
    public static final int SEP_COMMA         =  57;
    public static final int SEP_DOT           =  58;
    public static final int OP_ADD            =  59;
    public static final int OP_SUB            =  60;
    public static final int OP_MUL            =  61;
    public static final int OP_DIV            =  62;
    public static final int OP_REM            =  63;
    public static final int OP_SHL            =  64;
    public static final int OP_SHR            =  65;
    public static final int OP_USHR           =  66;
    public static final int OP_BITAND         =  67;
    public static final int OP_BITOR          =  68;
    public static final int OP_BITXOR         =  69;
    public static final int OP_BITNOT         =  70;
    public static final int OP_ASSIGN         =  71;
    public static final int OP_ASSIGN_ADD     =  72;
    public static final int OP_ASSIGN_SUB     =  73;
    public static final int OP_ASSIGN_MUL     =  74;
    public static final int OP_ASSIGN_DIV     =  75;
    public static final int OP_ASSIGN_REM     =  76;
    public static final int OP_ASSIGN_SHL     =  77;
    public static final int OP_ASSIGN_SHR     =  78;
    public static final int OP_ASSIGN_USHR    =  79;
    public static final int OP_ASSIGN_BITAND  =  80;
    public static final int OP_ASSIGN_BITOR   =  81;
    public static final int OP_ASSIGN_BITXOR  =  82;
    public static final int OP_TEST_EQ        =  83;
    public static final int OP_TEST_NE        =  84;
    public static final int OP_TEST_GT        =  85;
    public static final int OP_TEST_GE        =  86;
    public static final int OP_TEST_LT        =  87;
    public static final int OP_TEST_LE        =  88;
    public static final int OP_LOGICAL_AND    =  89;
    public static final int OP_LOGICAL_OR     =  90;
    public static final int OP_LOGICAL_NOT    =  91;
    public static final int OP_INCREMENT      =  92;
    public static final int OP_DECREMENT      =  93;
    public static final int OP_CONDITIONAL    =  94;
    public static final int OP_COLON          =  95;
    public static final int LIT_NULL          =  96;
    public static final int LIT_TRUE          =  97;
    public static final int LIT_FALSE         =  98;
    public static final int LIT_CHAR          =  99; // pseudo
    public static final int LIT_INT           = 100; // pseudo
    public static final int LIT_LONG          = 101; // pseudo
    public static final int LIT_FLOAT         = 102; // pseudo
    public static final int LIT_DOUBLE        = 103; // pseudo
    public static final int LIT_STRING        = 104; // pseudo
    public static final int IDENT             = 105; // pseudo
    public static final int CMT_SINGLELINE    = 106; // pseudo 2001.06.29 cp
    public static final int CMT_TRADITIONAL   = 107; // pseudo 2001.06.29 cp
    public static final int CMT_DOCUMENTATION = 108; // pseudo 2001.06.29 cp


    // ----- Java token categories ------------------------------------------

    /**
    * identifiers (3.8)
    */
    public static final int  IDENTIFIER = 1;
    /**
    * keywords (3.9)
    */
    public static final int  KEYWORD    = 2;
    /**
    * literals (3.10)
    */
    public static final int  LITERAL    = 3;
    /**
    * separators (3.11)
    */
    public static final int  SEPARATOR  = 4;
    /**
    * operators (3.12)
    */
    public static final int  OPERATOR   = 5;
    /**
    * comments (2001.06.29 cp)
    */
    public static final int  COMMENT    = 6;


    // ----- Java token sub-categories ----------------------------------

    /**
    * Literal:  Integers (Java Language Specification 3.10.1)
    */
    public static final int  INT    = 1;
    /**
    * Literal:  Long integers (3.10.1)
    */
    public static final int  LONG   = 2;
    /**
    * Literal:  Floating point numbers (3.10.2)
    */
    public static final int  FLOAT  = 3;
    /**
    * Literal:  Double-precision floating point numbers (3.10.2)
    */
    public static final int  DOUBLE = 4;
    /**
    * Literal:  Booleans (3.10.3)
    */
    public static final int  BOOL   = 5;
    /**
    * Literal:  Characters (3.10.4)
    */
    public static final int  CHAR   = 6;
    /**
    * Literal:  Strings (3.10.5)
    */
    public static final int  STRING = 7;
    /**
    * Literal:  Null references (3.10.7)
    */
    public static final int  NULL   = 8;

    /**
    * This is a HashTable which contains all Java tokens that are reserved
    * words.  The reserved tokens are the keywords and literals that, were
    * they not reserved, could be used as identifiers.
    *
    * WARNING!  DO NOT MODIFY THE CONTENTS OF THE HASHTABLE!
    */
    public static final Hashtable RESERVED = new Hashtable(150, 1.0F);
    }
