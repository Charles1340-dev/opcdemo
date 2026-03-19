package com.example.opcdemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 轻量级MQTT发送工具类
 * 支持：同步发送、异步发送、JSON发送、QoS设置、自动重连
 */
@Slf4j
@Component
public class SimpleMqttSender {

    // === 配置参数（从application.yml读取）===
    @Value("${mqtt.broker:}")
    private String broker;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    @Value("${mqtt.clientId:}")
    private String clientId;

    @Value("${mqtt.defaultQos:1}")
    private int defaultQos;

    @Value("${mqtt.timeout:30}")
    private int timeout;

    @Value("${mqtt.keepAlive:60}")
    private int keepAlive;

    @Value("${mqtt.cleanSession:true}")
    private boolean cleanSession;

    @Value("${mqtt.autoReconnect:true}")
    private boolean autoReconnect;

    // === 核心组件 ===
    private MqttClient mqttClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(3);
    private volatile boolean connected = false;

    /**
     * 初始化连接
     */
    @PostConstruct
    public void init() {
        connect();
    }

    /**
     * 连接MQTT服务器
     */
    public synchronized void connect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                return;
            }

            // 生成客户端ID
            String actualClientId = clientId;
            if (actualClientId == null || actualClientId.trim().isEmpty()) {
                actualClientId = "mqtt-sender-" + UUID.randomUUID().toString().substring(0, 8);
            }

            // 创建客户端
            mqttClient = new MqttClient(
                    broker,
                    actualClientId,
                    new MemoryPersistence()
            );

            // 设置回调
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    connected = true;
                    log.info("MQTT {}: {}", reconnect ? "重连成功" : "连接成功", serverURI);
                }

                @Override
                public void connectionLost(Throwable cause) {
                    connected = false;
                    log.warn("MQTT连接丢失: {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // 发送端不需要处理接收
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 消息发送完成回调
                }
            });

            // 连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setConnectionTimeout(timeout);
            options.setKeepAliveInterval(keepAlive);
            options.setCleanSession(cleanSession);
            options.setAutomaticReconnect(autoReconnect);

            if (username != null && !username.trim().isEmpty()) {
                options.setUserName(username);
                if (password != null && !password.trim().isEmpty()) {
                    options.setPassword(password.toCharArray());
                }
            }

            // 设置遗嘱消息
            options.setWill("status/offline", "disconnected".getBytes(), 1, true);

            // 连接
            mqttClient.connect(options);
            connected = true;

            log.info("MQTT发送器已就绪，Broker: {}, ClientId: {}", broker, actualClientId);

        } catch (Exception e) {
            connected = false;
            log.error("MQTT连接失败: {}", e.getMessage());
            // 可以添加重连逻辑
            scheduleReconnect();
        }
    }

    /**
     * 定时重连
     */
    private void scheduleReconnect() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000);
                if (!connected) {
                    log.info("尝试重新连接MQTT...");
                    connect();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, asyncExecutor);
    }

    /**
     * 发送文本消息（同步）
     */
    public boolean send(String topic, String message) {
        return send(topic, message, defaultQos, false);
    }

    /**
     * 发送文本消息（指定QoS和保留标志）
     */
    public boolean send(String topic, String message, int qos, boolean retained) {
        try {
            checkConnection();

            MqttMessage mqttMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
            mqttMessage.setQos(qos);
            mqttMessage.setRetained(retained);


            mqttClient.publish(topic, mqttMessage);

            log.debug("消息发送成功: {} -> {}", topic, message.length() > 100 ?
                    message.substring(0, 97) + "..." : message);
            return true;

        } catch (Exception e) {
            log.error("消息发送失败: {} -> {}，错误: {}", topic, message, e.getMessage());
            handleSendError(e);
            return false;
        }
    }

    /**
     * 发送JSON对象
     */
    public <T> boolean sendJson(String topic, T data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return send(topic, json);
        } catch (Exception e) {
            log.error("JSON消息发送失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 异步发送消息
     */
    public CompletableFuture<Boolean> sendAsync(String topic, String message) {
        return CompletableFuture.supplyAsync(() -> send(topic, message), asyncExecutor);
    }

    /**
     * 发送字节数组
     */
    public boolean sendBytes(String topic, byte[] data) {
        try {
            checkConnection();

            MqttMessage mqttMessage = new MqttMessage(data);
            mqttMessage.setQos(defaultQos);

            mqttClient.publish(topic, mqttMessage);

            log.debug("字节消息发送成功: {} -> {} bytes", topic, data.length);
            return true;

        } catch (Exception e) {
            log.error("字节消息发送失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 发送保留消息
     */
    public boolean sendRetained(String topic, String message) {
        return send(topic, message, defaultQos, true);
    }

    /**
     * 发送设备数据（物联网场景专用）
     */
    public boolean sendDeviceData(String deviceId, String type, Object value) {
        String topic = String.format("device/%s/%s", deviceId, type);

        try {
            String payload = String.format("{\"deviceId\":\"%s\",\"type\":\"%s\",\"value\":%s,\"timestamp\":%d}",
                    deviceId, type,
                    value instanceof String ? "\"" + value + "\"" : value,
                    System.currentTimeMillis());

            return send(topic, payload);
        } catch (Exception e) {
            log.error("设备数据发送失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查连接状态
     */
    private void checkConnection() throws MqttException {
        if (!connected || mqttClient == null || !mqttClient.isConnected()) {
            connect();
            if (!mqttClient.isConnected()) {
                throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
            }
        }
    }

    /**
     * 处理发送错误
     */
    private void handleSendError(Exception e) {
        if (e instanceof MqttException) {
            MqttException me = (MqttException) e;
            if (me.getReasonCode() == MqttException.REASON_CODE_CLIENT_NOT_CONNECTED) {
                connected = false;
                scheduleReconnect();
            }
        }
    }

    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return connected && mqttClient != null && mqttClient.isConnected();
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                connected = false;
                log.info("MQTT连接已关闭");
            }
        } catch (Exception e) {
            log.error("断开连接失败: {}", e.getMessage());
        }
    }

    /**
     * 清理资源
     */
    @PreDestroy
    public void destroy() {
        disconnect();
        asyncExecutor.shutdown();
    }

    // ===== 静态工具方法 =====

    /**
     * 快速发送消息（静态方法，适用于简单场景）
     */
    public static boolean quickSend(String broker, String topic, String message) {
        try {
            MqttClient client = new MqttClient(broker,
                    "quick-" + UUID.randomUUID(),
                    new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);
            options.setCleanSession(true);

            client.connect(options);

            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);

            client.publish(topic, mqttMessage);
            client.disconnect();
            client.close();

            return true;
        } catch (Exception e) {
            log.error("快速发送失败: {}", e.getMessage());
            return false;
        }
    }
}
