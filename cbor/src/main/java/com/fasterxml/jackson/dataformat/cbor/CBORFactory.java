package com.fasterxml.jackson.dataformat.cbor;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.BinaryTSFactory;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;

/**
 * Factory used for constructing {@link CBORParser} and {@link CBORGenerator}
 * instances; both of which handle
 * <a href="https://www.rfc-editor.org/info/rfc7049">CBOR</a>
 * encoded data.
 *<p>
 * Extends {@link JsonFactory} mostly so that users can actually use it in place
 * of regular non-CBOR factory instances.
 *<p>
 * Note on using non-byte-based sources/targets (char based, like
 * {@link java.io.Reader} and {@link java.io.Writer}): these can not be
 * used for CBOR documents; attempt will throw exception.
 * 
 * @author Tatu Saloranta
 */
public class CBORFactory
    extends BinaryTSFactory
    implements java.io.Serializable
{
	private static final long serialVersionUID = 1; // 2.6

    /*
    /**********************************************************
    /* Constants
    /**********************************************************
     */

	/**
     * Name used to identify CBOR format.
     * (and returned by {@link #getFormatName()}
     */
    public final static String FORMAT_NAME = "CBOR";
    
    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    final static int DEFAULT_CBOR_PARSER_FEATURE_FLAGS = CBORParser.Feature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    final static int DEFAULT_CBOR_GENERATOR_FEATURE_FLAGS = CBORGenerator.Feature.collectDefaults();

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    protected int _formatParserFeatures;
    protected int _formatGeneratorFeatures;

    /*
    /**********************************************************
    /* Symbol table management
    /**********************************************************
     */

    /**
     * Alternative to the basic symbol table, some stream-based
     * parsers use different name canonicalization method.
     */
    protected final transient ByteQuadsCanonicalizer _byteSymbolCanonicalizer = ByteQuadsCanonicalizer.createRoot();

    /*
    /**********************************************************
    /* Factory construction, configuration
    /**********************************************************
     */

    /**
     * Default constructor used to create factory instances.
     * Creation of a factory instance is a light-weight operation,
     * but it is still a good idea to reuse limited number of
     * factory instances (and quite often just a single instance):
     * factories are used as context for storing some reused
     * processing objects (such as symbol tables parsers use)
     * and this reuse only works within context of a single
     * factory instance.
     */
    public CBORFactory() { this(null); }

    public CBORFactory(ObjectCodec oc) {
        super(oc);
        _formatParserFeatures = DEFAULT_CBOR_PARSER_FEATURE_FLAGS;
        _formatGeneratorFeatures = DEFAULT_CBOR_GENERATOR_FEATURE_FLAGS;
    }

    public CBORFactory(CBORFactory src, ObjectCodec oc)
    {
        super(src, oc);
        _formatParserFeatures = src._formatParserFeatures;
        _formatGeneratorFeatures = src._formatGeneratorFeatures;
    }

    @Override
    public CBORFactory copy()
    {
        // note: as with base class, must NOT copy mapper reference
        return new CBORFactory(this, null);
    }

    /*
    /**********************************************************
    /* Serializable overrides
    /**********************************************************
     */

    /**
     * Method that we need to override to actually make restoration go
     * through constructors etc.
     * Also: must be overridden by sub-classes as well.
     */
    protected Object readResolve() {
        return new CBORFactory(this, _objectCodec);
    }

    /*                                                                                       
    /**********************************************************                              
    /* Capability introspection                                                                     
    /**********************************************************                              
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public boolean canParseAsync() {
        // 30-Sep-2017, tatu: Not yet, although eminently implementable
        return false;
    }

    @Override
    public boolean canUseCharArrays() { return false; }

    /*
    /**********************************************************
    /* Data format support
    /**********************************************************
     */

    @Override
    public String getFormatName() {
        return FORMAT_NAME;
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return false; // no (mandatory) FormatSchema for cbor
    }

    /*
    /**********************************************************
    /* Capability introspection
    /**********************************************************
     */

    @Override
    public boolean canHandleBinaryNatively() {
        return true;
    }

    @Override // since 2.6
    public Class<CBORParser.Feature> getFormatReadFeatureType() {
        return CBORParser.Feature.class;
    }

    @Override // since 2.6
    public Class<CBORGenerator.Feature> getFormatWriteFeatureType() {
        return CBORGenerator.Feature.class;
    }
    
    /*
    /**********************************************************
    /* Configuration, parser settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link CBORParser.Feature} for list of features)
     */
    public final CBORFactory configure(CBORParser.Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /**
     * Method for enabling specified parser feature
     * (check {@link CBORParser.Feature} for list of features)
     */
    public CBORFactory enable(CBORParser.Feature f) {
        _formatParserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link CBORParser.Feature} for list of features)
     */
    public CBORFactory disable(CBORParser.Feature f) {
        _formatParserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(CBORParser.Feature f) {
        return (_formatParserFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************
    /* Configuration, generator settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified generator feature
     * (check {@link CBORGenerator.Feature} for list of features)
     */
    public final CBORFactory configure(CBORGenerator.Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }


    /**
     * Method for enabling specified generator features
     * (check {@link CBORGenerator.Feature} for list of features)
     */
    public CBORFactory enable(CBORGenerator.Feature f) {
        _formatGeneratorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified generator feature
     * (check {@link CBORGenerator.Feature} for list of features)
     */
    public CBORFactory disable(CBORGenerator.Feature f) {
        _formatGeneratorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public final boolean isEnabled(CBORGenerator.Feature f) {
        return (_formatGeneratorFeatures & f.getMask()) != 0;
    }

    /*
    /******************************************************
    /* Parser factory methods
    /******************************************************
     */

    @Override
    protected IOContext _createContext(Object srcRef, boolean resourceManaged) {
        return super._createContext(srcRef, resourceManaged);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected CBORParser _createParser(InputStream in, IOContext ctxt) throws IOException
    {
        return new CBORParserBootstrapper(ctxt, in).constructParser(_factoryFeatures,
                _parserFeatures, _formatParserFeatures,
                _objectCodec, _byteSymbolCanonicalizer);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected CBORParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException
    {
        return new CBORParserBootstrapper(ctxt, data, offset, len).constructParser(
                _factoryFeatures, _parserFeatures, _formatParserFeatures,
                _objectCodec, _byteSymbolCanonicalizer);
    }

    @Override
    protected JsonParser _createParser(DataInput input, IOContext ctxt)
            throws IOException {
        // 30-Sep-2017, tatu: As of now not supported although should be quite possible
        //    to support
        return _unsupported();
    }

    /*
    /******************************************************
    /* Generator factory methods
    /******************************************************
     */
    
    @Override
    protected CBORGenerator _createGenerator(OutputStream out, IOContext ctxt) throws IOException {
        return _createCBORGenerator(ctxt,
                _generatorFeatures, _formatGeneratorFeatures, _objectCodec, out);
    }

    private final CBORGenerator _createCBORGenerator(IOContext ctxt,
            int stdFeat, int formatFeat, ObjectCodec codec, OutputStream out) throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        CBORGenerator gen = new CBORGenerator(ctxt, stdFeat, formatFeat, _objectCodec, out);
        if (CBORGenerator.Feature.WRITE_TYPE_HEADER.enabledIn(formatFeat)) {
            gen.writeTag(CBORConstants.TAG_ID_SELF_DESCRIBE);
        }
        return gen;
    }
}
