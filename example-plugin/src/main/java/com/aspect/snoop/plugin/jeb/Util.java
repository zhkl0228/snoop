package com.aspect.snoop.plugin.jeb;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Util {

    private static final String Wx = Nj.VC("115079707919157655794004271337785014085416524893510536896198880749104676170401151569284119272680555328442077552676617959280390278294721779275314259285460870759092640181498776671131410915393392138554764830318522728405842846940336918929510079155531877060531469699591700457820297645355241372954344694889513864783");
    private static final String lJ = Nj.VC(new byte[] { -8, 3, 0, 6, 4 }, 1, 206);

    public static CheckUpdate decodeCheckUpdate(String response) {
        if (response == null) {
            return null;
        }

        int index = response.indexOf('X');
        if (index == -1) {
            return null;
        }
        try {
            BigInteger n = new BigInteger(Wx);
            BigInteger e = new BigInteger(lJ);
            BigInteger responseInteger = new BigInteger(response.substring(0, index));
            responseInteger = responseInteger.modPow(e, n);
            byte[] responseData = responseInteger.toByteArray();
            if (responseData.length <= 128) {
                if (responseData.length < 128) {
                    byte[] tmp = new byte[128];
                    for (int m = 128 - responseData.length, i = 0; m < 128; m++, i++)
                        tmp[m] = responseData[i];
                    responseData = tmp;
                }
                byte[] sha256 = Hash.calculateSHA256(Arrays.copyOf(responseData, responseData.length - 32));
                byte[] cmp = Arrays.copyOfRange(responseData, responseData.length - 32, responseData.length);
                if (Arrays.equals(sha256, cmp)) {
                    ByteBuffer buffer = ByteBuffer.wrap(responseData);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    int random = buffer.getInt(4);

                    int code = buffer.getInt(8);
                    int bodySize = buffer.getInt(12);
                    int m = buffer.getInt(16);
                    if (20 + m > responseData.length) {
                        return null;
                    }

                    byte[] key = Arrays.copyOfRange(responseData, 20, 20 + m);
                    byte[] body = hexStringToByteArray(response.substring(index + 1).trim());
                    if (body.length != bodySize) {
                        return null;
                    }
                    iS.VC(key, body, 0, body.length);

                    LEDataInputStream dataIn = new LEDataInputStream(new ByteArrayInputStream(body));
                    int status = dataIn.readInt();
                    if (status != 0) {
                        return null;
                    }
                    String version = readUTF(dataIn);
                    String updateUrl = readUTF(dataIn);
                    String hash = readUTF(dataIn);
                    String password = readUTF(dataIn);
                    int channel = dataIn.readInt();
                    int flags = dataIn.readInt();
                    System.out.println("decodeCheckUpdate random=" + random + ", channel=" + channel + ", flags=0x" + Integer.toHexString(flags));
                    return new CheckUpdate(code, version, updateUrl, hash, password);
                }
            }
        } catch (NumberFormatException | IOException ignored) {}
        return null;
    }

    public static String readUTF(LEDataInputStream dataIn) throws IOException {
        int size = dataIn.readInt();
        if (size < 0)
            throw new IOException("size=" + size);
        byte[] data = new byte[size];
        int read = dataIn.read(data, 0, size);
        if (read != size)
            throw new EOFException();
        return new String(data, StandardCharsets.UTF_8);
    }


    public static String byteArrayToHexString(byte[] data) {
        return byteArrayToHexString(data, 0, data.length);
    }

    public static String byteArrayToHexString(byte[] data, int index) {
        return byteArrayToHexString(data, index, data.length);
    }

    public static String byteArrayToHexString(byte[] data, int index, int length) {
        if (data == null || index < 0 || length > data.length || index > length)
            throw new IllegalArgumentException();
        StringBuilder builder = new StringBuilder();
        for (int i = index; i < length; i++) {
            builder.append(String.format("%02X", data[i]));
        }
        return builder.toString();
    }

    public static byte[] hexStringToByteArray(String hex, int index, int length) {
        if (index < 0 || index > length || (length - index) % 2 != 0)
            return null;
        int i = (length - index) / 2;
        byte[] data = new byte[i];
        for (int b = 0; b < i; b++) {
            try {
                data[b] = (byte)Integer.parseInt(hex.substring(index, index + 2), 16);
                index += 2;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return data;
    }

    public static byte[] hexStringToByteArray(String hex) {
        return hexStringToByteArray(hex, 0, hex.length());
    }

}
