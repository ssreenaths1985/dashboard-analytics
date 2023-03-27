package com.tarento.analytics.constant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {

	public static final long ACCESS_TOKEN_VALIDITY_SECONDS = 30l * 24l * 60l * 60l;
	public static final String SIGNING_KEY = "devglan123r";
	public static final String JWT_ISSUER = "http://devglan.com";
	public static final String JWT_GRANTED_AUTHORITY = "ROLE_ADMIN";

	/**
	 * Allowed Origins for CORS Bean
	 */
	public static final String GET = "GET";
	public static final String POST = "POST";
	public static final String PUT = "PUT";
	public static final String DELETE = "DELETE";
	public static final String OPTIONS = "OPTIONS";

	public static final int UNAUTHORIZED_ID = 401;
	public static final int SUCCESS_ID = 200;
	public static final int FAILURE_ID = 320;
	public static final String UNAUTHORIZED = "Invalid credentials. Please try again.";
	public static final String PROCESS_FAIL = "Process failed, Please try again.";
	public static final String SUCCESS = "success";

	public static final String PRIMARY = "PRIMARY";
	public static final String SECONDARY = "SECONDARY";
	public static final String TERNARY = "TERNARY";
	public static final String QUADNARY = "QUADNARY";

	// chart format

	public static final String D3 = "d3";
	public static final String CHARTJS = "chartjs";

	// chart type
	public static final String BAR = "bar";
	public static final String PIE = "pie";
	public static final String STACKEDBAR = "stackedbar";
	public static final String LINE = "line";
	public static final String HORIZONTAL_BAR = "horizontalBar";
	public static final String DOUGHNUT = "doughnut";
	public static final String HEAT = "heat";
	public static final String RADAR = "radar";

	public static final Long FEEDBACK_MESSAGE_TIMEOUT = 2000l;

	public static final String STORE_ID = "storeId";

	public static final String PLACEMENTS_DASHBOARD = "DASHBOARD";
	public static final String PLACEMENTS_HOME = "HOME";

	protected static final List<Long> RATING_LIST = new ArrayList<>(Arrays.asList(1l, 2l, 3l, 4l, 5l));
	protected static final List<String> RATING_LIST_STRING = new ArrayList<>(Arrays.asList("1", "2", "3", "4", "5"));
	protected static final List<String> RATING_LIST_STRING_STAR = new ArrayList<>(
			Arrays.asList("1 Star", "2 Star", "3 Star", "4 Star", "5 Star"));

	public final class Modules {
		private Modules() {
		}

		public static final String HOME_REVENUE = "HOME_REVENUE";
		public static final String HOME_SERVICES = "HOME_SERVICES";
		public static final String COMMON = "COMMON";
		public static final String PT = "PT";
		public static final String TL = "TL";
	}

	public final class KafkaTopics {
		private KafkaTopics() {
		}

		public static final String NEW_CONTENT_MESSAGE = "SaveContent";
		public static final String SIMULATOR_TRANSACTION = "SaveTransaction";
	}

	public final class ConfigurationFiles {
		private ConfigurationFiles() {
		}

		public static final String CHART_API_CONFIG = "ChartApiConfig.json";
		public static final String REPORT_API_CONFIG = "ReportApiConfig.json";
	}

	public final class JsonPaths {
		private JsonPaths() {
		}

		public static final String CHART_TYPE = "chartType";
		public static final String QUERIES = "queries";
		public static final String AGGREGATION_QUERY = "aggrQuery";
		public static final String INDEX_NAME = "indexName";
		public static final String REQUEST_QUERY_MAP = "requestQueryMap";
		public static final String DATE_REF_FIELD = "dateRefField";
		public static final String AGGS = "aggs";
		public static final String AGGREGATIONS = "aggregations";
		public static final String MODULE = "module";
		public static final String INTERVAL_VAL = "intervalvalue";
		public static final String INTERVAL = "interval";
		public static final String IS_MDMS_ENABALED = "isMdmsEnabled";
		public static final String INSIGHT = "insight";
		public static final String ES_INSTANCE = "esInstance";
		public static final String ALWAYS_VIEW = "alwaysView";
		public static final String DATA_SOURCE = "dataSource";
		public static final String ACTION = "action";
		public static final String CONTROLS = "controls";
		public static final String KEY = "key";
		public static final String VALUE = "value";
		public static final String CHART_NAME = "chartName";
		public static final String ENRICHMENT_LOOKUP= "enrichmentLookUp";
	}
	
	public final class DruidQueryConstants {
		private DruidQueryConstants() {
		}
		public static final String QUERY = "query";
		public static final String RESULT_FORMAT= "resultFormat";
		public static final String HEADER = "header";
		public static final String USE_FALLBACK= "useFallback";
		public static final String SQL_OUTER_LIMIT = "sqlOuterLimit";
		public static final String CONTEXT = "context";
		public static final String OBJECT= "object";
	}

	public final class ResponseDataSource {
		private ResponseDataSource() {
		}

		public static final String DRUID_ONLY = "druidOnly";
		public static final String ES_ONLY = "esOnly";
		public static final String REDIS_ONLY = "redisOnly";
		public static final String DRUID_ES_BOTH = "druidEsBoth";
		public static final String DRUID_REDIS_BOTH = "druidRedisBoth";
		public static final String ES_REDIS_BOTH = "esRedisBoth";
		
	}
	
	public final class Filters {
		private Filters() {
		}

		public static final String MODULE = "module";
		public static final String FILTER_ALL = "*";
	}

	public final class Catagory {
		private Catagory() {
		}

		public static final String SEVICE = "service";
		public static final String REVENUE = "revenue";
	}

	public final class DashBoardConfig {
		private DashBoardConfig() {
		}

		public static final String ROLES = "roles";
		public static final String ROLE_ID = "roleId";
		public static final String ROLE_NAME = "roleName";
		public static final String DASHBOARDS = "dashboards";
		public static final String REALMS = "realms";
		public static final String VISUALISATIONS = "visualizations";
		public static final String NAME = "name";
		public static final String DESCRIPTION = "description"; 
		public static final String ID = "id";
		public static final String TITLE = "title";
		public static final String WIDGET_CHARTS = "widgetCharts";
		public static final String FILTERS = "filters";
		public static final String VISIBILITY = "visibility";
		public static final String KEY = "key";
		public static final String VALUE = "value";
		public static final String VALUES = "values";
		public static final String IS_ACTIVE = "isActive"; 
		public static final String STYLE = "style";
		public static final String WIDGET_TITLE = "widgetTitle"; 
		public static final String SHOW_FILTERS = "showFilters";
		public static final String SHOW_WIDGETS = "showWidgets"; 
		public static final String SHOW_DATE_FILTER = "showDateFilter"; 
		public static final String SHOW_WIDGET_TITLE = "showWidgetTitle"; 
		public static final String DEFAULT_REALM = "default"; 
		public static final String LANDING_DASHBOARD = "landingDashboard"; 
		
	}

	public final class MDMSKeys {
		private MDMSKeys() {
		}

		public static final String CODE = "code";
		public static final String DISTRICT_CODE = "districtCode";
		public static final String DDR_NAME = "ddrName";
		public static final String TENANTS = "tenants";
		public static final String KEY = "key";
	}

	public enum Interval {
		week, month, year, day, minute, date
	}

	public final class PostAggregationTheories {
		private PostAggregationTheories() {
		}

		public static final String RESPONSE_DIFF_DATES = "repsonseToDifferenceOfDates";
		public static final String QUOTIENT_OF_MAP_VALUES = "quotientOfMapValues";
		public static final String COUNT_OF_RECORDS = "CountOfRecords";
		public static final String COUNT_OF_RECORDS_NON_ZERO = "CountOfRecordsNonZero";
		public static final String SUM_OF_VALUES = "SumOfValues"; 
		public static final String LIST_OF_ALL_VALUES = "ListOfAllItems";
		public static final String LIST_OF_ALL_VALUES_LIMIT10 = "ListOfAllItemsLimit10";
		public static final String LIST_OF_ALL_VALUES_AND_GROUP = "ListOfAllItemsAndGroup";
		public static final String DRUID_BY_REDIS = "DRUID/REDIS";
		public static final String REDIS_BY_REDIS = "REDIS/REDIS";
		public static final String DRUID_BY_DRUID = "DRUID/DRUID";
		public static final String PERCENTAGE_HELPER = "PERCENTAGEHELPER";
		public static final String MULTIPLY_PERCENT_HELPER = "MULTIPLYPERCENT";
	}

	public enum ClientServiceType {
		DEFAULT_CLIENT, MDMS_CLIENT
	}
	
	public final class Configurations {
		private Configurations() {
		}

		public static final int LIMIT_OF_CHARS = 15;
	}
	
	public enum DataSourceType {
		ES, DRUID, REDIS
	}

	public final SimpleDateFormat dashboardDateFormat = new SimpleDateFormat("MMM dd, yyyy");

}
