package com.noterik.bart.fs.fscommand.dynamic.playlist.generators;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.noterik.bart.fs.LazyHomer;
import com.noterik.bart.fs.fscommand.dynamic.config.flash;
import com.noterik.bart.fs.fscommand.dynamic.playlist.GeneratorInterface;
import com.noterik.bart.fs.fscommand.dynamic.playlist.util.SBFReader;
import com.noterik.bart.fs.fscommand.dynamic.playlist.util.TimeLine;


/**
 * 
 * @author Daniel Ockeloen
 * 
 * Create a videoplaylist based on the layer name and filter , It generates video blocks
 * based on found blocks.
 * 
 * all events are shifted based on that (not implemented yet).
 *
 */
public class ResolutionData implements GeneratorInterface {

	/** Logger */
	private static Logger logger = Logger.getLogger(flash.class);
	private static SBFReader sbfr;
	private static boolean useOriginal = false;	
	private static Properties cachedInputs = new Properties();
	private static Properties keyMap = new Properties();
	private static Properties cachedStreams = new Properties();
	private static Properties cachedMeta = new Properties();
	private static Map<String,short[]> memoryStreams = new HashMap<String,short[]>();
	private String _INPUT = "";
	private int mb = 1024*1024;
	private int kb = 1024;
	

	/*
	 * the incoming generate call from either quickpresentation or direct restlet
	 * 
	 * @see com.noterik.bart.fs.fscommand.dynamic.playlist.GeneratorInterface#generate(org.dom4j.Node, java.lang.String)
	 */
	public Element generate(Element pr,String wantedplaylist, Element params,Element domainvpconfig,Element fsxml) {

		if (params!=null) {
			String basePath = "/springfield/smithers/data/";
			if(LazyHomer.isWindows()) {
				basePath = "c:\\springfield\\smithers\\data\\";
			}
			
			if(cachedStreams.size()==0) {
				checkDiskCache(basePath);
			}
			
			/*
			
			if(memoryStreams.size()==0) {
				
				Runnable r = new Runnable() {
					
					@Override
					public void run() {
						Enumeration e = cachedStreams.propertyNames();
						

					    while (e.hasMoreElements()) {
					    	String cacheKey = (String) e.nextElement();
					    	String binFile = cachedStreams.getProperty(cacheKey);
					    	System.out.println("Loading data stream in memory: " + binFile);
					    	RandomAccessFile file = null;
					    	try {
					    		file = new RandomAccessFile(binFile, "r");
					    		long file_size = file.length();
					    		long bytesRead = 0;
					    		short[] signal = new short[(int)file_size/2];
					    		int sample = 0;
					    		while(bytesRead<file_size) {
					    			signal[sample] = file.readShort();
					    			bytesRead = bytesRead+2;
					    			sample++;
					    		}
					    		
					    		memoryStreams.put(cacheKey, signal);
					    		signal=new short[0];
					    		file.close();
					    		
					    	}
					  	    catch(FileNotFoundException ex){
					  	    	System.out.println("File not found.");
					  	    }
					  	    catch(IOException ex){
					  	    	System.out.println(ex);
					  	    }
					    	System.out.print("..........[DONE]");
				    	}
					}
				};
				
				new Thread(r).start();
			}
			*/
			String input="";
			if(params.selectSingleNode("handlerparams/properties/input")!=null) {
				input = params.selectSingleNode("handlerparams/properties/input").getText();
			}
			
			if(input.isEmpty()) {
				return pr;
			}
			_INPUT = input;
			String action = "";
			if(params.selectSingleNode("handlerparams/properties/action")!=null) {
				action = params.selectSingleNode("handlerparams/properties/action").getText();
			}
			
			if(action.equals("load")) {
				System.out.println("RESOLUTION LOADING DATASET");
					Runnable r = new Runnable() {
					
					@Override
					public void run() {
						System.out.println("Loading data stream in memory: INPUT=" + _INPUT);
						String dir = cachedInputs.getProperty(_INPUT);
						System.out.println("Loading data stream in memory: DIR=" + dir);
						File d = new File(dir);
						File[] array = d.listFiles(new FilenameFilter() {
							
							public boolean accept(File dir, String name) {
								if(name.equals(".") || name.equals("..")) {
									return false;
								}
								if(name.toUpperCase().endsWith(".DAT")) {
									return true;
								}
								return false;
							}
						});
						

					    for(int i=0; i<array.length; i++) {
					    	String name = array[i].getName();
					    	String cacheKey = _INPUT + ":" + name.substring(0, name.lastIndexOf("."));
					    	if(memoryStreams.containsKey(cacheKey)) continue;
					    	String binFile = cachedStreams.getProperty(cacheKey);
					    	System.out.println("Loading data stream in memory: " + binFile);
					    	RandomAccessFile file = null;
					    	try {
					    		file = new RandomAccessFile(binFile, "r");
					    		byte[] buffer = new byte[2];
					    		long file_size = file.length();
					    		long bytesRead = 0;
					    		short[] signal = new short[(int)file_size/2];
					    		int sample = 0;
					    		while(bytesRead<file_size) {
					    			file.read(buffer);
					    			short val = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getShort();
					    			signal[sample] = val;
					    			bytesRead = bytesRead+2;
					    			sample++;
					    		}
					    		
					    		memoryStreams.put(cacheKey, signal);
					    		signal=new short[0];
					    		file.close();
					    		
					    	}
					  	    catch(FileNotFoundException ex){
					  	    	System.out.println("File not found.");
					  	    }
					  	    catch(IOException ex){
					  	    	System.out.println(ex);
					  	    }
					    	System.out.print("..........[DONE]");
				    	}
					}
				};
				
				new Thread(r).start();
				return pr;
			}
			
			if(action.equals("unload")) {
				Runtime runtime = Runtime.getRuntime();
				System.out.println("RESOLUTION UNLOADING DATASET");
				System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / kb);
				System.out.println("Unloading data stream from memory: " + input);
				System.out.println("MEMORY STREAMS: " + memoryStreams.size());
				Iterator it = memoryStreams.entrySet().iterator();
				while (it.hasNext()) {
			        Map.Entry pairs = (Map.Entry)it.next();
			        String cacheKey = (String)pairs.getKey();
			        String keyArr[] = cacheKey.split(":");
			        if(keyArr[0].equalsIgnoreCase(input)) {
			        	System.out.println("Unloading check key: " + cacheKey);
			        	it.remove();
			        }
			    }
				System.out.print("..........[DONE]");
				System.out.println("MEMORY STREAMS: " + memoryStreams.size());
				System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / kb);
				return pr;
			}
			
