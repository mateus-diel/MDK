#include <OneWire.h>
#include <DallasTemperature.h>
#include <FS.h>
#include "SPIFFS.h"
#include <WiFi.h>

#define PINO_DIM    4
#define PINO_ZC     2
#define maxBrightness 800 // brilho maximo em us
#define minBrightness 7500 // brilho minimo em us
#define TRIGGER_TRIAC_INTERVAL 20 // tempo quem que o triac fica acionado
#define IDLE -1

IPAddress ap_local_IP(192,168,10,1);
IPAddress ap_gateway(192,168,10,1);
IPAddress ap_subnet(255,255,255,0);


const char* ssid     = "ESP32";
const char* password = "12345678";

// Set web server port number to 80
WiFiServer server(80);

// Variable to store the HTTP request
String header;

short RELE_1 = 16;
//variaveis que indicam o núcleo
static uint8_t taskCoreZero = 0;
static uint8_t taskCoreOne  = 1;

volatile float tempPROG = 0.0;
volatile float tempATUAL = 0.0;
volatile float lastTempATUAL = 0.0;


// GPIO where the DS18B20 is connected to
const int oneWireBus = 15;     

// Setup a oneWire instance to communicate with any OneWire devices
OneWire oneWire(oneWireBus);

// Pass our oneWire reference to Dallas Temperature sensor 
DallasTemperature sensors(&oneWire);

 
//variaveis globais
int brilho = 0;
int brilho_convertido = 0;

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
 
void turnLightOn(){ // liga o dimmer no brilho maximo
  portENTER_CRITICAL(&mux);// desativa interrupçoes
    currentBrightness = maxBrightness;
    digitalWrite(PINO_DIM, HIGH);
  portEXIT_CRITICAL(&mux);// ativa as interrupçoes novamente
}
 
void turnLightOff(){// deliga o dimmer
  portENTER_CRITICAL(&mux); // desativa interrupçoes
    currentBrightness = IDLE;
    digitalWrite(PINO_DIM, LOW);
  portEXIT_CRITICAL(&mux); // ativa as interrupçoes novamente
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
      if(writeFile("10.00","/temp.txt")){
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

  Serial.print("Setting AP (Access Point)…");
  // Remove the password parameter, if you want the AP (Access Point) to be open
  
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ssid, password);
  Serial.println(WiFi.softAPConfig(ap_local_IP, ap_gateway, ap_subnet)? "Configuring Soft AP" : "Error in Configuration");    
 
  delay(100);
  
  Serial.print("AP IP address: ");
  Serial.println(WiFi.softAPIP());

  
  server.begin();
  
 
  Serial.println("Controlando dimmer com esp32");

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
  /*if ((millis() - ultimo_millis1) > debounce_delay) { // se ja passou determinado tempo que o botao foi precionado
    ultimo_millis1 = millis();
      if (maxv < 100) { // e o botao estiver precionado
        maxv++;
        brilho++; // aumente o brilho
        brilho = constrain(brilho, 0, 100); // limita a variavel
        brilho_convertido = map(brilho, 100, 0, maxBrightness, minBrightness); //converte a luminosidade em microsegundos
         portENTER_CRITICAL(&mux); //desliga as interrupçoes
            currentBrightness = brilho_convertido; // altera o brilho
         portEXIT_CRITICAL(&mux);// liga as interrupçoes
    }
  }
 
  if ((millis() - ultimo_millis2) > debounce_delay) { // se ja passou determinado tempo que o botao foi precionado
    ultimo_millis2 = millis();
      if (maxv == 100 && minv>0) {// e o botao estiver precionado
        minv--;
        brilho--;// diminui o brilho
        brilho = constrain(brilho, 0, 100);// limita a variavel
          brilho_convertido = map(brilho, 100, 0, maxBrightness, minBrightness);//converte a luminosidade em microsegundos
         portENTER_CRITICAL(&mux); //desliga as interrupçoes
            currentBrightness = brilho_convertido; // altera o brilho
         portEXIT_CRITICAL(&mux);// liga as interrupçoes
    }
  }
  if(maxv>=100 && minv<=0){
    maxv = 0;
    minv = 100;
  }*/

      sensors.requestTemperatures(); 
      tempATUAL = sensors.getTempCByIndex(0);
      if ((millis() - ultimo_millis2) > debounce_delay) { // se ja passou determinado tempo que o botao foi precionado
        ultimo_millis2 = millis();
        Serial.print(tempATUAL);
        Serial.println("ºC");
        Serial.print("Brilho -> ");
        Serial.println(brilho); // mostra a quantidade de brilho atual
      }
      
      if (tempATUAL<tempPROG - 1.0){
        ligaRELE(RELE_1);
      }else if(tempATUAL<tempPROG){
        desligaRELE(RELE_1);
        if (tempATUAL != lastTempATUAL){
          if(tempATUAL < lastTempATUAL){
              brilho = brilho + 5;
              brilho = constrain(brilho, 0, 100); // limita a variavel
              brilho_convertido = map(brilho, 100, 0, maxBrightness, minBrightness); //converte a luminosidade em microsegundos
              portENTER_CRITICAL(&mux); //desliga as interrupçoes
              currentBrightness = brilho_convertido; // altera o brilho
              portEXIT_CRITICAL(&mux);// liga as interrupçoes
          }else{
            brilho = brilho - 5;
            brilho = constrain(brilho, 0, 100);// limita a variavel
            brilho_convertido = map(brilho, 100, 0, maxBrightness, minBrightness);//converte a luminosidade em microsegundos
            portENTER_CRITICAL(&mux); //desliga as interrupçoes
            currentBrightness = brilho_convertido; // altera o brilho
            portEXIT_CRITICAL(&mux);// liga as interrupçoes
        }
        lastTempATUAL = tempATUAL;
      }
      }else{
        desligaRELE(RELE_1);
        brilho = 0;
      }
     delay(1);
    } 
}

