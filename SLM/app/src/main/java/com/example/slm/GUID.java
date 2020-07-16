package com.example.slm;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

public class GUID {
    private static final String TAG = "YOUR-TAG-NAME";

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static String creatUUIDFile(String saveFileName, /*查詢*/ ContentResolver resolver) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, saveFileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
        // TODO: 2019-10-11 IS_PENDING = 1表示对应的item还没准备好
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

        Uri uri = resolver.insert(collection, values);

        try {
            //访问 对于单个媒体文件，请使用 openFileDescriptor()。
            assert uri != null;
            ParcelFileDescriptor fielDescriptor = resolver.openFileDescriptor(uri, "w", null);
            assert fielDescriptor != null;
            FileOutputStream outputStream = new FileOutputStream(fielDescriptor.getFileDescriptor());
            try {
                //讲UUID写入到文件中
                String uuidStr = UUID.randomUUID().toString();
                outputStream.write(uuidStr.getBytes());
                outputStream.close();
                Log.d(TAG, "写入 uuidStr:" + uuidStr);

                return uuidStr;
            } catch (IOException e) {
                e.printStackTrace();
            }
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);          //设置为0
            resolver.update(uri, values, null, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static String checkUUIDFileByUri(String saveFileName, /*查詢*/ ContentResolver resolver) {
        Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media._ID
        };

        // 添加筛选条件
        String selection = MediaStore.Images.Media.DISPLAY_NAME + "=" + "'" + saveFileName + "'";
        Cursor mCursor = resolver.query(mImageUri, projection, selection, null, null);

        String getSaveContent = "";
        if (mCursor != null) {
            while (mCursor.moveToNext()) {

                int fileIdIndex = mCursor.getColumnIndex(MediaStore.Images.Media._ID);
                String thumbPath = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                        .appendPath(String.valueOf(mCursor.getInt(fileIdIndex))).build().toString();
                Uri fileUri = Uri.parse(thumbPath);
                try {
                    ParcelFileDescriptor fielDescriptor = resolver.openFileDescriptor(fileUri, "r", null);
                    assert fielDescriptor != null;
                    FileInputStream inputStream = new FileInputStream(fielDescriptor.getFileDescriptor());
                    getSaveContent = inputStreamToString(inputStream);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                //只有在得到的唯一标识符不为空的情况下才结束循环，否则一直循环
                if (!TextUtils.isEmpty(getSaveContent)) {
                    break;
                }
            }
            mCursor.close();

        }
        return getSaveContent;
    }

    private static String inputStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("/n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
