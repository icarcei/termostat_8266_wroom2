
#include <FS.h>

#include <TimeLib.h>
#include <ESP8266WiFi.h>          //https://github.com/esp8266/Arduino
#include <WiFiClient.h>

//needed for library
#include <DNSServer.h>
//#include <ESP8266WebServer.h>
#include <ESPAsyncWebServer.h>
//#include <WiFiManager.h>         //https://github.com/tzapu/WiFiManager
#include <ESPAsyncWiFiManager.h>
#include <ArduinoJson.h> 
#include <WiFiUdp.h>
#include "Gsender.h"
//#include <rBase64.h>
#include <ESPAsyncTCP.h>
#include <vector>
#include "Thermostat.h"
#include <SyncClient.h>


#define led           13
#define rel1          12
#define rel2          15
#define standalone    16
#define in0            0

#include <Ticker.h>

Ticker cliper;
Ticker scliper;

Thermostat *thermostat;

void clip(){
  int state = digitalRead(led);
  digitalWrite(led, !state);
}

int sclip_count = 0;
int sclip_max_count = 1;
void ssclip(){
  if(sclip_count > sclip_max_count){
    sclip_count = 0; digitalWrite(led, 0);
    scliper.detach();
  }
  else{
    clip();
    sclip_count++;
  }
}
void sclip(){
  scliper.attach(0.1,ssclip); 
}
void stinge(){
  cliper.detach();
  digitalWrite(led, 0);
  cliper.attach(3, sclip);
}
void aprinde(int sec){
  cliper.detach();
  scliper.detach();
  digitalWrite(led, 1);
  cliper.attach(sec, stinge);
}

//flag for saving data
bool shouldSaveConfig = false;

//callback notifying us of the need to save config
void saveConfigCallback () {
  Serial.println("Should save config");
  shouldSaveConfig = true;
}


String getMacAddress() {
  uint8_t baseMac[6];
  // Get MAC address for WiFi station
  WiFi.macAddress(baseMac);
  char baseMacChr[18] = {0};
  sprintf(baseMacChr, "%02X:%02X:%02X:%02X:%02X:%02X", baseMac[0], baseMac[1], baseMac[2], baseMac[3], baseMac[4], baseMac[5]);
  return String(baseMacChr);
}

// variable for validate communication
bool cantcomm = true;
bool alone    = false;
bool temporary= false;
//numai pentru Async
static std::vector<AsyncClient*> clients;//list to hold clients

//AsyncClient* sclient;//client for connect to bonjourserver
SyncClient* sclient;

AsyncServer* ss = new AsyncServer(4321);

AsyncWiFiManager *awfm=NULL;

DNSServer dns;

unsigned long lastwebrequest;
char startupackaddress[50];
String macaddr;

