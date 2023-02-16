/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package heizung;

import de.horatio.common.HoraFile;
import de.horatio.common.HoraIni;
import de.horatio.common.HoraTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 *
 * @author duemchen
 */
public class Heizkurve {
    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger("fern");
    private static double sollAkt = 45;

    public static PolynomialFunction getPolynomialFit(List<Point> pList) {
        PolynomialFunction result = null;
        if (pList == null) {
            return result;
        }
        try {

            final WeightedObservedPoints obs = new WeightedObservedPoints();
            for (Point p : pList) {
                obs.add(p.getX(), p.getY());
            }

            final ParametricUnivariateFunction function = new PolynomialFunction.Parametric();
            // Start fit from initial guesses that are far from the optimal
            // values.
            // final SimpleCurveFitter fitter =
            // SimpleCurveFitter.create(function,
            // new double[] { -1e20, 3e15, -5e25 });
            final SimpleCurveFitter fitter = SimpleCurveFitter.create(function, new double[]{-2e20, 1e15, -1e25});
            // 2e2 ist 2*10^2 = 2*100
            final double[] best = fitter.fit(obs.toList());
            // System.out.println("Parameters: " + best.length);
            // funktion ausgeben
            result = new PolynomialFunction(best);
        } catch (Exception e) {
            // e.printStackTrace();
            System.out.println("PolynomialFunction: " + e);
        }
        return result;
    }

    
    static void setSoll(double soll) {
        sollAkt = soll;
    }

    static double getSoll() {
        return sollAkt;
    }

    public static PolynomialSplineFunction getHeizkurveLin(String inifile) throws JSONException {
        log.info("getHeizkurveLin inidatei: " + HoraFile.getCanonicalPath(inifile));
        String standard = "[{'x':20.0,'y':20.0},{'x':10,'y':35},{'x':0.0,'y':55}]";
        String s = HoraIni.LeseIniString(inifile, "Einstellung", "param", standard, true);
        JSONArray ja = new JSONArray(s);
        JSONArray jas = getSorted(ja);
        log.info(HoraTime.dateToStr(new Date()) + "-----------------------------------------------------------------------");
        log.info("");
        log.info(ja);
        log.info("");
        log.info(jas);
        double[] x = new double[jas.length()];//{0, 50.5, 100, 200};
        double[] y = new double[jas.length()];//{0, 50.9, 200, 220};

        for (int i = 0; i < jas.length(); i++) {
            JSONObject jo = jas.getJSONObject(i);
            log.info(jo.getDouble("x") + ", " + jo.getDouble("y"));
            x[i] = jo.getDouble("x");
            y[i] = jo.getDouble("y");
        }

        LinearInterpolator interp = new LinearInterpolator();
        PolynomialSplineFunction result = interp.interpolate(x, y);

        log.info("Piecewise functions:");
        Arrays.stream(result.getPolynomials()).forEach(System.out::println); 
        log.info("Kurve: Aussen, Soll:");
        
        for (int i = -10; i <= 15; i++) {
            log.info("x: " + i + ",  y:" + result.value(i));
        }
        log.info("");
        return result;
    }

    public static double xxgetSollTempFromKurve(double aussenTemp, String inifile) {

        try {
            PolynomialSplineFunction f = getHeizkurveLin(inifile);
            double result = f.value(aussenTemp);
            return result;
        } catch (JSONException ex) {
            Logger.getLogger(Heizkurve.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 35;
    }

    private static JSONArray getSorted(JSONArray jsonArr) throws JSONException {
        JSONArray sortedJsonArray = new JSONArray();
        List<JSONObject> jsonValues = new ArrayList<JSONObject>();
        for (int i = 0; i < jsonArr.length(); i++) {
            jsonValues.add(jsonArr.getJSONObject(i));
        }
        Collections.sort(jsonValues, new Comparator<JSONObject>() {
            //You can change "Name" with "ID" if you want to sort by ID
            private static final String KEY_NAME = "x";

            @Override
            public int compare(JSONObject a, JSONObject b) {
                Double valA = Double.valueOf(0);
                Double valB = Double.valueOf(0);

                try {                     
                    valA = a.getDouble(KEY_NAME);
                    valB = b.getDouble(KEY_NAME);
                } catch (JSONException e) {
                    //do something
                }
                return valA.compareTo(valB);
                //if you want to change the sort order, simply use the following:
                //return -valA.compareTo(valB);
            }
        });

        for (int i = 0; i < jsonValues.size(); i++) {
            sortedJsonArray.put(jsonValues.get(i));
        }
        return sortedJsonArray;
    }
}
