/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package heizung;

import com.pi4j.io.gpio.PinState;
import de.horatio.common.HoraTime;
import static heizung.MQTTHeizung.getInstance;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author duemchen
 */
class SolarHeizung extends Thread {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger("solar");
    private MQTTHeizung mqtt;
    //
    private TEMP temp;
    private DA impuls;
    private DA hotter;  //on öffnet speicher, sodass heisswasser kommt. 
    //
    private final TEMP tempVL;
    private final TEMP tempRL;
    private final TEMP tempSP;
    //
    private double SOLL_TEMP = 45.0;
    private double MINDEST_DELTA_TEMP = 4.0;  // speicher muss wärmer als rücklauf sein,  sonst abschalten.
    private double HYSTERESE = 1.0;
    //
    private final int PERIODE = 10000;
    private int minuteLast = 0;

    /**
     * Der Rücklauf wird dann in den Speicher umgeleitet, sodass das heisse
     * Speicherwasser den Vorlauf vorheizt.
     *
     */
    SolarHeizung(TEMP vorlauf, TEMP ruecklauf, TEMP speicher, DA imp, DA hotter) {
        this.setName("SolarHeizung");
        this.tempVL = vorlauf;
        this.tempRL = ruecklauf;
        this.tempSP = speicher;
        this.impuls = imp;
        this.hotter = hotter;
        //
        this.mqtt = getInstance();
    }

    @Override
    public void run() {
        log.info("SolarHeizung Start");
        while (true) {
            try {
                control();
                //System.out.println("...");
            } catch (InterruptedException ex) {
                Logger.getLogger(SolarToSpeicher.class.getName()).log(Level.SEVERE, null, ex);
                Thread.currentThread().interrupt(); // very important
                break;
            }

        }
        System.out.println("...stop.");
        log.info("SolarHeizung Stop.");

    }

    /**
     * heizungsunterstützung. speicher heiss, dann damit heizen. muss heisser
     * als rücklauf sein! So wird alles ausgenutzt Was fehlt, übernimmt
     * fernwärmeregler Wenn so heiss wie rücklauf, aus. Unnötige durchleitung
     * vermeiden
     *
     * voll auf, aber nicht höher als gewünschte vorlauftemp.
     *
     * im sommerbetrieb (und das ist ja der Haupteinsatz) soll tagsüber nicht
     * geheizt, aber nachts geheizt werden. Abschaltung Heizung - über set
     * Sollwert als Ausnahme von der Heizkurve ? - über kommando mqtt und
     * iobroker Zeitsteuerung - Solar läuft unabhängig vom FernwärmeThread. Kann
     * also auch den Sollwert problemlos übeerchreiben für sich selbst!
     *
     *
     * @throws InterruptedException
     */
    void control() throws InterruptedException {
        double proz;
        double rl = tempRL.getTempLast().doubleValue();
        double sp = tempSP.getTempLast().doubleValue();
        SOLL_TEMP = Heizkurve.getSoll();
        if (mqtt.getHeizen()) {
            // speicher muss etwas wärmer sein.
            if ((sp - rl) > MINDEST_DELTA_TEMP) {
                // heizungsunterstützung. Heizen. Regeln.
                double ist = tempVL.getTempLast().doubleValue();
                //SOLL_TEMP = Heizkurve.getSoll();
                double delta = SOLL_TEMP - ist;
                boolean heizen = delta > 0;
                if (heizen) {
                    this.hotter.on();
                } else {
                    this.hotter.off();
                }
                if (Math.abs(delta) > HYSTERESE) {
                    proz = 10; // kurzer impuls 10% von 10 sek
                } else {
                    proz = 0; //nichts tun
                }

            } else {
                // abschalten wenn Speicher zu kalt
                this.hotter.off();
                proz = 100;
            }

        } else {
            // abschalten wenn mqtt "setAktiv off"
            this.hotter.off();
            proz = 100;

        }

        proz = Math.min(100, proz);
        proz = Math.max(0, proz);

        int on = (int) ((proz / 100) * PERIODE);
        int off = (int) (((100 - proz) / 100) * PERIODE);
        //String state = this.hotter.getPin().getState().getName();
        log.info("Solarhzg on/off:"+on+"/"+off+", proz:" + proz + " , vL:" + tempVL.getTempLast() + ", rL:" + tempRL.getTempLast() + ", sp:" + tempSP.getTempLast() + ", soll:" + SOLL_TEMP);
        //jede Minute. Immer wenn neue Minute
        if (isNewMinute()) {
            mqtt.sendTemps(SOLL_TEMP, tempSP.getTempLast().doubleValue(), tempVL.getTempLast().doubleValue());
        }
        if (on > 500) { //Kontaktschonung
            this.impuls.on();
        }
        Thread.sleep(on);
        if (off > 500) {
            this.impuls.off();
        }
        Thread.sleep(off);

    }

    private boolean isNewMinute() {
        int minute = 0;
        boolean result = false;
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        minute = cal.get(Calendar.MINUTE);
        if (minute != minuteLast) {
            minuteLast = minute;
            result = true;
        }
        return result;
    }
    

}
