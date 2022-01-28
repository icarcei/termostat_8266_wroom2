package ro.sun.thermostat;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class Programs {
    private Program[] week;
    private Program[] weekend;
    private int weekenddays;
    public List<Program> weekprogramsList;
    public List<Program> weekendprogramsList;

    public Programs(){
        week = new Program[6];
        weekend = new Program[6];
        for (int i = 0; i < 6; i++){
            week[i]     = new Program(i);
            weekend[i]  = new Program(i);
        }
        weekenddays = 2;
        weekprogramsList = new ArrayList<>();
        weekendprogramsList = new ArrayList<>();
    }

    public Programs(int weekenddays){
        this.weekenddays = weekenddays;
        week = new Program[6];
        weekend = new Program[6];
        for (int i = 0; i < 6; i++){
            week[i]     = new Program(i);
            weekend[i]  = new Program(i);
        }
        weekprogramsList = new ArrayList<>();
        weekendprogramsList = new ArrayList<>();
    }

    public Program getWeekProgram(int programno){
        return week[programno];
    }
    public Program getWeekendProgram(int programno){
        return weekend[programno];
    }

    public void refreshList(){
        weekendprogramsList.clear();
        weekprogramsList.clear();
        for (int j = 0; j < 6; j++){
            weekprogramsList.add(week[j]);
            weekendprogramsList.add(weekend[j]);
        }
    }

    public void load(JSONArray jsonArray){
        for (int i = 0; i < 6 ; i++){
            try{
                JSONArray ja = jsonArray.getJSONArray(i);
                week[i].hour    = ja.getInt(0);
                week[i].minute  = ja.getInt(1);
                week[i].value   = (float)ja.getDouble(2);
                week[i].sensor  = ja.getInt(3);

                JSONArray jja = jsonArray.getJSONArray(i+6);
                weekend[i].hour    = jja.getInt(0);
                weekend[i].minute  = jja.getInt(1);
                weekend[i].value   = (float)jja.getDouble(2);
                weekend[i].sensor  = jja.getInt(3);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            refreshList();
        }

    }

    public JSONArray put() throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i< 6; i++){
            JSONArray ja = new JSONArray();
            ja.put(week[i].hour);
            ja.put(week[i].minute);
            ja.put(week[i].value);
            ja.put(week[i].sensor);
            jsonArray.put(ja);
        }
        for (int i = 0; i< 6; i++){
            JSONArray ja = new JSONArray();
            ja.put(weekend[i].hour);
            ja.put(weekend[i].minute);
            ja.put(weekend[i].value);
            ja.put(weekend[i].sensor);
            jsonArray.put(ja);
        }
        return jsonArray;
    }
}


 class Program {
    public int      hour;
    public int      minute;
    public float    value;
    public int      sensor;
    private int     nr;

    public Program(int nr){this.nr = nr;};
    public Program(int hour, int minute, float value, int sensor){
        this.hour   = hour;
        this.minute = minute;
        this.value  = value;
        this.sensor = sensor;
    }
    public boolean isValid(){
        if(hour == 0 && minute == 0)return false;
        else return true;
    }
    public int getNr(){return  nr;}
}
