package com.noterik.bart.fs.fscommand.dynamic.playlist.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;

public class SBFile {
	
	private final int nHeader = 64;
	private final int nColHeader = 32;
	private final int nUnitHeader = 16;
	private final int nPointers = 8;
	private final int nData = 8;
	
	private String sFilename = "";
	private String sVersion = "";
	private int nBits = 64;
	private long nCols = 0; //Number of columns present
	private int frequency = 0; //Current file frequency
	
	private List<String> dataColumns = new ArrayList<String>();
	private List<String> dataUnits = new ArrayList<String>();
	private List<Long> dataStartBytes = new ArrayList<Long>();
	private List<Long> dataRows = new ArrayList<Long>();
	private List<float[]> signals = new ArrayList<float[]>();
	
	public void setFilename(String file) {
		sFilename = file;
	}
	
	public String getFilename() {
		return sFilename;
	}
	
	public void setVersion(String v) {
		sVersion = v;
	}
	
	public String getVersion() {
		return sVersion;
	}
	
	public void setDataBits(int bits) {
		nBits = bits;
	}
	
	public int getDataBits() {
		return nBits;
	}
	
	public void setColumnCount(long cols) {
		nCols = cols;
		
		for (int i=0; i<nCols; i++) {
			signals.add(i, null);
		}
	}
	
	public long getColumnCount() {
		return nCols;
	}
	
	public void setDataColumns(List<String> columns) {
		dataColumns = columns;
	}
	
	public void setDataColumns(int key, String value) {
		dataColumns.add(key, value);
	}
	
	public List<String> getDataColumns() {
		return dataColumns;
	}
	
	public String getDataColumns(int iCol) {
		return dataColumns.get(iCol);
	}
	
	public void setUnitColumns(List<String> units) {
		dataUnits = units;
	}
	
	public void setUnitColumns(int key, String value) {
		dataUnits.add(key, value);
	}
	
	public List<String> getUnitColumns() {
		return dataUnits;
	}
	
	public String getUnitColumns(int iCol) {
		return dataUnits.get(iCol);
	}
	
	public void setStartBytes(List<Long> bytes) {
		dataStartBytes = bytes;
	}
	
	public void setStartBytes(int key, Long value) {
		dataStartBytes.add(key, value);
	}
	
	public List<Long> getStartBytes() {
		return dataStartBytes;
	}
	
	public Long getStartBytes(int iCol) {
		return dataStartBytes.get(iCol);
	}
	
	public void setRows(List<Long> rows) {
		dataRows = rows;
	}
	
	public void setRows(int key, Long value) {
		dataRows.add(key,value);
	}
	
	public List<Long> getRows() {
		return dataRows;
	}
	
	public Long getRows(int iCol) {
		return dataRows.get(iCol);
	}
	
	public void setDATA(int iCol, float[] signal) {
		signals.add(iCol, signal);
	}
	
	public float[] getDATA(int iCol) {
		return signals.get(iCol);
	}
	
	public boolean dataLoaded(int iCol) {
		
		if(signals.size()>iCol) {
			if(signals.get(iCol)!=null) {
				return true;
			}
		}
		
		return false;
	}
	
	public float getMax(int iCol) {
		List<Float> b = Arrays.asList(ArrayUtils.toObject(signals.get(iCol)));
		float max = Collections.max(b);
		b = null;
		return max;
	}
	
	public float getMin(int iCol) {
		List<Float> b = Arrays.asList(ArrayUtils.toObject(signals.get(iCol)));
		float min = Collections.min(b);
		b = null;
		return min;
	}
	
	public float[] getMinMax(int iCol) {
		List<Float> b = Arrays.asList(ArrayUtils.toObject(signals.get(iCol)));
		float[] minmax = new float[2];
		minmax[0] = Collections.min(b); 
		minmax[1] = Collections.max(b);
		b = null;
		return minmax;
	}
	
	public void setFrequency(int freq) {
		frequency = freq;
	}
	
	public int getFrequency() {
		return frequency;
	}
}

