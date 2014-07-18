/* 
* SBFReader.java
* 
* Copyright (c) 2012 Noterik B.V.
* 
* This file is part of smithers, related to the Noterik Springfield project.
*
* Smithers is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Smithers is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Smithers.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.noterik.bart.fs.fscommand.dynamic.playlist.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SBFReader {
	
	private static final int nHeader = 64;
	private static final int nColHeader = 32;
	private static final int nUnitHeader = 16;
	private static final int nPointers = 8;
	private static final int nData = 8;
	
	private static SBFile dataFile = new SBFile();
	
	public SBFReader(String sbfFile) {
		
		dataFile.setFilename(sbfFile);
		
		String header = readHeader();
		
		getSBFVersion(header);
		
		if(dataFile.getVersion().equals("")) {
			System.out.println("Unknown file format found or new version");
			return;
		}
		
		readColumns();
		
		//Read the timesignal column always.
		getColsData(1);
		setDataStreamFrequency();
	}
	
	public SBFile getDataFile() {
		return dataFile;
	}
	
	public String readHeader() {
		
		byte[] buffer = new byte[nHeader];
		String output = "";
		int offset = 0;
		try {
			String filename = dataFile.getFilename();
			RandomAccessFile file = new RandomAccessFile(filename, "r");
			
			file.read(buffer);
			output = new String(buffer); //Read the header
            
            //Determine the number of columns
            //Read first column header
            //offset += nHeader;
            buffer = new byte[nColHeader];
            file.read(buffer);
            String col1Name = new String(buffer);
            col1Name = col1Name.trim();
            
            //Read first column unit line
            //offset += nColHeader;
            buffer = new byte[nUnitHeader];
            file.read(buffer);
            String col1Unit = new String(buffer);
            col1Unit = col1Unit.trim();
            
            
            //offset += nUnitHeader;
            buffer = new byte[nPointers];
            file.read(buffer);
            long value = 0;
            for (int i = 0; i < buffer.length; i++)
            {
               value += ((long) buffer[i] & 0xffL) << (8 * i);
            }
            String strStartByte = new String(buffer);
            long byteStart1 = value;
            
            //offset += 1;
            buffer = new byte[nData];
            file.read(buffer);
            value = 0;
            for (int i = 0; i < buffer.length; i++)
            {
               value += ((long) buffer[i] & 0xffL) << (8 * i);
            }
            long nRow1 = value;
            long nCols = (byteStart1 - nHeader)/(nColHeader + nUnitHeader + 2*nPointers);
            dataFile.setColumnCount(nCols);
            file.close();
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		output = output.trim();
        return output;
		
	}
	
	public void readColumns() {
		
		byte[] buffer;
		
		String filename = dataFile.getFilename();
		long nCols = dataFile.getColumnCount();	
		try {
			RandomAccessFile file = new RandomAccessFile(filename, "r");
			for(int iCol=1; iCol<=(int) nCols; iCol++) {
				int nArrIdx = iCol-1;
				int jump = nHeader + (iCol - 1)*(nColHeader + nUnitHeader + 2*nPointers);
				file.seek(0);
				file.seek(jump);
				
	            buffer = new byte[nColHeader];
	            file.read(buffer);
	            String colName = new String(buffer);
	            colName = colName.trim();
	            dataFile.setDataColumns(nArrIdx, colName);
	            
	            //Read first column unit line
	            //offset += nColHeader;
	            buffer = new byte[nUnitHeader];
	            file.read(buffer);
	            String colUnit = new String(buffer);
	            colUnit = colUnit.trim();
	            dataFile.setUnitColumns(nArrIdx, colUnit);
	            
	            //offset += nUnitHeader;
	            buffer = new byte[nPointers];
	            file.read(buffer);
	            long value = 0;
	            for (int i = 0; i < buffer.length; i++)
	            {
	               value += ((long) buffer[i] & 0xffL) << (8 * i);
	            }
	            String strStartByte = new String(buffer);
	            dataFile.setStartBytes(nArrIdx, value);
	            
	            //offset += 1;
	            buffer = new byte[nData];
	            file.read(buffer);
	            value = 0;
	            for (int i = 0; i < buffer.length; i++)
	            {
	               value += ((long) buffer[i] & 0xffL) << (8 * i);
	            }
	            dataFile.setRows(nArrIdx, value);
			}
            file.close();
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void getSBFVersion(String header) {
		
		Pattern pat = Pattern.compile("version ([0-9\\.]+)");
		Matcher m = pat.matcher(header);
		while (m.find()) {
			String sVersion = m.group(1);
			dataFile.setVersion(sVersion);
			break;
		}
		
		dataFile.setDataBits(64); //By default
		pat = Pattern.compile("(32|64)-bit");
		m = pat.matcher(header);
		while (m.find()) {
			int nBits = Integer.parseInt(m.group(1));
			dataFile.setDataBits(nBits);
			break;
		}

	}
	
	public float[] getColsData(int iCol) {
		
		int arrIdx = iCol-1;
		long size = dataFile.getRows(arrIdx);
		System.out.println("Data for col " + dataFile.getDataColumns(arrIdx) + "/" + dataFile.getUnitColumns(arrIdx) + ":");
		
		
		if(dataFile.dataLoaded(arrIdx)) {
			return dataFile.getDATA(arrIdx);
		}
		
		System.out.println("GETTING DATA FOR COL: " + Integer.toString(iCol));
		
		float[] data = new float[(int) size];
		String filename = dataFile.getFilename();
		long nRows = dataFile.getRows(arrIdx);
		try {
			RandomAccessFile file = new RandomAccessFile(filename, "r");
			
			long startByte = dataFile.getStartBytes(arrIdx);
			file.seek(startByte);
			int bufSize = dataFile.getDataBits()/8;
			
			for(int i=1; i<=nRows; i++) {
			
				byte[] buffer = new byte[bufSize];
	            file.read(buffer);
	            
	            float value = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getFloat();
	            
	            data[i-1] = value;
	            
	            //System.out.println(Integer.toString(i) + ": " + Float.toString(value));
	            
			}
			
			dataFile.setDATA(arrIdx, data);
            
			file.close();
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return data;
	}
	
	private void setDataStreamFrequency() {
		float[] timesignal = getColsData(1);
		
		float time_min = timesignal[0];
		float time_max = timesignal[timesignal.length-1];
		float total_time = time_max-time_min;
		String timeUnit = dataFile.getUnitColumns(0);
		float freq = 0f;
		if(timeUnit.equals("s")) {
			freq = timesignal.length/total_time;
		}else if(timeUnit.equals("ms")) {
			total_time = total_time/1000;
			freq = timesignal.length/total_time;
		}
		
		dataFile.setFrequency(Math.round(freq));
	}
	
	public int getDataStreamFrequency() {
		return dataFile.getFrequency();
	}

}
