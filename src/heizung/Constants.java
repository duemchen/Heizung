/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package heizung;

import de.horatio.common.HoraIni;

/**
 *
 * @author duemchen
 */
public class Constants {

    static private double sollVL = HoraIni.LeseIniInt("Regler.ini", "Temperaturen", "VL_Winter", 45, true);


    public static double getVL() {
        return sollVL;
    }


}
