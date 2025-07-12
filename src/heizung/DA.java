/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template

https://pi-buch.info/das-kommando-raspi-gpio/
cmd gpio funktionierte auf altem raspi b+


 */
package heizung;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public enum DA {

    SOLARHOT(0), //
    SOLARIMP(1), //
    WWPUMPE(6), //2 gewechselt nach 6 am 15.5.25 wegen pumpen rückschlagventil klärung
    FERNWAERME(3),//
    SOLARHEIZ_IMP(4),//
    SOLARHEIZ_HOTTER(5), // öffnen bringt heisswasser aus Speicher
    FREE(2), //6
    LIVE(10),;

    private int pin;

    private String doit(String par) {
        String s;
        StringBuilder result = new StringBuilder();
        Process p;
        //System.out.println("\ncmd: " + par);
        try {
            p = Runtime.getRuntime().exec(par);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));

            while ((s = br.readLine()) != null) {
                //System.out.println("line: " + s);
                result.append(s);
            }
            p.waitFor();
            //System.out.println("exit: " + p.exitValue());
            p.destroy();
        } catch (IOException | InterruptedException e) {
            System.out.println("catch DA doit() "+e);
        }
        return result.toString();
    }

    private DA(int pin) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {

        }
        this.pin = pin;
        doit("gpio mode " + pin + " out");
        off();

    }

    public int getPin() {
        return pin;
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s.substring(0, 1) + s.substring(1).toLowerCase();
    }

    public void on() {
        doit("gpio write " + pin + " 0");
        //pin.low();
    }

    public void off() {
        doit("gpio write " + pin + " 1");
        //pin.high();

    }

    public boolean isOff() {
        String s = doit("gpio read " + pin);
        boolean result = "1".equals(s);
        //System.out.println("isOff_" + pin + ": >" + s + "<" + result);

        return result;// pin.isHigh();

    }

    public boolean isOn() {
        String s = doit("gpio read " + pin);
        boolean result = "0".equals(s);
        //System.out.println("isOn_" + pin + ": >" + s + "<" + result);
        return result; //pin.isLow();
    }
}
