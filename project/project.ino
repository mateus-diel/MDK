#include <OneWire.h>
#include <DallasTemperature.h>
#include "SPIFFS.h"
#include <WiFi.h>
#include "ESPAsyncWebServer.h"
#include <Arduino_JSON.h>
#include <dimmable_light.h>

#define PINO_DIM_1    4
#define PINO_ZC     2
#define MAXPOT 255 
#define MINPOT  50 

IPAddress ap_local_IP(192,168,10,1);
IPAddress ap_gateway(192,168,10,1);
IPAddress ap_subnet(255,255,255,0);


// Set web server port number to 80
AsyncWebServer  server(80);


short RELE_1 = 16;

volatile float tempPROG = 35.0;
volatile float tempATUAL = 0.0;
volatile float lastTempATUAL = 0.0;
JSONVar configs;
JSONVar states;

volatile boolean core_0 = false;
volatile boolean core_1 = false;
boolean LINHA_1 = true;


// GPIO where the DS18B20 is connected to
const int oneWireBus = 15;     

// Setup a oneWire instance to communicate with any OneWire devices
OneWire oneWire(oneWireBus);

// Pass our oneWire reference to Dallas Temperature sensor 
DallasTemperature sensors(&oneWire);

 
//variaveis globais
int potencia_1 = 0;
int potencia_1_convertido = 0;


 
unsigned long ultimo_millis1 = 0; 
unsigned long ultimo_millis2 = 0; 
unsigned long debounce_delay = 500;
 

void responseToClient (AsyncWebServerRequest *req, String res){
  AsyncWebServerResponse *response = req->beginResponse(200, "text/plain", res);
    response->addHeader("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS,POST,PUT");
    response->addHeader("Access-Control-Allow-Credentials", "true");
    response->addHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
    response->addHeader("Access-Control-Allow-Origin", "*");  
    req->send(response);
}


void ligaRELE(short pin){
  digitalWrite(pin, HIGH);
}

void desligaRELE(short pin){
  digitalWrite(pin, LOW);
}

/**
  * @desc escreve conteúdo em um arquivo
  * @param string state - conteúdo a se escrever no arquivo
  * @param string path - arquivo a ser escrito
*/
boolean writeFile(String state, String path) { 
  //Abre o arquivo para escrita ("w" write)
  //Sobreescreve o conteúdo do arquivo
  File rFile = SPIFFS.open(path,"w+"); 
  if(!rFile){
    Serial.println("Erro ao abrir arquivo!");
    return false;
  } else {
    rFile.println(state);
    Serial.print("gravou estado: ");
    Serial.println(state);
  }
  rFile.close();
  return true;
}
 
/**
  * @desc lê conteúdo de um arquivo
  * @param string path - arquivo a ser lido
  * @return string - conteúdo lido do arquivo
*/
String readFile(String path) {
  File rFile = SPIFFS.open(path,"r");
  if (!rFile) {
    Serial.println("Erro ao abrir arquivo!");
  }
  //String content = rFile.readStringUntil('\r'); //desconsidera '\r\n'
  String content;
  while (rFile.available()){
            content += char(rFile.read());
          }
  rFile.close();
  Serial.print("leitura de estado: ");
  Serial.println(content);
  rFile.close();
  return content;
}
 
/**
  * @desc inicializa o sistema de arquivos
*/
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
 
 
void setup() {
  Serial.begin(9600);//inicia a serial

    // Start the DS18B20 sensor
  sensors.begin();
  
  if (!openFS()){
    Serial.println("Erro ao abrir sistema de arquivos!");
    delay(3000);
    ESP.restart();

  }



      
      configs = JSON.parse(readFile("/configs.json"));
  
      // JSON.typeof(jsonVar) can be used to get the type of the var
      if (JSON.typeof(configs) == "undefined") {
        Serial.println("Parsing input failed!");
        return;
      }
    
      Serial.print("JSON object = ");
      Serial.println(configs);
    
      Serial.println("\n\n testesss \n");
      Serial.println(configs["ssid"]);
      Serial.println(configs["password"]);
      float t = (double) configs["tempPROG_1"];
      Serial.println(t);
      Serial.println(configs["fgch"]);
      tempPROG = (double) configs["tempPROG_1"];
      LINHA_1 = (bool) configs["linha1"];
      



  
  Serial.print("\ntemperatura programada lida: ");
  Serial.println(tempPROG);

      pinMode(RELE_1, OUTPUT);
  digitalWrite(RELE_1, LOW);


  //cria uma tarefa que será executada na função coreTaskZero, com prioridade 1 e execução no núcleo 0
  //coreTaskZero: piscar LED e contar quantas vezes
  xTaskCreatePinnedToCore(
                    coreTaskZero,   /* função que implementa a tarefa */
                    "coreTaskZero", /* nome da tarefa */
                    10000,      /* número de palavras a serem alocadas para uso com a pilha da tarefa */
                    NULL,       /* parâmetro de entrada para a tarefa (pode ser NULL) */
                    1,          /* prioridade da tarefa (0 a N) */
                    NULL,       /* referência para a tarefa (pode ser NULL) */
                    0);         /* Núcleo que executará a tarefa */
                    
  delay(500); //tempo para a tarefa iniciar

  //cria uma tarefa que será executada na função coreTaskOne, com prioridade 2 e execução no núcleo 1
  //coreTaskOne: atualizar as informações do display
  xTaskCreatePinnedToCore(
                    coreTaskOne,   /* função que implementa a tarefa */
                    "coreTaskOne", /* nome da tarefa */
                    10000,      /* número de palavras a serem alocadas para uso com a pilha da tarefa */
                    NULL,       /* parâmetro de entrada para a tarefa (pode ser NULL) */
                    2,          /* prioridade da tarefa (0 a N) */
                    NULL,       /* referência para a tarefa (pode ser NULL) */
                    1);         /* Núcleo que executará a tarefa */

    delay(500); //tempo para a tarefa iniciar
 
}
 
