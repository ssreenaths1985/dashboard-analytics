package com.tarento.analytics.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.ConfigurationLoader;
import com.tarento.analytics.config.AppConfiguration;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dto.RoleDto;
import com.tarento.analytics.exception.AINException;
import com.tarento.analytics.model.dashboardConfig.Dashboard;
import com.tarento.analytics.service.MetadataService;

@Service("metadataService")
public class MetadataServiceImpl implements MetadataService {

	public static final Logger logger = LoggerFactory.getLogger(MetadataServiceImpl.class);

	@Autowired
	private ConfigurationLoader configurationLoader;

	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired 
	private AppConfiguration appConfig; 
	
	@Autowired
	private RetryTemplate retryTemplate; 

	@Override
	public ArrayNode getDashboardConfiguration(String profileName, String dashboardId, String catagory,
			List<RoleDto> roleIds) throws AINException, IOException {
		ObjectNode dashboardNode = configurationLoader.getConfigForProfile(profileName, ConfigurationLoader.MASTER_DASHBOARD_CONFIG);
		ArrayNode dashboardNodes = (ArrayNode) dashboardNode.findValue(Constants.DashBoardConfig.DASHBOARDS);

		ObjectNode roleMappingNode = configurationLoader.getConfigForProfile(profileName, ConfigurationLoader.ROLE_DASHBOARD_CONFIG);
		ArrayNode rolesArray = (ArrayNode) roleMappingNode.findValue(Constants.DashBoardConfig.ROLES);
		ArrayNode dbArray = JsonNodeFactory.instance.arrayNode();
		Map<String, ObjectNode> mapOfDashboards = new HashMap<>(); 

		rolesArray.forEach(role -> {
				ArrayNode visArray = JsonNodeFactory.instance.arrayNode();
				ArrayNode widgetArray = JsonNodeFactory.instance.arrayNode();
				ArrayNode filterArray = JsonNodeFactory.instance.arrayNode();
				// checks role has given db id
				role.get(Constants.DashBoardConfig.REALMS).forEach(realm -> {
					realm.get(Constants.DashBoardConfig.DASHBOARDS).forEach(db -> {
						ArrayNode visibilityArray = (ArrayNode) db.findValue(Constants.DashBoardConfig.VISIBILITY);
						ObjectNode copyDashboard = objectMapper.createObjectNode();
						
						JsonNode name = JsonNodeFactory.instance.textNode("");
						JsonNode id = JsonNodeFactory.instance.textNode("");
						JsonNode style = JsonNodeFactory.instance.textNode("");
						JsonNode isActive = JsonNodeFactory.instance.booleanNode(false);
						JsonNode widgetTitle = JsonNodeFactory.instance.textNode("");
						JsonNode showFilters= JsonNodeFactory.instance.booleanNode(false);
						JsonNode showWidgets = JsonNodeFactory.instance.booleanNode(false);
						JsonNode showDateFilter = JsonNodeFactory.instance.booleanNode(false);
						JsonNode showWidgetTitle = JsonNodeFactory.instance.booleanNode(false);
						
						if (db.get(Constants.DashBoardConfig.ID).asText().equalsIgnoreCase(dashboardId)) {
							for (JsonNode dbNode : dashboardNodes) {
								if(!mapOfDashboards.containsKey(dbNode.get(Constants.DashBoardConfig.NAME).asText())) { 
									if (dbNode.get(Constants.DashBoardConfig.ID).asText().equalsIgnoreCase(dashboardId)) {
										logger.info("dbNode: " + dbNode);
										name = dbNode.get(Constants.DashBoardConfig.NAME);
										id = dbNode.get(Constants.DashBoardConfig.ID);
										style = dbNode.get(Constants.DashBoardConfig.STYLE);
										isActive = dbNode.get(Constants.DashBoardConfig.IS_ACTIVE); 
										widgetTitle = dbNode.get(Constants.DashBoardConfig.WIDGET_TITLE); 
										showFilters = dbNode.get(Constants.DashBoardConfig.SHOW_FILTERS); 
										showWidgets = dbNode.get(Constants.DashBoardConfig.SHOW_WIDGETS);
											showDateFilter = dbNode.get(Constants.DashBoardConfig.SHOW_DATE_FILTER); 
										showWidgetTitle = dbNode.get(Constants.DashBoardConfig.SHOW_WIDGET_TITLE);
										
										dbNode.get(Constants.DashBoardConfig.VISUALISATIONS).forEach(visual -> {
											visArray.add(visual);
										});
										dbNode.get(Constants.DashBoardConfig.WIDGET_CHARTS).forEach(widget -> {
											widgetArray.add(widget);
										});
										dbNode.get(Constants.DashBoardConfig.FILTERS).forEach(filter -> {
											JsonNode node = filter.deepCopy(); 
											applyVisilibityLayer(visibilityArray, node);
											filterArray.add(node);
										});
										copyDashboard.set(Constants.DashBoardConfig.NAME, name);
										copyDashboard.set(Constants.DashBoardConfig.ID, id);
										copyDashboard.set(Constants.DashBoardConfig.STYLE, style);
										copyDashboard.set(Constants.DashBoardConfig.IS_ACTIVE, isActive);
										copyDashboard.set(Constants.DashBoardConfig.WIDGET_TITLE, widgetTitle);
										copyDashboard.set(Constants.DashBoardConfig.SHOW_FILTERS, showFilters);
										copyDashboard.set(Constants.DashBoardConfig.SHOW_WIDGETS, showWidgets);
										copyDashboard.set(Constants.DashBoardConfig.SHOW_WIDGET_TITLE, showWidgetTitle);
										copyDashboard.set(Constants.DashBoardConfig.WIDGET_CHARTS, widgetArray);
										copyDashboard.set(Constants.DashBoardConfig.FILTERS, filterArray);
										copyDashboard.set(Constants.DashBoardConfig.SHOW_DATE_FILTER, showDateFilter); 
										copyDashboard.set(Constants.DashBoardConfig.VISUALISATIONS, visArray);
										mapOfDashboards.put(copyDashboard.get(Constants.DashBoardConfig.NAME).asText(), copyDashboard);
									}
								}
							} 
						}
					});
				}
				);
				
		});
		for (Map.Entry<String,ObjectNode> entry : mapOfDashboards.entrySet()) 
            dbArray.add(entry.getValue()); 
		return dbArray;
	}
	
