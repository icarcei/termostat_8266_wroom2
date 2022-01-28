#include "Gsender.h"
Gsender* Gsender::_instance = 0;
Gsender::Gsender(){}
Gsender* Gsender::Instance()
{
    if (_instance == 0) 
        _instance = new Gsender;
    return _instance;
}

Gsender* Gsender::Subject(const char* subject)
{
  delete [] _subject;
  _subject = new char[strlen(subject)+1];
  strcpy(_subject, subject);
  return _instance;
}
Gsender* Gsender::Subject(const String &subject)
{
  return Subject(subject.c_str());
}

bool Gsender::AwaitSMTPResponse(WiFiClientSecure &client, const String &resp, uint16_t timeOut)
{
  uint32_t ts = millis();
  while (!client.available())
  {
    if(millis() > (ts + timeOut)) {
      _error = "SMTP Response TIMEOUT!";
      return false;
    }
  }
  _serverResponce = client.readStringUntil('\n');
#if defined(GS_SERIAL_LOG_1) || defined(GS_SERIAL_LOG_2) 
  Serial.println(_serverResponce);
#endif
  if (resp && _serverResponce.indexOf(resp) == -1) return false;
  return true;
}

String Gsender::getLastResponce()
{
  return _serverResponce;
}

const char* Gsender::getError()
{
  return _error;
}

bool Gsender::Send(const String &to, const String &message)
{
  WiFiClientSecure client;
#if defined(GS_SERIAL_LOG_2)
  Serial.print("Connecting to :");
  Serial.println(SMTP_SERVER);  
#endif
  if(!client.connect(SMTP_SERVER, SMTP_PORT)) {
    _error = "Could not connect to mail server";
    return false;
  }
  if(!AwaitSMTPResponse(client, "220")) {
    _error = "Connection Error";
    return false;
  }

#if defined(GS_SERIAL_LOG_2)
  Serial.println("HELO friend:");
#endif
  client.println("HELO friend");
  if(!AwaitSMTPResponse(client, "250")){
    _error = "identification error";
    return false;
  }

#if defined(GS_SERIAL_LOG_2)
  Serial.println("AUTH LOGIN:");
#endif
  client.println("AUTH LOGIN");
  AwaitSMTPResponse(client);

#if defined(GS_SERIAL_LOG_2)
  Serial.println("EMAILBASE64_LOGIN:");
#endif
  client.println(EMAILBASE64_LOGIN);
  AwaitSMTPResponse(client);

#if defined(GS_SERIAL_LOG_2)
  Serial.println("EMAILBASE64_PASSWORD:");
#endif
  client.println(EMAILBASE64_PASSWORD);
  if (!AwaitSMTPResponse(client, "235")) {
    _error = "SMTP AUTH error";
    return false;
  }
  
  String mailFrom = "MAIL FROM: <" + String(FROM) + '>';
#if defined(GS_SERIAL_LOG_2)
  Serial.println(mailFrom);
#endif
  client.println(mailFrom);
  AwaitSMTPResponse(client);

  String rcpt = "RCPT TO: <" + to + '>';
#if defined(GS_SERIAL_LOG_2)
  Serial.println(rcpt);
#endif
  client.println(rcpt);
  AwaitSMTPResponse(client);

#if defined(GS_SERIAL_LOG_2)
  Serial.println("DATA:");
#endif
  client.println("DATA");
  if(!AwaitSMTPResponse(client, "354")) {
    _error = "SMTP DATA error";
    return false;
  }
  
  client.println("From: <" + String(FROM) + '>');
  client.println("To: <" + to + '>');
  
  client.print("Subject: ");
  client.println(_subject);
  
  client.println("Mime-Version: 1.0");
  client.println("Content-Type: text/html; charset=\"UTF-8\"");
  client.println("Content-Transfer-Encoding: 7bit");
  client.println();
  String body = "<!DOCTYPE html><html lang=\"en\">" + message + "</html>";
  client.println(body);
  client.println(".");
  if (!AwaitSMTPResponse(client, "250")) {
    _error = "Sending message error";
    return false;
  }
  client.println("QUIT");
  if (!AwaitSMTPResponse(client, "221")) {
    _error = "SMTP QUIT error";
    return false;
  }
  return true;
}

