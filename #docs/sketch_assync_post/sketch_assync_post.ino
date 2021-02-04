#include "WiFi.h"
#include "ESPAsyncWebServer.h"
 
AsyncWebServer server(80);
 
void setup(){
  Serial.begin(9600);
 
  WiFi.mode(WIFI_AP);
    delay(2000); 
  WiFi.softAP("ESP32", "12345678");
 
server.on(
    "/post",
    HTTP_POST,
    [](AsyncWebServerRequest * request){
        // The following print statements work + removing them makes no difference
        // This is displayed on monitor "Content type::application/x-www-form-urlencoded"
        Serial.print("Content type::");
        Serial.println(request->contentType());
        Serial.println("OFF hit.");
    String message;
    int params = request->params();
    Serial.printf("%d params sent in\n", params);
    for (int i = 0; i < params; i++)
    {
        AsyncWebParameter *p = request->getParam(i);
        if (p->isFile())
        {
            Serial.printf("_FILE[%s]: %s, size: %u", p->name().c_str(), p->value().c_str(), p->size());
        }
        else if (p->isPost())
        {
            Serial.printf("%s: %s \n", p->name().c_str(), p->value().c_str());
        }
        else
        {
            Serial.printf("_GET[%s]: %s", p->name().c_str(), p->value().c_str());
        }
        
    }
    AsyncWebServerResponse *response = request->beginResponse(200, "application/json", "{\"ip\": \"187.109.233.207\"}");
       response->addHeader("Access-Control-Allow-Origin", "*");
             response->addHeader("Cache-Control", "private");
                   response->addHeader("Content-Encoding", "gzip");
                         response->addHeader("Vary", "Accept-Encoding");
    response->addHeader("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS,POST,PUT");
    response->addHeader("Access-Control-Allow-Credentials", "true");
    response->addHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
 
    request->send(response);
    
    });
 
  server.on("/put", HTTP_PUT, [](AsyncWebServerRequest *request){
    request->send(200, "text/plain", "Put route");
  });
 
  server.on("/get", HTTP_GET, [](AsyncWebServerRequest *request){
    request->send(200, "text/plain", "Get route");
  });
 
  server.on("/any", HTTP_ANY, [](AsyncWebServerRequest *request){
    request->send(200, "text/plain", "Any route");
  });
 
  server.begin();
}
 
void loop(){
}
