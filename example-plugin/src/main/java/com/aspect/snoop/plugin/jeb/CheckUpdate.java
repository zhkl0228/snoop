package com.aspect.snoop.plugin.jeb;

public class CheckUpdate {

    private final int code;
    private final String version;
    private final String updateUrl;
    private final String hash;
    private final String password;

    public CheckUpdate(int code, String version, String updateUrl, String hash, String password) {
        this.code = code;
        this.version = version;
        this.updateUrl = updateUrl;
        this.hash = hash;
        this.password = password;
    }

    @Override
    public String toString() {
        return "CheckUpdate{" +
                "code=" + code +
                ", version='" + version + '\'' +
                ", updateUrl='" + updateUrl + '\'' +
                ", hash='" + hash + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

}