void setup() {
  
    pinMode ( led, OUTPUT );
    digitalWrite ( led, 0 );
    pinMode ( rel1, OUTPUT );
    digitalWrite ( rel1, 0 );
    pinMode ( rel2, OUTPUT );
    digitalWrite ( rel2, 0 );
    //pinMode (pwm1, OUTPUT);
    //pinMode (pwm2, OUTPUT);
    pinMode (standalone, INPUT);
    pinMode (in0, INPUT_PULLUP);
    // put your setup code here, to run once:
    Serial.begin(115200);

    macaddr = getMacAddress();
    Serial.println(macaddr);
    Serial.flush();
    

    //read configuration from FS json
  Serial.println("mounting FS...");

  if (SPIFFS.begin()) {
    
     thermostat = Thermostat::Instance((char*) macaddr.c_str());
     if (SPIFFS.exists("/sys.json")) {
         //file exists, reading and loading
         File sysFile = SPIFFS.open("/sys.json", "r");
         if (sysFile) {
             size_t size = sysFile.size();
             // Allocate a buffer to store contents of the file.
             std::unique_ptr<char[]> buf(new char[size]);
             sysFile.readBytes(buf.get(), size);
             sysFile.close();
             DynamicJsonBuffer jsonBuffer;
             JsonObject& json = jsonBuffer.parseObject(buf.get());
             if (json.success()) {
                 if(json.containsKey("temporary")) 
                    temporary = json["temporary"];
             }
         }
         if(temporary)
            {Serial.println("Starting temporary......");
             thermostat->set_temporary_manual();
             sclip_max_count=3;
             }
     }
  
  } else {
    Serial.println("failed to mount FS");
  }
  //end read

  // if we want standalone - not connected to internet
  if((digitalRead(standalone) == 0) || (temporary)){
    setAlone();
    pinMode (in0, INPUT_PULLUP);
  }else{
    AsyncWebServer server(80);
    
   // The extra parameters to be configured (can be either global or just in the setup)
   // After connecting, parameter.getValue() will get you the configured value
   // id/name placeholder/prompt default length

   AsyncWiFiManagerParameter custom_startupackaddress("adress@domain.do", "StartUp Ack Email Address", startupackaddress, 50);

    //WiFiManager
    //Local intialization. Once its business is done, there is no need to keep it around
    //WiFiManager wifiManager;
    AsyncWiFiManager wifiManager(&server, &dns);
    WiFi.hostname(thermostat->name_of_dev);
    //set config save notify callback
    bool shouldSave = false;
    wifiManager.setSaveConfigCallback(saveConfigCallback);
    //add all your parameters here
    wifiManager.addParameter(&custom_startupackaddress);
 
     //reset saved settings
    //wifiManager.resetSettings();
    
    //sets timeout until configuration portal gets turned off
    //useful to make it all retry or go to sleep
    //in seconds
    //wifiManager.setTimeout(180);
    wifiManager.setConfigPortalTimeout(121);
    //fetches ssid and pass from eeprom and tries to connect
    //if it does not connect it starts an access point with the specified name
    //here  "AutoConnectAP"
    //and goes into a blocking loop awaiting configuration
    awfm = &wifiManager;
    ss->onClient(&handleNewClient, ss);
    ss->begin();
    cliper.attach(0.8, clip);

    wifiManager.setDebugOutput(false);
    pinMode (in0, INPUT_PULLUP);
    if(!wifiManager.autoConnect("IoT_SUN_Portal")){
    //or use this for auto generated name ESP + ChipID
    //wifiManager.autoConnect();
       Serial.println("failed to connect try stand alone for one hour..RESET NOW");
       DynamicJsonBuffer jsonBuffer;
       JsonObject& json              = jsonBuffer.createObject();
       json["temporary"]           = true;
       File sysFile = SPIFFS.open("/sys.json", "w");
       if (!sysFile) {
           Serial.println("failed to open config file for writing");
       }else{
           json.prettyPrintTo(Serial);
       json.printTo(sysFile);
       sysFile.close();
       }  
       delay(3000);
       ESP.reset();
       delay(5000);
    }else{
       DynamicJsonBuffer jsonBuffer;
       JsonObject& json  = jsonBuffer.createObject();
       json["temporary"] = false;
       File sysFile = SPIFFS.open("/sys.json", "w");
       if (!sysFile) {
           Serial.println("failed to open config file for writing");
       }else{
           json.prettyPrintTo(Serial);
       json.printTo(sysFile);
       sysFile.close();
       } 
    }

    if(shouldSaveConfig){
      strcpy(startupackaddress, custom_startupackaddress.getValue());
      
    }
    //if you get here you have connected to the WiFi
    Serial.println("connected...yeey :)");

    Serial.println("local ip");
    Serial.println(WiFi.localIP());
    Serial.println(WiFi.gatewayIP());
    Serial.println(WiFi.subnetMask());
    strcpy(thermostat->localip, WiFi.localIP().toString().c_str());
    cliper.detach();
    
    initializare_timp();

 awfm=NULL;
    Serial.println(getMacAddress());


    Serial.println("aes_init()");
    aes_init();

    sclient = new SyncClient();
    //setupBonjour();
 }
 cliper.attach(3, sclip);
  
 
 setup_analogic_out();
 //set_analogic_outs((float) 50,(float) 25.,(float) 50., (float)75.);
 Serial.print("free heap: "); Serial.println(ESP.getFreeHeap());
 pinMode (in0, INPUT_PULLUP);
}

