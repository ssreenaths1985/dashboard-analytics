package com.tarento.analytics.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.Data;
import com.tarento.analytics.dto.Plot;

/**
 * This implementation of Compute Helper is used to multiply by 100 to showcase the percentage value at 100s 
 * The output is added into another map and added to Data list again and be sent back as a response. 
 * @author Darshan Nagesh
 *
 */
@Component
public class MultiplyPercentHelper implements ComputeHelper {
	public static final String HELPER_IDENTIFIER = "multiplyPercentHelper";
	public static final String OUTPUT_SYMBOL = "Decimal"; 
	public static final Logger logger = LoggerFactory.getLogger(MultiplyPercentHelper.class);
	
	@Override
	public Double compute(AggregateRequestDto request, double value) {
		return null;
	}	

	@Override
	public List<Data> compute(AggregateRequestDto request, Map<String, List<Data>> dataMap, JsonNode chartNode) {
		List<Data> outputList = new ArrayList<Data>();
		Iterator<Entry<String, List<Data>>> itr = dataMap.entrySet().iterator();
		while (itr.hasNext()) {
			Entry<String, List<Data>> entry = itr.next();
			List<Data> dataList = entry.getValue();
			if (dataList != null && dataList.size() > 0) {
				for (Data data : dataList) {
					if (data.getPlots() != null && data.getPlots().size() > 0) {
						for (Plot plot : data.getPlots()) {
							Double plotValue = Double.parseDouble(plot.getValue().toString());
							plotValue = plotValue * 100.0;
							plot.setValue(plotValue);
						}
					}
					if(data.getHeaderValue() != null) { 
						Double headerValue = Double.parseDouble(data.getHeaderValue().toString());
						headerValue = headerValue * 100.0 ; 
						data.setHeaderValue(headerValue);
					}
					outputList.add(data);
				}
			}
		}
		return outputList;
	}

}
