package com.ms.metercollector.mqtt;

import com.ms.metercollector.reading.MeterReadingHandler;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

// 로컬 ActiveMQ(meter-publisher 의 docker compose) 가 떠 있어야 한다.
// 실제 토픽(meter/...)과 섞이지 않도록 test/ 로 시작하는 토픽을 쓰고, 브로커에 구독이 남지 않게 cleanSession 으로 붙는다.
@SpringBootTest
class MeterReadingSubscriberTest {

    @Value("${mqtt.broker-url}")
    String brokerUrl;

    @Test
    void 구독한_토픽의_메시지를_핸들러로_넘긴다() throws Exception {
        MeterReadingHandler handler = mock(MeterReadingHandler.class);
        MqttProperties properties = new MqttProperties(
                brokerUrl, "meter-collector-test-sub", "test/meter/+/energy", 1, true, null, null);
        MeterReadingSubscriber subscriber = new MeterReadingSubscriber(properties, handler);
        MqttClient publisher = new MqttClient(brokerUrl, "meter-collector-test-pub", new MemoryPersistence());
        try {
            subscriber.start();
            await().atMost(Duration.ofSeconds(5)).until(subscriber::isConnected);

            publisher.connect();
            byte[] payload = "{\"deviceCode\":\"AHU-01\"}".getBytes(StandardCharsets.UTF_8);
            publisher.publish("test/meter/AHU-01/energy", payload, 1, false);

            verify(handler, timeout(5_000)).handle(eq("test/meter/AHU-01/energy"), eq(payload));
        } finally {
            if (publisher.isConnected()) {
                publisher.disconnect();
            }
            publisher.close();
            subscriber.stop();
            subscriber.destroy();
        }
    }
}
