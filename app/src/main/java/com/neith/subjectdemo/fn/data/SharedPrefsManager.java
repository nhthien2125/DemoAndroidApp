package com.neith.subjectdemo.fn.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {
    private static final String PREF_NAME = "FinancePrefs";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_HAN_MUC = "HAN_MUC";

    public static final String ROLE_FINANCE = "FN";
    public static final String ROLE_EMPLOYEE = "EM";

    private final SharedPreferences sharedPreferences;

    public SharedPrefsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setUserRole(String role) {
        sharedPreferences.edit().putString(KEY_USER_ROLE, role).apply();
    }

    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, ROLE_EMPLOYEE);
    }

    public void saveHanMuc(float hanMuc) {
        sharedPreferences.edit().putFloat(KEY_HAN_MUC, hanMuc).apply();
    }

    public float getHanMuc() {
        return sharedPreferences.getFloat(KEY_HAN_MUC, 0);
    }
}