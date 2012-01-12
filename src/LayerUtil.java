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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.*;

public class LayerUtil
{
		static final String VERSION = "LayerUtil version - 2.0.0";
    static final String U = 
				"Usage: java -jar LayerUtil.jar [MODIFIER_FLAGS] [ENC/DEC_OPTION] [OPTION ARGS]\n"+
				"Possible modifier flags include:\n"+
				" -ns             Makes all `new' dec/enc meta skip value: noskip(0)\n"+
				"                  default skip value is: skip(1)\n"+
				" -fs#            Forces the skip value which is # [0=noskip;1=skip]\n"+
				" -np             Ignore any prints to cmd aside from errors\n"+
				"Possible ENC/DEC Options:\n"+
				" -h              Display usage\n"+
				" -v              Displays version number\n"+
				" -d [FILE]...    Decodes said files to `dout/[FILE]/*'\n"+
				" -e [FILE]...    Encodes said files to `dres/[FILE]/*'\n"+
				" -rd [IN] [OUT]  Decodes a set of files within IN into OUT\n"+
				" -re [IN] [OUT]  Encodes a set of files within IN into OUT\n\n"+
				"[FILE] refers to individual *.res files\n"+
				"[IN] and [OUT] refer to file directorys that contain *.res files\n";
    static final String SF[] = {
				"Invalid arguments\n"+
				"Usage: java -jar LayerUtil.jar [MOD_FLAGS] -d [FILE]...\n"+
				"Type `java -jar LayerUtil.jar -h' for more information\n",
				"Invalid arguments\n"+
				"Usage: java -jar LayerUtil.jar [MOD_FLAGS] -e [FILE]...\n"+
				"Type `java -jar LayerUtil.jar -h' for more information\n"
    };
    static final String RF[] = {
				"Invalid arguments\n"+
				"Usage: java -jar LayerUtil.jar [MOD_FLAGS] -rd [IN] [OUT]\n"+
				"Type `java -jar LayerUtil.jar -h' for more information\n",
				"Invalid arguments\n"+
				"Usage: java -jar LayerUtil.jar [MOD_FLAGS] -re [IN] [OUT]\n"+
				"Type `java -jar LayerUtil.jar -h' for more information\n"	
    };
    static final String NOF =
				"No options found\n"+
				"Type `java -jar LayerUtil.jar -h' for more information\n";
    static final String IVU =
				"Invalid usage of LayerUtil\n"+
				"Type `java -jar LayerUtil.jar -h' for more information\n";
		static final String
				SKIP		= "1",
				NOSKIP	= "0";

		static Map<String,String> skip = new HashMap<String,String>();
    static String skip_value = SKIP;
		static boolean pflag = true;
		static boolean fsflag = false;

    public static void main(String args[])
    {
				try{
				if(args.length < 1){
						System.out.print(NOF);
						System.exit(0);
				}
				int i = 0;
				boolean done = false;
				for(i=0;i<args.length;++i){
						switch(args[i]){
						case "-fs0": fsflag = true; skip_value = NOSKIP; break;
						case "-fs1": fsflag = true; skip_value = SKIP; break;
						case "-ns": skip_value = NOSKIP; break;
						case "-np": pflag = false; break;
						case "-v":
						case "-h": 
						case "-d": 
						case "-e": 
						case "-rd": 
						case "-re": done = true; break;		
						}
						if(done) break;
				}haven.Resource.skip_value = skip_value;

				switch(args[i]){
				case "-v": System.out.print(VERSION); System.exit(0); break;
				case "-h": usage(); break;
				case "-d": sf(i,args,0); break;
				case "-e": sf(i,args,1); break;
				case "-rd": rf(i,args,0); break;
				case "-re": rf(i,args,1); break;
				default: 
						System.out.print(IVU);
				}
				}catch(Exception e){
						e.printStackTrace();
						System.exit(1); 
				}
				
				System.exit(0);
    }
		
    static void usage()
    {
				System.out.print(U);
				System.exit(0);
    }
		
