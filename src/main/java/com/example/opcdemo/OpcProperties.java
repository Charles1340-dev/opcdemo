package com.example.opcdemo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opc")
public class OpcProperties {

    private String host;
    private String domain;
    private String user;
    private String password;
    private String progId;
    private String clsId;
    private String groupName = "spring-opc-group";
    private Integer updateRate = 1000;


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProgId() {
        return progId;
    }

    public void setProgId(String progId) {
        this.progId = progId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Integer getUpdateRate() {
        return updateRate;
    }

    public void setUpdateRate(Integer updateRate) {
        this.updateRate = updateRate;
    }
    public String getClsId() {
        return clsId;
    }
    public void setClsId(String clsId) {
        this.clsId = clsId;
    }
}