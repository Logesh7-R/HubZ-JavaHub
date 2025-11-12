package hubz.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static String getCurrentTimestamp() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
