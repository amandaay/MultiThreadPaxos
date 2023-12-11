import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility functions to avoid repeated code
 */
public class Utils {
    /**
     * Getting Current Timestamps
     * @return current timestamp
     */
    public static String getCurrentTimestamp() {
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        return dateFormat.format(new Date(currentTimeMillis));
    }
}