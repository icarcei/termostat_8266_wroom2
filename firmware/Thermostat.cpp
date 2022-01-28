#include "Thermostat.h"

Thermostat* Thermostat::_instance = 0;
Thermostat::Thermostat(char* mac)
{         strcpy(macaddr, mac);
          state = 0;
          sensorused = 1;
          value = 5;
          humidity = 50;
          humidity_en = true;
          default_program.sensor = 1;
          default_program.value  = 5;
          
          timeZone = 2;
          dayLight = true;
          
          strcpy(name_of_dev, "IoT SUN Thermostat");
          strcpy(http_username, "admin");
          strcpy(http_password, "admin");
          strcpy(bonjourserver, "888.888.255.255");
          bonjourport          = 555555;
          strcpy(emails, "gmail@gmail.com");
          //strcpy(key, "cheiedeaccessthermos");

          LoadState();
          LoadRadioSensors();
          LoadWiredSensors();
          LoadAlarms();
          LoadParams();
          LoadPrograms();
          timeChange = true;

          startup   = true;
          lastcheck = millis() + 60000;
          
          energized = false;
          emailsdata.must = false;
          if(last_time > 100000)
              setTime(last_time);
          
          driver = new RH_ASK(2000, 5, 4, 0);
          // detectie senzori DS18S20

          oneWire = new OneWire(ONE_WIRE_BUS);
          dallassensors = new DallasTemperature(oneWire);
          
          #if defined(debug_t)
              Serial.println("Locating devices...");
          #endif
          dallassensors->begin();
          #if defined(debug_t)
              Serial.print("Found ");
              Serial.print(dallassensors->getDeviceCount(), DEC);
              Serial.println(" devices.");
          #endif
          if (!driver->init())
               Serial.println("radio driver init failed");
          else
               Serial.println("radio driver init with success");

          //set default program as first ocurence of am=n valid program
          for(int qp=0;qp<6;qp++){
              if((week_programs[qp].h>0)||(week_programs[qp].m>0)){
                  p=week_programs[qp];
                  current_program_number=qp;
                  break;
                  }
              }
          };         
//-------------------------------------------------------------------------------------------------------------------------------          
Thermostat* Thermostat::Instance(char* mac)
{
    if (_instance == 0)
        _instance = new Thermostat(mac);
    return _instance;
}
//------------------------------------------------------------------------------------------------------------------------------- 

