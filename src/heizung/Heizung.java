/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package heizung;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import de.horatio.common.HoraFile;
import de.horatio.common.HoraIni;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.logging.Level;

import java.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.config.Configurator;

import se.hirt.w1.Sensor;
import se.hirt.w1.Sensors;

/**
 *
 * @author duemchen
 */
public class Heizung {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger("heizung");
    private static final GpioController gpio = GpioFactory.getInstance();
    //
    final static String datei = "heizung.ini";
    final static String reglerIni = "regler.ini";
    private static MqttTemperaturen mqttTemperaturen;
    private SolarToSpeicher s2s;
    private SpeicherToWarmwasser s2ww;
    private FernHeizung fernHeiz;
    private SolarHeizung solarHeiz;
    private int lastHour = -1;
    private String MQTTLINK = "duemchen.feste-ip.net:56686";
    private double aussenTemp;
    private double soll;

    /**
     * iii
     */
    public static enum DA {

        SOLARHOT(RaspiPin.GPIO_00), //
        SOLARIMP(RaspiPin.GPIO_01), //
        WWPUMPE(RaspiPin.GPIO_02), //
        FERNWAERME(RaspiPin.GPIO_03),//
        SOLARHEIZ_IMP(RaspiPin.GPIO_04),//
        SOLARHEIZ_HOTTER(RaspiPin.GPIO_05), // öffnen bringt heisswasser aus Speicher
        FREE(RaspiPin.GPIO_06), //
        LIVE(RaspiPin.GPIO_10),;

        private GpioPinDigitalOutput pin;

        private DA(Pin pin) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }

