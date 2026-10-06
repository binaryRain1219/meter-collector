package com.ms.metercollector.mqtt;

import com.ms.metercollector.reading.MeterReadingHandler;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// 계량기 토픽을 구독해 받은 메시지를 MeterReadingHandler 로 넘긴다.
// mqtt.subscriber.enabled=false 면 만들지 않는다. 테스트가 실제 토픽을 구독하지 않게 하기 위함.
@Component
@ConditionalOnProperty(name = "mqtt.subscriber.enabled", havingValue = "true", matchIfMissing = true)
public class MeterReadingSubscriber implements SmartLifecycle, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(MeterReadingSubscriber.class);

    private static final long RETRY_SECONDS = 10;

    private final MqttProperties properties;
    private final MqttClient client;
    private final MqttConnectOptions connectOptions;
    private ScheduledExecutorService connector;
    private volatile boolean running;

    public MeterReadingSubscriber(MqttProperties properties, MeterReadingHandler handler) throws MqttException {
        this.properties = properties;
        // 기본 생성자는 작업 디렉터리에 상태 파일을 만들기 때문에 메모리 저장소를 쓴다.
        this.client = new MqttClient(properties.brokerUrl(), properties.clientId(), new MemoryPersistence());
        this.client.setTimeToWait(10_000);
        this.client.setCallback(new Callback(handler));
        this.connectOptions = connectOptions(properties);
    }

    // 브로커가 처음부터 없거나 중간에 끊겨도, 붙을 때까지 RETRY_SECONDS 마다 다시 연결한다.
    @Override
    public synchronized void start() {
        connector = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "mqtt-connector");
            thread.setDaemon(true);
            return thread;
        });
        connector.scheduleWithFixedDelay(this::ensureConnected, 0, RETRY_SECONDS, TimeUnit.SECONDS);
        running = true;
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (connector != null) {
            connector.shutdownNow();
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            log.warn("MQTT 연결 종료 실패 ({})", e.getMessage());
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void destroy() throws MqttException {
        client.close();
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    private synchronized void ensureConnected() {
        if (!running || client.isConnected()) {
            return;
        }
        try {
            client.connect(connectOptions);
            client.subscribe(properties.topicFilter(), properties.qos());
            log.info("MQTT 구독 시작: {} {}", properties.brokerUrl(), properties.topicFilter());
        } catch (MqttException e) {
            log.warn("MQTT 브로커 연결 실패: {} ({}), {}초 후 재시도", properties.brokerUrl(), e.getMessage(), RETRY_SECONDS);
        }
    }

    private static MqttConnectOptions connectOptions(MqttProperties properties) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(properties.cleanSession());
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);
        if (properties.username() != null && !properties.username().isBlank()) {
            options.setUserName(properties.username());
            options.setPassword(Objects.requireNonNullElse(properties.password(), "").toCharArray());
        }
        return options;
    }

    private record Callback(MeterReadingHandler handler) implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            log.warn("MQTT 연결 끊김 ({}), {}초 안에 재연결", cause.getMessage(), RETRY_SECONDS);
        }

        // 여기서 예외가 나면 Paho 는 ack 하지 않고 연결을 끊는다. 재접속하면 브로커가 같은 메시지를 다시 보낸다.
        // 그래서 DB 장애처럼 다시 받으면 되는 실패만 handler 에서 예외로 올라온다.
        @Override
        public void messageArrived(String topic, MqttMessage message) {
            handler.handle(topic, message.getPayload());
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }
}
