package com.aspect.snoop.plugin.jeb;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

public class Hash {

    public static long calculateCRC32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    public static byte[] calculateSHA256(byte[] data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return messageDigest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("calculateSHA256", e);
        }
    }

}
