package com.neith.subjectdemo.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {
    // XML file name
    private static final String PREF_NAME = "QuanLyXayDungPrefs";

    // Keys4Data
    private static final String KEY_USER_ROLE = "user_role";

    public static final  String ROLE_FINANCE = "FN";
    public static final  String ROLE_HR = "HR";
    public static final  String ROLE_EMPLOYEE = "EM";

    private SharedPreferences sharedPreferences;

    // Constructor
    public SharedPrefsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }


    // Role getter and setter
    public void setUserRole(String role) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }
    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, ROLE_EMPLOYEE);
    }
}