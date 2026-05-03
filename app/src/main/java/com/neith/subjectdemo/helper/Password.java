package com.neith.subjectdemo.helper;

import java.security.MessageDigest;

public class Password {

    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes("UTF-8"));

            StringBuilder result = new StringBuilder();

            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) result.append("0");
                result.append(hex);
            }

            return result.toString();

        } catch (Exception e) {
            return "";
        }
    }
}