    static void sf(int st,String[] args,int fl)
    {
				if(args.length < 2){
						System.out.print(SF[fl]);
						System.exit(0);
				}
				
				int i;
				Resource r;
				for(i=st+1;i<args.length;++i){
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
		
    static void rf(int st,String[] args,int fl) throws Exception
    {
				if(args.length < st+3){
						System.out.print(RF[fl]);
						System.exit(0);
				}
				BufferedWriter bw = null;
				ArrayList<String> files = new ArrayList<String>();
				String in = args[st+1];
				int in_len = in.length();
				String out = args[st+2].replace("\\","//")+"//";
				if(pflag) System.out.println("Finding files...");
				if(fl == 1){
						if(new File(out+"//meta").exists()){
								if(pflag) System.out.println("Reading [out] meta...");
								BufferedReader br = new BufferedReader(new FileReader(out+"//meta"));
								String ln,obs[];
								while((ln=br.readLine()) != null) {
										obs = ln.toLowerCase().split("=");
										skip.put(obs[0],obs[1]);
								}
								br.close();
						} else {
								new File(out).mkdirs();
								new File(out+"//meta").createNewFile();
						}
						bw = new BufferedWriter(new FileWriter(out+"//meta",false));
						rcdget(new File(in),files);
				}
				else {
						if(new File(in+"//meta").exists()){
								if(pflag) System.out.println("Reading [in] meta...");
								BufferedReader br = new BufferedReader(new FileReader(in+"//meta"));
								String ln,obs[];
								while((ln=br.readLine()) != null) {
										obs = ln.toLowerCase().split("=");
										if(obs[1].equals(SKIP)){
												skip.put(obs[0],obs[1]);
										}
								}
								br.close();
						}
						rcget(new File(in),files);
				}
				String sk;
				String name;
				Resource r;
				for(String s : files){
						try{
								name = s.substring(s.indexOf(in)+in_len);
								if(pflag) System.out.println("ATTEMPT => "+ name);
								if(fl == 0 && skip.get(name) != null){
										if(pflag) System.out.println("->SKIPED");
										continue;
								} else if (fl==1) {
										BufferedReader br = new BufferedReader(new FileReader(s+"//meta"));
										haven.Utils.rnint(br);
										if(haven.Utils.rnint(br) == 1){
												if(pflag) System.out.println("->SKIPED");
												continue;
										}
										if(fsflag){
												if(pflag) System.out.println("->FORCE SKIP " + skip_value);
												skip.remove(name);
												bw.write(name+"="+skip_value+"\r\n");
										} else if((sk=skip.get(name)) != null){
												if(pflag) System.out.println("->PREVIOUS SKIP ENTRY " + sk);
												bw.write(name+"="+sk+"\r\n");
												skip.remove(name);
										} else {
												if(pflag) System.out.println("->NEW SKIP ENTRY " + skip_value);
												bw.write(name+"="+skip_value+"\r\n");
										}
										bw.flush();
								}

								r = new Resource(s,"//"+s.substring(s.indexOf(in)+in_len),out,fl==1?false:true);
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
				if(fl == 1){
						Iterator<String> it = skip.keySet().iterator();
						while(it.hasNext()){
								out = it.next();
								bw.write(out+"="+skip.get(out)+"\r\n");
						}
				}
				if(bw != null){
						bw.close();
				}
    }
		
    static void rcdget1(File f,ArrayList<String> fls)
    {
				if(f.isDirectory())
						if(f.getName().endsWith(".res") || f.getName().endsWith(".cache")){
								fls.add(f.getPath());
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
				if(f.isFile() && (f.getName().endsWith(".res") || f.getName().endsWith(".cache"))){
						fls.add(f.getPath());
						return;
				}
				if(f.isDirectory())
						for(File s : f.listFiles())
								rcget1(s,fls);
    }
		
    static void rcget(File f,ArrayList<String> fls)
    {
				rcget1(f,fls);
    }
}
