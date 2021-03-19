/** 
 * An extension of the first example to demonstrate 
 * how easy it is to control multiple lights.
 */ 
#include <dimmable_light.h>

#include "SPIFFS.h"
#include <WiFi.h>
#include "ESPAsyncWebServer.h"

IPAddress ap_local_IP(192,168,10,1);
IPAddress ap_gateway(192,168,10,1);
IPAddress ap_subnet(255,255,255,0);

unsigned long ultimo_millis1 = 0; 
unsigned long ultimo_millis2 = 0; 
unsigned long debounce_delay = 500;

// Set LED GPIO 
const int ledPin = 18; 

AsyncWebServer server(80);


 
const char* ssid     = "ESP32";
const char* password = "12345678";


// Pin listening to AC zero cross signal
const int syncPin = 2;

DimmableLight light2(17);
DimmableLight light1(4);

boolean openFS(void){
  //Abre o sistema de arquivos
  if(!SPIFFS.begin(true)){
    Serial.println("\nErro ao abrir o sistema de arquivos");
    return false;
  } else {
    Serial.println("\nSistema de arquivos aberto com sucesso!");
    return true;
  }
}


short RELE_1 = 5;
short RELE_2 = 16;

// Replaces placeholder with LED state value 
String processor(const String& var){ 
  Serial.println(var); 
  if(var == "STATE"){ 
    String ledState; 
    if(digitalRead(ledPin)){ 
      ledState = "ON"; 
    } 
    else{ 
      ledState = "OFF"; 
    } 
    Serial.print(ledState); 
    return ledState; 
  } 
  return String(); 
} 

// Delay between brightness changes, in millisecond
int period = 1000;

void setup() {
  Serial.begin(9600);
  while(!Serial);
  Serial.println();
  
  openFS();
  Serial.println("Dimmable Light for Arduino: second example");
  Serial.println();
    pinMode(RELE_1, OUTPUT);
  digitalWrite(RELE_1, LOW);
    pinMode(RELE_2, OUTPUT);
  digitalWrite(RELE_2, LOW);
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ssid, password);
  Serial.println(WiFi.softAPConfig(ap_local_IP, ap_gateway, ap_subnet)? "Configuring Soft AP" : "Error in Configuration");    
  Serial.println(WiFi.softAPIP());
    // Route for root / web page
  server.on("/", HTTP_GET, [](AsyncWebServerRequest *request){
    request->send(SPIFFS, "/index.html", String(), false, processor);
  });
  
  // Route to load style.css file
  server.on("/style.css", HTTP_GET, [](AsyncWebServerRequest *request){
    request->send(SPIFFS, "/style.css", "text/css");
  });

  // Route to set GPIO to HIGH
  server.on("/on", HTTP_GET, [](AsyncWebServerRequest *request){
    //digitalWrite(ledPin, HIGH);    
    request->send(SPIFFS, "/index.html", String(), false, processor);
  });
  
  // Route to set GPIO to LOW
  server.on("/off", HTTP_GET, [](AsyncWebServerRequest *request){
    //digitalWrite(ledPin, LOW);    
    request->send(SPIFFS, "/index.html", String(), false, processor);
  });


  
  server.begin();
 
  delay(100);
  
  Serial.print("Initializing the dimmable light class... ");
  DimmableLight::setSyncPin(syncPin);
  DimmableLight::begin();
  Serial.println("Done!");
  light1.setBrightness(100);
          light2.setBrightness(100);
}

void loop() {
  



        if ((millis() - ultimo_millis1) > 5000) { // se ja passou determinado tempo que o botao foi precionado
        ultimo_millis1 = millis();
         DimmableLight::pauseStop();
         Serial.println("desativa");
      
      }

      if ((millis() - ultimo_millis2) > 7000) { // se ja passou determinado tempo que o botao foi precionado
        ultimo_millis2 = millis();
        Serial.println("ligaaaaaaa");
         light1.setBrightness(random(60,120));
          light2.setBrightness(random(60,120));
          
      
      }
}
