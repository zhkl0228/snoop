package com.pnfsoftware.jeb;

import com.aspect.snoop.plugin.jeb.*;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class JebTest extends TestCase {

    public void testDecodeResponse() {
        String response = "63884278458035320773813657819980584471305048476431952585536307162729652911463248629302522991665311008338545950915853795990960555314363510771724780602582631380493207862078704609635207885984565987966902284611020626952513417183551025759132214744505020175263120150047854955571473812362029321120462560595625257501X3C1E8BA6E76C2458F7DF3E1848BDF82643DFAB09F514F412AB7DD800AEABA3056242E2BC3BE8D5DF874164A952998F01A8F257BB188B6F6900CDD32BBC9CDE1CD27E3F2D48D21426B88736E339CD89CFB676D722F245C3736F1741F866185D26AF5E02C1B7D9DE61A14C9514A8BC0869F0DA9F0D672DF6DBF8C12696E71C5F8CF2630D4DC92209AAEBD4CBEF2F5E42A905A12C1B34D1D2A2A5E6182EC9F2031F9FAE926867C50A701BE67670AA";
        CheckUpdate checkUpdate = Util.decodeCheckUpdate(response);
        assertNotNull(checkUpdate);
        System.out.println(checkUpdate);

        ByteBuffer buffer = ByteBuffer.wrap(Hash.calculateMD5("C02ZQ0L4MD6W".getBytes(StandardCharsets.UTF_8)));
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        long machine_id = buffer.getLong();
        System.out.println("machine_id=0x" + Long.toHexString(machine_id));
    }

    private static byte[] VC = new byte[] { 69, 103, -94, -103, 95, -125, -15, 16 };

    public void testDecodeRequest() throws Exception {
        String request = "AA0000008CB99AB6E96A9146B9E6938666E1AA9CC1F776919FB952DB78CA543057C250DDB602D3C892700BA1E77D644E5599D43F4D37987C1C3001167D357A089D8BDF53FC039A4729633505881CFA0A05F2F8B0CC9E489D5964617E2C07AB1876B67025770B4A9CDC9E83129835850F5AF0BD64A63EB6B9BEA6E2395152D6D0654BD390238F59691A8F2523964EA906303A61CB1384C1257CAD874470C7E3A993BF9A0425AD48175CE381B26C05C96A0C8F";
        byte[] data = Util.hexStringToByteArray(request);

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
        System.out.println("request user_id=" + user_id + ", license_id=" + license_id + ", machine_id=0x" + Long.toHexString(machine_id) +
                ", build_type=0x" + Integer.toHexString(build_type) + ", version=" + (major + "." + minor + "." + buildId + "." + timestamp) +
                ", startSeconds=" + startSeconds + ", classSecureMask=" + classSecureMask + ", random=" + random +
                ", username=" + username + ", javaVendor=" + javaVendor + ", javaVersion=" + javaVersion +
                ", osName=" + osName + ", osArch=" + osArch + ", osVersion=" + osVersion + ", compName=" + compName);
    }

}
