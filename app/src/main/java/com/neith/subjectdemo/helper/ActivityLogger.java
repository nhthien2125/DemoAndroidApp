package com.neith.subjectdemo.helper;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ActivityLogger {

    public static void log(SQLiteDatabase db, String actionCode, String performedBy, String description) {
        if (db == null || !db.isOpen()) return;

        ContentValues cv = new ContentValues();
        cv.put("ActionCode", actionCode);
        cv.put("ActionTime", currentTimestamp());
        cv.put("PerformedBy", performedBy);
        cv.put("Description", description);

        db.insert("ACTIVITY_LOG", null, cv);
    }

    private static String currentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}