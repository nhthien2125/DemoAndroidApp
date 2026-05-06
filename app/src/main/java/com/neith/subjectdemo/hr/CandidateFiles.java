package com.neith.subjectdemo.hr;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

public class CandidateFiles {

    private static final String ASSET_FOLDER = "CandidateFiles";
    private static final String DOWNLOAD_ROOT = "Download/TBT_Candidate_Files";

    public static boolean downloadOneFile(
            Context context,
            String fileName,
            String candidateName
    ) throws Exception {
        if (isEmpty(fileName)) {
            return false;
        }

        String cleanCandidateName = makeSafeFolderName(candidateName);
        String cleanFileName = makeSafeFileName(fileName);
        String relativePath = DOWNLOAD_ROOT + "/" + cleanCandidateName;

        InputStream inputStream = openCandidateFileInputStream(context, cleanFileName);

        if (inputStream == null) {
            throw new Exception("Không tìm thấy file: " + cleanFileName);
        }

        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();

        values.put(MediaStore.MediaColumns.DISPLAY_NAME, cleanFileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(cleanFileName));

        Uri uri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        } else {
            uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);
        }

        if (uri == null) {
            inputStream.close();
            throw new Exception("Không tạo được file trong Downloads.");
        }

        OutputStream outputStream = resolver.openOutputStream(uri);

        if (outputStream == null) {
            inputStream.close();
            throw new Exception("Không mở được nơi lưu file.");
        }

        byte[] buffer = new byte[8192];
        int length;

        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        outputStream.flush();
        outputStream.close();
        inputStream.close();
        return true;
    }

    private static InputStream openCandidateFileInputStream(Context context, String fileName) throws Exception {
        File root = context.getExternalFilesDir("CandidateFiles");

        if (root != null && root.exists()) {
            File found = findFileRecursive(root, fileName);

            if (found != null && found.exists()) {
                return new FileInputStream(found);
            }
        }

        try {
            return context.getAssets().open(ASSET_FOLDER + "/" + fileName);
        } catch (Exception ignored) {
        }

        return null;
    }

    private static File findFileRecursive(File dir, String fileName) {
        if (dir == null || !dir.exists()) {
            return null;
        }

        File[] files = dir.listFiles();

        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                File found = findFileRecursive(file, fileName);

                if (found != null) {
                    return found;
                }
            } else if (file.getName().equalsIgnoreCase(fileName)) {
                return file;
            }
        }

        return null;
    }

    public static int downloadAllFilesOfCandidate(
            Context context,
            CandidateActivity.CandidateItem item
    ) throws Exception {
        int count = 0;

        if (item == null) {
            return 0;
        }

        if (!isEmpty(item.fileThongTin)) {
            downloadOneFile(context, item.fileThongTin, item.tenUngVien);
            count++;
        }

        if (!isEmpty(item.fileBangCap)) {
            downloadOneFile(context, item.fileBangCap, item.tenUngVien);
            count++;
        }

        if (!isEmpty(item.fileKhac)) {
            downloadOneFile(context, item.fileKhac, item.tenUngVien);
            count++;
        }

        return count;
    }

    public static int downloadAllFilesOfCandidates(
            Context context,
            ArrayList<CandidateActivity.CandidateItem> list
    ) throws Exception {
        int count = 0;

        if (list == null || list.isEmpty()) {
            return 0;
        }

        for (CandidateActivity.CandidateItem item : list) {
            count += downloadAllFilesOfCandidate(context, item);
        }

        return count;
    }

    public static boolean assetFileExists(Context context, String fileName) {
        if (isEmpty(fileName)) {
            return false;
        }

        try {
            InputStream inputStream = openCandidateFileInputStream(context, makeSafeFileName(fileName));

            if (inputStream != null) {
                inputStream.close();
                return true;
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    private static String getMimeType(String fileName) {
        String lower = fileName.toLowerCase(Locale.getDefault());

        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }

        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        if (lower.endsWith(".png")) {
            return "image/png";
        }

        if (lower.endsWith(".doc")) {
            return "application/msword";
        }

        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        if (lower.endsWith(".zip")) {
            return "application/zip";
        }

        return "application/octet-stream";
    }

    private static String makeSafeFileName(String fileName) {
        if (fileName == null) {
            return "";
        }

        return fileName
                .trim()
                .replace("\\", "")
                .replace("/", "")
                .replace("..", "");
    }

    private static String makeSafeFolderName(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "Unknown_Candidate";
        }

        String normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

        String result = pattern.matcher(normalized).replaceAll("")
                .replace("Đ", "D")
                .replace("đ", "d")
                .replaceAll("[^a-zA-Z0-9_\\- ]", "")
                .replaceAll("\\s+", "_");

        if (result.trim().isEmpty()) {
            return "Unknown_Candidate";
        }

        return result;
    }

    private static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty() || text.equalsIgnoreCase("null");
    }
}