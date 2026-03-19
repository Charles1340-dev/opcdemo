package com.example.opcdemo;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@Component
public class CollectorTask {

    private final CollectorService collectorService;

    private final OpcDaClientService opcDaClientService;

    @Autowired
    private SimpleMqttSender simpleMqttSender;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${mqtt.dataTopic:}")
    private String dataTopic;

    public CollectorTask(CollectorService collectorService, OpcDaClientService opcDaClientService) {
        this.collectorService = collectorService;
        this.opcDaClientService = opcDaClientService;
    }

    @Scheduled(fixedDelayString = "${collector.fixed-delay-ms:60000}")
    public void run() throws JsonProcessingException {
        if (!opcDaClientService.isConnected()) {
            System.out.println("跳过本轮采集：OPC 尚未连接成功");
            return;
        }
        List<OpcValueRes> opcValueRes = collectorService.collectOnce();
        //发送数据 mqtt形式
        if (!opcValueRes.isEmpty()) {
            String dataString = objectMapper.writeValueAsString(opcValueRes);
            simpleMqttSender.send(dataTopic, dataString);
            System.out.println("mqtt发送成功！");
        }

    }
}