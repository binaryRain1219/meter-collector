package com.ms.metercollector.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mqtt")
public record MqttProperties(
        String brokerUrl,
        String clientId,
        String topicFilter,    // 예: meter/+/energy
        int qos,
        boolean cleanSession,  // false 면 끊겨 있던 동안의 메시지를 재접속 후 받는다
        String username,       // 브로커 인증을 켠 경우에만 설정
        String password
) {
}