	private void applyVisilibityLayer(ArrayNode visibilityArray, JsonNode filter) {
		try { 
		visibilityArray.forEach(visibility -> {
			String visibilityKey = visibility.get(Constants.DashBoardConfig.KEY).asText();
			String filterKey = filter.get(Constants.DashBoardConfig.KEY).asText();
			if(visibilityKey.equals(filterKey)) { 
				ArrayNode valuesAllowed = (ArrayNode) visibility.get(Constants.DashBoardConfig.VALUE);
				ArrayNode valuesAvailable = (ArrayNode) filter.get(Constants.DashBoardConfig.VALUES);
				ObjectNode availableValuesList = new ObjectMapper().createObjectNode(); 
				ArrayNode availableValuesArray = availableValuesList.putArray(Constants.DashBoardConfig.VALUES);
				List<String> allowedValuesList = new ArrayList<>();
				valuesAllowed.forEach(allowedValue -> { 
					allowedValuesList.add(allowedValue.asText());  
				});
				for(int i = 0 ; i < valuesAvailable.size() ; i++) { 
					if(allowedValuesList.contains(valuesAvailable.get(i).asText())) { 
						availableValuesArray.add(valuesAvailable.get(i).asText());  
					}
				}
				if(availableValuesArray.size() > 0) { 
					ObjectNode filterObjectNode = (ObjectNode) filter;
					filterObjectNode.put(Constants.DashBoardConfig.VALUES, availableValuesArray);
				}
			}
		});
		} catch (Exception e) { 
			
		}
	}
	
