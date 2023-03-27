package com.tarento.analytics.handler;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.ConfigurationLoader;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dto.AggregateDto;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.Data;
import com.tarento.analytics.enums.ChartType;
import com.tarento.analytics.helper.ComputeHelper;
import com.tarento.analytics.helper.ComputeHelperFactory;
import com.tarento.analytics.utils.ResponseRecorder;

/**
 * This handles ES response for single index, multiple index to represent single data value
 * Creates plots by merging/computing(by summation or by percentage) index values for same key
 * ACTION:  for the chart config defines the type either summation or computing percentage
 * AGGS_PATH : this defines the path/key to be used to search the tree
 *
 */
@Component
public class MetricChartResponseHandler implements IResponseHandler{
    public static final Logger logger = LoggerFactory.getLogger(MetricChartResponseHandler.class);
    char insightPrefix = 'i';
    
    @Autowired
    ConfigurationLoader configurationLoader;
    
    @Autowired
    ComputeHelperFactory computeHelperFactory; 
    
    @Autowired
    ResponseRecorder responseRecorder;

    @Override
    public AggregateDto translate(String profileName, AggregateRequestDto request, ObjectNode aggregations, Map<String, List<String>> responseDataSource) throws IOException {
        List<Data> dataList = new ArrayList<>();
        List<Data> outputList = new ArrayList<>();
        String requestId = request.getRequestId(); 
        String visualizationCode = request.getVisualizationCode();
        final JsonNode chartNode = configurationLoader.getConfigForProfile(profileName, API_CONFIG_JSON).get(request.getVisualizationCode());

        Boolean isDecimalCheck = chartNode.get(IS_DECIMAL).asBoolean();  
        List<Double> totalValues = new LinkedList<>();
        List<String> headerPathList = new LinkedList<>(); 
        String chartName = chartNode.get(CHART_NAME).asText();
        request.setChartName(chartName);
        String action = chartNode.get(ACTION).asText();
        Boolean allowZero = chartNode.get(ALLOW_ZERO).asBoolean();
        String vizType = chartNode.get(IResponseHandler.CHART_TYPE).asText();
        JsonNode enrichmentLookUp = null;
        String symbol = chartNode.get(IResponseHandler.VALUE_TYPE).asText();

        List<Double> percentageList = new LinkedList<>();
        ArrayNode aggrsPaths = (ArrayNode) chartNode.get(AGGS_PATH);

        aggrsPaths.forEach(headerPath -> {
        	if (responseDataSource.containsKey(Constants.DataSourceType.ES.toString())) {
                JsonNode aggregationNode = aggregations.get(AGGREGATIONS);
        		List<JsonNode> values =  aggregationNode.findValues(headerPath.asText());
                values.stream().parallel().forEach(value -> {
                    List<JsonNode> valueNodes = value.findValues(VALUE).isEmpty() ? value.findValues(DOC_COUNT) : value.findValues(VALUE);
                    Double sum = valueNodes.stream().mapToDouble(o -> o.asDouble()).sum();
                    DecimalFormat decimalFormat = null; 
                    decimalFormat = new DecimalFormat("#.#");
                    Double formattedSum = Double.parseDouble(decimalFormat.format(sum));
                    headerPathList.add(headerPath.asText()); 
                    if(action.equals(PERCENTAGE) && aggrsPaths.size()==2){
                        percentageList.add(formattedSum);
                    } else {
                        totalValues.add(formattedSum);
                    }
                });
        	} else if (responseDataSource.containsKey(Constants.DataSourceType.DRUID.toString())) {
        		JsonNode aggregationNode = aggregations.get(AGGREGATIONS);
        		List<String> modules = responseDataSource.get(Constants.DataSourceType.DRUID.toString());
        		for(String moduleName : modules) { 
        			ArrayNode arrayNode = (ArrayNode) aggregationNode.get(moduleName);
            		arrayNode.forEach(eachNode -> {
            				String key= chartNode.get("controls").get("key").asText();
            				if(eachNode.get(headerPath.asText()) != null && !eachNode.get(headerPath.asText()).asText().equals("null")) { 
            					Object value = chartNode.get(IResponseHandler.IS_DECIMAL).asBoolean() ? eachNode.get(headerPath.asText()).asDouble() : eachNode.get(headerPath.asText()).asLong();
            					if(allowZero && value != null && StringUtils.isNotBlank(key) && !key.equals("null")) {
                    					Data data = new Data(); 
                    					if(vizType.toUpperCase().equals(ChartType.METRICCOLLECTION.name())) { 
                    						data.setHeaderName(eachNode.get(key).asText()); 
                    					} else { 
                    						data.setHeaderName(key);
                    					}
                    					if(value instanceof Double) { 
                    						try { 
                        						Double doubleValue = (Double) value; 
                        						if(doubleValue.isNaN()) 
                        							value = 0.0; 
                        						DecimalFormat decimalFormat = null; 
                        	                    decimalFormat = new DecimalFormat("#.#");
                        	                    value = Double.parseDouble(decimalFormat.format(doubleValue));
                        					} catch(Exception ex) { 
                        						logger.info("Encountered an exception while converting the value to Double : "+  ex.getMessage());
                        					}
                    					}
                    					data.setHeaderValue(value);
                    					data.setIsDecimal(isDecimalCheck);
                    					dataList.add(data);
            					} else if(!allowZero && value != null && StringUtils.isNotBlank(key) && !key.equals("null") && !value.equals(0.0) && !value.equals(0)) {
                    					Data data = new Data(); 
                    					data.setHeaderName(key);
                    					data.setHeaderValue(value);
                    					data.setIsDecimal(isDecimalCheck);
                    					dataList.add(data);
            					}
            				}
                    });
        		}
        		if(responseDataSource.size() > 1) responseDataSource.remove(Constants.DataSourceType.DRUID.toString());
			} else if (responseDataSource.containsKey(Constants.DataSourceType.DRUID.toString()+"_1")) {
        		JsonNode aggregationNode = aggregations.get(AGGREGATIONS);
        		List<String> modules = responseDataSource.get(Constants.DataSourceType.DRUID.toString()+"_1");
        		for(String moduleName : modules) {
        			ArrayNode arrayNode = (ArrayNode) aggregationNode.get(moduleName);
            		arrayNode.forEach(eachNode -> {
            				String key= chartNode.get("controls").get("key").asText();
            				if(eachNode.get(headerPath.asText()) != null && !eachNode.get(headerPath.asText()).asText().equals("null")) { 
            					Object value = chartNode.get(IResponseHandler.IS_DECIMAL).asBoolean() ? eachNode.get(headerPath.asText()).asDouble() : eachNode.get(headerPath.asText()).asLong();
            					if(allowZero && value != null && StringUtils.isNotBlank(key) && !key.equals("null")) {
                    					Data data = new Data(); 
                    					if(vizType.toUpperCase().equals(ChartType.METRICCOLLECTION.name())) { 
                    						data.setHeaderName(eachNode.get(key).asText()); 
                    					} else { 
                    						data.setHeaderName(key);
                    					}
                    					data.setHeaderValue(value);
                    					data.setIsDecimal(isDecimalCheck);
                    					dataList.add(data);
            					} else if(!allowZero && value != null && StringUtils.isNotBlank(key) && !key.equals("null") && !value.equals(0.0) && !value.equals(0)) {
                    					Data data = new Data(); 
                    					data.setHeaderName(key);
                    					data.setHeaderValue(value);
                    					data.setIsDecimal(isDecimalCheck);
                    					dataList.add(data);
            					}
            				}
                    });
        		}
        		if(responseDataSource.size() > 1) responseDataSource.remove(Constants.DataSourceType.DRUID.toString());
			} else if (responseDataSource.containsKey(Constants.DataSourceType.REDIS.toString())) {
				JsonNode aggregationNode = aggregations.get(AGGREGATIONS);
				ArrayNode innerDataNode = (ArrayNode) aggregationNode.get(headerPath.asText());
				if(vizType.toUpperCase().equals(ChartType.METRICCOLLECTION.name())) {
					innerDataNode.forEach(eachNode -> {
						Data data = new Data();
						Object value = null; 
						data.setHeaderName(eachNode.get("headerName").asText());
						if(isDecimalCheck) { 
							Double doubleValue = eachNode.get("headerValue").asDouble(); 
							if(doubleValue.isNaN()) 
								value = 0.0; 
							DecimalFormat decimalFormat = null; 
		                    decimalFormat = new DecimalFormat("#.#");
		                    value = Double.parseDouble(decimalFormat.format(doubleValue));
		                    data.setHeaderValue(value); 
						} else { 
							data.setHeaderValue(eachNode.get("headerValue").asLong());
						}
						dataList.add(data);
					}); 
				} else { 
					Data data = new Data();
					data.setHeaderName(chartName);
					data.setIsDecimal(isDecimalCheck);
					data.setHeaderSymbol(symbol);
					data.setHeaderValue((innerDataNode.get(0)!=null && innerDataNode.get(0).get("headerValue") != null) ? innerDataNode.get(0).get("headerValue").asText() : "0");
					dataList.add(data);
				}
				if(responseDataSource.size() > 1) responseDataSource.remove(Constants.DataSourceType.REDIS.toString());
			}
            
        });

        try{
        	ChartType chartType = ChartType.fromValue(chartNode.get(Constants.JsonPaths.CHART_TYPE).asText());
        	Data data = null; 
        	/*if(!chartType.equals(ChartType.METRICCOLLECTION)) { 
        		data = new Data(chartName, action.equals(PERCENTAGE) && aggrsPaths.size()==2 ? percentageValue(percentageList) : (totalValues==null || totalValues.isEmpty())? 0.0 :totalValues.stream().reduce(0.0, Double::sum), symbol);
        		data.setIsDecimal(isDecimalCheck);
        		dataList.add(data);
        	} else { 
        		if(action.equals(METRIC_GROUP)) { 
        			for(int i = 0 ; i < totalValues.size(); i++) { 
        				data = new Data(headerPathList.get(i), totalValues.get(i), symbol);
        				dataList.add(data);
        			}
        		}
        	}*/
            responseRecorder.put(visualizationCode, request.getModuleLevel(), data);
            
            if(chartNode.get(POST_AGGREGATION_THEORY) != null) {  
            	ComputeHelper computeHelper = computeHelperFactory.getInstance(chartNode.get(POST_AGGREGATION_THEORY).asText());
            	Map<String, List<Data>> dataMap = new HashMap<String, List<Data>>();
            	dataMap.put(chartNode.get(POST_AGGREGATION_THEORY).asText(), dataList); 
            	outputList = computeHelper.compute(request, dataMap, chartNode);
            }
        }catch (Exception e){
            logger.info("data chart name = "+chartName +" ex occurred "+e.getMessage());
        }

        return getAggregatedDto(chartNode, chartNode.get(POST_AGGREGATION_THEORY) != null ? outputList : dataList, request.getVisualizationCode());
    }
}
