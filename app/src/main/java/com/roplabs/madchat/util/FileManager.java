package com.roplabs.madchat.util;

import java.io.IOException;
import java.io.InputStream;

public class FileManager {
    public static String readInputStream(InputStream input) throws IOException {
        int size = input.available();
        byte[] buffer = new byte[size];
        input.read(buffer);
        input.close();

        return new String(buffer, "UTF-8");
    }
}
