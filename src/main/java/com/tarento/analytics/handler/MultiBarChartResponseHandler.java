package com.tarento.analytics.handler;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dto.AggregateDto;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.Data;
import com.tarento.analytics.dto.Plot;

/**
 * This handles ES response for single index, multiple index to represent data
 * as line chart Creates plots by merging/computing(by summation) index values
 * for same key AGGS_PATH : this defines the path/key to be used to search the
 * tree VALUE_TYPE : defines the data type for the value formed, this could be
 * amount, percentage, number
 *
 */
@Component
public class MultiBarChartResponseHandler implements IResponseHandler {
	public static final Logger logger = LoggerFactory.getLogger(MultiBarChartResponseHandler.class);

	@Override
	public AggregateDto translate(String profileName, AggregateRequestDto requestDto, ObjectNode aggregations, Map<String, List<String>> responseDataSource)
			throws IOException {

		List<Data> dataList = new LinkedList<>();

		JsonNode aggregationNode = aggregations.get(AGGREGATIONS);
		JsonNode chartNode = requestDto.getChartNode();
		boolean isRequestInterval = null == requestDto.getRequestDate() ? false
				: requestDto.getRequestDate().getInterval() != null
						&& !requestDto.getRequestDate().getInterval().isEmpty();
		String symbol = chartNode.get(IResponseHandler.VALUE_TYPE).asText();
		ArrayNode aggrsPaths = (ArrayNode) chartNode.get(IResponseHandler.AGGS_PATH);
		String interval = isRequestInterval ? requestDto.getRequestDate().getInterval()
				: chartNode.get(Constants.JsonPaths.INTERVAL).asText();
		if (interval == null || interval.isEmpty()) {
			throw new RuntimeException("Interval must have value from config or request");
		}
		aggrsPaths.forEach(headerPath -> {
			List<JsonNode> aggrNodes = aggregationNode.findValues(headerPath.asText());

			Map<String, Double> plotMap = new LinkedHashMap<>();
			List<Double> totalValues = new LinkedList<>();
			String headerLabel = chartNode.get(HEADER_LABEL) != null ? chartNode.get(HEADER_LABEL).asText()
					: HEADER_LABEL_DEFAULT;
			String valueLabel = chartNode.get(VALUE_LABEL) != null ? chartNode.get(VALUE_LABEL).asText()
					: VALUE_LABEL_DEFAULT;
			aggrNodes.stream().forEach(aggrNode -> {
				if (aggrNode.findValues(IResponseHandler.BUCKETS).size() > 0) {

					ArrayNode buckets = (ArrayNode) aggrNode.findValues(IResponseHandler.BUCKETS).get(0);
					buckets.forEach(bucket -> {
						String bkey = bucket.findValue(IResponseHandler.KEY).asText();
						String key = "";
						if (interval.equals("15m")) {
							key = getIntervalKey15m(bkey, interval);
						} else {
							key = getIntervalKey(bkey, Constants.Interval.valueOf(interval));
						}
						// String key = bucket.findValue(IResponseHandler.KEY).asText();
						double value = ((bucket.findValue(IResponseHandler.VALUE) != null)
								? bucket.findValue(IResponseHandler.VALUE).asDouble()
								: bucket.findValue(IResponseHandler.DOC_COUNT).asDouble());
						plotMap.put(key, plotMap.get(key) == null ? new Double("0") + value : plotMap.get(key) + value);
						totalValues.add(value);
					});
				}
			});
			List<Plot> plots = plotMap.entrySet().stream()
					.map(e -> new Plot(e.getKey(), e.getValue(), symbol, headerLabel, valueLabel))
					.collect(Collectors.toList());
			try {
				Data data = new Data(headerPath.asText(), (totalValues == null || totalValues.isEmpty()) ? 0.0
						: totalValues.stream().reduce(0.0, Double::sum), symbol);
				/*
				 * Data data; if(!isCumulative) { data = new Data(headerPath.asText(),
				 * (totalValues==null || totalValues.isEmpty()) ? 0.0 :
				 * totalValues.stream().reduce(0.0, Double::sum), symbol); } else { data = new
				 * Data(headerPath.asText(), (totalValues==null || totalValues.isEmpty()) ? 0.0
				 * : plots.get(plots.size()-1), symbol); }
				 */
				data.setPlots(plots);
				dataList.add(data);
			} catch (Exception e) {
				logger.error(" Legend/Header " + headerPath.asText() + " exception occurred " + e.getMessage());
			}
		});
		return getAggregatedDto(chartNode, dataList, requestDto.getVisualizationCode());
	}

	private String getIntervalKey(String epocString, Constants.Interval interval) {
		try {
			long epoch = Long.parseLong(epocString);
			Date expiry = new Date(epoch);
			Calendar cal = Calendar.getInstance();
			cal.setTime(expiry);
			String hour = String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
			String min = String.valueOf(cal.get(Calendar.MINUTE));
			String day = String.valueOf(cal.get(Calendar.DATE));
			String month = monthNames(cal.get(Calendar.MONTH) + 1);
			String year = "" + cal.get(Calendar.YEAR);

			String intervalKey = "";
			if (interval.equals(Constants.Interval.day)) {
				intervalKey = day.concat("-").concat(month);
			} else if (interval.equals(Constants.Interval.week)) {
				intervalKey = day.concat("-").concat(month);
			} else if (interval.equals(Constants.Interval.year)) {
				intervalKey = year;
			} else if (interval.equals(Constants.Interval.month)) {
				intervalKey = month.concat("-").concat(year);
			} else {
				throw new RuntimeException("Invalid interval");
			}

			// String weekMonth = "Week " + cal.get(Calendar.WEEK_OF_YEAR) /*+ " : " +
			// dayMonth*/;//+" of Month "+ (cal.get(Calendar.MONTH) + 1);
			return intervalKey;
		} catch (Exception e) {
			return epocString;
		}
	}

	private String getIntervalKey15m(String epocString, String interval) {
		try {
			long epoch = Long.parseLong(epocString);
			Date expiry = new Date(epoch);
			Calendar cal = Calendar.getInstance();
			cal.setTime(expiry);
			cal.add(Calendar.HOUR, 5);
			cal.add(Calendar.MINUTE, 30);
			String hour = String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
			String min = String.valueOf(cal.get(Calendar.MINUTE));
			String day = String.valueOf(cal.get(Calendar.DATE));
			String month = monthNames(cal.get(Calendar.MONTH) + 1);
			String year = "" + cal.get(Calendar.YEAR);

			String intervalKey = "";
			intervalKey = hour.concat("h:").concat(min).concat("m");

			// String weekMonth = "Week " + cal.get(Calendar.WEEK_OF_YEAR) /*+ " : " +
			// dayMonth*/;//+" of Month "+ (cal.get(Calendar.MONTH) + 1);
			return intervalKey;
		} catch (Exception e) {
			return epocString;
		}
	}

	private String monthNames(int month) {
		if (month == 1)
			return "Jan";
		else if (month == 2)
			return "Feb";
		else if (month == 3)
			return "Mar";
		else if (month == 4)
			return "Apr";
		else if (month == 5)
			return "May";
		else if (month == 6)
			return "Jun";
		else if (month == 7)
			return "Jul";
		else if (month == 8)
			return "Aug";
		else if (month == 9)
			return "Sep";
		else if (month == 10)
			return "Oct";
		else if (month == 11)
			return "Nov";
		else if (month == 12)
			return "Dec";
		else
			return "Month";
	}
}
