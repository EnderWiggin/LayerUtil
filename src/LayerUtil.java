/*
 *  This file is part of the LayerUtil
 *  Copyright (C) 2011 Corey Furmanski <furmanskic@gmail.com>
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
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

import haven.Resource;

import java.io.File;
import java.util.ArrayList;

public class LayerUtil {
    static final String VERSION = "LayerUtil version - 2.0.0";
    static final String U = "Usage: java -jar LayerUtil.jar [MODIFIER_FLAGS] [ENC/DEC_OPTION] [OPTION ARGS]\n"
	    + "Possible modifier flags include:\n"
	    + " -ns             Do not skip processing older files."
	    + " -np             Do not print files being processed."
	    + " -ps             Print files being skipped."
	    + " -h              Display usage\n" + " -v              Displays version number\n"
	    + " -d [FILE]...    Decodes said files to `dout/[FILE]/*'\n"
	    + " -e [FILE]...    Encodes said files to `dres/[FILE]/*'\n"
	    + " -rd [IN] [OUT]  Decodes a set of files within IN into OUT\n"
	    + " -re [IN] [OUT]  Encodes a set of files within IN into OUT\n\n"
	    + "[FILE] refers to individual *.res files\n"
	    + "[IN] and [OUT] refer to file directorys that contain *.res files\n";
    static final String SF[] = {
	    "Invalid arguments\n" + "Usage: java -jar LayerUtil.jar [MOD_FLAGS] -d [FILE]...\n"
		    + "Type `java -jar LayerUtil.jar -h' for more information\n",
	    "Invalid arguments\n" + "Usage: java -jar LayerUtil.jar [MOD_FLAGS] -e [FILE]...\n"
		    + "Type `java -jar LayerUtil.jar -h' for more information\n" };
    static final String RF[] = {
	    "Invalid arguments\n" + "Usage: java -jar LayerUtil.jar [MOD_FLAGS] -rd [IN] [OUT]\n"
		    + "Type `java -jar LayerUtil.jar -h' for more information\n",
	    "Invalid arguments\n" + "Usage: java -jar LayerUtil.jar [MOD_FLAGS] -re [IN] [OUT]\n"
		    + "Type `java -jar LayerUtil.jar -h' for more information\n" };
    static final String NOF = "No options found\n" + "Type `java -jar LayerUtil.jar -h' for more information\n";
    static final String IVU = "Invalid usage of LayerUtil\n"
	    + "Type `java -jar LayerUtil.jar -h' for more information\n";

    static boolean skip = true;
    static boolean print = true;
    static boolean print_skips = false;
    
    public static void main(String args[]) {
	try {
	    if (args.length < 1) {
		System.out.print(NOF);
		System.exit(0);
	    }
	    int i = 0;
	    boolean done = false;
	    for (i = 0; i < args.length; ++i) {
		switch (args[i]) {
		    case "-ns":
			skip = false;
			break;
		    case "-np":
			print = false;
			break;
		    case "-ps":
			print_skips = true;
			break;
		    case "-v":
		    case "-h":
		    case "-d":
		    case "-e":
		    case "-rd":
		    case "-re":
			done = true;
			break;
		    default:
			System.out.print(IVU);
			System.exit(1);
			break;
		}
		if (done) break;
	    }

	    switch (args[i]) {
		case "-v":
		    System.out.print(VERSION);
		    System.exit(0);
		    break;
		case "-h":
		    usage();
		    break;
		case "-d":
		    sf(i, args, 0);
		    break;
		case "-e":
		    sf(i, args, 1);
		    break;
		case "-rd":
		    rf(i, args, 0);
		    break;
		case "-re":
		    rf(i, args, 1);
		    break;
		default:
		    System.out.print(IVU);
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}

	System.exit(0);
    }

    static void usage() {
	System.out.print(U);
	System.exit(0);
    }

    static void sf(int st, String[] args, int fl) {
	if (args.length < 2) {
	    System.out.print(SF[fl]);
	    System.exit(0);
	}

	int i;
	Resource r;
	for (i = st + 1; i < args.length; ++i) {
	    try {
		System.out.println("ATTEMPT => " + args[i]);
		r = new Resource(args[i], args[i], fl == 1 ? "nres//" : "dout//", fl == 1 ? false : true);
		if (fl == 1) {
		    r.encodeall();
		} else {
		    r.decodeall();
		}
	    } catch (Exception e) {
		System.out.print("Error loading file " + args[i]);
		e.printStackTrace();
	    }
	}
    }

    static void rf(int st, String[] args, int fl) throws Exception {
	if (args.length < st + 3) {
	    System.out.print(RF[fl]);
	    System.exit(0);
	}
	ArrayList<String> files = new ArrayList<String>();
	String in = args[st + 1];
	int in_len = in.length();
	String out = args[st + 2].replace("\\", "/") + "/";
	if (print) System.out.println("Processing resources in "+in);
	if (fl == 1) {
	    rcdget(new File(in), files);
	} else {
	    rcget(new File(in), files);
	}
	String name;
	Resource r;
	for (String s : files) {
	    try {
		name = s.substring(in_len);
		
		if(skip){
		    long in_date = getDate(in+name);
		    long out_date = getDate(out+name);
		    if(out_date > in_date){
			if (print_skips) System.out.println("SKIPPED => " + name);
			continue;
		    }
		}
		if (print) System.out.println("ATTEMPT => " + name);
		r = new Resource(s, "/" + name, out, fl == 1 ? false : true);
		if (fl == 1) {
		    r.encodeall();
		} else {
		    r.decodeall();
		}
	    } catch (Exception e) {
		System.out.print("Error loading file " + s);
		e.printStackTrace();
	    }
	}
    }

    private static long getDate(String name) {
	return getDate(new File(name));
    }
    
    private static long getDate(File file) {
	if(file.exists()){
	    if(file.isFile()){
		return file.lastModified();
	    } else {
		long date = 0;
		for (File s : file.listFiles())
		    date = Math.max(date, getDate(s));
		return date;
	    }
	}
	return 0;
    }
    
    
    
    static void rcdget1(File f, ArrayList<String> fls) {
	if (f.isDirectory()) if (f.getName().endsWith(".res") || f.getName().endsWith(".cache")) {
	    fls.add(f.getPath());
	    return;
	} else {
	    for (File s : f.listFiles())
		rcdget1(s, fls);
	}
    }

    static void rcdget(File f, ArrayList<String> fls) {
	rcdget1(f, fls);
    }

    static void rcget1(File f, ArrayList<String> fls) {
	if (f.isFile() && (f.getName().endsWith(".res") || f.getName().endsWith(".cache"))) {
	    fls.add(f.getPath());
	    return;
	}
	if (f.isDirectory()) for (File s : f.listFiles())
	    rcget1(s, fls);
    }

    static void rcget(File f, ArrayList<String> fls) {
	rcget1(f, fls);
    }
}