bool Gsender::SendAlarm(const String &to, const String &nameofdev, const String &message, float valueAlarm, int type, const String &nameofsensor, float value, float energy, uint8_t humidity, const String &data)
{
  WiFiClientSecure client;
#if defined(GS_SERIAL_LOG_2)
  Serial.print("Connecting to :");
  Serial.println(SMTP_SERVER);  
#endif
  if(!client.connect(SMTP_SERVER, SMTP_PORT)) {
    _error = "Could not connect to mail server";
    return false;
  }
  if(!AwaitSMTPResponse(client, "220")) {
    _error = "Connection Error";
    return false;
  }

#if defined(GS_SERIAL_LOG_2)
  Serial.println("HELO friend:");
#endif
  client.println("HELO friend");
  if(!AwaitSMTPResponse(client, "250")){
    _error = "identification error";
    return false;
  }

#if defined(GS_SERIAL_LOG_2)
  Serial.println("AUTH LOGIN:");
#endif
  client.println("AUTH LOGIN");
  AwaitSMTPResponse(client);

#if defined(GS_SERIAL_LOG_2)
  Serial.println("EMAILBASE64_LOGIN:");
#endif
  client.println(EMAILBASE64_LOGIN);
  AwaitSMTPResponse(client);

#if defined(GS_SERIAL_LOG_2)
  Serial.println("EMAILBASE64_PASSWORD:");
#endif
  client.println(EMAILBASE64_PASSWORD);
  if (!AwaitSMTPResponse(client, "235")) {
    _error = "SMTP AUTH error";
    return false;
  }
  
  String mailFrom = "MAIL FROM: <" + String(FROM) + '>';
#if defined(GS_SERIAL_LOG_2)
  Serial.println(mailFrom);
#endif
  client.println(mailFrom);
  AwaitSMTPResponse(client);

  String rcpt = "RCPT TO: <" + to + '>';
#if defined(GS_SERIAL_LOG_2)
  Serial.println(rcpt);
#endif
  client.println(rcpt);
  AwaitSMTPResponse(client);

#if defined(GS_SERIAL_LOG_2)
  Serial.println("DATA:");
#endif
  client.println("DATA");
  if(!AwaitSMTPResponse(client, "354")) {
    _error = "SMTP DATA error";
    return false;
  }
  
  client.println("From: <" + String(FROM) + '>');
  client.println("To: <" + to + '>');
  
  client.print("Subject: ");
  client.println(_subject);
  
  client.println("Mime-Version: 1.0");
  client.println("Content-Type: text/html; charset=\"UTF-8\"");
  client.println("Content-Transfer-Encoding: 7bit");
  client.println();
  client.println("<!DOCTYPE html><html lang=\"en\">");// + message + "</html>";
  client.println("<head>");
  /*client.println("<style>");
  client.println("body {");
  client.println("width: 700px;");
  client.println("margin: 40px auto;");
  client.println("font-family: 'trebuchet MS', 'Lucida sans', Arial;");
  client.println("font-size: 14px;");
  client.println("color: #444;");
  client.println("}");
  client.println("table {");
  client.println("*border-collapse: collapse;");
  client.println("border-spacing: 0;");
  client.println("width: 100%;");
  client.println("}");
  client.println("footer{");
  client.println("position:fixed;");
  client.println("bottom:0px;");
  client.println("heigh:50px;");
  client.println("}");
  client.println(".zebra td, .zebra th {");
  client.println("padding: 10px;");
  client.println("border-bottom: 1px solid #f2f2f2;");
  client.println("}");
  client.println(".zebra tbody tr:nth-child(even) {");
  client.println("background: #f5f5f5;");
  client.println("-webkit-box-shadow: 0 1px 0 rgba(255,255,255,.8) inset;");
  client.println("-moz-box-shadow:0 1px 0 rgba(255,255,255,.8) inset;");
  client.println("box-shadow: 0 1px 0 rgba(255,255,255,.8) inset;");
  client.println("}");
  client.println("</style>");
  */
  client.println("</head> ");
  String title = "<body style=\"color: #444; font-family: 'trebuchet MS', 'Lucida sans', Arial;font-size: 14px;\"><h1 style=\"text-align:center\">Alarm on " + nameofdev + "</h1><h3 style=\"text-align:center\">" + message + "</h3>";
  client.println(title);
  client.println("<table align=\"center\" style=\"400px; borde-spacing: 0;\">");
  if(valueAlarm > -100){
    client.print("<tr><td>Trigger alarm threshould</td><td>"); client.print(valueAlarm);client.println(" &#8451</td></tr>");
    client.print("<tr style=\"background: #f5f5f5\"><td>Type</td><td>");client.print(type == 0 ? "minimal": "Maximal");client.println("</td></tr>");
  }
  client.print("<tr><td>Location Name</td><td>"); client.print(nameofsensor);client.println("</td></tr>");
  client.print("<tr style=\"background: #f5f5f5\"><td>Value</td><td>");client.print(value);client.println(" &#8451</td></tr>");
  client.print("<tr><td>Enegy</td><td>");client.print(energy);client.println(" V</td></tr>");
  client.print("<tr style=\"background: #f5f5f5\"><td>Humidity</td><td>");client.print(humidity);client.println(" %</td></tr>");
  client.print("<tr><td>Last data from</td><td>");client.print(data);client.println("</td></tr>");
  // client.println("</table></body></html>", thermostat->name_of_dev, thermostat->radioSensors[i].location);
 
  client.println("</table></body></html>");
  
  client.println(".");
  if (!AwaitSMTPResponse(client, "250")) {
    _error = "Sending message error";
    return false;
  }
  client.println("QUIT");
  if (!AwaitSMTPResponse(client, "221")) {
    _error = "SMTP QUIT error";
    return false;
  }
  return true;
}
