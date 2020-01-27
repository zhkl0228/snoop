package com.aspect.snoop.plugin.jeb;

public class iS {

    public static void VC(byte[] paramArrayOfbyte1, byte[] paramArrayOfbyte2) {
        VC(paramArrayOfbyte1, 0, paramArrayOfbyte1.length, paramArrayOfbyte2, 0, paramArrayOfbyte2.length);
    }

    public static void VC(byte[] paramArrayOfbyte1, byte[] paramArrayOfbyte2, int paramInt1, int paramInt2) {
        VC(paramArrayOfbyte1, 0, paramArrayOfbyte1.length, paramArrayOfbyte2, paramInt1, paramInt2);
    }

    public static void VC(byte[] paramArrayOfbyte1, int paramInt1, int paramInt2, byte[] paramArrayOfbyte2, int paramInt3, int paramInt4) {
        byte[] arrayOfByte = new byte[256];
        int i;
        for (i = 0; i < 256; i++)
            arrayOfByte[i] = (byte)i;
        int j = 0;
        int k = paramInt1;
        for (i = 0; i < 256; i++) {
            if (k == paramInt2)
                k = paramInt1;
            j = (j + arrayOfByte[i] + paramArrayOfbyte1[k]) % 256 & 0xFF;
            byte b = arrayOfByte[i];
            arrayOfByte[i] = arrayOfByte[j];
            arrayOfByte[j] = b;
            k++;
        }
        i = 0;
        j = 0;
        int m = paramInt3;
        while (m < paramInt4) {
            i = (i + 1) % 256 & 0xFF;
            j = (j + arrayOfByte[i]) % 256 & 0xFF;
            byte b1 = arrayOfByte[i];
            arrayOfByte[i] = arrayOfByte[j];
            arrayOfByte[j] = b1;
            byte b2 = arrayOfByte[(arrayOfByte[i] + arrayOfByte[j]) % 256 & 0xFF];
            paramArrayOfbyte2[m] = (byte)(paramArrayOfbyte2[m] ^ b2);
            m++;
        }
    }
}
