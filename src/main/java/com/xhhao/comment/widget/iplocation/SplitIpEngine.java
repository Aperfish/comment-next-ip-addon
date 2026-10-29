package com.xhhao.comment.widget.iplocation;

import com.google.common.net.InetAddresses;
import org.json.JSONObject;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;
import java.net.*;
import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Local classification is mandatory; an unknown country never triggers a foreign API. */
public final class SplitIpEngine {
    public static final String DOMESTIC = "https://api.map.baidu.com/location/ip?ip={ip}&coor=bd09ll&ak=YOUR_BAIDU_AK";
    public static final String FOREIGN = "http://ip-api.com/json/{ip}?lang=zh-CN";
    public record Options(boolean enabled, String domestic, String foreign, int cacheMinutes, int timeoutMillis, boolean fallback, boolean domesticDistrictEnabled) {
        public Options(boolean enabled, String domestic, String foreign, int cacheMinutes, int timeoutMillis, boolean fallback) {
            this(enabled, domestic, foreign, cacheMinutes, timeoutMillis, fallback, false);
        }
        public Options {
            domestic = domestic == null || domestic.isBlank() ? DOMESTIC : domestic.strip();
            foreign = foreign == null || foreign.isBlank() ? FOREIGN : foreign.strip();
            cacheMinutes = Math.max(1, Math.min(43200, cacheMinutes));
            timeoutMillis = Math.max(500, Math.min(10000, timeoutMillis));
        }
    }
    @FunctionalInterface public interface Lookup { String lookup(String ip) throws Exception; }
    @FunctionalInterface public interface Fetch { String fetch(String ip, String template, Duration timeout) throws Exception; }
    private record Entry(String text, long until) {}
    private record Key(String ip, Options options) {}
    private final Lookup lookup;
    private final ConcurrentHashMap<Key, Entry> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Key, CompletableFuture<String>> flights = new ConcurrentHashMap<>();
    private final Semaphore permits = new Semaphore(8);
    private long rateWindow; private int rateCount;
    public SplitIpEngine(Lookup lookup) { this.lookup = lookup; }