//-------------------------------------------------------------------------------------------------------------------------------
bool Thermostat::LoadParams(){
if (SPIFFS.exists("/params.json")) {
    //file exists, reading and loading
    #if defined(debug_t)
        Serial.println("reading params file");
    #endif
    File regFile = SPIFFS.open("/params.json", "r");
    if (regFile) {
        #if defined(debug_t)
            Serial.println("opened params file");
        #endif
         size_t size = regFile.size();
         // Allocate a buffer to store contents of the file.
         std::unique_ptr<char[]> buf(new char[size]);

         regFile.readBytes(buf.get(), size);
         regFile.close();
         DynamicJsonBuffer jsonBuffer;
         JsonObject& json = jsonBuffer.parseObject(buf.get());
         #if defined(debug_t)
             json.printTo(Serial);
         #endif
         if (json.success()) {
             #if defined(debug_t)
                 Serial.println("\nparsed json");
             #endif
             if(json.containsKey("name_of_dev"))          strcpy(name_of_dev, json["name_of_dev"]);
             if(json.containsKey("http_username"))        strcpy(http_username, json["http_username"]);
             if(json.containsKey("http_password"))        strcpy(http_password, json["http_password"]);
             if(json.containsKey("bonjourserver"))        strcpy(bonjourserver, json["bonjourserver"]);
             if(json.containsKey("bonjourport"))          bonjourport  = json["bonjourport"];
             if(json.containsKey("defaultsensor"))        default_program.sensor  = json["defaultsensor"];
             if(json.containsKey("defaultvalue"))         default_program.value  = json["defaultvalue"];
             if(json.containsKey("timeZone"))             timeZone = json["timeZone"];
             if(json.containsKey("dayLight"))             dayLight  = json["dayLight"];
             if(json.containsKey("emails"))               strcpy(emails, json["emails"]);
             if(json.containsKey("repeat_alarm_sensors")) repeat_alarm_sensors = json["repeat_alarm_sensors"];
             return true;
         }
    }
    #if defined(debug_t)
        Serial.println("failed to open params.json .");
    #endif
 }
 #if defined(debug_t)
        Serial.println("params.json doesn't exist.");
    #endif
return false;
}// end of loadparams
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::SaveParams(void){
#if defined(debug_t)
    Serial.println("saving Params");
#endif
DynamicJsonBuffer jsonBuffer;
JsonObject& json              = jsonBuffer.createObject();
json["name_of_dev"]           = name_of_dev;
json["http_username"]         = http_username;
json["http_password"]         = http_password;
json["bonjourserver"]         = bonjourserver;
json["bonjourport"]           = bonjourport;
json["defaulsensor"]          = default_program.sensor;
json["defaultvalue"]          = default_program.value;
json["timeZone"]              = timeZone;
json["dayLight"]              = dayLight;
json["emails"]                = emails;
json["repeat_alarm_sensors"]  = repeat_alarm_sensors; 

File regFile = SPIFFS.open("/params.json", "w");
if (!regFile) {
   #if defined(debug_t)
       Serial.println("failed to open config file for writing");
   #endif
   return false;
}else{
   #if defined(debug_t)
       json.prettyPrintTo(Serial);
   #endif
   json.printTo(regFile);
   regFile.close();
   return true;
   }
}
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::LoadState(){
if (SPIFFS.exists("/state.json")) {
    //file exists, reading and loading
    #if defined(debug_t)
        Serial.println("reading state file");
    #endif
    File regFile = SPIFFS.open("/state.json", "r");
    if (regFile) {
        #if defined(debug_t)
            Serial.println("opened state file");
        #endif
         size_t size = regFile.size();
         // Allocate a buffer to store contents of the file.
         std::unique_ptr<char[]> buf(new char[size]);

         regFile.readBytes(buf.get(), size);
         regFile.close();
         DynamicJsonBuffer jsonBuffer;
         JsonObject& json = jsonBuffer.parseObject(buf.get());
         #if defined(debug_t)
             json.printTo(Serial);
         #endif
         if (json.success()) {
             #if defined(debug_t)
                 Serial.println("\nparsed json");
             #endif
             if(json.containsKey("state"))                   state                     = json["state"];
             if(json.containsKey("value"))                   value                     = json["value"];
             if(json.containsKey("humidity"))                humidity                  = json["humidity"];
             if(json.containsKey("sensorused"))              sensorused                = json["sensorused"];
             if(json.containsKey("program_number_on_forced"))program_number_on_forced  = json["program_number_on_forced"];
             if(json.containsKey("previos_state"))           previos_state             = json["previos_state"];
             if(json.containsKey("time"))                    last_time                 = json["time"];
             return true;
         }
    }
    #if defined(debug_t)
             Serial.println("failed to open state.json .");
    #endif
 }
 #if defined(debug_t)
        Serial.println("state.json doesn't exist.");
 #endif
return false;
}// end of loadparams
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::SaveState(void){
#if defined(debug_t)
    Serial.println("saving State");
#endif
DynamicJsonBuffer jsonBuffer;
JsonObject& json                 = jsonBuffer.createObject();
json["state"]                    = state;
json["value"]                    = value;
json["humidity"]                 = humidity;
json["sensorused"]               = sensorused;
json["program_number_on_forced"] = program_number_on_forced;
json["previos_state"]            = previos_state;
json["time"]                     = now();

File regFile = SPIFFS.open("/state.json", "w");
if (!regFile) {
   #if defined(debug_t)
       Serial.println("failed to open state file for writing");
   #endif
   return false;
}else{
   #if defined(debug_t)
       json.prettyPrintTo(Serial);
   #endif
   json.printTo(regFile);
   regFile.close();
   return true;
   }
}
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::LoadRadioSensors(){
   if (SPIFFS.exists("/radio.json")) {
       //file exists, reading and loading
       #if defined(debug_t)
           Serial.println("reading radio file");
       #endif
       File regFile = SPIFFS.open("/radio.json", "r");
       if (regFile) {
           #if defined(debug_t)
               Serial.println("opened radio file");
           #endif
           size_t size = regFile.size();
           // Allocate a buffer to store contents of the file.
           std::unique_ptr<char[]> buf(new char[size]);
           regFile.readBytes(buf.get(), size);
           regFile.close();
           DynamicJsonBuffer jsonBuffer;
           JsonArray& ja = jsonBuffer.parseArray(buf.get());
           #if defined(debug_t)
               ja.printTo(Serial);
           #endif
           if (ja.success()) {
               #if defined(debug_t)
                   Serial.println("\nparsed json");
               #endif
               for (int j=0 ;j < ja.size(); j++){
                   radioSensors[j].number = ja[j]["number"];
                   strcpy(radioSensors[j].location, ja[j]["location"]);
                   for (int jj=0; jj<8; jj++)radioSensors[j].address[jj] = ja[j]["address"][jj];  
                   //radioSensors[j].alarm_sent_at = now() + 300; 
                   radioSensors[j].timestamp = now();      
               }
               return true;
           }
       }
       #if defined(debug_t)
             Serial.println("failed to open radio.json .");
       #endif
   }
for (int j=0 ;j < 9; j++){
    radioSensors[j].number = 0;
    strcpy(radioSensors[j].location, "");
    for (int jj=0; jj<8; jj++)radioSensors[j].address[jj] = 0; 
}
#if defined(debug_t)
        Serial.println("radio.json doesn't exist.");
 #endif
return false;
}// end of loadradiosensors
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::SaveRadioSensors(){
DynamicJsonBuffer jsonBuffer;
JsonArray& ja = jsonBuffer.createArray();
for (int j=0 ;j < 9; j++){
    JsonObject& jo = ja.createNestedObject();
    jo["number"]    = radioSensors[j].number;
    jo["location"]  = radioSensors[j].location;
    JsonArray& addr = jo.createNestedArray("address");
    for (int jj=0; jj<8; jj++)
        addr.add(radioSensors[j].address[jj]);   
    #if defined(debug_t)
                  Serial.printf("RadioSensor:%d, nr:%d, location:%s, with address:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X\n",
                                j, radioSensors[j].number, radioSensors[j].location,
                                radioSensors[j].address[0],radioSensors[j].address[j],radioSensors[j].address[2],radioSensors[j].address[3],
                                radioSensors[j].address[4],radioSensors[j].address[j],radioSensors[j].address[6],radioSensors[j].address[7]);
              #endif
    }
            
File regFile = SPIFFS.open("/radio.json", "w");
if (!regFile) {
    #if defined(debug_t)
        Serial.println("failed to open config file for writing");
    #endif
    return false;
}else{
    #if defined(debug_t)
        ja.prettyPrintTo(Serial);
    #endif
    ja.printTo(regFile);
    regFile.close();
    return true;
}
}//end of save radio sensors
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::LoadWiredSensors(){
              if (SPIFFS.exists("/wired.json")) {
                  //file exists, reading and loading
                 #if defined(debug_t)
                     Serial.println("reading wired file");
                 #endif
                 File regFile = SPIFFS.open("/wired.json", "r");
                 if (regFile) {
                    #if defined(debug_t)
                        Serial.println("opened wired file");
                    #endif
                    size_t size = regFile.size();
                    // Allocate a buffer to store contents of the file.
                    std::unique_ptr<char[]> buf(new char[size]);

                    regFile.readBytes(buf.get(), size);
                    regFile.close();
                    DynamicJsonBuffer jsonBuffer;
                    JsonArray& ja = jsonBuffer.parseArray(buf.get());
                    #if defined(debug_t)
                        ja.printTo(Serial);
                    #endif
                    if (ja.success()) {
                        #if defined(debug_t)
                            Serial.println("\nparsed json");
                        #endif
                        for (int j=0 ;j < ja.size(); j++){
                            wiredSensors[j].number = ja[j]["number"];
                            for (int jj=0; jj<8; jj++)wiredSensors[j].address[jj] = ja[j]["address"][jj];
                            strcpy(wiredSensors[j].location, ja[j]["location"]);
                          }
                        return true;
                   }
              }
              #if defined(debug_t)
                  Serial.println("failed to open wired.json .");
              #endif
          }
          for (int j=0 ;j < 9; j++){
             wiredSensors[j].number = 0;
             strcpy(wiredSensors[j].location, "");
             for (int jj=0; jj<8; jj++)wiredSensors[j].address[jj] = 0;
          } 
#if defined(debug_t)
    Serial.println("wired.json doesn't exist.");
#endif
          return false;
         }//end of loadwiredsensors
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::SaveWiredSensors(){
            DynamicJsonBuffer jsonBuffer;
            JsonArray& ja = jsonBuffer.createArray();
            for (int j=0 ;j < 4; j++){
              JsonObject& jo = ja.createNestedObject();
              jo["number"]    = wiredSensors[j].number;
              jo["location"]  = wiredSensors[j].location;
              JsonArray& jaa  = jo.createNestedArray("address");
              for (int jj=0; jj<8; jj++)
                   jaa.add(wiredSensors[j].address[jj]);
              #if defined(debug_t)
                  Serial.printf("RadioSensor:%d, nr:%d, location:%s, with address:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X\n",
                                j, radioSensors[j].number, radioSensors[j].location,
                                radioSensors[j].address[0],radioSensors[j].address[j],radioSensors[j].address[2],radioSensors[j].address[3],
                                radioSensors[j].address[4],radioSensors[j].address[j],radioSensors[j].address[6],radioSensors[j].address[7]);
              #endif
            }
            
            File regFile = SPIFFS.open("/wired.json", "w");
            if (!regFile) {
                #if defined(debug_t)
                    Serial.println("failed to open wired file for writing");
                #endif
                return false;
            }else{
                #if defined(debug_t)
                    ja.prettyPrintTo(Serial);
                #endif
                ja.printTo(regFile);
                regFile.close();
                return true;
            } 
          
         }//end of save wired sensors
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::LoadAlarms(){
              if (SPIFFS.exists("/alarms.json")) {
                  //file exists, reading and loading
                 #if defined(debug_t)
                     Serial.println("reading alarms file");
                 #endif
                 File regFile = SPIFFS.open("/alarms.json", "r");
                 if (regFile) {
                    #if defined(debug_t)
                        Serial.println("opened alarms file");
                    #endif
                    size_t size = regFile.size();
                    // Allocate a buffer to store contents of the file.
                    std::unique_ptr<char[]> buf(new char[size]);

                    regFile.readBytes(buf.get(), size);
                    regFile.close();
                    DynamicJsonBuffer jsonBuffer;
                    JsonArray& ja = jsonBuffer.parseArray(buf.get());
                    #if defined(debug_t)
                        ja.printTo(Serial);
                    #endif
                    if (ja.success()) {
                        #if defined(debug_t)
                            Serial.println("\nparsed json");
                        #endif
                        for (int j=0 ;j < ja.size(); j++){
                            alarms[j].sensor  = ja[j]["sensor"];
                            alarms[j].value   = ja[j]["value"];
                            alarms[j].type    = ja[j]["type"];
                            if(ja[j].size() > 4)
                                alarms[j].repeat  = ja[j]["repeat"];
                            strcpy(alarms[j].message, ja[j]["message"]);
                          }
                        return true;
                   }
              }
              #if defined(debug_t)
                  Serial.println("failed to open alarms.json .");
              #endif
          }
#if defined(debug_t)
        Serial.println("alarms.json doesn't exist.");
#endif
          return false;
         }//end of loadAlarms
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::SaveAlarms(){
    DynamicJsonBuffer jsonBuffer;
    JsonArray& ja = jsonBuffer.createArray();
    for (int j=0 ;j < 8; j++){
        JsonObject& jo = ja.createNestedObject();
        jo["sensor"]    = alarms[j].sensor;
        jo["value"]     = alarms[j].value;
        jo["type"]      = alarms[j].type;
        jo["repeat"]    = alarms[j].repeat;
        jo["message"]   = alarms[j].message;
    }
    File regFile = SPIFFS.open("/alarms.json", "w");
    if (!regFile) {
        #if defined(debug_t)
            Serial.println("failed to open alarms file for writing");
        #endif
        return false;
    }else{
        #if defined(debug_t)
            ja.prettyPrintTo(Serial);
        #endif
        ja.printTo(regFile);
        regFile.close();
        return true;
    }
}// end of save alarms
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::LoadPrograms(){
    if (SPIFFS.exists("/programs.json")) {
        //file exists, reading and loading
        #if defined(debug_t)
            Serial.println("reading programs file");
        #endif
        File regFile = SPIFFS.open("/programs.json", "r");
        if (regFile) {
            #if defined(debug_t)
                Serial.println("opened programs file");
            #endif
            size_t size = regFile.size();
            // Allocate a buffer to store contents of the file.
            std::unique_ptr<char[]> buf(new char[size]);
            regFile.readBytes(buf.get(), size);
            regFile.close();
            DynamicJsonBuffer jsonBuffer;
            JsonObject& json = jsonBuffer.parseObject(buf.get());
            #if defined(debug_t)
                json.printTo(Serial);
            #endif
            if (json.success()) {
                #if defined(debug_t)
                    Serial.println("\nparsed json");
                #endif
                if(json.containsKey("week_programs")){
                    JsonArray& ja = json["week_programs"];
                    for (int j=0 ;j < ja.size(); j++){
                        week_programs[j].h = ja[j]["h"];
                        week_programs[j].m = ja[j]["m"];
                        week_programs[j].value = ja[j]["value"];
                        week_programs[j].sensor = ja[j]["sensor"];
                    }
                }
                if(json.containsKey("weekend_programs")){
                    JsonArray& ja = json["weekend_programs"];
                    for (int j=0 ;j < ja.size(); j++){
                        weekend_programs[j].h = ja[j]["h"];
                        weekend_programs[j].m = ja[j]["m"];
                        weekend_programs[j].value = ja[j]["value"];
                        weekend_programs[j].sensor = ja[j]["sensor"];
                    }
                }
                return true;
            }
        }
        #if defined(debug_t)
             Serial.println("failed to open radio.json .");
        #endif
    }
    #if defined(debug_t)
        Serial.println("programs.json doesn't exist.");
    #endif
    return false;
}// end of load programs
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::SavePrograms(){
    DynamicJsonBuffer jsonBuffer;
    JsonObject& json = jsonBuffer.createObject();
    JsonArray& ja = json.createNestedArray("week_programs");
    for (int j=0 ;j < 6; j++){
        JsonObject& jo = ja.createNestedObject();
        jo["h"]      = week_programs[j].h;
        jo["m"]      = week_programs[j].m;
        jo["value"]  = week_programs[j].value;
        jo["sensor"] = week_programs[j].sensor;
        #if defined(debug_t)
           Serial.printf("Save WeekProgram:%d, hour:%d, minute:%d, sensor:%d, value:%.1f\n",
                          j, week_programs[j].h, week_programs[j].m, week_programs[j].sensor, week_programs[j].value);
        #endif
    }
    JsonArray& jja = json.createNestedArray("weekend_programs");
    for (int j=0 ;j < 6; j++){
        JsonObject& jo = jja.createNestedObject();
        jo["h"]      = weekend_programs[j].h;
        jo["m"]      = weekend_programs[j].m;
        jo["value"]  = weekend_programs[j].value;
        jo["sensor"] = weekend_programs[j].sensor;
        #if defined(debug_t)
           Serial.printf("Save WeekendProgram:%d, hour:%d, minute:%d, sensor:%d, value:%.1f\n",
                          j, weekend_programs[j].h, weekend_programs[j].m, weekend_programs[j].sensor, weekend_programs[j].value);
        #endif
    }

    File regFile = SPIFFS.open("/programs.json", "w");
    if (!regFile) {
        #if defined(debug_t)
            Serial.println("failed to open programs file for writing");
        #endif
        return false;
    }else{
        #if defined(debug_t)
            json.prettyPrintTo(Serial);
        #endif
        json.printTo(regFile);
        regFile.close();
        return true;
    }
}// end of save programs
//------------------------------------------------------------------------------------------------------------------------------- 
int Thermostat::Status(){
return state;
}
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::SetOff(uint8_t s){
            if( s>14)
                return false;
            state = 0;
            sensorused = s;
            default_program.sensor = s;
            lastchange = now();
            SaveState();
            return true;
          }
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::SetManual(float val, uint8_t s){
            if (val<5 || val >30)
               return false;
            if( s>14)
                return false;
            value = val;
            state = 1;
            sensorused = s;
            lastchange = now(); 
            SaveState();
            return true;
          }
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::SetForced(float val, uint8_t s){
            if (val<5 || val >30)
               return false;
            if( s>14)
                return false;
            value = val;
            state != 2 ? previos_state = state : previos_state = previos_state;
            state = 2;
            sensorused = s;
            lastchange = now();
            program_number_on_forced = current_program_number; 
            SaveState();
            return true;
}
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::SetAuto(uint8_t weekend_days){
if (weekend_days == 2)
    state = 5;
else{
    if (weekend_days == 1)
        state = 4;
    else
        state = 3;
    }
lastchange = now();
SaveState();
return true;
}
//------------------------------------------------------------------------------------------------------------------------------- 
TemperatureSensor Thermostat::current_sensor(){
  if(sensorused >9)
     return wiredSensors[sensorused-10];
  else
     return radioSensors[sensorused];
}
//------------------------------------------------------------------------------------------------------------------------------- 
TemperatureSensor Thermostat::current_sensor(int nr){
  //Serial.print("Looking for sensor nr:");Serial.println(nr); 
  if(nr >10){
     for(int i = 0; i < 4; i++)
         if (wiredSensors[i].number == nr )
             return wiredSensors[i];
  }else{
     for(int i = 0; i < 6; i++){
         //Serial.print(radioSensors[i].location);Serial.print(" with number:");Serial.print(radioSensors[i].number);
         if (radioSensors[i].number == nr ){
            //Serial.println(" Is what we looking");
             return radioSensors[i];
         }else{
            //Serial.println(" Is NOT what we looking");
         }
     }
  }           
  return radioSensors[0];
}
//-------------------------------------------------------------------------------------------------------------------------------

