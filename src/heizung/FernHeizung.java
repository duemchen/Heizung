/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package heizung;

import de.horatio.common.HoraFile;
import de.horatio.common.HoraIni;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.json.JSONException;

/**
 *
 * @author duemchen
 */
class FernHeizung extends Thread {
    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger("fern");
    private static SimpleDateFormat sdhh = new SimpleDateFormat("HH");

    private final Heizung.DA da;
    private final Heizung.TEMP tempVorlauf;
    private final Heizung.TEMP tempRuecklauf;
    private final Heizung.TEMP tempSpeicher;
    private final Heizung.TEMP tempFernRL;
    //
    private double HYSTERESE = 0;//;1;

    private final int PERIODE = 10000;
    private final double VERSTAERKUNG = 1.0;//2;//5;//10; //5  20*delta 5 grad bringen 100% heizung
    //
    private double INTERGALFAKTOR = 0.01;  //
    private double integralAnteil = 25;  //0 solange Abweichung ist, wird hier langsam gegengezogen
    private double lastTemp = 0;
    private double DIFERENTIALFAKTOR = 10;//40;
    //
    private double tempAussen = 11;
    private PolynomialFunction heizkurve = null;
    private PolynomialSplineFunction heizkurveLin = null;
    private double sollWert = 0;
    private int absenkVon;
    private int absenkBis;
    private int absenkGrad;
    private final String HEIZKURVE_INI = "heizkurve.ini";
    private long ageHeizkurveIni = -3;
    private final String REGLER_INI = "regler.ini";
    private long ageReglerIni = -3;

    public FernHeizung(Heizung.TEMP tempVorlauf, Heizung.TEMP tempRuecklauf, Heizung.TEMP tempSpeicher, Heizung.TEMP tempStadtRuecklauf, Heizung.DA da) {
        this.tempVorlauf = tempVorlauf;
        this.tempRuecklauf = tempRuecklauf;
        this.tempSpeicher = tempSpeicher;
        this.tempFernRL = tempStadtRuecklauf;
        this.da = da;

    }

    @Override
    public void run() {
        log.info("FernHeizung Start");
        while (true) {
            try {
                control();
                //sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(SolarToSpeicher.class.getName()).log(Level.SEVERE, null, ex);
                Thread.currentThread().interrupt(); // very important
                break;
            }

        }
        log.info("FernHeizung Stop.");
    }

