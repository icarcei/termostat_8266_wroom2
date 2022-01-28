package ro.sun.thermostat;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ThermoDevs {
    private String database;
    private Context context;
    private AsyncResponse delegate;
    private int THERMOSTATINUSE;
    public List<Thermo> thermoList;
    public ThermoDevs(Context context, String database, AsyncResponse asyncResponseWeakReference){
        this.context = context;
        this.database = database;
        this.delegate = asyncResponseWeakReference;
        thermoList = new ArrayList<>();
        Load(database);
    }
    public boolean Load(String file){
        try {
            InputStream is = context.openFileInput(file);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, "UTF-8");
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                if(jsonObject.has("THERMOSTATINUSE")){
                    THERMOSTATINUSE = jsonObject.getInt("THERMOSTATINUSE");
                }
                if (jsonObject.has("THERMOSTATS")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("THERMOSTATS");
                    thermoList.clear();
                    for(int j=0; j< jsonArray.length(); j++){
                        JSONObject joo = jsonArray.getJSONObject(j);
                        thermoList.add(new Thermo(context, joo.getString("FILE"), delegate, 0, true));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(thermoList.size() == 0){
            thermoList.add(new Thermo(context, "thermostat.jj0", delegate, 0, false));
            Save();
            return false;
        }else
            return true;
    }

    protected void Save(){
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("THERMOSTATINUSE", THERMOSTATINUSE);
            JSONArray jsonArray = new JSONArray();
            for(int j=0; j< thermoList.size(); j++){
                JSONObject joo= new JSONObject();
                joo.put("FILE",thermoList.get(j).getMem());
                joo.put("NAME",thermoList.get(j).getName());
                joo.put("MAC",thermoList.get(j).getMac());
                jsonArray.put(joo);
            }
            jsonObject.put("THERMOSTATS", jsonArray);
            FileOutputStream outfile = context.openFileOutput(database, Context.MODE_PRIVATE);
            outfile.write(jsonObject.toString().getBytes());
            outfile.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public int getTHERMOSTATINUSE(){
        return THERMOSTATINUSE;
    }
    public void setTHERMOSTATINUSE(int THERMOSTATINUSE){
        this.THERMOSTATINUSE = THERMOSTATINUSE;
        Save();
    }

    public void getAll(){
        for(Thermo a:thermoList){
            a.reqState();
        }
    }

    public void Add(){
        thermoList.add(new Thermo(context, "thermostat.jj"+getFirstFree(8), delegate, 0, true));
        thermoList.get(thermoList.size()-1).save();
        this.THERMOSTATINUSE = thermoList.size()-1;
        Save();
    }
    public void Delete(int index){
        if(index>=thermoList.size() || thermoList.size() ==1 || index < 0)
            return;
        Thermo thermo = thermoList.get(index);
        thermoList.remove(index);
        thermo = null;
        context.deleteFile("thermostat.jj"+index);
        if(THERMOSTATINUSE>0)
            THERMOSTATINUSE--;
        Save();
        Load(database);
    }
    private int getFirstFree(int max){
        for(int j=0;j<max;j++){
            File f = new File(context.getFilesDir(),"thermostat.jj"+j);
            if(!f.exists())
                return j;
        }
        return thermoList.size();
    }
}
