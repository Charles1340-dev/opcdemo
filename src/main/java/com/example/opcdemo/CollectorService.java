package com.example.opcdemo;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CollectorService {

    private final CsvPointLoader csvPointLoader;
    private final OpcDaClientService opcDaClientService;

    public CollectorService(CsvPointLoader csvPointLoader, OpcDaClientService opcDaClientService) {
        this.csvPointLoader = csvPointLoader;
        this.opcDaClientService = opcDaClientService;
    }

    public List<OpcValueRes> collectOnce() {
        List<OpcPoint> points = csvPointLoader.getAllPoints();
        System.out.println(points.size());
        List<OpcValue> values = opcDaClientService.batchRead(points);
        System.out.println(values.size());
        List<OpcValueRes> valuesSender = new ArrayList<>();
        int success = 0;
        int fail = 0;

        for (OpcValue value : values) {
            if (value.isSuccess()) {
                OpcValueRes opcValue = new OpcValueRes();
                opcValue.setItemId(value.getItemId());
                opcValue.setValue(value.getValue());
                valuesSender.add(opcValue);
                success++;
            } else {
                fail++;
            }
        }
        System.out.println("本轮采集完成: success=" + success + ", fail=" + fail);
        return valuesSender;
    }
}