/* 
* SearchParams.java
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
package com.noterik.bart.fs.search;

public class SearchParams {
	
	private String searchEngine;
	private boolean fullResults;
	private boolean propertiesOnly;
	private boolean headOnly;
	private String eventUri;
	private String uri;
	private String query;
	private String start;
	private String limit;
	private String sort;
	private boolean childs;

	public boolean isChilds() {
		return childs;
	}

	public void setChilds(boolean childs) {
		this.childs = childs;
	}

	public boolean isFullResults() {
		return fullResults;
	}

	public void setFullResults(boolean fullResults) {
		this.fullResults = fullResults;
	}

	public String getLimit() {
		return limit;
	}

	public void setLimit(String limit) {
		this.limit = limit;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public String getStart() {
		return start;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getEventUri() {
		return eventUri;
	}

	public void setEventUri(String eventUri) {
		this.eventUri = eventUri;
	}
	
	public String getSearchEngine() {
		return searchEngine;
	}

	public void setSearchEngine(String searchEngine) {
		this.searchEngine = searchEngine;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof SearchParams){
			if(((SearchParams)o).getUri() != null && uri != null){
				return (uri.equals(((SearchParams)o).getUri()));
			}
		}
		return false;
	}

	public boolean isPropertiesOnly() {
		return propertiesOnly;
	}

	public void setPropertiesOnly(boolean propertiesOnly) {
		this.propertiesOnly = propertiesOnly;
	}

	public boolean isHeadOnly() {
		return headOnly;
	}

	public void setHeadOnly(boolean headOnly) {
		this.headOnly = headOnly;
	}

}