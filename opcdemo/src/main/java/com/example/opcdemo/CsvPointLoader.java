package com.example.opcdemo;

import com.opencsv.CSVReader;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CsvPointLoader {

    private final List<OpcPoint> allPoints = new ArrayList<>();

    public List<OpcPoint> getAllPoints() {
        return allPoints;
    }

    @PostConstruct
    public void init() throws Exception {
        loadCsvIfExists("D:/opc-app/points/OPC_CONF.csv", "GBK");
        loadCsvIfExists("points/OPC_CONF.csv", "GBK");

        Map<String, OpcPoint> dedup = new LinkedHashMap<>();
        for (OpcPoint point : allPoints) {
            if (StringUtils.isNotBlank(point.getItemId())) {
                dedup.put(point.getItemId(), point);
            }
        }

        allPoints.clear();
        allPoints.addAll(dedup.values());

        System.out.println("CSV点位加载完成，总点位数: " + allPoints.size());

        for (int i = 0; i < Math.min(10, allPoints.size()); i++) {
            System.out.println("点位[" + i + "]=" + allPoints.get(i).getItemId());
        }
    }

    private void loadCsvIfExists(String path, String charsetName) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("文件不存在，跳过: " + path);
            return;
        }
        loadCsv(path, charsetName);
    }

    private void loadCsv(String path, String charsetName) throws Exception {
        File file = new File(path);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), Charset.forName(charsetName)));
             CSVReader csvReader = new CSVReader(reader)) {

            String[] row;
            while ((row = csvReader.readNext()) != null) {
                if (row.length < 1) {
                    continue;
                }

                String itemId = safeGet(row, 0);

                if (StringUtils.isBlank(itemId)) {
                    continue;
                }

                itemId = itemId.trim();

                if ("itemId".equalsIgnoreCase(itemId)) {
                    continue;
                }
                if ("tag".equalsIgnoreCase(itemId)) {
                    continue;
                }

                OpcPoint point = new OpcPoint();
                point.setSourceFile(path);
                point.setItemId(itemId);

                allPoints.add(point);
            }
        }
    }

    private String safeGet(String[] row, int index) {
        return index < row.length ? row[index] : null;
    }
}