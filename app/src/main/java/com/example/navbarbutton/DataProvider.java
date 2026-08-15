package com.example.navbarbutton;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class DataProvider extends ContentProvider {

    static final String AUTHORITY = "com.example.navbarbutton.data";

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {

        String key = uri.getLastPathSegment();

        String value = getContext()
                .getSharedPreferences("data", Context.MODE_PRIVATE)
                .getString(key, null);

        MatrixCursor cursor =
                new MatrixCursor(new String[]{"value"});

        cursor.addRow(new Object[]{value});

        return cursor;
    }

    @Override public boolean onCreate() {
        return true;
    }

    @Override public String getType(Uri uri) {
        return "text/plain";
    }

    @Override public int delete(Uri uri, String s, String[] a) {
        return 0;
    }

    @Override public int update(Uri uri, ContentValues v, String s, String[] a) {
        return 0;
    }

    @Override public Uri insert(Uri uri, ContentValues v) {
        return null;
    }
}
