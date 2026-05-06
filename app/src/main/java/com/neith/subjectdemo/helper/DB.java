package com.neith.subjectdemo.helper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class DB {

    public static final String DB_NAME = "QLNSVATC_F.db";

    public static SQLiteDatabase openDatabase(Context context) {
        copyDatabase(context);

        return SQLiteDatabase.openDatabase(
                context.getDatabasePath(DB_NAME).getPath(),
                null,
                SQLiteDatabase.OPEN_READWRITE
        );
    }

    private static void copyDatabase(Context context) {
        try {
            File dbFile = context.getDatabasePath(DB_NAME);
            if (dbFile.exists()) {
                Log.d("DB", "DB đã tồn tại, không copy lại");
                return;
            }

            dbFile.getParentFile().mkdirs();

            InputStream inputStream = context.getAssets().open(DB_NAME);
            FileOutputStream outputStream = new FileOutputStream(dbFile);

            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Log.d("DB", "Copy DB từ assets thành công");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Lỗi copy DB: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void logAllTables(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'",
                null
        );

        while (cursor.moveToNext()) {
            String tableName = cursor.getString(0);
            Log.d("DB_TABLE", "Table: " + tableName);
        }

        cursor.close();
    }
}