unsigned long lasttimetemp;
unsigned long lastSerial;
unsigned long ts_min_email, ts_max_email;
unsigned long lastcheckwifi;
unsigned long timestart;
int last_alarms_system_check;
float temperatura;
boolean alarm, email_ok;


void setAlone(){
       WiFi.softAP(thermostat->name_of_dev, "thermostat");
       IPAddress myIP = WiFi.softAPIP();
       Serial.print("AP IP address: ");
       Serial.println(myIP);
       ss->onClient(&handleNewClient, ss);
       ss->begin();
       alone = true;
       timestart = millis();
}

boolean TrimiteEmail(String sub, String mes){
  
  if(millis()-lastwebrequest < 3000)
     {Serial.printf("Mai trebuie sa astept %d ms\n",(3000-(millis()-lastwebrequest)));
      return false;
     }
  if(ss)ss->end();//stop server TCP local
  if(sclient)sclient->stop();//stop client for bonjour
  cantcomm = false;
  //delay(10); 
  Gsender *gsender = Gsender::Instance();    // Getting pointer to class instance
    
    String subject = sub; subject +=thermostat->name_of_dev;//IoT SUN thermostat";
    String Message = mes; 
    if(gsender->Subject(subject)->Send(thermostat->emails, Message)) {
        Serial.println("Message send.");
        if(ss){
            ss->onClient(&handleNewClient, &ss);
            ss->begin();}
        cantcomm = true;
        return true;
    } else {
        Serial.print("Error sending message: ");
        Serial.println(gsender->getError());
          }
          
  if(ss){
      ss->onClient(&handleNewClient, &ss);
      ss->begin();
      }
  cantcomm = true;
  return false;
   
}

//trimite email de alarma proprie
boolean TrimiteEmail(String sub, int i){
  
  if(millis()-lastwebrequest < 3000)
     {Serial.printf("Mai trebuie sa astept %d ms\n",(3000-(millis()-lastwebrequest)));
      return false;
     }
  if(ss)ss->end();//stop server TCP local
  if(sclient)sclient->stop();//stop client for bonjour
  cantcomm = false;
  //delay(10); 
  Gsender *gsender = Gsender::Instance();    // Getting pointer to class instance
    
    String subject = sub; subject +=thermostat->name_of_dev;//IoT SUN thermostat";
    //String Message = mes;
    //thermostat->current_sensor(thermostat->a.sensor) 
    String message;
       message = String(thermostat->alarms[i].message) + String("\r\nAlarm: ") +
                 String(thermostat->alarms[i].value) + String(thermostat->alarms[i].type == 1 ? "Maximal":"minimal") +
                 String("\r\nSensor ") + 
                 String(thermostat->current_sensor(thermostat->alarms[i].sensor).location) + String(": ") +
                 String(thermostat->current_sensor(thermostat->alarms[i].sensor).value) + String("\nTimestamp: ") + 
                 String(thermostat->current_sensor(thermostat->alarms[i].sensor).timestamp);
    char dt[50];
    //Serial.print(message);
    time_t ttt = thermostat->current_sensor(thermostat->alarms[i].sensor).timestamp;
    //Serial.println(ttt);
           if(ttt > 1550486439){
               sprintf(dt,"%02d.%02d.%04d - %02d:%02d:%02d \n",day(ttt),month(ttt),year(ttt),hour(ttt),minute(ttt),second(ttt));  
           }
           else 
               strcpy(dt, "Never receive data.");
    
    Serial.println(dt);
    //SendAlarm(const String &to, const String &nameofdev, const String &message, const String &nameofsensor, float value, float energy, uint8_t humidity, const String &data)
   
    if(gsender->Subject(subject)->SendAlarm(thermostat->emails, thermostat->name_of_dev, thermostat->alarms[i].message, thermostat->alarms[i].value, thermostat->alarms[i].type,
        thermostat->current_sensor(thermostat->alarms[i].sensor).location, thermostat->current_sensor(thermostat->alarms[i].sensor).value, 
        thermostat->current_sensor(thermostat->alarms[i].sensor).energy, thermostat->current_sensor(thermostat->alarms[i].sensor).humidity, dt)) {
        Serial.println("Message send.");
        if(ss){
            ss->onClient(&handleNewClient, &ss);
            ss->begin();}
        cantcomm = true;
        return true;
    } else {
        Serial.print("Error sending message: ");
        Serial.println(gsender->getError());
          }
          
  if(ss){
      ss->onClient(&handleNewClient, &ss);
      ss->begin();
      }
  cantcomm = true;
  return false;
   
}

