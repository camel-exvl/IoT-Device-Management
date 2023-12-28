package pers.camel.mqttClient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

@Slf4j
public class App {
    private static final Vector<WorkerThread> threadVector = new Vector<>();

    public static void main(String[] args) {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.mongodb.driver").setLevel(Level.INFO);
        Properties properties = new Properties();
        try {
            properties.load(App.class.getClassLoader().getResourceAsStream("application.properties"));
            log.info("Load properties success");
        } catch (Exception e) {
            log.error("Load properties error: " + e.getMessage());
        }
        try (MongoClient mongoClient = MongoClients.create(properties.getProperty("mongodb.uri"))) {
            MongoDatabase database = mongoClient.getDatabase(properties.getProperty("mongodb.database"));
            log.info("Connect to database: " + database.getName());
            MongoCollection<Document> collection = database.getCollection("user");
            log.info("Connect to collection: " + collection.getNamespace().getCollectionName());
            int maxThread = Integer.parseInt(properties.getProperty("max.thread"));
            for (Document doc : collection.find()) {
                List<Document> devices = (List<Document>) doc.get("devices");
                List<ObjectId> deviceIds = new ArrayList<>();
                for (Document device : devices) {
                    deviceIds.add((ObjectId) device.get("_id"));
                }

                if (deviceIds.isEmpty()) {
                    log.warn("User {} has no device", doc.get("_id"));
                    continue;
                }

                for (int i = 0; i < maxThread; ++i) {
                    WorkerThread workerThread = new WorkerThread();
                    workerThread.setUserId((ObjectId) doc.get("_id"));
                    workerThread.setRunning(true);
                    workerThread.setClientId(String.valueOf(i));
                    workerThread.setMqttServer(properties.getProperty("mqtt.server"));
                    workerThread.setTopic(properties.getProperty("mqtt.topic"));
                    workerThread.setDevices(deviceIds);
                    threadVector.add(workerThread);
                    workerThread.start();
                }
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Waiting for all threads to finish");
                for (WorkerThread workerThread : threadVector) {
                    workerThread.setRunning(false);
                }
            }));
            for (WorkerThread workerThread : threadVector) {
                workerThread.join();
            }
            log.info("The program is exiting...");
        } catch (Exception e) {
            log.error("MongoDB connection error: " + e.getMessage());
        }
    }
}