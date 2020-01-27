package com.aspect.snoop.plugin.jeb;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Nj {

    private String VC;

    private byte[] Wx;

    private int lJ;

    private int ca;

    public static String VC(String paramString) {
        return paramString;
    }

    public static String VC(byte[] paramArrayOfbyte, int paramInt1, int paramInt2) {
        return (new Nj(paramArrayOfbyte, paramInt1, paramInt2)).VC();
    }

    Nj(String paramString) {
        this.VC = paramString;
    }

    public Nj(byte[] paramArrayOfbyte, int paramInt1, int paramInt2) {
        this.Wx = paramArrayOfbyte;
        this.lJ = paramInt1;
        this.ca = paramInt2;
    }

    public String VC() {
        if (this.VC != null)
            return this.VC;
        if (this.Wx == null)
            throw new RuntimeException();
        if (this.lJ == 0 || this.Wx.length == 0)
            try {
                return new String(this.Wx, StandardCharsets.UTF_8);
            } catch (Exception exception) {
                return new String(this.Wx, Charset.defaultCharset());
            }
        if (this.lJ == 1) {
            int i = this.Wx.length;
            byte[] arrayOfByte = new byte[i];
            byte b = (byte)this.ca;
            for (byte b1 = 0; b1 < i; b1++) {
                arrayOfByte[b1] = (byte)(b ^ this.Wx[b1]);
                b = arrayOfByte[b1];
            }
            try {
                return new String(arrayOfByte, StandardCharsets.UTF_8);
            } catch (Exception exception) {
                return new String(arrayOfByte, Charset.defaultCharset());
            }
        }
        if (this.lJ == 2) {
            int i = this.Wx.length;
            byte[] arrayOfByte = new byte[i];
            String str = "Copyright (c) 1993, 2015, Oracle and/or its affiliates. All rights reserved. ";
            int j = 0;
            for (byte b = 0; b < i; b++) {
                arrayOfByte[b] = (byte)(this.Wx[b] ^ (byte)str.charAt(j));
                j = (j + 1) % str.length();
            }
            try {
                return new String(arrayOfByte, StandardCharsets.UTF_8);
            } catch (Exception exception) {
                return new String(arrayOfByte, Charset.defaultCharset());
            }
        }
        throw new RuntimeException();
    }
}