    public String resolve(String raw, Options options, Fetch fetch) {
        String ip = canonicalPublicIp(raw);
        if (!options.enabled() || ip.isEmpty()) return "";
        Key key = new Key(ip, options);
        long now = System.currentTimeMillis();
        Entry hit = cache.get(key);
        if (hit != null && hit.until > now) return hit.text;
        CompletableFuture<String> mine = new CompletableFuture<>();
        CompletableFuture<String> previous = flights.putIfAbsent(key, mine);
        if (previous != null) {
            try { return previous.get(options.timeoutMillis() + 2000L, TimeUnit.MILLISECONDS); }
            catch (Exception ignored) { return ""; }
        }
        String result = ""; boolean online = false;
        try {
            String region;
            try { region = lookup.lookup(ip); } catch (Exception e) { region = ""; }
            String[] parts = region == null ? new String[0] : region.split("\\|", -1);
            String country = parts.length > 0 ? clean(parts[0]) : "";
            String offline = offline(parts);
            // Unknown/offline DB failure is a third state, not 'foreign'.
            if (!country.isEmpty()) {
                boolean domestic = currentDatabaseFormat(parts) ? "CN".equalsIgnoreCase(parts[4]) : china(country);
                String template = domestic ? options.domestic() : options.foreign();
                if (permits.tryAcquire()) {
                    try {
                        URI uri = uri(ip, template);
                        if (!template.contains("YOUR_BAIDU_AK") && allowRate(uri)) {
                            result = normalize(fetch.fetch(ip, template, Duration.ofMillis(options.timeoutMillis())));
                            if (result.equals("台湾") && offline.startsWith("台湾·")) result = offline;
                            online = !result.isEmpty();
                        }
                    } catch (Exception ignored) { result = ""; }
                    finally { permits.release(); }
                }
            }
            if (result.isEmpty() && options.fallback()) result = offline;
            long ttl = online ? options.cacheMinutes() * 60000L : result.isEmpty() ? 30000L : 60000L;
            if (cache.size() >= 10000) {
                cache.entrySet().removeIf(e -> e.getValue().until <= System.currentTimeMillis());
                if (cache.size() >= 10000) cache.clear();
            }
            cache.put(key, new Entry(result, System.currentTimeMillis() + ttl));
            mine.complete(result);
            return result;
        } finally {
            mine.complete(result);
            flights.remove(key, mine);
        }
    }
    private synchronized boolean allowRate(URI uri) {
        if (!"ip-api.com".equalsIgnoreCase(uri.getHost())) return true;
        long now = System.currentTimeMillis();
        if (now - rateWindow >= 60000) { rateWindow = now; rateCount = 0; }
        if (rateCount >= 40) return false;
        rateCount++; return true;
    }
    public static URI uri(String ip, String template) {
        if (template == null || !template.contains("{ip}")) throw new IllegalArgumentException("Missing {ip}");
        URI uri = URI.create(template.strip().replace("{ip}", ip));
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
            || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("Invalid API URL");
        return uri;
    }
    public static String canonicalPublicIp(String raw) {
        try {
            if (raw == null || raw.contains("%")) return "";
            InetAddress a = InetAddresses.forString(raw.strip());
            byte[] b = a.getAddress(); int x = b[0] & 255, y = b[1] & 255;
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) return "";
            if (b.length == 4) {
                if (x == 0 || x == 10 || x == 127 || x >= 224 || x == 100 && y >= 64 && y <= 127
                    || x == 169 && y == 254 || x == 172 && y >= 16 && y <= 31 || x == 192 && y == 168
                    || x == 192 && y == 0 && ((b[2] & 255) == 0 || (b[2] & 255) == 2)
                    || x == 198 && (y == 18 || y == 19 || y == 51 && (b[2] & 255) == 100)
                    || x == 203 && y == 0 && (b[2] & 255) == 113) return "";
            } else {
                if ((x & 0xe0) != 0x20 || x == 0x20 && y == 1 && (b[2]&255) == 0x0d && (b[3]&255) == 0xb8
                    || x == 0x20 && y == 1 && (b[2]&255) == 0 && ((b[3]&255) == 0 || (b[3]&255) == 2)
                    || x == 0x20 && y == 2) return "";
            }
            return InetAddresses.toAddrString(a);
        } catch (IllegalArgumentException e) { return ""; }
    }
    static String clean(String s) {
        if (s == null) return "";
        s = s.strip();
        return s.equals("0") || s.equalsIgnoreCase("null") || s.equalsIgnoreCase("none")
            || s.equalsIgnoreCase("unknown") || s.equals("-") || s.equals("未知") || s.equals("未知位置") || s.equals("-1") ? "" : s;
    }
    static String normalize(String s) {
        s = clean(s).replaceAll("[\\p{Cntrl}<>]", "");
        return s.length() > 96 ? s.substring(0, 96) : s;
    }
    static boolean china(String s) { return Set.of("中国", "中國", "CN", "China", "中国大陆").contains(s); }
    static String country(String s, String code) {
        if ("HK".equalsIgnoreCase(code) || Set.of("香港", "中国香港", "中国香港特别行政区", "香港特别行政区", "Hong Kong", "Hong Kong SAR China").contains(s)) return "香港";
        if ("TW".equalsIgnoreCase(code) || Set.of("台湾", "台灣", "中国台湾", "台湾省", "Taiwan").contains(s)) return "台湾";
        if (!clean(code).isEmpty() && code.matches("[A-Za-z]{2}")) {
            if (!Arrays.asList(Locale.getISOCountries()).contains(code.toUpperCase(Locale.ROOT))) return clean(s);
            String name = Locale.forLanguageTag("und-" + code.toUpperCase(Locale.ROOT)).getDisplayCountry(Locale.SIMPLIFIED_CHINESE);
            if (!name.isBlank() && !name.equalsIgnoreCase(code)) return name;
        }
        if (s.matches("[A-Za-z]{2}") && code.isEmpty()) return country("", s);
        return clean(s);
    }
    static String admin(String s) {
        s = clean(s);
        return s.replaceFirst("(?:壮族自治区|回族自治区|维吾尔自治区|特别行政区|自治区|省|市)$", "");
    }
    static String pair(String first, String second) {
        first = clean(first); second = clean(second);
        if (first.isEmpty()) return normalize(second);
        if (second.isEmpty() || first.equals(second)) return normalize(first);
        return normalize(first + "·" + second);
    }
    static String domestic(String province, String city, String district) {
        String p = admin(province), c = admin(city);
        Set<String> municipalities = Set.of("北京", "上海", "天津", "重庆");
        String municipality = municipalities.contains(p) ? p : municipalities.contains(c) ? c : "";
        if (!municipality.isEmpty()) {
            String d = clean(district);
            if (d.isEmpty() && !c.isEmpty() && !c.equals(municipality) && !Set.of("市辖区", "县").contains(c)) d = clean(city);
            return pair(municipality, d);
        }
        if (p.isEmpty()) return c;
        if (c.isEmpty()) return p;
        return normalize(p + "·" + c);
    }
    static String foreign(String country, String province, String city) {
        country = country(country, "");
        province = SplitIpNames.local(country, province, true);
        city = SplitIpNames.local(country, city, false);
        if (country.equals("台湾")) {
            String place = clean(city);
            if (place.isEmpty()) place = clean(province);
            if (Set.of("台湾", "台灣", "台湾省", "Taiwan").contains(place)) place = "";
            return pair(country, admin(place));
        }
        return normalize(String.join("·", java.util.stream.Stream.of(country, province, city)
            .map(SplitIpEngine::clean).filter(s -> !s.isEmpty()).toList()));
    }
    public static String offline(String[] p) {
        if (p.length == 0 || clean(p[0]).isEmpty()) return "";
        // Bundled XDB uses country|province|city|ISP|ISO2, unlike older ip2region databases.
        boolean current = currentDatabaseFormat(p);
        String province = p.length > (current ? 1 : 2) ? p[current ? 1 : 2] : "";
        String city = p.length > (current ? 2 : 3) ? p[current ? 2 : 3] : "";
        boolean domestic = current ? "CN".equalsIgnoreCase(p[4]) : china(p[0]);
        return domestic ? domestic(province, city, "") : foreign(country(p[0], current ? p[4] : ""), province, city);
    }
    static boolean currentDatabaseFormat(String[] p) { return p.length == 5 && p[4].matches("[A-Za-z]{2}"); }
    private static String text(JSONObject o, String... keys) {
        for (String k : keys) {
            Object v = o.opt(k);
            if (v instanceof String || v instanceof Number) {
                String s = clean(String.valueOf(v)); if (!s.isEmpty()) return s;
            }
        }
        return "";
    }
    public static String parse(String json) {
        return parse(json, false);
    }
    public static String parse(String json, boolean domesticDistrictEnabled) {
        try { return parseObject(new JSONObject(json), 0, domesticDistrictEnabled); } catch (Exception e) { return ""; }
    }
    static String domestic(String province, String city, String district, boolean showDistrict) {
        String base = domestic(province, city, district);
        Set<String> municipalities = Set.of("北京", "上海", "天津", "重庆");
        if (municipalities.contains(admin(province))) return showDistrict ? base : admin(province);
        if (municipalities.contains(admin(city))) return showDistrict ? base : admin(city);
        if (!showDistrict) return base;
        String d = clean(district);
        if (d.isEmpty() || d.matches("[0-9]+") || Set.of("市辖区", "未知区县", "县").contains(d)
            || d.equals(clean(city)) || d.equals(admin(city))) return base;
        return base.isEmpty() ? normalize(d) : normalize(base + "·" + d);
    }
    private static String parseObject(JSONObject o, int depth, boolean showDistrict) {
        if (depth > 5) return "";
        String status = text(o, "status");
        if (!status.isEmpty() && !Set.of("0", "success", "ok", "200").contains(status.toLowerCase(Locale.ROOT))) return "";
        if (o.has("success") && !o.optBoolean("success", true)) return "";
        String code = text(o, "code");
        if (!code.isEmpty() && !Set.of("0", "200", "Success").contains(code)) return "";
        JSONObject content = o.optJSONObject("content");
        if (content != null) {
            JSONObject detail = content.optJSONObject("address_detail");
            if (detail != null) return domestic(text(detail, "province"), text(detail, "city"), text(detail, "district", "county", "area"), showDistrict);
        }
        String c = text(o, "country", "nation", "country_name");
        String cc = text(o, "countryCode", "country_code");
        String province = text(o, "regionName", "province", "pro", "prov", "state", "region");
        String city = text(o, "city", "city_name");
        if (china(c) || "CN".equalsIgnoreCase(cc) || c.isEmpty() && cc.isEmpty() && !province.isEmpty())
            return domestic(province, city, text(o, "district", "county", "area", "district_name"), showDistrict);
        if (!c.isEmpty() || !cc.isEmpty()) return foreign(country(c, cc), province, city);
        for (String key : List.of("data", "result", "content", "location")) {
            JSONObject child = o.optJSONObject(key);
            if (child != null) { String r = parseObject(child, depth + 1, showDistrict); if (!r.isEmpty()) return r; }
        }
        return "";
    }
    public static final class Database implements Lookup {
        private Searcher v4, v6;
        public synchronized String lookup(String ip) throws Exception {
            boolean ipv6 = ip.contains(":");
            Searcher s = ipv6 ? v6 : v4;
            if (s == null) {
                String resource = "/ip2region/ip2region_" + (ipv6 ? "v6" : "v4") + ".xdb";
                try (InputStream in = SplitIpEngine.class.getResourceAsStream(resource)) {
                    if (in == null) throw new IOException("Missing local IP database");
                    s = Searcher.newWithBuffer(ipv6 ? Version.IPv6 : Version.IPv4, Searcher.loadContentFromInputStream(in));
                }
                if (ipv6) v6 = s; else v4 = s;
            }
            return s.search(ip);
        }
    }
}
