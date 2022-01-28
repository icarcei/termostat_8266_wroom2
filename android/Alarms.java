package ro.sun.thermostat;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;


public class Alarms {
    private Alarm[] alarm;
    public List<Alarm> alarmList;

    public Alarms(){
        alarm = new Alarm[8];
        for (int i =0; i<8;i++){
            alarm[i] = new Alarm();
        }
        alarmList = new ArrayList<>();
    }
    public Alarm getAlarm(int alarmno){
        return alarm[alarmno];
    }
    public void refreshAlarmList(){
        alarmList.clear();
        for (int i=0; i<8; i++){
            alarmList.add(alarm[i]);
        }
    }
    public void load(JSONArray jsonArray){
        alarmList.clear();
        for(int i = 0; i< 8; i++){
            try {
                JSONArray ja = jsonArray.getJSONArray(i);
                alarm[i].sensor = ja.getInt(0);
                alarm[i].value  = (float)ja.getDouble(1);
                alarm[i].type   = ja.getInt(2);
                alarm[i].repeat = ja.getInt(3);
                alarm[i].message= ja.getString(4);
                if(ja.length() > 5 )
                    alarm[i].active = ja.getInt(5) > 0;

            } catch (JSONException e) {
                e.printStackTrace();
            }
            alarmList.add(alarm[i]);
        }
    }
    public JSONArray put() {
        JSONArray jsonArray = new JSONArray();
        for (Alarm a : alarm){
            JSONArray ja = new JSONArray();
            ja.put(a.sensor);
            try {
                ja.put(a.value);
            } catch (JSONException e) {
                e.printStackTrace();
                ja.put(0);
            }
            ja.put(a.type);
            ja.put(a.repeat);
            ja.put(a.message);
            jsonArray.put(ja);
        }
        return  jsonArray;
    }
    public JSONArray putmem() {
        JSONArray jsonArray = new JSONArray();
        for (Alarm a : alarm){
            JSONArray ja = new JSONArray();
            ja.put(a.sensor);
            try {
                ja.put(a.value);
            } catch (JSONException e) {
                e.printStackTrace();
                ja.put(0);
            }
            ja.put(a.type);
            ja.put(a.repeat);
            ja.put(a.message);
            ja.put(a.active);
            jsonArray.put(ja);
        }
        return  jsonArray;
    }
}

class Alarm {
    public int      sensor;
    public float    value;
    public int      type;
    public int      repeat;
    public String   message;
    public boolean active;

    public Alarm(){type =-1;}
}
