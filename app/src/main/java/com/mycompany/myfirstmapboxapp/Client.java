package com.mycompany.myfirstmapboxapp;

import android.content.Context;

import java.io.*;
import java.util.*;

/**
 * A class for searching through our data
 * @author Willis Roberts, roberws
 *
 */
public class Client {
    private double longitude;
    private double latitude;
    private  ArrayList<String> title;
    private ArrayList<Double> lon;
    private ArrayList<Double> lat;
    private ArrayList<String> snippet;
    private double range;
    private ArrayList<Double> longResults;
    private ArrayList<Double> latResults;
    private Context asset;
    private RangeSearch lonSearch;
    private RangeSearch latSearch;
    private ArrayList<String> titles;
    private ArrayList<String> snippets;
    private int check;

    /**
     * A constructor to initialize the variables
     * @param c Context
     * @param range the range around the user to check for crimes
     */
    public Client(Context c, double range){
        title = new ArrayList<String>();
        lon = new ArrayList<Double>();
        lat = new ArrayList<Double>();
        snippet = new ArrayList<>();
        lonSearch = new RangeSearch();
        latSearch = new RangeSearch();
        this.range = range;
        asset = c;
        check = 0;


    }

    /**
     * A method for getting the user's longitude
     * @return longitude
     */
    public double getLong(){ //return longitude of user
        return longitude;
    }

    /**
     * A method for getting the user's latitude
     * @return latitude
     */
    public double getLat(){ //return latitude of user
        return latitude;
    }

    /**
     * A method for getting the range around the user to use
     * @return the range
     */
    public double getRange(){
        return range;
    }
    /**
     * A method for changing the range
     * @param r the value to change the range to
     */
    public void setRange(double r){
        range = r;
    }

    /**
     * A method for reading the values from the csv crime file and putting them in their respective variables
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void readFile() throws FileNotFoundException, IOException{
        InputStream f = asset.getAssets().open("updated_Crimes_2012-2015.txt");
        BufferedReader in = new BufferedReader(new InputStreamReader(f)); //read in the data from the .txt file
        String str;
        String[] split;
        int i=0;
        while ((str = in.readLine()) !=null) {
            split = str.split(",");
            snippet.add(i, "time : " + split[1] + "\n" + "date : " + split[0]);
            title.add(i,split[2]);
            lon.add(i,Double.parseDouble(split[3]));
            lat.add(i, Double.parseDouble(split[4]));
            lonSearch.put(lon.get(i), (double) i); //fill the lon search tree
            latSearch.put(lat.get(i), (double) i); //fill the lat search tree
            i++;
        }
        in.close();
    }


    public void setLatLon(double lat, double lon){
        this.latitude = lat;
        this.longitude = lon;
        longResults = new ArrayList<Double>();
        latResults = new ArrayList<Double>();
        titles = new ArrayList<String>();
        snippets = new ArrayList<String>();
    }

    /**
     * A method which runs the range search algorithm on this data to find all the coordinates of crimes within the range, and returns true if there are crimes in the range
     */
    public void Search(){
        double r1long = longitude-range, r2long = longitude+range; //range from their location where we look for crimes
        double r1lat = latitude-range, r2lat = latitude+range; //same as above
        check=0;
        for(Double d : lonSearch.range(r1long, r2long)){
            for(Double d2 : latSearch.range(r1lat,r2lat)) {
                if (d2.equals(lat.get(lon.indexOf(d)))) {
                    check++; // return true if there are crimes within the range
                    longResults.add(d);
                    latResults.add(d2);
                    titles.add(title.get(lon.indexOf(d)));
                    snippets.add(snippet.get(lon.indexOf(d)));
                }
            }
        }

    }

    /**
     * Gets the list of longitudes of crimes in the range
     * @return longitudes in the range
     */
    public ArrayList<Double> getLongInRange(){ //get the longitudes of crimes within the range
        return longResults;
    }
    /**
     * Gets the list of latitudes of crimes in the range
     * @return latitudes in the range
     */
    public ArrayList<Double> getLatInRange(){ //get the latitudes of crimes within the range
        return latResults;
    }

    public ArrayList<String> getTitles(){ return titles; }

    public ArrayList<String> getSnippets(){ return snippets; }

    public boolean getCheck(){ return check >= 5; }
}
