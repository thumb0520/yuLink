package com.yulink.nas.util;

public class PathUtils {

    public static String normalize(String path) {
        if (path == null || path.isEmpty()) return "/";

        // Replace backslashes with forward slashes
        path = path.replace("\\", "/");

        // Remove trailing slash (except for root)
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // Ensure leading slash
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return path;
    }

    public static String join(String... paths) {
        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            if (path == null || path.isEmpty()) continue;

            if (sb.length() > 0) {
                if (sb.charAt(sb.length() - 1) != '/') {
                    sb.append('/');
                }
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
            }
            sb.append(path);
        }

        String result = sb.toString();
        return result.isEmpty() ? "/" : result;
    }

    public static String getParent(String path) {
        if (path == null || path.equals("/")) return "/";

        path = normalize(path);
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) return "/";
        return path.substring(0, lastSlash);
    }

    public static String getFileName(String path) {
        if (path == null || path.isEmpty()) return "";
        path = normalize(path);
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) return path;
        return path.substring(lastSlash + 1);
    }

    public static String[] getPathSegments(String path) {
        if (path == null || path.isEmpty()) return new String[0];
        path = normalize(path);
        if (path.equals("/")) return new String[0];
        return path.substring(1).split("/");
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
