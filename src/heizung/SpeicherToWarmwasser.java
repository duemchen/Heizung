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
    private final DA da;
    private final TEMP tempSpeicher;
    private final TEMP tempWarmwasser;
    //es gibt keine mindesttemp im speicher. Immer , wenn 3 grad mehr im Speicher, wird nachgeheizt
    // dann läuft die pumpe aber auch lange.
    // wenn die Fernwärme einsteigt wird irgendwann der speicher zu kalt sein, um zu heizen.
    private static double WW_SOLL = 47; // 50; // sollwert ww
    private final double HYSTERESE = 1;

    public SpeicherToWarmwasser(TEMP tempSpeicher, TEMP tempWarmwasser, DA da) {
        this.setName("SpeicherToWarmWasser");
        this.tempSpeicher = tempSpeicher;
        this.tempWarmwasser = tempWarmwasser;
        this.da = da;
    }
    
    public static void setSoll(int soll){       
        WW_SOLL = soll;
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
        // Hysterese: wieder Ein erst etwas unter der solltemp, Aus sofort an der solltemp
        if (heizen) {
            // da wird hier als Speicher des Istzustandes benutzt.
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
