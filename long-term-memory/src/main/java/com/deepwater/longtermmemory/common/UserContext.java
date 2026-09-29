package com.deepwater.longtermmemory.common;

public final class UserContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    public static void set(String userId) { CURRENT.set(userId); }
    public static String get() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }
}
