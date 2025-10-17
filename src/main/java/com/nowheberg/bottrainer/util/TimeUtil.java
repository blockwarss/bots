package com.nowheberg.bottrainer.util;

public class TimeUtil {
    public static long parseToTicks(String input) {
        input = input.trim().toLowerCase();
        long seconds = 0;
        try {
            if (input.matches("\\d+")) seconds = Long.parseLong(input);
            else {
                long cur = 0; String num = "";
                for (char c : input.toCharArray()) {
                    if (Character.isDigit(c)) num += c; else {
                        long n = num.isEmpty()?0:Long.parseLong(num); num = "";
                        if (c=='h') cur += n*3600; else if (c=='m') cur += n*60; else if (c=='s') cur += n;
                    }
                }
                if (!num.isEmpty()) cur += Long.parseLong(num);
                seconds = cur;
            }
        } catch (Exception ignored) { return 0; }
        return seconds*20L;
    }
}
