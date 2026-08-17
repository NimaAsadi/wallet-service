package ir.ebb.base.util;

import java.time.LocalDate;
import java.time.temporal.WeekFields;

/**
 * Date/time helpers shared across modules.
 */
public final class DateUtil {

    private DateUtil() {}

    /**
     * Current year and week-of-year concatenated as {@code yyyyWW} — e.g. {@code 202601}
     * for week 1 of 2026, {@code 202640} for week 40 of 2026.
     * <p>
     * Uses the ISO-8601 definition via {@link #yearWeek(LocalDate)}; see there for
     * the year-boundary behaviour.
     */
    public static String currentYearWeek() {
        return yearWeek(LocalDate.now());
    }

    /**
     * Year and week-of-year of the given date, concatenated as {@code yyyyWW}
     * (week zero-padded to two digits).
     * <p>
     * Weeks are ISO-8601: Monday-first, week 1 is the week containing the first
     * Thursday of the year, so the year below is the <em>week-based</em> year —
     * a date near a year boundary carries the adjacent year's number
     * (e.g. {@code 2027-01-01} is week 53 of 2026 and yields {@code 202653}).
     */
    public static String yearWeek(LocalDate date) {
        WeekFields weekFields = WeekFields.ISO;
        int year = date.get(weekFields.weekBasedYear());
        int week = date.get(weekFields.weekOfWeekBasedYear());
        return String.format("%d%02d", year, week);
    }
}
