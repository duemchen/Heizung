/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package heizung;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author krause
 */
public class OpenWeather {

    private double lat;
    private double lon;

//    public static void xmain(String[] args) throws JSONException, MalformedURLException, IOException {
//
//        // D:\Programme\java\netbeansproject\OpenWeather\src\openweather
//        String s = ("{\"coord\":\n"
//                + "{\"lon\":145.77,\"lat\":-16.92},\n"
//                + "\"weather\":[{\"id\":803,\"main\":\"Clouds\",\"description\":\"broken clouds\",\"icon\":\"04n\"}],\n"
//                + "\"base\":\"cmc stations\",\n"
//                + "\"main\":{\"temp\":293.25,\"pressure\":1019,\"humidity\":83,\"temp_min\":289.82,\"temp_max\":295.37},\n"
//                + "\"wind\":{\"speed\":5.1,\"deg\":150},\n"
//                + "\"clouds\":{\"all\":75},\n"
//                + "\"rain\":{\"3h\":3},\n"
//                + "\"dt\":1435658272,\n"
//                + "\"sys\":{\"type\":1,\"id\":8166,\"message\":0.0166,\"country\":\"AU\",\"sunrise\":1435610796,\"sunset\":1435650870},\n"
//                + "\"id\":2172797,\n"
//                + "\"name\":\"Cairns\",\n"
//                + "\"cod\":200}");
//        JSONObject json = new JSONObject(s);
//
//
//
//        String city = (String) json.get("name");
//        System.out.println("Stadt: " + city);
//        int dt = (int) json.getInt("dt");
//        System.out.println("Time of data calculation, unix, UTC: " + dt);
//
//        JSONObject jsonCoord = json.getJSONObject("coord");
//        System.out.printf("City geo location, longitude: ");
//        System.out.println(jsonCoord.getString("lon"));
//        System.out.printf("City geo location, latitude: ");
//        System.out.println(jsonCoord.getString("lat"));
//        JSONObject jsonWind = json.getJSONObject("wind");
//        System.out.printf("Windgeschwindigkeit: ");
//        System.out.println(jsonWind.getString("speed"));
//        System.out.printf("Windrichtung: ");
//        System.out.println(jsonWind.getString("deg"));
//        Scanner scanner = new Scanner(System.in);
//        System.out.print("Bitte geben Sie den Breitengrad ein: ");
//        String breitgrad = scanner.next();
//        System.out.print("Bitte geben Sie den Längengrad ein: ");
//        String langgrad = scanner.next();
//        String geourl1 = ("http://api.openweathermap.org/data/2.5/weather?lat=");
//        String georul2 = ("&lon=");
//        String geourl3 = ("&appid=aaa67fe700a77d94eb3d115299f40e93&lang=de");
//
//        StringBuilder urlstr = new StringBuilder(geourl1);
//        urlstr.append(breitgrad);
//        urlstr.append(georul2);
//        urlstr.append(langgrad);
//        urlstr.append(geourl3);
//        System.out.println(urlstr);
//
//        String url2 = urlstr.toString();
    // http://api.openweathermap.org/data/2.5/weather?q=London&appid=aaa67fe700a77d94eb3d115299f40e93
    // http://api.openweathermap.org/data/2.5/forecast/daily?id=aaa67fe700a77d94eb3d115299f40e93&lang=de
    // http://api.openweathermap.org/data/2.5/box/city?bbox=13,41,52,52,10&cluster=yes&appid=aaa67fe700a77d94eb3d115299f40e93&lang=de
    // http://api.openweathermap.org/data/2.5/weather?lat=52.52&lon=13.41&appid=aaa67fe700a77d94eb3d115299f40e93
//        URL url = new URL(url2);
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection.setRequestMethod("GET");
//        connection.connect();
//
//        InputStream stream = connection.getInputStream();
//        // System.out.println(stream);
//        // read the contents using an InputStreamReader
//
//        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
//        StringBuilder result = new StringBuilder();
//        String line;
//        while ((line = reader.readLine()) != null) {
//            result.append(line);
//        }
//        String str;
//        str = result.toString();
//
//        System.out.println(str);
//
//        JSONObject json2 = new JSONObject(str);
//        String city2 = (String) json2.get("name");
//        System.out.println("Stadt: " + city2);
//        System.out.println("_____________________");
//        int dt = (int) json.getInt("dt");
//        System.out.println("Time of data calculation, unix, UTC: " + dt);
//
//        JSONObject jsonCoord = json.getJSONObject("coord");
//        System.out.printf("City geo location, longitude: ");
//        System.out.println(jsonCoord.getString("lon"));
//        System.out.printf("City geo location, latitude: ");
//        System.out.println(jsonCoord.getString("lat"));
//        JSONObject json2Wind = json2.getJSONObject("wind");
//        System.out.printf("Windgeschwindigkeit: ");
//        System.out.printf(json2Wind.getString("speed"));
//        System.out.println(" Meter pro Sekunde");
//        System.out.printf("Windrichtung: ");
//        System.out.printf(json2Wind.getString("deg"));
//        System.out.printf(" = ");
//        double windrichtung;
//        windrichtung = Double.parseDouble(json2Wind.getString("deg"));
//
//        if (windrichtung > 348.75 || windrichtung <= 11.25) {
//            System.out.println("Nord");
//        }
//        if (windrichtung > 11.25 && windrichtung <= 33.75) {
//            System.out.println("Nord-Nord-Ost");
//        }
//        if (windrichtung > 33.75 && windrichtung <= 56.25) {
//            System.out.println("Nord-Ost");
//        }
//        if (windrichtung > 56.25 && windrichtung <= 78.75) {
//            System.out.println("Ost-Nord-Ost");
//        }
//        if (windrichtung > 78.75 && windrichtung <= 101.25) {
//            System.out.println("Ost");
//        }
//        if (windrichtung > 101.25 && windrichtung <= 123.75) {
//            System.out.println("Ost-Süd-Ost");
//        }
//        if (windrichtung > 123.75 && windrichtung <= 146.25) {
//            System.out.println("Süd-Ost");
//        }
//        if (windrichtung > 146.25 && windrichtung <= 168.75) {
//            System.out.println("Süd-Süd-Ost");
//        }
//        if (windrichtung > 168.75 && windrichtung <= 191.25) {
//            System.out.println("Süd");
//        }
//        if (windrichtung > 191.25 && windrichtung <= 213.75) {
//            System.out.println("Süd-Süd-West");
//        }
//        if (windrichtung > 213.75 && windrichtung <= 236.25) {
//            System.out.println("Süd-West");
//        }
//        if (windrichtung > 236.25 && windrichtung <= 258.75) {
//            System.out.println("West-Süd-West");
//        }
//        if (windrichtung > 258.75 && windrichtung <= 281.25) {
//            System.out.println("West");
//        }
//        if (windrichtung > 281.25 && windrichtung <= 303.75) {
//            System.out.println("West-Nord-West");
//        }
//        if (windrichtung > 303.75 && windrichtung <= 326.25) {
//            System.out.println("West");
//        }
//        if (windrichtung > 326.25 && windrichtung <= 348.75) {
//            System.out.println("Nord-West");
//        }
//        if (windrichtung > 348.75 && windrichtung <= 360 || windrichtung >= 0 && windrichtung <= 11.25) {
//            System.out.println("Nord");
//        }
//
//        JSONArray results = json2.getJSONArray("weather");
//        JSONObject first = results.getJSONObject(0);
////        System.out.println(first);
////        Integer id = first.getInt("id");
////        System.out.println("Wetter ID: " + id);
//
//        String main = first.getString("main");
//
//        String regen = "Regen";
//        if ("Rain".equals(main)) {
//            main = regen;
//        }
//
//        String wolken = "Wolkig";
//        if ("Clouds".equals(main)) {
//            main = wolken;
//        }
//
//        System.out.println("Himmel: " + main);
//        String description = first.getString("description");
//        System.out.println("Nähere Beschreibung: " + description);
//
//        JSONObject jsonCoord = json2.getJSONObject("coord");
//        System.out.printf("City Geo Location, Längengrad: ");
//        System.out.println(jsonCoord.getString("lon"));
//        System.out.printf("City Geo Location, Breitengrad: ");
//        System.out.println(jsonCoord.getString("lat"));
//
//    }
//
//    private static JSONObject loadJSONArray(String datajson) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
    public void setCoord(double lon, double lat) {
        this.lat = lat;
        this.lon = lon;

    }

