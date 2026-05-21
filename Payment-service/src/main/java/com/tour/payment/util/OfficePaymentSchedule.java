package com.tour.payment.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Hạn thanh toán tại quầy = thời điểm xác nhận đặt chỗ + N giờ (mặc định 48h).
 */
public final class OfficePaymentSchedule {

    private OfficePaymentSchedule() {
    }

    public static LocalDateTime resolvePaymentDueAt(LocalDateTime bookedAt, int hoursAfterBooking) {
        LocalDateTime anchor = bookedAt != null ? bookedAt : LocalDateTime.now();
        LocalDateTime dueAt = anchor.plusHours(hoursAfterBooking);
        if (!dueAt.isAfter(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Không thể thanh toán tại quầy: đã quá "
                            + hoursAfterBooking + " giờ kể từ lúc đặt (hạn lúc "
                            + formatDisplay(dueAt) + ").");
        }
        return dueAt;
    }

    public static LocalDateTime parseBookedAt(String bookedAt) {
        if (bookedAt == null || bookedAt.isBlank() || "null".equalsIgnoreCase(bookedAt.trim())) {
            return null;
        }
        String normalized = bookedAt.trim().replace(" ", "T");
        if (normalized.endsWith("Z")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            } catch (DateTimeParseException ex2) {
                throw new RuntimeException("Thời điểm đặt không hợp lệ: " + bookedAt);
            }
        }
    }

    public static LocalDateTime parseDeparture(String startDate) {
        if (startDate == null || startDate.isBlank() || "null".equalsIgnoreCase(startDate.trim())) {
            return null;
        }
        String normalized = startDate.trim().replace(" ", "T");
        if (normalized.endsWith("Z")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        try {
            if (normalized.length() == 10) {
                return LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE).atTime(8, 0);
            }
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            } catch (DateTimeParseException ex2) {
                return null;
            }
        }
    }

    public static String formatDisplay(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
