package com.tarento.analytics.org.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.ConfigurationLoader;
import com.tarento.analytics.actions.ActionFactory;
import com.tarento.analytics.actions.ActionHelper;
import com.tarento.analytics.config.AppConfiguration;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dto.AggregateDto;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.CummulativeDataRequestDto;
import com.tarento.analytics.dto.DashboardHeaderDto;
import com.tarento.analytics.dto.Data;
import com.tarento.analytics.dto.RoleDto;
import com.tarento.analytics.enums.AlwaysView;
import com.tarento.analytics.enums.ChartType;
import com.tarento.analytics.exception.AINException;
import com.tarento.analytics.handler.IResponseHandler;
import com.tarento.analytics.handler.InsightsHandler;
import com.tarento.analytics.handler.InsightsHandlerFactory;
import com.tarento.analytics.handler.ResponseHandlerFactory;
import com.tarento.analytics.model.InsightsConfiguration;
import com.tarento.analytics.repository.RedisRepository;
import com.tarento.analytics.service.impl.RestService;

@Component
public class TarentoServiceImpl implements ClientService {

	public static final Logger logger = LoggerFactory.getLogger(TarentoServiceImpl.class);
	ObjectMapper mapper = new ObjectMapper();
	char insightPrefix = 'i';

	@Autowired
	private RestService restService;

	@Autowired
	private ConfigurationLoader configurationLoader;

	@Autowired
	private ResponseHandlerFactory responseHandlerFactory;

	@Autowired
	private InsightsHandlerFactory insightsHandlerFactory;
	
	@Autowired
	private ActionFactory actionFactory;
	
	@Autowired
	private QueryServiceFactory queryServiceFactory; 
	
	@Autowired
	private RedisRepository redisRepository; 
	
	@Autowired
	private AppConfiguration appConfig; 

	@Override
	public AggregateDto getAggregatedData(String profileName, AggregateRequestDto request, List<RoleDto> roles)
			throws AINException, IOException {

		// Setting up instances to hold data
		InsightsConfiguration insightsConfig = null;

		// Read visualization Code from the request
		String internalChartId = request.getVisualizationCode();
		logger.info("Started working on magic for the Visualization : %s", internalChartId);

		// Load Chart API configuration to Object Node for easy retrieval later
		ObjectNode node = configurationLoader.getConfigForProfile(profileName,
				Constants.ConfigurationFiles.CHART_API_CONFIG);
		ObjectNode chartNode = (ObjectNode) node.get(internalChartId);
		logger.info("Chart Node for the Visualization : " + chartNode.toString());

		// Get the data for Visualization Code
		if (chartNode != null && chartNode.get(CHART_TYPE) != null
				&& StringUtils.isNotBlank(chartNode.get(CHART_TYPE).asText())
				&& chartNode.get(CHART_TYPE).asText().equals(COMBINATION)) {
			String primeChartVisualizationCode = chartNode.get(PRIME).asText();
			ObjectNode primeChartNode = (ObjectNode) node.get(primeChartVisualizationCode);
			AggregateRequestDto temporaryPrimeRequest = request;
			temporaryPrimeRequest.setVisualizationCode(primeChartVisualizationCode);
			AggregateDto aggregateDto = getDataForVisualizationCode(request, insightsConfig, primeChartNode, profileName);

			ArrayNode listOfVisualizationCodes = (ArrayNode) chartNode.get(INNER_WIDGET);
			listOfVisualizationCodes.forEach(eachVisualizationCode -> {
				String innerWidgetVisualization = new ObjectMapper().convertValue(eachVisualizationCode, String.class);
				ObjectNode innerWidgetChartNode = (ObjectNode) node.get(innerWidgetVisualization);
				try {
					AggregateRequestDto temporaryInnerRequest = request; 
					temporaryInnerRequest.setVisualizationCode(eachVisualizationCode.asText());
					AggregateDto innerAggregateDto = getDataForVisualizationCode(temporaryInnerRequest, insightsConfig, innerWidgetChartNode, profileName);
					if(aggregateDto.getInnerWidget() != null && aggregateDto.getInnerWidget().size() > 0) { 
						aggregateDto.getInnerWidget().addAll(innerAggregateDto.getData()); 
					} else { 
						aggregateDto.setInnerWidget(innerAggregateDto.getData());
					}
				} catch (IOException e) {
					logger.error("Encountered an Exception while getting inner widget visualization data: "
							+ e.getMessage());
				}
			});
			return aggregateDto;
		} else {
			AggregateDto aggregateDto = getDataForVisualizationCode(request, insightsConfig, chartNode, profileName);
			return aggregateDto;
		}
	}
	
