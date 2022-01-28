#include "AESLib.h"

extern "C" {
#include "user_interface.h"
#include <cont.h>
  extern cont_t g_cont;
}

AESLib aesLib;

String plaintext = "HELLO WORLD!";

char cleartext[256];
char ciphertext[512];

// AES Encryption Key
byte aes_key[] = { 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30 };

// General initialization vector (you must use your own IV's in production for full security!!!)
byte aes_iv[N_BLOCK] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

// Generate IV (once)
void aes_init() {
  Serial.println("gen_iv()");
  aesLib.gen_iv(aes_iv);
  // workaround for incorrect B64 functionality on first run...
  Serial.println("encrypt()");
  Serial.println(encrypt(strdup(plaintext.c_str()), aes_iv));
}

String encrypt(char * msg, byte iv[]) {  
  int msgLen = strlen(msg);
  Serial.print("msglen = "); Serial.println(msgLen);
  char encrypted[2 * msgLen]; // AHA! needs to be large, 2x is not enough
  aesLib.encrypt64(msg, encrypted, aes_key, iv);
  return String(encrypted);
}

int s_encrypt(char * msg, byte iv[], byte * out){
  AES aes;
  aes.do_aes_encrypt((byte *)msg, strlen(msg)+1, out, aes_key, 128, iv);
  return aes.get_size();
}

int s_decrypt(char * msg, int msglen, byte iv[], char * out){
  AES aes;
  aes.do_aes_decrypt((byte *)msg, msglen, (byte*)out, aes_key, 128, iv);
  return aes.get_size();
}

String decrypt(char * msg, byte iv[]) {
  unsigned long ms = micros();
  int msgLen = strlen(msg);
  char decrypted[msgLen]; // half may be enough
  aesLib.decrypt64(msg, decrypted, aes_key, iv);
  return String(decrypted);
}

/* non-blocking wait function */
void wait(unsigned long milliseconds) {
  unsigned long timeout = millis() + milliseconds;
  while (millis() < timeout) {
    yield();
  }
}

void log_free_stack(String tag) {
  extern cont_t g_cont;
  register uint32_t *sp asm("a1");
  unsigned long heap = system_get_free_heap_size();
  Serial.printf("[%s] STACK U=%4d ", tag.c_str(), cont_get_free_stack(&g_cont));
  Serial.printf("F=%4d ", 4 * (sp - g_cont.stack));
  Serial.print("H="); Serial.println(heap);
}
