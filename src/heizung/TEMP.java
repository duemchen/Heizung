/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package heizung;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.hirt.w1.Sensor;

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
