package eu.guardiansystems.livesapp.service;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface MqttResponse {
    public void onSuccess(String topic);
    public void onResponse(String topic, MqttMessage message);
    public void onError(String message);
}