	private AggregateDto getDataForVisualizationCode(AggregateRequestDto request, InsightsConfiguration insightsConfig,
			ObjectNode chartNode, String profileName) throws IOException {
			ObjectNode aggrObjectNode = JsonNodeFactory.instance.objectNode();
			ObjectNode nodes = JsonNodeFactory.instance.objectNode();
		
		// Check whether Insights is required for the Visualization
				if (chartNode.get(Constants.JsonPaths.INSIGHT) != null) {
					insightsConfig = mapper.treeToValue(chartNode.get(Constants.JsonPaths.INSIGHT),
							InsightsConfiguration.class);
				}
				
				// Fetch the chartType from the Chart Configurations 
				ChartType chartType = ChartType.fromValue(chartNode.get(Constants.JsonPaths.CHART_TYPE).asText());

				// Fetch defaults and intervals settings 
				boolean isDefaultPresent = chartType.equals(ChartType.LINE)
						&& chartNode.get(Constants.JsonPaths.INTERVAL) != null;
				boolean isRequestContainsInterval = null == request.getRequestDate() ? false
						: (request.getRequestDate().getInterval() != null && !request.getRequestDate().getInterval().isEmpty());
				String interval = isRequestContainsInterval ? request.getRequestDate().getInterval()
						: (isDefaultPresent ? chartNode.get(Constants.JsonPaths.INTERVAL).asText() : "");
				if (chartNode.get(Constants.JsonPaths.ALWAYS_VIEW) != null) {
					if (AlwaysView.MONTHWISE
							.equals(AlwaysView.fromValue(chartNode.get(Constants.JsonPaths.ALWAYS_VIEW).asText()))) {
						changeDatesToMonthWiseView(request);
					}
				}
				
				// Tint Application has been disabled as there is no use case for the same right now 
				// applyVisibilityTint(profileName, request, roles);
				Map<String, List<String>> responseDataSource = new HashMap<String, List<String>>(); 
				executeConfiguredQueries(chartNode, aggrObjectNode, nodes, request, interval, responseDataSource);
				logger.info("Fetched data from the data sources : " + responseDataSource.toString());
				request.setChartNode(chartNode);
				IResponseHandler responseHandler = responseHandlerFactory.getInstance(chartType);
				AggregateDto aggregateDto = new AggregateDto();

				// Translate the responses obtained
				if (aggrObjectNode.fields().hasNext()) {
					aggregateDto = responseHandler.translate(profileName, request, aggrObjectNode, responseDataSource);
				}
				logger.info("After aggregation response : " + aggregateDto.toString() );
				
				// Apply Insights to Visualizations 
				
				if (insightsConfig != null && StringUtils.isNotBlank(insightsConfig.getInsightInterval())) {
					applyInsightsToVisualizations(request, insightsConfig, responseDataSource, interval, chartNode,
							chartType, responseHandler, profileName, aggregateDto);
				}
				
				return aggregateDto; 
	}