    /**
     *
     * @return @throws MalformedURLException
     * @throws IOException
     * @throws JSONException
     */
    public double getWind() throws MalformedURLException, IOException, JSONException {
        double result2 = 0;

        String geourl1 = ("http://api.openweathermap.org/data/2.5/weather?lat=");
        String georul2 = ("&lon=");
        //String geourl3 = ("&appid=aaa67fe700a77d94eb3d115299f40e93&lang=de&units=Metric&temperature.unit=Celsius,");
        String geourl3 = ("&appid=aaa67fe700a77d94eb3d115299f40e93&lang=de&units=Metric");

        StringBuilder urlstr = new StringBuilder(geourl1);
        urlstr.append(lat);
        urlstr.append(georul2);
        urlstr.append(lon);
        urlstr.append(geourl3);
//        System.out.println(urlstr);

        String url2 = urlstr.toString();

        URL url = new URL(url2);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        InputStream stream = connection.getInputStream();
        // System.out.println(stream);
        // read the contents using an InputStreamReader

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        String str;
        str = result.toString();

        System.out.println(str);
        JSONObject json2 = new JSONObject(str);

        JSONObject json2Wind = json2.getJSONObject("wind");
        result2 = json2Wind.getDouble("speed");
        // System.out.println("wind:" + result2);
        return result2;

    }

