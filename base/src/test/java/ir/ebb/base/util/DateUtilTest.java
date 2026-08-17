package ir.ebb.base.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link DateUtil#yearWeek(LocalDate)} — the deterministic overload
 * {@link DateUtil#currentYearWeek()} delegates to — with a focus on the ISO-8601
 * week-based-year behaviour at year boundaries.
 */
class DateUtilTest {

    @Test
    void weekOneOfTheYearItselfIsPaddedToTwoDigits() {
        assertThat(DateUtil.yearWeek(LocalDate.of(2026, 1, 1))).isEqualTo("202601");
    }

    @Test
    void midYearMondayYieldsItsWeek() {
        assertThat(DateUtil.yearWeek(LocalDate.of(2026, 8, 17))).isEqualTo("202634");
    }

    @Test
    void weekFortyExample() {
        assertThat(DateUtil.yearWeek(LocalDate.of(2026, 9, 28))).isEqualTo("202640");
    }

    @Test
    void lastWeekOf2026() {
        assertThat(DateUtil.yearWeek(LocalDate.of(2026, 12, 28))).isEqualTo("202653");
    }

    @Test
    void januaryDateBelongingToThePreviousYearsLastWeek() {
        assertThat(DateUtil.yearWeek(LocalDate.of(2027, 1, 1))).isEqualTo("202653");
    }

    @Test
    void decemberDateBelongingToTheNextYearsFirstWeek() {
        assertThat(DateUtil.yearWeek(LocalDate.of(2025, 12, 29))).isEqualTo("202601");
    }
}
