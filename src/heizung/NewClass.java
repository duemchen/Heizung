/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package heizung;

import java.text.DecimalFormat;

/**
 *
 * @author duemchen
 */
public class NewClass {
    public static void main(String[] args) throws InterruptedException {
        
          DecimalFormat myFormatter = new DecimalFormat("###,###.###");
      String output = myFormatter.format(1234567);
        
        System.out.println(output);
        
        
    }
}
