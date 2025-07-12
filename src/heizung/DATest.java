/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package heizung;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author duemchen
 */
public class DATest {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger("heizung");


    public String doit(String par) {
        String s;
        Process p;
        System.out.println("\ncmd: " + par);
        try {
            p = Runtime.getRuntime().exec(par);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            while ((s = br.readLine()) != null) {
                System.out.println("line: " + s);
            }
            p.waitFor();
            System.out.println("exit: " + p.exitValue());
            p.destroy();
        } catch (IOException | InterruptedException e) {
            System.out.println("heizung.DATest.doit()");
        }
        return "";
    }

    public void daTest() throws InterruptedException {
        //log.info("DATest Start");
        DA live = DA.WWPUMPE;
        while (true) {
            live.on();
            live.isOn();
            live.isOff();
            Thread.sleep(1000);
            live.off();
            live.isOn();
            live.isOff();
            Thread.sleep(1000);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        DATest dat = new DATest();
        dat.doit("");

    }
}