	private void applyInsightsToVisualizations(AggregateRequestDto request, InsightsConfiguration insightsConfig,
			Map<String, List<String>> responseDataSource, String interval, ObjectNode chartNode, ChartType chartType,
			IResponseHandler responseHandler, String profileName, AggregateDto aggregateDto) throws IOException {
		ObjectNode insightAggrObjectNode = JsonNodeFactory.instance.objectNode();
		ObjectNode insightNodes = JsonNodeFactory.instance.objectNode();
		Boolean continueWithInsight = Boolean.FALSE;
		continueWithInsight = getInsightsDate(request, insightsConfig.getInsightInterval());
		if (continueWithInsight) {
			String insightVisualizationCode = insightPrefix + request.getVisualizationCode();
			request.setVisualizationCode(insightVisualizationCode);
			executeConfiguredQueries(chartNode, insightAggrObjectNode, insightNodes, request, interval, responseDataSource);
			request.setChartNode(chartNode);
			
			responseHandler = responseHandlerFactory.getInstance(chartType);
			if (insightAggrObjectNode.fields().hasNext()) {
				responseHandler.translate(profileName, request, insightAggrObjectNode, responseDataSource);
			}
			InsightsHandler insightsHandler = insightsHandlerFactory.getInstance(chartType);
			aggregateDto = insightsHandler.getInsights(aggregateDto, request.getVisualizationCode(),
					request.getModuleLevel(), insightsConfig);
		}
	
	}
	private void applyVisibilityTint(String profileName, AggregateRequestDto request, List<RoleDto> roles) {
		ObjectNode roleMappingNode = configurationLoader.getConfigForProfile(profileName,
				ConfigurationLoader.ROLE_DASHBOARD_CONFIG);
		ArrayNode rolesArray = (ArrayNode) roleMappingNode.findValue(Constants.DashBoardConfig.ROLES);

		rolesArray.forEach(role -> {
			Object roleName = roles.stream()
					.filter(x -> role.get(Constants.DashBoardConfig.ROLE_NAME).asText().equals(x.getName())).findAny()
					.orElse(null);
			logger.info("Role Name: " + roleName);
			RoleDto dto = RoleDto.class.cast(roleName);
			if (dto != null && dto.getId() != null && role.get(Constants.DashBoardConfig.ROLE_NAME).asText().equals(dto.getName())) { 
				// checks role has given db id
				role.get(Constants.DashBoardConfig.REALMS).forEach(realm -> {
					realm.get(Constants.DashBoardConfig.DASHBOARDS).forEach(db -> {
						if (db.get(Constants.DashBoardConfig.ID).asText().equalsIgnoreCase(request.getDashboardId())) {
							if(db.get(Constants.DashBoardConfig.VISIBILITY) != null) { 
								ArrayNode visibilityArray = (ArrayNode) db.get(Constants.DashBoardConfig.VISIBILITY);
								if (visibilityArray != null) {
									Map<String, Object> filterMap = new HashMap<>();
									visibilityArray.forEach(visibility -> {
										String key = visibility.get(Constants.DashBoardConfig.KEY).asText();
										ArrayNode valueArray = (ArrayNode) visibility.get(Constants.DashBoardConfig.VALUE);
										List<String> valueList = new ArrayList<>();
										valueArray.forEach(value -> {
											valueList.add(value.asText());
										});
										if (!request.getFilters().containsKey(key)) {
											filterMap.put(key, valueList);
										}
									});
									request.getFilters().putAll(filterMap);
								}
							}
						}
					});
				}); 
			}
		});
	}