//trimite email de alarma system
boolean TrimiteEmail(String sub, String mes, TemperatureSensor ts){
  
  if(millis()-lastwebrequest < 3000)
     {Serial.printf("Mai trebuie sa astept %d ms\n",(3000-(millis()-lastwebrequest)));
      return false;
     }
  if(ss)ss->end();//stop server TCP local
  if(sclient)sclient->stop();//stop client for bonjour
  cantcomm = false;
  //delay(10); 
  Gsender *gsender = Gsender::Instance();    // Getting pointer to class instance
    
    String subject = sub; subject +=thermostat->name_of_dev;//IoT SUN thermostat";
    String Message = mes; 
    char dt[50];
    time_t ttt = ts.timestamp;
           if(ttt > 1550486439)
               sprintf(dt,"%02d.%02d.%04d - %02d:%02d:%02d \n",day(ttt),month(ttt),year(ttt),hour(ttt),minute(ttt),second(ttt));  
           else 
               strcpy(dt, "Never receive data.");
    //SendAlarm(const String &to, const String &nameofdev, const String &message, const String &nameofsensor, float value, float energy, uint8_t humidity, const String &data)
    float alarmValue = -100;
    int type=0;
    if(gsender->Subject(subject)->SendAlarm(thermostat->emails, thermostat->name_of_dev, mes, alarmValue, type, ts.location, ts.value, ts.energy, ts.humidity, dt)) {
        Serial.println("Message send.");
        if(ss){
            ss->onClient(&handleNewClient, &ss);
            ss->begin();}
        cantcomm = true;
        return true;
    } else {
        Serial.print("Error sending message: ");
        Serial.println(gsender->getError());
          }
          
  if(ss){
      ss->onClient(&handleNewClient, &ss);
      ss->begin();
      }
  cantcomm = true;
  return false;
   
}

