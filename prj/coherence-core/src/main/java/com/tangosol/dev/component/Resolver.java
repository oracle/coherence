/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import java.io.IOException;

import java.util.Iterator;

import com.tangosol.dev.assembler.ClassFile;

import com.tangosol.util.Base;
import com.tangosol.util.ErrorList;


/**
* A Component Definition Resolver instantiates and resolves a Component
* Definition based on a name.  The complexities associated with derivation
* are isolated to this class.
*
* (The complexities associated with modification are isolated to the
* repository type handler for Component Definitions.)
*
* A large number of dependencies must be resolved in order to resolve a
* Component Definition.  These dependencies include each Component Definition
* (CD) and Java Class Signature (JCS) which are used to compose the Component
* being resolved.
*
* The trait hierarchy and associated resolve dependencies within a Component
* Definition are as follows:
*
*   Trait/Attribute         Derivation Resolve Dependency
*   ----------------------  -----------------------------
*   Component
*     extends               CD  - super (0/1)
*     implements            JCS - interface (0+)
*     dispatches            JCS - interface (0+)
*     Integration
*     Property
*       Component           CD  - complex property (0/1)
*         ...
*     Behavior
*       ReturnValue
*       Parameter
*       Throwee
*       Implementation
*     Category
*     Component             CD  - child (0+)
*       ...
*
* The process of resolving a Component Definition can be described as:
*   1.  The super for the Component Definition to resolve is loaded
*       and, if it is a derivation, it is resolved (which recursively
*       executes this same process starting with number 1).
*   2.  The Resolver requests the Component Definition to resolve, passing
*       the resolved super and itself (since it implements the Loader
*       interface).
*   3.  The Component Definition resolves as much as possible, which is
*       itself and any of its children that correspond to the children
*       of its super.
*   4.  For children which do not derive from children of its super, the
*       Component Definition requests the supplied loader (i.e. the
*       Resolver) to load the Component Definitions from which the children
*       derive.  The Component Definition then requests the children to
*       resolve (basically recursing to step 2) passing on the Loader
*       interface (i.e. the Resolver).
*   5.  The Component Definition resolves its other dependent traits,
*       such as complex Properties, interfaces, and integration.  (The
*       order of this particular step is not important; it may be performed
*       before step 3.)
*
* Consider the persistent Component Definitions:
*
* Name   Mode          Super  Description                         Stands For
* -----  ------------  -----  ----------------------------------  -----------
* B      Resolved      n/a    The base component                  B=Base
* B.C    Derivation    B      An aggregable component             C=Containable
* B.D    Derivation    B      An aggregating component            D=Derived
* B.D$I  Derivation    B.C    An aggregated component within B.D  I=Inner
*
* The pseudo-operations for the top-down construction of the resolved
* Component Definition "D" are as follows:
*
* 1) The "user" requests the Component Definition by the name of "D":
*      resolver = new Resolver(loader);
*      CD = resolver.loadComponent("D", fReadOnly, errlist)
* 2) The Resolver requests its Loader to instantiate D
* 3) The Loader returns the requested Component Definition to the Resolver;
*    note that D is a derivation-mode Component Definition
* 4) The Resolver determines that B is required to resolve D, and requests
*    itself to load B
* 5) The Resolver requests its Loader to instantiate B
* 6) The Loader returns the requested Component Definition to the Resolver;
*    note that B is a resolved Component Definition
* 7) The Resolver returns B to itself (see step 4)
* 8) The Resolver requests the Component Definition D to resolve itself,
*    passing B as the super and itself (Resolver) as the Loader to allow
*    the Component Definition to load any other dependencies
* 9) D applies itself to B, thus resolving D (but not its children)
* 10) D attempts to resolve its children using the children of B, but there
*     are no matches (in this case, B has no children at all)
* 11) D iterates through the remaining unresolved children and, for each
*     that derives from a global Component Definition, D requests its
*     Loader (i.e. the Resolver) to load the required super Component.
*     In this case, D requests C to be loaded in order to resolve I.
* 12) The Resolver requests its Loader to instantiate C (see step 2)
* 13) The Loader returns the requested Component Definition to the
*     Resolver; note that C is a derivation-mode Component Definition
* 14) The Resolver determines that B is required to resolve C, and requests
*     itself to load B
* 15) B is returned from the Resolver's cache (it was placed there between
*     steps 6 & 7)
* 16) The Resolver requests C to resolve itself (see step 8), passing
*     itself as the Loader
* 17) C resolves itself; it has no child Component Definitions
* 18) The Resolver returns C to D (closing step 11)
* 19) D requests its child I to resolve, passing the resolved C and the
*     Loader (i.e. the Resolver)
* 20) I resolves itself; it has no child Component Definitions; this
*     completes the resolve of D begun in step 8
* 21) The Resolver returns D to the "user"
*
* @version 1.00, 02/04/98
* @author  Cameron Purdy
*/
public class Resolver
        extends    Base
        implements Constants, Loader
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a Resolver given a loader.
    *
    * @param loader  the Loader object to use to load resolved or derived
    *                Component Definitions (plus JCS's); note that
    *                this loader must <B>NOT</B> return modifications
    */
    public Resolver(Loader loader)
        {
        this(loader, null);
        }

    /**
    * Construct a Resolver given a loader and a destination package for
    * relocatable classes.
    *
    * @param loader  the Loader object to use to load resolved or derived
    *                Component Definitions (plus JCS's); note that
    *                this loader must <B>NOT</B> return modifications
    * @param sPkg    the package name to use for relocatable classes
    */
    public Resolver(Loader loader, String sPkg)
        {
        m_loader = loader;

        if (sPkg != null)
            {
            m_sPkg      = sPkg;
            m_relocator = new ClassFile.Relocator(sPkg);
            }
        }


    // ----- Loader interface -----------------------------------------------

    /**
    * Load the specified Component.
    *
    * @param sName      fully qualified Component Definition name
    * @param fReadOnly  true if the loaded component will be read-only
    * @param errlist    the ErrorList object to log any derivation/
    *                   modification errors to
    *
    * @return the specified Component Definition or null
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public Component loadComponent(String sName, boolean fReadOnly, ErrorList errlist)
            throws ComponentException
        {
        // load the component
        Component cd = m_loader.loadComponent(sName, true, errlist);
        if (cd != null)
            {
            // make sure the component is resolved
            switch (cd.getMode())
                {
                case RESOLVED:
                    // gg: 2001.4.25 JCS Interface should be stored as derivation
                    if (cd.isInterface())
                        {
                        throw new ComponentException(CLASS + ".loadComponent:  " + 
                            "Interface " + sName + " should be a derivation");
                        }
                    if (!fReadOnly)
                        {
                        // the only resolved component is "Root"
                        // the only resolved signature is "java.lang.Object"
                        cd.setModifiable(true);
                        }
                    break;
                    
                case DERIVATION:
                    Component cdSuper = cd.isInterface()
                        ? new Component(null, Component.SIGNATURE, BLANK)
                        : loadComponent(cd.getSuperName(), true, errlist);

                    if (cdSuper == null)
                        {
                        throw new ComponentException("Failed to load the super component: \"" +
                            cd.getSuperName() + "\" of component \"" + sName + '"');
                        }

                    if (Trait.DEBUG)
                        {
                        out();
                        out("***Resolver*** Component Super before resolve:");
                        cdSuper.dump();
                        out();
                        out("***Resolver*** Component Delta before resolve:");
                        cd.dump();
                        }

                    cd = cdSuper.resolve(cd, this, errlist);
                    if (Trait.DEBUG)
                        {
                        out();
                        out("***Resolver*** Component Derived after resolve:");
                        cd.dump();
                        }

                    // finish resolve processing
                    cd.finalizeResolve(this, errlist);
                    if (Trait.DEBUG)
                        {
                        out();
                        out("***Resolver*** Component Derived after finalizeResolve:");
                        cd.dump();
                        }

                    if (fReadOnly)
                        {
                        // default is: Modifiable == true
                        cd.setModifiable(false);
                        }
                    
                    break;

                case MODIFICATION:
                case INVALID:
                default:
                    throw new ComponentException(CLASS + ".loadComponent:  " + 
                        "Loader returned invalid mode \"" + sName + "\"(" +
                        (cd.getMode() == MODIFICATION ? "MODIFICATION" : "INVALID")
                        + ")");
                }
            }
        
        return cd;
        }

    /**
    * Load the specified Class Signature by delegating to the Resolver's
    * loader.
    *
    * @param sName    qualified Java Class Signature (JCS) name
    *
    * @return the specified Class Signature
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public Component loadSignature(String sName)
            throws ComponentException
        {
        // load the signature
        Component signature = m_loader.loadSignature(sName);
        if (signature != null)
            {
            // make sure the signature is resolved
            switch (signature.getMode())
                {
                case RESOLVED:
                    // java.lang.Object is the only resolved one
                    break;
                    
                case DERIVATION:
                    String    sSuperName = signature.getSuperName();
                    Component signatureSuper;
                    if (sSuperName.length() == 0)
                        {
                        // this is the case for interface JCSes that extend another interface
                        signatureSuper = new Component(null, Component.SIGNATURE, BLANK);
                        }
                    else
                        {
                        // this is all non-interface classes except java.lang.Object
                        signatureSuper = loadSignature(sSuperName);
                        if (signatureSuper == null)
                            {
                            throw new ComponentException(CLASS + ".loadSignature:  " + 
                                "Failure to load the super " + signature.getSuperName() +
                                " for class " + sName);
                            }
                        }
                    Component signatureDelta = signature;
                    if (Trait.DEBUG)
                        {
                        out();
                        out("***Resolver*** Signature Super before resolve:");
                        signatureSuper.dump();
                        out();
                        out("***Resolver*** Signature Delta before resolve:");
                        signatureDelta.dump();
                        }
                    
                    ErrorList errlist = new ErrorList();
                    signature = signatureSuper.resolve(signatureDelta, this, errlist);
                    if (Trait.DEBUG)
                        {
                        out();
                        out("***Resolver*** Signature Derived after resolve:");
                        signature.dump();
                        }
                    
                    // an error during JCS resolution is "fatal"
                    if (!errlist.isEmpty())
                        {
                        throw new ComponentException(formatErrorList(sName, errlist, false));
                        }
                    
                    // finish resolve processing
                    signature.finalizeResolve(this, errlist);
                    if (Trait.DEBUG)
                        {
                        out();
                        out("***Resolver*** Signature Derived after finalizeResolve:");
                        signature.dump();
                        }
                    
                    // an error during JCS finalize resolution is also "fatal"
                    if (!errlist.isEmpty())
                        {
                        throw new ComponentException(formatErrorList(sName, errlist, true));
                        }
                    
                    break;
                    
                case MODIFICATION:
                case INVALID:
                default:
                    throw new ComponentException(CLASS + ".loadSignature:  " + 
                        "Loader returned invalid mode \"" + sName + "\"(" +
                        (signature.getMode() == MODIFICATION ? "MODIFICATION" : "INVALID")
                        + ")");
                }
            }
        
        return signature;
        }
    
    private String formatErrorList(String sName, ErrorList errlist, boolean fFinalize)
        {
        StringBuffer sb = new StringBuffer();
        if (fFinalize)
            {
            sb.append("Finalize ");
            }
        sb.append("Resolve of Java Class Signature ");
        sb.append(sName);
        sb.append(" contains ");
        sb.append(errlist.size());
        sb.append(" errors:");
        for (Iterator iter = errlist.iterator(); iter.hasNext(); )
            {
            sb.append("\n    ");
            sb.append(iter.next().toString());
            }
        return sb.toString();
        }
    
    /**
    * Load the original (before any customization takes place) Java Class.
    *
    * @param sName  fully qualified Java Class name
    *
    * @return the specified Class structure
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public ClassFile loadOriginalClass(String sName)
            throws ComponentException
        {
        return m_loader.loadOriginalClass(sName);
        }

    /**
    * Load the specified generated Java Class.
    *
    * @param sName  fully qualified Java Class name
    *
    * @return the specified Class structure
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public ClassFile loadClass(String sName)
            throws ComponentException
        {
        ClassFile clz = m_loader.loadClass(sName);

        if (m_sPkg != null)
            {
            // check if the requested class is in the relocatable package
            if (clz == null && sName.startsWith(m_sPkg))
                {
                sName = ClassFile.Relocator.PACKAGE + sName.substring(m_sPkg.length());
                clz   = m_loader.loadClass(sName);
                }

            if (clz != null)
                {
                clz.resolve(m_relocator);
                }
            }

        return clz;
        }

    /**
    * Load the source code for the specified (original) Java class.
    *
    * @param sName  fully qualified Java Class name
    *
    * @return the specified Java source code as a String
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public String loadJava(String sName)
            throws IOException
        {
        String sScript = m_loader.loadJava(sName);

        // check if the requested Java source is in the relocatable package
        if (sScript == null && m_sPkg != null && sName.startsWith(m_sPkg))
            {
            sName   = ClassFile.Relocator.PACKAGE + sName.substring(m_sPkg.length());
            sScript = m_loader.loadJava(sName);
            }

        return sScript;
        }

    /**
    * Load the original (before any customization takes place) resource.
    *
    * @param sName  fully qualified resource name
    *
    * @return the specified resource as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public byte[] loadOriginalResource(String sName)
            throws IOException
        {
        return m_loader.loadOriginalResource(sName);
        }

    /**
    * Load the Resource Signature.
    *
    * @param sName  fully qualified resource name
    *
    * @return the specified Resource Signature as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public byte[] loadResourceSignature(String sName)
            throws IOException
        {
        return m_loader.loadResourceSignature(sName);
        }

    /**
    * Load the generated resource.
    *
    * @param sName  fully qualified resource name
    *
    * @return the specified resource as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public byte[] loadResource(String sName)
            throws IOException
        {
        byte[] ab = m_loader.loadResource(sName);

        // check if the requested resource is in the relocatable package
        if (ab == null && m_sPkg != null && sName.startsWith(m_sPkg))
            {
            sName = ClassFile.Relocator.PACKAGE + sName.substring(m_sPkg.length());
            ab    = m_loader.loadResource(sName);
            }

        return ab;
        }

    /**
    * Provide a short human-readable description of the trait.
    *
    * @return a human-readable description of this trait
    */
    public String toString()
        {
        return CLASS + '(' + m_loader + ')';
        }    
    
    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Resolver";

    /**
    * The Loader which the Resolver uses to access the "persistent storage"
    * of CD's, and JCS's.
    */
    private Loader m_loader;

    /**
    * The package for relocatable classes.
    */
    private String m_sPkg;

    /**
    * The relocator for relocatable classes.
    */
    private ClassFile.Relocator m_relocator;
    }


