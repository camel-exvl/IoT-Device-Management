package pers.camel.iotdm;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Slf4j
@Setter
public class WorkerThread extends Thread {
    private boolean running;
    private ObjectId userId;
    private String mqttServer;
    private String topic;
    private List<ObjectId> devices;
    private String clientId;

    public void run() {
        try {
            String content;
            int qos = 2;
            clientId = userId + "-" + clientId;

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient mqttClient = new MqttClient(mqttServer, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setAutomaticReconnect(true);
            connOpts.setCleanSession(true);
            mqttClient.connect(connOpts);
            log.info("[{}] Connected to broker: {}", clientId, mqttServer);
            Random rand = new Random();
            while (running) {
                // 随机等待10秒
                int interval = rand.nextInt(10);
                Thread.sleep(interval * 1000);

                // 随机选取一个设备
                ObjectId deviceId = devices.get(rand.nextInt(devices.size()));

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date now = new Date();
                Long value = rand.nextLong(100);
                Message msg = new Message();
                msg.setUserID(userId.toString());
                msg.setDeviceID(deviceId.toString());
                msg.setInfo("Device Data " + sdf.format(now));
                msg.setValue(value);
                //超过80告警
                msg.setAlert(value > 80);
                rand.nextFloat();
                //根据杭州经纬度随机生成设备位置信息
                msg.setLng(119.9 + rand.nextFloat() * 0.6);
                msg.setLat(30.1 + rand.nextFloat() * 0.4);
                msg.setTime(now.getTime());
                ObjectMapper mapper = new ObjectMapper();
                content = mapper.writeValueAsString(msg);
                MqttMessage message = new MqttMessage(content.getBytes());
                message.setQos(qos);
                mqttClient.publish(topic, message);
                log.info("[{}] Message published: {}", clientId, content);
            }
            mqttClient.disconnect();
            log.info("[{}] Disconnected", clientId);
        } catch (Exception e) {
            log.error("[{}] Error: {}", clientId, e.getMessage());
        }
    }
}