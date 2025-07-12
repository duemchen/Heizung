/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package heizung;

import de.horatio.common.HoraIni;
import de.horatio.common.NetworkUtil;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 *
 * @author duemchen
 *
 * Heizung soll auf mqtt ereignis reagieren und Rückmeldung über den Zustand
 * geben.
 *
 */
public class MQTTHeizung implements MqttCallbackExtended {

    private static final String INI = "solarMQTT.ini";
    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger();
    private static MQTTHeizung instance = null;
    private final MqttClient client;
    private String MQUSER;
    private static final String MQURI = HoraIni.LeseIniString(INI, "MQTT", "URI", "tcp://192.168.10.51:1883", true);
    private static final String MQROOT = HoraIni.LeseIniString(INI, "MQTT", "ROOT", "simago/heizungSolar", true);

    //
    private boolean bHeizen = "true".equalsIgnoreCase(HoraIni.LeseIniString(INI, "Solarheizung", "aktiv", "false", true));

    public static MQTTHeizung getInstance() {
        if (instance == null) {
            try {
                instance = new MQTTHeizung();
            } catch (MqttException ex) {
                log.error(ex);
            }
        }
        return instance;
    }

    public MQTTHeizung() throws MqttException {
        //client = new MqttClient("tcp://duemchen.ddns.net:1883", "publishTempVeranda");

        MQUSER = NetworkUtil.getMyHostname("dummy");
        MQUSER += (int) (Math.random() * 999);
        log.info("MQURI: " + MQURI + ", MQUSER: " + MQUSER);
        //3.23 autoreconnect
        MqttConnectOptions conOpt = new MqttConnectOptions();
        conOpt.setCleanSession(true);
        conOpt.setAutomaticReconnect(true);
        //3.23 autoreconnect
        this.client = new MqttClient(MQURI, MQUSER, new MemoryPersistence());
        this.client.setCallback(this);//3.23 autoreconnect
        this.client.connect(conOpt);//3.23 autoreconnect

    }

    public boolean getHeizen() {
        return bHeizen;
    }

    private String getMemory() {
        Runtime rt = Runtime.getRuntime();
        return new DecimalFormat("###,###.###").format((rt.totalMemory() - rt.freeMemory()));

    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void messageArrived(String path, MqttMessage mm) throws Exception {
        log.debug("messageArrived()" + path + ",,," + mm);
        String s = mm.toString();
        if (path.toLowerCase().contains("setaktiv")) {
            bHeizen = s.toLowerCase().contains("on");
            HoraIni.SchreibeIniString(INI, "Solarheizung", "aktiv", "" + bHeizen);
            sendInfoAktiv(bHeizen); //Rückmeldung an MQTT
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
        // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void connectComplete(boolean bln, String string) {
        //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        System.out.println("connectComplete. subscribe...");
        log.info("MqttHeizung connectComplete. subscribe...");
        try {
            client.subscribe(MQROOT + "/setAktiv", 1);
            log.info("MqttVeranda subscribe done ok.");
            sendSetAktiv(bHeizen); //erzeugen des SetEintrages (für händische Eingabe) und sofort eine echte Inforückmeldung davon ausgelöst                        
        } catch (MqttException ex) {
            log.error(ex);
        }
    }

    private void sendSetAktiv(boolean b) {
        MqttMessage message = new MqttMessage();
        String content = bHeizen ? "on" : "off";
        message.setPayload(content.getBytes());
        message.setRetained(true);

        try {
            if (!client.isConnected()) {
                client.connect();
            }
            client.setCallback(this);
            client.publish(MQROOT + "/setAktiv", message);
        } catch (MqttException ex) {
            log.error(ex);
        }

    }

    private void sendInfoAktiv(boolean b) {
        MqttMessage message = new MqttMessage();
        String content = bHeizen ? "Solarspeicher genutzt" : "abgeschaltet";
        message.setPayload(content.getBytes());

        try {
            if (!client.isConnected()) {
                client.connect();
            }
            client.setCallback(this);
            client.publish(MQROOT + "/zustand", message);
        } catch (MqttException ex) {
            log.error(ex);
        }

    }

    public void sendTemps(double SOLL_TEMP, double tempSP, double tempVL) {
        sendTemp("soll", SOLL_TEMP);
        sendTemp("speicher", tempSP);
        sendTemp("vl", tempVL);
    }

    private void sendTemp(String topic, double temp) {
        MqttMessage message = new MqttMessage();
        String content = "" + Double.parseDouble(String.format("%3.1f", temp));
        message.setPayload(content.getBytes());

        try {
            if (!client.isConnected()) {
                client.connect();
            }
            client.setCallback(this);
            client.publish(MQROOT + "/" + topic, message);
        } catch (MqttException ex) {
            log.error(ex);
        }

    }

    public static void main(String[] args) {
        MQTTHeizung mqtt = getInstance();
        System.out.println("MQTTHeizung Memory: " + mqtt.getMemory());
        while (true) {
            try {
                Thread.sleep(100);
                mqtt.getHeizen();
            } catch (InterruptedException ex) {
                Logger.getLogger(MQTTHeizung.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
