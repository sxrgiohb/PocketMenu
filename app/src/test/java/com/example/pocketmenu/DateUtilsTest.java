package com.example.pocketmenu;

import com.example.pocketmenu.data.repository.ShoppingListRepository;
import com.example.pocketmenu.utils.DateUtils;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateUtilsTest {

    @Test
    public void getMonday_fromMonday_returnsSameDay() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 16); // monday
        Date monday = DateUtils.getMonday(cal.getTime());
        Calendar result = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        result.setTime(monday);
        assertEquals(Calendar.MONDAY, result.get(Calendar.DAY_OF_WEEK));
        assertEquals(16, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void getMonday_fromWednesday_returnsPreviousMonday() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 18); // wednesday
        Date monday = DateUtils.getMonday(cal.getTime());
        Calendar result = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        result.setTime(monday);
        assertEquals(Calendar.MONDAY, result.get(Calendar.DAY_OF_WEEK));
        assertEquals(16, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void getMonday_fromSunday_returnsPreviousMonday() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 22); // sunday
        Date monday = DateUtils.getMonday(cal.getTime());
        Calendar result = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        result.setTime(monday);
        assertEquals(Calendar.MONDAY, result.get(Calendar.DAY_OF_WEEK));
        assertEquals(16, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void getMonday_fromSaturday_returnsPreviousMonday() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 21); // saturday
        Date monday = DateUtils.getMonday(cal.getTime());
        Calendar result = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        result.setTime(monday);
        assertEquals(Calendar.MONDAY, result.get(Calendar.DAY_OF_WEEK));
        assertEquals(16, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void getMonday_resultIsNormalizedToMidnight() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 18, 15, 30, 45);
        Date monday = DateUtils.getMonday(cal.getTime());
        Calendar result = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        result.setTime(monday);
        assertEquals(0, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
    }

    // normalizeDate
    @Test
    public void normalizeDate_setsTimeToMidnight() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 16, 14, 30, 45);
        Date normalized = DateUtils.normalizeDate(cal.getTime());
        Calendar result = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        result.setTime(normalized);
        assertEquals(0, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND));
    }

    @Test
    public void normalizeDate_preservesDatePart() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 16, 14, 30, 45);
        Date normalized = DateUtils.normalizeDate(cal.getTime());
        Calendar result = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        result.setTime(normalized);
        assertEquals(2026, result.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, result.get(Calendar.MONTH));
        assertEquals(16, result.get(Calendar.DAY_OF_MONTH));
    }

    // getWeekDates
    @Test
    public void getWeekDates_returnsSevenDays() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 16);
        List<Date> dates = DateUtils.getWeekDates(cal.getTime());
        assertEquals(7, dates.size());
    }

    @Test
    public void getWeekDates_firstDayIsMonday() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 16);
        List<Date> dates = DateUtils.getWeekDates(cal.getTime());
        Calendar result = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        result.setTime(dates.get(0));
        assertEquals(Calendar.MONDAY, result.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void getWeekDates_lastDayIsSunday() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 16);
        List<Date> dates = DateUtils.getWeekDates(cal.getTime());
        Calendar result = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        result.setTime(dates.get(6));
        assertEquals(Calendar.SUNDAY, result.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void getWeekDates_consecutiveDays() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 16);
        List<Date> dates = DateUtils.getWeekDates(cal.getTime());
        for (int i = 0; i < 6; i++) {
            long diffMs = dates.get(i + 1).getTime() - dates.get(i).getTime();
            long diffDays = diffMs / (24 * 60 * 60 * 1000);
            assertEquals(1, diffDays);
        }
    }

    // getCurrentAndNextWeekIds
    @Test
    public void getCurrentAndNextWeekIds_returnsCorrectCount() {
        List<String> weekIds = DateUtils.getCurrentAndNextWeekIds(4);
        assertEquals(4, weekIds.size());
    }

    @Test
    public void getCurrentAndNextWeekIds_allHaveCorrectFormat() {
        List<String> weekIds = DateUtils.getCurrentAndNextWeekIds(4);
        for (String weekId : weekIds) {
            assertTrue(weekId.matches("\\d{4}-W\\d{2}"));
        }
    }

    @Test
    public void getCurrentAndNextWeekIds_noDuplicates() {
        List<String> weekIds = DateUtils.getCurrentAndNextWeekIds(4);
        long distinctCount = weekIds.stream().distinct().count();
        assertEquals(4, distinctCount);
    }

    @Test
    public void getCurrentAndNextWeekIds_areConsecutive() {
        List<String> weekIds = DateUtils.getCurrentAndNextWeekIds(4);
        for (int i = 0; i < weekIds.size() - 1; i++) {
            Date monday1 = ShoppingListRepository.getMondayFromWeekId(weekIds.get(i));
            Date monday2 = ShoppingListRepository.getMondayFromWeekId(weekIds.get(i + 1));
            Calendar cal1 = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
            Calendar cal2 = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
            cal1.setTime(monday1);
            cal2.setTime(monday2);
            cal1.add(Calendar.DAY_OF_MONTH, 7);
            assertEquals(cal1.get(Calendar.DAY_OF_MONTH), cal2.get(Calendar.DAY_OF_MONTH));
            assertEquals(cal1.get(Calendar.MONTH), cal2.get(Calendar.MONTH));
            assertEquals(cal1.get(Calendar.YEAR), cal2.get(Calendar.YEAR));
        }
    }

    @Test
    public void getCurrentAndNextWeekIds_firstIsCurrentWeek() {
        List<String> weekIds = DateUtils.getCurrentAndNextWeekIds(4);
        String currentWeekId = ShoppingListRepository.getWeekId(new Date());
        assertEquals(currentWeekId, weekIds.get(0));
    }

    // getSunday
    @Test
    public void getSunday_returnsSundayDate() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 16);
        Date sunday = DateUtils.getSunday(cal.getTime());
        Calendar result = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        result.setTime(sunday);
        assertEquals(Calendar.SUNDAY, result.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void getSunday_isSevenDaysAfterMonday() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 16);
        Date monday = cal.getTime();
        Date sunday = DateUtils.getSunday(monday);
        Calendar mondayCal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        Calendar sundayCal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        mondayCal.setTime(monday);
        sundayCal.setTime(sunday);
        mondayCal.add(Calendar.DAY_OF_MONTH, 6);
        assertEquals(mondayCal.get(Calendar.DAY_OF_MONTH), sundayCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(mondayCal.get(Calendar.MONTH), sundayCal.get(Calendar.MONTH));
    }

    @Test
    public void getSunday_timeIsEndOfDay() {
        Calendar cal = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        cal.set(2026, Calendar.MARCH, 16);
        Date sunday = DateUtils.getSunday(cal.getTime());
        Calendar result = Calendar.getInstance(Locale.forLanguageTag("es-ES"));
        result.setTime(sunday);
        assertEquals(23, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, result.get(Calendar.MINUTE));
        assertEquals(59, result.get(Calendar.SECOND));
    }
}