//------------------------------------------------------------------------------------------------------------------------------- 
Program Thermostat::current_program(){
  int program = -1, ph =0, pm=0, wd=weekday();
  //check for weekend auto sunday and saturday state =5 sunday,wd=1 and saturday,wd=7
  if(( state == 5 && ( wd == 1 || wd == 7 )) || ( state == 4 && wd == 1 )){
      #if defined(debug_t)
          Serial.printf("Is weekend (wd=%d, state=%d)\n", wd, state);
      #endif
      for (int i =0;i<6;i++){ 
           #if defined(debug_t)
              Serial.printf("weekend %d program %02d:%02d, now is %02d:%02d\n", i, weekend_programs[i].h, weekend_programs[i].m, hour(), minute());
           #endif
          if( (weekend_programs[i].h > 0) || (weekend_programs[i].m > 0)){// if it is a valid program
              #if defined(debug_t)
                      Serial.printf("1Valid program\n");
              #endif
              if(hour() > weekend_programs[i].h){
                  if(weekend_programs[i].h > ph){
                      ph = weekend_programs[i].h;
                      pm = weekend_programs[i].m;
                      program = i;  
                      #if defined(debug_t)
                          Serial.printf("2in program\n");
                      #endif 
                  }else{
                      if((weekend_programs[i].h == ph) && (minute() >= weekend_programs[i].m)){// > pm)){
                          ph = weekend_programs[i].h;
                          pm = weekend_programs[i].m;
                          program = i;  
                          #if defined(debug_t)
                             Serial.printf("3in program\n");
                          #endif      
                      }
                  }      
              }else{
                  if( ((hour() == weekend_programs[i].h) && (minute() >= weekend_programs[i].m))  && 
                      ((weekend_programs[i].h > ph) ||((weekend_programs[i].h == ph) && (weekend_programs[i].m > pm)) )){
                      program = i;
                      ph = weekend_programs[i].h;
                      pm = weekend_programs[i].m; 
                      #if defined(debug_t)
                      Serial.printf("4in program\n");
                      #endif
                  }else{
                      #if defined(debug_t)
                      Serial.printf("5NOT in program\n");
                      #endif
                  }
                  
              }
            
          }else{
              #if defined(debug_t)
                       Serial.printf("6INValid program\n");
              #endif
          }
          
      }
      //current_program_number = program + 10;
      if(program == -1) return p;//default_program;
      else {current_program_number = program + 10;
            return weekend_programs[program];
           }
  }else{
      #if defined(debug_t)
          Serial.printf("Is week (wd=%d, state=%d)\n", wd, state);
      #endif
      for (int i =0;i<6;i++){ 
          #if defined(debug_t)
              Serial.printf("week %d program %02d:%02d, memory %02d:%02d, now is %02d:%02d\n", i, week_programs[i].h, week_programs[i].m, ph, pm, hour(), minute());
          #endif
          if( (week_programs[i].h > 0) || (week_programs[i].m > 0)){// if it is a valid program
              #if defined(debug_t)
                      Serial.printf("7Valid program\n");
              #endif
              if(hour() > week_programs[i].h){
                  if(week_programs[i].h > ph){
                      ph = week_programs[i].h;
                      pm = week_programs[i].m;
                      program = i;  
                      #if defined(debug_t)
                          Serial.printf("8in program\n");
                      #endif 
                  }else{
                      if((week_programs[i].h == ph) && (minute() >= week_programs[i].m)){// > pm){
                          ph = week_programs[i].h;
                          pm = week_programs[i].m;
                          program = i;  
                          #if defined(debug_t)
                             Serial.printf("9in program\n");
                          #endif      
                      }
                  }
              }else{
               
                  if(((hour() == week_programs[i].h) && (minute() >= week_programs[i].m)) && 
                      ((week_programs[i].h > ph) ||((week_programs[i].h == ph) && (week_programs[i].m > pm)))){
                          ph = week_programs[i].h;
                          pm = week_programs[i].m;
                          program = i;
                          #if defined(debug_t)
                              Serial.printf("10in program\n");
                          #endif
                    
                  }else{
                      #if defined(debug_t)
                      Serial.printf("11 NOT in program\n h=%02d wh=%02d ph=%02d m=%02d wm=%02d pm=%02d", hour(), week_programs[i].h, ph, minute(), week_programs[i].m, pm);
                      #endif
                  }
                  
              }
            
          }else{
              #if defined(debug_t)
                       Serial.printf(" 12 INValid program\n");
              #endif
          }
         
       
      }
      #if defined(debug_t)
           Serial.printf("Current program number: %d\n", program);
      #endif
      
      if(program == -1) return p;//default_program;
      else {current_program_number = program;
            return week_programs[program];
           }
      
  }
}// end of current_program
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::is_energized(){
  //Program p;
  switch(state){
    case 0: p    = default_program;
        p.sensor = sensorused;
        break;
    case 1: 
        p.sensor = sensorused;
        p.value  = value;
        break;
    case 2:
        p        = current_program();
        if(program_number_on_forced != current_program_number)// Forced time is finished?
            {
              state = previos_state;
            }
        else{
              p.sensor = sensorused;
              p.value  = value;
            }
        break;
    default: p   = current_program();
  }
 
  if(current_sensor(p.sensor).value < (p.value - 0.3)) energized = true;
  else if (current_sensor(p.sensor).value > (p.value + 0.3)) energized = false;

  if(current_sensor(p.sensor).humidity > (humidity + 1)) humidity_en = false;
  else if(current_sensor(p.sensor).humidity < (humidity - 1)) humidity_en = true; 
  
  #if defined(debug_t)
      Serial.printf("new state %d with sensor %d and sensor value of %.1f on: %02d:%02d:%02d\n", state, p.sensor, p.value, hour(), minute(), second());
  #endif

 return energized;
 }
