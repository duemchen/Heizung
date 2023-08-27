/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package heizung;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author duemchen
 */
class SpeicherToWarmwasser extends Thread {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger("soWarm");
    private final Heizung.DA da;
    private final Heizung.TEMP tempSpeicher;
    private final Heizung.TEMP tempWarmwasser;
    //
    private double SP_MIN = 53; // mindesttemp
    private double WW_SOLL = 50; // sollwert ww
    private double HYSTERESE = 1;

    public SpeicherToWarmwasser(Heizung.TEMP tempSpeicher, Heizung.TEMP tempWarmwasser, Heizung.DA da) {
        this.tempSpeicher = tempSpeicher;
        this.tempWarmwasser = tempWarmwasser;
        this.da = da;
    }

    @Override
    public void run() {
        log.info("SpeicherToWarmwasser Start");
        while (true) {
            try {
                control();
            } catch (InterruptedException ex) {
                System.out.println("---stop.");
                System.out.flush();

                Logger.getLogger(SolarToSpeicher.class.getName()).log(Level.SEVERE, null, ex);
                Thread.currentThread().interrupt(); // very important
                break;
            }

        }
        log.info("SpeicherToWarmwasser Stop.");
    }

    /**
     * wwPumpe einschalten, wenn speichertemp > 50 und ww < 40
     * und
     * sp > ww+5
     *
     */
    void control() throws InterruptedException {
        // System.out.println("s2ww sp:" + tempSpeicher.getTempLast() + ", ww:" + tempWarmwasser.getTempLast() + " ");
        boolean heizen = false;
        //if (tempSpeicher.getTempLast().doubleValue() >= SP_MIN) {
        // speicher soll immer mindestens 3 grad heisser als das WW sein
        if (tempSpeicher.getTempLast().doubleValue() >= 3 + tempWarmwasser.getTempLast().doubleValue()) {

            if (tempWarmwasser.getTempLast().doubleValue() < WW_SOLL) {

                heizen = true;
            }
        }
        // Hysterese: Ein erst etwas unter der solltemp, Aus sofort an der solltemp
        if (heizen) {
            if (da.isOff()) {
                double delta = Math.abs(tempWarmwasser.getTempLast().doubleValue() - WW_SOLL);
                if (delta < HYSTERESE) {
                    // noch sehr nah an soll
                    heizen = false;
                    // System.out.println("s2ww delta:" + delta + ", Heizen verzögert ");
                }
            }
        }

        if (heizen) {
            da.on();
        } else {
            da.off();
        }
        Thread.sleep(5000);
    }

}