	private void executeConfiguredQueries(ObjectNode chartNode, ObjectNode aggrObjectNode, ObjectNode nodes,
			AggregateRequestDto request, String interval, Map<String, List<String>> responseDataSource) {
		JsonNode lookupNode = null;  
		if(chartNode.get(Constants.JsonPaths.ENRICHMENT_LOOKUP) != null) { 
			Map<String, String> enrichmentLookUpMap = new ObjectMapper().convertValue(chartNode.get(Constants.JsonPaths.ENRICHMENT_LOOKUP), new TypeReference<Map<String, String>>(){});
			if(enrichmentLookUpMap != null && !enrichmentLookUpMap.isEmpty()) { 
				lookupNode = fetchEnrichmentLookUpValues(enrichmentLookUpMap.get(TABLE_NAME), request, chartNode);
				aggrObjectNode.set(Constants.JsonPaths.ENRICHMENT_LOOKUP, lookupNode); 
			}
		}
		 
		ArrayNode queries = (ArrayNode) chartNode.get(Constants.JsonPaths.QUERIES);
		queries.forEach(query -> {
			String dataSource = query.get(Constants.JsonPaths.DATA_SOURCE).asText();
			String module = query.get(Constants.JsonPaths.MODULE).asText();
			String indexName = query.get(Constants.JsonPaths.INDEX_NAME).asText();
			String rqMs = query.get(Constants.JsonPaths.REQUEST_QUERY_MAP).asText();
			JsonNode requestQueryMaps = null ; 
			try {
				requestQueryMaps = new ObjectMapper().readTree(rqMs);
			} catch (Exception ex) {
				logger.error("Encountered an Exception while converting Request Query Map: " + ex.getMessage());
			}
			if(dataSource.equals(Constants.DataSourceType.ES.name())) { 
				ObjectNode objectNode = queryServiceFactory.getInstance(Constants.DataSourceType.ES)
						.getChartConfigurationQuery(request, query, indexName, interval);
				String instance = query.get(Constants.JsonPaths.ES_INSTANCE).asText();
				try {
					JsonNode aggrNode = restService.search(indexName, objectNode.toString(), instance);
					if (nodes.has(indexName)) {
						indexName = indexName + "_1";
					}
					logger.info("indexName +" + indexName);
					nodes.set(dataSource, aggrNode.get(Constants.JsonPaths.AGGREGATIONS));
					if(responseDataSource.containsKey(dataSource)) { 
						List<String> modules = responseDataSource.get(dataSource);
						modules.add(module); 
					} else { 
						List<String> modules = new ArrayList<>(); 
						modules.add(module); 
						responseDataSource.put(dataSource, modules); 
					}
				} catch (Exception e) {
					logger.error("Encountered an Exception while Executing the Query over ES : " + e.getMessage());
				}
				aggrObjectNode.set(Constants.JsonPaths.AGGREGATIONS, nodes);
			} else if(dataSource.equals(Constants.DataSourceType.DRUID.name())) { 
				
				ObjectNode objectNode = queryServiceFactory.getInstance(Constants.DataSourceType.DRUID)
						.getChartConfigurationQuery(request, query, indexName, interval);
				String aggrQuery = query.get(Constants.JsonPaths.AGGREGATION_QUERY).asText();
				if(request.getFilters() != null) { 
					aggrQuery = replaceFilters(aggrQuery, request, requestQueryMaps);
				}
				try { 
					logger.info("Executing the Druid Query : " + aggrQuery);
					JsonNode aggrNode = restService.searchDruid(aggrQuery);
					logger.info("Aggregation Node obtained : " + aggrNode);
					nodes.set(module, aggrNode);
					if(responseDataSource.containsKey(dataSource)) { 
						List<String> modules = responseDataSource.get(dataSource);
						modules.add(module); 
					} else { 
						List<String> modules = new ArrayList<>(); 
						modules.add(module); 
						responseDataSource.put(dataSource, modules); 
					}
				} catch (Exception e) {
					logger.error("Encountered an Exception while Executing the Query over DRUID : " + e.getMessage());
				}
				aggrObjectNode.set(Constants.JsonPaths.AGGREGATIONS, nodes);
			} else if(dataSource.equals(Constants.DataSourceType.REDIS.name())) {
				String aggrQuery = query.get(Constants.JsonPaths.AGGREGATION_QUERY).asText();
				try { 
					String action = chartNode.get(Constants.JsonPaths.ACTION).asText();
					JsonNode aggrNode = null; 
					if(aggrQuery.equals("GET")) { 
						aggrNode = redisRepository.getForKey(indexName); 
					} else { 
						aggrNode = redisRepository.getAllForKey(indexName, request, chartNode);
					}
					
					ActionHelper actionHelper = actionFactory.getInstance(action);
					Map<String, List<Data>> dataMap = new HashMap<String, List<Data>>(); 
					List<Data> dataList = actionHelper.compute(aggrNode, request, dataMap);
					JsonNode finalNode = new ObjectMapper().valueToTree(dataList);
					nodes.set(indexName, finalNode);
					if(responseDataSource.containsKey(dataSource)) { 
						List<String> modules = responseDataSource.get(dataSource);
						modules.add(module); 
					} else { 
						List<String> modules = new ArrayList<>(); 
						modules.add(module); 
						responseDataSource.put(dataSource, modules); 
					}
				} catch (Exception e) {
					logger.error("Encountered an Exception while Executing the Query over DRUID : " + e.getMessage());
				}
				aggrObjectNode.set(Constants.JsonPaths.AGGREGATIONS, nodes);
			}

		});
	}
	
	private JsonNode fetchEnrichmentLookUpValues(String indexName, AggregateRequestDto request, ObjectNode chartNode) { 
		JsonNode aggrNode = redisRepository.getAllForKey(indexName, request, chartNode);
		return aggrNode; 
	}

	private String replaceFilters(String aggrQuery, AggregateRequestDto request, JsonNode requestQueryMaps) {
		String startDate = "" ; 
		String endDate = "" ; 
		if(request != null && 
				request.getRequestDate() != null && 
				StringUtils.isNotBlank(request.getRequestDate().getStartDate()) &&
				StringUtils.isNotBlank(request.getRequestDate().getEndDate())) { 
			startDate = getTimeStampFromEpoch(request.getRequestDate().getStartDate());
			endDate = getTimeStampFromEpoch(request.getRequestDate().getEndDate());
		}
		if(startDate.equals("")) startDate = appConfig.getDefaultStartDate();
		if(endDate.equals("")) endDate = appConfig.getDefaultEndDate();
		aggrQuery = aggrQuery.replace("$StartDate$", startDate.replace("Z", "")); 
		aggrQuery = aggrQuery.replace("$EndDate$", endDate.replace("Z", ""));
		
		if(request.getFilters() != null && !request.getFilters().isEmpty()) {
			Map<String, Object> filterMap = request.getFilters();
			Iterator<Entry<String, Object>> itr = filterMap.entrySet().iterator();
			while(itr.hasNext()) { 
				Entry<String, Object> entry = itr.next();
				if(requestQueryMaps.get(entry.getKey()) != null && StringUtils.isNotBlank(requestQueryMaps.get(entry.getKey()).asText())) {
					if(entry.getKey().equals("courseId") || entry.getKey().equals("courseId1") ) { 
						aggrQuery = aggrQuery.replace("$"+entry.getKey()+"$", " AND " + requestQueryMaps.get(entry.getKey()).asText() + " = " + "'" + entry.getValue() + "'");
					} else { 
						aggrQuery = aggrQuery.replace("$"+entry.getKey()+"$", " AND " + requestQueryMaps.get(entry.getKey()).asText() + " = " + entry.getValue());
					}
					
				}
			}
		} else { 
			aggrQuery = aggrQuery.replace("$mdo$", ""); 
			aggrQuery = aggrQuery.replace("$courseId$", ""); 
		}
		return aggrQuery; 
	}
	