//-------------------------------------------------------------------------------------------------------------------------------  
void Thermostat::checkforalarm(){
  alarmsActive = false;
  for(int i =0;i<8;i++){
      alarms[i].active = false;
      if(alarms[i].type > -1){//if alam is enabled
          
          if((alarms[i].type == 0 && current_sensor(alarms[i].sensor).value < (alarms[i].value - 0.3)) || //alarm triger for minimal
             (alarms[i].type == 1 && current_sensor(alarms[i].sensor).value > (alarms[i].value + 0.3))){  //alarm triger for maximal
                //if(!alarms[i].active){//first activation after
                //   alarms[i].sent = 0;
                //}
                alarms[i].active = true; 
            }
          else {
                alarms[i].sent = 0;
          }
          
      }
  alarmsActive |= alarms[i].active;     
  }
}
//------------------------------------------------------------------------------------------------------------------------------- 
void Thermostat::check(){//must be in loop
    checkforradio();
    //checkforI2Cradio();
    if(millis() - lastcheck > 60000){
        
        is_energized();       // check if is energized
        // check for alarms
        checkforalarm();
        //read wired sensors
        dallassensors->requestTemperatures();
        wiredSensorsAlarm = false;
        for(int i = 0; i < 4; i++){
            if(wiredSensors[i].number > 9){
                wiredSensors[i].value = dallassensors->getTempC(wiredSensors[i].address); 
                if(wiredSensors[i].value < -40)
                    {wiredSensors[i].alarm = true;}
                else
                    {wiredSensors[i].alarm = false;
                     wiredSensors[i].timestamp = now();}
                wiredSensorsAlarm |= wiredSensors[i].alarm;    
            }
                
        }    
        // check for radio sensors alarms
        if(startup){
          if(millis() > 360000)startup = false; // cancel startup condition 
        }else{
          radioSensorsAlarm = false;
          for(int i=0; i < 9; i++)
            if(radioSensors[i].number > 0){
              if(radioSensors[i].energy < 2.8 || (now() - radioSensors[i].timestamp > 600))
                 radioSensors[i].alarm = true;
              else
                 radioSensors[i].alarm = false;
              radioSensorsAlarm |= radioSensors[i].alarm;      
            }
        }

       // following is for retransmission of values on i2cBUS
        //getAnaOut(values_out); 
        //set_analogic_outs(values_out);
           
        lastcheck = millis();
    }
            
}
//------------------------------------------------------------------------------------------------------------------------------- 
bool Thermostat::sendEmail(Alarm a){

  a.sent = true;
  return true;
}
//------------------------------------------------------------------------------------------------------------------------------- 
void Thermostat::checkforradio(){
  //uint8_t lsbuf[RH_ASK_MAX_MESSAGE_LEN];
  //uint8_t lbuflen = sizeof(lsbuf);
  if(driver->recv(sbuf, &buflen)){
      #if defined(debug_t)
        driver->printBuffer("Got:", sbuf, buflen);
        Serial.printf("With length %d", buflen);
      #endif
      //mesage type 8 byte address 2 byte value
      if(buflen > 11){//if it has corect length 
         int sensorindex = radioSensorIndex(sbuf);
         #if defined(debug_t)
             Serial.printf("SensorIndex:%d\n", sensorindex);
         #endif
         if( sensorindex >= 0){
              int val;
              val = ((int)sbuf[9])<<8 | ((int)sbuf[8]);
              radioSensors[sensorindex].value  = val;
              radioSensors[sensorindex].value /= 10;
              val = ((int)sbuf[11])<<8 | ((int)sbuf[10]);
              radioSensors[sensorindex].energy  = val;
              float formula;
              formula  = VREF * ( RSUS + RJOS );
              formula /= RJOS * 1024 * 1000;
              radioSensors[sensorindex].energy *= formula;
              if(buflen >13) // sensor with humidity
                 {
                  val = ((int)sbuf[13])<<8 | ((int)sbuf[12]);
                  radioSensors[sensorindex].humidity = val;
                 }
              radioSensors[sensorindex].timestamp = now();
              #if defined(debug_t)
                  Serial.printf("Sensor new data on: %02d.%02d.%04d - %02d:%02d:%02d\n",day(), month(), year(), hour(), minute(), second());
                  Serial.print("Sensor Value:");Serial.println(radioSensors[sensorindex].value);
                  Serial.print("Sensor Energy:");Serial.println(radioSensors[sensorindex].energy);
                  Serial.printf("Sensor Humidity: %02.f\n", radioSensors[sensorindex].humidity);
     
              #endif
         }
      }
      buflen = sizeof(sbuf);
  }
}
//------------------------------------------------------------------------------------------------------------------------------- 
int Thermostat::radioSensorIndex(uint8_t* addr){
  for(int i = 0; i < 9; i++){
      boolean ok = true;
      for( int j = 0; j < 8; j++)
          if(addr[j] != radioSensors[i].address[j]) ok = false;
      if(ok) return i;
  }
  return -1;
}
//------------------------------------------------------------------------------------------------------------------------------- 
int Thermostat::check_message(char* m){
   DynamicJsonBuffer jsonBuffer;
    //JsonArray& jsonarray = jsonBuffer.parseArray(m);
   JsonObject& json = jsonBuffer.parseObject(m);
    //json.printTo(Serial);
    //Message types {"MAC":"....", "REQ":"SET", "STATUS":[status, sensornr, value]}
    //              {"MAC":"....", "REQ":"SET", "PARAMS":[status, sensornr, value]}
    //              {"MAC":"....", "REQ":"SET", "PROGRAMS":[status, sensornr, value]}
    //              {"MAC":"....", "REQ":"SET", "SENSORS":[status, sensornr, value]}
    //              {"MAC":"....", "REQ":"SET", "ALARMS":[status, sensornr, value]}
    //              {"MAC":"....", "REQ":"GET_STATUS", "FROM":"....."}
    //              {"MAC":"....", "REQ":"GET_PROGRAMS", "FROM":"....."}
    //              {"MAC":"....", "REQ":"GET_SENSORS", "FROM":"....."}
    //              {"MAC":"....", "REQ":"GET_ALARMS", "FROM":"....."}
    //              {"MAC":"....", "REQ":"GET_PARAMS", "FROM":"....."}
    if (json.success() && json.containsKey("MAC"))
    if(strcmp(json["MAC"], macaddr) == 0) {
      String req = json["REQ"];
      if(req == "SET"){
        //{"MAC":"....", "REQ":"SET", "STATUS":[status, sensornr, value]}
        if(json.containsKey("STATUS")){
          int   new_status   = json["STATUS"][0],
                new_sensornr = json["STATUS"][1];
          float new_value    = json["STATUS"][2];
          if(json["STATUS"].size() > 3)
               {
                humidity    = json["STATUS"][3];
               }
          switch(new_status){
              case 0: SetOff(new_sensornr);break;
              case 1: SetManual(new_value, new_sensornr);break;
              case 2: SetForced(new_value, new_sensornr);break;
              case 3:
              case 4:
              case 5: SetAuto(new_status - 3);break;
          default:
              strcpy(m, "{\"MAC\":\"");strcat(m, macaddr);strcat(m, "\",\"REQ\":\"");
              strcat(m,"ANS\"}\n");
              
              return check_message(m);//0;   
        }
        is_energized();       // check if is energized          
        checkforalarm(); // check for alarms
        strcpy(m, "{\"MAC\":\"");strcat(m, macaddr);strcat(m, "\",\"REQ\":\"");
        strcat(m,"GET_STATUS\"}\n");
          return check_message(m);//0;     
        }//end of setStatus
        //{"MAC":"....", "REQ":"SET", "PARAMS":[name_of_dev, bonjourserver, bonjourport, emails, repeat_alarm_sensors]}
        if(json.containsKey("PARAMS")){
          strcpy(name_of_dev, json["PARAMS"][0]);
          strcpy(bonjourserver, json["PARAMS"][1]);
          strcpy(emails, json["PARAMS"][3]);
          bonjourport = json["PARAMS"][2];
          if(json["PARAMS"].size() > 4)
              repeat_alarm_sensors = json["PARAMS"][4];
          SaveParams();
          strcpy(m, "{\"MAC\":\"");strcat(m, macaddr);strcat(m, "\",\"REQ\":\"");
          strcat(m,"GET_PARAMS\"}\n");
          paramchange = true;
          return check_message(m);//0;
        }//end of setPARAMS
        //{"MAC":"....", "REQ":"SET", "PROGRAMS":[[h, m, value,s],....]} 12 subarray
        if(json.containsKey("PROGRAMS")){
          for(int i =0; i<6; i++){
            week_programs[i].h = json["PROGRAMS"][i][0];
            week_programs[i].m = json["PROGRAMS"][i][1];
            week_programs[i].value = json["PROGRAMS"][i][2];
            week_programs[i].sensor = json["PROGRAMS"][i][3];
          }
          for(int i =0; i<6; i++){
            weekend_programs[i].h = json["PROGRAMS"][i+6][0];
            weekend_programs[i].m = json["PROGRAMS"][i+6][1];
            weekend_programs[i].value = json["PROGRAMS"][i+6][2];
            weekend_programs[i].sensor = json["PROGRAMS"][i+6][3];
          }
          SavePrograms();
          strcpy(m, "{\"MAC\":\"");strcat(m, macaddr);strcat(m, "\",\"REQ\":\"");
          strcat(m,"GET_PROGRAMS\"}\n");
          is_energized();       // check if is energized          
          checkforalarm(); // check for alarms
          return check_message(m);//0;
        }//end of setPrograms
        //{"MAC":"....", "REQ":"SET", "SENSORS":[[number; location[30]; address[8];]......]} 9/4 subarray
        if(json.containsKey("WIRED")){
            for(int i = 0; i < json["WIRED"].size(); i++){
              wiredSensors[i].number = json["WIRED"][i][0];
              strcpy(wiredSensors[i].location, json["WIRED"][i][1]);
              for(int j=0; j<8; j++)
                 wiredSensors[i].address[j] = json["WIRED"][i][2][j];
              #if defined(debug_t)
                  Serial.printf("WiredSensor:%d, nr:%d, location:%s, with address:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X\n",
                                i, wiredSensors[i].number, wiredSensors[i].location,
                                wiredSensors[i].address[0],wiredSensors[i].address[1],wiredSensors[i].address[2],wiredSensors[i].address[3],
                                wiredSensors[i].address[4],wiredSensors[i].address[5],wiredSensors[i].address[6],wiredSensors[i].address[7]);
              #endif
            }
           SaveWiredSensors();
           strcpy(m, "{\"MAC\":\"");strcat(m, macaddr);strcat(m, "\",\"REQ\":\"");
          strcat(m,"GET_WIRED\"}\n");
          is_energized();       // check if is energized          
          checkforalarm(); // check for alarms
          return check_message(m);//0;
           }
        if(json.containsKey("RADIO")){
            #if defined(debug_t)
                Serial.printf("SET %d RADIO sensors\n", json["RADIO"].size());
            #endif
            bool existnumberone = false;
            for(int i = 0; i < json["RADIO"].size(); i++){
              radioSensors[i].number = json["RADIO"][i][0];
              
              if(radioSensors[i].number == 1){
                  existnumberone = true;
                }
              
              strcpy(radioSensors[i].location, json["RADIO"][i][1]);
              //Serial.println(json["SENSORS"][i][2]);
              for(int j=0; j<8; j++)
                 {radioSensors[i].address[j] = json["RADIO"][i][2][j];
                  //Serial.println(radioSensors[i].address[j]);
                 }
              #if defined(debug_t)
                  Serial.printf("RadioSensor:%d, nr:%d, location:%s, with address:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X\n",
                                i, radioSensors[i].number, radioSensors[i].location,
                                radioSensors[i].address[0],radioSensors[i].address[1],radioSensors[i].address[2],radioSensors[i].address[3],
                                radioSensors[i].address[4],radioSensors[i].address[5],radioSensors[i].address[6],radioSensors[i].address[7]);
              #endif
            }
            if(!existnumberone){
                for(int i = 0; i < 6; i++){
                   if (radioSensors[i].number >0 ){
                      radioSensors[i].number = 1 ;
                      existnumberone = true;
                      break;
                   }
                }
            }
            if(!existnumberone){
                 radioSensors[0].number = 1;
            }
            SaveRadioSensors();
            strcpy(m, "{\"MAC\":\"");strcat(m, macaddr);strcat(m, "\",\"REQ\":\"");
            strcat(m,"GET_RADIO\"}\n");
            is_energized();       // check if is energized          
            checkforalarm(); // check for alarms
            return check_message(m);//0;
           }
          
        //end of setSensors
        
        //{"MAC":"....", "REQ":"SET", "ALARMS":[uint8_t sensor; float value; int type; int repeat; char message[50];]}
        if(json.containsKey("ALARMS")){
          for(int i = 0; i < json["ALARMS"].size(); i++){
            alarms[i].sensor = json["ALARMS"][i][0];
            alarms[i].value  = json["ALARMS"][i][1];
            alarms[i].type   = json["ALARMS"][i][2];
            alarms[i].repeat = json["ALARMS"][i][3];
            strcpy(alarms[i].message, json["ALARMS"][i][4]);
          }
          SaveAlarms();
          strcpy(m, "{\"MAC\":\"");strcat(m, macaddr);strcat(m, "\",\"REQ\":\"");
          strcat(m,"GET_ALARMS\"}\n");
          is_energized();       // check if is energized          
          checkforalarm(); // check for alarms
          return check_message(m);//0; 
        }//end of setAlarms
        //{"MAC":"....", "REQ":"SET", "TIME":[time_t time; int timeZone; bool daylight]}
        if(json.containsKey("TIME")){
          time_t new_time = (time_t)json["TIME"][0];
          timeZone = json["TIME"][1];
          dayLight = json["TIME"][2];
          if (new_time > 1543411754) 
               setTime(new_time);
          timeChange = true;
          SaveParams();

          strcpy(m, "{\"MAC\":\"");strcat(m, macaddr);strcat(m, "\",\"REQ\":\"");
          strcat(m,"GET_TIME\"}\n");
          is_energized();       // check if is energized          
          checkforalarm(); // check for alarms
          return check_message(m);//0;
      
        }//end of setTime
        //ignore another type of set and exit
        strcpy(m, "{\"MAC\":\"");strcat(m, macaddr);strcat(m, "\",\"REQ\":\"");
          strcat(m,"ANS\"}\n");
          return check_message(m);//0;
      }// end of SET KEY

    DynamicJsonBuffer jsonBuffer;
    //JsonArray& jsonarray = jsonBuffer.parseArray(m);
   JsonObject& jsonansw = jsonBuffer.createObject();
   jsonansw["MAC"] = macaddr;
    //              {"MAC":"....", "REQ":"GET_STATUS", "FROM":"....."}
    if(req == "GET_STATUS"){
      // answer     {"MAC":"....", "STATUS":[status, sensorSET, valueSET, energized, valueACT, alarm?, radioSensorAlarm, wiredSensorAlarm]} 
      JsonArray& R = jsonansw.createNestedArray("STATUS");
      R.add(state);R.add(p.sensor);R.add(p.value);R.add(energized);R.add(current_sensor(p.sensor).value);R.add(current_sensor(p.sensor).humidity);
      R.add(alarmsActive); R.add(radioSensorsAlarm);R.add(wiredSensorsAlarm); 
      R.add(current_program_number);R.add(humidity);
      jsonansw.printTo(m, jsonansw.measureLength()+1);
      return jsonansw.measureLength()+1;     
    }// end of get_status
    //              {"MAC":"....", "REQ":"GET_PROGRAMS", "FROM":"....."}
    if(req == "GET_PROGRAMS"){
      // answer     {"MAC":"....", "PROGRAMS":[[h, m, value, s], []....}12 subarrays 
      JsonArray& R = jsonansw.createNestedArray("PROGRAMS");
      for (int i = 0; i < 6; i++){
        JsonArray& pa = R.createNestedArray();
        pa.add(week_programs[i].h);pa.add(week_programs[i].m);pa.add(week_programs[i].value);pa.add(week_programs[i].sensor);
      }
      for (int i = 0; i < 6; i++){
        JsonArray& pa = R.createNestedArray();
        pa.add(weekend_programs[i].h);pa.add(weekend_programs[i].m);pa.add(weekend_programs[i].value);pa.add(weekend_programs[i].sensor);
      }
      jsonansw.printTo(m, jsonansw.measureLength()+1);
      return jsonansw.measureLength()+1; 
    }// end of get programs
    //              {"MAC":"....", "REQ":"GET_RADIO", "REFRESH":"","FROM":"....."}
    if(req == "GET_RADIO"){
      // answer     {"MAC":"....", "RADIO":[[number; location[30]; address[8];]......]} 6 subarray 
      JsonArray& R = jsonansw.createNestedArray("RADIO");
      for (int i = 0; i < 6; i++){
        JsonArray& sr = R.createNestedArray();
        sr.add(radioSensors[i].number);sr.add(radioSensors[i].location);sr.add(radioSensors[i].value);
        JsonArray& sraddr = sr.createNestedArray();
        for(int ii = 0; ii < 10; ii++) 
            sraddr.add(radioSensors[i].address[ii]);
        //sr.add(radioSensors[i].address);
        sr.add(radioSensors[i].alarm);sr.add(radioSensors[i].energy);sr.add(radioSensors[i].humidity);
      }
      jsonansw.printTo(m, jsonansw.measureLength()+1);
      return jsonansw.measureLength()+1; 
    }//end of get radio sensors   
    if(req == "GET_WIRED"){
      if(json.containsKey("REFRESH")){
        reset_wired_sensors();
      }
      // answer     {"MAC":"....", "WIRED":[[number; location[30]; address[8];]......]} 4 subarray 
      JsonArray& R = jsonansw.createNestedArray("WIRED");
      for (int i = 0; i < 4; i++){
        JsonArray& sw = R.createNestedArray();
        sw.add(wiredSensors[i].number);sw.add(wiredSensors[i].location);sw.add(wiredSensors[i].value);
        JsonArray& swaddr = sw.createNestedArray();
        for(int ii = 0; ii < 10; ii++) 
            swaddr.add(wiredSensors[i].address[ii]);
        //sw.add(wiredSensors[i].address);
        sw.add(wiredSensors[i].alarm);
      }
      jsonansw.printTo(m, jsonansw.measureLength()+1);
      return jsonansw.measureLength()+1; 
    }//end of get wired sensors
    //              {"MAC":"....", "REQ":"GET_ALARMS", "FROM":"....."}
    if(req == "GET_ALARMS"){
      // answer     {"MAC":"....", "ALARMS":[uint8_t sensor; float value; int type; int repeat; char message[50];]}
      JsonArray& R = jsonansw.createNestedArray("ALARMS");
      for (int i = 0; i < 8; i++){
        JsonArray& sr = R.createNestedArray();
        sr.add(alarms[i].sensor);sr.add(alarms[i].value);sr.add(alarms[i].type);
        sr.add(alarms[i].repeat);sr.add(alarms[i].message);sr.add(alarms[i].active);
      }
      jsonansw.printTo(m, jsonansw.measureLength()+1);
      return jsonansw.measureLength()+1; 
    }//end of get alarms
    //              {"MAC":"....", "REQ":"GET_PARAMS", "FROM":"....."} 
    if(req == "GET_PARAMS"){  
      // answer     {"MAC":"....", "PARAMS":[name_of_dev, bonjourserver, bonjourport, emails]};
      JsonArray& R = jsonansw.createNestedArray("PARAMS");
      R.add(name_of_dev);R.add(bonjourserver);R.add(bonjourport);R.add(emails);R.add(repeat_alarm_sensors);
      R.add(localip);
      jsonansw.printTo(m, jsonansw.measureLength()+1);
      return jsonansw.measureLength()+1; 
    }//end of Get params
    if(req == "GET_TIME"){
      JsonArray& R = jsonansw.createNestedArray("TIME");
      unsigned long tnow = now();
      Serial.print("Now is:");Serial.println(tnow);
      R.add(tnow);R.add(timeZone);R.add(dayLight);
      jsonansw.printTo(m, jsonansw.measureLength()+1);
      return jsonansw.measureLength()+1; 
    }//end of get time
    
    //for another type return error
    jsonansw["ANS"] = "ERROR";
    jsonansw.printTo(m, jsonansw.measureLength()+1);
    return jsonansw.measureLength()+1;
    }// end of success parsed json message
    
    //not a valid json request 
    return 0;
}

