package com.neith.subjectdemo.helper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExcelExporter {

    public static void exportCursorToExcel(Context context, Cursor cursor, String fileNamePrefix, String[] columnNames) {
        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(context, "Không có dữ liệu để xuất", Toast.LENGTH_SHORT).show();
            return;
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Report");

        // Header Row
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columnNames.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnNames[i]);
        }

        // Data Rows
        int rowNum = 1;
        int colCount = cursor.getColumnCount();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < Math.min(colCount, columnNames.length); i++) {
                Cell cell = row.createCell(i);
                try {
                    String val = cursor.getString(i);
                    cell.setCellValue(val != null ? val : "");
                } catch (Exception e) {
                    cell.setCellValue("");
                }
            }
        }

        saveWorkbook(context, workbook, fileNamePrefix);
    }

    private static void saveWorkbook(Context context, Workbook workbook, String fileNamePrefix) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = fileNamePrefix + "_" + timeStamp + ".xlsx";

        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download");
        }

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

        if (uri != null) {
            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                workbook.write(outputStream);
                workbook.close();
                Toast.makeText(context, "Đã lưu báo cáo vào thư mục Download: " + fileName, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(context, "Lỗi khi lưu file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Không thể tạo file", Toast.LENGTH_SHORT).show();
        }
    }
}
