#include <ESP8266WiFi.h>

#include "ESPAsyncTCP.h"
#include "SyncClient.h"



char tcpserver[50]   = "89.121.205.44";//"carcei.go.ro";
unsigned int tcpport = 50002;//55000;

unsigned long lastconntry;
unsigned long lastupdate;
unsigned long timefromanswer;
char lastrequest[800];

bool answered = true;

bool needtoanswer = false;

void checkTcpcomm(void){
      if(!cantcomm || alone)
         return;
      if(!needtoanswer && (millis()-lastupdate < 1000))
          return;
      if(!sclient->connected()){ 
        sprintf(lastrequest,"{\"MAC\":\"%s\", \"REQ\":\"GET_STATUS\"}\n", macaddr.c_str());
        thermostat->check_message(lastrequest);
          if(!sclient->connect(thermostat->bonjourserver, thermostat->bonjourport)){
              Serial.println("Conection to BonjourServer has failed");
              lastupdate = millis();
              return;
          }else
              Serial.println("Device has connected to Bonjour server");
          
      }
      
      sclient->setTimeout(2);
      needtoanswer = false;
      Serial.println(">SENDING:");Serial.println(lastrequest);
      byte enc_iv[16] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; // iv_block gets written to, provide own fresh copy...
      byte buff_enc[1000];
      int lll = s_encrypt(lastrequest, enc_iv, buff_enc);
      if(sclient->write(buff_enc, lll) == lll){
          lastupdate = millis();
          while(sclient->connected() && sclient->available() == 0){
             delay(1);
          }
          int readed = 0;
          while(sclient->available()){
            buff_enc[readed++] = sclient->read();
          }
          byte enc_iv2[16] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
          int l = s_decrypt((char *)buff_enc, readed, enc_iv2, lastrequest); 
          lastrequest[l] = '\0';
          Serial.printf(" receiving: %s >\n", lastrequest);
          l = thermostat->check_message(lastrequest);
          if(l >0){
                Serial.printf("i must to answer: %s\n", lastrequest);
                needtoanswer = true;
          }else{
            //if(sclient->connected())
            //   sclient->stop();
            sprintf(lastrequest,"{\"MAC\":\"%s\", \"REQ\":\"GET_STATUS\"}\n", macaddr.c_str());
            thermostat->check_message(lastrequest);
           
          }
          
      }else{
            if(sclient->connected())
               sclient->stop();
            
      }
  }

/*
void setupBonjour(void){
  if(!sclient){
     sclient = new AsyncClient;
  }
  sclient->onData(&handleDataServer, sclient);
  sclient->onConnect(&onConnectToServer, sclient);
  sclient->onDisconnect(&handleDisconnectServer, NULL);
  sclient->connect(thermostat->bonjourserver, thermostat->bonjourport);
}

void checkTcpcomm(void){
  if(cantcomm){
      if(sclient){
          if(sclient->connected()){
              if(millis() - lastupdate > 1000)
              {
                  if((millis()-timefromanswer > 5000) || (!answered))
                  {
                       Serial.print("^...STOP");
                       sclient->stop();
                       if(sclient->freeable()){
                           Serial.print("close connection,");
                           sclient->close(true);
                           Serial.print(" freee TCP connection, ");
                           sclient->free();
                           Serial.print(" delete Socket,");
                           delete(sclient);
                           Serial.print(" Start new Socket ^");
                           setupBonjour();
                           lastconntry = millis();
                       }
                  }else{
                       getRequestsFromBonjour(NULL, sclient);
                  }
             }
         }else{
               if(millis()-lastconntry > 10000){
                   setupBonjour();
                   lastconntry = millis();
               }
         }
    }else{
      setupBonjour();
    }
  }
}
*/
void senddata(AsyncClient * client, char* b, int len){
  unsigned long ts = millis();
  while(!client->canSend() && (millis()-ts) < 10) {
            delay(0);
            }
  if(!client->canSend())
     {answered = false;
      Serial.print("@");
     }
  else{  
          client->add((char *)b, len);//a, strlen(a));
          client->send();
          answered = true;
       }
}

/* event callbacks */
static void handleDataServer(void* arg, AsyncClient* client, void *data, size_t len) {
  lastupdate = millis();
  timefromanswer = millis();
  //Serial.printf("\n data received from %s \n", client->remoteIP().toString().c_str());
  char a[1000];
  byte enc_iv[16] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
  int l = s_decrypt((char *)data, len, enc_iv, a); 
  //Serial.println(l);
  a[l] = '\0';
  //Serial.println(a);
  
  l = thermostat->check_message(a);
  if(l >0){
    // Encrypt
    byte enc_iv2[16] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; // iv_block gets written to, provide own fresh copy...
    byte rez[500];
    //Serial.print("message length = ");
    int lll = s_encrypt(a, enc_iv2, rez);
    //Serial.println(lll);
    //for (int jj = 0; jj< lll;jj++){
    //  if(rez[jj]<16)
    //     Serial.print("0");
    //  Serial.print(rez[jj], HEX);
    //}
    //Serial.println("");
    Serial.printf("<Request: %s\n", data);
    Serial.printf(">Answered: %s\n", a);
    Serial.print("space for sending:");Serial.println(client->space());
    Serial.println(client->canSend()?"Pot Trimite":"Nu pot Trimite");
   
    //client->add((char *)rez, lll);//a, strlen(a));
    //client->send();
    senddata(client, (char*)rez, lll);//a, strlen(a));
                    
 }else Serial.print("<");
 lastupdate = millis();
}

void onConnectToServer(void* arg, AsyncClient* client) {
  Serial.printf("\n client has been connected to %s on port %d \n", thermostat->bonjourserver, thermostat->bonjourport);
  getRequestsFromBonjour(arg, client);
}

void getRequestsFromBonjour(void*arg, AsyncClient* client){
  char buff[100];
  lastupdate = millis();
  Serial.print(">");
  if(answered)
     sprintf(buff,"{\"MAC\":\"%s\", \"REQ\":\"GET_STATUS\"}\n", macaddr.c_str());
  else
     strcpy(buff, lastrequest);
  thermostat->check_message(buff);
  //Serial.println("SENDING:");Serial.println(buff);
  byte enc_iv[16] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; // iv_block gets written to, provide own fresh copy...
  byte buff_enc[1000];
  int lll = s_encrypt(buff, enc_iv, buff_enc);
  senddata(client, (char*)buff_enc, lll);
  lastupdate = millis();
  timefromanswer = millis();
}

static void handleDisconnectServer(void* arg, AsyncClient* client) {
  Serial.printf("\n client %s disconnected from Bonjour Server \n", client->remoteIP().toString().c_str());
}
