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
import java.util.ArrayList;
import java.io.File;

public class LayerUtil
{
    static final String U = 
				"Usage: java -jar LayerUtil.jar [OPTION] [OPTION ARGS]\n"+
				"Possible Arguments:\n"+
				" -h              Display usage\n"+
				" -d [FILE]...    Decodes said files to `dout/[FILE]/*'\n"+
				" -e [FILE]...    Encodes said files to `dres/[FILE]/*'\n"+
				" -rd [IN] [OUT]  Decodes a set of files within IN into OUT\n"+
				" -re [IN] [OUT]  Encodes a set of files within IN into OUT\n\n"+
				"[FILE] refers to individual *.res files\n"+
				"[IN] and [OUT] refer to file directorys that contain *.res files\n";
    static final String SF[] = {
				"Invalid arguments\n"+
				"Usage: java -jar LayerUtil.jar -d [FILE]...\n"+
				"Type `java -jar LayerUtil.jar -h' for more information\n",
				"Invalid arguments\n"+
				"Usage: java -jar LayerUtil.jar -e [FILE]...\n"+
				"Type `java -jar LayerUtil.jar -h' for more information\n"
    };
    static final String RF[] = {
				"Invalid arguments\n"+
				"Usage: java -jar LayerUtil.jar -rd [IN] [OUT]\n"+
				"Type `java -jar LayerUtil.jar -h' for more information\n",
				"Invalid arguments\n"+
				"Usage: java -jar LayerUtil.jar -re [IN] [OUT]\n"+
				"Type `java -jar LayerUtil.jar -h' for more information\n"	
    };
    static final String NOF =
				"No options found\n"+
				"Type `java -jar LayerUtil.jar -h' for more information\n";
    static final String IVU =
				"Invalid usage of LayerUtil\n"+
				"Type `java -jar LayerUtil.jar -h' for more information\n";
    
    public static void main(String args[])
    {
				if(args.length < 1){
						System.out.print(NOF);
						System.exit(0);
				}
				
				switch(args[0]){
				case "-h": usage(); break;
				case "-d": sf(args,0); break;
				case "-e": sf(args,1); break;
				case "-rd": rf(args,0); break;
				case "-re": rf(args,1); break;
				default: 
						System.out.print(IVU);
				}
				
				System.exit(0);
    }
		
    static void usage()
    {
				System.out.print(U);
				System.exit(0);
    }
		
    static void sf(String[] args,int fl)
    {
				if(args.length < 2){
						System.out.print(SF[fl]);
						System.exit(0);
				}
				
				int i;
				Resource r;
				for(i=1;i<args.length;++i){
						try{
								System.out.println("ATTEMPT => "+ args[i]);
								r = new Resource(args[i],args[i],fl==1?"nres//":"dout//",fl==1?false:true);
								if(fl == 1){
										r.encodeall();
								} else {
										r.decodeall();
								}
						}catch(Exception e){
								System.out.print("Error loading file "+args[i]);
								e.printStackTrace();
						}
				}
    }
		
    static void rf(String[] args,int fl)
    {
				if(args.length != 3){
						System.out.print(RF[fl]);
						System.exit(0);
				}
				ArrayList<String> files = new ArrayList<String>();
				String in = args[1];
				String out = args[2].replace("\\","//")+"//";
				System.out.println("Finding files...");
				if(fl == 1)
						rcdget(new File(in),files);
				else
						rcget(new File(in),files);
				Resource r;
				for(String s : files){
						try{
								System.out.println("ATTEMPT => "+ s);
								r = new Resource(s,s.substring(s.indexOf(in)),out,fl==1?false:true);
								if(fl == 1){
										r.encodeall();
								} else {
										r.decodeall();
								}
						}catch(Exception e){
								System.out.print("Error loading file "+s);
								e.printStackTrace();
						}
				}
    }
		
    static void rcdget1(File f,ArrayList<String> fls)
    {
				if(f.isDirectory())
						if(f.getName().endsWith(".res") || f.getName().endsWith(".cache")){
								fls.add(f.getAbsolutePath());
								return;
						} else {
								for(File s : f.listFiles())
										rcdget1(s,fls);
						}
    }
    
    static void rcdget(File f,ArrayList<String> fls)
    {
				rcdget1(f,fls);
    }

    static void rcget1(File f,ArrayList<String> fls)
    {
				if(f.isFile()){
						fls.add(f.getAbsolutePath());
						return;
				}
				for(File s : f.listFiles())
						rcget1(s,fls);
    }
		
    static void rcget(File f,ArrayList<String> fls)
    {
				rcget1(f,fls);
    }
}
