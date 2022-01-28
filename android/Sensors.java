package ro.sun.thermostat;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Sensors{
    private String type;
    private Sensor[] sensor;
    private int many;
    public List<Sensor> sensorList;

    public Sensors(String type, int many) {
        this.type = type;
        this.many = many;
        sensor = new Sensor[many];
        for(int j = 0; j < many; j++)
            sensor[j] = new Sensor();
        sensorList = new ArrayList<>();
    }

    public Sensor SensorNo(int i){
        return sensor[i];
    }
    public Sensor SensorWithNumber(int i){
        for (int j =0; j< many; j++){
            if (sensor[j].number == i)
                return sensor[j];
        }
        return sensor[0];
    }
    public Sensor SensorWithId(int[] id){
        for (int i = 0; i < many; i++){
            boolean finded = true;
            if (isSensor(i)){
                for (int j = 0; j<8; j++){
                    finded &= sensor[i].add[j] == id[j];
                }
            }
            if(finded)
                return sensor[i];
        }
        return null;
    }
    public Sensor SensorWithId(Integer[] id){
        for (int i = 0; i < many; i++){
            boolean finded = true;
            if (isSensor(i)){
                for (int j = 0; j<8; j++){
                    finded &= sensor[i].add[j] == id[j];
                }
                if(finded)
                    return sensor[i];
            }

        }
        return null;
    }
    public Sensor SensorWithName(String name){
        for(int i = 0; i < many; i++){
            if(sensor[i].location.equalsIgnoreCase(name))
                return sensor[i];
        }
        return null;
    }

    public int getMany() {
        return many;
    }

    public boolean isSensor(int i) {
        return sensor[i].number > 0? true:false;
    }
    public int firstFreeIdx(){
        for(int j=0; j< many; j++)
            if(!isSensor(j))return j;
        return -1;
    }
    public int firstFreeNumber(int minnr, int maxnr){
        for(int j=minnr; j< maxnr; j++) {
            boolean alocated = false;
            for (int jj = 0; jj < many; jj++){
                if(sensor[jj].number == j) {
                    alocated = true;
                    break;
                }
            }
            if (!alocated)
                return j;
        }
        return maxnr;
    }
    public Sensor firsFreeSensor(){
        int freesensoridx = firstFreeIdx();
        if (freesensoridx!=-1)
            return SensorNo(firstFreeIdx());
        else
            return SensorNo(many-1);
    }
    public void refreshList(){
        sensorList.clear();
        for (int j = 0; j < many; j++){
            if(sensor[j].number > 0)
                sensorList.add(sensor[j]);
        }
    }

    public boolean load(JSONArray jsonArray){
        try{sensorList.clear();
            for (int j = 0; j< jsonArray.length(); j++){
                JSONArray ja = jsonArray.getJSONArray(j);
                sensor[j].number = ja.getInt(0);
                sensor[j].location = ja.getString(1);
                JSONArray jadd = ja.getJSONArray(2);
                for(int jj = 0; jj<8; jj++)
                    sensor[j].add[jj] = jadd.getInt(jj);
                if( ja.length() > 6) {
                    sensor[j].energy = (float) ja.getDouble(3);
                    sensor[j].humidity = ja.getInt(4);
                    sensor[j].value = (float) ja.getDouble(5);
                    sensor[j].alarm = ja.getBoolean(6);
                }
                if(sensor[j].number > 0)
                    sensorList.add(sensor[j]);
            }
            return true;
        }catch (JSONException e){
            return false;
        }
    }

    public boolean loadcomm(JSONArray jsonArray){
        try{sensorList.clear();
            for (int j = 0; j< jsonArray.length(); j++){
                JSONArray ja = jsonArray.getJSONArray(j);
                sensor[j].number = ja.getInt(0);
                sensor[j].location = ja.getString(1);
                sensor[j].value = (float)ja.getDouble(2);
                JSONArray jadd = ja.getJSONArray(3);
                for(int jj = 0; jj<8; jj++)
                    sensor[j].add[jj] = jadd.getInt(jj);

                sensor[j].alarm = new Integer(1).equals(ja.getInt(4));
                if(ja.length()>6) {
                    sensor[j].energy = (float) ja.getDouble(5);
                    sensor[j].humidity = ja.getInt(6);
                }
                if(sensor[j].number > 0)
                    sensorList.add(sensor[j]);
            }
            return true;
        }catch (JSONException e){
            return false;
        }
    }

    public JSONArray put(){
        JSONArray jsonArray = new JSONArray();
        for (int j = 0; j< this.many; j++) {
            JSONArray ja = new JSONArray();
            ja.put(sensor[j].number);
            ja.put(sensor[j].location);
            JSONArray jadd = new JSONArray();
            for (int jj = 0; jj < 8; jj++)
                jadd.put(sensor[j].add[jj]);
            ja.put(jadd);
            //ja.put(sensor[j].energy);
            //ja.put(sensor[j].humidity);
            //ja.put(sensor[j].value);
            //ja.put(sensor[j].alarm?1:0);
            jsonArray.put(ja);
        }
        return jsonArray;
    }
    public JSONArray putmem(){
        JSONArray jsonArray = new JSONArray();
        for (int j = 0; j< this.many; j++) {
            JSONArray ja = new JSONArray();
            ja.put(sensor[j].number);
            ja.put(sensor[j].location);
            JSONArray jadd = new JSONArray();
            for (int jj = 0; jj < 8; jj++)
                jadd.put(sensor[j].add[jj]);
            ja.put(jadd);
            try {
                ja.put(sensor[j].energy);
            }catch (JSONException e){
                e.printStackTrace();
                ja.put(0);
            }
            ja.put(sensor[j].humidity);
            try {
                ja.put(sensor[j].value);
            }catch (JSONException e){
                e.printStackTrace();
                ja.put(0);
            }
            ja.put(sensor[j].alarm);
            jsonArray.put(ja);
        }
        return jsonArray;
    }

    public JSONArray putcomm(){
        return put();
    }

    public String puts(){
        return put().toString();
    }
}

class Sensor{
    public int number = 0;
    public String location = "Unknow";
    public float value = -50;
    public int add[] = {0,0,0,0,0,0,0,0};
    public boolean alarm = false;
    public float energy = 0;
    public int humidity = 0;


    public Sensor(){};
    public Sensor(String location){
        this.location = location;
    }

    public String IntToHex(int j){
        return j<16?"0"+Integer.toHexString(j).toUpperCase():Integer.toHexString(j).toUpperCase();
    }

    public String getadd(){
        return IntToHex(add[0]) + ":" + IntToHex(add[1]) + ":" +
                IntToHex(add[2]) + ":" + IntToHex(add[3]) + ":" +
                IntToHex(add[4]) + ":" + IntToHex(add[5]) + ":" +
                IntToHex(add[6]) + ":" + IntToHex(add[7]) ;

    }

    public void setAdd(Integer[] add) {
        for(int i=0; i<8; i++)
            this.add[i] = add[i];
    }

    public void setadd(String adds){
        byte[] data = Base64.decode(adds, Base64.DEFAULT);
        for (int j=0;j<8;j++){
            add[j] = (int) data[j];
        }
    }


}