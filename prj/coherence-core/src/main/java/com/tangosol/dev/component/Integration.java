/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ErrorList;
import com.tangosol.util.StringMap;
import com.tangosol.util.IllegalStringException;
import com.tangosol.util.WrapperException;
import com.tangosol.util.StringTable;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ChainedEnumerator;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.run.xml.XmlElement;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;

import java.beans.PropertyVetoException;

import java.util.Enumeration;
import java.util.NoSuchElementException;


/**
* An Integration Map provides a mapping of the methods and constants (final
* fields) of a Java Class Signature into a Component Definition.
*
* @version 1.00, 12/03/97
* @author  Cameron Purdy
* @version 2.00, 10/18/99
* @author  Patrick J. McNerthney
*/
public class Integration
        extends    Trait
        implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct an Integration Map to provide a method/behavior and
    * field/property mapping between a Java Class Signature and a
    * Component Definition.
    *
    * @param parent  the parent component that this integration belongs to
    * @param sSignature the name of the Java Class Signature this is mapping against
    */
    public Integration(Component parent, String sSignature)
        {
        super(parent, RESOLVED);

        if (sSignature == null || sSignature.length() == 0)
            {
            sSignature = BLANK;
            }

        m_sPrevSignature = BLANK;
        m_sSignature     = sSignature;
        m_sPrevModel     = BLANK;
        m_sModel         = BLANK;
        m_sPrevMisc      = BLANK;
        m_sMisc          = BLANK;

        m_mapPrevMethod  = new StringMap();
        m_mapMethod      = new StringMap();
        m_mapPrevField   = new StringMap();
        m_mapField       = new StringMap();

        m_jcs            = null;
        }

    /**
    * Copy constructor.
    *
    * @param parent  the Component containing this Integration
    * @param that    the Integration object to copy from
    */
    public Integration(Component parent, Integration that)
        {
        super(parent, that);
        this.m_sPrevSignature = that.m_sPrevSignature;
        this.m_sSignature     = that.m_sSignature;
        this.m_sPrevModel     = that.m_sPrevModel;
        this.m_sModel         = that.m_sModel;
        this.m_sPrevMisc      = that.m_sPrevMisc;
        this.m_sMisc          = that.m_sMisc;
        this.m_mapPrevMethod  = that.m_mapPrevMethod; // immutable
        this.m_mapMethod      = (StringMap) that.m_mapMethod.clone();
        this.m_mapPrevField   = that.m_mapPrevField;  // immutable
        this.m_mapField       = (StringMap) that.m_mapField .clone();

        this.m_jcs            = that.m_jcs;
        }

    /**
    * Construct the integration from a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param cd        the containing Component Definition
    * @param stream    the stream to read this Integration Definition from
    * @param nVersion  version of the data structure in the stream
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            reading the Integration Definition from the stream or if the
    *            stream does not contain a valid Integration Definition.
    */
    protected Integration(Component cd, DataInput stream, int nVersion)
            throws IOException
        {
        super(cd, stream, nVersion);

        m_sSignature = stream.readUTF();
        if (m_sSignature.length() == 0)
            {
            m_sSignature = BLANK;
            }

        m_sModel = stream.readUTF();
        if (m_sModel.length() == 0)
            {
            m_sModel = BLANK;
            }
        m_sMisc = stream.readUTF();
        if (m_sMisc.length() == 0)
            {
            m_sMisc = BLANK;
            }

        m_sPrevSignature = BLANK;
        m_sPrevModel     = BLANK;
        m_sPrevMisc      = BLANK;

        m_mapPrevMethod = new StringMap();
        m_mapMethod     = new StringMap(stream);
        m_mapPrevField  = new StringMap();
        m_mapField      = new StringMap(stream);

        m_jcs = null;
        }

    /**
    * Construct a Trait object from persistent data.  All traits implement
    * implement a constructor from persistent data.
    *
    * @param parent    the trait that contains this trait
    * @param xml       the XmlElement to read the persistent information from
    * @param nVersion  version of the data structure in the stream
    *
    * @throws IOException if an exception occurs reading the trait data
    */
    protected Integration(Trait parent, XmlElement xml, int nVersion)
            throws IOException
        {
        super(parent, xml, nVersion);

        m_sSignature = readString(xml.getElement("signature"));
        m_sModel     = readString(xml.getElement("model"));
        m_sMisc      = readString(xml.getElement("misc"));

        m_sPrevSignature = BLANK;
        m_sPrevModel     = BLANK;
        m_sPrevMisc      = BLANK;

        m_mapPrevMethod = new StringMap();
        m_mapMethod     = readStringMap(xml, "methods", "method");
        m_mapPrevField  = new StringMap();
        m_mapField      = readStringMap(xml, "fields", "field");

        m_jcs = null;
        }

    /**
    * Construct a blank Integration Trait.
    *
    * @param base    the base Integration to derive from
    * @param parent  the Component containing this Integration
    * @param nMode   one of RESOLVED, DERIVATION, MODIFICATION
    *
    * @see Trait#getBlankDerivedTrait
    */
    protected Integration(Integration base, Component parent, int nMode)
        {
        super(base, parent, nMode);

        m_sPrevSignature = BLANK;
        m_sSignature     = BLANK;
        m_sPrevModel     = BLANK;
        m_sModel         = BLANK;
        m_sPrevMisc      = BLANK;
        m_sMisc          = BLANK;

        m_mapPrevMethod  = new StringMap();
        m_mapMethod      = new StringMap();
        m_mapPrevField   = new StringMap();
        m_mapField       = new StringMap();

        m_jcs            = null;
        }


    // ----- persistence ----------------------------------------------------

    /**
    * Save the Interface Map to a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param stream   the stream to write this Interface Map to
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            writing the Interface Map to the stream
    */
    public synchronized void save(DataOutput stream)
            throws IOException
        {
        // save the trait information
        super.save(stream);

        stream.writeUTF(m_sSignature);
        stream.writeUTF(m_sModel);
        stream.writeUTF(m_sMisc);

        m_mapMethod.save(stream);
        m_mapField .save(stream);
        }

    /**
    * Save the Trait information to XML.  If derived traits have any
    * data of their own, then they must implement (i.e. supplement) this
    * method.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param xml  an XmlElement to write the persistent information to
    *
    * @throws IOException if an exception occurs saving the trait data
    */
    protected synchronized void save(XmlElement xml)
            throws IOException
        {
        xml.addElement("signature").setString(m_sSignature);
        xml.addElement("model").setString(m_sModel);
        xml.addElement("misc").setString(m_sMisc);

        super.save(xml);

        saveStringMap(xml, "methods", "method", m_mapMethod);
        saveStringMap(xml, "fields", "field", m_mapField);
        }


    // ----- derivation/modification ----------------------------------------

    /**
    * Construct a blank Trait.  All traits implement this method.
    *
    * A blank derived trait is an "l-value" for resolve and extract
    * processing.  In other words, when resolve and extract create a result
    * trait (either a resolved or delta trait) it is this method which is
    * responsible for creating that trait in its initial state ("blank").
    *
    * The resolve and extract methods implemented by this class (Trait) are
    * responsible for instantiating a resulting derived trait, either a
    * RESOLVED, DERIVATION, or MODIFICATION trait, depending on which method
    * (resolve or extract) was called and the parameters that were passed.
    *
    * This virtual method (getBlankDerivedTrait) is a "virtual constructor".
    * In other words, it is responsible for instantiating the correct class,
    * which is the same class as the "this" parameter (the base trait).
    *
    * @param parent  the containing trait or null if the blank trait is
    *                not going to be contained by another trait
    * @param nMode   one of RESOLVED, DERIVATION, MODIFICATION
    *
    * @return a new blank derived trait of this trait's class with the
    *         specified parent and mode
    *
    * @see #resolve
    * @see #extract
    */
    protected Trait getBlankDerivedTrait(Trait parent, int nMode)
        {
        return new Integration(this, (Component) parent, nMode);
        }

    /**
    * Create a derived/modified Trait by combining the information from this
    * Trait with the super or base level's Trait information.  Neither the
    * base ("this") nor the delta may be modified in any way.
    *
    * @param traitDelta  the Trait derivation or modification
    * @param parent      the Trait which will contain the resulting Trait or
    *                    null if the resulting Trait will not be contained
    * @param loader      the Loader object for JCS, CIM, and CD dependencies
    * @param errlist     the error list object to log error information to
    *
    * @return the result of applying the specified delta Trait to this Trait
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    protected Integration resolve(Integration traitDelta,       Component   parent,
                                  StringTable tblBehavior,      StringTable tblProperty,
                                  StringTable tblDeferBehavior, StringTable tblDeferProperty,
                                  Loader      loader,           ErrorList   errlist)
            throws ComponentException
        {
        Integration base    = this;
        Integration delta   = (Integration) resolveDelta(traitDelta, loader, errlist);
        Integration derived = (Integration) super.resolve(delta, parent, loader, errlist);

        // resolve class signature
        String signatureBase    = base.m_sSignature;
        String signatureDelta   = delta.m_sSignature;
        String signatureDerived = signatureBase;
        if (signatureDelta != BLANK && !signatureDelta.equals(signatureBase))
            {
            // TODO:  Use loader to verify that this class is a sub-class
            // of the previous class.  Actually, this should be part of
            // the isSignatureLegal call ???
            if (derived.isSignatureLegal(signatureDelta))
                {
                signatureDerived = signatureDelta;
                }
            else
                {
                out("TODO: log error -- Illegal signature " + signatureDelta);
                }
            }
        derived.m_sPrevSignature = signatureBase;
        derived.m_sSignature     = signatureDerived;

        if (signatureDerived == BLANK)
            {
            return derived;
            }

        // resolve model and misc
        String modelBase    = base.m_sModel;
        String miscBase     = base.m_sMisc;
        String modelDelta   = delta.m_sModel;
        String miscDelta    = delta.m_sMisc;
        String modelDerived = modelBase;
        String miscDerived  = miscBase;

        if (modelDelta != BLANK || miscDelta != BLANK)
            {
            if (!modelDelta.equals(modelBase) || !miscDelta.equals(miscBase))
                {
                // TODO:  We could verify that this model/misc can in fact
                // override the previous model/misc.  This should probably
                // be a static call into the model class themselves.  Actually,
                // this should be part of the isValid call ???
                if (isModelLegal(modelDelta, miscDelta))
                    {
                    modelDerived = modelDelta;
                    miscDerived  = miscDelta;
                    }
                else
                    {
                    out("TODO: log error -- Illegal model " + modelDelta + " " + miscDelta);
                    }
                }
            }
        derived.m_sPrevModel = modelBase;
        derived.m_sModel     = modelDerived;
        derived.m_sPrevMisc  = miscBase;
        derived.m_sMisc      = miscDerived;

        // Copy the base mapped methods to the derived's previous mapped methods.

        StringMap mapPrev = (StringMap) base.m_mapPrevMethod.clone();
        StringMap mapNew  = base.m_mapMethod;
        for (Enumeration enmr = mapNew.primaryStrings(); enmr.hasMoreElements();)
            {
            String sPrimary = (String) enmr.nextElement();
            try
                {
                mapPrev.put(sPrimary, mapNew.get(sPrimary));
                }
            catch (IllegalStringException e)
                {
                throw new WrapperException(e);
                }
            }
        derived.m_mapPrevMethod = mapPrev;
        derived.m_mapMethod     = new StringMap();

        // Copy the base mapped fields to the derived's previous mapped fields.

        mapPrev = (StringMap) base.m_mapPrevField.clone();
        mapNew  = base.m_mapField;
        for (Enumeration enmr = mapNew.primaryStrings(); enmr.hasMoreElements();)
            {
            String sPrimary = (String) enmr.nextElement();
            try
                {
                mapPrev.put(sPrimary, mapNew.get(sPrimary));
                }
            catch (IllegalStringException e)
                {
                throw new WrapperException(e);
                }
            }
        derived.m_mapPrevField = mapPrev;
        derived.m_mapField     = new StringMap();

        String sDerivedName = parent.getQualifiedName();

        // load the JCS referenced by the integration map
        Component jcsDerived = loader.loadSignature(signatureDerived);
        if (jcsDerived == null)
            {
            // TODO: this should not be reported as a problem
            //       when a modification is loaded...
            // unable to load Java Class Signature of integratee
            logError(RESOLVE_LOADINTERFACE, ERROR, new Object[]
                    {signatureDerived, ATTR_INTEGRATION_SIGNATURE,
                     sDerivedName}, errlist);
            return derived;
            }

        // resolve mapped methods

        for (Enumeration enmr = delta.getMethods(); enmr.hasMoreElements();)
            {
            boolean fAddable = true;

            // verify the "from" method name exists in the jcs (if we have one)
            String   sMethod = (String) enmr.nextElement();
            Behavior method  = null;
            method = jcsDerived.getBehavior(sMethod);
            if (method == null)
                {
                fAddable = false;
                logError(MAP_MISSINGMETHOD, WARNING, new Object[]
                        {signatureDerived, sDerivedName, sMethod, jcsDerived.getName()}, errlist);
                }
            else
                {
                // verify that method is accessible
                int nAccess = method.getAccess();
                if (nAccess != ACCESS_PUBLIC && nAccess != ACCESS_PROTECTED)
                    {
                    fAddable = false;
                    logError(MAP_INVALIDMETHOD, WARNING, new Object[]
                            {signatureDerived, sDerivedName, sMethod, jcsDerived.getName()}, errlist);
                    }
                }

            // verify the "to" behavior name is valid for this component
            String   sBehavior = delta.getBehavior(sMethod);
            Behavior behavior  = parent.getBehavior(sBehavior);
            if (behavior == null)
                {
                // verify that the signature is not reserved
                if (parent.isBehaviorReserved(sBehavior))
                    {
                    fAddable = false;
                    logError(MAP_RESERVEDBEHAVIOR, WARNING, new Object[]
                            {signatureDerived, sDerivedName, sBehavior}, errlist);
                    }

                // verify that the signature is not an accessor for an
                // integrated field (property)
                // TODO
                }
            else
                {
                if (method != null)
                    {
                    // certain behavior attributes that must match cannot be
                    // modified (fixed) if the behavior has non-manual origin
                    boolean fFixable  = !behavior.isFromNonManual();
                    int     nSeverity = (fFixable ? INFO : WARNING);

                    // verify that the static attribute matches
                    if (method.isStatic() != behavior.isStatic())
                        {
                        fAddable = fAddable && fFixable;
                        logError(MAP_SCOPEMISMATCH, nSeverity, new Object[]
                                {signatureDerived, sDerivedName, sBehavior, sMethod}, errlist);
                        }

                    // verify that the return value type matches
                    if (method  .getReturnValue().getDataType() !=
                        behavior.getReturnValue().getDataType())
                        {
                        fAddable = fAddable && fFixable;
                        logError(MAP_RETURNMISMATCH, nSeverity, new Object[]
                                {signatureDerived, sDerivedName,
                                 sBehavior, behavior.getReturnValue().getDataType().toString(),
                                 sMethod, method.getReturnValue().getDataType().toString()}, errlist);
                        }

                    // verify that the parameter directions match
                    if (!method.getParameterDirections().equals(behavior.getParameterDirections()))
                        {
                        fAddable = fAddable && fFixable;
                        logError(MAP_DIRECTIONMISMATCH, nSeverity, new Object[]
                                {signatureDerived, sDerivedName, sBehavior, sMethod}, errlist);
                        }
                    }
                }

            if (!fAddable)
                {
                continue;
                }

            // add the entry to the integration method map
            try
                {
                derived.m_mapMethod.put(sMethod, sBehavior);
                }
            catch (IllegalStringException e)
                {
                throw new WrapperException(e);
                }

            // search for a Behavior in the "defer" list and use it
            // if possible instead of creating a Behavior from scratch
            // (note - changes made here need to be made below too)
            if (behavior == null && tblDeferBehavior != null)
                {
                behavior = (Behavior) tblDeferBehavior.get(sBehavior);
                if (behavior != null)
                    {
                    // un-defer it
                    tblDeferBehavior.remove(sBehavior);

                    // copy the behavior to this component
                    behavior = new Behavior(parent, behavior);

                    // the Behavior is new at this level
                    behavior.setExists(EXISTS_INSERT);

                    // add it to this level
                    tblBehavior.put(sBehavior, behavior);
                    }
                }

            if (method != null)
                {
                if (behavior == null)
                    {
                    // if no such Behavior signature already exists, create
                    // one based on the method
                    String sName = sBehavior.substring(0, sBehavior.indexOf('('));
                    behavior = new Behavior(parent, this, sName, method);
                    try
                        {
                        parent.addBehavior(behavior, false);
                        }
                    catch (PropertyVetoException e)
                        {
                        throw new WrapperException(e);
                        }
                    }
                else
                    {
                    // resolve any differences between the existing Behavior
                    // and the integrated method
                    behavior.setDeclaredByIntegration(this, method);
                    }
                }

            }

        // resolve mapped fields

        for (Enumeration enmr = delta.getFields(); enmr.hasMoreElements();)
            {
            boolean fAddable = true;

            // verify the "from" field name exists in the jcs (if we have one)
            String   sField = (String) enmr.nextElement();
            Property field  = jcsDerived.getProperty(sField);
            if (field == null)
                {
                fAddable = false;
                logError(MAP_MISSINGFIELD, WARNING, new Object[]
                        {signatureDerived, sDerivedName, sField, jcsDerived.getName()}, errlist);
                }

            // verify the "to" property name is valid for this component
            String   sProperty = delta.getProperty(sField);
            Property property  = parent.getProperty(sProperty);
            if (field != null)
                {
                if (property == null)
                    {
                    DataType dt      = field.getDataType();
                    boolean  fStatic = field.isStatic();

                    int      nAccess = field.getAccess();
                    boolean  fAccess = (nAccess == ACCESS_PUBLIC || nAccess == ACCESS_PROTECTED);
                    if (fAccess && field.isJavaConstant())
                        {
                        if (!Property.isJavaConstantCreatable(parent, dt, sProperty,
                                                              PROP_SINGLE, fStatic, field.getAccess()))
                            {
                            fAddable = false;
                            logError(MAP_PROPNOTCREATABLE, WARNING, new Object[]
                                    {signatureDerived, sDerivedName, sProperty,
                                     dt.toString()}, errlist);
                            }
                        }
                    else if (fAccess && field.isStandardProperty())
                        {
                        if (!Property.isPropertyCreatable(parent, dt, sProperty,
                                                          PROP_SINGLE, fStatic, false))
                            {
                            fAddable = false;
                            logError(MAP_PROPNOTCREATABLE, WARNING, new Object[]
                                    {signatureDerived, sDerivedName, sProperty,
                                     dt.toString()}, errlist);
                            }
                        }
                    else
                        {
                        fAddable = false;
                        logError(MAP_INVALIDFIELD, WARNING, new Object[]
                                {signatureDerived, sDerivedName, sField,
                                 jcsDerived.getName()}, errlist);
                        }
                    }
                else
                    {
                    // certain Property attributes that must match cannot be
                    // modified (fixed) if the Property has non-manual origin
                    boolean fFixable   = !property.isFromNonManual();
                    int     nSeverity  = (fFixable ? INFO : WARNING);

                    // determine if the Property can be modified to match the
                    // integrated field
                    int nAccess = (field.isJavaConstant() ? field.getAccess() : ACCESS_PRIVATE);
                    if (property.getAccess() != nAccess)
                        {
                        fAddable = fAddable && fFixable;
                        logError(MAP_PROPFIELDMISMATCH, WARNING, new Object[]
                                {signatureDerived, sDerivedName, Property.ATTR_ACCESS,
                                 sProperty, sField}, errlist);
                        }

                    if (field.isStandardProperty())
                        {
                        // ! TODO accessors must be available
                        }

                    if (property.isStatic() != field.isStatic())
                        {
                        fAddable = fAddable && fFixable;
                        logError(MAP_PROPFIELDMISMATCH, WARNING, new Object[]
                                {signatureDerived, sDerivedName, Property.ATTR_STATIC,
                                 sProperty, sField}, errlist);
                        }

                    if (property.isFinal() != field.isFinal())
                        {
                        fAddable = fAddable && fFixable;
                        logError(MAP_PROPFIELDMISMATCH, WARNING, new Object[]
                                {signatureDerived, sDerivedName, Property.ATTR_FINAL,
                                 sProperty, sField}, errlist);
                        }

                    if (property.isPersistent() != field.isPersistent())
                        {
                        fAddable = fAddable && fFixable;
                        logError(MAP_PROPFIELDMISMATCH, WARNING, new Object[]
                                {signatureDerived, sDerivedName, Property.ATTR_PERSISTENT,
                                 sProperty, sField}, errlist);
                        }

                    if (property.getDirection() != field.getDirection())
                        {
                        fAddable = fAddable && fFixable;
                        logError(MAP_PROPFIELDMISMATCH, WARNING, new Object[]
                                {signatureDerived, sDerivedName, Property.ATTR_DIRECTION,
                                 sProperty, sField}, errlist);
                        }

                    if (property.getDataType() != field.getDataType())
                        {
                        fAddable = fAddable && fFixable;
                        logError(MAP_PROPFIELDMISMATCH, WARNING, new Object[]
                                {signatureDerived, sDerivedName, Property.ATTR_DATATYPE,
                                 sProperty, sField}, errlist);
                        }

                    if (property.getIndexed() != PROP_SINGLE)
                        {
                        fAddable = fAddable && fFixable;
                        logError(MAP_PROPFIELDMISMATCH, WARNING, new Object[]
                                {signatureDerived, sDerivedName, Property.ATTR_INDEXED,
                                 sProperty, sField}, errlist);
                        }

                    // value must be NO_VALUE (or NO_DELTA)
                    if (!(property.isNoValue() || property.isNoDelta()))
                        {
                        fAddable = fAddable && fFixable;
                        logError(MAP_PROPVALUE, WARNING, new Object[]
                                {signatureDerived, sDerivedName, sProperty}, errlist);
                        }
                    }
                }

            if (!fAddable)
                {
                continue;
                }

            // add the entry to the integration field map and interface
            try
                {
                derived.m_mapField.put(sField, sProperty);
                }
            catch (IllegalStringException e)
                {
                throw new WrapperException(e);
                }

            // search for a Property in the "defer" list and use it
            // if possible instead of creating a Property from scratch
            if (property == null && tblDeferProperty != null)
                {
                property = (Property) tblDeferProperty.get(sProperty);
                if (property != null)
                    {
                    // un-defer it
                    tblDeferProperty.remove(sProperty);

                    // copy the property to this component
                    property = new Property(parent, property);

                    // the Property is new at this level
                    property.setExists(EXISTS_INSERT);

                    // add it to this level
                    tblProperty.put(sProperty, property);
                    }
                }

            if (field != null)
                {
                if (property == null)
                    {
                    // if no such Property already exists, create one based
                    // on the field
                    DataType dt       = field.getDataType();
                    boolean  fStatic  = field.isStatic();

                    if (field.isJavaConstant())
                        {
                        property = Property.createJavaConstant(
                                parent, dt, sProperty, PROP_SINGLE, fStatic, field.getAccess());
                        }
                    else
                        {
                        property = Property.createProperty(
                                parent, dt, sProperty, PROP_SINGLE, fStatic, false);
                        }

                    try
                        {
                        parent.addProperty(property, false);
                        }
                    catch (PropertyVetoException e)
                        {
                        throw new WrapperException(e);
                        }
                    }

                // resolve any differences between the existing Property
                // and the integrated field
                property.setFromIntegration(this, field);

                // make sure accessors are available
                if (!property.isConstant())
                    {
                    if (tblDeferBehavior != null)
                        {
                        for (int i = Property.PA_FIRST; i <= Property.PA_LAST; ++i)
                            {
                            if (property.isAccessorApplicable(i) && property.getAccessor(i) == null)
                                {
                                // search for a Behavior in the "defer" list and
                                // use it if possible instead of creating one
                                // (note - cut&paste from above)
                                String   sBehavior = property.getAccessorSignature(i);
                                Behavior behavior  = (Behavior) tblDeferBehavior.get(sBehavior);
                                if (behavior != null)
                                    {
                                    // un-defer it
                                    tblDeferBehavior.remove(sBehavior);

                                    // copy the behavior to this component
                                    behavior = new Behavior(parent, behavior);

                                    // the Behavior is new at this level
                                    behavior.setExists(EXISTS_INSERT);

                                    // add it to this level
                                    tblBehavior.put(sBehavior, behavior);
                                    }
                                }
                            }
                        }

                    // now that we've supplied as many accessors as we can
                    // find, let the property create the rest and synch the
                    // ones that already exist
                    property.addAccessors();
                    }
                }

            }

        return derived;
        }

    /*
    * Create a derivation/modification Trait by determining the differences
    * between the derived Trait ("this") and the passed base Trait.  Neither
    * the derived Trait ("this") nor the base may be modified in any way.
    *
    * @param base     the base Trait to extract
    * @param parent   the Trait which will contain the resulting Trait or
    *                 null if the resulting Trait will not be contained
    * @param loader   the Loader object for JCS, CIM, and CD dependencies
    * @param errlist  the error list object to log error information to
    *
    * @return the delta between this derived Trait and the base Trait
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    protected Trait extract(Trait traitBase, Trait parent, Loader loader, ErrorList errlist)
            throws ComponentException
        {
        Integration derived = this;
        Integration base    = (Integration) traitBase;
        Integration delta   = (Integration) super.extract(base, parent, loader, errlist);

        // We only derive.
        if (delta.getMode() != DERIVATION)
            {
            throw new IllegalArgumentException(
                    CLASS + ".extract:  "
                    + "Only Integration derivations can be extracted!");
            }

        // extract signature
        String signatureBase    = base.m_sSignature;
        String signatureDerived = derived.m_sSignature;
        String signatureDelta   = BLANK;
        if (!signatureDerived.equals(signatureBase))
            {
            signatureDelta = signatureDerived;
            }
        delta.m_sSignature = signatureDelta;

        // extract model/misc
        String modelBase    = base.m_sModel;
        String modelDerived = derived.m_sModel;
        String modelDelta   = BLANK;
        String miscBase     = base.m_sMisc;
        String miscDerived  = derived.m_sMisc;
        String miscDelta    = BLANK;

        if (!modelDerived.equals(modelBase)
            || !miscDerived.equals(miscBase))
            {
            modelDelta = modelDerived;
            miscDelta  = miscDerived;
            }
        delta.m_sModel = modelDelta;
        delta.m_sMisc  = miscDelta;

        // ----- Methods
        extractMap(base.m_mapPrevMethod,
                   base.m_mapMethod,
                   derived.m_mapPrevMethod,
                   derived.m_mapMethod,
                   delta.m_mapMethod,
                   errlist);

        // ----- Fields
        extractMap(base.m_mapPrevField,
                   base.m_mapField,
                   derived.m_mapPrevField,
                   derived.m_mapField,
                   delta.m_mapField,
                   errlist);

        delta.m_jcs = derived.m_jcs;

        return delta;
        }

    private void extractMap(StringMap mapPrevBase,
                            StringMap mapBase,
                            StringMap mapPrevDerived,
                            StringMap mapDerived,
                            StringMap mapDelta,
                            ErrorList errlist)
        {
        // copy to the delta ALL map entries with reside in the
        // derived, but do not reside in the base

        for (Enumeration enmr = mapPrevDerived.primaryStrings(); enmr.hasMoreElements(); )
            {
            String sName = (String) enmr.nextElement();
            if (!mapPrevBase.contains(sName) && !mapBase.contains(sName))
                {
                // copy it to the delta component
                try
                    {
                    mapDelta.put(sName, mapPrevDerived.get(sName));
                    }
                catch (IllegalStringException e)
                    {
                    throw new WrapperException(e);
                    }
                }
            }
        for (Enumeration enmr = mapDerived.primaryStrings(); enmr.hasMoreElements(); )
            {
            String sName = (String) enmr.nextElement();
            if (!mapPrevBase.contains(sName) && !mapBase.contains(sName))
                {
                // copy it to the delta component
                try
                    {
                    mapDelta.put(sName, mapDerived.get(sName));
                    }
                catch (IllegalStringException e)
                    {
                    throw new WrapperException(e);
                    }
                }
            }
        }

    /**
    * Finalize the extract process.  This means that this trait will not
    * be asked to extract again.  A trait is considered persistable after
    * it has finalized the extract process.
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    public synchronized void finalizeExtract()
            throws ComponentException
        {
        super.finalizeExtract(null, null);

        try
            {

            // Discard non-mapped methods
            for (Enumeration enmrMethods = m_mapMethod.primaryStrings(); enmrMethods.hasMoreElements();)
                {
                String method = (String) enmrMethods.nextElement();
                String behavior = m_mapMethod.get( method );
                if ( behavior == null )
                    {
                    m_mapMethod.remove( method );
                    }
                }

            // Discard non-mapped fields
            for (Enumeration enmrFields = m_mapField.primaryStrings(); enmrFields.hasMoreElements(); )
                {
                String field = (String) enmrFields.nextElement();
                String property = m_mapField.get( field );
                if ( property == null )
                    {
                    m_mapField.remove( field );
                    }
                }

            }
        catch (IllegalStringException e)
            {
            throw new WrapperException(e);
            }

        }

    // ----- accessors ------------------------------------------------------

    /**
    * Determine the Component which contains this Integration.
    *
    * @return the Component trait that contains this Integration
    */
    public Component getComponent()
        {
        return (Component) getParentTrait();
        }

    /**
    * Determine if this Integration is different from integration
    * on the super component.
    */
    public boolean isNewIntegration()
        {
        return (!m_sPrevSignature.equals(m_sSignature)
                || !m_sPrevModel.equals(m_sModel)
                || !m_sPrevMisc.equals(m_sMisc)
                || !m_mapMethod.isEmpty()
                || !m_mapField.isEmpty()
                );
        }

    /**
    * Determine if the integration is modifiable.
    *
    * @return true if changes are permitted to the integration
    */
    public boolean isModifiable()
        {
        return (super.isModifiable()
                && ((Component) getParentTrait()).isIntegrationSettable()
                && m_jcs != null);
        }

    /**
    * Determine the unique name for this trait.
    *
    * A sub-trait that exists within a collection of sub-traits must have
    * two ways of being identified:
    *
    *   1)  A unique name, which is a string identifier
    *   2)  A UID as a secondary identifier (dog tag)
    *
    * @return the primary string identifier of the trait
    */
    protected String getUniqueName()
        {
        return getSignature();
        }

    /**
    * Determine the unique description for this trait.  The unique
    * description is in the format:
    *
    *   <descriptor> + ' ' + <uniquename>
    *
    * All traits must be uniquely identifiable by a "description string".
    * This enables the origin implementation built into Trait to use the
    * description string to differentiate between Trait origins.
    *
    * @return the description string for the trait
    */
    protected String getUniqueDescription()
        {
        return DESCRIPTOR + ' ' + getUniqueName();
        }

    /**
    * Determines if this Integration exists for any reason at all.
    *
    * @return true if the Behavior is discardable
    */
    protected boolean isDiscardable()
        {
        int nMode = getMode();
        if (nMode == DERIVATION || nMode == MODIFICATION)
            {
            // check for delta information
            if (isNewIntegration())
                {
                return false;
                }
            }

        return super.isDiscardable();
        }

    /**
    * Get the name of the Java Class Signature that the previous
    * Integration Map maps from.  This property is read-only.
    *
    * @return the name of the Java Class Signature
    */
    public String getPreviousSignature()
        {
        return m_sPrevSignature;
        }

    /**
    * Get the name of the Java Class Signature that this Integration Map maps
    * from
    *
    * @return the name of the Java Class Signature
    */
    public String getSignature()
        {
        return m_sSignature;
        }

    /**
    * Determine if the name of the map can be changed.
    *
    * @return true if okay to change
    */
    public boolean isSignatureSettable()
        {
        return super.isModifiable();
        }

    /**
    * Determine if the specified map name is legal.
    *
    * @return true if the specified name is legal
    */
    public boolean isSignatureLegal(String sName)
        {
        return ClassHelper.isQualifiedNameLegal(sName);
        }

    /**
    * Set the name of the Integration Map.
    *
    * @param sSignature  the name of the Integration Map
    */
    public void setSignature(String sSignature)
            throws PropertyVetoException
        {
        setSignature(sSignature, true);
        }

    /**
    * Set the name of the Integration Map.
    *
    * @param sSignature  the new name for the Component
    * @param fVetoable   true if the setter can veto the value
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    protected synchronized void setSignature(String sSignature, boolean fVetoable)
            throws PropertyVetoException
        {
        if (sSignature == null || sSignature.length() == 0)
            {
            sSignature = BLANK;
            }

        String sPrev = m_sSignature;

        if (sSignature.equals(sPrev))
            {
            return;
            }

        if (fVetoable)
            {
            if (!isSignatureSettable())
                {
                readOnlyAttribute(ATTR_INTEGRATION_SIGNATURE, sPrev, sSignature);
                }

            if (!isSignatureLegal(sSignature))
                {
                illegalAttributeValue(ATTR_INTEGRATION_SIGNATURE, sPrev, sSignature);
                }

            fireVetoableChange(ATTR_INTEGRATION_SIGNATURE, sPrev, sSignature);
            }

        m_sSignature = sSignature;
        m_sModel     = m_sPrevModel;
        m_sMisc      = m_sPrevMisc;
        m_mapMethod  = new StringMap();
        m_mapField   = new StringMap();
        clearJcs();

        firePropertyChange(ATTR_INTEGRATION_SIGNATURE, sPrev, sSignature);
        }

    /**
    * Get the name of the Integration Model.
    *
    * @return the name of the Integration Model
    */
    public String getModel()
        {
        return m_sModel;
        }

    /**
    * Get the miscellaneous model information.  This information is stored
    * in the Integration Map by a tool to provide model-specific build
    * instructions to the Integration Model.
    *
    * @return the miscellaneous model information
    */
    public String getMisc()
        {
        return m_sMisc;
        }

    /**
    * Determine if the name of the Integration model can be changed.
    *
    * @return true if okay to change
    */
    public boolean isModelSettable()
        {
        return isModifiable();
        }

    /**
    * Determine whether the passed Integration Model name is legal.
    *
    * @param sModel the name of the Integration Model
    *
    * @return true if the specified value is a legal Integration Model name
    *
    * @see com.tangosol.util.ClassHelper#getDerivedName
    */
    public boolean isModelLegal(String sModel, String sMisc)
        {
        if (sModel == null || sModel.length() == 0)
            {
            sModel = BLANK;
            }
        if (sMisc == null  || sMisc.length() == 0)
            {
            sMisc = BLANK;
            }
        if (sModel == BLANK)
            {
            return sMisc == BLANK;
            }
        return ClassHelper.isPartialNameLegal(sModel);
        }

    /**
    * Set the name and misc of the Integration Model.
    *
    * @param sModel the name of the Integration Model
    * @param sMisc  the misc of the Integration Model
    */
    public void setModel(String sModel, String sMisc)
            throws PropertyVetoException
        {
        setModel(sModel, sMisc, true);
        }

    /**
    * Set the name and misc of the Integration Model.
    *
    * @param sModel     the name of the Integration Model
    * @param sMisc      TODO
    * @param fVetoable  true if the setter can veto the value
    */
    protected synchronized void setModel(String sModel, String sMisc, boolean fVetoable)
            throws PropertyVetoException
        {
        if (sModel == null || sModel.length() == 0)
            {
            sModel = BLANK;
            }
        if (sMisc == null  || sMisc.length() == 0)
            {
            sMisc = BLANK;
            }

        String[] asPrev = new String[]
                {
                m_sModel, m_sMisc
                };
        String[] asNew  = new String[]
                {
                sModel, sMisc
                };

        if (asNew[0].equals(asPrev[0])
         && asNew[1].equals(asPrev[1]))
            {
            return;
            }

        if (fVetoable)
            {
            if (!isModelSettable())
                {
                readOnlyAttribute(ATTR_INTEGRATION_MODEL, asPrev, asNew);
                }

            if (!isModelLegal(asNew[0], asNew[1]))
                {
                illegalAttributeValue(ATTR_INTEGRATION_MODEL, asPrev, asNew);
                }

            fireVetoableChange(ATTR_INTEGRATION_MODEL, asPrev, asNew);
            }

        m_sModel = asNew[0];
        m_sMisc  = asNew[1];

        firePropertyChange(ATTR_INTEGRATION_MODEL, asPrev, asNew);
        }

    // ----- methods/behaviors

    /**
    * Get all Java Class Signature method signatures.  Some of the methods
    * may have been removed from the map; if so, the getBehavior method will
    * return null for the method.
    *
    * @return an enumeration of strings specifying each Java Class Signature
    *         method signature
    */
    public Enumeration getMethods()
        {
        return new ChainedEnumerator(m_mapPrevMethod.primaryStrings(), m_mapMethod.primaryStrings());
        }

    /**
    * Get all Component Definition behavior signatures corresponding to
    * the Java Class Signature method signatures.  Some of the behavior
    * signatures may be null meaning that the corresponding method was
    * removed.
    *
    * @return an enumeration of strings specifying each Component Definition
    *         behavior signature with an order corresponding to the
    *         enumeration returned from getMethods
    */
    public Enumeration getBehaviors()
        {
        return new ChainedEnumerator(m_mapPrevMethod.secondaryStrings(), m_mapMethod.secondaryStrings());
        }

    /**
    * Determine the behavior signature that corresponds to the specified
    * method signature.
    *
    * @param sMethodSignature the signature of the Java Class Signature
    *        method to look up
    *
    * @return a string specifying the corresponding behavior signature or null if
    *         the method signature does not exist or has been removed from the map
    */
    public String getBehavior(String sMethodSignature)
        {
        String sBehavior = m_mapPrevMethod.get(sMethodSignature);
        if (sBehavior == null)
            {
            sBehavior = m_mapMethod.get(sMethodSignature);
            }
        return sBehavior;
        }

    public String getBehavior(Behavior method)
        {
        return getBehavior(method.getSignature());
        }

    public String getSuggestedBehavior(Behavior method)
        {
        String sBehavior = getBehavior(method);
        if (sBehavior != null)
            {
            return sBehavior;
            }
        Property field = getFieldFromAccessor(method);
        if (field == null)
            {
            return null;
            }
        String sProperty = getSuggestedProperty(field);
        if (sProperty == null)
            {
            return null;
            }
        sBehavior = method.getSignature();
        return sBehavior.substring(0, sBehavior.charAt(0) == 'i' ? 2 : 3)
            + sProperty + sBehavior.substring(sBehavior.indexOf('('));
        }

    /**
    * Determine if the method signature exists in the integration map
    *
    * @param sMethodSignature the signature of the Java Class Signature
    *        method to look up
    *
    * @return true if the signature is in the map, false otherwise
    */
    public boolean containsMethod(String sMethodSignature)
        {
        //return m_mapMethod.contains(sMethodSignature);
        boolean fContains = m_mapPrevMethod.contains(sMethodSignature);
        if (!fContains)
            {
            fContains = m_mapMethod.contains(sMethodSignature);
            }
        return fContains;
        }

    /**
    * Determine the method signature that corresponds to the specified
    * Component Definition behavior signature.
    *
    * @param sBehaviorSignature the signature of the Component Definition
    *        behavior to look up
    *
    * @return a string specifying the corresponding method signature or
    *         null if the behavior signature was not found
    */
    public String getMethod(String sBehaviorSignature)
        {
        String sMethod = m_mapPrevMethod.getPrimary(sBehaviorSignature);
        if (sMethod == null)
            {
            sMethod = m_mapMethod.getPrimary(sBehaviorSignature);
            }
        return sMethod;
        }

    /**
    * Determine if the method was inherited from the super-class.
    *
    * @return true if it was inherited
    */
    public boolean isMethodInherited(Behavior method)
        {
        return method.getExists() != EXISTS_INSERT;
        }

    /**
    * Determine if the behavior was inherited from our super-map
    *
    * @return true if it was inherited
    */
    public boolean isMethodBehaviorInherited(Behavior method)
        {
        return m_mapPrevMethod.contains(method.getSignature());
        }

    /**
    * Determine if it is okay to add or remove mappped methods.
    *
    * @return true if okay to add mapped methods
    */
    public boolean isMethodSettable(Behavior method)
        {
        if (!isModifiable() || method == null)
            {
            return false;
            }
        String sMethodSignature = method.getSignature();
        if (m_mapPrevMethod.contains(sMethodSignature)
            || m_jcs.getBehavior(sMethodSignature) != method)
            {
            return false;
            }

        return true;
        }

    /**
    * Determine if it is okay to rename mappped methods.
    *
    * @return true if okay to rename mapped methods
    */
    public boolean isMethodRenamable(Behavior method)
        {
        Property field = getFieldFromAccessor(method);
        if (field != null)
            {
            // We are a property accessor, check if the property
            // is currently mapped.
            String sProperty = getProperty(field);
            if (sProperty != null)
                {
                // We claim the property is already mapped, this will be the case as long
                // as one of it's accessors are mapped.  Only allow renaming of this
                // accessor if the property is not defined in the parent component.
                if (isPropertyFromIntegration(sProperty))
                    {
                    return false;
                    }
                }
            }
        return true;
        }

    private Property getFieldFromAccessor(Behavior method)
        {
        // Check if we are the accessor of any of the properties.
        String [] asProperties = m_tblProperties.strings();
        for (int i = 0; i < asProperties.length; ++i)
            {
            Property field = (Property) m_tblProperties.get(asProperties[i]);
            if (field != null)
                {
                Behavior[] accessors = field.getAccessors();
                for (int ii = 0; ii < accessors.length; ++ii)
                    {
                    if (accessors[ii] == method)
                        {
                        return field;
                        }
                    }
                }
            }
        return null;
        }

    /**
    * Determine whether the passed method and behavior names are legal.
    *
    * @param method              the method to check
    * @param sBehaviorSignature  the behavior to check
    *
    * @return true if the specified values are okay
    *
    */
    public boolean isBehaviorLegal(Behavior method, String sBehaviorSignature)
        {
        if (method == null)
            {
            return false;
            }
        String sMethodSignature = method.getSignature();

        Component parent = (Component) getParentTrait();
        if (m_jcs == null)
            {
            return false;
            }

        if (m_jcs.getBehavior(sMethodSignature) != method)
            {
            return false;
            }

        // verify that method is accessible
        int nAccess = method.getAccess();
        if (nAccess != ACCESS_PUBLIC && nAccess != ACCESS_PROTECTED)
            {
            return false;
            }

        String sMethodParms = sMethodSignature.substring(sMethodSignature.indexOf('('));

        // if method is being mapped to
        if (sBehaviorSignature == null || sBehaviorSignature.length() == 0)
            {
            return true;
            }

        String sPrevBehaviorSignature = getBehavior(sMethodSignature);
        if (sPrevBehaviorSignature != null && sPrevBehaviorSignature.length() == 0)
            {
            sPrevBehaviorSignature = null;
            }
        if (sBehaviorSignature.equals(sPrevBehaviorSignature))
            {
            return true;
            }

        int of = sBehaviorSignature.indexOf('(');
        if (of < 0)
            {
            return false;
            }
        String sBehaviorName  = sBehaviorSignature.substring(0, of);
        String sBehaviorParms = sBehaviorSignature.substring(of);

        // Behavior name must be a legal simple Java identifier
        //   and parameters must match
        if (!ClassHelper.isSimpleNameLegal(sBehaviorName)
            || !sBehaviorParms.equals(sMethodParms))
            {
            return false;
            }

        // Make sure the behavior is already not mapped to a different method
        String sBehaviorMethod = getMethod(sBehaviorSignature);
        if (sBehaviorMethod != null)
            {
            return false;
            }

        // Get the current behavior if it exists
        Behavior behavior = null;
        if (sPrevBehaviorSignature != null)
            {
            behavior = parent.getBehavior(sPrevBehaviorSignature);
            }
        if (behavior != null)
            {
            return behavior.isNameLegal(sBehaviorName);
            }

        // Check if we are mapping to an existing behavior
        behavior = parent.getBehavior(sBehaviorSignature);
        if (behavior == null)
            {
            // verify that the new signature is not reserved
            if (parent.isBehaviorReserved(sBehaviorSignature))
                {
                return false;
                }
            // verify that the signature is not an accessor for an
            // integrated field (property)
            // TODO - also in Component.java
            return true;
            }

        // We are mapping to an existing behavior.
        // certain behavior attributes that must match cannot be
        // modified (fixed) if the behavior has non-manual origin
        if (!behavior.isFromNonManual())
            {
            return true;
            }

        // verify that the static attribute matches
        if (method.isStatic() != behavior.isStatic())
            {
            return false;
            }
        // verify that the return value type matches
        if (method.getReturnValue().getDataType()
            != behavior.getReturnValue().getDataType())
            {
            return false;
            }
        // verify that the parameter directions match
        if (!method.getParameterDirections()
            .equals(behavior.getParameterDirections()))
            {
            return false;
            }

        return true;
        }

    /**
    * Modify the behavior signature that maps to a particular method.
    *
    * @param method              the method
    * @param sBehaviorSignature  the (new) signature of the behavior
    *                            (may be null)
    *
    * @exception PropertyVetoException if the behavior name
    *            already maps to a different method
    */
    public void setBehavior(Behavior method, String sBehaviorSignature)
            throws PropertyVetoException
        {
        setBehavior(method, sBehaviorSignature, true);
        }

    /**
    * Modify the behavior signature that maps to a particular method.
    *
    * @param method              TODO
    * @param sBehaviorSignature  the (new) signature of the behavior
    *                            (may be null)
    * @param fVetoable           TODO
    *
    * @exception PropertyVetoException if the behavior name
    *            already maps to a different method
    */
    protected synchronized void setBehavior(Behavior method, String sBehaviorSignature, boolean fVetoable)
            throws PropertyVetoException
        {
        if (method == null)
            {
            return;
            }
        String sMethodSignature = method.getSignature();

        if (sBehaviorSignature != null && sBehaviorSignature.length() == 0)
            {
            sBehaviorSignature = null;
            }

        // get the current mapping, if any
        String sPrevBehaviorSignature = getBehavior(sMethodSignature);
        if (sPrevBehaviorSignature != null && sPrevBehaviorSignature.length() == 0)
            {
            sPrevBehaviorSignature = null;
            }

        // don't proceed if there is no change
        if (sPrevBehaviorSignature == null)
            {
            if (sBehaviorSignature == null)
                {
                return;
                }
            }
        else
            {
            if (sPrevBehaviorSignature.equals(sBehaviorSignature))
                {
                return;
                }
            }

        // used for property change events
        Object[] asPrev = new Object[]
            {
            method, sPrevBehaviorSignature
            };

        // verify the validitiy of the change
        if (fVetoable)
            {
            if (!isMethodSettable(method))
                {
                readOnlyAttribute(ATTR_INTEGRATION_METHOD, asPrev, sBehaviorSignature);
                }

            // check if a method and behavior can be added
            if (!isBehaviorLegal(method, sBehaviorSignature))
                {
                illegalAttributeValue(ATTR_INTEGRATION_METHOD, asPrev, sBehaviorSignature);
                }

            // notify veto listeners
            fireVetoableChange(ATTR_INTEGRATION_METHOD, asPrev, sBehaviorSignature);
            }

        // Check if this is the last mapped accessor of
        // a "manufactured" field to be removed.
        boolean fLastAccessor = false;
        Property field = getFieldFromAccessor(method);
        if (sBehaviorSignature == null && field != null)
            {
            fLastAccessor = true;
            Behavior[] accessors = field.getAccessors();
            for (int i = 0; i < accessors.length; ++i)
                {
                // It is the last mapped accessor, remove
                // any mapping against the field also.
                Behavior accessor = accessors[i];
                if (accessor != null && accessor != method)
                    {
                    if (getBehavior(accessor) != null)
                        {
                        fLastAccessor = false;
                        }
                    }
                }
            }

        // add/remove the method from the map.
        try
            {
            if (sBehaviorSignature == null)
                {
                if (fLastAccessor)
                    {
                    setProperty(field, null, false);
                    if (m_mapMethod.contains(sMethodSignature))
                        {
                        m_mapMethod.remove(sMethodSignature);
                        }
                    }
                else
                    {
                    m_mapMethod.remove(sMethodSignature);
                    }
                }
            else
                {
                m_mapMethod.put(sMethodSignature, sBehaviorSignature);
                }
            }
        catch (IllegalStringException e)
            {
            throw new WrapperException(e);
            }

        // Update the parent component to reflect the new behavior
        Component parent = (Component) getParentTrait();

        // Get the existing behavior that this method maps to (if any)
        Behavior behavior = null;
        if (sPrevBehaviorSignature != null)
            {
            behavior = parent.getBehavior(sPrevBehaviorSignature);
            }

        // Check if removing mapping
        if (sBehaviorSignature == null)
            {
            // Remove the current behavior if it exists
            if (behavior != null)
                {
                if (behavior.isFromIntegration())
                    {
                    behavior.removeOriginTrait(this);
                    if (behavior.isDiscardable())
                        {
                        parent.removeBehavior(behavior, false);
                        }
                    }
                }
            }
        else
            {
            String sBehaviorName  = sBehaviorSignature.substring(0, sBehaviorSignature.indexOf('('));
            // Check if rename or adding the behavior
            if (behavior == null)
                {
                // Check if a behavior already exists under that name
                behavior = parent.getBehavior(sBehaviorSignature);
                if (behavior == null)
                    {
                    // if no such Behavior signature already exists, create
                    // one based on the method
                    behavior = new Behavior(parent, this, sBehaviorName, method);
                    parent.addBehavior(behavior, false);
                    }
                else
                    {
                    // resolve any differences between the existing Behavior
                    // and the integrated method
                    behavior.setDeclaredByIntegration(this, method);
                    }
                }
            else
                {
                // We checked if we are renaming an existing behavior
                // that the new one does not exist.
                behavior.setName(sBehaviorName, false);
                }
            }

        // notify listeners
        firePropertyChange(ATTR_INTEGRATION_METHOD, asPrev, sBehaviorSignature);
        }

    // ----- fields/properties

    /**
    * Get all Java Class Signature final field names.  Some of the fields
    * may have been removed from the map; if so, the getProperty method
    * will return null for the field name.
    *
    * @return an enumeration of strings specifying each Java Class Signature
    *         final field name
    */
    public Enumeration getFields()
        {
        return new ChainedEnumerator(m_mapPrevField.primaryStrings(), m_mapField.primaryStrings());
        }

    /**
    * Get all Component Definition constant property names corresponding to
    * the Java Class Signature final field names.  Some of the property
    * names may be null meaning that the corresponding field was removed.
    *
    * @return an enumeration of strings specifying each Component Definition
    *         constant property name with an order corresponding to the
    *         enumeration returned from getFields
    */
    public Enumeration getProperties()
        {
        return new ChainedEnumerator(m_mapPrevField.secondaryStrings(), m_mapField.secondaryStrings());
        }

    /**
    * Determine the constant property name that corresponds to the specified
    * final field name.
    *
    * @param sField  the name of the Java Class Signature final field
    *                to look up
    *
    * @return a string specifying the corresponding constant property name or
    *         null if the specified field doesn't exists or has been removed
    */
    public String getProperty(String sField)
        {
        String sProperty = m_mapPrevField.get(sField);
        if (sProperty == null)
            {
            sProperty = m_mapField.get(sField);
            }
        return sProperty;
        }

    public String getProperty(Property field)
        {
        String sFieldSignature = getFieldSignature(field);

        // Check if an integrated Java Constant.
        Property property = (Property) m_tblProperties.get(sFieldSignature);
        if (property == null)
            {
            return getProperty(field.getName());
            }

        String sProperty = getSuggestedProperty(field);
        if (sProperty != null)
            {
            if (!isPropertyFromIntegration(sProperty))
                {
                sProperty = null;
                }
            }
        return sProperty;
        }

    public String getSuggestedProperty(Property field)
        {
        String sFieldSignature = getFieldSignature(field);

        // Check if an integrated Java Constant.
        Property property = (Property) m_tblProperties.get(sFieldSignature);
        if (property == null)
            {
            return getProperty(field.getName());
            }

        String sProperty = null;
        Behavior[] accessors = field.getAccessors();
        for (int i = 0; i < accessors.length; ++i)
            {
            Behavior accessor = accessors[i];
            if (accessor != null)
                {
                String sBehavior = getBehavior(accessor.getSignature());
                if (sBehavior != null)
                    {
                    sBehavior = sBehavior.substring(sBehavior.charAt(0) == 'i' ? 2 : 3,
                                                    sBehavior.indexOf('('));
                    if (sProperty == null)
                        {
                        sProperty = sBehavior;
                        }
                    else
                        {
                        if (!sBehavior.equals(sProperty))
                            {
                            return field.getName();
                            }
                        }
                    }
                }
            }
        return sProperty;
        }

    /**
    * Determine if the field exists in the integration map
    *
    * @param sField  the name of the Java Class Signature final field
    *                to look up
    *
    * @return true if the property is in the map, false otherwise
    */
    public boolean containsField(String sField)
        {
        boolean fContains = m_mapPrevField.contains(sField);
        if (!fContains)
            {
            fContains = m_mapField.contains(sField);
            }
        return fContains;
        }

    /**
    * Determine the field name that corresponds to the specified Component
    * Definition property.
    *
    * @param sProperty the name of the Component Definition constant property
    *                  to look up
    *
    * @return a string specifying the corresponding field name or
    *         null if the property name was not found
    */
    public String getField(String sProperty)
        {
        String sField = m_mapPrevField.getPrimary(sProperty);
        if (sField == null)
            {
            sField = m_mapField.getPrimary(sProperty);
            }
        return sField;
        }

    /**
    * Determine if the method was inherited from the super-class.
    *
    * @return true if it was inherited
    */
    public boolean isFieldInherited(Property field)
        {
        if (field.getExists() != EXISTS_INSERT)
            {
            return true;
            }
        Behavior[] accessors = field.getAccessors();
        for (int i = 0; i < accessors.length; ++i)
            {
            Behavior accessor = accessors[i];
            if (accessor != null)
                {
                if (accessor.getExists() != EXISTS_INSERT)
                    {
                    return true;
                    }
                }
            }
        return false;
        }

    /**
    * Determine if the field was inherited from our super-map
    *
    * @return true if it was inherited
    */
    public boolean isFieldPropertyInherited(Property field)
        {
        Property property = (Property) m_tblProperties.get(getFieldSignature(field));
        if (property == null)
            {
            return m_mapPrevField.contains(field.getName());
            }
        Behavior[] accessors = property.getAccessors();
        for (int i = 0; i < accessors.length; ++i)
            {
            Behavior accessor = accessors[i];
            if (accessor != null)
                {
                if (m_mapPrevMethod.contains(accessor.getSignature()))
                    {
                    return true;
                    }
                }
            }
        return false;
        }

    /**
    * Determine if it is okay to add mappped fields.
    *
    * @return true if okay to add mapped fields
    */
    public boolean isFieldSettable(Property field)
        {
        if (!isModifiable() || field == null)
            {
            return false;
            }

        // Make sure that this field looks like it belongs to us.
        String sFieldSignature = getFieldSignature(field);
        if (!m_tblProperties.contains(sFieldSignature))
            {
            return false;
            }

        // Check if this property was defined by the "super-map".
        if (isFieldPropertyInherited(field))
            {
            return false;
            }

        // Check if a "real" field in the JCS.
        if (m_tblProperties.get(sFieldSignature) == null)
            {
            // Make sure this field is really from the JCS.
            return m_jcs.getProperty(field.getName()) == field;
            }

        String sProperty = getProperty(field);
        if (sProperty == null)
            {
            // We are not already mapped, also check that all of
            // the accessors are settable and of a consistent mapping.
            Behavior[] accessors = field.getAccessors();
            for (int i = 0; i < accessors.length; ++i)
                {
                Behavior accessor = accessors[i];
                if (accessor != null)
                    {
                    if (!isMethodSettable(accessor))
                        {
                        return false;
                        }
                    // Check if the method a currently mapped.
                    String sBehavior = getBehavior(accessor.getSignature());
                    if (sBehavior != null)
                        {
                        // Generate the property name.
                        sBehavior = sBehavior.substring(sBehavior.charAt(0) == 'i' ? 2 : 3,
                                                        sBehavior.indexOf('('));
                        // Ensure that the mapped names are consistent.
                        if (sProperty == null)
                            {
                            sProperty = sBehavior;
                            }
                        else
                            {
                            if (!sBehavior.equals(sProperty))
                                {
                                return false;
                                }
                            }
                        }
                    }
                }
            }
        else
            {
            // We claim to be already mapped, this can also be the case as long
            // as one of our accessors are mapped.  Only allow unmapping if
            // we are defined as a property in the parent component.
            if (!isPropertyFromIntegration(sProperty))
                {
                return false;
                }
            }

        return true;
        }

    /**
    * Determine if it is okay to add mappped fields.
    *
    * @return true if okay to add mapped fields
    */
    public boolean isFieldRenamable(Property field)
        {
        return true;
        }

    /**
    * Determine whether the passed field and property names are legal.
    *
    * @param field      the field name to check
    * @param sProperty  the property name to check
    *
    * @return true if the specified values are okay
    *
    */
    public boolean isPropertyLegal(Property field, String sProperty)
        {
        if (field == null)
            {
            return false;
            }

        Component parent = (Component) getParentTrait();
        if (m_jcs == null)
            {
            return false;
            }

        String sFieldSignature = getFieldSignature(field);
        if (!m_tblProperties.contains(sFieldSignature))
            {
            return false;
            }

        // if a property is not being mapped to
        if (sProperty == null || sProperty.length() == 0)
            {
            // Verify that the properties' accessors don't
            // mind being unmapped.
            Behavior[] accessors = field.getAccessors();
            for (int i = 0; i < accessors.length; ++i)
                {
                Behavior accessor = accessors[i];
                if (accessor != null)
                    {
                    if (!isBehaviorLegal(accessors[i], null))
                        {
                        return false;
                        }
                    }
                }
            return true;
            }

        String sPrevProperty = getProperty(field);
        if (sPrevProperty != null && sPrevProperty.length() == 0)
            {
            sPrevProperty = null;
            }
        if (sProperty.equals(sPrevProperty))
            {
            return true;
           }

        // Property name must be a legal simple Java identifier
        if (!ClassHelper.isSimpleNameLegal(sProperty))
            {
            return false;
            }

        // Make sure the property is not already mapped to a different field
        String sPropertyField = getField(sProperty);
        if (sPropertyField != null)
            {
            return false;
            }

        // Get the current property if it exists
        Property property = null;
        if (sPrevProperty != null)
            {
            property = parent.getProperty(sPrevProperty);
            }
        // check if the field is already mapped to a property
        if (property != null)
            {
            // we are mapping an existing mapped property, first we
            // check if the name change is legal.
            if (!property.isNameLegal(sProperty))
                {
                return false;
                }
            }
        else
            {

            // Check if we are mapping to an existing property in the
            // parent component.
            property = parent.getProperty(sProperty);
            if (property == null)
                {

                // We are creating a brand new property in the parent component.

                DataType dt       = field.getDataType();
                boolean  fStatic  = field.isStatic();
                int      nIndexed = field.getIndexed();

                if (field.isJavaConstant())
                    {
                    int nAccess  = field.getAccess();
                    if (nAccess != ACCESS_PUBLIC && nAccess != ACCESS_PROTECTED)
                        {
                        return false;
                        }
                    if (!Property.isJavaConstantCreatable(
                            parent, dt, sProperty, nIndexed, fStatic, nAccess))
                        {
                        return false;
                        }
                    }
                else if (field.isCalculatedProperty())
                    {
                    if (!Property.isCalculatedPropertyCreatable(
                            parent, dt, sProperty, nIndexed, fStatic))
                        {
                        return false;
                        }
                    }
                else if (field.isFunctionalProperty())
                    {
                    if (!Property.isFunctionalPropertyCreatable(
                            parent, dt, sProperty, nIndexed, fStatic))
                        {
                        return false;
                        }
                    }
                else if (field.isStandardProperty())
                    {
                    if (!Property.isPropertyCreatable(
                            parent, dt, sProperty, nIndexed, fStatic, false))
                        {
                        return false;
                        }
                    }
                else
                    {
                    return false;
                    }

                }
            else
                {

                // We are mapping to an existing property in the parent component.
                // Certain Property attributes that must match cannot be
                // modified (fixed) if the Property has non-manual origin
                if (property.isFromNonManual())
                    {

                    // determine if the Property can be modified to match the
                    // integrated field
                    int nAccess = (field.isJavaConstant() ? field.getAccess() : ACCESS_PRIVATE);
                    if (property.getAccess() != nAccess)
                        {
                        return false;
                        }
                    if (property.isStatic() != field.isStatic())
                        {
                        return false;
                        }
                    if (property.isFinal() != field.isFinal())
                        {
                        return false;
                        }
                    if (property.isPersistent() != field.isPersistent())
                        {
                        return false;
                        }
                    if (property.getDirection() != field.getDirection())
                        {
                        return false;
                        }
                    if (property.getDataType() != field.getDataType())
                        {
                        return false;
                        }
                    if (property.getIndexed() != field.getIndexed())
                        {
                        return false;
                        }
                    // value must be NO_VALUE (or NO_DELTA)
                    if (!(property.isNoValue() || property.isNoDelta()))
                        {
                        return false;
                        }

                    }

                }

            // Verify that the properties' accessors also pass all of
            // our checks.

            Behavior[] accessors = field.getAccessors();
            String[] behaviors = getAccessorsBehaviors(accessors, sProperty, field.isBooleanGet());
            for (int i = 0; i < behaviors.length; ++i)
                {
                String behavior = behaviors[i];
                if (behavior != null)
                    {
                    if (!isBehaviorLegal(accessors[i], behavior))
                        {
                        return false;
                        }
                    }
                }

            }


        return true;
        }

    /**
    * Modify the property that maps to a particular field.
    *
    * @param field      the field being mapped
    * @param sProperty  the property to map the field to,
    *                   null unmapped the field
    *
    */
    public void setProperty(Property field, String sProperty)
            throws PropertyVetoException
        {
        setProperty(field, sProperty, true);
        }

    /**
    * Modify the property that maps to a particular field.
    *
    * @param field      the field being mapped
    * @param sProperty  the property to map the field to,
    *                   null unmapped the field
    * @param fVetoable  TODO
    *
    */
    protected synchronized void setProperty(Property field, String sProperty, boolean fVetoable)
            throws PropertyVetoException
        {
        if (sProperty != null && sProperty.length() == 0)
            {
            sProperty = null;
            }

        String sPrevProperty = getProperty(field);
        if (sPrevProperty != null && sPrevProperty.length() == 0)
            {
            sPrevProperty = null;
            }

        if (sPrevProperty == null)
            {
            if (sProperty == null)
                {
                return;
                }
            }
        else
            {
            if (sPrevProperty.equals(sProperty))
                {
                return;
                }
            }

        Object [] asPrev = new Object[]
            {
            field, sPrevProperty
            };

        if (fVetoable)
            {
            if (!isFieldSettable(field))
                {
                readOnlyAttribute(ATTR_INTEGRATION_FIELD, asPrev, sProperty);
                }

            // check if a field and property is legit
            if (!isPropertyLegal(field, sProperty))
                {
                illegalAttributeValue(ATTR_INTEGRATION_FIELD, asPrev, sProperty);
                }

            // notify veto listeners
            fireVetoableChange(ATTR_INTEGRATION_FIELD, asPrev, sProperty);
            }

        // add the property to the map if it is defined by the integrated
        // component as a field.
        try
            {
            // check if this is a "real" field being integrated
            if (m_tblProperties.get(getFieldSignature(field)) == null)
                {
                if (sProperty == null)
                    {
                    m_mapField.remove(field.getName());
                    }
                else
                    {
                    m_mapField.put(field.getName(), sProperty);
                    }
                }
            }
        catch (IllegalStringException e)
            {
            throw new WrapperException(e);
            }

        // Update the parent component to reflect the new property
        Component parent = (Component) getParentTrait();

        // Get the existing property that this field maps to (if any)
        Property property = null;
        if (sPrevProperty != null)
            {
            property = parent.getProperty(sPrevProperty);
            }

        // Check if removing mapping
        if (sProperty == null)
            {
            // Remove the current property if it exists
            if (property != null)
                {
                if (isPropertyFromIntegration(property))
                    {
                    property.removeOriginTrait(this);
                    parent.removeProperty(property, false);
                    }
                }
            // Remove all mapped accessors.
            Behavior[] accessors = field.getAccessors();
            for (int i = 0; i < accessors.length; ++i)
                {
                Behavior accessor = accessors[i];
                if (accessor != null)
                    {
                    setBehavior(accessor, null, false);
                    }
                }
            }
        else
            {
            // Check if rename or adding the property
            if (property == null)
                {
                // Check if a behavior already exists under that name
                property = parent.getProperty(sProperty);
                if (property == null)
                    {
                    // if no such Property already exists, create one based
                    // on the field
                    DataType dt       = field.getDataType();
                    int      nIndexed = field.getIndexed();
                    boolean  fStatic  = field.isStatic();
                    if (field.isJavaConstant())
                        {
                        property = Property.createJavaConstant(
                                parent, dt, sProperty, nIndexed, fStatic, field.getAccess());
                        }
                    else if (field.isCalculatedProperty())
                        {
                        property = Property.createCalculatedProperty(
                                parent, dt, sProperty, nIndexed, fStatic);
                        }
                    else if (field.isFunctionalProperty())
                        {
                        property = Property.createFunctionalProperty(
                                parent, dt, sProperty, nIndexed, fStatic);
                        }
                    else
                        {
                        property = Property.createProperty(
                                parent, dt, sProperty, nIndexed, fStatic, false);
                        }
                    parent.addProperty(property, false);
                    }
                // resolve any differences between the existing Property
                // and the integrated field
                property.setFromIntegration(this, field);
                // make sure accessors are available
                // let the property create the accessors and synch the
                // ones that already exist
                property.addAccessors();
                // TODO add "setFromIntegration" to Component.expandIntegrates
                // As a work around don't let extraction to remove the property
                property.setFromManual();
                }
            else
                {
                // We checked if we are renaming an existing behavior
                // that the new one does not exist.
                property.setName(sProperty, false);
                }

            // Now, deal with all of this properties' accessors

            Behavior[] accessors = field.getAccessors();
            String[] behaviors = getAccessorsBehaviors(accessors, sProperty, field.isBooleanGet());
            for (int i = 0; i < behaviors.length; ++i)
                {
                String behavior = behaviors[i];
                if (behavior != null)
                    {
                    setBehavior(accessors[i], behavior, false);
                    }
                }

            }

        // notify listeners
        firePropertyChange(ATTR_INTEGRATION_FIELD, asPrev, sProperty);
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Get the current Java Class Signature associated with this Integration Map
    *
    * @return The Java Class Signature
    */
    public String[] getImplements()
        {
        return m_tblImplements.strings();
        }

    public Interface getImplements(String sInterface)
        {
        if (!m_tblImplements.contains(sInterface))
            {
            return null;
            }
        return m_jcs.getImplements(sInterface);
        }

    public String[] getDispatches()
        {
        return m_tblDispatches.strings();
        }

    public Interface getDispatches(String sInterface)
        {
        if (!m_tblDispatches.contains(sInterface))
            {
            return null;
            }
        Interface iface = (Interface) m_tblDispatches.get(sInterface);
        if (iface == null)
            {
            iface = m_jcs.getImplements(sInterface);
            }
        return iface;
        }

    public String[] getMethodBehavior()
        {
        return m_tblBehaviors.strings();
        }

    public Behavior getMethodBehavior(String sMethod)
        {
        return m_jcs.getBehavior(sMethod);
        }

    public String[] getFieldProperty()
        {
        return m_tblProperties.strings();
        }

    public Property getFieldProperty(String sField)
        {
        if (!m_tblProperties.contains(sField))
            {
            return null;
            }
        Property property = (Property) m_tblProperties.get(sField);
        if (property == null)
            {
            property = m_jcs.getProperty(sField.substring(0,sField.indexOf('.')));
            }
        return property;
        }


    /**
    * TODO
    */
    public void clearJcs()
        {
        m_jcs           = null;
        m_tblImplements = null;
        m_tblDispatches = null;
        m_tblBehaviors  = null;
        m_tblProperties = null;
        }

    /**
    * TODO
    */
    public void loadJcs(Loader loader)
            throws ComponentException
        {
        clearJcs();

        // gg: 2005.01.26  when super component removes Integration
        // resolve does not remove it causing NPE; working around
        // by making an empty Integration valid 
        m_tblImplements = new StringTable();
        m_tblDispatches = new StringTable();
        m_tblBehaviors  = new StringTable();
        m_tblProperties = new StringTable();

        if (m_sSignature == BLANK)
            {
            return;
            }

        m_jcs = loader.loadSignature(m_sSignature);
        if (m_jcs == null)
            {
            return;
            }

        // Put all of the methods in the behaviors table for now, we
        // will later remove those that correspond to properties and
        // dispatches.

        String[] as = m_jcs.getBehavior();
        for (int i = 0; i < as.length; ++i)
            {
            String sMethod = as[i];
            if (!sMethod.startsWith("<init>"))
                {
                m_tblBehaviors.add(as[i]);
                }
            }

        // First go through the jcs's interfaces and organize between dispatches and
        // interfaces.

        //as = m_jcs.getImplements();
        //for (int i = 0; i < as.length; ++i)
        //    {
        //    String sImplements = as[i];
        //    if (isEventListener(sImplements, loader))
        //        {
        //        m_tblDispatches.add(sImplements);
        //        }
        //    else
        //        {
        //        m_tblImplements.add(sImplements);
        //        }
        //    for (Enumeration enmr = m_jcs.getImplements(sImplements).getBehaviors();
        //        enmr.hasMoreElements();)
        //        {
        //        m_tblBehaviors.remove((String) enmr.nextElement());
        //        }
        //    }

        // Next set up all the fields as properties.

        as = m_jcs.getProperty();
        for (int i = 0; i < as.length; ++i)
            {
            m_tblProperties.add(getFieldSignature(m_jcs.getProperty(as[i])));
            }

        // Now, sort the methods into appropriate categories.

        as = m_jcs.getBehavior();
        for (int i = 0; i < as.length; ++i)
            {
            Behavior method = m_jcs.getBehavior(as[i]);

            // parse a verb and noun from the method name

            String sVerb = method.getName();
            String sNoun = null;
            for (int ii = 0; ii < sVerb.length(); ++ii)
                {
                if (!Character.isLowerCase(sVerb.charAt(ii)))
                    {
                    sNoun = sVerb.substring(ii);
                    sVerb = sVerb.substring(0,ii);
                    break;
                    }
                }
            if (sNoun == null)
                {
                continue;
                }

            DataType dataType = method.getReturnValue().getDataType();
            if (sVerb.equals("is") || sVerb.equals("get"))
                {
                switch (method.getParameterCount())
                    {
                    case 0:
                        setupProperty(sNoun, dataType);
                        break;
                    case 1:
                        if (method.getParameter(0).getDataType() == DataType.INT)
                            {
                            setupProperty(sNoun, dataType.getArrayType());
                            }
                        break;
                    }
                }
            else if (dataType == DataType.VOID && sVerb.equals("set"))
                {
                switch (method.getParameterCount())
                    {
                    case 1:
                        setupProperty(sNoun, method.getParameter(0).getDataType());
                        break;
                    case 2:
                        if (method.getParameter(0).getDataType() == DataType.INT)
                            {
                            setupProperty(sNoun, method.getParameter(1).getDataType().getArrayType());
                            }
                        break;
                    }
                }
            else if (dataType == DataType.VOID
                     && (sVerb.equals("add") || sVerb.equals("remove"))
                     && sNoun.endsWith("Listener")
                     && method.getParameterCount() == 1)
                {
                dataType = method.getParameter(0).getDataType();
                if (dataType.isClass()
                    && isEventListener(dataType.getClassName(), loader))
                    {
                    setupDispatches(dataType);
                    }
                }

            }

        }

    private boolean isEventListener(String sClass, Loader loader)
        {
        if (sClass.equals("java.util.EventListener"))
            {
            return true;
            }
        try
            {
            Component component = loader.loadSignature(sClass);
            if (component != null)
                {
                String[] asImplements = component.getImplements();
                for (int i = 0; i < asImplements.length; ++i)
                    {
                    if (isEventListener(asImplements[i], loader))
                        {
                        return true;
                        }
                    }
                }
            }
        catch (ComponentException ce)
            {
            }
        return false;
        }

    private String getFieldSignature(Property field)
        {
        DataType dataType = field.getDataType();
        if (!field.isSingle())
            {
            dataType = dataType.getArrayType();
            }
        return field.getName() + '.' + dataType.getTypeString();
        }

    private String[] getAccessorsBehaviors(Behavior[] accessors, String sProperty, boolean fBooleanGet)
        {
        // First determine if we are dealing with all of
        // the accessors, or a sub-set.
        boolean fAllSet   = true;
        boolean fAllClear = true;
        if (sProperty != null)
            {
            for (int i = 0; i < accessors.length; ++i)
                {
                Behavior accessor = accessors[i];
                if (accessor != null)
                    {
                    if (getBehavior(accessor.getSignature()) == null)
                        {
                        fAllSet = false;
                        }
                    else
                        {
                        fAllClear = false;
                        }
                    }
                }
            }

        String[] behaviors = new String[accessors.length];
        for (int i = 0; i < accessors.length; ++i)
            {
            behaviors[i] = null;
            Behavior accessor = accessors[i];
            if (accessor != null)
                {
                String sMethod = accessor.getSignature();
                if (fAllClear || fAllSet || getBehavior(sMethod) != null)
                    {
                    String sPrefix;
                    switch (sMethod.charAt(0))
                        {
                        case 'g':
                            if (!fBooleanGet)
                                {
                                sPrefix = "get";
                                break;
                                }
                            // drop into 'i'
                        case 'i':
                            sPrefix = "is";
                            break;
                        default:
                            sPrefix = "set";
                            break;
                        }
                    behaviors[i] = sPrefix + sProperty + sMethod.substring(sMethod.indexOf('('));
                    }
                }
            }
        return behaviors;
        }

    private void setupProperty(String sProperty, DataType dataType)
        {
        String sPropertySignature = sProperty + '.' + dataType.getTypeString();
        // Check if this property has already been setup.
        if (m_tblProperties.contains(sPropertySignature))
            {
            return;
            }
        boolean fArray = dataType.isArray();
        DataType elementType = (fArray ? dataType.getElementType() : dataType);

        boolean  fBoolean    = ((dataType.isArray()
                                 ? dataType.getElementType()
                                 : dataType) == DataType.BOOLEAN);

        String   sOut        = fBoolean ? "is" : "get";
        String   sSingleOut  = sOut + sProperty + "()";
        Behavior method      = m_jcs.getBehavior(sSingleOut);
        if (method == null || method.getReturnValue().getDataType() != dataType)
            {
            if (fBoolean)
                {
                sOut       = "get";
                sSingleOut = sOut + sProperty + "()";
                method     = m_jcs.getBehavior(sSingleOut);
                if (method == null || method.getReturnValue().getDataType() != dataType)
                    {
                    sOut       = "is";
                    sSingleOut = null;
                    }
                }
            else
                {
                sSingleOut = null;
                }
            }
        String sSingleIn = "set" + sProperty + '(' + dataType.getTypeString() + ')';
        method = m_jcs.getBehavior(sSingleIn);
        if (method == null || method.getReturnValue().getDataType() != DataType.VOID)
            {
            sSingleIn = null;
            }
        String sIndexedOut = null;
        String sIndexedIn  = null;
        if (fArray)
            {
            sIndexedOut = sOut + sProperty + "(I)";
            method      = m_jcs.getBehavior(sIndexedOut);
            if (method == null || method.getReturnValue().getDataType() != elementType)
                {
                if (fBoolean && sSingleOut == null)
                    {
                    sOut        = "get";
                    sIndexedOut = sOut + sProperty + "(I)";
                    method      = m_jcs.getBehavior(sIndexedOut);
                    if (method == null || method.getReturnValue().getDataType() != elementType)
                        {
                        sOut        = "is";
                        sIndexedOut = null;
                        }
                    }
                }
            sIndexedIn = "set" + sProperty + "(I" + elementType.getTypeString() + ')';
            method = m_jcs.getBehavior(sIndexedIn);
            if (method == null || method.getReturnValue().getDataType() != DataType.VOID)
                {
                sIndexedIn = null;
                }
            }

        int iType = 0;
        int nProp = 0;
        if (sSingleOut != null && sSingleIn != null)
            {
            iType = 1; // standard property
            if (sIndexedOut == null || sIndexedIn == null)
                {
                nProp = PROP_SINGLE;
                sIndexedOut = null;
                sIndexedIn  = null;
                }
            else
                {
                nProp = PROP_INDEXED;
                }
            }
        else if (sIndexedOut != null && sIndexedIn != null)
            {
            iType = 1; // standard property
            nProp = PROP_INDEXEDONLY;
            sSingleOut = null;
            sSingleIn  = null;
            }
        else if (sSingleOut != null)
            {
            if (sIndexedIn == null)
                {
                iType = 2; // calculated property
                if (sIndexedOut == null)
                    {
                    nProp = PROP_SINGLE;
                    }
                else
                    {
                    nProp = PROP_INDEXED;
                    }
                }
            }
        else if (sSingleIn != null)
            {
            if (sIndexedOut == null)
                {
                iType = 3; // functional property
                if (sIndexedIn == null)
                    {
                    nProp = PROP_SINGLE;
                    }
                else
                    {
                    nProp = PROP_INDEXED;
                    }
                }
            }
        else if (sIndexedOut != null)
            {
            iType = 2; // calculated property
            nProp = PROP_INDEXEDONLY;
            }
        else if (sIndexedIn != null)
            {
            iType = 3; // functional property
            nProp = PROP_INDEXEDONLY;
            }

        if (nProp == PROP_SINGLE && fArray)
            {
            elementType = dataType;
            }

        Property property = null;
        switch (iType)
            {
            case 1:
                property = Property.createProperty(m_jcs, elementType, sProperty, nProp, false, false);
                break;
            case 2:
                property = Property.createCalculatedProperty(m_jcs, elementType, sProperty, nProp, false);
                break;
            case 3:
                property = Property.createFunctionalProperty(m_jcs, elementType, sProperty, nProp, false);
                break;
            }

        if (property != null)
            {
            if (fBoolean && sOut.equals("get"))
                {
                property.setBooleanGet(true);
                }
            m_tblProperties.put(sPropertySignature, property);
            if (sSingleOut != null)
                {
                m_tblBehaviors.remove(sSingleOut);
                }
            if (sSingleIn != null)
                {
                m_tblBehaviors.remove(sSingleIn);
                }
            if (sIndexedOut != null)
                {
                m_tblBehaviors.remove(sIndexedOut);
                }
            if (sIndexedIn != null)
                {
                m_tblBehaviors.remove(sIndexedIn);
                }
            }
        }

    private boolean isPropertyFromIntegration(String sProperty)
        {
        Property property = ((Component) getParentTrait()).getProperty(sProperty);
        if (property == null)
            {
            return false;
            }
        return isPropertyFromIntegration(property);
        }

    private boolean isPropertyFromIntegration(Property property)
        {
        if (property.isFromIntegration())
            {
            return true;
            }
        Behavior[] accessors = property.getAccessors();
        for (int i = 0; i < accessors.length; ++i)
            {
            Behavior accessor = accessors[i];
            if (accessor != null && accessor.isFromIntegration())
                {
                return true;
                }
            }
        return false;
        }

    private void setupDispatches(DataType dataType)
        {
        String className = dataType.getClassName();
        // Check if already setup.
        if (m_tblDispatches.get(className) != null)
            {
            return;
            }
        String name = className.substring(className.lastIndexOf('.') + 1) + "(L" + className + ";)";
        String sAdd = "add" + name;
        Behavior method = m_jcs.getBehavior(sAdd);
        if (method == null
            || method.getReturnValue().getDataType() != DataType.VOID
            || method.getParameterCount() != 1
            || method.getParameter(0).getDataType() != dataType)
            {
            return;
            }
        String sRemove = "remove" + name;
        method = m_jcs.getBehavior(sRemove);
        if (method == null
            || method.getReturnValue().getDataType() != DataType.VOID
            || method.getParameterCount() != 1
            || method.getParameter(0).getDataType() != dataType)
            {
            return;
            }
        Enumeration enmrBehaviors;
        Enumeration enmrProperties;
        Interface iface = m_jcs.getImplements(className);
        if (iface != null)
            {
            enmrBehaviors  = iface.getBehaviors();
            enmrProperties = iface.getProperties();
            }
        else
            {
            enmrBehaviors  = NullImplementation.getEnumeration();
            enmrProperties = NullImplementation.getEnumeration();
            }
        iface = new Interface(m_jcs,
                              className,
                              Interface.DISPATCHES,
                              new ChainedEnumerator(enmrBehaviors,
                                                    new SimpleEnumerator(new Object[] {sAdd, sRemove})),
                              enmrProperties);
        m_tblDispatches.put(className, iface);
        m_tblBehaviors.remove(sAdd);
        m_tblBehaviors.remove(sRemove);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compares this Integration map to another Object for equality.
    *
    * @param obj  the other Object to compare to this
    *
    * @return true if this Component equals that Object
    */
    public boolean equals(Object obj)
        {
        if (!(obj instanceof Integration))
            {
            return false;
            }

        Integration that = (Integration) obj;
        if (this == that)
            {
            return true;
            }

        return super.equals(that)
            && this.m_sPrevSignature.equals(that.m_sPrevSignature)
            && this.m_sSignature.equals(that.m_sSignature)
            && this.m_sPrevModel.equals(that.m_sPrevModel)
            && this.m_sModel.equals(that.m_sModel)
            && this.m_sPrevMisc.equals(that.m_sPrevMisc)
            && this.m_sMisc.equals(that.m_sMisc)
            && this.m_mapMethod.equals(that.m_mapMethod)
            && this.m_mapField.equals(that.m_mapField)
            ;
        }

    /**
    * Reset state, discarding all information.
    */
    protected synchronized void invalidate()
        {
        super.invalidate();

        m_sPrevSignature = null;
        m_sSignature     = null;
        m_sPrevModel     = null;
        m_sModel         = null;
        m_sPrevMisc      = null;
        m_sMisc          = null;
        m_mapPrevMethod  = null;
        m_mapMethod      = null;
        m_mapPrevField   = null;
        m_mapField       = null;
        m_jcs            = null;
        }

    // ----- debug methods --------------------------------------------------

    /**
    * Provide the entire set of Integration information in a printed format.
    */
    public void dump()
        {
        dump(getOut(), BLANK);
        }
    /**
    * Provide the entire set of Integration information in a printed format.
    *
    * @param out      PrintWriter object to dump the information to
    * @param sIndent  a string used to indent each line of dumped information
    */
    public void dump(PrintWriter out, String sIndent)
        {
        dump(out, sIndent, true);
        }

    /**
    * Provide the entire set of Integration information in a printed format.
    *
    * @param out      PrintWriter object to dump the information to
    * @param sIndent  a string used to indent each line of dumped information
    * @param fIndentFirstLine  false to suppress first line indention
    */
    public void dump(PrintWriter out, String sIndent, boolean fIndentFirstLine)
        {
        if (fIndentFirstLine)
            {
            out.print(sIndent);
            }
        out.println("PrevSignature=" + m_sPrevSignature );
        out.println(sIndent + "Signature="     + m_sSignature );
        out.println(sIndent + "PrevModel="     + m_sPrevModel);
        out.println(sIndent + "Model="         + m_sModel);
        out.println(sIndent + "PrevMisc="      + m_sPrevMisc);
        out.println(sIndent + "Misc="          + m_sMisc);
        // trait attributes
        super.dump(out, sIndent);
        out();
        dump(out, sIndent + "  ", m_mapPrevMethod, m_mapMethod, "Java Method", "Behavior");
        out();
        dump(out, sIndent + "  ", m_mapPrevField , m_mapField,  "Java Field",  "Property");
        }

    /**
    * Provide the entire set of Integration information in a printed format.
    *
    * @param out      PrintWriter object to dump the information to
    * @param sIndent  a string used to indent each line of dumped information
    * @param map      the StringMap
    * @param sCol1    heading for primary strings
    * @param sCol2    heading for secondary strings
    */
    private void dump(PrintWriter out, String sIndent, StringMap mapPrev, StringMap map, String sCol1, String sCol2)
        {
        // determine longest string in each column
        Enumeration enmr1 = new ChainedEnumerator(mapPrev.primaryStrings(),   map.primaryStrings());
        Enumeration enmr2 = new ChainedEnumerator(mapPrev.secondaryStrings(), map.secondaryStrings());
        int cch1 = sCol1.length();
        int cch2 = sCol2.length();
        try
            {
            while (true)
                {
                String s1 = (String) enmr1.nextElement();
                if (s1.length() > cch1)
                    {
                    cch1 = s1.length();
                    }

                String s2 = (String) enmr2.nextElement();
                if (s2 == null)
                    {
                    s2 = NULL_STRING;
                    }
                else if (s2 == s1)
                    {
                    s2 = EQUAL_STRING;
                    }
                if (s2.length() > cch2)
                    {
                    cch2 = s2.length();
                    }
                }
            }
        catch (NoSuchElementException e)
            {
            }

        // build dividers to place below headers
        int    cchMax = (cch1 > cch2 ? cch1 : cch2);
        char[] achSpc = new char[cchMax];
        char[] achDiv = new char[cchMax];
        for (int i = 0; i < cchMax; ++i)
            {
            achSpc[i] = ' ';
            achDiv[i] = '-';
            }
        String sSpc  = new String(achSpc);
        String sDiv  = new String(achDiv);
        String sDiv1 = sDiv.substring(0, cch1);
        String sDiv2 = sDiv.substring(0, cch2);

        // dump strings
        out.println(sIndent + (sCol1 + sSpc).substring(0, cch1)
                    + "  " + (sCol2 + sSpc).substring(0, cch2)
                    + "  Type");
        out.println(sIndent + sDiv1 + "  " + sDiv2 + "  ---------");

        enmr1 = new ChainedEnumerator(mapPrev.primaryStrings(),   map.primaryStrings());
        enmr2 = new ChainedEnumerator(mapPrev.secondaryStrings(), map.secondaryStrings());
        try
            {
            while (true)
                {
                String s1 = (String) enmr1.nextElement();
                String s2 = (String) enmr2.nextElement();
                if (s2 == null)
                    {
                    s2 = NULL_STRING;
                    }
                else if (s2 == s1)
                    {
                    s2 = EQUAL_STRING;
                    }
                out.println(sIndent + (s1 + sSpc).substring(0, cch1)
                            + "  " + (s2 + sSpc).substring(0, cch2)
                            + "  " + (mapPrev.contains(s1) ? "Inherited" : "Local"));
                }
            }
        catch (NoSuchElementException e)
            {
            }

        }

    // ----- data members ---------------------------------------------------

    /**
    * The map name.
    */
    public static final String ATTR_INTEGRATION_SIGNATURE = "IntegrationSignature";

    /**
    * The Model attribute.
    */
    public static final String ATTR_INTEGRATION_MODEL = "IntegrationModel";

    /**
    * The Behavior attribute.
    */
    public static final String ATTR_INTEGRATION_METHOD = "IntegrationMethod";

    /**
    * The Property attribute.
    */
    public static final String ATTR_INTEGRATION_FIELD = "IntegrationField";

    /**
    * The name of this class.
    */
    private static final String CLASS = "Integration";

    /**
    * The Parameter's descriptor string.
    */
    protected static final String DESCRIPTOR = CLASS;

    /**
    * The printed representation of a null string.
    */
    private static final String NULL_STRING = "<null>";

    /**
    * The printed representation of an equal string.
    */
    private static final String EQUAL_STRING = "<same>";

    /**
    * The name of the Java Class Signature being integrated.
    */
    private String m_sPrevSignature;
    private String m_sSignature;

    /**
    * The name of the Integration Model.  This name is used during Component
    * Compilation to determine the class responsible for building the
    * integration interfaces and classes.
    */
    private String m_sPrevModel;
    private String m_sModel;

    /**
    * Model-defined miscellaneous information used in the build process.
    */
    private String m_sPrevMisc;
    private String m_sMisc;

    /**
    * A map between the signatures of Java Class Signature methods and
    * Component Definition behaviors.
    */
    private StringMap m_mapPrevMethod;
    private StringMap m_mapMethod;

    /**
    * A map between the names of Java Class Signature final fields and
    * Component Definition constant properties.
    */
    private StringMap m_mapPrevField;
    private StringMap m_mapField;

    /**
    * The Java Class Signature that this maps against.  This field is
    * set by the UI tool during modification and is only used to very
    * the validity of methods and fields being mapped.
    */
    private Component m_jcs;

    private StringTable m_tblImplements;
    private StringTable m_tblDispatches;
    private StringTable m_tblBehaviors;
    private StringTable m_tblProperties;

    }
