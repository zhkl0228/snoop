package com.aspect.snoop.plugin.jeb;

import cn.banny.utils.Hex;
import com.fuzhu8.inspector.plugin.Appender;
import de.robv.android.xposed.XC_MethodHook;

import java.io.ByteArrayInputStream;

public class JebNetPostHandler extends XC_MethodHook {

    private final Appender appender;

    public JebNetPostHandler(Appender appender) {
        this.appender = appender;
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        super.afterHookedMethod(param);
        String host = (String) param.args[1];
        String data = (String) param.args[2];
        String response = (String) param.getResult();
        appender.out_println(new Exception("post host=" + host + ", data=" + data + ", response=" + response));

        if ("https://www.pnfsoftware.com/jps/checkupdate".equals(host) ||
                "https://lise.pnfsoftware.com/jps/checkupdate".equals(host)) {
            dumpRequest(data, response);
            param.setResult(null);
        }
    }

    private static byte[] VC = new byte[] { 69, 103, -94, -103, 95, -125, -15, 16 };

    private void dumpRequest(String request, String response) {
        try {
            byte[] data = Hex.decodeHex(request.toCharArray());

            byte[] key = new byte[16];
            System.arraycopy(data, 0, key, 0, 8);
            System.arraycopy(VC, 0, key, 8, 8);
            iS.VC(key, data, 8, data.length);

            LEDataInputStream dataIn = new LEDataInputStream(new ByteArrayInputStream(data));
            dataIn.skipBytes(16);
            int user_id = dataIn.readInt();
            long license_id = dataIn.readLong();
            long machine_id = dataIn.readLong();
            int build_type = dataIn.readInt();
            int major = dataIn.readInt();
            int minor = dataIn.readInt();
            int buildId = dataIn.readInt();
            long timestamp = dataIn.readLong();
            int startSeconds = dataIn.readInt();
            int classSecureMask = dataIn.readInt();
            int random = dataIn.readInt();
            String username = Util.readUTF(dataIn);
            String javaVendor = Util.readUTF(dataIn);
            String javaVersion = Util.readUTF(dataIn);
            String osName = Util.readUTF(dataIn);
            String osArch = Util.readUTF(dataIn);
            String osVersion = Util.readUTF(dataIn);
            String compName = Util.readUTF(dataIn);
            appender.out_println("dumpRequest user_id=" + user_id + ", license_id=" + license_id + ", machine_id=" + machine_id +
                    ", build_type=0x" + Integer.toHexString(build_type) + ", version=" + (major + "." + minor + "." + buildId + "." + timestamp) +
                    ", startSeconds=" + startSeconds + ", classSecureMask=0b" + Integer.toBinaryString(classSecureMask) + ", random=" + random +
                    ", username=" + username + ", javaVendor=" + javaVendor + ", javaVersion=" + javaVersion +
                    ", osName=" + osName + ", osArch=" + osArch + ", osVersion=" + osVersion + ", compName=" + compName);
            appender.out_println("dumpRequest response=" + Util.decodeCheckUpdate(response));
        } catch(Exception ignored) {}
    }

}