            this.pin = gpio.provisionDigitalOutputPin(pin, "", PinState.HIGH);
        }

        public GpioPinDigitalOutput getPin() {
            return pin;
        }

        @Override
        public String toString() {
            String s = super.toString();
            return s.substring(0, 1) + s.substring(1).toLowerCase();
        }

        public void on() {
            pin.low();
        }

        public boolean isOff() {
            return pin.isHigh();

        }

        public boolean isOn() {
            return pin.isLow();
        }

        public void off() {
            pin.high();

        }

    }

    /**
     * jeder fühler wird aktiviert wenn er beim start gefunden wurde - alle IDs
     * sammeln in Hash, sortieren und speichern - laden *
     */
    public enum TEMP {

        KOLL_VL,
        SP1_OBEN,
        SP1_MITTE,
        WARMWASSER,
        FERNHEIZ_VORLAUF,
        FERNHEIZ_RUECKLAUF,
        FERN_STADT_RUECK;
        private Sensor sensor;
        private Number lastTemp = 0;

        public void setSensor(Sensor sensor) {
            this.sensor = sensor;
        }

        public Number getTemp() {
            if (sensor == null) {
                return 0;
            }
            try {
                lastTemp = sensor.getValue();
                return lastTemp;

            } catch (IOException ex) {
                Logger.getLogger(Heizung.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        public Number getTempLast() {
            return lastTemp;
        }

        @Override
        public String toString() {
            return "TemperaturSensor " + this.name() + ", sensor:" + sensor + ", val:" + lastTemp;
        }

    }

    private static void initTemperatureSensoren() throws IOException {
        System.out.println("hello " + HoraFile.getCanonicalPath(datei));
        Set<se.hirt.w1.Sensor> sensors = Sensors.getSensors();
        System.out.println(String.format("Found %d sensors!", sensors.size()));
        // jeder Sensor wird mit ID in eine Propertydatei eingetragen.
        // dort erfolgt die Zuordnung zu dem konkreten Sensor
        // FERN ID = 28-000000e46c60
        for (Sensor sensor : sensors) {
            System.out.println(String.format("%s(%s):%3.2f%s",
                    sensor.getPhysicalQuantity(), sensor.getID(),
                    sensor.getValue(), sensor.getUnitString()));
            HoraIni.SchreibeIniString(datei, "SensorIDs", sensor.getID(), String.format("%3.2f", sensor.getValue()));

        }

        for (TEMP temp : TEMP.values()) {

            String id = HoraIni.LeseIniString(datei, "Temperaturen", temp.name(), "", true);
            if ("".equals(id)) {
                continue;
            }
            // diese ID in sensoren suchen und verschalten
            System.out.println(id);
            for (Sensor sensor : sensors) {
                //System.out.println(id + " " + sensor.getID());
                if (!id.equalsIgnoreCase(sensor.getID())) {
                    continue;
                }
                temp.setSensor(sensor);
                break;
            }
        }
        // alle angebunden?

        for (TEMP temp : TEMP.values()) {
            System.out.println(temp);
        }

    }

    public Heizung() throws IOException, InterruptedException {
        log.info("Heizung Start");
        //
        MQTTLINK = HoraIni.LeseIniString(datei, "MQTT", "LINK_PORT", MQTTLINK, true);
        MQTTLINK = "tcp://" + MQTTLINK;
        MqttTemperaturen.getInstance(MQTTLINK).sendHallo();
        //
        initTemperatureSensoren();
        s2s = new SolarToSpeicher(TEMP.KOLL_VL, TEMP.SP1_OBEN, DA.SOLARHOT, DA.SOLARIMP);
        s2ww = new SpeicherToWarmwasser(TEMP.SP1_OBEN, TEMP.WARMWASSER, DA.WWPUMPE);
        s2s.setName("solar2Speicher");
        s2ww.setName("speicher2Warmwasser");
        s2s.start();
        s2ww.start();
        fernHeiz = new FernHeizung(TEMP.FERNHEIZ_VORLAUF, TEMP.FERNHEIZ_RUECKLAUF, TEMP.SP1_OBEN, TEMP.FERN_STADT_RUECK, DA.FERNWAERME);

        fernHeiz.setName("fernHeizung");
        fernHeiz.start();
        solarHeiz = new SolarHeizung(TEMP.FERNHEIZ_VORLAUF, TEMP.FERNHEIZ_RUECKLAUF, TEMP.SP1_OBEN, DA.SOLARHEIZ_IMP, DA.SOLARHEIZ_HOTTER);
        solarHeiz.setName("solarHeizung");
        solarHeiz.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    s2s.interrupt();
                    s2ww.interrupt();
                    fernHeiz.interrupt();
                    solarHeiz.interrupt();
                    s2s.join(1000);
                    s2ww.join(1000);
                    fernHeiz.join(1000);
                    solarHeiz.join(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Heizung.class.getName()).log(Level.SEVERE, null, ex);
                }

                for (DA da : DA.values()) {
                    da.off();
                }
                MqttTemperaturen.getInstance(MQTTLINK).sendByebye();
                System.out.println("TschAu.");
                log.info("Heizung Stop.");
                LogManager.shutdown();
                // version 2.3?((LifeCycle) LogManager.getContext()).stop();

            }
        });
        int i = 0;
        int lastMinute = 0;
        while (true) {
            Thread.sleep(2000);
            if (DA.LIVE.isOn()) {
                DA.LIVE.off();
            } else {
                DA.LIVE.on();
            }

//            System.out.println(i + "  " + HoraTime.dateToStr(new Date()));
            for (TEMP temp : TEMP.values()) {
                temp.getTemp();
            }
//            System.out.println("");
            // da testen
            /*
             if (i % 20 == 0) {
             for (DA da : DA.values()) {
             da.on();
             Thread.sleep(50);
             da.off();
             Thread.sleep(100);
             }
             }*/

            Calendar cal = new GregorianCalendar();
            int aktMinute = cal.get(Calendar.MINUTE);
            if (lastMinute != aktMinute) {
                lastMinute = aktMinute;
                try {
                    soll = fernHeiz.getSollwert();
                    MqttTemperaturen.getInstance(MQTTLINK).sendTemps(soll, aussenTemp);
                    MqttTemperaturen.getInstance(MQTTLINK).sendEinzelTemps(soll, aussenTemp);

                } catch (Exception ex) {
                    Logger.getLogger(Heizung.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
            i++;
            if (lastHour == cal.get(Calendar.HOUR_OF_DAY)) {
                continue;
            }
            // wetterabfragen stündlich
            try {

                OpenWeather ow;
                ow = new OpenWeather();
                double lon = 12.89;
                double lat = 53.09;
                ow.setCoord(lon, lat);
                double temp = ow.getTemp();
                fernHeiz.setAussentemp(temp);
                aussenTemp = temp;
                lastHour = cal.get(Calendar.HOUR_OF_DAY);
            } catch (Exception e) {
                System.out.println(e);
            }

        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException {

        Heizung heizung = new Heizung();

    }

}

/*
 TODOS

 ww pegel beobachten mit fernwärme
 danach die zuheizung einpegeln




 praktikanten
 remotesitzung auf pc-acht
 netbeans install

 json temp und DA

 paho reconnect
 speicherung in Tagesdatei minütlich
 grafische darstellungen aus tagesdatei

 ini änderung neu einlesen

 Jeder Regler ein Nachrichtensender.
 Empfänger zeigt jede DA Ändeurng an mit Temperaturen






 regler SpeicherToHeizung
 regler FernwaermeToHeizung




 */
