#include <OneWire.h>
#include <DallasTemperature.h>
#include "SPIFFS.h"
#include <WiFi.h>
#include "ESPAsyncWebServer.h"

#define PINO_DIM    4
#define PINO_ZC     2
#define maxBrightness 800 // brilho maximo em us
#define minBrightness 7500 // brilho minimo em us
#define TRIGGER_TRIAC_INTERVAL 20 // tempo quem que o triac fica acionado
#define IDLE -1

IPAddress ap_local_IP(192,168,10,1);
IPAddress ap_gateway(192,168,10,1);
IPAddress ap_subnet(255,255,255,0);

// Set LED GPIO 
const int ledPin = 18; 


 
const char* ssid     = "ESP32";
const char* password = "12345678";

// Set web server port number to 80
AsyncWebServer server(80);


short RELE_1 = 16;

volatile float tempPROG = 35.0;
volatile float tempATUAL = 0.0;
volatile float lastTempATUAL = 0.0;


// GPIO where the DS18B20 is connected to
const int oneWireBus = 15;     

// Setup a oneWire instance to communicate with any OneWire devices
OneWire oneWire(oneWireBus);

// Pass our oneWire reference to Dallas Temperature sensor 
DallasTemperature sensors(&oneWire);

 
//variaveis globais
int potencia_1 = 0;
int potencia_1_convertido = 0;



int maxv = 0;
int minv = 100;
 
unsigned long ultimo_millis1 = 0; 
unsigned long ultimo_millis2 = 0; 
unsigned long debounce_delay = 500;
 
hw_timer_t * timerToPinHigh;
hw_timer_t * timerToPinLow;
 
portMUX_TYPE mux = portMUX_INITIALIZER_UNLOCKED;
 
volatile bool isPinHighEnabled = false;
volatile long currentBrightness = minBrightness;
volatile boolean pauseInterr = false;

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
  String content = rFile.readStringUntil('\r'); //desconsidera '\r\n'
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
 
void IRAM_ATTR ISR_turnPinLow(){ // desliga o pino dim
  portENTER_CRITICAL_ISR(&mux); // desativa interrupçoes
    digitalWrite(PINO_DIM, LOW);
    isPinHighEnabled = false;
  portEXIT_CRITICAL_ISR(&mux); // ativa as interrupçoes novamente
}
 
void IRAM_ATTR setTimerPinLow(){ // executa as configuracoes de pwm e aplica os valores da luminosidade ao dimmer no tempo em que ra ficar em low
  timerToPinLow = timerBegin(1, 80, true);
  timerAttachInterrupt(timerToPinLow, &ISR_turnPinLow, true);
  timerAlarmWrite(timerToPinLow, TRIGGER_TRIAC_INTERVAL, false);
  timerAlarmEnable(timerToPinLow);
}
 
void IRAM_ATTR ISR_turnPinHigh(){ // liga o pino dim
  portENTER_CRITICAL_ISR(&mux);  // desativa interrupçoes
    digitalWrite(PINO_DIM, HIGH); 
    setTimerPinLow();
  portEXIT_CRITICAL_ISR(&mux); // ativa as interrupçoes novamente
}
 
void IRAM_ATTR setTimerPinHigh(long brightness){ // executa as configuracoes de pwm e aplica os valores da luminosidade ao dimmer no tempo que ira ficar em high
  isPinHighEnabled = true;
  timerToPinHigh = timerBegin(1, 80, true);
  timerAttachInterrupt(timerToPinHigh, &ISR_turnPinHigh, true);
  timerAlarmWrite(timerToPinHigh, brightness, false);
  timerAlarmEnable(timerToPinHigh);
}
 
void IRAM_ATTR ISR_zeroCross()  {// funçao que é chamada ao dimmer registrar passagem por 0
  if(currentBrightness == IDLE) return;
  portENTER_CRITICAL_ISR(&mux); // desativa interrupçoes
    if(!isPinHighEnabled){
       setTimerPinHigh(currentBrightness); // define o brilho
    }
  portEXIT_CRITICAL_ISR(&mux); // ativa as interrupçoes novamente
} 
 
