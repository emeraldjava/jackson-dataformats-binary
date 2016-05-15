package com.fasterxml.jackson.dataformat.smile.gen;

import java.io.*;

import org.junit.Assert;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.dataformat.smile.BaseTestForSmile;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator.Feature;
import com.fasterxml.jackson.dataformat.smile.testutil.ThrottledInputStream;

public class TestSmileGeneratorBinary extends BaseTestForSmile
{
    
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testStreamingBinary() throws Exception
    {
        _testStreamingBinary(true);
        _testStreamingBinary(false);
    }

    public void testBinaryWithoutLength() throws Exception
    {
        final SmileFactory f = new SmileFactory();
        JsonGenerator jg = f.createGenerator(new ByteArrayOutputStream());
        try {
            jg.writeBinary(new ByteArrayInputStream(new byte[1]), -1);
            fail("Should have failed");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "must pass actual length");
        }
        jg.close();
    }
    
    public void testStreamingBinaryPartly() throws Exception {
    	_testStreamingBinaryPartly(true);
    	_testStreamingBinaryPartly(false);
    }
    
    private void _testStreamingBinaryPartly(boolean rawBinary) throws Exception
    {
    	final SmileFactory f = new SmileFactory();
    	f.configure(Feature.ENCODE_BINARY_AS_7BIT, rawBinary);
    	
    	final byte[] INPUT = TEXT4.getBytes("UTF-8");
    	ByteArrayInputStream in = new ByteArrayInputStream(INPUT);
    	
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	JsonGenerator jg = f.createGenerator(out);
    	jg.writeStartArray();
    	jg.writeBinary(in, 1);
    	jg.writeEndArray();
    	jg.close();
    	in.close();
    	
        JsonParser jp = f.createParser(out.toByteArray());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, jp.nextToken());
        byte[] b = jp.getBinaryValue();
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
    	
    	assertEquals(1, b.length);
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private final static String TEXT = "Some content so that we can test encoding of base64 data; must"
            +" be long enough include a line wrap or two...";
    private final static String TEXT4 = TEXT + TEXT + TEXT + TEXT;

    private void _testStreamingBinary(boolean rawBinary) throws Exception
    {
        final SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.ENCODE_BINARY_AS_7BIT, !rawBinary);
        
        final byte[] INPUT = TEXT4.getBytes("UTF-8");
        for (int chunkSize : new int[] { 1, 2, 3, 4, 7, 11, 29, 5000 }) {
            JsonGenerator jgen;
            
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            jgen = f.createGenerator(bytes);
            jgen.writeStartArray();
            InputStream data = new ThrottledInputStream(INPUT, chunkSize);
            jgen.writeBinary(data, INPUT.length);
            jgen.writeEndArray();
            jgen.close();
            jgen.close();
            data.close();

            JsonParser jp = f.createParser(bytes.toByteArray());
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, jp.nextToken());
            byte[] b = jp.getBinaryValue();
            Assert.assertArrayEquals(INPUT, b);
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            jp.close();
        }
    }
}
