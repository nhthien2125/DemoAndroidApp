package com.neith.subjectdemo.helper;

import java.security.SecureRandom;

public class OTP {

    public static String generateOTP8() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            otp.append(chars.charAt(random.nextInt(chars.length())));
        }

        return otp.toString();
    }
}