void checkforalarms(void){
  for(int i =0;i<8;i++){//alarms of user
    if( thermostat->alarms[i].active && ((thermostat->alarms[i].repeat == 0 && thermostat->alarms[i].sent == 0) || 
        ( thermostat->alarms[i].repeat > 0 && ( millis() > (thermostat->alarms[i].sent + thermostat->alarms[i].repeat * 60000))) ) ){//verify necesity to send email
       String message;
       message = String(thermostat->alarms[i].message) + String("\r\nAlarm: ") + String(thermostat->alarms[i].value) + String("\r\nSensor ") + 
                 String(thermostat->current_sensor(thermostat->alarms[i].sensor).location) + String(": ") +
                 String(thermostat->current_sensor(thermostat->alarms[i].sensor).value) + String("\nTimestamp: ") + 
                 String(thermostat->current_sensor(thermostat->alarms[i].sensor).timestamp);
       Serial.println("Try to send ALARM email"); 
       Serial.println(message);         
       if(TrimiteEmail("Alarm ", i))
                    thermostat->alarms[i].sent = millis();
         }  
  }

  if(true)//hour() > last_alarms_system_check + 6){// check every 6 hours
  {
     String message="";
     for(int i =0;i<6;i++){//alarms of radio sensors
        if( thermostat->radioSensors[i].alarm && thermostat->radioSensors[i].number > 0)// && now() - thermostat->radioSensors[i].alarm_sent_at > 21600)
        {//Serial.println("Alarm on RadioSensor");
         if(now() - thermostat->radioSensors[i].alarm_sent_at > thermostat->repeat_alarm_sensors * 60 ){
          Serial.println("Try to send SENSOR alarm email");
         if(TrimiteEmail("Radio Sensor Alarm ", "Radio Sensor Alarm ", thermostat->radioSensors[i]))
             thermostat->radioSensors[i].alarm_sent_at = now();
         } 
        }
        
     }
     
     
     message="";
     for(int i =0;i<6;i++){//alarms of wired sensors
        if( thermostat->wiredSensors[i].alarm && thermostat->wiredSensors[i].number > 9 && now() - thermostat->wiredSensors[i].alarm_sent_at > 43200){
           char dt[50];
           
           time_t ttt = thermostat->wiredSensors[i].timestamp;
           if(ttt > 1550486439)
               sprintf(dt,"\nLast data: %02d.%02d.%04d - %02d:%02d:%02d",day(ttt),month(ttt),year(ttt),hour(ttt),minute(ttt),second(ttt));  
           else
               strcpy(dt, "\n Never receive data");
           message = String("\r\nSensor   : ") + String(thermostat->wiredSensors[i].location) +
                     String("\r\nValue    : ") + String(thermostat->wiredSensors[i].value) + 
                     String(dt);
           thermostat->wiredSensors[i].alarm_sent_at = now();
         } 
     }
     if (message.length()>1){
         TrimiteEmail("Wired Sensor Alarm ", message);
     }
     last_alarms_system_check = hour(); 
  }
}

void loop() {
    // put your main code here, to run repeatedly:

 if(WiFi.status() != WL_CONNECTED){
      //Serial.println("Wireless is NOT Connected."); 
 }else{
      lastcheckwifi = millis();
      if(!alone){
         checkTcpcomm();
         checkforalarms();
      }
 }

 if(millis() - lastcheckwifi > 3600000 && digitalRead(standalone)){
     Serial.println("Wireless conectivity has been lost for more that one hour ... reboot now!");
     //delay(3000);
     //ESP.restart();
     lastcheckwifi = millis();
 }
 if(temporary){if(((millis() - timestart) > 600000)||(!digitalRead(in0))){
     Serial.println("Temporary reset to look for network......!");
     DynamicJsonBuffer jsonBuffer;
       JsonObject& json  = jsonBuffer.createObject();
       json["temporary"] = false;
       File sysFile = SPIFFS.open("/sys.json", "w");
       if (!sysFile) {
           Serial.println("failed to open config file for writing");
       }else{
           json.prettyPrintTo(Serial);
       json.printTo(sysFile);
       sysFile.close();}
     delay(3000);
     ESP.restart();
     delay(5000);  
     } 
 }
 thermostat->check();
 //Standard use
 digitalWrite(rel1, thermostat->isEnergized());
 // modified use (with analogic outs on i2c)
 //digitalWrite(rel2, thermostat->isEnergized());
 //digitalWrite(rel1, thermostat->humidity_en);
 
 if(thermostat->timeChange){
     setTimezoneAndDaylight(thermostat->timeZone, thermostat->dayLight);
     thermostat->timeChange = false;
 }

 //---------check for memory leak--------------------------------------------------------------------
 
 if(millis() - lastSerial > 3000 ){
 log_free_stack("loop-text-after in");
 //Serial.print("Clients number:");Serial.println(clients.size());
 for (int j =0; j< clients.size(); j++){
   if((AsyncClient*)clients[j] != NULL){
     Serial.print("Clientul ");Serial.print(j); Serial.println(" nu este NULL");
     if((AsyncClient*)clients[j]->freeable()){
         AsyncClient* a =clients[j];
         a->close(true);
         a->free();
         delete a;
         clients.erase(clients.begin()+j); 
         }
     }
   }
 clients.shrink_to_fit();
 lastSerial = millis();
 }
 
 //------end of check for memory leak--------------------------------------------------------------------
 /*if(!digitalRead(in0)|| thermostat->paramchange){// reset WiFi
     sclip_max_count = 255;
     AsyncWiFiManager wifiManager(&server,&dns);
     wifiManager.resetSettings();
     ESP.reset();
     delay(1000);
     }
 */   
}
//------end of loop-------------------------------------------------------------------------------------------------------------------

