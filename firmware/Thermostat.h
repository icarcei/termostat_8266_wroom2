#include <FS.h>
#include <TimeLib.h>
#include <ESP8266WiFi.h>  
#include <ArduinoJson.h>
#include <RH_ASK.h>
#include <SPI.h> // Not actually used but needed to compile
#include <OneWire.h>
#include <DallasTemperature.h>
#include <Wire.h>


#ifndef THERMOSTAT
#define THERMOSTAT
#define debug_t
#define ONE_WIRE_BUS 4

#define RSUS 56
#define RJOS 27
#define VREF 1100



// Alarm structure
// sensor (1-9) sensor number
// value to triger alarm
// type -1 not used, 0 minimal, 1 maximal, 10 minimal second value, 11 maximal second value 
typedef struct alarm{uint8_t sensor; float value; int type; int repeat; unsigned long sent;boolean active; char message[50];}Alarm;

// number (1-9) radio sensor (11-18) 1 wire sensor 
// location
// value
// energy - battery voltage for radio sensor
// timestamp - timestamp from last reading
// address of sensor in hexadecimal                                                           
typedef struct temperatureSensor{uint8_t number; char location[30]; float value; float energy;float humidity; uint8_t address[8]; time_t timestamp; boolean alarm; time_t alarm_sent_at;}TemperatureSensor;

// Program structure
// h - hour to start program
// m - minute to star program
// value - value for thermostat
// senzor (0-9) wich sensor is used 0 = program dezactivated
typedef struct program{uint8_t h; uint8_t m; float value; uint8_t sensor;}Program;

typedef struct emaildata{String subject; String message; String addresses; bool must;}Emaildata; 




class Thermostat
{
    
    private:
        Program default_program;      //default program
        Program p;                    //current program
        float value;                  //value for thermostat
        int humidity;                 //humidity value setpoint
        int state;                    // 0 - Off minim 5 Celsius
                                      // 1 - Manual between 5 - 30
                                      // 2 - Forced only for current program between 5 - 30
                                      // 3 - Automat from Monday to Sunday, without weekend
                                      // 4 - Automat from Monday to Saturday, Sunday is weekend
                                      // 5 - Automat from Monday to Friday, Saturday and Sunday is weekend
        uint8_t sensorused;           // sensor to use in states 0-2
                    
        time_t lastchange;            // time from last state change
        time_t last_time;
        static Thermostat* _instance; // instance off Thermostate object 
        unsigned long lastcheck;      // last time when values has been checked
        boolean  startup;             // variable created true on create after 360 sec is false
        int current_program_number;   // 
        int program_number_on_forced;
        int previos_state;
        
        RH_ASK* driver;//(2000, 5, 4, 0); 
        uint8_t sbuf[RH_ASK_MAX_MESSAGE_LEN];
        uint8_t buflen = sizeof(sbuf);
        char macaddr[21]; 
        
        
        OneWire* oneWire;
        DallasTemperature *dallassensors;
        
        
        
        
    protected:
        Thermostat(char* mac);
        bool energized;
        bool radioSensorsAlarm;
        bool wiredSensorsAlarm;
        bool alarmsActive;
        
    public:
          // 6 daily programs for week time
         Program week_programs[6];

          // 6 daily programs for weekend time
         Program weekend_programs[6];

         //8 maxim alarms can be trigered
         Alarm alarms[8];
         Emaildata emailsdata;

         // 9 radio sensors maxim
         TemperatureSensor radioSensors[9];
         // 4 1 wire sensors maxim
         TemperatureSensor wiredSensors[4];

         int8_t timeZone;
         bool dayLight;
         bool timeChange;
         bool paramchange;
         bool humidity_en;

         char name_of_dev[50];//     ="IoT SUN Thermostat";
         char http_username[30];//   = "admin";
         char http_password[30];//   = "admin";
         char bonjourserver[50];//   = "carcei.go.ro";
         int bonjourport             = 5000;
         char emails[100];//         = "icarcei@gmail.com";
         int repeat_alarm_sensors    = 15;
         char localip[20];

         bool LoadParams();
         bool SaveParams();
         bool LoadState();
         bool SaveState();
         bool LoadRadioSensors();
         bool SaveRadioSensors();
         bool LoadWiredSensors();
         bool SaveWiredSensors();
         bool LoadAlarms();
         bool SaveAlarms();
         bool LoadPrograms();
         bool SavePrograms();
         static Thermostat* Instance(char* mac);

         int Status();
         bool SetOff(uint8_t s=1);
         bool SetManual(float val, uint8_t s=1);
         bool SetForced(float val, uint8_t s=1);
         bool SetAuto(uint8_t weekend_days=0);
         TemperatureSensor current_sensor();
         TemperatureSensor current_sensor(int nr);

         int radioSensorIndex(uint8_t* addr); 

         Program current_program();
         bool is_energized();
         bool isEnergized(){return energized;};
         void checkforalarm();
         void checkforradio();
         void check();
         bool sendEmail(Alarm a);

         float getValue(){return value;};
         int getHumidity(){return humidity;};

          uint16_t values_out[4];
         void getAnaOut(uint16_t * ao);
         
         int check_message(char* m);
         void reset_wired_sensors();
         void set_analogic_outs(uint16_t* ao);  
         void checkforI2Cradio();
         void set_temporary_manual(){state=1;};
          
};

#endif