    /**
     *
     * @return @throws MalformedURLException
     * @throws IOException
     * @throws JSONException
     */
    public double getTemp() throws MalformedURLException, IOException, JSONException {
        double result = 0;

        String geourl1 = ("http://api.openweathermap.org/data/2.5/weather?lat=");
        String georul2 = ("&lon=");
        //String geourl3 = ("&appid=aaa67fe700a77d94eb3d115299f40e93&lang=de&units=Metric&temperature.unit=Celsius,");
        String geourl3 = ("&appid=aaa67fe700a77d94eb3d115299f40e93&lang=de&units=Metric");

        StringBuilder urlstr = new StringBuilder(geourl1);
        urlstr.append(lat);
        urlstr.append(georul2);
        urlstr.append(lon);
        urlstr.append(geourl3);
//        System.out.println(urlstr);

        String url2 = urlstr.toString();

        URL url = new URL(url2);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        InputStream stream = connection.getInputStream();
        // System.out.println(stream);
        // read the contents using an InputStreamReader

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String str;
        str = sb.toString();

//        System.out.println(str);
        JSONObject json2 = new JSONObject(str);
        JSONObject jsonT = json2.getJSONObject("main");
        result = jsonT.getDouble("temp");
        //  System.out.println("Temp:" + result);
        return result;

    }

    /**
     *
     * @param args
     * @throws JSONException
     * @throws MalformedURLException
     * @throws IOException
     */
    @SuppressWarnings("empty-statement")
    public static void main(String[] args) throws JSONException, MalformedURLException, IOException {

        OpenWeather ow;
        ow = new OpenWeather();
        double lon = 12.89;
        double lat = 53.09;
        ow.setCoord(lon, lat);
        System.out.println("Windgeschwindigkeit: " + ow.getWind());
        //System.out.println("Temperatur: " + ow.getTemp());

    }
}