			System.out.println("RESOLUTION CREATING DATASET");
			
			int startblock = 0;
			int timedelta = 0;
			int resolution = 0;
			int blockcount = 1;
			float scale = 1f;
			int offset = 0;
			String streams = "1";
			
			String dataset = "";
			if(params.selectSingleNode("handlerparams/properties/dataset")!=null) {
				dataset = params.selectSingleNode("handlerparams/properties/dataset").getText();
			}
			
			if(params.selectSingleNode("handlerparams/properties/scale")!=null) {
				scale = Float.parseFloat(params.selectSingleNode("handlerparams/properties/scale").getText());
			}
			
			if(params.selectSingleNode("handlerparams/properties/offset")!=null) {
				offset = Integer.parseInt(params.selectSingleNode("handlerparams/properties/offset").getText());
			}
			
			if(params.selectSingleNode("handlerparams/properties/original")!=null) {
				useOriginal = Boolean.parseBoolean(params.selectSingleNode("handlerparams/properties/original").getText());
			}
			startblock = Integer.parseInt(params.selectSingleNode("handlerparams/properties/startblock").getText());
			timedelta = Integer.parseInt(params.selectSingleNode("handlerparams/properties/timedelta").getText());
			resolution = Integer.parseInt(params.selectSingleNode("handlerparams/properties/resolution").getText());
			blockcount = Integer.parseInt(params.selectSingleNode("handlerparams/properties/blockcount").getText());
			
			if(params.selectSingleNode("handlerparams/properties/streams")!=null) {
				streams = params.selectSingleNode("handlerparams/properties/streams").getText();
			}
			
			boolean forceResample = false;
			if(params.selectSingleNode("handlerparams/properties/resample")!=null) {
				forceResample = Boolean.parseBoolean(params.selectSingleNode("handlerparams/properties/resample").getText());
			}
			
			if(forceResample) {
				cachedStreams = new Properties();
			}
			
