package com.example.pocketmenu.utils;

import com.example.pocketmenu.data.repository.ShoppingListRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateUtils {

    private DateUtils() {}

    public static Date getMonday(Date anyDay) {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.setTime(anyDay);
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -6);
        } else {
            cal.add(Calendar.DAY_OF_MONTH,
                    -(cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY));
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date normalizeDate(Date date) {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static List<Date> getWeekDates(Date monday) {
        List<Date> dates = new ArrayList<>();
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.setTime(monday);
        for (int i = 0; i < 7; i++) {
            dates.add(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dates;
    }

    public static List<String> getCurrentAndNextWeekIds(int count) {
        List<String> weekIds = new ArrayList<>();
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.setMinimalDaysInFirstWeek(4);
        cal.setFirstDayOfWeek(Calendar.MONDAY);

        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? -6 : Calendar.MONDAY - dow;
        cal.add(Calendar.DAY_OF_MONTH, diff);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < count; i++) {
            String weekId = ShoppingListRepository.getWeekId(cal.getTime());
            if (!weekIds.contains(weekId)) weekIds.add(weekId);
            cal.add(Calendar.DAY_OF_MONTH, 7);
        }
        return weekIds;
    }

    public static Date getSunday(Date monday) {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.setTime(monday);
        cal.add(Calendar.DAY_OF_MONTH, 6);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }
}