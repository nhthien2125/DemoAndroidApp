package com.neith.subjectdemo.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.neith.subjectdemo.admin.AdminActivity;
import com.neith.subjectdemo.auth.SignIn;
import com.neith.subjectdemo.fn.FNActivity;
import com.neith.subjectdemo.hr.HRActivity;

public class SessionManager {

    private static final String CACHE_NAME = "LOGIN_CACHE";

    public static void saveLogin(Context context, String username, String auth) {
        context.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("IS_LOGGED_IN", true)
                .putString("USERNAME", username)
                .putString("AUTH", auth)
                .apply();
    }

    public static boolean checkLoginAndRedirect(Activity activity) {
        boolean isLoggedIn = activity.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
                .getBoolean("IS_LOGGED_IN", false);

        if (!isLoggedIn) return false;

        String username = activity.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
                .getString("USERNAME", "");

        String auth = activity.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
                .getString("AUTH", "");

        return goToArea(activity, username, auth);
    }

    public static boolean goToArea(Activity activity, String username, String auth) {
        if (auth == null || auth.length() < 2) return false;

        String area = auth.substring(0, 2);
        Intent intent;

        switch (area) {
            case "AD":
                intent = new Intent(activity, AdminActivity.class);
                break;
            case "HR":
                intent = new Intent(activity, HRActivity.class);
                break;
            case "FN":
                intent = new Intent(activity, FNActivity.class);
                break;
            default:
                return false;
        }

        intent.putExtra("USERNAME", username);
        intent.putExtra("AUTH", auth);
        activity.startActivity(intent);
        activity.finish();
        return true;
    }

    public static void logout(Activity activity) {
        activity.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Intent intent = new Intent(activity, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