void setup() {
  Serial.begin(9600);//inicia a serial
  currentBrightness = IDLE;

    // Start the DS18B20 sensor
  sensors.begin();
  
  if (openFS()){
    if(SPIFFS.exists("/temp.txt")){
      tempPROG = readFile("/temp.txt").toFloat();
    }else{
      if(writeFile("35.00","/temp.txt")){
        tempPROG = readFile("/temp.txt").toFloat();
      }
    }
  }
  Serial.print("\ntemperatura programada lida: ");
  Serial.println(tempPROG);
 
  pinMode(PINO_ZC,  INPUT_PULLUP);
  pinMode(PINO_DIM, OUTPUT);
  pinMode(RELE_1, OUTPUT);
  digitalWrite(PINO_DIM, LOW);
  digitalWrite(RELE_1, LOW);
    pinMode(ledPin, OUTPUT); 


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
 
}

void coreTaskZero( void * pvParameters ){
 
    String taskMessage = "Task running on core ";
    taskMessage = taskMessage + xPortGetCoreID();
    Serial.println(taskMessage);
    attachInterrupt(digitalPinToInterrupt(PINO_ZC), ISR_zeroCross, RISING);
 
    while(true){

      sensors.requestTemperatures(); 
      tempATUAL = sensors.getTempCByIndex(0);
      if ((millis() - ultimo_millis2) > debounce_delay) { // se ja passou determinado tempo que o botao foi precionado
        ultimo_millis2 = millis();
        Serial.print(tempATUAL);
        Serial.println("ºC");
        Serial.print("Brilho -> ");
        Serial.println(potencia_1); // mostra a quantidade de brilho atual
      }
      
      if (tempATUAL<tempPROG - 1.0){
              potencia_1 = 0;
              potencia_1_convertido = map(0, 100, 0, maxBrightness, minBrightness); //converte a luminosidade em microsegundos
              portENTER_CRITICAL(&mux); //desliga as interrupçoes
              currentBrightness = potencia_1_convertido; // altera o brilho
              portEXIT_CRITICAL(&mux);// liga as interrupçoes
              ligaRELE(RELE_1);
      }else{
        desligaRELE(RELE_1);
        if (tempATUAL != lastTempATUAL){
          if(tempATUAL < lastTempATUAL && tempATUAL < tempPROG-0.3){
              potencia_1 = potencia_1 + 5;
              potencia_1 = constrain(potencia_1, 0, 100); // limita a variavel
              potencia_1_convertido = map(potencia_1, 100, 0, maxBrightness, minBrightness); //converte a luminosidade em microsegundos
              portENTER_CRITICAL(&mux); //desliga as interrupçoes
              currentBrightness = potencia_1_convertido; // altera o brilho
              portEXIT_CRITICAL(&mux);// liga as interrupçoes
          }else if(tempATUAL > tempPROG || tempATUAL > tempPROG-0.3){
            potencia_1 = potencia_1 - 5;
            potencia_1 = constrain(potencia_1, 0, 100);// limita a variavel
            potencia_1_convertido = map(potencia_1, 100, 0, maxBrightness, minBrightness);//converte a luminosidade em microsegundos
            portENTER_CRITICAL(&mux); //desliga as interrupçoes
            currentBrightness = potencia_1_convertido; // altera o brilho
            portEXIT_CRITICAL(&mux);// liga as interrupçoes
        }
        lastTempATUAL = tempATUAL;
      }
      }
     delay(1);
    } 
}

void coreTaskOne( void * pvParameters ){
    String taskMessage = "Task running on core ";
    taskMessage = taskMessage + xPortGetCoreID();
    Serial.println(taskMessage);
    WiFi.mode(WIFI_AP);
  WiFi.softAP(ssid, password);
  Serial.println(WiFi.softAPConfig(ap_local_IP, ap_gateway, ap_subnet)? "Configuring Soft AP" : "Error in Configuration");    
 
  delay(100);
  
  Serial.print("AP IP address: ");
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
     while(true){
      delay(500);
     /* portENTER_CRITICAL(&mux); //desliga as interrupçoes
      writeFile(String(potencia_1), "/brilho.txt");
      portEXIT_CRITICAL(&mux);// liga as interrupçoes*/

  }
     
}
