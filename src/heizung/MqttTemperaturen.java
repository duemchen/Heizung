/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package heizung;

import de.horatio.common.HoraTime;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.log4j.core.appender.MemoryMappedFileManager;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author duemchen
 */
class MqttTemperaturen {

    private MqttClient client;
    private static MqttTemperaturen instance = null;
    private String MQTTLINK = "?";

    public static MqttTemperaturen getInstance(String MQTTLINK) {
        if (instance == null) {
            try {
                instance = new MqttTemperaturen(MQTTLINK);
            } catch (MqttException ex) {
                Logger.getLogger(MqttTemperaturen.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return instance;
    }

    public MqttTemperaturen(String MQTTLINK) throws MqttException {
        MemoryPersistence persistence = new MemoryPersistence();
        SecureRandom random = new SecureRandom();
        String id = new BigInteger(60, random).toString(32);
        System.out.println("id=" + id);
        client = new MqttClient(MQTTLINK, id, persistence);
        this.MQTTLINK = MQTTLINK;
    }

    void sendTemps(double soll, double aussenTemp) throws JSONException {
        //TODO Format json

        JSONObject jo = new JSONObject();
        JSONObject temperaturen = new JSONObject();
        JSONObject relais = new JSONObject();
        for (Heizung.TEMP temp : Heizung.TEMP.values()) {
            //content += temp.name() + "=" + temp.getTempLast() + ", ";
            temperaturen.put(temp.name(), temp.getTempLast());
        }
        temperaturen.put("AUSSEN", aussenTemp);
        temperaturen.put("SOLL", Double.parseDouble(String.format("%3.2f", soll)));

        for (Heizung.DA da : Heizung.DA.values()) {
            relais.put(da.name(), da.isOn());
        }
        jo.put("time", HoraTime.dateToStr(new Date()));
        jo.put("temperatures", temperaturen);
        jo.put("relais", relais);

        MqttMessage message = new MqttMessage();
        Runtime rt = Runtime.getRuntime();

        String content = jo.toString();
        message.setPayload(content.getBytes());
        try {
            if (!client.isConnected()) {
                client.connect();
            }
            client.publish("simago/test", message);
        } catch (MqttException ex) {
            Logger.getLogger(MqttTemperaturen.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    void sendEinzelTemps(double soll, double aussenTemp) throws JSONException {
        //TODO Format json

        String content;
        content = String.format("%3.2f", aussenTemp);
        MqttMessage mmAussen = new MqttMessage();
        mmAussen.setPayload(content.getBytes());
        //
        content = String.format("%3.2f", soll);
        MqttMessage mmSoll = new MqttMessage();
        mmSoll.setPayload(content.getBytes());
//
        content = String.format("%3.2f", Heizung.TEMP.FERNHEIZ_VORLAUF.getTempLast().doubleValue());
        MqttMessage mmVL = new MqttMessage();
        mmVL.setPayload(content.getBytes());
//
        content = String.format("%3.2f", Heizung.TEMP.FERNHEIZ_RUECKLAUF.getTempLast().doubleValue());
        MqttMessage mmRL = new MqttMessage();
        mmRL.setPayload(content.getBytes());

        try {
            if (!client.isConnected()) {
                client.connect();
            }
            client.publish("simago/heizung/soll", mmSoll);
            client.publish("simago/heizung/aussen", mmAussen);
            client.publish("simago/heizung/vl", mmVL);
            client.publish("simago/heizung/rl", mmRL);
        } catch (MqttException ex) {
            Logger.getLogger(MqttTemperaturen.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    void sendByebye() {
        //TODO Format json
        MqttMessage message = new MqttMessage();
        String content = "Byebye vom Heizungsregler.  Speicher:" + getMemory();
        message.setPayload(content.getBytes());
        try {
            if (!client.isConnected()) {
                client.connect();
            }
            client.publish("simago/test", message);

        } catch (MqttException ex) {
            Logger.getLogger(MqttTemperaturen.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    void sendHallo() {
        MqttMessage message = new MqttMessage();
        String content = "Hallo vom Heizungsregler. Speicher: " + getMemory();
        message.setPayload(content.getBytes());

        try {
            if (!client.isConnected()) {
                client.connect();
            }
            client.publish("simago/test", message);
        } catch (MqttException ex) {
            Logger.getLogger(MqttTemperaturen.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private String getMemory() {
        Runtime rt = Runtime.getRuntime();
        return new DecimalFormat("###,###.###").format((rt.totalMemory() - rt.freeMemory()));

    }

    void setMqttLink(String MQTTLINK) {
        this.MQTTLINK = MQTTLINK;
    }

}
