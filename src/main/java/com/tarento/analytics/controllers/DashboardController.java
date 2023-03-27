package com.tarento.analytics.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.constant.ErrorCode;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.RequestDto;
import com.tarento.analytics.dto.RoleDto;
import com.tarento.analytics.dto.UserDto;
import com.tarento.analytics.exception.AINException;
import com.tarento.analytics.model.Item;
import com.tarento.analytics.org.service.ClientServiceFactory;
import com.tarento.analytics.producer.AnalyticsProducer;
import com.tarento.analytics.repository.RedisRepository;
import com.tarento.analytics.service.MetadataService;
import com.tarento.analytics.service.impl.RetryTemplate;
import com.tarento.analytics.utils.PathRoutes;
import com.tarento.analytics.utils.ResponseGenerator;

@RestController
@RequestMapping(PathRoutes.DashboardApi.DASHBOARD_ROOT_PATH)
public class DashboardController {

	public static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

	@Autowired
	private MetadataService metadataService;

	@Autowired
	private ClientServiceFactory clientServiceFactory;

	@Autowired
	private AnalyticsProducer analyticsProducer;

	@Autowired
	private RetryTemplate retryTemplate;

	@Autowired
	private RedisRepository redisRepository;

	private static final String MESSAGE = "message";

	@GetMapping(value = PathRoutes.DashboardApi.TEST_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTest() throws JsonProcessingException {
		return ResponseGenerator.successResponse("success");

	}

	@PostMapping(value = "/item")
	public String addItem(@RequestBody Item item) {
		redisRepository.save(item);
		return item.toString();
	}

	@GetMapping(value = "/item")
	public List<String> getItem(@RequestParam(value = "id", required = false) String id) {
		return redisRepository.findAll();
	}

	@GetMapping(value = PathRoutes.DashboardApi.GET_DASHBOARD_CONFIG + "/{profileName}" + "/{dashboardId}")
	public String getDashboardConfiguration(@PathVariable String profileName, @PathVariable String dashboardId,
			@RequestParam(value = "catagory", required = false) String catagory,
			@RequestHeader(value = "x-user-info", required = false) String xUserInfo,
			@RequestHeader(value = "X-Channel-Id", required = false) String channelId,
			@RequestHeader(value = "x-authenticated-userid", required = false) String fullUserId,
			@RequestHeader(value = "x-authenticated-user-token", required = false) String userToken,
			@RequestHeader(value = "Authorization", required = false) String authorization, ServletWebRequest request)
			throws IOException, AINException {
		logger.info("Channel ID : " + channelId);
		logger.info("User Token : " + userToken);
		logger.info("Authenticated Full User ID : " + fullUserId);
		String userId = resolveUserId(fullUserId); 
		logger.info("Authenticated Short User ID : " + userId);
		List<String> roleList = metadataService.getUserInfo(userToken, userId);
		UserDto user = buildUserObject(xUserInfo, userId, roleList);
		logger.info("user {} ", xUserInfo);
		return ResponseGenerator.successResponse(
				metadataService.getDashboardConfiguration(profileName, dashboardId, catagory, user.getRoles()));
	}
	
	@GetMapping(value = PathRoutes.DashboardApi.GET_REPORTS_CONFIG + "/{profileName}" + "/{dashboardId}")
	public String getReportsConfiguration(@PathVariable String profileName, @PathVariable String dashboardId,
			@RequestParam(value = "catagory", required = false) String catagory,
			@RequestHeader(value = "x-user-info", required = false) String xUserInfo) throws AINException, IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		UserDto user = gson.fromJson(xUserInfo, UserDto.class);

		logger.info("user {} ", xUserInfo);
		return ResponseGenerator.successResponse(
				metadataService.getReportsConfiguration(profileName, dashboardId, catagory, user.getRoles()));
	}

