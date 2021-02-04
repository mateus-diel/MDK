#include "WiFi.h"
#include "ESPAsyncWebServer.h"
 
AsyncWebServer server(80);

void responseToClient (AsyncWebServerRequest *req, String res){
  AsyncWebServerResponse *response = req->beginResponse(200, "text/plain", res);
    response->addHeader("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS,POST,PUT");
    response->addHeader("Access-Control-Allow-Credentials", "true");
    response->addHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
    response->addHeader("Access-Control-Allow-Origin", "*");  
    req->send(response);
}
 
void setup(){
  Serial.begin(9600);
 
  WiFi.mode(WIFI_AP);
    delay(2000); 
  WiFi.softAP("ESP32", "12345678");
  
 
server.on(
    "/post",
    HTTP_POST,
    [](AsyncWebServerRequest * request){},
    NULL,
    [](AsyncWebServerRequest * request, uint8_t *data, size_t len, size_t index, size_t total) {
      String t;
      for (size_t i = 0; i < len; i++) {
        t+=(char)(data[i]);
      }
 
      Serial.println(t);
      Serial.println();
  //request->send(response(request, "Ok Tigrao"));
  responseToClient(request,"Ok Tigrao");
      
    
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
  Serial.println("server started");
}
 
void loop(){
}
