package com.aspect.snoop.plugin.jeb;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * LEDataInputStream.java copyright (c) 1998-2006 Roedy Green, Canadian Mind
 * Products #327 - 964 Heywood Avenue Victoria, BC Canada V8V 2Y5 tel: (250)
 * 361-9093 mailto:roedyg@mindprod.com http://mindprod.com Version 1.0 1998
 * January 6 1.1 1998 January 7 - officially implements DataInput 1.2 1998
 * January 9 - add LERandomAccessFile 1.3 1998 August 27 - fix bug, readFully
 * instead of read. 1.4 1998 November 10 - add address and phone. 1.5 1999
 * October 8 - use com.mindprod.ledatastream package name. 1.6 2005-06-13 - made
 * readLine deprecated Very similar to DataInputStream except it reads
 * little-endian instead of big-endian binary data. We can't extend
 * DataInputStream directly since it has only final methods, though
 * DataInputStream itself is not final. This forces us implement
 * LEDataInputStream with a DataInputStream object, and use wrapper methods.
 */

public class LEDataInputStream implements DataInput {

    /** work array for buffering input */
    private final byte[] w;

    /** to get at the big-Endian methods of a basic DataInputStream */
    protected DataInputStream d;

    /** to get at the a basic readBytes method */
    protected InputStream in;

    public final void close() throws IOException {
        d.close();
    }

    // InputStream

    // p u r e l y w r a p p e r m e t h o d s
    // We can't simply inherit since dataInputStream is final.
    /**
     * Watch out, read may return fewer bytes than requested.
     *
     * @param b
     *        where the bytes go
     * @param off
     *        offset in buffer, not offset in file.
     * @param len
     *        count of bytes ot ade
     * @return how many bytes read
     */
    public final int read(byte b[], int off, int len) throws IOException {
        // For efficiency, we avoid one layer of wrapper
        return in.read(b, off, len);
    }

    /**
     * only reads on byte
     *
     * @see DataInput#readBoolean()
     */
    public final boolean readBoolean() throws IOException {
        d.readFully(w, 0, 4);
        return (w[0] == 0x01);
    }

    /**
     * @inheritDoc
     * @see DataInput#readByte()
     */
    public final byte readByte() throws IOException {
        return d.readByte();
    }

    /**
     * like DataInputStream.readChar except little endian.
     *
     * @return little endian 16-bit unicode char from the stream
     */
    public final char readChar() throws IOException {
        d.readFully(w, 0, 2);
        return (char) ((w[1] & 0xff) << 8 | (w[0] & 0xff));
    }

    static final String charset = "UTF-8";

    public final String readString() throws IOException {
        int len = d.read();
        //System.out.println("len:"+len);

        byte b[] = new byte[len];
        d.readFully(b);
        return new String(b, charset);
    }

    public final String readGBKString() throws IOException {
        int len = d.read();

        byte b[] = new byte[len];
        d.readFully(b);
        return new String(b, "GBK");
    }

    public final int read() throws IOException {
        return d.read();
    }

    /**
     * like DataInputStream.readDouble except little endian.
     *
     * @return little endian IEEE double from the datastream
     */
    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * like DataInputStream.readFloat except little endian.
     *
     * @return little endian IEEE float from the datastream
     */
    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * @inheritDoc
     * @see DataInput#readFully(byte[])
     */
    public final void readFully(byte b[]) throws IOException {
        d.readFully(b, 0, b.length);
    }

    /**
     * @inheritDoc
     * @see DataInput#readFully(byte[], int, int)
     */
    public final void readFully(byte b[], int off, int len) throws IOException {
        d.readFully(b, off, len);
    }

    /**
     * like DataInputStream.readInt except little endian.
     *
     * @return little-endian binary int from the datastream
     * @throws IOException
     */
    public final int readInt() throws IOException {
        d.readFully(w, 0, 4);
        return (w[3]) << 24 | (w[2] & 0xff) << 16 | (w[1] & 0xff) << 8
                | (w[0] & 0xff);
    }

    /**
     * @return a rough approximation of the 8-bit stream as a 16-bit unicode
     *         string
     * @throws IOException
     * @deprecated This method does not properly convert bytes to characters.
     *             Use a Reader instead with a little-endian encoding.
     */
    @Deprecated
    public final String readLine() throws IOException {
        return d.readLine();
    }

    /**
     * like DataInputStream.readLong except little endian.
     *
     * @return little-endian binary long from the datastream
     * @throws IOException
     */
    public final long readLong() throws IOException {
        d.readFully(w, 0, 8);
        return (long) (w[7]) << 56 | /* long cast needed or shift done modulo 32 */
                (long) (w[6] & 0xff) << 48 | (long) (w[5] & 0xff) << 40
                | (long) (w[4] & 0xff) << 32 | (long) (w[3] & 0xff) << 24
                | (long) (w[2] & 0xff) << 16 | (long) (w[1] & 0xff) << 8
                | (w[0] & 0xff);
    }

    // L I T T L E E N D I A N R E A D E R S
    // Little endian methods for multi-byte numeric types.
    // Big-endian do fine for single-byte types and strings.
    /**
     * like DataInputStream.readShort except little endian.
     *
     * @return little endian binary short from stream
     * @throws IOException
     */
    public final short readShort() throws IOException {
        d.readFully(w, 0, 2);
        return (short) ((w[1] & 0xff) << 8 | (w[0] & 0xff));
    }

    /**
     * note: returns an int, even though says Byte (non-Javadoc)
     *
     * @inheritDoc
     * @see DataInput#readUnsignedByte()
     */
    public final int readUnsignedByte() throws IOException {
        return d.readUnsignedByte();
    }

    /**
     * like DataInputStream.readUnsignedShort except little endian. Note,
     * returns int even though it reads a short.
     *
     * @return little-endian int from the stream
     * @throws IOException
     */
    public final int readUnsignedShort() throws IOException {
        d.readFully(w, 0, 2);
        return ((w[1] & 0xff) << 8 | (w[0] & 0xff));
    }

    /**
     * @inheritDoc
     */
    public final String readUTF() throws IOException {
        return d.readUTF();
    }

    /**
     * See the general contract of the <code>skipBytes</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @param n
     *        the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @exception IOException
     *            if an I/O error occurs.
     */
    public final int skipBytes(int n) throws IOException {
        return d.skipBytes(n);
    }

    // i n s t a n c e v a r i a b l e s

    /**
     * constructor
     *
     * @param in
     *        binary inputstream of little-endian data.
     */
    public LEDataInputStream(InputStream in) {
        this.in = in;
        this.d = new DataInputStream(in);
        w = new byte[8];
    }

    /**
     * Note. This is a STATIC method!
     *
     * @param in
     *        stream to read UTF chars from (endian irrelevant)
     * @return string from stream
     * @throws IOException
     */
    public final static String readUTF(DataInput in) throws IOException {
        return DataInputStream.readUTF(in);
    }

} // end class LEDataInputStream