void loop() {
 vTaskSuspend(NULL);
}

void coreTaskZero( void * pvParameters ){
 
    String taskMessage = "Task running on core ";
    taskMessage = taskMessage + xPortGetCoreID();
    Serial.println(taskMessage);
    DimmableLight DIMMER_1(PINO_DIM_1);
    DimmableLight::setSyncPin(PINO_ZC);
    DimmableLight::begin();
 
    while(true){

      sensors.requestTemperatures(); 
      tempATUAL = sensors.getTempCByIndex(0);
      if ((millis() - ultimo_millis2) > debounce_delay) { // se ja passou determinado tempo que o botao foi precionado
        ultimo_millis2 = millis();
        Serial.print(tempATUAL);
        states["sensor1"]=tempATUAL;
        states["linha1"]=LINHA_1;
        states["tempPROG"]=tempPROG;
        Serial.println("ºC");
        Serial.print("Potencia -> ");
        Serial.println(DIMMER_1.getBrightness()); // mostra a quantidade de brilho atual
      }

      if ((millis() - ultimo_millis1) > debounce_delay+59500) { // se ja passou determinado tempo que o botao foi precionado
        ultimo_millis1 = millis();
        configs["linha1"]=LINHA_1;
        configs["tempPROG_1"]=tempPROG;
        DimmableLight::pauseStop();
        delay(20);
        writeFile(JSON.stringify(configs),"/configs.json"); 
        delay(10);
        DIMMER_1.setBrightness(10);
        DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
        
        
        
      }

      if(!core_0){
        core_1 = false;     
      
      if (tempATUAL<tempPROG - 1.0 && LINHA_1){
              potencia_1 = 0;
              DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
              ligaRELE(RELE_1);
      }else if(LINHA_1){
        desligaRELE(RELE_1);
        if (tempATUAL != lastTempATUAL){
          if(tempATUAL < lastTempATUAL && tempATUAL < tempPROG-0.0){
              potencia_1 = potencia_1 + 5;
              potencia_1 = constrain(potencia_1, 0, 100);// limita a variavel
             DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
          }else if(tempATUAL > tempPROG || tempATUAL > tempPROG-0.1){
            potencia_1 = potencia_1 - 5;
            potencia_1 = constrain(potencia_1, 0, 100);// limita a variavel
            DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
        }
        lastTempATUAL = tempATUAL;
      }
      }else{
        desligaRELE(RELE_1);
        potencia_1 = 0;
        DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
      }
     
    }else{
    detachInterrupt(PINO_ZC);
    core_1 = true;
    while(core_0==true){
      delay(1);
    }
    //attachInterrupt(digitalPinToInterrupt(PINO_ZC), ISR_zeroCross, RISING);
    }
    delay(1);
    }
}

void coreTaskOne( void * pvParameters ){
    String taskMessage = "Task running on core ";
    taskMessage = taskMessage + xPortGetCoreID();
    Serial.println(taskMessage);
    WiFi.mode(WIFI_AP);
    delay(2000); 
  WiFi.softAP(configs["ssid"], configs["ssid"]);
  delay(2000); 
  //Serial.println(WiFi.softAPConfig(ap_local_IP, ap_gateway, ap_subnet)? "Configuring Soft AP" : "Error in Configuration");    
 
  delay(100);
  
  Serial.print("AP IP address: ");
  Serial.println(WiFi.softAPIP());
    
  
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
      Serial.println("Json");
      JSONVar jso =  JSON.parse(t);
      if (jso.hasOwnProperty("tempPROG")){
        tempPROG = (double) jso["tempPROG"];
      }
      if (jso.hasOwnProperty("linha1")){
        LINHA_1 = (bool) jso["linha1"];
      }
    Serial.println(jso["email"]);
  //request->send(response(request, "Ok Tigrao"));
  responseToClient(request,"Ok Tigrao");
      
    
    });
 
  server.on("/get", HTTP_GET, [](AsyncWebServerRequest *request){
    responseToClient(request,JSON.stringify(states));
  });
 
  
  server.begin();
  Serial.println("HTTP server started");
     while(true){
      //server.handleClient();
      delay(1);
     

  }
     
}