	@Override
	public List<Dashboard> getDashboardsForProfile(String profileName, List<RoleDto> rolesOfUser, String requestRealm)
			throws AINException, IOException {
		ObjectNode roleMappingNode = configurationLoader.getConfigForProfile(profileName,
				ConfigurationLoader.ROLE_DASHBOARD_CONFIG);
		ArrayNode rolesArray = (ArrayNode) roleMappingNode.findValue(Constants.DashBoardConfig.ROLES);
		ArrayNode dbArray = JsonNodeFactory.instance.arrayNode();
		List<Dashboard> dashboardList = new LinkedList<Dashboard>(); 
		rolesArray.forEach(role -> {
			Object roleName = rolesOfUser.stream()
					.filter(x -> role.get(Constants.DashBoardConfig.ROLE_NAME).asText().equals(x.getName())).findAny()
					.orElse(null);
			logger.info("Role Name: " + roleName);
			RoleDto dto = RoleDto.class.cast(roleName);
			if (dto != null && dto.getId() != null && role.get(Constants.DashBoardConfig.ROLE_NAME).asText().equals(dto.getName()))
				role.get(Constants.DashBoardConfig.REALMS).forEach(realmForRole -> {
					String roleRealm = realmForRole.get(Constants.DashBoardConfig.NAME).asText(); 
					if(roleRealm.equals(requestRealm)) { 
						realmForRole.get(Constants.DashBoardConfig.DASHBOARDS).forEach(db -> {
							JsonNode name = JsonNodeFactory.instance.textNode("");
							JsonNode id = JsonNodeFactory.instance.textNode("");
							JsonNode landingDashboard = JsonNodeFactory.instance.booleanNode(Boolean.FALSE); 
							name = db.get(Constants.DashBoardConfig.NAME);
							id = db.get(Constants.DashBoardConfig.ID);
							if(db.get(Constants.DashBoardConfig.LANDING_DASHBOARD) != null) { 
								landingDashboard = db.get(Constants.DashBoardConfig.LANDING_DASHBOARD);
							} 
							Dashboard dashboard = new Dashboard(id.asText(), name.asText(), null, landingDashboard.asBoolean());
							dashboardList.add(dashboard);
						});
					}
				});
		});
		Set<String> nameSet = new HashSet<>();
		Set<String> idSet = new HashSet<>();
		List<Dashboard> dashboardDistinctByName = dashboardList.stream()
		            .filter(e -> nameSet.add(e.getName()))
		            .filter(e -> idSet.add(e.getId()))
		            .collect(Collectors.toList());
		return dashboardDistinctByName;
	}

	@Override
	public ArrayNode getReportsConfiguration(String profileName, String dashboardId, String catagory,
			List<RoleDto> roleIds) throws AINException, IOException {


		Calendar cal = Calendar.getInstance();
		cal.set(cal.getWeekYear()-1, Calendar.APRIL, 1);
		Date startDate = cal.getTime();
		Date endDate = new Date();
		
		// To show the date selection if needed 
		// String fyInfo = "From " + Constants.DASHBOARD_DATE_FORMAT.format(startDate) + " to " + Constants.DASHBOARD_DATE_FORMAT.format(endDate);

		ObjectNode dashboardNode = configurationLoader.getConfigForProfile(profileName, ConfigurationLoader.MASTER_REPORTS_CONFIG);
		ArrayNode dashboardNodes = (ArrayNode) dashboardNode.findValue(Constants.DashBoardConfig.DASHBOARDS);

		ObjectNode roleMappingNode = configurationLoader.getConfigForProfile(profileName, ConfigurationLoader.ROLE_REPORTS_CONFIG);
		ArrayNode rolesArray = (ArrayNode) roleMappingNode.findValue(Constants.DashBoardConfig.ROLES);
		ArrayNode dbArray = JsonNodeFactory.instance.arrayNode();

		rolesArray.forEach(role -> {
			Object roleId = roleIds.stream()
					.filter(x -> role.get(Constants.DashBoardConfig.ROLE_ID).asLong() == (x.getId())).findAny()
					.orElse(null);
			if (null != roleId) {
				ArrayNode visArray = JsonNodeFactory.instance.arrayNode();
				ArrayNode filterArray = JsonNodeFactory.instance.arrayNode();
				// checks role has given db id
				role.get(Constants.DashBoardConfig.DASHBOARDS).forEach(db -> {
					ArrayNode visibilityArray = (ArrayNode) db.findValue(Constants.DashBoardConfig.VISIBILITY);
					ObjectNode copyDashboard = objectMapper.createObjectNode();
					
					JsonNode name = JsonNodeFactory.instance.textNode("");
					JsonNode id = JsonNodeFactory.instance.textNode("");
					// Set the FY Info in Title if needed
					// JsonNode title = JsonNodeFactory.instance.textNode(fyInfo);

					if (db.get(Constants.DashBoardConfig.ID).asText().equalsIgnoreCase(dashboardId)) {
						// dasboardNodes.forEach(dbNode -> {
						for (JsonNode dbNode : dashboardNodes) {
							if (dbNode.get(Constants.DashBoardConfig.ID).asText().equalsIgnoreCase(dashboardId)) {
								logger.info("dbNode: " + dbNode);
								name = dbNode.get(Constants.DashBoardConfig.NAME);
								id = dbNode.get(Constants.DashBoardConfig.ID);
								dbNode.get(Constants.DashBoardConfig.VISUALISATIONS).forEach(visual -> {
									visArray.add(visual);
								});
								dbNode.get(Constants.DashBoardConfig.FILTERS).forEach(filter -> {
									JsonNode node = filter.deepCopy(); 
									applyVisilibityLayer(visibilityArray, node);
									filterArray.add(node);
								});
							}
							copyDashboard.set(Constants.DashBoardConfig.NAME, name);
							copyDashboard.set(Constants.DashBoardConfig.ID, id);
							// add TITLE with variable dynamically
							// copyDashboard.set(Constants.DashBoardConfig.TITLE, title);
							copyDashboard.set(Constants.DashBoardConfig.FILTERS, filterArray);
							copyDashboard.set(Constants.DashBoardConfig.VISUALISATIONS, visArray);

						} // );
						dbArray.add(copyDashboard);
					}
				});
			}
		});
		return dbArray;
	}

