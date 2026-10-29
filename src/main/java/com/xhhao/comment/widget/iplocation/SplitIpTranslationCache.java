package com.xhhao.comment.widget.iplocation;

import java.util.concurrent.*;
import java.util.function.Function;

/** Translation cache receives place names only, never an IP address. */
final class SplitIpTranslationCache {
    private record Key(String configuration, String location) {}
    private record Value(String text, long expires) {}
    private final ConcurrentHashMap<Key, Value> entries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Key, CompletableFuture<String>> pending = new ConcurrentHashMap<>();
    private final Semaphore slots = new Semaphore(4);

    String translate(String location, String configuration, Function<String,String> provider) {
        if (location == null || !location.matches(".*[A-Za-z].*")) return location;
        Key key = new Key(configuration, location);
        Value cached = entries.get(key);
        if (cached != null && cached.expires > System.currentTimeMillis()) return cached.text;
        CompletableFuture<String> mine = new CompletableFuture<>();
        CompletableFuture<String> previous = pending.putIfAbsent(key, mine);
        if (previous != null) {
            try { return previous.get(6, TimeUnit.SECONDS); }
            catch (Exception ignored) { return location; }
        }
        String result = location;
        boolean acquired = slots.tryAcquire();
        try {
            if (!acquired) return location;
            try {
                String answer = provider.apply(location);
                if (valid(location, answer)) result = answer.strip();
            } catch (Exception ignored) { }
            if (entries.size() >= 10000) entries.clear();
            entries.put(key, new Value(result, System.currentTimeMillis() + (result.equals(location) ? 60000L : 604800000L)));
            return result;
        } finally {
            if (acquired) slots.release();
            mine.complete(result);
            pending.remove(key, mine);
        }
    }
    static boolean valid(String original, String answer) {
        if (answer == null || answer.isBlank() || answer.length() > 128 || answer.matches("(?s).*[A-Za-z<>\\r\\n].*")) return false;
        String[] before = original.split("·", -1), after = answer.strip().split("·", -1);
        if (before.length != after.length) return false;
        for (int i = 0; i < before.length; i++) {
            if (after[i].isBlank() || !after[i].matches(".*[\\p{IsHan}].*")) return false;
            if (!before[i].matches(".*[A-Za-z].*") && !before[i].equals(after[i])) return false;
        }
        return true;
    }
}