//------------------------------------------------------------------------------------------------------------------------------- 
void Thermostat::reset_wired_sensors(){//called from getwiredsensors or reset button
  for(int i = 0; i < dallassensors->getDeviceCount(); i++)
      if(!dallassensors->getAddress(wiredSensors[i].address, i)){
          #if defined(debug_t)
              {Serial.print("Unable to find address for Device ");
               Serial.println(i);
              }
          #endif
          wiredSensors[i].number = 0;
      }else{
          wiredSensors[i].number = i + 10;
          sprintf(wiredSensors[i].location, "Device %d", i+1);
      }
      
}
//------------------------------------------------------------------------------------------------------------------------------- 
void Thermostat::getAnaOut(uint16_t * ao){
  // formula Vout=pct/403.7678 or  pct = Vout*403.7678
  
  // value off SETPOINT TEMPERATURE
  // scale from 10 - 30
  //            0V - 10V
  float v = ((p.value - 10) / 2 ) * 403.7678;
  ao[0] = v;      
  // value off ACTUAL TEMPERATURE
  v = ((current_sensor(p.sensor).value - 10) / 2 ) * 403.768;
  ao[1] = v;
  // value off SETPOINT HUMIDITY
  v = current_sensor(p.sensor).humidity ;
  v = v / 10 * 403.768;
  ao[2] = v;
  // value off ACTUAL HUMIDITY
  v = humidity;
  v = v / 10 * 403.768;
  ao[3] = v; 
  }
