public class AverageSQLSubquery extends  AbstractSQLSubQuery{

    AbstractSQLSubQuery subQuery;
    String windowTime;


    public AverageSQLSubquery(AbstractSQLSubQuery subQuery, String windowTime){
        this.subQuery = subQuery;
        this.windowTime = windowTime;
    }

    @Override
    public String toString() {

        return "SELECT timestamp, AVG(`value`) OVER (ORDER BY timestamp RANGE BETWEEN " + parseDuration(windowTime) + " PRECEDING AND CURRENT ROW) AS `value` FROM (" + subQuery.toString() + ") sub";



    }

    public static long parseDuration(String s) {
        s = s.trim().toLowerCase();

        long total = 0;
        int i = 0;
        int n = s.length();

        while (i < n) {
            // parse number
            int start = i;
            while (i < n && (s.charAt(i) >= '0' && s.charAt(i) <= '9')) {
                i++;
            }
            if (start == i) {
                throw new IllegalArgumentException("Expected number at position " + i + " in " + s);
            }
            long value = Long.parseLong(s.substring(start, i));

            // parse unit
            int unitStart = i;
            while (i < n && Character.isLetter(s.charAt(i))) {
                i++;
            }
            if (unitStart == i) {
                throw new IllegalArgumentException("Missing unit at position " + i + " in " + s);
            }
            String unit = s.substring(unitStart, i);

            total += value * unitToMillis(unit);
        }

        return total;
    }

    private static long unitToMillis(String unit) {
        switch (unit) {
            case "ms": return 1L;
            case "s":  return 1000L;
            case "m":  return 60_000L;
            case "h":  return 3_600_000L;
            case "d":  return 86_400_000L;       // 24h
            case "w":  return 604_800_000L;      // 7d
            case "y":  return 31_536_000_000L;   // 365d (Prometheus uses 365d)
        }
        throw new IllegalArgumentException("Unknown duration unit: " + unit);
    }


}
