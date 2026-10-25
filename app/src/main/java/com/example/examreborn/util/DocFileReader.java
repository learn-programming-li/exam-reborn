package com.example.examreborn.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** 读用户选中的 txt / md / json 文件。中文老文档常是 GBK，所以要做编码兜底。 */
public final class DocFileReader {

    private DocFileReader() {
    }

    public static String read(Context context, Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new IOException("无法打开文件");
        }
        byte[] bytes;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int length;
            while ((length = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, length);
            }
            bytes = buffer.toByteArray();
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {
                // 关不掉就算了
            }
        }

        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        if (utf8.indexOf('\uFFFD') >= 0) {
            try {
                return new String(bytes, Charset.forName("GBK"));
            } catch (Exception ignored) {
                return utf8;
            }
        }
        return utf8;
    }

    public static String displayName(Context context, Uri uri) {
        String name = null;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    name = cursor.getString(index);
                }
            }
        } catch (Exception ignored) {
            // 有些 provider 不支持查询，退回从路径取
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (name == null || name.isEmpty()) {
            name = uri.getLastPathSegment();
        }
        return name == null ? "" : name;
    }
}