			System.out.println("DATASET="+dataset+" STARTTIME="+startblock+" TIMEDELTA="+timedelta+" RESOLUTION="+resolution+" BLOCKCOUNT="+blockcount+" INPUT="+input+" STREAMS="+streams);
			
			
			String sbfFile = null;
			String[] sbfFiles = new String[0];
			
			String[] streamArr = streams.split(",");
			
			List<String> list = new ArrayList<String>();

		    for(String s : streamArr) {
		       if(s != null && s.length() > 0) {
		          list.add(s);
		       }
		    }
		    //Fix the stream array if it contains empty or null elements
		    streamArr = list.toArray(new String[list.size()]);
			list = null;

			
			boolean loadSBFile = false;
			/*
			for(String cStream : streamArr) {
				cStream = cStream.trim();
				String cacheKey = cStream;
				String stream = cacheKey.split("_")[1];
				String binFile = basePath + input + "_" + stream + ".dat";
				if(!cachedStreams.containsKey(cacheKey)) { 
					loadSBFile = true;
				}
			}
			*/
			if(loadSBFile) {
				if(sbfr==null) {
					sbfr = new SBFReader(sbfFile);
				} else {
					String currentFile = sbfr.getDataFile().getFilename();
					if(!currentFile.equals(sbfFile)) {
						sbfr = null;
						sbfr = new SBFReader(sbfFile);
					}
				}
			
				
				for(String cStream : streamArr) {
					cStream = cStream.trim();
					String cacheKey = cStream;
					String stream = cacheKey.split("_")[1];
					String binFile = basePath + input + "_" + stream + ".dat";
					//String binFile = basePath + cacheKey + ".dat";
					if(!cachedStreams.containsKey(cacheKey)) { 
						int iStream = Integer.parseInt(stream);
						//handleSampling(iStream, binFile);
					} else {
						System.out.println("DISK CACHE EXISTS: " + binFile);
					}
					
				}
			}
			
			
			TimeLine timeline = new TimeLine(pr);
			