    /**
     * Je nach Abweichung vom Sollwert wird das Impulsverh??ltnis ge??ndert stark
     * heizen Dauerein in Prozent angeben. 10 stufen zyklus 20..60 Sekunden
     *
     * heizungsunterst??tzung beachten. Wenn dort genug hitze, fernw??rme
     * auslassen.
     *
     *
     * @throws InterruptedException
     *
     */
    void control() throws InterruptedException {
        double soll = Constants.getVL();
        if (ageReglerIni != HoraFile.fileAge(REGLER_INI)) {
            readIni(REGLER_INI);
            ageReglerIni = HoraFile.fileAge(REGLER_INI);
        }
        //jetzt berechnen aus akt. Aussentemp an Hand der Heizkurve
        //linear und so auch richtiger und nachvollziehbarer
        if (ageHeizkurveIni != HoraFile.fileAge(HEIZKURVE_INI)) {
            heizkurveLin = null;
            ageHeizkurveIni = HoraFile.fileAge(HEIZKURVE_INI);
            System.out.println("Neueinlesen "+HEIZKURVE_INI);
        }

        if (heizkurveLin == null) {
            try {
                heizkurveLin = Heizkurve.getHeizkurveLin(HEIZKURVE_INI);
            } catch (JSONException ex) {
                Logger.getLogger(FernHeizung.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (heizkurveLin != null) {
            soll = heizkurveLin.value(tempAussen); // tageszeit
        }

        if (isNachtabsenkungszeit()) {
            //soll = Constants.getVLNacht();
            {
                soll = soll - absenkGrad;
            }
        }

        // R??cklaufbegrenzung. wenn keine W??rme gebraucht wird, fast aus.
        if (tempRuecklauf.getTempLast().doubleValue() > 40) {
            // System.out.println("r??cklauf " + tempRuecklauf.getTempLast().doubleValue());
            soll = soll - absenkGrad;
            // soll = Constants.getVLSommer();
        }
        Heizkurve.setSoll(soll);
        log.info("soll:" + soll);
        da.off();
        if (!HoraIni.LeseIniBool("Regler.ini", "Heizung", "aktiv", true, true)) {
            Thread.sleep(30000);
            return;
        }
        if (isNachtabsenkungszeit()) {
            if (tempAussen > 15) {
                Thread.sleep(5000);
                return;
            }
        } else {
            if (tempAussen > 18) {
                Thread.sleep(5000);
                return;
            }
        }
        if (tempVorlauf.getTempLast().doubleValue() <= 10) {
            // sensor hat versagt
            Thread.sleep(5000);
            return;
        }
        // System.out.println("SollTemp: " + soll + "   Aussen: " + tempAussen);
        // erst wenn die Speicher verbraucht sind. kleine hysterese einbauen
        setSollwert(soll);
        boolean zuHeizen = tempSpeicher.getTempLast().doubleValue() < soll;
        //isSolar=!zuHeizen;
        //zuHeizen = true;
        if (!zuHeizen) {
            //System.out.println(" -> Fernw??rme AUS, Speicher ist heiss genug: " + tempSpeicher.getTempLast());
            //anheizen=true;
            Thread.sleep(60000);
            return;
        }

        if (tempFernRL.getTempLast().doubleValue() > 40) {
            // Abschalten, wenn offenbar nix gebraucht
            Thread.sleep(10000);
            return;
        }
        if (tempFernRL.getTempLast().doubleValue() > tempVorlauf.getTempLast().doubleValue()) {
            // Abschalten, wenn StadtR??cklauf heisser als HeizungsVorlauf
            // Thread.sleep(10000);
            // return;
        }

        double ist = tempVorlauf.getTempLast().doubleValue();
        double delta = soll - ist; // wenn positiv heizen
        integralAnteil = integralAnteil + delta * INTERGALFAKTOR;
        if (Math.abs(delta) < HYSTERESE) {
            Thread.sleep(5000);
            return;
        }
//        if (delta > 0) {
//            Thread.sleep(5000);
//            return;
//        }
        // heizen mit einer impulsweite.  je weiter weg, um so mehr
        // 10s  1+9  3+7   8+2
        // delta = Math.abs(delta);
        // bei 5 grd 100%
        double proz = (delta * VERSTAERKUNG);
        double prozAnteil = proz;
        proz += integralAnteil; // um den festen anteil erh??hen (n??tige Grundlast auch bei genau der solltemp)
        double diff = ist - lastTemp; // 40 - 50 = -10  starker (pos) anstieg, also weniger heizen
        lastTemp = ist;
        double diffAnteil = diff * DIFERENTIALFAKTOR;
        proz -= diffAnteil;
        proz = Math.min(100, proz);
        proz = Math.max(0, proz);

        int on = (int) ((proz / 100) * PERIODE);
        int off = (int) (((100 - proz) / 100) * PERIODE);
        // System.out.println("vorlauf:" + tempVorlauf.getTempLast() + " C / " + tempRuecklauf.getTempLast() + " C, gesamt:" + proz + ", p:" + prozAnteil + ", i:" + integralAnteil + ", d:" + diffAnteil + ", on:" + on + ",  off:" + off);
        if (on > 500) { //Kontaktschonung
            da.on();
        }
        Thread.sleep(on);
        if (off > 500) {
            da.off();
        }
        Thread.sleep(off);
    }

    void setAussentemp(double temp) {
        tempAussen = temp;
    }

    double getSollwert() {
        return sollWert;
    }

    private void setSollwert(double soll) {
        sollWert = soll;
    }

    private boolean isNachtabsenkungszeit() {
        int hh = Integer.parseInt(sdhh.format(new Date()));
        return (hh >= absenkVon) || (hh < absenkBis); // 22:00 bis 5:59
    }

    void readIni(String reglerIni) {
        absenkVon = HoraIni.LeseIniInt(reglerIni, "Absenkung", "von", 22, true);
        absenkBis = HoraIni.LeseIniInt(reglerIni, "Absenkung", "bis", 6, true);
        absenkGrad = HoraIni.LeseIniInt(reglerIni, "Absenkung", "grad", 5, true);
    }

}
