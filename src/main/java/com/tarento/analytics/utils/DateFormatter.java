package com.tarento.analytics.utils;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DateFormatter {


	private static ObjectMapper objectMapper = new ObjectMapper();

    public static String getIntervalKey(String startDateEpoch, String endDateEpoch) throws JsonProcessingException{
    	try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); 
		    Date firstDate = sdf.parse(startDateEpoch);
		    Date secondDate = sdf.parse(endDateEpoch);

		    long diffInMillies = Math.abs(secondDate.getTime() - firstDate.getTime());
		    long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
		    if(diff <= 7) { 
		    	return "P1D"; 
		    } else if(diff > 7 && diff <= 31) {
		    	return "P1W";
		    } else { 
		    	return "P1M"; 
		    }
		} catch (Exception e) {
			return "P1M";
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

	private static String monthNames(int month) {
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
