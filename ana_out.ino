#include <Wire.h>

bool dac[4];

void check_if_exist_I2C() {
  byte error, address;
  int nDevices;
  nDevices = 0;
  for (address = 1; address < 127; address++ )  {
    // The i2c_scanner uses the return value of
    // the Write.endTransmisstion to see if
    // a device did acknowledge to the address.
    Wire.beginTransmission(address);
    error = Wire.endTransmission();

    if (error == 0){
      Serial.print("I2C device found at address 0x");
      if (address < 16)
        Serial.print("0");
      Serial.print(address, HEX);
      Serial.println("  !");

      nDevices++;
    } else if (error == 4) {
      Serial.print("Unknow error at address 0x");
      if (address < 16)
        Serial.print("0");
      Serial.println(address, HEX);
    }
  } //for loop
  if (nDevices == 0)
    Serial.println("No I2C devices found");
  else
    Serial.println("**********************************\n");
  //delay(1000);           // wait 1 seconds for next scan, did not find it necessary
}

void setup_analogic_out(void){
  Wire.begin(2,14);
  for(uint8_t i =0; i<4; i++)
    {
      Wire.beginTransmission(0x60 + i);
      int err = Wire.endTransmission();
      if (err == 0){
        dac[i] = true;
        Serial.printf("Device found at address %d. :)\n", 0x60+i);
      } 
      else{
        dac[i] = false;
        Serial.printf("Device NOT found at address %d. :(\n", 0x60+i);
      }
      
    }

    check_if_exist_I2C();
    
}

void set_analogic_outs(float a0, float a1, float a2, float a3){
  float v;
  uint16_t aao[4];
  v = 4096*a0/100;
  aao[0] = v;
  v = 4096*a1/100;
  aao[1] = v;
   v = 4096*a2/100;
  aao[2] = v;
  v = 4096*a3/100;
  aao[3] = v;
  set_analogic_outs(aao); 
}
void set_analogic_outs(uint16_t* ao){
  for(uint8_t i =0; i<4; i++)
    {
      Wire.beginTransmission(0x60 + i);
      Wire.write((uint8_t) ((ao[i] >> 8) & 0x0F));   // MSB: (D11, D10, D9, D8) 
      Wire.write((uint8_t) (ao[i]));  // LSB: (D7, D6, D5, D4, D3, D2, D1, D0)
      int err = Wire.endTransmission();
      
      
    }
}
void set_analogic_outs(float* ao){
  // 0 - 100 %
  float v;
  uint16_t aao[4];
  v = 4096*ao[0]/100;
  aao[0] = v;
  v = 4096*ao[1]/100;
  aao[1] = v;
   v = 4096*ao[2]/100;
  aao[2] = v;
  v = 4096*ao[3]/100;
  aao[3] = v;
  set_analogic_outs(aao);
}

void set_analogic_outs(uint16_t a0, uint16_t a1, uint16_t a2, uint16_t a3){
 
      Wire.beginTransmission(0x60);
      Wire.write((uint8_t) ((a0 >> 8) & 0x0F));   // MSB: (D11, D10, D9, D8) 
      Wire.write((uint8_t) (a0));  // LSB: (D7, D6, D5, D4, D3, D2, D1, D0)
      int err0 = Wire.endTransmission();
      Wire.beginTransmission(0x61);
      Wire.write((uint8_t) ((a1 >> 8) & 0x0F));   // MSB: (D11, D10, D9, D8) 
      Wire.write((uint8_t) (a1));  // LSB: (D7, D6, D5, D4, D3, D2, D1, D0)
      int err1 = Wire.endTransmission();
      Wire.beginTransmission(0x62);
      Wire.write((uint8_t) ((a2 >> 8) & 0x0F));   // MSB: (D11, D10, D9, D8) 
      Wire.write((uint8_t) (a2));  // LSB: (D7, D6, D5, D4, D3, D2, D1, D0)
      int err2 = Wire.endTransmission();
      Wire.beginTransmission(0x63);
      Wire.write((uint8_t) ((a3 >> 8) & 0x0F));   // MSB: (D11, D10, D9, D8) 
      Wire.write((uint8_t) (a3));  // LSB: (D7, D6, D5, D4, D3, D2, D1, D0)
      int err3 = Wire.endTransmission();

}