/* clients events */
static void handleError(void* arg, AsyncClient* client, int8_t error) {
  Serial.printf("\n connection error %s from client %s \n", client->errorToString(error), client->remoteIP().toString().c_str());
  lastwebrequest = millis();
}

static void handleData(void* arg, AsyncClient* client, void *data, size_t len) {
  Serial.printf("\n data received from client %s \n", client->remoteIP().toString().c_str());
  Serial.write((uint8_t*)data, len);
  Serial.printf("\n len of data is: %d is %s valid data.\n", len, len > 16 ? "":"not");
  if(awfm!=NULL){
   DynamicJsonBuffer jsonBuffer;
   JsonObject& json = jsonBuffer.parseObject((uint8_t*)data);
   //json.printTo(Serial);
   if (json.success()) {
    
    Serial.println("Setare doar Wifi");char reply[50];
    if(json.containsKey("WiFiSSID")&& json.containsKey("WiFiPASS"))
             {
              sprintf( reply,"{\"ans\":\"ok\", \"mac\":\"%s\"}\n", getMacAddress().c_str());
              awfm->setSSIDandPASSWORD(json["WiFiSSID"],json["WiFiPASS"]);
              aprinde(3); 
             }
           else
             {Serial.println("Nu esti in configurare wifi");
              strcpy(reply, "{\"ans\":\"error\"}\n");
             }
   if (client->space() > 50 && client->canSend()) {
            client->add(reply, strlen(reply));
            client->send();
           } 
   }
 else{Serial.println("Error on parse JSON");} 
  }
 else{
  char a[1000];
  byte enc_iv[16] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
  s_decrypt((char *)data, len, enc_iv, a); 
  Serial.println("");
  Serial.println(a);
  int l =thermostat->check_message(a);

  Serial.printf("Answer: %s \n witdt length:%d", a, l);
 
 if(l >0){
  // Encrypt
    byte enc_iv2[16] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; // iv_block gets written to, provide own fresh copy...
    byte rez[500];
    Serial.print("message length = ");
    int lll = s_encrypt(a, enc_iv2, rez);
    Serial.println(lll);
    for (int jj = 0; jj< lll;jj++){
      if(rez[jj]<16)
         Serial.print("0");
      Serial.print(rez[jj], HEX);
    }
    Serial.println("");
    
  Serial.print("space for sending:");Serial.println(client->space());
  Serial.println(client->canSend()?"Pot Trimite":"Nu pot Trimite");
  
  if (client->space() > lll && client->canSend()) 
     { client->add((char *)rez, lll);//a, strlen(a));
            client->send();
            Serial.println("Trimis?");
           }
  else{
    
      }
           
 }
           Serial.print("free heap after transmission: "); Serial.println(ESP.getFreeHeap());
 }

 lastSerial = millis();
 
 lastwebrequest = millis();
}

static void handleDisconnect(void* arg, AsyncClient* client) {
  Serial.printf("\n client %s disconnected \n", client->remoteIP().toString().c_str());
  //delete(client);
  lastwebrequest = millis();
}

static void handleTimeOut(void* arg, AsyncClient* client, uint32_t time) {
  Serial.printf("\n client ACK timeout ip: %s \n", client->remoteIP().toString().c_str());
  lastwebrequest = millis();
}


/* server events */
static void handleNewClient(void* arg, AsyncClient* client) {
  Serial.printf("\n new client has been connected to server, ip: %s and PORT %d", client->remoteIP().toString().c_str(), client->remotePort());
  // add to list
  
  clients.push_back(client);
  
  // register events
  client->onData(&handleData, NULL);
  client->onError(&handleError, NULL);
  client->onDisconnect(&handleDisconnect, NULL);
  client->onTimeout(&handleTimeOut, NULL);
}
