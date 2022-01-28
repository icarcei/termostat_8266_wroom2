package ro.sun.thermostat;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Thermo implements Serializable {

    private Context     context;
    private WeakReference<AsyncResponse> delegate;
    private Timer       timer;
    private TimerTask   timerTask;
    private boolean     waitingforanswer;
    private boolean     waitforrequest;

    private Sensors     radio;
    private Sensors     wired;
    private Programs    programs;
    private Alarms      alarms;
    private int[]       numberofsensors;
    //params
    private String      mac="";
    private String      key="";
    private String      name;
    private String      http_username="";
    private String      http_password="";
    private String      emails="";
    private String      bonjourserver="ip.of.your.bonjour.server";
    private int         bonjourport=55555;
    private int         defaultsensor=1;
    private float       defaultvalue=22.0f;
    private int         timeZone=2;
    private int         dayLight=1;
    private long        actTime;
    private int         weekenddays=2;
    //local ip, local port
    private String      localip = "127.0.0.2";
    private int         localport = 4321;

    private boolean     uselocalap = false;
    private String      localipap ="192.168.4.1";
    //state
    private int         state;
    private float       value;
    private float       valueset;
    private int         hygro;
    private int         hygrot;
    private int         sensorused;
    private int         program_number_on_forced;
    private int         program_number;
    private int         previos_state;
    private boolean     energized;
    private boolean     alarm;
    private boolean     radioalarm;
    private boolean     wiredalarm;
    private boolean     commalarm;
    private int         repeat_alarm_sensors;

    private long        timeset;
    private long        timerefresh;
    public int          whatyouarelooking;

    private boolean     local;
    private boolean     complet;
    private int         act_speed;
    private int         desired_speed;
    private String      mem;

    public Thermo(Context context, String file, AsyncResponse asyncResponseWeakReference, int whatareyoulooking, boolean local) {
        radio                   = new Sensors("RADIO", 6);
        wired                   = new Sensors("WIRED", 4);
        programs                = new Programs();
        alarms                  = new Alarms();
        name                    = "No Name";
        emails                  = "thermostat@sun.com";
        repeat_alarm_sensors    = 15;
        this.context            = context;
        this.whatyouarelooking  = whatareyoulooking;
        mem                     = file;
        this.local              = local;
        waitforrequest          = false;
        if(!load(file)){

        }
        this.delegate = new WeakReference<>(asyncResponseWeakReference);
    }
    public Thermo(Context context, String file, AsyncResponse asyncResponseWeakReference, int whatareyoulooking ) {
        this(context, file, asyncResponseWeakReference, whatareyoulooking, true);
    }
    public void startTimer() {
        //set a new Timer
        timer = new Timer();
        // initialize Timer Task
        initializeTimerTask();
        timer.schedule(timerTask, 10000, 20000);
    }
    public void stoptimertask(){
        if(timer != null){
            timer.cancel();
            timer = null;
        }
    }
    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if((waitingforanswer) && (System.currentTimeMillis() - timeset > 5000)){
                    waitingforanswer = false;
                    commalarm = true;
                    if(local){
                        local = false;
                    }
                }else{
                    commalarm = false;
                    switch (whatyouarelooking) {
                        case 2: //req wired
                            reqWired();
                            while (waitingforanswer);
                        case 0://req State
                            reqState();
                            break;
                        case 1: //req radio
                            reqRadio();
                            while (waitingforanswer);
                            reqState();

                    }
                }
            }
        };
    }
    public boolean load() {
        return load(mem);
    }
    public boolean load(String file){
        try{
            InputStream is = context.openFileInput(file);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer,"UTF-8");
            try{
                JSONObject jsonObject = new JSONObject(jsonString);
                if(jsonObject.has("RADIO"))
                    radio.load(jsonObject.getJSONArray("RADIO"));
                if(jsonObject.has("WIRED"))
                    wired.load(jsonObject.getJSONArray("WIRED"));
                if(jsonObject.has("PARAMS"))
                    loadparams(jsonObject.getJSONArray("PARAMS"));
                if(jsonObject.has("PROGRAMS"))
                    programs.load(jsonObject.getJSONArray("PROGRAMS"));
                if(jsonObject.has("ALARMS"))
                    alarms.load(jsonObject.getJSONArray("ALARMS"));
                if(jsonObject.has("STATUS"))
                    loadstate(jsonObject.getJSONArray("STATUS"));



            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    public void save(String file){
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("PARAMS", saveparams());
            jsonObject.put("RADIO", radio.putmem());
            jsonObject.put("WIRED", wired.putmem());
            jsonObject.put("PROGRAMS", programs.put());
            jsonObject.put("ALARMS", alarms.putmem());
            jsonObject.put("STATUS", savestate());

            FileOutputStream outfile = context.openFileOutput(file, Context.MODE_PRIVATE);
            outfile.write(jsonObject.toString().getBytes());
            outfile.close();

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void save(){
        save(mem);
    }
    public boolean loadparams(JSONArray jsonArray){
        try{
            mac                  = jsonArray.getString(0);
            key                  = jsonArray.getString(1);
            name                 = jsonArray.getString(2);
            http_username        = jsonArray.getString(3);
            http_password        = jsonArray.getString(4);
            bonjourserver        = jsonArray.getString(5);
            bonjourport          = jsonArray.getInt(6);
            defaultsensor        = jsonArray.getInt(7);
            defaultvalue         = (float)jsonArray.getDouble(8);
            timeZone             = jsonArray.getInt(9);
            dayLight             = jsonArray.getInt(10);
            localip              = jsonArray.getString(11);
            localport            = jsonArray.getInt(12);
            weekenddays          = jsonArray.getInt(13);
            emails               = jsonArray.getString(14);
            repeat_alarm_sensors = jsonArray.getInt(15);
            uselocalap           = jsonArray.getBoolean(16);
            localipap            = jsonArray.getString(17);
            if (mac.startsWith("CA:5C:31") || mac.startsWith("B8:27:EB")) complet = true;
            else complet = false;
            return  true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
    public JSONArray saveparams() throws JSONException {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(mac);
        jsonArray.put(key);
        jsonArray.put(name);
        jsonArray.put(http_username);
        jsonArray.put(http_password);
        jsonArray.put(bonjourserver);
        jsonArray.put(bonjourport);
        jsonArray.put(defaultsensor);
        jsonArray.put(defaultvalue);
        jsonArray.put(timeZone);
        jsonArray.put(dayLight);
        jsonArray.put(localip);
        jsonArray.put(localport);
        jsonArray.put(weekenddays);
        jsonArray.put(emails);
        jsonArray.put(repeat_alarm_sensors);
        jsonArray.put(uselocalap);
        jsonArray.put(localipap);
        return jsonArray;
    }
    public void loadstate(JSONArray jsonArray){
        try {
            state = jsonArray.getInt(0);
            sensorused = jsonArray.getInt(1);
            valueset = (float) jsonArray.getDouble(2);
            energized = jsonArray.getBoolean(3);//new Integer(1).equals(jsonArray.getInt(3));
            value = (float) jsonArray.getDouble(4);
            hygro = jsonArray.getInt(5);
            alarm = jsonArray.getBoolean(6);//new Integer(1).equals(jsonArray.getInt(6));
            radioalarm = jsonArray.getBoolean(7);//new Integer(1).equals(jsonArray.getInt(7));
            wiredalarm = jsonArray.getBoolean(8);//new Integer(1).equals(jsonArray.getInt(8));
            program_number = jsonArray.getInt(9);
            hygrot = jsonArray.getInt(10);
            act_speed = jsonArray.getInt(11);
            desired_speed = jsonArray.getInt(12);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }
    public JSONArray savestate()throws JSONException{
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(state);//               = jsonArray.getInt(0);
        jsonArray.put(sensorused);//           = jsonArray.getInt(1);
        jsonArray.put(valueset);//             = (float) jsonArray.getDouble(2);
        jsonArray.put(energized);//            = jsonArray.getBoolean(3);//new Integer(1).equals(jsonArray.getInt(3));
        jsonArray.put(value);//                = (float) jsonArray.getDouble(4);
        jsonArray.put(hygro);//                = jsonArray.getInt(5);
        jsonArray.put(alarm);//                = jsonArray.getBoolean(6);//new Integer(1).equals(jsonArray.getInt(6));
        jsonArray.put(radioalarm);//           = jsonArray.getBoolean(7);//new Integer(1).equals(jsonArray.getInt(7));
        jsonArray.put(wiredalarm);//           = jsonArray.getBoolean(8);//new Integer(1).equals(jsonArray.getInt(8));
        jsonArray.put(program_number);//
        jsonArray.put(hygrot);
        jsonArray.put(act_speed);
        jsonArray.put(desired_speed);
        return jsonArray;
    }
    public int decodeMessage(String jsonString){
        try {waitingforanswer = false;
            JSONObject jsonObject = new JSONObject(jsonString);
            if(jsonObject.get("MAC").equals(mac)){//it's our thermostat
                waitforrequest = false;
                if(jsonObject.has("STATUS")) {// it's status message
                    JSONArray jsonArray = jsonObject.getJSONArray("STATUS");
                    state               = jsonArray.getInt(0);
                    sensorused          = jsonArray.getInt(1);
                    valueset            = (float) jsonArray.getDouble(2);
                    energized           = jsonArray.getBoolean(3);//new Integer(1).equals(jsonArray.getInt(3));
                    value               = (float) jsonArray.getDouble(4);
                    hygro               = jsonArray.getInt(5);
                    alarm               = jsonArray.getBoolean(6);//new Integer(1).equals(jsonArray.getInt(6));
                    radioalarm          = jsonArray.getBoolean(7);//new Integer(1).equals(jsonArray.getInt(7));
                    wiredalarm          = jsonArray.getBoolean(8);//new Integer(1).equals(jsonArray.getInt(8));
                    program_number      = jsonArray.getInt(9);
                    hygrot              = jsonArray.getInt(10);
                    if(jsonArray.length()>12){
                        act_speed       = jsonArray.getInt(11);
                        desired_speed   = jsonArray.getInt(12);
                    }

                    callback(1);
                    return 1;
                }

                if(jsonObject.has("RADIO")){// it's a radio sensors message
                    JSONArray jsonArray = jsonObject.getJSONArray("RADIO");
                    radio.loadcomm(jsonArray);
                    callback(2);
                    return 2;
                }
                if(jsonObject.has("WIRED")) {// it's a radio sensors message
                    JSONArray jsonArray = jsonObject.getJSONArray("WIRED");
                    wired.loadcomm(jsonArray);
                    callback(3);
                    return 3;
                }
                if(jsonObject.has("PARAMS")){// it's a params message
                    JSONArray jsonArray      = jsonObject.getJSONArray("PARAMS");
                    name                     = jsonArray.getString(0);
                    bonjourserver            = jsonArray.getString(1);
                    bonjourport              = jsonArray.getInt(2);
                    emails                   = jsonArray.getString(3);
                    if(jsonArray.length() > 4)
                        repeat_alarm_sensors = jsonArray.getInt(4);
                    if((jsonArray.length() > 5) && (!uselocalap))
                        localip              = jsonArray.getString(5);
                    callback(4);
                    return 4;
                }
                if(jsonObject.has("PROGRAMS")) {// it's a programs message
                    JSONArray jsonArray = jsonObject.getJSONArray("PROGRAMS");
                    programs.load(jsonArray);
                    callback(5);
                    return 5;
                }
                if(jsonObject.has("ALARMS")) {// it's a programs message
                    JSONArray jsonArray = jsonObject.getJSONArray("ALARMS");
                    alarms.load(jsonArray);
                    callback(6);
                    return 6;
                }
                if(jsonObject.has("TIME")){//it's a time message
                    JSONArray jsonArray = jsonObject.getJSONArray("TIME");
                    try{
                        actTime     = jsonArray.getInt(0);
                        timeZone    = jsonArray.getInt(1);
                        dayLight    = jsonArray.getInt(2);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    callback(7);
                    return 7;
                }
                callback(0);
                return 0;// unknow message type

            }
        }catch (JSONException e){
            e.printStackTrace();

        }
        commalarm = true;
        callback(-1);
        return -1;// error on parsing message or mac incorect
    }
    private void callback(int success){
        if(delegate != null){
            try{
                AsyncResponse activity = delegate.get();
                activity.refreshed(success);
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
    public String getStatus(){
        String r = "{\"MAC\":\"" + mac + "\", \"REQ\":\"GET_STATUS\"}\n";
        return r;
    }
    public String setState(int state){
        return setState(state, this.sensorused, this.valueset);
    }
    public String setState(int state, float valueset){
        return setState(state, this.sensorused, valueset);
    }
    public String setState(int state, int sensorused, float valueset){
        this.state = state;this.sensorused = sensorused; this.valueset = valueset;
        String r = "{\"MAC\":\"" + mac + "\", \"REQ\":\"SET\", \"STATUS\":[" + state + "," + sensorused + "," + valueset +"]}\n";
        waitingforanswer = true;
        new comm(this).execute(r);
        return r;
    }
    public void reqState(){
        comm comunication   = new comm(this);
        comunication.execute(getStatus());
        waitforrequest      = true;
    }
    public void reqRadio(){
        String r = "{\"MAC\":\"" + mac + "\", \"REQ\":\"GET_RADIO\"}\n";
        comm comunication = new comm(this);
        comunication.execute(r);
        waitforrequest = true;
    }
    public void reqWired(){
        String r = "{\"MAC\":\"" + mac + "\", \"REQ\":\"GET_WIRED\"}\n";
        comm comunication = new comm(this);
        comunication.execute(r);
        waitforrequest = true;
    }
    public void reqParams(){
        String r = "{\"MAC\":\"" + mac + "\", \"REQ\":\"GET_PARAMS\"}\n";
        comm comunication = new comm(this);
        comunication.execute(r);
        waitforrequest = true;
    }
    public void reqPrograms(){
        String r = "{\"MAC\":\"" + mac + "\", \"REQ\":\"GET_PROGRAMS\"}\n";
        comm comunication = new comm(this);
        comunication.execute(r);
        waitforrequest = true;
    }
    public void reqAlarms(){
        String r = "{\"MAC\":\"" + mac + "\", \"REQ\":\"GET_ALARMS\"}\n";
        comm comunication = new comm(this);
        comunication.execute(r);
        waitforrequest = true;
    }
    public void reqTime(){
        String r = "{\"MAC\":\"" + mac + "\", \"REQ\":\"GET_TIME\"}\n";
        comm comunication = new comm(this);
        comunication.execute(r);
        waitforrequest = true;
    }
    public String setRadio(int index, int number, String location, int[] add){
        radio.SensorNo(index).number = number;
        radio.SensorNo(index).location = location;
        for (int j = 0; j<8;j++)
            radio.SensorNo(index).add[j] = add[j];
        return setRadio();
    }
    public String setRadio(int number, String location){
        radio.SensorWithNumber(number).location = location;
        save();
        radio.refreshList();
        return setRadio();
    }
    public String addRadio(String location, Integer[] add){
        int freeidx = radio.firstFreeIdx();
        radio.SensorNo(freeidx).location = location;
        radio.SensorNo(freeidx).setAdd(add);
        radio.SensorNo(freeidx).number = radio.firstFreeNumber(1,6);
        save();
        getRadio().refreshList();
        return setRadio();
    }
    public String remRadio(int number){
        radio.SensorWithNumber(number).location = "Unknow";
        radio.SensorWithNumber(number).number = 0;
        save();
        radio.refreshList();
        return setRadio();
    }
    public String setRadio(){
        String r = "{\"MAC\":\"" + mac + "\", \"REQ\":\"SET\", \"RADIO\":" + radio.puts() +"}\n";
        waitingforanswer = true;
        new comm(this).execute(r);
        return r;
    }
    public String setWired(){
        String r = "{\"MAC\":\"" + mac + "\", \"REQ\":\"SET\", \"WIRED\":" + wired.puts() +"}\n";
        waitingforanswer = true;
        new comm(this).execute(r);
        return r;
    }
    public String setWired(int index, int number, String location, int[] add){
        wired.SensorNo(index).number = number;
        wired.SensorNo(index).location = location;
        for (int j = 0; j<8;j++)
            wired.SensorNo(index).add[j] = add[j];

        return setWired();
    }
    public String setWired(int number, String location){
        wired.SensorWithNumber(number).location = location;
        save();
        wired.refreshList();
        return setWired();
    }
    public String addWired(String location, Integer[] add){
        int freeidx = wired.firstFreeIdx();
        wired.SensorNo(freeidx).location = location;
        wired.SensorNo(freeidx).setAdd(add);
        wired.SensorNo(freeidx).number = wired.firstFreeNumber(11,14);
        save();
        getWired().refreshList();
        return setWired();
    }
    public String remWired(int number){
        wired.SensorWithNumber(number).location = "Unknow";
        wired.SensorWithNumber(number).number = 0;
        save();
        wired.refreshList();
        return setWired();
    }
    public void setAlarm(int index, int sensor, float value, int type, int repeat, String message){
        alarms.getAlarm(index).sensor   = sensor;
        alarms.getAlarm(index).value    = value;
        alarms.getAlarm(index).type     = type;
        alarms.getAlarm(index).repeat   = repeat;
        alarms.getAlarm(index).message  = message;
        setAlarms();
    }
    public void setAlarmValue(int index, float value){
        alarms.getAlarm(index).value   = value;
        save();
    }
    public void setAlarmRepeat(int index, int repeat){
        alarms.getAlarm(index).repeat  = repeat;
        save();
    }
    public void setAlarmEnabled(int index, boolean enabled, boolean minmax){
        if(enabled){
            if(minmax){
                alarms.getAlarm(index).type = 1;
            }else{
                alarms.getAlarm(index).type = 0;
            }
        }else{
            if(minmax){
                alarms.getAlarm(index).type = -1;
            }else{
                alarms.getAlarm(index).type = -2;
            }
        }
        save();
        alarms.refreshAlarmList();
    }
    public void setAlarmEnabled(int index, boolean enabled){
        if(enabled){
            if(alarms.getAlarm(index).type < 0){
                alarms.getAlarm(index).type += 2;
            }else{
                //nop
            }
        }else{
            if(alarms.getAlarm(index).type > -1){
                alarms.getAlarm(index).type -= 2;
            }else{
                //nop
            }
        }
        save();
        alarms.refreshAlarmList();
    }
    public void setAlarmMessage(int index, String message){
        alarms.getAlarm(index).message = message;
        save();
    }
    public void setAlarmSensor(int index, int sensor){
        alarms.getAlarm(index).sensor = sensor;
        save();
    }
    public boolean isAlarmEnabled(int index){
        return alarms.getAlarm(index).type > -1;
    }
    public boolean isAlarmMaximal(int index){
        return alarms.getAlarm(index).type > -1 ? alarms.getAlarm(index).type > 0 : alarms.getAlarm(index).type > -2;
    }
    public boolean setAlarms(){
        try {
            String r = "{\"MAC\":\"" + mac + "\", \"REQ\":\"SET\", \"ALARMS\":" + alarms.put().toString() + "}\n";
            waitingforanswer = true;
            new comm(this).execute(r);
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    public void setWeektime(int index, int hour, int minute){
        programs.getWeekProgram(index).hour     = hour;
        programs.getWeekProgram(index).minute   = minute;
        save();
    }
    public void setWeekValue(int index, float value){
        programs.getWeekProgram(index).value    = value;
        save();
    }
    public void setWeekSensor(int index, int sensor){
        programs.getWeekProgram(index).sensor    = sensor;
        save();
    }
    public void setWeek(int index, int hour, int minute, float value, int sensor, boolean write) {
        programs.getWeekProgram(index).hour     = hour;
        programs.getWeekProgram(index).minute   = minute;
        programs.getWeekProgram(index).value    = value;
        programs.getWeekProgram(index).sensor   = sensor;
        save();
        if(write)setPrograms();
    }
    public void setWeekendtime(int index, int hour, int minute){
        programs.getWeekendProgram(index).hour     = hour;
        programs.getWeekendProgram(index).minute   = minute;
        save();
    }
    public void setWeekendValue(int index, float value){
        programs.getWeekendProgram(index).value    = value;
        save();
    }
    public void setWeekendSensor(int index, int sensor){
        programs.getWeekendProgram(index).sensor    = sensor;
        save();
    }
    public void setWeekend(int index, int hour, int minute, float value, int sensor, boolean write){
        programs.getWeekendProgram(index).hour     = hour;
        programs.getWeekendProgram(index).minute   = minute;
        programs.getWeekendProgram(index).value    = value;
        programs.getWeekendProgram(index).sensor   = sensor;
        save();
        if(write)setPrograms();
    }
    public boolean setPrograms() {
        try {
            String r = "{\"MAC\":\"" + mac + "\", \"REQ\":\"SET\", \"PROGRAMS\":" + programs.put().toString() + "}\n";
            waitingforanswer = true;
            new comm(this).execute(r);
            return true;
        }catch (JSONException e){
            e.printStackTrace();
        }
        return false;
    }
    public String setParams(){
        String r ="{\"MAC\":\"" + mac + "\", \"REQ\":\"SET\", \"PARAMS\":[\"" +
                  name + "\",\"" + bonjourserver + "\"," + bonjourport + ",\"" + emails +"\"," + repeat_alarm_sensors + "]}\n";
        waitingforanswer = true;
        new comm(this).execute(r);
        return r;
    }
    public void setParams(String name, String emails, String bonjourserver, int bonjourport){
        this.name = name;
        this.emails = emails;
        this.bonjourserver = bonjourserver;
        this.bonjourport = bonjourport;
        setParams();
    }
    public void setTime() {
        String r ="{\"MAC\":\"" + mac + "\", \"REQ\":\"SET\", \"TIME\":[" +
                actTime + "," + timeZone + "," + dayLight + "]}\n";
        waitingforanswer = true;
        new comm(this).execute(r);
    }
    public void setTime(long newTime){
        actTime = newTime;
        setTime();
    }
    public void setTime(long newTime, int newTimeZone, int newDayLight){
        actTime = newTime;
        timeZone = newTimeZone;
        dayLight = newDayLight;
        setTime();
    }
    public long getActTime(){
        return actTime;
    }
    public int getTimeZone(){
        return timeZone;
    }
    public int getDayLight(){
        return dayLight;
    }
    public void setName(String name) {
        this.name = name;
        setParams();
    }
    public void setBonjourserver(String bonjourserver) {
        this.bonjourserver = bonjourserver;
    }
    public void setBonjourport(int bonjourport) {
        this.bonjourport = bonjourport;
    }
    public void setEmails(String emails){
        this.emails = emails;
        setParams();
    }
    public void setRepeat_alarm_sensors(int repeat_alarm_sensors){
        if(repeat_alarm_sensors >= 0 && repeat_alarm_sensors < 1081){
            this.repeat_alarm_sensors = repeat_alarm_sensors;
            setParams();
        }
    }
    public String getName(){return name;}
    public String getBonjourserver(){return bonjourserver;};
    public int getBonjourport() {
        return bonjourport;
    }
    public String getEmails() {
        return emails;
    }
    public int getRepeat_alarm_sensors(){
        return repeat_alarm_sensors;
    }
    public void setAlarms(Alarms alarms) {
        this.alarms = alarms;
    }
    public Alarms getAlarms(){
        return alarms;
    }
    public String getMac() {
        return mac;
    }
    public void setMac(String mac) {
        this.mac = mac;
        save();
    }
    public int getWeekenddays(){
        return weekenddays;
    }
    public void setWeekenddays(int weekenddays){
        this.weekenddays = weekenddays;
        if(state>2){
            state = 3 + weekenddays;
        }
        save();
        setState(state);
    }
    public int getState(){
        return state;
    }
    public float getValue() {
        return value;
    }
    public float getValueset() {
        return valueset;
    }
    public int getHygro() {
        return hygro;
    }
    public int getHygroT(){return hygrot; }
    public void setHygroT(int hygrot){this.hygrot = hygrot; save();}
    public int getProgram_number() {
        return program_number;
    }
    public boolean isEnergized() {
        return energized;
    }
    public boolean isAlarm() {
        return alarm;
    }
    public boolean isRadioalarm() {
        return radioalarm;
    }
    public boolean isWiredalarm() {
        return wiredalarm;
    }
    public Context getContext(){
        return context;
    }
    public boolean isCommalarm() {
        return commalarm;
    }
    public Sensors getRadio() {
        return radio;
    }
    public Sensors getWired() {
        return wired;
    }
    public Programs getPrograms(){return programs;}
    public Sensor getSensor(int index){
        for(int i =0; i<6; i++)
            if (radio.SensorNo(i).number == index)
                return radio.SensorNo(i);
        for(int i =0; i<4; i++)
            if (wired.SensorNo(i).number == index)
                return wired.SensorNo(i);
        return radio.SensorNo(0);
    }
    public Sensor getActSensor(){
        return getSensor(sensorused);
    }
    public String[] getNameOfSensors(){
        List <String > r= new ArrayList<>();
        int[] a= new int[16];
        int c=0;
        for(Sensor ss: radio.sensorList){
            r.add(ss.location);
            a[c++] = ss.number;
        }
        for (Sensor ss: wired.sensorList){
            r.add(ss.location);
            a[c++] = ss.number;
        }
        numberofsensors = new int[c];

        for(int j=0;j < c; j++)
            numberofsensors[j] = a[j];

        String[] rr = r.toArray(new String[0]);
        return rr;
    }
    public int[] getNumberofsensors(){
        return numberofsensors;
    }
    public void getAll(){
        new Thread(new Ask()).start();
    }

    public String getIP() {
        return localip;
    }
    public int getPort(){return localport;}

    public boolean isLocal(){return local;}
    public void setDistant(){
        local = false;
        //Toast.makeText(context, "to bonjour: "+ bonjourserver + " Port:"+ bonjourport, Toast.LENGTH_SHORT).show();
    }
    public String getMem(){return mem;}
    public void setIP(String ip) {localip = ip;
    save();
    }
    public boolean isUselocalap(){return uselocalap;}
    public void setUselocalap(boolean val){uselocalap=val;save();}
    public String getLocalipap(){return localipap;}
    private boolean isComplet(){return complet;}
    public int getActSpeed(){return act_speed;}
    public int getDesiredSpeed(){return desired_speed;}
    public void setDesired_speed(int desired_speed){
        if((desired_speed >9)&&(desired_speed<101))this.desired_speed = desired_speed;
        this.setState(this.state);
    }
    public float getExternalValue(){      //return value of first sensor named External or Exterior or else -99 returned
        if(radio.SensorWithName("External")!=null)
            return radio.SensorWithName("External").value;
        if(radio.SensorWithName("Exterior")!=null)
            return radio.SensorWithName("Exterior").value;
        if(wired.SensorWithName("External")!=null)
            return wired.SensorWithName("External").value;
        if(wired.SensorWithName("Exterior")!=null)
            return wired.SensorWithName("Exterior").value;
        return -99;
    }
    class Ask implements Runnable{
        @Override
        public void run(){
            reqParams();
            timeset = System.currentTimeMillis();
            while ((waitforrequest) && (System.currentTimeMillis() - timeset < 10000));
            if(waitforrequest) return;
            reqPrograms();
            timeset = System.currentTimeMillis();
            while ((waitforrequest) && (System.currentTimeMillis() - timeset < 10000));
            if(waitforrequest) return;
            reqAlarms();
            timeset = System.currentTimeMillis();
            while ((waitforrequest) && (System.currentTimeMillis() - timeset < 10000));
            if(waitforrequest) return;
            reqTime();
            timeset = System.currentTimeMillis();
            while ((waitforrequest) && (System.currentTimeMillis() - timeset < 10000));
            if(waitforrequest) return;
            reqRadio();
            timeset = System.currentTimeMillis();
            while ((waitforrequest) && (System.currentTimeMillis() - timeset < 10000));
            if(waitforrequest) return;
            reqWired();
            timeset = System.currentTimeMillis();
            while ((waitforrequest) && (System.currentTimeMillis() - timeset < 10000));
            if(waitforrequest) return;
            reqState();
            timeset = System.currentTimeMillis();
            while ((waitforrequest) && (System.currentTimeMillis() - timeset < 10000));
            waitforrequest = false;
            //
        }
    }
}