//essa função será responsável apenas por atualizar as informações no 
//display a cada 100ms
void coreTaskOne( void * pvParameters ){
    String taskMessage = "Task running on core ";
    taskMessage = taskMessage + xPortGetCoreID();
    Serial.println(taskMessage);
     while(true){
      delay(1);
        WiFiClient client = server.available();   // Listen for incoming clients

  if (client) {                             // If a new client connects,
    Serial.println("New Client.");          // print a message out in the serial port
    String currentLine = "";                // make a String to hold incoming data from the client
    while (client.connected()) {            // loop while the client's connected
      if (client.available()) {             // if there's bytes to read from the client,
        char c = client.read();             // read a byte, then
        Serial.write(c);                    // print it out the serial monitor
        header += c;
        if (c == '\n') {                    // if the byte is a newline character
          // if the current line is blank, you got two newline characters in a row.
          // that's the end of the client HTTP request, so send a response:
          if (currentLine.length() == 0) {
            // HTTP headers always start with a response code (e.g. HTTP/1.1 200 OK)
            // and a content-type so the client knows what's coming, then a blank line:
            client.println("HTTP/1.1 200 OK");
            client.println("Content-type:text/html");
            client.println("Connection: close");
            client.println();
            
            // turns the GPIOs on and off
            if (header.indexOf("GET /26/on") >= 0) {
              Serial.println("GPIO 26 on");
            } else if (header.indexOf("GET /26/off") >= 0) {
              Serial.println("GPIO 26 off");
            } else if (header.indexOf("GET /27/on") >= 0) {
              Serial.println("GPIO 27 on");
            } else if (header.indexOf("GET /27/off") >= 0) {
              Serial.println("GPIO 27 off");
            }
            
            // Display the HTML web page
            client.println("<!DOCTYPE html><html>");
            client.println("<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
            client.println("<link rel=\"icon\" href=\"data:,\">");
            // CSS to style the on/off buttons 
            // Feel free to change the background-color and font-size attributes to fit your preferences
            client.println("<style>html { font-family: Helvetica; display: inline-block; margin: 0px auto; text-align: center;}");
            client.println(".button { background-color: #4CAF50; border: none; color: white; padding: 16px 40px;");
            client.println("text-decoration: none; font-size: 30px; margin: 2px; cursor: pointer;}");
            client.println(".button2 {background-color: #555555;}</style></head>");
            
            // Web Page Heading
            client.println("<body><h1>ESP32 Web Server</h1>");
            
            // Display current state, and ON/OFF buttons for GPIO 26  
            client.println("<p>GPIO 26 - State </p>");
            // If the output26State is off, it displays the ON button       
            
              
            client.println("</body></html>");
            
            // The HTTP response ends with another blank line
            client.println();
            // Break out of the while loop
            break;
          } else { // if you got a newline, then clear currentLine
            currentLine = "";
          }
        } else if (c != '\r') {  // if you got anything else but a carriage return character,
          currentLine += c;      // add it to the end of the currentLine
        }
      }
    }
    // Clear the header variable
    header = "";
    // Close the connection
    client.stop();
    Serial.println("Client disconnected.");
    Serial.println("");
  }
    } 
}
