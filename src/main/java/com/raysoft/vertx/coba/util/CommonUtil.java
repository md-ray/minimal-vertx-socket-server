package com.raysoft.vertx.coba.util;

/**
 * Created by saviourcat on 6/20/16.
 */
public class CommonUtil {
    public static String[] getParams(String rawString) {
        return rawString.trim().split("##");
    }
}