	private String getTimeStampFromEpoch(String epoch) { 
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String parsedDate = formatter.format(new Date(Long.parseLong(epoch))); 
		return parsedDate; 
	}
	
	private String getQueryTimeStampFromEpoch(String epoch) { 
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'");
		String parsedDate = formatter.format(new Date(Long.parseLong(epoch))); 
		return parsedDate; 
	}
	
	private void changeDatesToMonthWiseView(AggregateRequestDto request) {
		Long daysBetween = daysBetween(Long.parseLong(request.getRequestDate().getStartDate()),
				Long.parseLong(request.getRequestDate().getEndDate()));
		if (daysBetween <= 30) {
			Calendar startCal = Calendar.getInstance();
			Calendar endCal = Calendar.getInstance();
			startCal.setTime(new Date(Long.parseLong(request.getRequestDate().getStartDate())));
			startCal.set(Calendar.DAY_OF_MONTH, 1);
			endCal.setTime(new Date(Long.parseLong(request.getRequestDate().getEndDate())));
			int month = endCal.get(Calendar.MONTH) + 1;
			endCal.set(Calendar.MONTH, month + 1);
			request.getRequestDate().setStartDate(String.valueOf(startCal.getTimeInMillis()));
			request.getRequestDate().setEndDate(String.valueOf(endCal.getTimeInMillis()));
		}
	}

	private Boolean getInsightsDate(AggregateRequestDto request, String insightInterval) {
		Long daysBetween = daysBetween(Long.parseLong(request.getRequestDate().getStartDate()),
				Long.parseLong(request.getRequestDate().getEndDate()));
		if (insightInterval.equalsIgnoreCase(Constants.Interval.month.toString()) && daysBetween > 32) {
			return Boolean.FALSE;
		}
		if (insightInterval.equalsIgnoreCase(Constants.Interval.week.toString()) && daysBetween > 8) {
			return Boolean.FALSE;
		}
		if (insightInterval.equalsIgnoreCase(Constants.Interval.year.toString()) && daysBetween > 366) {
			return Boolean.FALSE;
		}
		Calendar startCal = Calendar.getInstance();
		Calendar endCal = Calendar.getInstance();
		startCal.setTime(new Date(Long.parseLong(request.getRequestDate().getStartDate())));
		endCal.setTime(new Date(Long.parseLong(request.getRequestDate().getEndDate())));
		if (insightInterval.equalsIgnoreCase(Constants.Interval.month.toString())) {
			startCal.add(Calendar.MONTH, -1);
			endCal.add(Calendar.MONTH, -1);
		} else if (insightInterval.equalsIgnoreCase(Constants.Interval.week.toString())) {
			startCal.add(Calendar.WEEK_OF_YEAR, -1);
			endCal.add(Calendar.WEEK_OF_YEAR, -1);
		} else if (StringUtils.isBlank(insightInterval)
				|| insightInterval.equalsIgnoreCase(Constants.Interval.year.toString())) {
			startCal.add(Calendar.YEAR, -1);
			endCal.add(Calendar.YEAR, -1);
		}
		request.getRequestDate().setStartDate(String.valueOf(startCal.getTimeInMillis()));
		request.getRequestDate().setEndDate(String.valueOf(endCal.getTimeInMillis()));
		return Boolean.TRUE;
	}

	public long daysBetween(Long start, Long end) {
		return TimeUnit.MILLISECONDS.toDays(Math.abs(end - start));
	}

	@Override
	public List<DashboardHeaderDto> getHeaderData(CummulativeDataRequestDto requestDto, List<RoleDto> roles)
			throws AINException {
		// TODO Auto-generated method stub
		return null;
	}

}