	@Override
	public Object getReportsForProfile(String profileName, List<RoleDto> roleIds) throws AINException, IOException {
		ObjectNode roleMappingNode = configurationLoader.getConfigForProfile(profileName,
				ConfigurationLoader.ROLE_REPORTS_CONFIG);
		ArrayNode rolesArray = (ArrayNode) roleMappingNode.findValue(Constants.DashBoardConfig.ROLES);
		ArrayNode dbArray = JsonNodeFactory.instance.arrayNode();

		rolesArray.forEach(role -> {
			Object roleId = roleIds.stream()
					.filter(x -> role.get(Constants.DashBoardConfig.ROLE_ID).asLong() == (x.getId())).findAny()
					.orElse(null);
			logger.info("roleId: " + roleId);
			RoleDto dto = RoleDto.class.cast(roleId);
			if (dto != null && dto.getId() != null && role.get(Constants.DashBoardConfig.ROLE_ID).asLong() == dto.getId())
				role.get(Constants.DashBoardConfig.DASHBOARDS).forEach(db -> {
					JsonNode name = JsonNodeFactory.instance.textNode("");
					JsonNode id = JsonNodeFactory.instance.textNode("");
					JsonNode description = JsonNodeFactory.instance.textNode("");
					name = db.get(Constants.DashBoardConfig.NAME);
					id = db.get(Constants.DashBoardConfig.ID);
					description  = db.get(Constants.DashBoardConfig.DESCRIPTION);
					ObjectNode copyDashboard = objectMapper.createObjectNode();
					copyDashboard.set(Constants.DashBoardConfig.NAME, name);
					copyDashboard.set(Constants.DashBoardConfig.ID, id);
					copyDashboard.set(Constants.DashBoardConfig.DESCRIPTION, description);
					dbArray.add(copyDashboard);
				});

		});
		return dbArray;
	}

	@Override
	public List<String> getUserInfo(String authToken, String userId) {
		String url = appConfig.getUserReadHost() + appConfig.getUserReadApi() + userId; 
		List<String> roleList = new ArrayList<String>(); 
		HttpHeaders headers = new HttpHeaders();
		if (authToken != null && !authToken.isEmpty()) { 
			headers.add("Authorization", appConfig.getUserReadApiKey());
			headers.add("x-authenticated-user-token", authToken);
		}
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> headerEntity = new HttpEntity<>("{}", headers);

		JsonNode responseNode = null;
		try {
			ResponseEntity<Object> response = retryTemplate.getForEntity(url, headerEntity);
			responseNode = new ObjectMapper().convertValue(response.getBody(), JsonNode.class);
			logger.info("RestTemplate response :- " + responseNode);

		} catch (HttpClientErrorException e) {
			logger.error("get client exception: " + e.getMessage());
		}
		if(responseNode != null && responseNode.get("result") != null && responseNode.get("result").get("response") != null && responseNode.get("result").get("response").get("roles") != null) { 
			ArrayNode arrayNode = (ArrayNode) responseNode.get("result").get("response").get("roles");
			arrayNode.forEach(eachNode -> {
				roleList.add(eachNode.asText()); 
			});
		}
		System.out.println("Role List : " + roleList.toString());
		return roleList;
	}

}
