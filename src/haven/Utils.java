/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Bj�rn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    public static String sessdate(long sess) {
	return (new SimpleDateFormat("yyyy-MM-dd HH.mm.ss")).format(new Date(sess));
    }
    
    public static String timestamp() {
	return (new SimpleDateFormat("[HH:mm] ")).format(new Date());
    }
    
    static Coord imgsz(BufferedImage img) {
    	if(img != null)//annoying ass eror
    		return(new Coord(img.getWidth(), img.getHeight()));
    	return(new Coord(0,0));
    }
	
    static int ub(byte b) {
	if(b < 0)
	    return(256 + b);
	else
	    return(b);
    }
	
    static byte sb(int b) {
	if(b > 127)
	    return((byte)(-256 + b));
	else
	    return((byte)b);
    }
	
    static int uint16d(byte[] buf, int off) {
	return(ub(buf[off]) + (ub(buf[off + 1]) * 256));
    }
	
    static int int16d(byte[] buf, int off) {
	int u = uint16d(buf, off);
	if(u > 32767)
	    return(-65536 + u);
	else
	    return(u);
    }

    static byte[] byte_int16d(int value){
	return(new byte[] {
		(byte)(value & 0xff),
		(byte)((value >> 8) & 0xff)
	    });
    }

    static byte[] byte_float32d(float value) {
        byte[] buf = new byte[4];
        float32e(value, buf, 0);
	return(buf);
    }
	
    static long uint32d(byte[] buf, int off) {
	return(ub(buf[off]) + (ub(buf[off + 1]) * 256) + (ub(buf[off + 2]) * 65536) + (ub(buf[off + 3]) * 16777216));
    }
	
    static void uint32e(long num, byte[] buf, int off) {
	buf[off] = sb((int)(num & 0xff));
	buf[off + 1] = sb((int)((num & 0xff00) >> 8));
	buf[off + 2] = sb((int)((num & 0xff0000) >> 16));
	buf[off + 3] = sb((int)((num & 0xff000000) >> 24));
    }

    static int int32d(byte[] buf, int off) {
	long u = uint32d(buf, off);
	if(u > Integer.MAX_VALUE)
	    return ((int) ((((long) Integer.MIN_VALUE) * 2) - u));
	else
	    return ((int) u);
    }

    public static long int64d(byte[] buf, int off) {
	long b = 0;
	for (int i = 0; i < 8; i++)
	    b |= ((long) ub(buf[off + i])) << (i * 8);
	return (b);
    }

    public static void int64e(long num, byte[] buf, int off) {
	for (int i = 0; i < 8; i++) {
	    buf[off++] = (byte) (num & 0xff);
	    num >>>= 8;
	}
    }

    static byte[] byte_int32d(int value) {
	return (new byte[]{
	    (byte) (value & 0xff),
	    (byte) ((value >> 8) & 0xff),
	    (byte) ((value >> 16) & 0xff),
	    (byte) ((value >> 24) & 0xff)
	});
    }

    static void int32e(int num, byte[] buf, int off) {
	if(num < 0)
	    uint32e(0x100000000L + ((long) num), buf, off);
	else
	    uint32e(num, buf, off);
    }

    static void uint16e(int num, byte[] buf, int off) {
	buf[off] = sb(num & 0xff);
	buf[off + 1] = sb((num & 0xff00) >> 8);
    }

    public static void int16e(short num, byte[] buf, int off) {
	uint16e(((int) num) & 0xffff, buf, off);
    }

    public static double floatd(byte[] buf, int off) {
	int e = buf[off];
	long t = uint32d(buf, off + 1);
	int m = (int) (t & 0x7fffffffL);
	boolean s = (t & 0x80000000L) != 0;
	if(e == -128) {
	    if(m == 0)
		return (0.0);
	    throw (new RuntimeException("Invalid special float encoded (" + m + ")"));
	}
	double v = (((double) m) / 2147483648.0) + 1.0;
	if(s)
	    v = -v;
	return (Math.pow(2.0, e) * v);
    }

    public static float float32d(byte[] buf, int off) {
	return (Float.intBitsToFloat(int32d(buf, off)));
    }

    public static double float64d(byte[] buf, int off) {
	return (Double.longBitsToDouble(int64d(buf, off)));
    }

    public static void float32e(float num, byte[] buf, int off) {
	int32e(Float.floatToIntBits(num), buf, off);
    }

    public static void float64e(double num, byte[] buf, int off) {
	int64e(Double.doubleToLongBits(num), buf, off);
    }

    public static void float9995d(int word, float[] ret) {
	int xb = (word & 0x7f800000) >> 23, xs = ((word & 0x80000000) >> 31) & 1,
	    yb = (word & 0x003fc000) >> 14, ys = ((word & 0x00400000) >> 22) & 1,
	    zb = (word & 0x00001fe0) >> 5, zs = ((word & 0x00002000) >> 13) & 1;
	int me = (word & 0x1f) - 15;
	int xe = Integer.numberOfLeadingZeros(xb) - 24,
	    ye = Integer.numberOfLeadingZeros(yb) - 24,
	    ze = Integer.numberOfLeadingZeros(zb) - 24;
	if(xe == 8) ret[0] = 0;
	else ret[0] = Float.intBitsToFloat((xs << 31) | ((me - xe + 127) << 23) | ((xb << (xe + 16)) & 0x007fffff));
	if(ye == 8) ret[1] = 0;
	else ret[1] = Float.intBitsToFloat((ys << 31) | ((me - ye + 127) << 23) | ((yb << (ye + 16)) & 0x007fffff));
	if(ze == 8) ret[2] = 0;
	else ret[2] = Float.intBitsToFloat((zs << 31) | ((me - ze + 127) << 23) | ((zb << (ze + 16)) & 0x007fffff));
    }

    public static float hfdec(short bits) {
	int b = ((int) bits) & 0xffff;
	int e = (b & 0x7c00) >> 10;
	int m = b & 0x03ff;
	int ee;
	if(e == 0) {
	    if(m == 0) {
		ee = 0;
	    } else {
		int n = Integer.numberOfLeadingZeros(m) - 22;
		ee = (-15 - n) + 127;
		m = (m << (n + 1)) & 0x03ff;
	    }
	} else if(e == 0x1f) {
	    ee = 0xff;
	} else {
	    ee = e - 15 + 127;
	}
	int f32 = ((b & 0x8000) << 16) |
	    (ee << 23) |
	    (m << 13);
	return (Float.intBitsToFloat(f32));
    }

    public static short hfenc(float f) {
	int b = Float.floatToIntBits(f);
	int e = (b & 0x7f800000) >> 23;
	int m = b & 0x007fffff;
	int ee;
	if(e == 0) {
	    ee = 0;
	    m = 0;
	} else if(e == 0xff) {
	    ee = 0x1f;
	} else if(e < 127 - 14) {
	    ee = 0;
	    m = (m | 0x00800000) >> ((127 - 14) - e);
	} else if(e > 127 + 15) {
	    return (((b & 0x80000000) == 0) ? ((short) 0x7c00) : ((short) 0xfc00));
	} else {
	    ee = e - 127 + 15;
	}
	int f16 = ((b >> 16) & 0x8000) |
	    (ee << 10) |
	    (m >> 13);
	return ((short) f16);
    }

    public static float mfdec(byte bits) {
	int b = ((int) bits) & 0xff;
	int e = (b & 0x78) >> 3;
	int m = b & 0x07;
	int ee;
	if(e == 0) {
	    if(m == 0) {
		ee = 0;
	    } else {
		int n = Integer.numberOfLeadingZeros(m) - 29;
		ee = (-7 - n) + 127;
		m = (m << (n + 1)) & 0x07;
	    }
	} else if(e == 0x0f) {
	    ee = 0xff;
	} else {
	    ee = e - 7 + 127;
	}
	int f32 = ((b & 0x80) << 24) |
	    (ee << 23) |
	    (m << 20);
	return (Float.intBitsToFloat(f32));
    }

    public static byte mfenc(float f) {
	int b = Float.floatToIntBits(f);
	int e = (b & 0x7f800000) >> 23;
	int m = b & 0x007fffff;
	int ee;
	if(e == 0) {
	    ee = 0;
	    m = 0;
	} else if(e == 0xff) {
	    ee = 0x0f;
	} else if(e < 127 - 6) {
	    ee = 0;
	    m = (m | 0x00800000) >> ((127 - 6) - e);
	} else if(e > 127 + 7) {
	    return (((b & 0x80000000) == 0) ? ((byte) 0x78) : ((byte) 0xf8));
	} else {
	    ee = e - 127 + 7;
	}
	int f8 = ((b >> 24) & 0x80) |
	    (ee << 3) |
	    (m >> 20);
	return ((byte) f8);
    }

    public static double clip(double d, double min, double max) {
	if(d < min)
	    return (min);
	if(d > max)
	    return (max);
	return (d);
    }

    public static float clip(float d, float min, float max) {
	if(d < min)
	    return (min);
	if(d > max)
	    return (max);
	return (d);
    }

    public static int clip(int i, int min, int max) {
	if(i < min)
	    return (min);
	if(i > max)
	    return (max);
	return (i);
    }

    public static String strd(byte[] buf, int[] off) {
	int i;
	for (i = off[0]; buf[i] != 0; i++) ;
	String ret;
	try {
	    ret = new String(buf, off[0], i - off[0], "utf-8");
	} catch (UnsupportedEncodingException e) {
	    throw (new IllegalArgumentException(e));
	}
	off[0] = i + 1;
	return (ret);
    }

    static byte[] byte_strd(String s) throws Exception{
	int i;
	byte[] utf8 = s.getBytes("UTF-8");
	byte[] b = new byte[utf8.length+1];
	for(i=0;i<utf8.length;++i){
	    b[i] = utf8[i];
	}
	b[utf8.length] = 0;// nul end
	return(b);
    }

    static byte[] byte_str(String s) throws Exception{
	return(s.getBytes("UTF-8"));
    }
	
    static char num2hex(int num) {
	if(num < 10)
	    return((char)('0' + num));
	else
	    return((char)('A' + num - 10));
    }
	
    static int hex2num(char hex) {
	if((hex >= '0') && (hex <= '9'))
	    return(hex - '0');
	else if((hex >= 'a') && (hex <= 'f'))
	    return(hex - 'a' + 10);
	else if((hex >= 'A') && (hex <= 'F'))
	    return(hex - 'A' + 10);
	else
	    throw(new RuntimeException());
    }

    static String byte2hex(byte[] in) {
	StringBuilder buf = new StringBuilder();
	for(byte b : in) {
	    buf.append(num2hex((b & 0xf0) >> 4));
	    buf.append(num2hex(b & 0x0f));
	}
	return(buf.toString());
    }

    static byte[] hex2byte(String hex) {
	if(hex.length() % 2 != 0)
	    throw(new RuntimeException("Invalid hex-encoded string"));
	byte[] ret = new byte[hex.length() / 2];
	for(int i = 0, o = 0; i < hex.length(); i += 2, o++)
	    ret[o] = (byte)((hex2num(hex.charAt(i)) << 4) | hex2num(hex.charAt(i + 1)));
	return(ret);
    }
	
   


    static int atoi(String a) {
	try {
	    return(Integer.parseInt(a));
	} catch(NumberFormatException e) {
	    return(0);
	}
    }
    
    static void readtileof(InputStream in) throws IOException {
        byte[] buf = new byte[4096];
        while(true) {
            if(in.read(buf, 0, buf.length) < 0)
                return;
        }
    }
    
    static byte[] readall(InputStream in) throws IOException {
	byte[] buf = new byte[4096];
	int off = 0;
	while(true) {
	    if(off == buf.length) {
		byte[] n = new byte[buf.length * 2];
		System.arraycopy(buf, 0, n, 0, buf.length);
		buf = n;
	    }
	    int ret = in.read(buf, off, buf.length - off);
	    if(ret < 0) {
		byte[] n = new byte[off];
		System.arraycopy(buf, 0, n, 0, off);
		return(n);
	    }
	    off += ret;
	}
    }

    public static int rnint(BufferedReader br) throws Exception{
	return(Integer.parseInt(rnstr(br)));
    }
    
    public static float rfloat(BufferedReader br) throws Exception{
	return(Float.parseFloat(rnstr(br)));
    }

    public static String rnstr(BufferedReader br) throws Exception {
	String n = "";
	while ((n = br.readLine()) != null) {
	    if(n.length() > 0 && (n.charAt(0) == '#' || n.startsWith("﻿")))
		continue;
	    break;
	}
	return n != null ? (n.replace("\\n", "\n")) : null;
    }

    public static String rstr(BufferedReader br) throws Exception {
	String n;
	while ((n = br.readLine()) != null) {
	    if(n.length() > 0 && (n.charAt(0) == '#' || n.startsWith("﻿")))
		continue;
	    break;
	}
	return n != null ? n.trim() : null;
    }

    public static byte[] rstrbytes(BufferedReader br) throws Exception {
	String[] split = Utils.rstr(br).split(", ");
	List<Byte> list = Arrays.stream(split)
		.map(Byte::parseByte)
		.collect(Collectors.toList());

	byte[] bytes = new byte[list.size()];
	for (int i = 0; i < list.size(); i++) {
	    bytes[i] = list.get(i);
	}
	return bytes;
    }

    public static boolean isJavaClass(byte[] bytes) {
	return bytes.length >= 4
	    && bytes[0] == (byte) 0xCA
	    && bytes[1] == (byte) 0xFE
	    && bytes[2] == (byte) 0xBA
	    && bytes[3] == (byte) 0xBE;
    }
    
    public static byte[] readBytes(File file) throws IOException {
	byte[] tmp = new byte[(int) file.length()];
	FileInputStream fis = new FileInputStream(file);
	fis.read(tmp);
	fis.close();
	return  tmp;
    }

    public static int floordiv(int a, int b) {
	if(a < 0)
	    return(((a + 1) / b) - 1);
	else
	    return(a / b);
    }
    
    public static int floormod(int a, int b) {
	int r = a % b;
	if(r < 0)
	    r += b;
	return(r);
    }

    public static int floordiv(float a, float b) {
	return((int)Math.floor(a / b));
    }
    
    public static float floormod(float a, float b) {
	float r = a % b;
	if(r < 0)
	    r += b;
	return(r);
    }


    public static void serialize(Object obj, OutputStream out) throws IOException {
	ObjectOutputStream oout = new ObjectOutputStream(out);
	oout.writeObject(obj);
	oout.flush();
    }
    
    public static byte[] serialize(Object obj) {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	try {
	    serialize(obj, out);
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
	return(out.toByteArray());
    }
    
    public static Object deserialize(InputStream in) throws IOException {
	ObjectInputStream oin = new ObjectInputStream(in);
	try {
	    return(oin.readObject());
	} catch(ClassNotFoundException e) {
	    return(null);
	}
    }

    public static Object deserialize(byte[] buf) {
	if(buf == null)
	    return(null);
	InputStream in = new ByteArrayInputStream(buf);
	try {
	    return(deserialize(in));
	} catch(IOException e) {
	    return(null);
	}
    }
}
