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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

public class Resource {
    static final String SIG = "Haven Resource 1";
    static final byte[] BSIG = { 72, 97, 118, 101, 110, 32, 82, 101, 115, 111, 117, 114, 99, 101, 32, 49 };
    public static String OUT = "dout/";
    private static final String END = "\r\n";
    private static Map<String, Class<? extends Layer>> ltypes = new TreeMap<String, Class<? extends Layer>>();
    public static Class<Image> imgc = Image.class;
    public static Class<Tile> tile = Tile.class;
    public static Class<Neg> negc = Neg.class;
    public static Class<Anim> animc = Anim.class;
    public static Class<Tileset> tileset = Tileset.class;
    public static Class<Pagina> pagina = Pagina.class;
    public static Class<AButton> action = AButton.class;
    public static Class<Audio> audio = Audio.class;
    public static Class<Tooltip> tooltip = Tooltip.class;

    static int TYPES = 0;
    static final int IMAGE = TYPES++;
    static final int TILE = TYPES++;
    static final int NEG = TYPES++;
    static final int ANIM = TYPES++;
    static final int TILESET = TYPES++;
    static final int PAGINA = TYPES++;
    static final int ABUTTON = TYPES++;
    static final int AUDIO = TYPES++;
    static final int TOOLTIP = TYPES++;
    static final int MUSIC = TYPES++;
    static final int CODE = TYPES++;
    static final int CODEENTRY = TYPES++;
    static final int SOURCES = TYPES++;
    /*
      IMAGE	=> .data + .png
      TILE	=> .data + .png
      CODE	=> .data + .class
      NEG	=> .data
      ANIM	=> .data
      TILESET	=> .data
      PAGINA	=> .data
      ABUTTON   => .data
      TOOLTIP	=> .data      
      CODEENTRY => .data
      AUDIO	=> .ogg
      MUSIC	=> .music
     */

    private Collection<? extends Layer> layers = new LinkedList<Layer>();
    public final String out;
    public final String name;
    public int ver;

    public static Coord cdec(Message buf) {
	return (new Coord(buf.int16(), buf.int16()));
    }

    public static Coord cdec(byte[] buf, int off) {
	return (new Coord(Utils.int16d(buf, off), Utils.int16d(buf, off + 2)));
    }

    public static class LoadException extends RuntimeException {
	public Resource res;

	public LoadException(String msg, Resource res) {
	    super(msg);
	    this.res = res;
	}

	public LoadException(String msg, Throwable cause, Resource res) {
	    super(msg, cause);
	    this.res = res;
	}

	public LoadException(Throwable cause, Resource res) {
	    super("Load error in resource " + res.toString() + "\n" + cause + "\n");
	    this.res = res;
	}
    }

    public abstract class Layer implements Serializable {
	public abstract void init();

	public abstract int size();

	public abstract int type();

	public abstract byte[] type_buffer();

	public abstract void decode(String r, int i) throws Exception;

	public abstract void encode(OutputStream f) throws Exception;
    }

    public static class ImageReadException extends IOException {
	public final String[] supported = ImageIO.getReaderMIMETypes();

	public ImageReadException() {
	    super("Could not decode image data");
	}
    }

    public static BufferedImage readimage(InputStream fp) throws IOException {
	try {
	    /* This can crash if not privileged due to ImageIO
	     * creating tempfiles without doing that privileged
	     * itself. It can very much be argued that this is a bug
	     * in ImageIO. */
	    return (AccessController.doPrivileged(new PrivilegedExceptionAction<BufferedImage>() {
		public BufferedImage run() throws IOException {
		    BufferedImage ret;
		    ret = ImageIO.read(fp);
		    if(ret == null)
			throw (new ImageReadException());
		    return (ret);
		}
	    }));
	} catch (PrivilegedActionException e) {
	    Throwable c = e.getCause();
	    if(c instanceof IOException)
		throw ((IOException) c);
	    throw (new AssertionError(c));
	}
    }

    public class Image extends Layer {
	public transient BufferedImage img;
	public byte[] raw;
	public final int z, subz;
	public final boolean nooff, custom;
	public final int id;
	private float scale = 1;
	public Coord sz, o, tsz;

	public Image(byte[] bytes) {
	    MessageBuf buf = new MessageBuf(bytes);
	    z = buf.int16();/* 2 bytes */
	    subz = buf.int16();/* 2 bytes */
	    /* Obsolete flag 1: Layered */
	    int fl = buf.uint8();
	    nooff = (fl & 2) != 0;/* byte */
	    id = buf.int16();/* 2 bytes */
	    o = cdec(buf);/* 4 bytes */
	    custom = (fl & 4) != 0;
	    if(custom) {
		while (true) {
		    String key = buf.string();
		    if(key.equals(""))
			break;
		    int len = buf.uint8();
		    if((len & 0x80) != 0)
			len = buf.int32();
		    Message val = new MessageBuf(buf.bytes(len));
		    if(key.equals("tsz")) {
			tsz = val.coord();
		    } else if(key.equals("scale")) {
			scale = val.float32();
		    }
		}
	    }

	    try {
		img = readimage(new MessageInputStream(buf));
	    } catch (IOException e) {
		throw (new LoadException(e, Resource.this));
	    }
	    sz = Utils.imgsz(img);
	    if(tsz == null)
		tsz = sz;
	    if(img == null) throw (new LoadException("Invalid image data in " + name, Resource.this));
	}

