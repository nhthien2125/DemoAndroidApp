package com.neith.subjectdemo.helper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExcelExporter {

    public static void exportCursorToExcel(Context context,
                                           Cursor cursor,
                                           String fileNamePrefix,
                                           String[] columnNames) {

        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(context, "Không có dữ liệu để xuất Excel", Toast.LENGTH_SHORT).show();
            return;
        }

        Workbook workbook = null;

        try {
            // Dùng XSSFWorkbook để xuất đúng file .xlsx
            workbook = new XSSFWorkbook();

            Sheet sheet = workbook.createSheet("Report");

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle normalStyle = createNormalStyle(workbook);
            CellStyle evenRowStyle = createEvenRowStyle(workbook);

            int colLength = columnNames.length;

            // ===== TITLE ROW =====
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(28);

            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(formatReportTitle(fileNamePrefix));
            titleCell.setCellStyle(titleStyle);

            if (colLength > 1) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colLength - 1));

                for (int i = 1; i < colLength; i++) {
                    Cell cell = titleRow.createCell(i);
                    cell.setCellStyle(titleStyle);
                }
            }

            // ===== DATE ROW =====
            Row dateRow = sheet.createRow(1);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue(
                    "Ngày xuất: " +
                            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                    .format(new Date())
            );
            dateCell.setCellStyle(normalStyle);

            // ===== EMPTY ROW =====
            sheet.createRow(2);

            // ===== HEADER ROW =====
            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(22);

            for (int i = 0; i < columnNames.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnNames[i]);
                cell.setCellStyle(headerStyle);
            }

            // ===== DATA ROWS =====
            int rowNum = 4;
            int colCount = cursor.getColumnCount();

            cursor.moveToPosition(-1);

            while (cursor.moveToNext()) {
                Row row = sheet.createRow(rowNum);
                CellStyle rowStyle = rowNum % 2 == 0 ? evenRowStyle : normalStyle;

                for (int i = 0; i < Math.min(colCount, columnNames.length); i++) {
                    Cell cell = row.createCell(i);

                    try {
                        String value = cursor.getString(i);
                        cell.setCellValue(value != null ? value : "");
                    } catch (Exception e) {
                        cell.setCellValue("");
                    }

                    cell.setCellStyle(rowStyle);
                }

                rowNum++;
            }

            // Không dùng sheet.autoSizeColumn(i) trên Android
            // Vì nó gọi java.awt.font.FontRenderContext và Android không có java.awt
            setSafeColumnWidths(sheet, columnNames);

            sheet.createFreezePane(0, 4);

            saveWorkbookAsXlsx(context, workbook, fileNamePrefix);

        } catch (Exception e) {
            e.printStackTrace();

            try {
                if (workbook != null) {
                    workbook.close();
                }
            } catch (IOException ignored) {

            }

            Toast.makeText(
                    context,
                    "Lỗi xuất XLSX: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private static void setSafeColumnWidths(Sheet sheet, String[] columnNames) {
        for (int i = 0; i < columnNames.length; i++) {
            String name = columnNames[i] == null ? "" : columnNames[i].toLowerCase(Locale.ROOT);

            int width;

            if (name.contains("stt")) {
                width = 2500;
            } else if (name.contains("tên") || name.contains("ten")) {
                width = 9000;
            } else if (name.contains("mã") || name.contains("ma")) {
                width = 5000;
            } else if (name.contains("doanh thu")) {
                width = 6500;
            } else if (name.contains("chi phí") || name.contains("chi phi")) {
                width = 6500;
            } else if (name.contains("ngày") || name.contains("ngay") || name.contains("date")) {
                width = 5200;
            } else if (name.contains("nhóm") || name.contains("nhom") || name.contains("group")) {
                width = 4800;
            } else if (name.contains("tìm") || name.contains("tim") || name.contains("search")) {
                width = 6500;
            } else {
                width = 5200;
            }

            sheet.setColumnWidth(i, width);
        }
    }

    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.WHITE.getIndex());

        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GOLD.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        setBorder(style);

        return style;
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());

        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        setBorder(style);

        return style;
    }

    private static CellStyle createNormalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.BLACK.getIndex());

        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        setBorder(style);

        return style;
    }

    private static CellStyle createEvenRowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.BLACK.getIndex());

        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        setBorder(style);

        return style;
    }

    private static void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
    }

    private static String formatReportTitle(String fileNamePrefix) {
        if (fileNamePrefix == null || fileNamePrefix.trim().isEmpty()) {
            return "REPORT";
        }

        return fileNamePrefix
                .replace("_", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static void saveWorkbookAsXlsx(Context context, Workbook workbook, String fileNamePrefix) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

        // Chỗ này xuất đúng đuôi .xlsx
        String fileName = fileNamePrefix + "_" + timeStamp + ".xlsx";

        ContentResolver resolver = context.getContentResolver();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(
                MediaStore.MediaColumns.MIME_TYPE,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS
            );
        }

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

        if (uri == null) {
            try {
                workbook.close();
            } catch (IOException ignored) {

            }

            Toast.makeText(context, "Không thể tạo file XLSX", Toast.LENGTH_SHORT).show();
            return;
        }

        try (OutputStream outputStream = resolver.openOutputStream(uri)) {
            workbook.write(outputStream);
            workbook.close();

            Toast.makeText(
                    context,
                    "Đã xuất XLSX vào Download: " + fileName,
                    Toast.LENGTH_LONG
            ).show();

        } catch (IOException e) {
            try {
                workbook.close();
            } catch (IOException ignored) {

            }

            Toast.makeText(
                    context,
                    "Lỗi khi lưu XLSX: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}