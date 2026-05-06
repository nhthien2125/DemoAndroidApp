package com.neith.subjectdemo.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.neith.subjectdemo.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public class FinanceDbHelper extends SQLiteOpenHelper{
    private static final String DATABASE_NAME = "finance.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "transactions";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TYPE = "type"; // 0=expense, 1=income
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_AMOUNT = "amount";

    public FinanceDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TYPE + " INTEGER, " +
                COLUMN_AMOUNT + " REAL, " +
                COLUMN_DESCRIPTION + " TEXT)";
        db.execSQL(createTable);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Methods
    public long addTransaction(int type, double amount, String description) {
        SQLiteDatabase db = this.getWritableDatabase(); // writeable db
        ContentValues values = new ContentValues();
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_AMOUNT, amount);
        values.put(COLUMN_DESCRIPTION, description);

        // insert() reject error ' n SQL Injection
        long result = db.insert(TABLE_NAME, null, values);
        db.close();
        return result;
    }
    public double getTotalExpense() {
        SQLiteDatabase db = this.getReadableDatabase(); // readable db
        double total = 0;
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_NAME + " WHERE " + COLUMN_TYPE + " = 0", null);

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }
    public List<Transaction> getAllTransaction() {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                // Đọc dữ liệu từ từng cột
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));

                list.add(new Transaction(id, type, amount, description));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}
