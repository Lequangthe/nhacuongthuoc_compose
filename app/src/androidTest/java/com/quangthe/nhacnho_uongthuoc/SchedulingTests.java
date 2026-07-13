package com.quangthe.nhacnho_uongthuoc;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SchedulingTests {
    private DateTimeManager dateTimeManager;
    private final String DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm";

    @Before
    public void setUp() {
        dateTimeManager = new DateTimeManager();
    }

    @Test
    public void testEveryOtherDayScheduling() {
        String startDate = "2023/10/01"; // Sunday
        String time = "08:00";
        int frequency = 2; // Every other day

        // Now is Sunday 07:00 -> Next should be Sunday 08:00
        DateTime now1 = DateTime.parse("2023/10/01 07:00", DateTimeFormat.forPattern(DATE_TIME_FORMAT));
        long next1 = dateTimeManager.getNextScheduledTimeMillis(startDate, time, frequency, now1);
        assertEquals("2023/10/01 08:00", new DateTime(next1).toString(DATE_TIME_FORMAT));

        // Now is Sunday 09:00 -> Next should be Tuesday 08:00 (10/03)
        DateTime now2 = DateTime.parse("2023/10/01 09:00", DateTimeFormat.forPattern(DATE_TIME_FORMAT));
        long next2 = dateTimeManager.getNextScheduledTimeMillis(startDate, time, frequency, now2);
        assertEquals("2023/10/03 08:00", new DateTime(next2).toString(DATE_TIME_FORMAT));

        // Now is Monday 12:00 -> Next should be Tuesday 08:00 (10/03)
        DateTime now3 = DateTime.parse("2023/10/02 12:00", DateTimeFormat.forPattern(DATE_TIME_FORMAT));
        long next3 = dateTimeManager.getNextScheduledTimeMillis(startDate, time, frequency, now3);
        assertEquals("2023/10/03 08:00", new DateTime(next3).toString(DATE_TIME_FORMAT));
    }

    @Test
    public void testWeeklyScheduling() {
        String startDate = "2023/10/01"; // Sunday
        String time = "08:00";
        int frequency = 7; // Weekly

        // Now is Sunday 10/01 09:00 -> Next should be Sunday 10/08 08:00
        DateTime now1 = DateTime.parse("2023/10/01 09:00", DateTimeFormat.forPattern(DATE_TIME_FORMAT));
        long next1 = dateTimeManager.getNextScheduledTimeMillis(startDate, time, frequency, now1);
        assertEquals("2023/10/08 08:00", new DateTime(next1).toString(DATE_TIME_FORMAT));
        
        // Now is Saturday 10/07 23:00 -> Next should be Sunday 10/08 08:00
        DateTime now2 = DateTime.parse("2023/10/07 23:00", DateTimeFormat.forPattern(DATE_TIME_FORMAT));
        long next2 = dateTimeManager.getNextScheduledTimeMillis(startDate, time, frequency, now2);
        assertEquals("2023/10/08 08:00", new DateTime(next2).toString(DATE_TIME_FORMAT));
    }

    @Test
    public void testDailyScheduling() {
        String startDate = "2023/10/01"; 
        String time = "08:00";
        int frequency = 1; // Daily

        // Now is Sunday 07:00 -> Next should be Sunday 08:00
        DateTime now1 = DateTime.parse("2023/10/01 07:00", DateTimeFormat.forPattern(DATE_TIME_FORMAT));
        long next1 = dateTimeManager.getNextScheduledTimeMillis(startDate, time, frequency, now1);
        assertEquals("2023/10/01 08:00", new DateTime(next1).toString(DATE_TIME_FORMAT));

        // Now is Sunday 09:00 -> Next should be Monday 08:00
        DateTime now2 = DateTime.parse("2023/10/01 09:00", DateTimeFormat.forPattern(DATE_TIME_FORMAT));
        long next2 = dateTimeManager.getNextScheduledTimeMillis(startDate, time, frequency, now2);
        assertEquals("2023/10/02 08:00", new DateTime(next2).toString(DATE_TIME_FORMAT));
    }
}