	@GetMapping(value = PathRoutes.DashboardApi.GET_DASHBOARDS_FOR_PROFILE + "/{profileName}")
	public String getDashboardsForProfile(@PathVariable String profileName,
			@RequestParam(value = "realm", required = false) String realm,
			@RequestHeader(value = "x-user-info", required = false) String xUserInfo,
			@RequestHeader(value = "X-Channel-Id", required = false) String channelId,
			@RequestHeader(value = "x-authenticated-userid", required = false) String fullUserId,
			@RequestHeader(value = "x-authenticated-user-token", required = false) String userToken)
			throws AINException, IOException {
		// Setting realm as default as portal based access is not needed right now
		if (StringUtils.isBlank(realm)) {
			realm = Constants.DashBoardConfig.DEFAULT_REALM;
		}
		logger.info("Channel ID : " + channelId);
		logger.info("User Token : " + userToken);
		logger.info("Authenticated Full User ID : " + fullUserId);
		String userId = resolveUserId(fullUserId);
		logger.info("Authenticated Short User ID : " + userId);
		UserDto user = new UserDto();
		List<String> roleList = metadataService.getUserInfo(userToken, userId);
		List<RoleDto> roles = new ArrayList<>();
		for (String roleItem : roleList) {
			RoleDto role = new RoleDto();
			role.setName(roleItem);
			role.setId(0000l);
			roles.add(role);
		}
		user.setRoles(roles);
		logger.info("user {} ", xUserInfo);
		return ResponseGenerator
				.successResponse(metadataService.getDashboardsForProfile(profileName, user.getRoles(), realm));
	}

	@GetMapping(value = PathRoutes.DashboardApi.GET_REPORTS_FOR_PROFILE + "/{profileName}")
	public String getReportsForProfile(@PathVariable String profileName,
			@RequestHeader(value = "x-user-info", required = false) String xUserInfo) throws AINException, IOException {
		/*
		 * Gson gson = new GsonBuilder().setPrettyPrinting().create();
		 * UserDto user = gson.fromJson(xUserInfo, UserDto.class);
		 */

		UserDto user = new UserDto();
		RoleDto role = new RoleDto();
		role.setId(2068l);
		List<RoleDto> roles = new ArrayList<>();
		roles.add(role);
		user.setRoles(roles);
		return ResponseGenerator.successResponse(metadataService.getReportsForProfile(profileName, user.getRoles()));
	}

	@PostMapping(value = PathRoutes.DashboardApi.GET_CHART_V2 + "/{profileName}")
	public String getVisualizationChartV2(@PathVariable String profileName, @RequestBody RequestDto requestDto,
			@RequestHeader(value = "x-user-info", required = false) String xUserInfo,
			@RequestHeader(value = "X-Channel-Id", required = false) String channelId,
			@RequestHeader(value = "x-authenticated-userid", required = false) String fullUserId,
			@RequestHeader(value = "x-authenticated-user-token", required = false) String userToken,
			@RequestHeader(value = "Authorization", required = false) String authorization, ServletWebRequest request)
			throws IOException {
		logger.info("Channel ID : " + channelId);
		logger.info("User Token : " + userToken);
		logger.info("Authenticated Full User ID : " + fullUserId);
		String userId = resolveUserId(fullUserId);
		logger.info("Authenticated Short User ID : " + userId);

		List<String> roleList = metadataService.getUserInfo(userToken, userId);
		logger.info("Request Detail: {} ", new Gson().toJson(requestDto));
		UserDto user = buildUserObject(xUserInfo, userId, roleList);

		// Getting the request information only from the Full Request
		AggregateRequestDto requestInfo = requestDto.getAggregationRequestDto();
		String response = "";
		try {
			if (requestDto.getAggregationRequestDto() == null) {
				logger.error("Please provide requested Visualization Details");
				throw new AINException(ErrorCode.ERR320, "Visualization Request is missing");
			}

			if (requestDto.getAggregationRequestDto() != null
					&& requestDto.getAggregationRequestDto().getFilters() != null &&
					StringUtils.isNotBlank(channelId)) {
				requestDto.getAggregationRequestDto().getFilters().put("mdo", channelId);
			}
			
			if (requestDto.getAggregationRequestDto() != null
					&& requestDto.getAggregationRequestDto().getFilters() != null &&
					StringUtils.isNotBlank(channelId)) {
				requestDto.getAggregationRequestDto().getFilters().put("mdo2", channelId);
			}

			if (requestDto.getAggregationRequestDto() != null
					&& requestDto.getAggregationRequestDto().getFilters() != null &&
					StringUtils.isNotBlank(userId)) {
				requestDto.getAggregationRequestDto().getFilters().put("userId", userId);
			}

			Object responseData = clientServiceFactory.get(profileName, requestInfo.getVisualizationCode())
					.getAggregatedData(profileName, requestInfo, user.getRoles());
			response = ResponseGenerator.successResponse(responseData);
			// Commenting the User Request Data Push - This was earlier used for Analytics
			// of Users and the Dashboards that they were consuming.
			/*
			 * Long responseTime = new Date().getTime();
			 * pushRequestsToLoggers(requestDto, user, requestTime, responseTime);
			 */
		} catch (AINException e) {
			logger.error("error while executing api getVisualizationChart");
			response = ResponseGenerator.failureResponse(e.getErrorCode(), e.getMessage());
		} catch (Exception e) {
			logger.error("error while executing api getVisualizationChart {} ", e.getMessage());
		}
		return response;
	}

