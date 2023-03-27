package com.tarento.analytics.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tarento.analytics.enums.ChartType;

/**
 * @author Darshan Nagesh
 *
 */
public class AggregateDto {

	private ChartType chartType;
	
	private String visualizationCode; 
	
	private String chartFormat;
	
	private String drillDownChartId;
	
	private Object filterKeys;
	
	private Map<String, Object> customData;
	
	private RequestDate dates;
	
	private Object filter;

	private List<Data> innerWidget = new ArrayList<>();
	
	private List<Data> data = new ArrayList<>();
	
	private String comments;
	
	private String image; 
	
	public List<Data> getInnerWidget() {
		return innerWidget;
	}

	public void setInnerWidget(List<Data> innerWidget) {
		this.innerWidget = innerWidget;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getVisualizationCode() {
		return visualizationCode;
	}

	public Object getFilterKeys() {
		return filterKeys;
	}

	public void setFilterKeys(Object filterKeys) {
		this.filterKeys = filterKeys;
	}

	public void setVisualizationCode(String visualizationCode) {
		this.visualizationCode = visualizationCode;
	}

	public String getDrillDownChartId() {
		return drillDownChartId;
	}

	public void setDrillDownChartId(String drillDownChartId) {
		this.drillDownChartId = drillDownChartId;
	}

	public List<Data> getData() {
		return data;
	}

	public void setData(List<Data> data) {
		this.data = data;
	}

	public ChartType getChartType() {
		return chartType;
	}

	public void setChartType(ChartType chartType) {
		this.chartType = chartType;
	}

	public String getChartFormat() {
		return chartFormat;
	}

	public void setChartFormat(String chartFormat) {
		this.chartFormat = chartFormat;
	}


	public Map<String, Object> getCustomData() {
		return customData;
	}

	public void setCustomData(Map<String, Object> customData) {
		this.customData = customData;
	}

	public RequestDate getDates() {
		return dates;
	}

	public void setDates(RequestDate dates) {
		this.dates = dates;
	}

	public Object getFilter() {
		return filter;
	}

	public void setFilter(Object filter) {
		this.filter = filter;
	}
}
