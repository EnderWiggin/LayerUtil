/*
  Copyright (c) <2011> <Corey Furmanski>
  
  Permission is hereby granted, free of charge, to any person obtaining a copy of 
  this software and associated documentation files (the "Software"), to deal in the 
  Software without restriction, including without limitation the rights to use, copy,
  modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
  and to permit persons to whom the Software is furnished to do so, subject to the 
  following conditions:
  
  The above copyright notice and this permission notice shall be included in all 
  copies or substantial portions of the Software.
  
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
  PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
  FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
	String out = args[2];
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
	    if(f.getName().endsWith(".res")){
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
