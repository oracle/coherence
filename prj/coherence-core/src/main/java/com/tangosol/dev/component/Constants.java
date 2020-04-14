/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import java.text.Collator;

import java.util.Hashtable;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ErrorList;
import com.tangosol.util.Resources;


/**
* This interface is used to define the constants that are used by the
* classes related to Component Definitions.
*
* @version 1.00, 09/09/97
* @author  Cameron Purdy
*/
public interface Constants
    {
    // ----- miscellaneous --------------------------------------------------

    /**
    * The package resources.
    */
    public static final Resources RESOURCES =
            ClassHelper.getPackageResources("com.tangosol.dev.component.");

    /**
    * The case (etc.) insensitive comparator used to ensure unique parameter
    * names for scripting languages that do not enforce case sensitivity.
    */
    public static final Collator INSENS = Component.getDefaultScriptingCollator();

    /**
    * The character delimiting names in a global identity.
    */
    public static final char GLOBAL_ID_DELIM   = '.';

    /**
    * The character delimiting names in a local identity.
    */
    public static final char LOCAL_ID_DELIM    = '$';

    /**
    * Blank string.
    */
    public static final String BLANK = "";


    // ----- component definition binary ------------------------------------

    /**
    * Component Definition binary (result of save method) is tagged with a
    * magic value to identify it as being a Component Definition binary.
    */
    public static final int MAGIC   = 0x74617073;       // taps

    /**
    * Component Definition binary (result of save method) is tagged with a
    * version identifier to insure forwards and backward compatibility
    * support, or at least predictable functional degredation (e.g. an
    * IOException on construction from stream).
    */
    public static final int VERSION = 0x04020000;       // 4.02
    
    // ----- component definition modes -------------------------------------

    /*
    * There are three modes of a Component Definition:  resolved, derivation,
    * and modification.  When a Component Definition is constructed, it is
    * resolved.  The result of the extractDerivation method is a derivation.
    * The result of the extractModification method is a modification.
    */

    /**
    * Component Mode:  Invalid.  This component has been disposed of and
    * should not be used.
    */
    public static final int INVALID      = 0;

    /**
    * Component Mode:  Resolved.  Derivations may be applied to this
    * component.
    */
    public static final int RESOLVED     = 1;

    /**
    * Component Mode:  Derivation.  This component may be applied to a
    * resolved component.
    */
    public static final int DERIVATION   = 2;
    
    /**
    * Component Mode:  Modification.  This component may be applied to
    * resolved or derived components.
    */
    public static final int MODIFICATION = 3;
    
    // ----- trait processing states ----------------------------------------

    /**
    * The Trait processing state is used to track where in the resolve and
    * extract process this trait came from.  This is used by some of the
    * extracts to perform special processing during certain state transitions.
    */

    /**
    * Processing State:  New.  This is the default value and indicates that
    * this trait was created outside of the resolve/extract process, usually
    * by being added during the edit session.
    */
    public static final int STATE_NEW        = 0;

    /**
    * Processing State:  Resolving.  This trait is in the process of being
    * resolved.  A trait is given this value when it is instantiated from
    * persistent storage and when a derived trait is resolved from a base
    * and delta trait.
    */
    public static final int STATE_RESOLVING  = 1;

    /**
    * Processing State:  Resolved.  This trait has been resolved to finalization.
    */
    public static final int STATE_RESOLVED   = 2;

    /*
    * Processing State:  Extracting.  This trait is in the process of being
    * extracted.  A trait is given this value when a delta trait is extracted
    * from a derived and base trait.
    */
    public static final int STATE_EXTRACTING = 3;
    
    // ----- common trait attributes ----------------------------------------
    
    /*
    * The following boolean and enumerated attributes are used by certain
    * implementations of the Trait class:
    *
    *   Existence           (nonexistence) insert update delete
    *   Accessibility       public protected private
    *   Synchronization     (nomonitor) synchronized
    *   Scope               (instance) static
    *   Implementation      concrete abstract
    *   Derivability        (derivable) final
    *   Antiquity           (current) deprecated
    *   Persistence         persistent transient
    *   Distributable       local remote
    *   Direction           in out inout
    *   Visibility          system autogen hidden visible
    *
    * Not all of the above attributes are applicable to all traits.
    *
    * Enumerated attributes (accessibility, distributability, and
    * direction) are public so that they can be used as parameters
    * and return values for attribute accessor (get/set) methods.
    *
    * The Existence attribute is only used on derivations/modifications.
    * The same applies to the "_SPECIFIED" flags.
    *
    * The default setting for a flag is usually the zero-value.  One-way
    * attributes usually have ascending values corresponding to the one-way
    * pattern of the attribute.
    */

    /**
    * Bit 0:  Is existence specified?  on=yes, off=no
    */
    public static final int EXISTS_SPECIFIED = 0x00000001;
    /**
    * Bit 1 and 2 are the existence bits.
    * <pre>
    *  2   1  Description
    * --- --- -----------
    * off off update (exists here and in base)
    * off on  insert (exists, but not in base)
    * on  off delete (exists in base, but not here)
    * on  on  (nonexistence)
    * </pre>
    */
    public static final int EXISTS_MASK      = 0x00000006;
    /**
    * Existence:  update
    */
    public static final int EXISTS_UPDATE    = 0x00000000;
    /**
    * Existence:  insert
    */
    public static final int EXISTS_INSERT    = 0x00000002;
    /**
    * Existence:  delete
    */
    public static final int EXISTS_DELETE    = 0x00000004;
    /**
    * Existence:  nonexistence
    */
    public static final int EXISTS_NOT       = 0x00000006;
    /**
    * Exists:  Full mask
    */
    public static final int EXISTS_FULLMASK  = EXISTS_SPECIFIED | EXISTS_MASK;

    /**
    * Bit 3:  Is accessibility specified?  on=yes, off=no
    */
    public static final int ACCESS_SPECIFIED = 0x00000008;
    /**
    * Bit 4 and 5 are the accessibility bits.
    * <pre>
    *  5   4  Description
    * --- --- -----------
    * off off private
    * off on  (reserved, potentially package private)
    * on  off protected
    * on  on  public
    * </pre>
    */
    public static final int ACCESS_MASK      = 0x00000030;
    /**
    * Accessibility:  Private
    */
    public static final int ACCESS_PRIVATE   = 0x00000000;
    /**
    * Accessibility:  Package (JCS only)
    */
    public static final int ACCESS_PACKAGE   = 0x00000010;
    /**
    * Accessibility:  Protected
    */
    public static final int ACCESS_PROTECTED = 0x00000020;
    /**
    * Accessibility:  Public
    */
    public static final int ACCESS_PUBLIC    = 0x00000030;
    /**
    * Accessibility:  Full mask
    */
    public static final int ACCESS_FULLMASK  = ACCESS_SPECIFIED | ACCESS_MASK;

    /**
    * Bit 6:  Is synchronization specified?  on=yes, off=no
    */
    public static final int SYNC_SPECIFIED   = 0x00000040;
    /**
    * Bit 7 is the Synchronization bit.
    */
    public static final int SYNC_MASK        = 0x00000080;
    /**
    * Bit 7:  Synchronization:  off=nomonitor
    */
    public static final int SYNC_NOMONITOR   = 0x00000000;
    /**
    * Bit 7:  Synchronization:  on=monitor (synchronized)
    */
    public static final int SYNC_MONITOR     = 0x00000080;
    /**
    * Synchronization:  Full mask
    */
    public static final int SYNC_FULLMASK    = SYNC_SPECIFIED | SYNC_MASK;

    /**
    * Bit 8:  Is scope specified?  on=yes, off=no
    */
    public static final int SCOPE_SPECIFIED  = 0x00000100;
    /**
    * Bit 9 is the Scope bit.
    */
    public static final int SCOPE_MASK       = 0x00000200;
    /**
    * Bit 9:  Scope:  off=instance
    */
    public static final int SCOPE_INSTANCE   = 0x00000000;
    /**
    * Bit 9:  Scope:  on=static
    */
    public static final int SCOPE_STATIC     = 0x00000200;
    /**
    * Scope:  Full mask
    */
    public static final int SCOPE_FULLMASK   = SCOPE_SPECIFIED | SCOPE_MASK;

    /**
    * Bit 10:  Is implementation specified?  on=yes, off=no
    */
    public static final int IMPL_SPECIFIED   = 0x00000400;
    /**
    * Bit 11 is the Implementation bit.
    */
    public static final int IMPL_MASK        = 0x00000800;
    /**
    * Bit 11:  Implementation:  off=concrete
    */
    public static final int IMPL_CONCRETE    = 0x00000000;
    /**
    * Bit 11:  Implementation:  on=abstract
    */
    public static final int IMPL_ABSTRACT    = 0x00000800;
    /**
    * Implementation:  Full mask
    */
    public static final int IMPL_FULLMASK    = IMPL_SPECIFIED | IMPL_MASK;

    /**
    * Bit 12:  Is derivability specified?  on=yes, off=no
    */
    public static final int DERIVE_SPECIFIED = 0x00001000;
    /**
    * Bit 13 is the Derivability bit.
    */
    public static final int DERIVE_MASK      = 0x00002000;
    /**
    * Bit 13:  Derivability: off=derivable
    */
    public static final int DERIVE_DERIVABLE = 0x00000000;
    /**
    * Bit 13:  Derivability: on=final
    */
    public static final int DERIVE_FINAL     = 0x00002000;
    /**
    * Derivability:  Full mask
    */
    public static final int DERIVE_FULLMASK  = DERIVE_SPECIFIED | DERIVE_MASK;

    /**
    * Bit 14:  Is antiquity specified?  on=yes, off=no
    */
    public static final int ANTIQ_SPECIFIED  = 0x00004000;
    /**
    * Bit 15 is the antiquity bit.
    */
    public static final int ANTIQ_MASK       = 0x00008000;
    /**
    * Bit 15:  Antiquity: off=current
    */
    public static final int ANTIQ_CURRENT    = 0x00000000;
    /**
    * Bit 15:  Antiquity: on=deprecated
    */
    public static final int ANTIQ_DEPRECATED = 0x00008000;
    /**
    * Antiquity:  Full mask
    */
    public static final int ANTIQ_FULLMASK   = ANTIQ_SPECIFIED | ANTIQ_MASK;

    /**
    * Bit 16:  Is persistent storage specified?  on=yes, off=no
    */
    public static final int STG_SPECIFIED    = 0x00010000;
    /**
    * Bit 17 is the Storage bit.
    */
    public static final int STG_MASK         = 0x00020000;
    /**
    * Bit 17:  Storage:  off=transient
    */
    public static final int STG_TRANSIENT    = 0x00000000;
    /**
    * Bit 17:  Storage:  on=persistent
    */
    public static final int STG_PERSIST      = 0x00020000;
    /**
    * Storage:  Full mask
    */
    public static final int STG_FULLMASK     = STG_SPECIFIED | STG_MASK;

    /**
    * Bit 18:  Is distribution specified?  on=yes, off=no
    */
    public static final int DIST_SPECIFIED   = 0x00040000;
    /**
    * Bit 19 and 20 are the distribution bits.
    * <pre>
    * 20  19  Description
    * --- --- -----------
    * off off (reserved)
    * off on  local
    * on  off (reserved, potentially remote-only)
    * on  on  remote
    * </pre>
    */
    public static final int DIST_MASK        = 0x00180000;
    /**
    * Distribution:  Local
    */
    public static final int DIST_LOCAL       = 0x00080000;
    /**
    * Distribution:  Remote
    */
    public static final int DIST_REMOTE      = 0x00180000;
    /**
    * Distribution:  Full mask
    */
    public static final int DIST_FULLMASK    = DIST_SPECIFIED | DIST_MASK;

    /**
    * Bit 21:  Is direction specified?  on=yes, off=no
    */
    public static final int DIR_SPECIFIED    = 0x00200000;
    /**
    * Bit 22 and 23 are the direction bits.
    * <pre>
    * 23  22  Description
    * --- --- -----------
    * off off (reserved)
    * off on  in
    * on  off out
    * on  on  inout
    * </pre>
    */
    public static final int DIR_MASK         = 0x00C00000;
    /**
    * Direction:  In
    */
    public static final int DIR_IN           = 0x00400000;
    /**
    * Direction:  Out
    */
    public static final int DIR_OUT          = 0x00800000;
    /**
    * Direction:  In/Out
    */
    public static final int DIR_INOUT        = 0x00C00000;
    /**
    * Direction:  Full mask
    */
    public static final int DIR_FULLMASK     = DIR_SPECIFIED | DIR_MASK;

    /**
    * Bit 24:  Is visibility specified?  on=yes, off=no
    */
    public static final int VIS_SPECIFIED    = 0x01000000;
    /**
    * Bit 25 and 26 are the visibility bits.
    * <pre>
    * 26  25  Description
    * --- --- -----------
    * off off visible
    * off on  hidden
    * on  off autogen
    * on  on  system
    * </pre>
    */
    public static final int VIS_MASK         = 0x06000000;
    /**
    * Visibility:  visible
    */
    public static final int VIS_VISIBLE      = 0x00000000;
    /**
    * Visibility:  advanced
    */
    public static final int VIS_ADVANCED     = 0x02000000;
    /**
    * Visibility:  hidden
    */
    public static final int VIS_HIDDEN       = 0x04000000;
    /**
    * Visibility:  system
    */
    public static final int VIS_SYSTEM       = 0x06000000;
    /**
    * Visibility:  Full mask
    */
    public static final int VIS_FULLMASK     = VIS_SPECIFIED | VIS_MASK;

    /**
    * Bit 27:  Is indexed specified?  on=yes, off=no
    */
    public static final int PROP_SPECIFIED   = 0x08000000;
    /**
    * Bit 28 and 29 are the indexed bits.
    * <pre>
    * 29  28  Description
    * --- --- -----------
    * off off (reserved)
    * off on  single only
    * on  off single and indexed
    * on  on  indexed only
    * </pre>
    */
    public static final int PROP_MASK        = 0x30000000;
    /**
    * Property:  Standard (not indexed) accessors
    */
    public static final int PROP_SINGLE      = 0x10000000;
    /**
    * Property:  Both indexed and standard accessors
    */
    public static final int PROP_INDEXED     = 0x30000000;
    /**
    * Property:  Only indexed accessors are available
    */
    public static final int PROP_INDEXEDONLY = 0x20000000;
    /**
    * Property:  Full mask
    */
    public static final int PROP_FULLMASK    = PROP_SPECIFIED | PROP_MASK;

    /**
    * Misc:  is Interface?  (JCS-only)
    */
    public static final int MISC_ISINTERFACE = 0x40000000;
    /**
    * Misc:  is Throwable?  (JCS-only)
    */
    public static final int MISC_ISTHROWABLE = 0x80000000;

    /**
    * All "specified" bits.
    */
    public static final int ALL_SPECIFIED    = EXISTS_SPECIFIED |
                                               ACCESS_SPECIFIED |
                                               SYNC_SPECIFIED   |
                                               SCOPE_SPECIFIED  |
                                               IMPL_SPECIFIED   |
                                               DERIVE_SPECIFIED |
                                               ANTIQ_SPECIFIED  |
                                               STG_SPECIFIED    |
                                               DIST_SPECIFIED   |
                                               DIR_SPECIFIED    |
                                               VIS_SPECIFIED    |
                                               PROP_SPECIFIED;


    // ----- error codes:  attribute accessor methods -----------------------

    // Prefixes:
    // ATTR    - attribute accessor errors
    // RES     - application of derivation/modification
    // EXT     - extraction of derivation/modification
    // IFACE   - application of interfaces, including integration maps

    // Code Blocks:
    // 0?? - various traits or Trait itself
    // 1?? - Component
    // 2?? - Behavior
    // 3?? - Parameter
    // 4?? - ReturnValue
    // 5?? - Exception
    // 6?? - Implementation
    // 7?? - Property
    // 8?? - Interface

    /**
    * The "{0}" attribute is not modifiable.
    */
    public static final String ATTR_READONLY                = "ATTR-001";

    /**
    * An attempt was made to set the "{0}" attribute to an illegal value.
    */
    public static final String ATTR_ILLEGAL                 = "ATTR-002";

    /**
    * The "{0}" sub-trait is not addable.
    */
    public static final String ATTR_NO_ADD                  = "ATTR-003";

    /**
    * The "{0}" sub-trait is not removable.
    */
    public static final String ATTR_NO_REMOVE               = "ATTR-004";

    /**
    * The "{0}" sub-trait is not un-removable.
    */
    public static final String ATTR_NO_UNREMOVE             = "ATTR-005";


    // RESOLVE - resolve derivation or modification errors

    /**
    * Trait UID's did not match (and base trait UID was not null).
    */
    public static final String RESOLVE_UIDCHANGE            = "RES-001";

    /**
    * Trait was forced to resolve.
    */
    public static final String RESOLVE_FORCERESOLVE         = "RES-002";

    /**
    * Trait modes required extraction before resolve.
    */
    public static final String RESOLVE_FORCEEXTRACT         = "RES-003";

    /**
    * Name conflict forced discard of property.
    */
    public static final String RESOLVE_PROPERTYDISCARDED    = "RES-101";

    /**
    * Name conflict forced discard of behavior.
    */
    public static final String RESOLVE_BEHAVIORDISCARDED    = "RES-102";

    /**
    * Name conflict forced discard of child.
    */
    public static final String RESOLVE_CHILDDISCARDED       = "RES-103";

    /**
    * Property discarded because it was orphaned.
    */
    public static final String RESOLVE_PROPERTYORPHANED     = "RES-104";

    /**
    * Behavior discarded because it was orphaned.
    */
    public static final String RESOLVE_BEHAVIORORPHANED     = "RES-105";

    /**
    * Child discarded because it was orphaned.
    */
    public static final String RESOLVE_CHILDORPHANED        = "RES-106";

    /**
    * Child attempts to derive from a final component.
    */
    public static final String RESOLVE_GLOBALFINAL          = "RES-107";

    /**
    * Child doesn't fit an aggregation category.
    */
    public static final String RESOLVE_NOCATEGORY           = "RES-108";

    /**
    * Behavior discarded because it is reserved.
    */
    public static final String RESOLVE_BEHAVIORRESERVED     = "RES-109";

    /**
    * Property discarded because it is reserved.
    */
    public static final String RESOLVE_PROPERTYRESERVED     = "RES-110";

    /**
    * Delta Behavior discarded because base was final.
    */
    public static final String RESOLVE_BEHAVIORFINAL        = "RES-111";

    /**
    * Delta Property discarded because base was static.
    */
    public static final String RESOLVE_PROPERTYSTATIC       = "RES-112";

    /**
    * Delta Component discarded because base was final.
    */
    public static final String RESOLVE_LOCALFINAL           = "RES-113";

    /**
    * Failed to load a required integration map.
    */
    public static final String RESOLVE_LOADINTEGRATES       = "RES-114";

    /**
    * Failed to load a required interface.
    */
    public static final String RESOLVE_LOADINTERFACE        = "RES-115";

    /**
    * Failed to expand interface.
    */
    public static final String RESOLVE_EXPANDINTERFACE      = "RES-116";

    /**
    * Behavior name changed.
    */
    public static final String RESOLVE_BEHAVIORNAMECHANGE   = "RES-201";

    /**
    * Behavior parameter discarded.
    */
    public static final String RESOLVE_PARAMETERDISCARDED   = "RES-202";

    /**
    * Behavior exception discarded.
    */
    public static final String RESOLVE_EXCEPTIONDISCARDED   = "RES-203";

    /**
    * Parameter data type changed.
    */
    public static final String RESOLVE_PARAMTYPECHANGE      = "RES-301";

    /**
    * Parameter name changed.
    */
    public static final String RESOLVE_PARAMNAMECHANGE      = "RES-302";

    /**
    * Parameter direction changed.
    */
    public static final String RESOLVE_PARAMDIRCHANGE       = "RES-303";

    /**
    * Return data type changed.
    */
    public static final String RESOLVE_RETTYPECHANGE        = "RES-401";

    /**
    * Exception data type changed.
    */
    public static final String RESOLVE_EXCEPTTYPECHANGE     = "RES-501";

    /**
    * Property name changed.
    */
    public static final String RESOLVE_PROPNAMECHANGE       = "RES-701";

    /**
    * Property data type changed.
    */
    public static final String RESOLVE_PROPTYPECHANGE       = "RES-702";

    /**
    * Property complex value discarded because it was orphaned.
    */
    public static final String RESOLVE_PROPVALUEORPHANED    = "RES-703";

    // EXTRACT - extract derivation or modification errors

    /**
    * Trait UID's did not match (and base trait UID was not null).
    */
    public static final String EXTRACT_UIDCHANGE            = "EXT-001";

    /**
    * Name conflict forced discard of property.
    */
    public static final String EXTRACT_PROPERTYDISCARDED    = "EXT-101";

    /**
    * Name conflict forced discard of behavior.
    */
    public static final String EXTRACT_BEHAVIORDISCARDED    = "EXT-102";

    /**
    * Name conflict forced discard of child.
    */
    public static final String EXTRACT_CHILDDISCARDED       = "EXT-103";

    /**
    * Property discarded because it was orphaned.
    */
    public static final String EXTRACT_PROPERTYORPHANED     = "EXT-104";

    /**
    * Behavior discarded because it was orphaned.
    */
    public static final String EXTRACT_BEHAVIORORPHANED     = "EXT-105";

    /**
    * Child discarded because it was orphaned.
    */
    public static final String EXTRACT_CHILDORPHANED        = "EXT-106";

    /**
    * Child attempts to derive from a final component.
    */
    public static final String EXTRACT_GLOBALFINAL          = "EXT-107";

    /**
    * Child doesn't fit an aggregation category.
    */
    public static final String EXTRACT_NOCATEGORY           = "EXT-108";

    /**
    * Behavior name changed.
    */
    public static final String EXTRACT_BEHAVIORNAMECHANGE   = "EXT-201";

    /**
    * Behavior parameter discarded.
    */
    public static final String EXTRACT_PARAMETERDISCARDED   = "EXT-202";

    /**
    * Behavior exception discarded.
    */
    public static final String EXTRACT_EXCEPTIONDISCARDED   = "EXT-203";

    /**
    * Parameter data type changed.
    */
    public static final String EXTRACT_PARAMTYPECHANGE      = "EXT-301";

    /**
    * Parameter name changed.
    */
    public static final String EXTRACT_PARAMNAMECHANGE      = "EXT-302";

    /**
    * Parameter direction changed.
    */
    public static final String EXTRACT_PARAMDIRCHANGE       = "EXT-303";

    /**
    * Return data type changed.
    */
    public static final String EXTRACT_RETTYPECHANGE        = "EXT-401";

    /**
    * Exception data type changed.
    */
    public static final String EXTRACT_EXCEPTTYPECHANGE     = "EXT-501";

    /**
    * Property name changed.
    */
    public static final String EXTRACT_PROPNAMECHANGE       = "EXT-701";

    /**
    * Property data type changed.
    */
    public static final String EXTRACT_PROPTYPECHANGE       = "EXT-702";

    /**
    * Property complex value discarded because it was orphaned.
    */
    public static final String EXTRACT_PROPVALUEORPHANED    = "EXT-703";


    // IFACE

    /**
    * Invalid parameters to expand integration map.
    */
    public static final String MAP_ILLEGALINTEGRATES        = "IFACE-001";

    /**
    * Map specifies a method which is not present in the JCS interface.
    */
    public static final String MAP_MISSINGMETHOD            = "IFACE-002";

    /**
    * Integrated method not accessible or is otherwise invalid.
    */
    public static final String MAP_INVALIDMETHOD            = "IFACE-003";

    /**
    * The behavior signature is reserved.
    */
    public static final String MAP_RESERVEDBEHAVIOR         = "IFACE-004";

    /**
    * Scope of behavior does not match integration method.
    */
    public static final String MAP_SCOPEMISMATCH            = "IFACE-005";

    /**
    * Return value of behavior does not match integration method.
    */
    public static final String MAP_RETURNMISMATCH           = "IFACE-006";

    /**
    * Parameter directions of behavior does not match integration method.
    */
    public static final String MAP_DIRECTIONMISMATCH        = "IFACE-007";

    /**
    * Map specifies a field which is not present in the JCS interface.
    */
    public static final String MAP_MISSINGFIELD             = "IFACE-008";

    /**
    * Integrated field not accessible or is otherwise invalid.
    */
    public static final String MAP_INVALIDFIELD             = "IFACE-009";

    /**
    * The integrated property is not createable.
    */
    public static final String MAP_PROPNOTCREATABLE         = "IFACE-010";

    /**
    * An attribute does not match between the property and the integrated
    * field.
    */
    public static final String MAP_PROPFIELDMISMATCH        = "IFACE-011";

    /**
    * The integrated property has a value.
    */
    public static final String MAP_PROPVALUE                = "IFACE-012";

    /**
    * The interface contains a reserved behavior.
    */
    public static final String IFACE_RESERVEDBEHAVIOR       = "IFACE-101";

    /**
    * An interface behavior is static.
    */
    public static final String IFACE_SCOPEMISMATCH          = "IFACE-102";

    /**
    * An interface behavior return type does not match the interface method.
    */
    public static final String IFACE_RETURNMISMATCH         = "IFACE-103";

    /**
    * An interface behavior's parameter directions does not match the
    * parameter directions on the interface method.
    */
    public static final String IFACE_DIRECTIONMISMATCH      = "IFACE-104";

    /**
    * An interface behavior is not public.
    */
    public static final String IFACE_BEHAVIORACCESS         = "IFACE-105";


    // ----- error information ----------------------------------------------

    public static final int NONE    = ErrorList.Constants.NONE;
    public static final int INFO    = ErrorList.Constants.INFO;
    public static final int WARNING = ErrorList.Constants.WARNING;
    public static final int ERROR   = ErrorList.Constants.ERROR;
    public static final int FATAL   = ErrorList.Constants.FATAL;

    // ----- trait events ---------------------------------------------------

    /**
    * The sub-trait was modified.
    */
    public static final int SUB_CHANGE   = 0;

    /**
    * The sub-trait is being added.
    */
    public static final int SUB_ADD      = 1;

    /**
    * The sub-trait is being removed.
    */
    public static final int SUB_REMOVE   = 2;

    /**
    * The sub-trait is being un-removed.
    */
    public static final int SUB_UNREMOVE = 3;

    /**
    * The change has not occurred and can be vetoed.
    */
    public static final int CTX_VETO    = 0;

    /**
    * The change has been vetoed (this is the undo).
    */
    public static final int CTX_UNDO    = 1;

    /**
    * The change has not occurred and can be vetoed.
    */
    public static final int CTX_DONE    = 2;


    // ----- text -----------------------------------------------------------
    public static final String DIR_IN_TEXT    = "in";
    public static final String DIR_OUT_TEXT   = "out";
    public static final String DIR_INOUT_TEXT = "inout";
    }