//------------------------------------------------------------------------------------------------------------------------------- 
void Thermostat::set_analogic_outs(uint16_t* ao){
  for(uint8_t i =0; i<4; i++)
    {
      Wire.beginTransmission(0x60 + i);
      Wire.write((uint8_t) ((ao[i] >> 8) & 0x0F));   // MSB: (D11, D10, D9, D8) 
      Wire.write((uint8_t) (ao[i]));  // LSB: (D7, D6, D5, D4, D3, D2, D1, D0)
      int err = Wire.endTransmission();
      
      
    }
}
//------------------------------------------------------------------------------------------------------------------------------- 
void Thermostat::checkforI2Cradio(){
  //Wire.beginTransmission(8);
  //Wire.write(0x01);
  //int err = Wire.endTransmission();
  byte rez=0;
  do{
      if(Wire.requestFrom(8, 16))//if request succeeded
         {byte buf[16];
          //b0( >0 = valid., 0 invalid), (b1 = buflen), (b2-15) data
          for (byte q =0; q < 16; q++)
              buf[q] = Wire.read();
          rez = buf[0];
          buflen = buf[1];
         //rez > 0? Serial.println("I2C radio has data."):Serial.println("No data on I2C radio");
         memcpy(sbuf, buf+2, 14);
         if(buflen > 13 && rez > 0){//if it has corect length 
             int sensorindex = radioSensorIndex(sbuf);
             #if defined(debug_t)
                 Serial.printf("Receive data From: %02x:%02x:%02x:%02x:%02x:%02x:%02x:%02x, Temp:%02x:%02x, Batt:%02x:%02x, Hygro%02x:%02x", 
                           sbuf[0], sbuf[1], sbuf[2], sbuf[3], sbuf[4], sbuf[5], sbuf[6], sbuf[7], sbuf[8], sbuf[9], sbuf[10], sbuf[11], sbuf[12], sbuf[13]);
                 Serial.printf("With length %d", buflen);
                 Serial.printf("SensorIndex:%d\n", sensorindex);
             #endif
             if( sensorindex >= 0){
                  int val;
                  val = ((int)sbuf[9])<<8 | ((int)sbuf[8]);
                  radioSensors[sensorindex].value  = val;
                  radioSensors[sensorindex].value /= 10;
                  val = ((int)sbuf[11])<<8 | ((int)sbuf[10]);
                  radioSensors[sensorindex].energy  = val;
                  float formula;
                  formula  = VREF * ( RSUS + RJOS );
                  formula /= RJOS * 1024 * 1000;
                  radioSensors[sensorindex].energy *= formula;
                  if(buflen >13) // sensor with humidity
                     {
                      val = ((int)sbuf[13])<<8 | ((int)sbuf[12]);
                      radioSensors[sensorindex].humidity = val;
                    }
                  radioSensors[sensorindex].timestamp = now();
                  #if defined(debug_t)
                      Serial.printf("Sensor new data on: %02d.%02d.%04d - %02d:%02d:%02d\n",day(), month(), year(), hour(), minute(), second());
                      Serial.print("Sensor Value:");Serial.println(radioSensors[sensorindex].value);
                      Serial.print("Sensor Energy:");Serial.println(radioSensors[sensorindex].energy);
                      Serial.printf("Sensor Humidity: %02.f\n", radioSensors[sensorindex].humidity);
                  #endif
                 }  
             }
         }
      else
         {
          Serial.println("I2C radio failure ... maybe slave wasn't ready or not connected");
         }
   
  }while(rez > 0);
  /*
  switch(err){
           case 0:
                delay(1); // Comment this out and watch requestFrom() fails.
                if(Wire.requestFrom(8, 1) == 1) {
                    uint8_t count = Wire.read();
                    Serial.print("Count data= ");Serial.print(count); Serial.print(" On:");Serial.println(millis());
                    
                } else {
                    Serial.println("error in return");
                    err = 4;
                }

                break;

            case 1:
                Serial.println("Data too long");
                break;

            case 2:
                Serial.println("NACK on address");
                break;

            case 3:
                Serial.println("NACK on data");
                break;

            case 4:
                Serial.println("Some error");
                break;
            }
 */  

}
//------------------------------------------------------------------------------------------------------------------------------- 
