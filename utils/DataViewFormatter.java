package utils;

import java.util.*;

/**
 * DataViewFormatter provides very small, dependency-free helpers for
 * producing consistent, plain ASCII tables for console output. It avoids
 * colors / box drawing so output remains portable across basic terminals.
 */
public final class DataViewFormatter {

    public static String table(List<String> headers, List<List<String>> rows) {
        if (headers == null) headers = Collections.emptyList();
        if (rows == null) rows = Collections.emptyList();
        int cols = headers.size();
        int[] widths = new int[cols];
        for (int c = 0; c < cols; c++) widths[c] = headers.get(c).length();
        for (List<String> r : rows) {
            for (int c = 0; c < cols && c < r.size(); c++) {
                String cell = r.get(c) == null ? "" : r.get(c);
                if (cell.length() > widths[c]) widths[c] = cell.length();
            }
        }
        int totalWidth = 0;
        for (int w : widths) totalWidth += w + 3; // padding & separator
        totalWidth += 1; // initial pipe
        StringBuilder sb = new StringBuilder();
        String line = repeat('-', totalWidth);
        sb.append(line).append('\n');
        // Header row
        sb.append('|');
        for (int c = 0; c < cols; c++) {
            sb.append(' ').append(pad(headers.get(c), widths[c])).append(' ').append('|');
        }
        sb.append('\n').append(line).append('\n');
        // Data rows
        for (List<String> r : rows) {
            sb.append('|');
            for (int c = 0; c < cols; c++) {
                String cell = c < r.size() ? (r.get(c) == null ? "" : r.get(c)) : "";
                sb.append(' ').append(pad(cell, widths[c])).append(' ').append('|');
            }
            sb.append('\n');
        }
        if (!rows.isEmpty()) sb.append(line).append('\n');
        if (rows.isEmpty()) sb.append("(no records)\n");
        return sb.toString();
    }

    public static String pad(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s;
        StringBuilder b = new StringBuilder(width);
        b.append(s);
        while (b.length() < width) b.append(' ');
        return b.toString();
    }

    private static String repeat(char c, int count) {
        char[] arr = new char[count];
        Arrays.fill(arr, c);
        return new String(arr);
    }

    public static String money(double v) {
        return String.format("%.2f", v);
    }

    // Format a billing month in YYYYMM into a readable label like "Sep 2025".
    public static String monthLabel(String yyyymm) {
        if (yyyymm == null || yyyymm.length() != 6) return String.valueOf(yyyymm);
        try {
            java.time.format.DateTimeFormatter in = java.time.format.DateTimeFormatter.ofPattern("yyyyMM");
            java.time.YearMonth ym = java.time.YearMonth.parse(yyyymm, in);
            java.time.format.DateTimeFormatter out = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");
            return ym.format(out);
        } catch (Exception e) {
            return String.valueOf(yyyymm);
        }
    }
}
