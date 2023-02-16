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
class SolarToSpeicher extends Thread {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger("soSpei");
    //
    private double KOLL_MINDEST = 50; // erst dann beginnt das Laden des Speichers  
    private double DELTA = 5; // Sollwert. höher als die Speichertemp -> laden    
    private double HYSTERESE = 1;  // schieber nicht ändern. 
    //
    private final Heizung.TEMP tempSp; // temp im Speicher
    private final Heizung.TEMP tempKoll; // Temp vom Kollektor kommend
    private final Heizung.DA hotter; // Richtung heisser = mehr Durchfluss zum Speicher (sonst kälter)
    private final Heizung.DA impuls; // schieber ein stück rücken.
    //
    private final long IMPULS_MS = 1000; // schieber ansteuerzeit
    private final long SLEEP_MS = 10000; // Pause dazwischen
    //

    SolarToSpeicher(Heizung.TEMP tempKoll, Heizung.TEMP tempSp, Heizung.DA hot, Heizung.DA impuls) {
        this.tempKoll = tempKoll;
        this.tempSp = tempSp;
        this.hotter = hot;
        this.impuls = impuls;

    }

    // stellventil einige sekunden in die richtung. thread starten warten ende
    // endezeit berechnen und in eigenem thread ausschalten lassen.
    // thread läuft immer. wenn zeit in zukunft an, sonst aus.
    // 100% = 10sek  1% =1 sek  sleep(x*10s) sleep((1-x)*10s)  0 kein impuls
    @Override
    public void run() {
        log.info("SolarToSpeicher Start");
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
        log.info("SolarToSpeicher Stop.");
    }

    /**
     * einfacher IRegler Schieber in etappen öffnen und schliessen Schieber
     * öffnen wenn Wärme kommt. temperatur immer 10 grad höher als spTemp halten
     *
     * gleitend mit der erhöhten SPTemperatur mitziehen
     *
     * NOT: wenn solar > als Grenzwert Auf.
     *
     *
     * @throws InterruptedException
     */
    void control() throws InterruptedException {
        double delta = tempKoll.getTempLast().floatValue() - tempSp.getTempLast().floatValue();
        // System.out.println("koll:" + tempKoll.getTempLast().floatValue() + ", sp:" + tempSp.getTempLast().floatValue() + ", deltaSolar:" + delta);
        // temp differenz zum Laden des Speichers 
        boolean doOeffnen = (delta > DELTA); //solar powered-> oeffnen, sonst schliessen
        double abweich = (int) (delta - DELTA);
        abweich = Math.abs(abweich);
        boolean doImpuls = abweich > HYSTERESE;
        // Richtung einstellen
        if (doOeffnen) {
            hotter.on();
        } else {
            hotter.off();
        }
        // Impuls
        if (doImpuls) {
            impuls.on();
            // je nach Abweichung stärker ausregeln
            int openTimeMS = (int) (IMPULS_MS * abweich);
            Thread.sleep(openTimeMS);
            //CANDO wenn weit weg, durchstarten. Nicht ausschalten wenn abweichung zu gross
            impuls.off();

        } else {
            // System.out.println("s2s delta:" + delta + ", Impuls unterdrückt.");
        }

        Thread.sleep(SLEEP_MS);

    }

}