			// now lets insert out datablocks // ok check is weird i am lazy
			for (String curStream : streamArr) {
				curStream = curStream.trim();
				String cacheKey = input + ":" + curStream;
				String keyMap_key = input + ":" + curStream;
				if(keyMap.containsKey(keyMap_key)) {
					cacheKey = keyMap.getProperty(keyMap_key);
				}
				
				for(int i=1; i<=blockcount; i++) {
					timeline.insertNode(createDataNode(i,startblock+(i-1),timedelta,resolution,curStream, scale, cacheKey, offset));
					
				}
			}
			// ok we are all done so put all the events into the presentation
			timeline.insertNodesInPresentation();
			
		}

		return pr;
	}
	/*
	private void handleGroupFiles(String streams) {
		String basePath = "/springfield/smithers/data/";
		if(LazyHomer.isWindows()) {
			basePath = "c:\\springfield\\smithers\\data\\";
		}
		String[] streamArr = streams.split(",");
		List<String> list = new ArrayList<String>();

	    for(String s : streamArr) {
	       if(s != null && s.length() > 0) {
	          list.add(s);
	       }
	    }
	    //Fix the stream array if it contains empty or null elements
	    streamArr = list.toArray(new String[list.size()]);
	    
		for(String cStream : streamArr) {
			cStream = cStream.trim();
			String input = cStream.split("_")[0];
			String sbfFile = null;
			switch (inputs.valueOf(input)) {
				case analog:
					sbfFile = basePath + "9812109________001_014__export_file_set4.sbf";
					break;
				case analog2:
					sbfFile = basePath + "9812109________001_014__export_file_set2.sbf";
					break;
				case analog23:
					sbfFile = basePath + "9812109________001_014__export_file_set3.sbf";
					break;
				case analog3:
					sbfFile = basePath + "9812109________001_014__export_file_set1.sbf";
					break;
				case digital:
					sbfFile = basePath + "9812109________001_014__export_file_set5.sbf";
					break;
				case analogfiltered:
					sbfFile = basePath + "9812109________001_014__export_filtered_set4.sbf";
					break;
				case analog2filtered:
					sbfFile = basePath + "9812109________001_014__export_filtered_set2.sbf";
					break;
				case analog23filtered:
					sbfFile = basePath + "9812109________001_014__export_filtered_set3.sbf";
					break;
				case analog3filtered:
					sbfFile = basePath + "9812109________001_014__export_filtered_set1.sbf";
					break;
				case digitalfiltered:
					sbfFile = basePath + "9812109________001_014__export_filtered_set5.sbf";
					break;
				default:
					sbfFile = basePath + "9812109________001_014__export_filtered_set2.sbf";
					break;
			}
			
			boolean loadSBFile = false;
			
			String cacheKey = cStream;
			String binFile = basePath + cacheKey + ".dat";
			if(!cachedStreams.containsKey(cacheKey)) { 
				loadSBFile = true;
			}
			
			
			if(loadSBFile) {
				if(sbfr==null) {
					sbfr = new SBFReader(sbfFile);
				} else {
					String currentFile = sbfr.getDataFile().getFilename();
					if(!currentFile.equals(sbfFile)) {
						sbfr = null;
						sbfr = new SBFReader(sbfFile);
					}
				}
			
				String stream = cacheKey.split("_")[1];
				if(!cachedStreams.containsKey(cacheKey)) { 
					int iStream = Integer.parseInt(stream);
					handleSampling(iStream, binFile);
				} else {
					System.out.println("DISK CACHE EXISTS: " + binFile);
				}
			}
		}
	}
	*/
	/*
	private void handleSampling(int iStream, String binFile) {
		System.out.println("HANDLE SMAPLING FOR FILE: " + binFile);
		String cacheKey = binFile.substring(binFile.lastIndexOf(System.getProperty("file.separator"))+1, binFile.lastIndexOf("."));
		String[] keys = cacheKey.split("_");
		String input = keys[0];
		// routine downsamples creates a value for every ms (so 1khz outout).
		int frequency = sbfr.getDataStreamFrequency();
		
		int scale = 1;
		switch (inputs.valueOf(input)) {
			case run1:
			case run2:
				scale = 4;
				break;
			case analogfiltered:
			case analog2filtered:
			case analog23filtered:
			case analog3filtered:
			case digitalfiltered:
				scale = 1;
				break;
			default:
				scale = 1;
				break;
		}
		frequency = scale*frequency;
		System.out.println("HANDLE SMAPLING FREQ: " + frequency);
		
		if(frequency>5100) {
			loadAndDownsampleSBF(iStream, binFile);
		} else if (frequency< 4900) {
			loadAndUpsampleSBF(iStream, binFile);
		} else {
			loadSBF(iStream, binFile);
		}
	}
	*/
	private Element createDataNode(int id,int startblock,int timedelta,int resolution, String stream, float scale, String cacheKey, int offset) {
		
		if(!cachedStreams.containsKey(cacheKey) && !memoryStreams.containsKey(cacheKey)) {
			return null;
		}
		
		if(!cachedMeta.containsKey(cacheKey)) {
			createMetaInfo(cacheKey);
		}
		
		float multiplier = 0f;
		float min = 0f;
		if(useOriginal) {
			String metainfo = cachedMeta.getProperty(cacheKey);
			String[] meta = metainfo.split(",");
			multiplier = Float.parseFloat(meta[2]);
			min = Float.parseFloat(meta[0]);
		}
		
		boolean useMemory = false;
		if(memoryStreams.containsKey(cacheKey)) useMemory = true;
		
		String binFile = cachedStreams.getProperty(cacheKey);
		Element datanode = DocumentHelper.createElement("resolutiondata");
		RandomAccessFile file = null;
    	try {
    		if(!useMemory) {
    			file = new RandomAccessFile(binFile, "r");
    		}

			// set the id and aim it to our original video
			datanode.addAttribute("id", ""+id);
			datanode.addAttribute("input", cacheKey.split(":")[0]);
			datanode.addAttribute("stream", ""+cacheKey.split(":")[1]);
			
			//Send meta information
			// create the properties and set them (this can be done easer?)
			Element p = DocumentHelper.createElement("properties");
			Element st = DocumentHelper.createElement("starttime");
			Element du = DocumentHelper.createElement("duration");
			Element meta = DocumentHelper.createElement("metainfo");
			Element dataset = DocumentHelper.createElement("dataset");
			String body = "";
			int sampleCount = 0;
			int starttime = startblock*(timedelta*resolution);
			
			if(!useMemory) {
				System.out.println("READING FROM DISKCACHE");
				long file_size = file.length();
				
				//int newtimedelta =  Math.round(timedelta*scale);
				//int newstarttime =startblock*(newtimedelta*resolution);
				
				
				if(offset>0 && id==1) {
					for(int i=0; i<(offset/timedelta); i++) {
						body+="0;";
						sampleCount++;
						resolution--;
					}
				}
				
				if(offset>0 && id!=1) {
					starttime = starttime - offset;
				}
				byte[] buffer = new byte[2];
				for (int i=0;i<resolution;i++) {
					long pos = starttime+(i*timedelta); 
					pos = pos*5; //*5 since our file is 5KHz
					pos = pos*Math.round(scale);
					pos = pos*2;
					if(pos<file_size) {
						sampleCount++;
						file.seek(pos);
						
						file.read(buffer);
						short val = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getShort();
						if(useOriginal) {
							float oVal = (val/multiplier) + min;
							body+=oVal+";";
						} else {
							body+=val+";";
						}
						
					} else {
						body+="0;";
					}
				}
	
		    	file.close();
			} else {
				System.out.println("READING FROM MEMORY");
				short[] signal = memoryStreams.get(cacheKey);
				int signal_size = signal.length;
				if(offset>0 && id==1) {
					for(int i=0; i<(offset/timedelta); i++) {
						body+="0;";
						sampleCount++;
						resolution--;
					}
				}
				
				if(offset>0 && id!=1) {
					starttime = starttime - offset;
				}
				for (int i=0;i<resolution;i++) {
					int pos = starttime+(i*timedelta);
					pos = pos*5; //*5 since our file is 5KHz
					pos = pos*Math.round(scale);
					if(pos<signal_size) {
						sampleCount++;
						//System.out.println("SAMPLE: " + pos);
						short val = signal[pos];
						if(useOriginal) {
							float oVal = (val/multiplier) + min;
							body+=oVal+";";
						} else {
							body+=val+";";
						}
					} else {
						body+="0;";
					}
					
				}
				signal = null;
			}
	    	
	    	String metaBody = cachedMeta.getProperty(cacheKey);
	    	metaBody += "," + sampleCount;
	    	meta.setText(metaBody);

			dataset.setText(body);

			st.setText(""+starttime);
			du.setText(""+(timedelta*resolution));
			
			
			p.add(st);
			p.add(du);
			p.add(meta);
			p.add(dataset);
			
			// add the properties to the video node so it plays just that part.
			datanode.add(p);
    	}
  	    catch(FileNotFoundException ex){
  	    	System.out.println("File not found.");
  	    }
  	    catch(IOException ex){
  	    	System.out.println(ex);
  	    }
		return datanode;
	}
	
	private void createMetaInfo(String cacheKey) {
		System.out.println("Creating meta information: " + cacheKey);
		String[] keys = cacheKey.split(":");
		String input = keys[0];
		String stream = keys[1];
		
		String basePath = "/springfield/smithers/data/";
		if(LazyHomer.isWindows()) {
			basePath = "c:\\springfield\\smithers\\data\\";
		}
		
		String dataFile = basePath + input + System.getProperty("file.separator") + stream + ".meta";
		
		File file = new File(dataFile);
		if(!file.exists()) {
			return;
		}
		FileInputStream in = null;
		StringBuffer str = new StringBuffer("");
		try {
			int ch;
			
			in = new FileInputStream(file);
									
			/* read text */
			while ((ch = in.read()) != -1) {
				str.append((char)ch);
			}
			in.close();
		} catch(Exception e) {
			System.out.println(e);
		}
		
		cachedMeta.put(cacheKey, str.toString());		
	}
	
	/*
	private void loadAndDownsampleSBF(int stream, String binFile) {
		System.out.println("DONWSAMPLING in a File: " + binFile);
		logger.info("DONWSAMPLING in a File: " + binFile);
		String cacheKey = binFile.substring(binFile.lastIndexOf(System.getProperty("file.separator"))+1, binFile.lastIndexOf("."));
		String[] keys = cacheKey.split("_");
		String input = keys[0];
		// routine downsamples creates a value for every ms (so 1khz outout).
		int frequency = sbfr.getDataStreamFrequency();
		
		int scale = 1;
		switch (inputs.valueOf(input)) {
			case run1:
			case run2:
				scale = 4;
				break;
			case analogfiltered:
			case analog2filtered:
			case analog23filtered:
			case analog3filtered:
			case digitalfiltered:
				scale = 1;
				break;
			default:
				scale = 1;
				break;
		}
		frequency = scale*frequency;
		//we have to go to 5KHz
		int downSample = frequency/5000;
		
		float[] timesignal = sbfr.getColsData(1);
		
		float[] wave = sbfr.getColsData(stream+1);
		float[] minmax = sbfr.getDataFile().getMinMax(stream);		
		
		float min = minmax[0];
		float max = minmax[1];
		float multiplier = 2048/(max-min);
		
		RandomAccessFile file = null;
    	try {
    		file = new RandomAccessFile(binFile, "rw");
  	    }
  	    catch(FileNotFoundException ex){
  	    	System.out.println("File not found.");
  	    }
  	    catch(IOException ex){
  	    	System.out.println(ex);
  	    }
	
		
    	float s11 = wave[0];
    	int sample = 0;
    	int[] signal = new int[timesignal.length/(downSample-1)];
    	signal[sample] = Math.round((s11-min)*multiplier);
    	try {
			file.writeShort(new Integer(signal[sample]).shortValue());
			//output.write();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	for(int i=1; i<timesignal.length; i++) {
    		float sum = 0f;
    		for(int j=0; j<downSample;j++) {
    			if((i+j)>=timesignal.length) {
    				sample++;
    	    		signal[sample] =  Math.round(sum/(j+1));
    	    		try {
        				file.writeShort(new Integer(signal[sample]).shortValue());
        				//output.write();
        			} catch (IOException e) {
        				// TODO Auto-generated catch block
        				e.printStackTrace();
        			}
    	    		//System.out.println("Final TIME="+timesignal[i+j-1]+" Sample="+sample+" Value=" + signal[sample]);
    	    		break;
    			} else {
    				sum += (wave[i+j]-min)*multiplier;
    			}
    			
    			
    		}
    		i=i+downSample-1;
    		if(i<timesignal.length) {
    			sample++;
    			signal[sample] =  Math.round(sum/downSample);
    			try {
    				file.writeShort(new Integer(signal[sample]).shortValue());
    				//output.write();
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    			//System.out.println("TIME="+timesignal[i]+" Sample="+sample+" Value=" + signal[sample]);
    		}
    		
	    }
    	
    	try {
    		file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	if(!cachedMeta.containsKey(cacheKey)) {
    		String dataMeta = Float.toString(min) + "," + Float.toString(max) + "," + Float.toString(multiplier) + "," + sbfr.getDataFile().getDataColumns(stream) + "," + sbfr.getDataFile().getUnitColumns(stream);
    		cachedMeta.put(cacheKey, dataMeta);
    	}
    	
    	cachedStreams.put(cacheKey, binFile);
    	final String curFile = binFile;
    	final String curCacheKey = cacheKey;
    	if(!memoryStreams.containsKey(cacheKey)) {
    		Runnable r = new Runnable() {
    			
				@Override
				public void run() {
					
				    	System.out.println("Loading data stream in memory: " + curFile);
				    	RandomAccessFile file = null;
				    	try {
				    		file = new RandomAccessFile(curFile, "r");
				    		long file_size = file.length();
				    		long bytesRead = 0;
				    		short[] signal = new short[(int)file_size/2];
				    		int sample = 0;
				    		while(bytesRead<file_size) {
				    			signal[sample] = file.readShort();
				    			bytesRead = bytesRead+2;
				    			sample++;
				    		}
				    		
				    		memoryStreams.put(curCacheKey, signal);
				    		signal=new short[0];
				    		file.close();
				    		
				    	}
				  	    catch(FileNotFoundException ex){
				  	    	System.out.println("File not found.");
				  	    }
				  	    catch(IOException ex){
				  	    	System.out.println(ex);
				  	    }
				    	System.out.print("..........[DONE]");
				}
			};
			
			new Thread(r).start();
    	}
	}
	
	private void loadAndUpsampleSBF(int stream, String binFile) {
		System.out.println("UPSAMPLING in a File: " + binFile);
		logger.info("UPSAMPLING in a File: " + binFile);
		String cacheKey = binFile.substring(binFile.lastIndexOf(System.getProperty("file.separator"))+1, binFile.lastIndexOf("."));
		String[] keys = cacheKey.split("_");
		String input = keys[0];
		// routine upsamples creates a value for every ms (so 1khz outout).
		float[] timesignal = sbfr.getColsData(1);
		float[] wave = sbfr.getColsData(stream+1);

		float[] minmax = sbfr.getDataFile().getMinMax(stream);		
		
		float min = minmax[0];
		float max = minmax[1];
		float multiplier = 2048/(max-min);
		
		int frequency = sbfr.getDataStreamFrequency();
		//we have to go to 5KHz
		int scale = 1;
		switch (inputs.valueOf(input)) {
			case run1:
			case run2:
				scale = 4;
				break;
			case analogfiltered:
			case analog2filtered:
			case analog23filtered:
			case analog3filtered:
			case digitalfiltered:
				scale = 1;
				break;
			default:
				scale = 1;
				break;
		}
		frequency = scale*frequency;
		int dtime = 5000/frequency;
		
		int stime1 = Math.round(timesignal[0]*1000);
    	float s11 = wave[0];
    	int sample = 0;
    	int[] signal = new int[timesignal.length*dtime];
    	
    	RandomAccessFile file = null;
    	try {
    		file = new RandomAccessFile(binFile, "rw");
  	    }
  	    catch(FileNotFoundException ex){
  	    	System.out.println("File not found.");
  	    }
  	    catch(IOException ex){
  	    	System.out.println(ex);
  	    }
    	
    	for(int i=1; i<timesignal.length; i++) {

    		int stime2 = Math.round(timesignal[i]*1000);
    		
	    	float s12 = wave[i];
	    	
	    	// create the R/C's per signal
	    	
	    	float rc1 = (s12-s11)/dtime;
	    	// now we should be able to loop to S2 time
	    	for (int k=0;k<dtime;k++) {
	    		float u1 = s11 + (rc1*k);

	    		signal[sample] =  Math.round((u1-min)*multiplier);
	    		try {
    				file.writeShort(new Integer(signal[sample]).shortValue());
    				//output.write();
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
	    		// shift to the next sample
	    		sample++;
	    		
	    		//System.out.println("TIME="+stime1+" Sample="+sample+" Value="+Float.toString(u1));
	    	}
	    	
	    	// lets switch set 1 with 2
	    	stime1 = stime2;
	    	s11 = s12;
	    }
    	
    	try {
    		file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	if(!cachedMeta.containsKey(cacheKey)) {
    		String dataMeta = Float.toString(min) + "," + Float.toString(max) + "," + Float.toString(multiplier) + "," + sbfr.getDataFile().getDataColumns(stream) + "," + sbfr.getDataFile().getUnitColumns(stream);
    		cachedMeta.put(cacheKey, dataMeta);
    	}
    	
    	cachedStreams.put(cacheKey, binFile);
    	final String curFile = binFile;
    	final String curCacheKey = cacheKey;
    	if(!memoryStreams.containsKey(cacheKey)) {
    		Runnable r = new Runnable() {
    			
				@Override
				public void run() {
					
				    	System.out.println("Loading data stream in memory: " + curFile);
				    	RandomAccessFile file = null;
				    	try {
				    		file = new RandomAccessFile(curFile, "r");
				    		long file_size = file.length();
				    		long bytesRead = 0;
				    		short[] signal = new short[(int)file_size/2];
				    		int sample = 0;
				    		while(bytesRead<file_size) {
				    			signal[sample] = file.readShort();
				    			bytesRead = bytesRead+2;
				    			sample++;
				    		}
				    		
				    		memoryStreams.put(curCacheKey, signal);
				    		signal=new short[0];
				    		file.close();
				    		
				    	}
				  	    catch(FileNotFoundException ex){
				  	    	System.out.println("File not found.");
				  	    }
				  	    catch(IOException ex){
				  	    	System.out.println(ex);
				  	    }
				    	System.out.print("..........[DONE]");
				}
			};
			
			new Thread(r).start();
    	}
	}
	
	private void loadSBF(int stream, String binFile) {
		logger.info("NO RESAMPLING for File: " + binFile);
		String cacheKey = binFile.substring(binFile.lastIndexOf(System.getProperty("file.separator"))+1, binFile.lastIndexOf("."));
		// routine upsamples creates a value for every ms (so 1khz outout).
		float[] timesignal = sbfr.getColsData(1);
		float[] wave = sbfr.getColsData(stream+1);

		float[] minmax = sbfr.getDataFile().getMinMax(stream);		
		
		float min = minmax[0];
		float max = minmax[1];
		float multiplier = 2048/(max-min);
		
		RandomAccessFile file = null;
    	try {
    		file = new RandomAccessFile(binFile, "rw");
  	    }
  	    catch(FileNotFoundException ex){
  	    	System.out.println("File not found.");
  	    }
  	    catch(IOException ex){
  	    	System.out.println(ex);
  	    }

    	int[] signal = new int[timesignal.length]; 
    	for(int i=0; i<timesignal.length; i++) {
    		float s = wave[i];
    		signal[i] =  Math.round((s-min)*multiplier);
    		try {
				file.writeShort(new Integer(signal[i]).shortValue());
				//output.write();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
    	
    	try {
    		file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    	
    	if(!cachedMeta.containsKey(cacheKey)) {
    		String dataMeta = Float.toString(min) + "," + Float.toString(max) + "," + Float.toString(multiplier) + "," + sbfr.getDataFile().getDataColumns(stream) + "," + sbfr.getDataFile().getUnitColumns(stream);
    		cachedMeta.put(cacheKey, dataMeta);
    	}
    	
    	cachedStreams.put(cacheKey, binFile);
    	final String curFile = binFile;
    	final String curCacheKey = cacheKey;
    	if(!memoryStreams.containsKey(cacheKey)) {
    		Runnable r = new Runnable() {
    			
				@Override
				public void run() {
					
				    	System.out.println("Loading data stream in memory: " + curFile);
				    	RandomAccessFile file = null;
				    	try {
				    		file = new RandomAccessFile(curFile, "r");
				    		long file_size = file.length();
				    		long bytesRead = 0;
				    		short[] signal = new short[(int)file_size/2];
				    		int sample = 0;
				    		while(bytesRead<file_size) {
				    			signal[sample] = file.readShort();
				    			bytesRead = bytesRead+2;
				    			sample++;
				    		}
				    		
				    		memoryStreams.put(curCacheKey, signal);
				    		signal=new short[0];
				    		file.close();
				    		
				    	}
				  	    catch(FileNotFoundException ex){
				  	    	System.out.println("File not found.");
				  	    }
				  	    catch(IOException ex){
				  	    	System.out.println(ex);
				  	    }
				    	System.out.print("..........[DONE]");
				}
			};
			
			new Thread(r).start();
    	}
	}
	*/
	/* get all files from a directory */
	private void checkDiskCache(String directory) {
		File d = new File(directory);
		File[] array = d.listFiles(new FilenameFilter() {
			
			public boolean accept(File dir, String name) {
				if(name.equals(".") || name.equals("..")) {
					return false;
				}
				
				File cDir = new File(dir.getAbsolutePath()  + System.getProperty("file.separator") + name);
				if (cDir.isDirectory()) {
					System.out.println("cachedInputs: " + name);
					cachedInputs.put(name, cDir.getAbsolutePath());
					return true;
				}
				return false;
			}
		});
		for(int i=0; i<array.length; i++) {
			array[i].listFiles(new FilenameFilter() {
				
				public boolean accept(File dir, String name) {
					if(name.equals(".") || name.equals("..")) {
						return false;
					}
					if(name.toUpperCase().endsWith(".DAT")) {
						String cacheKey = dir.getName() + ":" + name.substring(0, name.lastIndexOf("."));
						String streamId = name.substring(name.lastIndexOf("_")+1);
						streamId = streamId.substring(0, streamId.lastIndexOf("."));
						String keyMap_key = dir.getName() + ":" + streamId;
						System.out.println("keyMap key: " + keyMap_key + "; cacheKey: " + cacheKey);
						keyMap.put(keyMap_key, cacheKey);
						cachedStreams.put(cacheKey, dir.getAbsolutePath() + System.getProperty("file.separator") + name);
					}
					return false;
				}
			});
		}
	}
}