	public Image(File data, File png) throws Exception {
	    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(data), "UTF-8"));
	    z = Utils.rnint(br);
	    subz = Utils.rnint(br);
	    int fl = Utils.rnint(br);
	    nooff = (fl & 2) != 0;
	    id = Utils.rnint(br);
	    o = new Coord(Utils.rnint(br), Utils.rnint(br));
	    tsz = sz;
	    scale = 1;
	    boolean tmp = false;
	    while (true) {
		String k = Utils.rnstr(br);
		if(k == null) {
		    break;
		} else if("tsz".equals(k)) {
		    tsz = new Coord(Utils.rnint(br), Utils.rnint(br));
		} else if("scale".equals(k)) {
		    scale = Utils.rfloat(br);
		}
		tmp = true;
	    }
	    custom = tmp;
	    img = ImageIO.read(png);
	    br.close();
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ImageIO.write(img, "png", baos);
	    baos.flush();
	    raw = baos.toByteArray();
	    baos.close();
	}

	public int size() {
	    int s = 11;
	    if(custom) {
		if(scale != 1) {
		    s += 6; //'scale\0'
		    s += 1;//len byte
		    s += 4;//value
		}
		if(tsz != sz) {
		    s += 4; //'tsz\0'
		    s += 1; //len byte
		    s += 8;// 2 int16 values for size
		}
		s += 1;//last empty string
	    }
	    s += raw.length;
	    return (s);
	}

	public int type() {
	    return IMAGE;
	}

	@Override
	public byte[] type_buffer() { return new byte[]{105, 109, 97, 103, 101, 0}; }

	public void init() {
	}

	public void decode(String res, int i) throws Exception {
	    new File(res + "/image/").mkdirs();
	    File f = new File(res + "/image/image_" + i + ".data");
	    f.createNewFile();
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8"));
	    bw.write("#IMAGE LAYER FOR RES " + res + END);
	    bw.write("#int16 z" + END);
	    bw.write(Integer.toString(z) + END);
	    bw.write("#int16 subz" + END);
	    bw.write(Integer.toString(subz) + END);
	    bw.write("#Byte nooff" + END);
	    bw.write(Integer.toString(nooff ? 1 : 0) + END);
	    bw.write("#int16 id" + END);
	    bw.write(Integer.toString(id) + END);
	    bw.write("#Coord o" + END);
	    bw.write(Integer.toString(o.x) + END);
	    bw.write(Integer.toString(o.y) + END);
	    if(tsz != sz) {
		bw.write("tsz" + END);
		bw.write(Integer.toString(tsz.x) + END);
		bw.write(Integer.toString(tsz.y) + END);
	    }
	    if(scale != 1) {
		bw.write("scale" + END);
		bw.write(Float.toString(scale) + END);
	    }
	    bw.flush();
	    bw.close();
	    ImageIO.write(img, "png", new File(res + "/image/image_" + i + ".png"));
	}

	public void encode(OutputStream out) throws Exception {
	    out.write(Utils.byte_int16d(z)); /* 2 bytes */
	    out.write(Utils.byte_int16d(subz)); /* 2 bytes */
	    out.write(new byte[]{(byte) ((nooff ? 2 : 0) | (custom ? 4 : 0))}); /* 1 byte */
	    out.write(Utils.byte_int16d(id)); /* 2 bytes */
	    out.write(Utils.byte_int16d(o.x)); /* 2 bytes */
	    out.write(Utils.byte_int16d(o.y)); /* 2 bytes */
	    if(scale != 1) {
		out.write(Utils.byte_strd("scale"));
		out.write(4);
		out.write(Utils.byte_float32d(scale));
	    }
	    if(tsz != sz) {
		out.write(Utils.byte_strd("tsz"));
		out.write(32);
		out.write(Utils.byte_int16d(tsz.x)); /* 2 bytes */
		out.write(Utils.byte_int16d(tsz.y)); /* 2 bytes */
	    }
	    if(custom) {out.write(Utils.byte_strd(""));}
	    out.write(raw); /* img bytes */
	}
    }

    static {
	ltypes.put("image", Image.class);
    }

    public class Tooltip extends Layer {
	public final String t;
	private int size = 0;

	public Tooltip(byte[] buf) {
	    try {
		t = new String(buf, "UTF-8");
	    } catch (UnsupportedEncodingException e) {
		throw (new LoadException(e, Resource.this));
	    }
	}

	public Tooltip(File data) throws Exception {
	    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(data), "UTF-8"));
	    t = Utils.rstr(br);
	    size = Utils.byte_str(t).length;
	    br.close();
	}

	public int size() {
	    return (size);
	}

	public int type() {
	    return TOOLTIP;
	}

	@Override
	public byte[] type_buffer() { return new byte[]{116, 111, 111, 108, 116, 105, 112, 0}; }

	public void init() {
	}

	public void decode(String res, int i) throws Exception {
	    File f = new File(res + "/tooltip/tooltip_" + i + ".data");
	    new File(res + "/tooltip/").mkdirs();
	    f.createNewFile();
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8"));
	    bw.write("#TOOLTIP LAYER FOR RES " + res + END);
	    bw.write("#String tooltip" + END);
	    bw.write(t.replace("\n", "\\n") + END);
	    bw.flush();
	    bw.close();
	}

	public void encode(OutputStream out) throws Exception {
	    out.write(Utils.byte_str(t)); /* str bytes */
	}
    }

    static {
	ltypes.put("tooltip", Tooltip.class);
    }

    public class Tile extends Layer {
	transient BufferedImage img;
	byte[] raw;
	public int id;
	int w;
	char t;

	public Tile(byte[] buf) {
	    t = (char) Utils.ub(buf[0]);/* 1 Byte */
	    id = Utils.ub(buf[1]);/* 1 Byte */
	    w = Utils.uint16d(buf, 2);/* 2 Bytes */
	    try {
		img = ImageIO.read(new ByteArrayInputStream(buf, 4, buf.length - 4));
	    } catch (IOException e) {
		throw (new LoadException(e, Resource.this));
	    }
	    if (img == null) throw (new LoadException("Invalid image data in " + name, Resource.this));
	}

	public Tile(File data, File png) throws Exception {
	    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(data), "UTF-8"));
	    t = (char) Utils.rnint(br);
	    id = Utils.rnint(br);
	    w = Utils.rnint(br);
	    img = ImageIO.read(png);
	    br.close();

	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ImageIO.write(img, "png", baos);
	    baos.flush();
	    raw = baos.toByteArray();
	    baos.close();
	}

	public int size() {
	    int s = 4;
	    s += raw.length;
	    return (s);
	}

	public int type() {
	    return TILE;
	}

	@Override
	public byte[] type_buffer() { return new byte[]{116, 105, 108, 101, 0}; }

	public void decode(String res, int i) throws Exception {
	    File f = new File(res + "/tile/tile_" + i + ".data");
	    new File(res + "/tile/").mkdirs();
	    f.createNewFile();
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8"));
	    bw.write("#TILE LAYER FOR RES " + res + END);
	    bw.write("#Byte t" + END);
	    bw.write(Integer.toString((int) t) + END);
	    bw.write("#Byte id" + END);
	    bw.write(Integer.toString(id) + END);
	    bw.write("#uint16 w" + END);
	    bw.write(Integer.toString(w) + END);
	    bw.flush();
	    bw.close();
	    ImageIO.write(img, "png", new File(res + "/tile/tile_" + i + ".png"));
	}

	public void encode(OutputStream out) throws Exception {
	    out.write(new byte[] { (byte) (t & 0xFF) }); /* 1 byte */
	    out.write(new byte[] { (byte) (id & 0xFF) }); /* 1 byte */
	    out.write(Utils.byte_int16d(w)); /* 2 bytes */
	    out.write(raw); /* img bytes */
	}

	public void init() {
	}
    }

    static {
	ltypes.put("tile", Tile.class);
    }

    public class Neg extends Layer {
	public Coord cc;
	public Coord bc, bs;
	public Coord sz;
	public Coord[][] ep;

	public int en;
	public ArrayList<Integer> cns = new ArrayList<Integer>();
	public ArrayList<Integer> epds = new ArrayList<Integer>();

	public Neg(byte[] buf) {
	    int off;

	    cc = cdec(buf, 0);/* 4 bytes */
	    bc = cdec(buf, 4);/* 4 bytes */
	    bs = cdec(buf, 8);/* 4 bytes */
	    sz = cdec(buf, 12);/* 4 bytes */
	    // bc = MapView.s2m(bc);
	    // bs = MapView.s2m(bs).add(bc.inv());
	    ep = new Coord[8][0];
	    en = buf[16];/* 1 byte */
	    off = 17;
	    for (int i = 0; i < en; i++) {
		int epid = buf[off]; /* 1 byte */
		int cn = Utils.uint16d(buf, off + 1); /* 2 bytes */
		epds.add(epid);
		cns.add(cn);
		off += 3;
		ep[epid] = new Coord[cn];
		for (int o = 0; o < cn; o++) {
		    ep[epid][o] = cdec(buf, off); /* 4 bytes */
		    off += 4;
		}
	    }
	}

	public Neg(File data) throws Exception {
	    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(data), "UTF-8"));
	    cc = new Coord(Utils.rnint(br), Utils.rnint(br)); /* 4 bytes */
	    bc = new Coord(Utils.rnint(br), Utils.rnint(br)); /* 4 bytes */
	    bs = new Coord(Utils.rnint(br), Utils.rnint(br)); /* 4 bytes */
	    sz = new Coord(Utils.rnint(br), Utils.rnint(br)); /* 4 bytes */
	    ep = new Coord[8][0];
	    en = Utils.rnint(br); /* 1 byte */
	    for (int i = 0; i < en; ++i) {
		int epid = Utils.rnint(br); /* 1 byte */
		int cn = Utils.rnint(br); /* 2 bytes */
		epds.add(epid);
		cns.add(cn);
		ep[epid] = new Coord[cn];
		for (int o = 0; o < cn; ++o) {
		    ep[epid][o] = new Coord(Utils.rnint(br), Utils.rnint(br));
		}
	    }
	    br.close();
	}

	public int size() {
	    int s = 17;
	    for (int i = 0; i < cns.size(); ++i) {
		s += 3;
		for (int o = 0; o < cns.get(i); ++o) {
		    s += 4;
		}
	    }
	    return (s);
	}

	public int type() {
	    return NEG;
	}

	@Override
	public byte[] type_buffer() { return new byte[]{110, 101, 103, 0}; }

	public void decode(String res, int i) throws Exception {
	    File f = new File(res + "/neg/neg_" + i + ".data");
	    new File(res + "/neg/").mkdirs();
	    f.createNewFile();
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8"));
	    bw.write("#NEG LAYER FOR RES: " + res + END);
	    bw.write("#Coord cc" + END);
	    bw.write(Integer.toString(cc.x) + END);
	    bw.write(Integer.toString(cc.y) + END);
	    bw.write("#Coord bc" + END);
	    bw.write(Integer.toString(bc.x) + END);
	    bw.write(Integer.toString(bc.y) + END);
	    bw.write("#Coord bs" + END);
	    bw.write(Integer.toString(bs.x) + END);
	    bw.write(Integer.toString(bs.y) + END);
	    bw.write("#Coord sz" + END);
	    bw.write(Integer.toString(sz.x) + END);
	    bw.write(Integer.toString(sz.y) + END);
	    bw.write("#Byte en" + END);
	    bw.write(Integer.toString(en) + END);
	    int j;
	    for (j = 0; j < cns.size(); ++j) {
		bw.write("#Byte epid" + END);
		bw.write(Integer.toString(epds.get(j)) + END);
		bw.write("#uint16 cn" + END);
		bw.write(Integer.toString(cns.get(j)) + END);
		for (int o = 0; o < cns.get(j); o++) {
		    bw.write("#Coord ep[" + epds.get(j) + "][" + o + "]" + END);
		    bw.write(Integer.toString(ep[epds.get(j)][o].x) + END);
		    bw.write(Integer.toString(ep[epds.get(j)][o].y) + END);
		}
	    }
	    bw.flush();
	    bw.close();
	}

	public void encode(OutputStream out) throws Exception {
	    out.write(Utils.byte_int16d(cc.x));
	    out.write(Utils.byte_int16d(cc.y));
	    out.write(Utils.byte_int16d(bc.x));
	    out.write(Utils.byte_int16d(bc.y));
	    out.write(Utils.byte_int16d(bs.x));
	    out.write(Utils.byte_int16d(bs.y));
	    out.write(Utils.byte_int16d(sz.x));
	    out.write(Utils.byte_int16d(sz.y));
	    out.write(new byte[] { (byte) (en & 0xFF) });
	    for (int j = 0; j < cns.size(); ++j) {
		out.write(new byte[] { (byte) (epds.get(j) & 0xFF) });
		out.write(Utils.byte_int16d(cns.get(j)));
		for (int o = 0; o < cns.get(j); ++o) {
		    out.write(Utils.byte_int16d(ep[epds.get(j)][o].x));
		    out.write(Utils.byte_int16d(ep[epds.get(j)][o].y));
		}
	    }
	}

	public void init() {
	}
    }

    static {
	ltypes.put("neg", Neg.class);
    }

    public class Anim extends Layer {
	private int[] ids;
	public int id, d;
	public Image[][] f;

	public Anim(byte[] buf) {
	    id = Utils.int16d(buf, 0);/* 2 bytes */
	    d = Utils.uint16d(buf, 2);/* 2 bytes */
	    ids = new int[Utils.uint16d(buf, 4)];/* 2 bytes */
	    if (buf.length - 6 != ids.length * 2)
		throw (new LoadException("Invalid anim descriptor in " + name, Resource.this));
	    for (int i = 0; i < ids.length; i++) {
		ids[i] = Utils.int16d(buf, 6 + (i * 2)); /* 2 bytes */
	    }
	}

	public Anim(File data) throws Exception {
	    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(data), "UTF-8"));
	    id = Utils.rnint(br);
	    d = Utils.rnint(br);
	    ids = new int[Utils.rnint(br)];
	    for (int j = 0; j < ids.length; ++j) {
		ids[j] = Utils.rnint(br);
	    }
	    br.close();
	}

	public int size() {
	    int s = 6;
	    s += (2 * ids.length);
	    return (s);
	}

	public int type() {
	    return ANIM;
	}

	@Override
	public byte[] type_buffer() { return new byte[]{97, 110, 105, 109, 0}; }

	public void decode(String res, int i) throws Exception {
	    File f = new File(res + "/anim/anim_" + i + ".data");
	    new File(res + "/anim/").mkdirs();
	    f.createNewFile();
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8"));
	    bw.write("#ANIM LAYER FOR RES " + res + END);
	    bw.write("#int16 id [keep -1]" + END);
	    bw.write(Integer.toString(id) + END);
	    bw.write("#uint16 d [duration of animation]" + END);
	    bw.write(Integer.toString(d) + END);
	    bw.write("#uint16 ids [length]" + END);
	    bw.write(Integer.toString(ids.length) + END);
	    for (int j = 0; j < ids.length; j++) {
		bw.write("#uint16 ids[" + j + "]" + END);
		bw.write(Integer.toString(ids[j]) + END);
	    }
	    bw.flush();
	    bw.close();
	}

	public void encode(OutputStream out) throws Exception {
	    out.write(Utils.byte_int16d(id));
	    out.write(Utils.byte_int16d(d));
	    out.write(Utils.byte_int16d(ids.length));
	    for (int i = 0; i < ids.length; ++i)
		out.write(Utils.byte_int16d(ids[i]));
	}

	public void init() {
	}
    }

    static {
	ltypes.put("anim", Anim.class);
    }

    public class Tileset extends Layer {
	private int fl;
	private String[] fln;
	private int[] flv;
	private int[] flw;
	int flnum;
	int flavprob;

	private int size = 0;

	public Tileset(byte[] buf) {
	    int[] off = new int[1];
	    off[0] = 0;
	    fl = Utils.ub(buf[off[0]++]); /* 1 Byte off = 0 */
	    flnum = Utils.uint16d(buf, off[0]); /* 2 Bytes off = 1 */
	    off[0] += 2; /* off = 3 */
	    flavprob = Utils.uint16d(buf, off[0]);/* 2 Bytes */
	    off[0] += 2; /* off = 5 */
	    fln = new String[flnum];
	    flv = new int[flnum];
	    flw = new int[flnum];
	    for (int i = 0; i < flnum; i++) {
		fln[i] = Utils.strd(buf, off); /* String */
		flv[i] = Utils.uint16d(buf, off[0]); /* 2 Bytes */
		off[0] += 2;
		flw[i] = Utils.ub(buf[off[0]++]); /* 1 Byte */
	    }
	}

	public Tileset(File data) throws Exception {
	    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(data), "UTF-8"));
	    fl = Utils.rnint(br);
	    flnum = Utils.rnint(br);
	    flavprob = Utils.rnint(br);
	    fln = new String[flnum];
	    flv = new int[flnum];
	    flw = new int[flnum];
	    size = 5;
	    for (int j = 0; j < flnum; j++) {
		fln[j] = Utils.rnstr(br);
		size += Utils.byte_strd(fln[j]).length;
		flv[j] = Utils.rnint(br);
		flw[j] = Utils.rnint(br);
		size += 3;
	    }
	    br.close();
	}

	public int size() {
	    return (size);
	}

	public int type() {
	    return TILESET;
	}

	@Override
	public byte[] type_buffer() { return new byte[]{116, 105, 108, 101, 115, 101, 116, 0}; }

	public void decode(String res, int i) throws Exception {
	    File f = new File(res + "/tileset/tileset_" + i + ".data");
	    new File(res + "/tileset/").mkdirs();
	    f.createNewFile();
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8"));
	    bw.write("#TILESET LAYER FOR RES " + res + END);
	    bw.write("#Byte fl" + END);
	    bw.write(Integer.toString(fl) + END);
	    bw.write("#uint16 flnum" + END);
	    bw.write(Integer.toString(flnum) + END);
	    bw.write("#uint16 flavprob" + END);
	    bw.write(Integer.toString(flavprob) + END);
	    for (int j = 0; j < flnum; j++) {
		bw.write("#String fln[" + j + "]" + END);
		bw.write(fln[j].replace("\n", "\\n") + END);
		bw.write("#uint16d flv[" + j + "]" + END);
		bw.write(Integer.toString(flv[j]) + END);
		bw.write("#byte flw[" + j + "]" + END);
		bw.write(Integer.toString(flw[j]) + END);
	    }
	    bw.flush();
	    bw.close();
	}

	public void encode(OutputStream out) throws Exception {
	    out.write(new byte[] { (byte) (fl & 0xFF) });
	    out.write(Utils.byte_int16d(flnum));
	    out.write(Utils.byte_int16d(flavprob));
	    for (int j = 0; j < flnum; ++j) {
		out.write(Utils.byte_strd(fln[j]));
		out.write(Utils.byte_int16d(flv[j]));
		out.write(new byte[] { (byte) (flw[j] & 0xFF) });
	    }
	}

	public void init() {
	}
    }

    static {
	ltypes.put("tileset", Tileset.class);
    }

    public class Pagina extends Layer {
	public final String text;
	private int size = 0;

	public Pagina(byte[] buf) {
	    try {
		text = new String(buf, "UTF-8"); /* String */
	    } catch (UnsupportedEncodingException e) {
		throw (new LoadException(e, Resource.this));
	    }
	}

	public Pagina(File data) throws Exception {
	    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(data), "UTF-8"));
	    text = Utils.rnstr(br);
	    size = Utils.byte_strd(text).length;
	    br.close();
	}

	public int size() {
	    return (size);
	}

	public int type() {
	    return PAGINA;
	}

	@Override
	public byte[] type_buffer() { return new byte[]{112, 97, 103, 105, 110, 97, 0}; }

	public void decode(String res, int i) throws Exception {
	    File f = new File(res + "/pagina/pagina_" + i + ".data");
	    new File(res + "/pagina/").mkdirs();
	    f.createNewFile();
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8"));
	    bw.write("#PAGINA LAYER FOR RES " + res + END);
	    bw.write("#String text" + END);
	    bw.write(text.replace("\n", "\\n") + END);
	    bw.flush();
	    bw.close();
	}

	public void encode(OutputStream out) throws Exception {
	    out.write(Utils.byte_strd(text));
	}

	public void init() {
	}
    }

    static {
	ltypes.put("pagina", Pagina.class);
    }

    public class AButton extends Layer {
	public final String name;
	public final String preq;
	public final char hk;
	public final String[] ad;
	int adl;
	int pver;
	String pr;
	int size = 0;

	public AButton(byte[] buf) {
	    int[] off = new int[1];
	    off[0] = 0;
	    pr = Utils.strd(buf, off); /* String */
	    pver = Utils.uint16d(buf, off[0]); /* 2 Byte */
	    off[0] += 2;
	    name = Utils.strd(buf, off); /* String */
	    preq = Utils.strd(buf, off); /* String Prerequisite skill */
	    hk = (char) Utils.uint16d(buf, off[0]); /* 2 Bytes */
	    off[0] += 2;
	    ad = new String[adl = Utils.uint16d(buf, off[0])]; /* 2 Bytes */
	    off[0] += 2;
	    for (int i = 0; i < ad.length; i++)
		ad[i] = Utils.strd(buf, off); /* String */
	}

	public AButton(File data) throws Exception {
	    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(data), "UTF-8"));
	    pr = Utils.rnstr(br);
	    size = Utils.byte_strd(pr).length;
	    pver = Utils.rnint(br);
	    name = Utils.rnstr(br);
	    size += Utils.byte_strd(name).length;
	    preq = Utils.rnstr(br);
	    size += Utils.byte_strd(preq).length;
	    hk = (char) Utils.rnint(br);
	    ad = new String[Utils.rnint(br)];
	    for (int j = 0; j < ad.length; ++j) {
		ad[j] = Utils.rnstr(br);
		size += Utils.byte_strd(ad[j]).length;
	    }
	    br.close();
	}

	public int size() {
	    return (size + 6);
	}

	public int type() {
	    return ABUTTON;
	}

	@Override
	public byte[] type_buffer() { return new byte[]{97, 99, 116, 105, 111, 110, 0}; }

	public void decode(String res, int i) throws Exception {
	    File f = new File(res + "/action/action_" + i + ".data");
	    new File(res + "/action/").mkdirs();
	    f.createNewFile();
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8"));
	    bw.write("#ABUTTON LAYER FOR RES " + res + END);
	    bw.write("#String pr" + END);
	    bw.write(pr.replace("\n", "\\n") + END);
	    bw.write("#uint16 pver" + END);
	    bw.write(Integer.toString(pver) + END);
	    bw.write("#String name" + END);
	    bw.write(name.replace("\n", "\\n") + END);
	    bw.write("#String preq" + END);
	    bw.write(preq.replace("\n", "\\n") + END);
	    bw.write("#uint16 hk" + END);
	    bw.write(Integer.toString((int) hk) + END);
	    bw.write("#uint16 ad length" + END);
	    bw.write(Integer.toString(adl) + END);
	    for (int j = 0; j < adl; ++j) {
		bw.write("#String ad[" + j + "]" + END);
		bw.write(ad[j].replace("\n", "\\n") + END);
	    }
	    bw.flush();
	    bw.close();
	}

	public void encode(OutputStream out) throws Exception {
	    out.write(Utils.byte_strd(pr));
	    out.write(Utils.byte_int16d(pver));
	    out.write(Utils.byte_strd(name));
	    out.write(Utils.byte_strd(preq));
	    out.write(Utils.byte_int16d(hk));
	    out.write(Utils.byte_int16d(ad.length));
	    for (int j = 0; j < ad.length; ++j)
		out.write(Utils.byte_strd(ad[j]));
	}

	public void init() {
	}
    }

    static {
	ltypes.put("action", AButton.class);
    }

    public class Code extends Layer {
	public final String name;
	transient public final byte[] data;
	private int size = 0;

	public Code(byte[] buf) {
	    int[] off = new int[1];
	    off[0] = 0;
	    name = Utils.strd(buf, off);/* String */
	    data = new byte[buf.length - off[0]]; /* rest */
	    System.arraycopy(buf, off[0], data, 0, data.length);
	}

	public Code(File dat, File clas) throws Exception {
	    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dat), "UTF-8"));
	    name = Utils.rnstr(br);
	    size = Utils.byte_strd(name).length;
	    byte[] tmp = Utils.readBytes(clas);
	    if(!Utils.isJavaClass(tmp)) {
		clas = new File(clas.getParentFile() + File.separator + new String(tmp));
		tmp = Utils.readBytes(clas);
	    }
	    data = tmp;
	    br.close();
	}

	public int size() {
	    return (size + data.length);
	}

	public int type() {
	    return CODE;
	}

	@Override
	public byte[] type_buffer() { return new byte[]{99, 111, 100, 101, 0}; }

	public void decode(String res, int i) throws Exception {
	    File f = new File(res + "/code/code_" + i + ".data");
	    new File(res + "/code/").mkdirs();
	    f.createNewFile();
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8"));
	    bw.write("#CODE LAYER FOR RES " + res + END);
	    bw.write("#String class_name" + END);
	    bw.write("#Note: the .class file will have the same name as this file" + END);
	    bw.write(name.replace("\n", "\\n") + END);
	    bw.flush();
	    bw.close();
	    f = new File(res + "/code/code_" + i + ".class");
	    FileOutputStream fout = new FileOutputStream(f);
	    fout.write(data);
	    fout.flush();
	    fout.close();
	}

	public void encode(OutputStream out) throws Exception {
	    out.write(Utils.byte_strd(name));
	    out.write(data);
	}

	public void init() {
	}
    }

    static {
	ltypes.put("code", Code.class);
    }

    public class CodeEntry extends Layer {
	private int size = 0;
	private ArrayList<String> p = new ArrayList<String>();
	private ArrayList<String> e = new ArrayList<String>();

	public CodeEntry(byte[] buf) {
	    int[] off = new int[1];
	    off[0] = 0;
	    while (off[0] < buf.length) {
		int t = buf[off[0]++];
		if(t == 1) {
		    while(true) {
			String ps = Utils.strd(buf, off);
			String es = Utils.strd(buf, off);
			if(ps.length() == 0)
			    break;
			p.add(ps);/* String */
			e.add(es);/* String */
		    }
		} else if(t == 2) {
		    while(true) {
			String ln = Utils.strd(buf, off);
			if(ln.length() == 0)
			    break;
			int ver = Utils.uint16d(buf, off[0]); off[0] += 2;
			//classpath.add(Resource.load(ln, ver));
		    }
		}
	    }
	}

	public CodeEntry(File data) throws Exception {
	    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(data), "UTF-8"));
	    String t;
	    int s = Utils.rnint(br);
	    for (int j = 0; j < s; ++j) {
		t = Utils.rnstr(br);
		p.add(t);
		size += Utils.byte_strd(t).length;
		t = Utils.rnstr(br);
		e.add(t);
		size += Utils.byte_strd(t).length;
	    }
	    br.close();
	}

	public int size() {
	    return (size);
	}

	public int type() {
	    return CODEENTRY;
	}

	@Override
	public byte[] type_buffer() { return new byte[]{99, 111, 100, 101, 101, 110, 116, 114, 121, 0}; }

	public void decode(String res, int i) throws Exception {
	    File f = new File(res + "/codeentry/codeentry_" + i + ".data");
	    new File(res + "/codeentry/").mkdirs();
	    f.createNewFile();
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8"));
	    bw.write("#CODEENTRY LAYER FOR RES " + res + END);
	    bw.write("#int32 length" + END);
	    bw.write(p.size() + END);
	    for (int j = 0; j < p.size(); ++j) {
		bw.write("#String key[" + j + "]" + END);
		bw.write(p.get(j).replace("\n", "\\n") + END);
		bw.write("#String value[" + j + "]" + END);
		bw.write(e.get(j).replace("\n", "\\n") + END);
	    }
	    bw.flush();
	    bw.close();
	}

	public void encode(OutputStream out) throws Exception {
	    for (int i = 0; i < p.size(); ++i) {
		out.write(Utils.byte_strd(p.get(i)));
		out.write(Utils.byte_strd(e.get(i)));
	    }
	}

	public void init() {
	}
    }

    static {
	ltypes.put("codeentry", CodeEntry.class);
    }

    public class Audio extends Layer {
	// transient public byte[] clip;
	byte[] raw;

	public Audio(byte[] buf) {
	    raw = new byte[buf.length];
	    for (int i = 0; i < buf.length; ++i) {
		raw[i] = buf[i];
	    }
	}

	public Audio(File ogg) throws Exception {
	    FileInputStream fis = new FileInputStream(ogg);
	    raw = new byte[(int) ogg.length()];
	    fis.read(raw);
	    fis.close();
	}

	public int size() {
	    return (raw.length);
	}

	public int type() {
	    return AUDIO;
	}

	@Override
	public byte[] type_buffer() { return new byte[]{97, 117, 100, 105, 111, 0}; }

	public void decode(String res, int i) throws Exception {
	    File f = new File(res + "/audio/audio_" + i + ".ogg");
	    new File(res + "/audio/").mkdirs();
	    f.createNewFile();
	    FileOutputStream fout = new FileOutputStream(f);
	    fout.write(raw);
	    fout.flush();
	    fout.close();
	}

	public void encode(OutputStream out) throws Exception {
	    out.write(raw);
	}

	public void init() {
	}
    }

    static {
	ltypes.put("audio", Audio.class);
    }

    public class Music extends Resource.Layer {
	// transient javax.sound.midi.Sequence seq;
	byte[] raw;

	public Music(byte[] buf) {
	    raw = new byte[buf.length];
	    for (int i = 0; i < buf.length; ++i) {
		raw[i] = buf[i];
	    }
	}

	public Music(File midi) throws Exception {
	    FileInputStream fis = new FileInputStream(midi);
	    raw = new byte[(int) midi.length()];
	    fis.read(raw);
	    fis.close();
	}

	public int size() { return (raw.length); }

	public int type() { return MUSIC; }

	@Override
	public byte[] type_buffer() { return new byte[]{109, 105, 100, 105, 0}; }

	public void decode(String res, int i) throws Exception {
	    File f = new File(res + "/midi/midi_" + i + ".midi"); /* what file type is this idk? */
	    new File(res + "/midi/").mkdirs();
	    f.createNewFile();
	    FileOutputStream fout = new FileOutputStream(f);
	    fout.write(raw);
	    fout.flush();
	    fout.close();
	}

	public void encode(OutputStream out) throws Exception {
	    out.write(raw);
	}

	public void init() {
	}
    }

    static {
	ltypes.put("midi", Music.class);
    }

    public class Sources extends Resource.Layer {
	byte[] raw;

	public Sources(byte[] buf) {
	    raw = new byte[buf.length];
	    for (int i = 0; i < buf.length; ++i) {
		raw[i] = buf[i];
	    }
	}

	public Sources(File src) throws Exception {
	    FileInputStream fis = new FileInputStream(src);
	    raw = new byte[(int) src.length()];
	    fis.read(raw);
	    fis.close();
	}

	@Override
	public void init() { }

	@Override
	public int size() { return raw.length; }

	@Override
	public int type() { return SOURCES; }

	@Override
	public byte[] type_buffer() { return new byte[]{115, 114, 99, 0}; }

	@Override
	public void decode(String res, int i) throws Exception {
	    File f = new File(res + "/src/src_" + i + ".java");
	    new File(res + "/src/").mkdirs();
	    f.createNewFile();
	    FileOutputStream fout = new FileOutputStream(f);
	    fout.write(raw);
	    fout.flush();
	    fout.close();
	}

	@Override
	public void encode(OutputStream out) throws Exception {
	    out.write(raw);
	}
    }

    static {
	ltypes.put("src", Sources.class);
    }

    public Resource(String full, String name, String out, boolean w) throws Exception {
	this.out = out;
	this.name = name;
	if(w) {
	    /* should be in .res format */
	    load(new FileInputStream(new File(full)));
	} else {
	    /* decoded format */
	    loadfromdecode(full);
	}
    }

    public Resource(String full, String name, boolean w) throws Exception {
	this(full, name, OUT, w);
    }

    private void readall(InputStream in, byte[] buf) throws IOException {
	int ret, off = 0;
	while (off < buf.length) {
	    ret = in.read(buf, off, buf.length - off);
	    if (ret < 0) throw (new LoadException("Incomplete resource at " + name, this));
	    off += ret;
	}
    }

    private void load(InputStream in) throws Exception {
	byte buf[] = new byte[SIG.length()];
	readall(in, buf);/* String */
	if (!SIG.equals(new String(buf))) throw (new LoadException("Invalid res signature", this));
	buf = new byte[2];
	readall(in, buf);
	ver = Utils.uint16d(buf, 0); /* 2 bytes */
	List<Layer> layers = new LinkedList<Layer>();
	outer: while (true) {
	    StringBuilder tbuf = new StringBuilder();
	    while (true) { /* load in layer type */
		byte bb;
		int ib;
		if ((ib = in.read()) == -1) { /* 1 byte */
		    if (tbuf.length() == 0) break outer;
		    throw (new LoadException("Incomplete resource at " + name, this));
		}
		bb = (byte) ib;
		if (bb == 0) break;
		tbuf.append((char) bb);
	    }

	    /* get length of layer */
	    buf = new byte[4];
	    readall(in, buf);
	    int len = Utils.int32d(buf, 0); /* 4 bytes */
	    /* read in rest of data and init layer */
	    buf = new byte[len];
	    readall(in, buf);
	    String layerName = tbuf.toString();
	    Class<? extends Layer> lc = ltypes.get(layerName);
	    if(lc == null) {
		System.out.println(String.format("Couldn't find  layer class for '%s'", layerName));
		continue;
	    }
	    Constructor<? extends Layer> cons;
	    try {
		cons = lc.getConstructor(Resource.class, byte[].class);
	    } catch (NoSuchMethodException e) {
		throw (new LoadException(e, Resource.this));
	    }
	    Layer l;
	    try {
		l = cons.newInstance(this, buf);
	    } catch (InstantiationException e) {
		throw (new LoadException(e, Resource.this));
	    } catch (InvocationTargetException e) {
		Throwable c = e.getCause();
		if (c instanceof RuntimeException)
		    throw ((RuntimeException) c);
		else
		    throw (new LoadException(c, Resource.this));
	    } catch (IllegalAccessException e) {
		throw (new LoadException(e, Resource.this));
	    }
	    layers.add(l);
	}

	this.layers = layers;
	for (Layer l : layers)
	    l.init();
    }

    public void decodeall() throws Exception {
	final String base = out + name;
	new File(base).mkdirs();
	int c[] = new int[TYPES];
	{
	    for (int i = 0; i < TYPES; ++i)
		c[i] = 0;
	}
	for (Layer l : layers) {
	    l.decode(base, c[l.type()]++);
	}
	BufferedWriter bw = new BufferedWriter(new FileWriter(base + "/meta"));
	bw.write("#General info for res " + base + END);
	bw.write("#int16 ver" + END);
	bw.write(Integer.toString(ver) + END);
	bw.flush();
	bw.close();
    }

    private void loadfromdecode(String full) throws Exception {
	if (!full.endsWith(".res")) throw (new Exception("Invalid decoded res directory"));
	File f = new File(full);
	if (!f.isDirectory()) throw (new Exception("Invalid decoded res directory"));
	File l[] = f.listFiles();
	File df[];
	String n;
	int i, j;
	List<Layer> layers = new LinkedList<Layer>();
	for (i = 0; i < l.length; ++i) {
	    if (l[i].isDirectory()) {
		n = l[i].getName();
		Class<? extends Layer> lc = ltypes.get(n);
		if (lc == null) continue;
		Constructor<? extends Layer> cons;

		df = l[i].listFiles();
		switch (n) {
		    case "image":
		    case "tile": { /* .data + .png */
			if (df.length % 2 != 0) throw (new Exception("Invalid number of decoded files for " + n));
			try {
			    cons = lc.getConstructor(Resource.class, File.class, File.class);
			} catch (NoSuchMethodException e) {
			    throw (new LoadException(e, Resource.this));
			}
			for (j = 0; j < df.length - 1; ++j) {
			    if (df[j].getName().endsWith(".data") || df[j + 1].getName().endsWith(".png"))
				layers.add(cons.newInstance(this, df[j++], df[j]));
			}
		    }
			break;
		    case "code": { /* .data + .class */
			if(df.length % 2 != 0) throw (new Exception("Invalid number of decoded files for " + n));
			try {
			    cons = lc.getConstructor(Resource.class, File.class, File.class);
			} catch (NoSuchMethodException e) {
			    throw (new LoadException(e, Resource.this));
			}
			for (j = 0; j < df.length - 1; j += 2) {
			    if(df[j].getName().endsWith(".data")) {
				layers.add(cons.newInstance(this, df[j], df[j + 1]));
			    } else if(df[j].getName().endsWith(".class")) {
				layers.add(cons.newInstance(this, df[j + 1], df[j]));
			    }
			}
		    }
			break;
		    case "neg":
		    case "anim":
		    case "tooltip":
		    case "tileset":
		    case "codeentry":
		    case "pagina":
		    case "action": { /* .data */
			try {
			    cons = lc.getConstructor(Resource.class, File.class);
			} catch (NoSuchMethodException e) {
			    throw (new LoadException(e, Resource.this));
			}
			for (j = 0; j < df.length; ++j)
			    if (df[j].getName().endsWith(".data")) layers.add(cons.newInstance(this, df[j]));
		    }
			break;
		    case "midi": { /* .music */
			try {
			    cons = lc.getConstructor(Resource.class, File.class);
			} catch (NoSuchMethodException e) {
			    throw (new LoadException(e, Resource.this));
			}
			for (j = 0; j < df.length; ++j)
			    if (df[j].getName().endsWith(".midi")) layers.add(cons.newInstance(this, df[j]));
		    }
			break;
		    case "audio": { /* .ogg */
			try {
			    cons = lc.getConstructor(Resource.class, File.class);
			} catch (NoSuchMethodException e) {
			    throw (new LoadException(e, Resource.this));
			}
			for (j = 0; j < df.length; ++j)
			    if (df[j].getName().endsWith(".ogg")) layers.add(cons.newInstance(this, df[j]));
		    }
			break;
		    case "src":  /* .java */
			try {
			    cons = lc.getConstructor(Resource.class, File.class);
			} catch (NoSuchMethodException e) {
			    throw (new LoadException(e, Resource.this));
			}
			for (j = 0; j < df.length; ++j)
			    if(df[j].getName().endsWith(".java")) layers.add(cons.newInstance(this, df[j]));

			break;
		}
	    }
	}
	this.layers = layers;

	BufferedReader br = new BufferedReader(new FileReader(full + "/meta"));
	ver = Utils.rnint(br);
	br.close();
    }

    public void encodeall() throws Exception {
	File f = new File(out + name);
	f.mkdirs();
	f.delete();
	f.createNewFile();
	FileOutputStream fos = new FileOutputStream(f);
	byte buf[] = BSIG;
	fos.write(buf);/* 1 String */
	buf = Utils.byte_int16d(ver);
	fos.write(buf);/* 2 Bytes */

	for (Layer l : layers) {
	    fos.write(l.type_buffer()); /* Layer id */
	    fos.write(Utils.byte_int32d(l.size())); /* 4 bytes */
	    l.encode(fos); /* l.size() bytes */
	}
	fos.flush();
	fos.close();
    }
}