	private UserDto buildUserObject(String xUserInfo, String userId, List<String> roleList) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		UserDto user = gson.fromJson(xUserInfo, UserDto.class);
		List<RoleDto> roles = new ArrayList<>();
		user = new UserDto();
		user.setUserName(userId);
		for (String roleName : roleList) {
			RoleDto role = new RoleDto();
			role.setId(2068l);
			role.setName(roleName);
			roles.add(role);
		}
		user.setRoles(roles);
		logger.info("user {} ", xUserInfo);
		return user;
	}

	@PostMapping(value = PathRoutes.DashboardApi.GET_REPORT)
	public String getReport(@PathVariable String profileName, @RequestBody RequestDto requestDto,
			@RequestHeader(value = "x-user-info", required = false) String xUserInfo,
			@RequestHeader(value = "Authorization", required = false) String authorization, ServletWebRequest request)
			throws IOException {
		/*
		 * logger.info("Request Detail:" + requestDto); Gson gson = new
		 * GsonBuilder().setPrettyPrinting().create(); UserDto user =
		 * gson.fromJson(xUserInfo, UserDto.class);
		 */
		UserDto user = new UserDto();
		logger.info("user {} ", xUserInfo);

		// Getting the request information only from the Full Request
		AggregateRequestDto requestInfo = requestDto.getAggregationRequestDto();
		// requestInfo.getFilters().putAll(headers);
		String response = "";
		try {
			if (requestDto.getAggregationRequestDto() == null) {
				logger.error("Please provide requested Visualization Details");
				throw new AINException(ErrorCode.ERR320, "Visualization Request is missing");
			}

			// To be removed once the development is complete
			if (StringUtils.isBlank(requestInfo.getModuleLevel())) {
				requestInfo.setModuleLevel(Constants.Modules.HOME_REVENUE);
			}
			Object responseData = clientServiceFactory.get(profileName, requestInfo.getVisualizationCode())
					.getAggregatedData(profileName, requestInfo, user.getRoles());
			// clientService.getAggregatedData(requestInfo, user.getRoles());
			response = ResponseGenerator.successResponse(responseData);
		} catch (AINException e) {
			logger.error("error while executing api getVisualizationChart");
			response = ResponseGenerator.failureResponse(e.getErrorCode(), e.getErrorMessage());
		} catch (Exception e) {
			logger.error("error while executing api getVisualizationChart {} ", e.getMessage());
			// could be bad request or internal server error
			// response =
			// ResponseGenerator.failureResponse(HttpStatus.BAD_REQUEST.toString(),"Bad
			// request");
		}
		return response;
	}
	
	private String resolveUserId(String fullUserId) { 
		String userId = fullUserId;
		if (StringUtils.isNotBlank(fullUserId) && fullUserId.contains(":")) {
			String[] userIdSeries = fullUserId.split(":");
			logger.info("UserID Series Split : " + userIdSeries);
			if (userIdSeries.length > 0) {
				userId = userIdSeries[2];
			}
		}
		return userId; 
	}

}
