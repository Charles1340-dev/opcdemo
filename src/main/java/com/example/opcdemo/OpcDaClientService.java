package com.example.opcdemo;


import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OpcDaClientService {

    private final OpcProperties opcProperties;

    private Server server;
    private Group group;
    private volatile boolean connected = false;

    public OpcDaClientService(OpcProperties opcProperties) {
        this.opcProperties = opcProperties;
    }

    @PostConstruct
    public void connect() throws Exception {
        ConnectionInformation ci = new ConnectionInformation();
        ci.setHost(opcProperties.getHost());
        ci.setDomain(opcProperties.getDomain());
        ci.setUser(opcProperties.getUser());
        ci.setPassword(opcProperties.getPassword());
//        ci.setProgId(opcProperties.getProgId());
        ci.setClsid(opcProperties.getClsId());

        this.server = new Server(ci, null);
        this.server.connect();

        this.group = server.addGroup(opcProperties.getGroupName());
        this.group.setActive(true);

        this.connected = true;
        System.out.println("OPC连接成功: " + opcProperties.getHost() + " / " + opcProperties.getClsId());
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (server != null) {
                server.disconnect();
            }
        } catch (Exception ignored) {
        }
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    public OpcValue readPoint(OpcPoint point) {
        OpcValue result = new OpcValue();
        result.setItemId(point.getItemId());

        try {
            Item item = group.addItem(point.getItemId());
            ItemState state = item.read(false);

            result.setSuccess(true);
            result.setValue(state.getValue() != null ? state.getValue().getObject() : null);
            result.setQuality(String.valueOf(state.getQuality()));
            result.setTimestamp(state.getTimestamp() != null ? state.getTimestamp().getTime() : new Date());
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMsg(e.getMessage());
            result.setTimestamp(new Date());
        }

        return result;
    }

    public List<OpcValue> batchRead(List<OpcPoint> points) {
        List<OpcValue> list = new ArrayList<>();
        for (OpcPoint point : points) {
            list.add(readPoint(point));
        }
        return list;
    }
}