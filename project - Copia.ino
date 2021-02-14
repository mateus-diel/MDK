#include <OneWire.h>
#include <DallasTemperature.h>
#include <LITTLEFS.h>
#include <WiFi.h>
#include "ESPAsyncWebServer.h"
#include <Arduino_JSON.h>
#include <dimmable_light.h>
#include <ESPmDNS.h>
#include <PubSubClient.h>
#include <WiFiClientSecure.h>


#define PINO_DIM_1    4
#define PINO_ZC     2
#define MAXPOT 255
#define MINPOT  50
#define PINORESET 13
#define CONFIGURATION "/configs.json"
#define DEVICESINFO "/devices.json"

// Set web server port number to 80
AsyncWebServer  server(80);


short RELE_1 = 16;

volatile float tempPROG = 35.0;
volatile float tempATUAL = 0.0;
volatile float lastTempATUAL = 0.0;
JSONVar configs;
JSONVar devices;

WiFiClientSecure espClient;
PubSubClient pubsubclient(espClient);

const char* mqtt_server = "a3s8a5j64d3vt4-ats.iot.sa-east-1.amazonaws.com";
const int mqtt_port = 8883;
String rootca;
String cert;
String privateKey;

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
unsigned long ultimo_millis3 = 0;
unsigned long debounce_delay = 500;


void responseToClient (AsyncWebServerRequest *req, String res) {
  AsyncWebServerResponse *response = req->beginResponse(200, "text/plain", res);
  response->addHeader("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS,POST,PUT");
  response->addHeader("Access-Control-Allow-Credentials", "true");
  response->addHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
  response->addHeader("Access-Control-Allow-Origin", "*");
  req->send(response);
}


void ligaRELE(short pin) {
  digitalWrite(pin, HIGH);
}

void desligaRELE(short pin) {
  digitalWrite(pin, LOW);
}

String readFile(String path) {

  File rfile = LITTLEFS.open(path);
  if (!rfile || rfile.isDirectory()) {
    Serial.println("- failed to open file for reading");
    return "";
  }

  String content;
  while (rfile.available()) {
    content += char(rfile.read());
  }
  rfile.close();
  Serial.print("CONTEUDO LIDO: ");
  Serial.println(content);
  return content;

}

boolean writeFile(String message, String path) {
  Serial.printf("Writing file: %s\r\n", path);

  File file = LITTLEFS.open(path, FILE_WRITE);
  if (!file) {
    Serial.println("- failed to open file for writing");
    return false;
  }
  if (file.print(message)) {
    Serial.println("- file written");
    return true;
  } else {
    Serial.println("- write failed");
    return false;
  }
  file.close();
  return true;
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();
}
void reconnect() {
  // Loop until we're reconnected
  while (!pubsubclient.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Create a random client ID
    String clientId = "ESP32-";
    clientId += String(random(0xffff), HEX);
    // Attempt to connect
    if (pubsubclient.connect(clientId.c_str())) {
      Serial.println("connected");
      // Once connected, publish an announcement...
      pubsubclient.publish("ei_out", "hello world");
      // ... and resubscribe
      pubsubclient.subscribe("ei_in");
    } else {
      Serial.print("failed, rc=");
      Serial.print(pubsubclient.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}


void setup() {
  Serial.begin(115200);//inicia a serial
  while (!Serial);
  Serial.println("ESP INICIADO");
  pinMode(PINORESET, INPUT);

  if (!LITTLEFS.begin(false)) {
    Serial.println("LITTLEFS Mount Failed");
    ESP.restart();
  }

  configs = JSON.parse(readFile(CONFIGURATION));

  short isReset = 0;
  if (digitalRead(PINORESET) == LOW) {
    while (digitalRead(PINORESET) == LOW) {
      delay(1000);
      isReset++;
    }
  }
  if (isReset > 5) {
    configs["default"] = true;
    writeFile(JSON.stringify(configs), CONFIGURATION);
    Serial.println("RESET BY PIN");
    ESP.restart();
  }
  rootca = readFile("/RootCA1.pem");
  cert = readFile("/certificate.pem.crt");
  privateKey = readFile("/private.pem.key");

  if (JSON.typeof(configs) == "undefined") {
    Serial.println("Parsing input failed!");
    return;
  }



  if (!(bool) configs["default"]) {

    // JSON.typeof(jsonVar) can be used to get the type of the var
    if (JSON.typeof(configs) == "undefined") {
      Serial.println("Parsing input failed!");
      return;
    }

    devices = JSON.parse(readFile(DEVICESINFO));
    tempPROG = (double) devices["tempPROG_1"];
    LINHA_1 = (bool) devices["linha_1"];

    // Start the DS18B20 sensor
    sensors.begin();

    Serial.print("\ntemperatura programada lida: ");
    Serial.println(tempPROG);

  }

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

void coreTaskZero( void * pvParameters ) {

  String taskMessage = "Task running on core ";
  taskMessage = taskMessage + xPortGetCoreID();
  Serial.println(taskMessage);
  DimmableLight DIMMER_1(PINO_DIM_1);
  DimmableLight::setSyncPin(PINO_ZC);
  DimmableLight::begin();

  while ((bool) configs["default"]) {
    vTaskSuspend(NULL);
  }

  while (true) {

    sensors.requestTemperatures();
    tempATUAL = sensors.getTempCByIndex(0);
    if ((millis() - ultimo_millis2) > debounce_delay) {
      ultimo_millis2 = millis();
      //Serial.print(tempATUAL);
      devices["sensor1"] = tempATUAL;
      devices["linha_1"] = LINHA_1;
      devices["tempPROG"] = tempPROG;
      //Serial.println("ºC");
      //Serial.print("Potencia -> ");
      //Serial.println(DIMMER_1.getBrightness()); // mostra a quantidade de brilho atual
    }

    if ((millis() - ultimo_millis1) > debounce_delay + 59500) {
      ultimo_millis1 = millis();
      devices["linha_1"] = LINHA_1;
      devices["tempPROG_1"] = tempPROG;
      DimmableLight::pauseStop();
      delay(20);
      writeFile(JSON.stringify(devices), DEVICESINFO);
      delay(10);
      DIMMER_1.setBrightness(10);
      DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));



    }



    if (tempATUAL < tempPROG - 1.0 && LINHA_1) {
      potencia_1 = 0;
      DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
      ligaRELE(RELE_1);
    } else if (LINHA_1) {
      desligaRELE(RELE_1);
      if (tempATUAL != lastTempATUAL) {
        if (tempATUAL < lastTempATUAL && tempATUAL < tempPROG - 0.0) {
          potencia_1 = potencia_1 + 5;
          potencia_1 = constrain(potencia_1, 0, 100);// limita a variavel
          DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
        } else if (tempATUAL > tempPROG || tempATUAL > tempPROG - 0.1) {
          potencia_1 = potencia_1 - 5;
          potencia_1 = constrain(potencia_1, 0, 100);// limita a variavel
          DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
        }
        lastTempATUAL = tempATUAL;
      }
    } else {
      desligaRELE(RELE_1);
      potencia_1 = 0;
      DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
    }

    delay(1);
  }
}

void coreTaskOne( void * pvParameters ) {
  String taskMessage = "Task running on core ";
  taskMessage = taskMessage + xPortGetCoreID();
  Serial.println(taskMessage);

  if ((bool) configs["default"]) {
    WiFi.mode(WIFI_AP);
    delay(1000);
    Serial.print("macc: ");
    String mac = WiFi.macAddress();
    Serial.println(mac);
    Serial.print("Size of: ");
    Serial.println(sizeof(mac));

    char ssid[sizeof(mac) + 6];
    mac.toCharArray(ssid, sizeof(ssid));
    char pass[sizeof((const char*)configs["defaultPassword"]) + 1];
    String((const char*)configs["defaultPassword"]).toCharArray(pass, sizeof(pass));


    WiFi.softAP(ssid, pass);
    delay(2000);
    //Serial.println(WiFi.softAPConfig(ap_local_IP, ap_gateway, ap_subnet)? "Configuring Soft AP" : "Error in Configuration");

    delay(100);

    Serial.print("AP IP address: ");
    Serial.println(WiFi.softAPIP());


  } else {

    WiFi.mode(WIFI_MODE_STA);
    delay(1000);
    WiFi.begin((const char*) configs["ssid"], (const char*) configs["password"]);
    Serial.print("Wifi e senha: ");
    Serial.print((const char*) configs["ssid"]);
    Serial.println((const char*) configs["password"]);
    delay(1000);


    while (WiFi.status() != WL_CONNECTED) {
      delay(500);
      Serial.println("Connecting to WiFi..");
    }
    Serial.print("Ip->: ");
    Serial.println(WiFi.localIP());



  }


  server.on(
    "/post",
    HTTP_POST,
  [](AsyncWebServerRequest * request) {},
  NULL,
  [](AsyncWebServerRequest * request, uint8_t *data, size_t len, size_t index, size_t total) {
    String t;
    for (size_t i = 0; i < len; i++) {
      t += (char)(data[i]);
    }
    JSONVar ok;
    ok["request"] = "error";

    Serial.println(t);
    Serial.println("Json");
    JSONVar jso =  JSON.parse(t);
    if (jso.hasOwnProperty("configNetwork") && (bool) configs["default"]) {
      configs["default"] = (bool) jso["configNetwork"];
      configs["ssid"] = (const char*) jso["ssid"];
      configs["password"] = (const char*) jso["password"];
      configs["deviceName"] = (const char*) jso["deviceName"];
      configs["defaultPassword"] = (const char*)configs["defaultPassword"];
      writeFile(JSON.stringify(configs), CONFIGURATION);
      ok["request"] = "ok";
      responseToClient(request, JSON.stringify(ok));
      delay(1000);
      ESP.restart();


    } else {
      if (jso.hasOwnProperty("tempPROG")) {
        tempPROG = (double) jso["tempPROG"];
      }
      if (jso.hasOwnProperty("linha_1")) {
        LINHA_1 = !LINHA_1;
      }

      ok["linha_1"] = LINHA_1;
      ok["tempPROG"] = tempPROG;
      ok["request"] = "ok";
    }
    responseToClient(request, JSON.stringify(ok));
  });

  server.on("/get", HTTP_GET, [](AsyncWebServerRequest * request) {
    responseToClient(request, JSON.stringify(devices));
  });

  server.begin();

  if (configs.hasOwnProperty("deviceName")) {
    if (!MDNS.begin((const char*) configs["deviceName"])) {
      Serial.println("Error setting up MDNS responder!");
      delay(1000);
      ESP.restart();
    }
    Serial.println("MDNS begib successful;");
  } else {
    if (!MDNS.begin("ESP32")) {
      Serial.println("Error setting up MDNS responder!");
      delay(1000);
      ESP.restart();
    }
  }

  Serial.println("mDNS responder started");

  // Start TCP (HTTP) server
  Serial.println("TCP server started");
  /*
    if ((millis() - ultimo_millis3) > 5000) { // se ja passou determinado tempo que o botao foi precionado
      ultimo_millis3 = millis();
      MDNS.addService("dimmer", "tcp", 80);
    }*/

  // Add service to MDNS-SD
  MDNS.addService("dimmer", "tcp", 80);
  Serial.println("HTTP server started");


  char* p_rootca;
  p_rootca = (char *)malloc(sizeof(char) * (rootca.length() + 1));
  strcpy(p_rootca, rootca.c_str());

  Serial.print("root ca ponteiro\n");
  Serial.print(p_rootca);
  Serial.println("ok");
  char* p_cert;
  p_cert = (char *)malloc(sizeof(char) * (cert.length() + 1));
  strcpy(p_cert, cert.c_str());

  char* p_privateKey;
  p_privateKey = (char *)malloc(sizeof(char) * (privateKey.length() + 1));
  strcpy(p_privateKey, privateKey.c_str());

  espClient.setCACert(p_rootca);
  espClient.setCertificate(p_cert);
  espClient.setPrivateKey(p_privateKey);

  pubsubclient.setServer(mqtt_server, mqtt_port);
  pubsubclient.setCallback(callback);


  while (true) {
    if (!pubsubclient.connected()) {
      reconnect();
    }
    pubsubclient.loop();

    if ((millis() - ultimo_millis3) > 5000) { // se ja passou determinado tempo que o botao foi precionado
      ultimo_millis3 = millis();
      JSONVar jsonvar = new JSONVar();
      jsonvar["item1"] = "Olá pessoas";
      jsonvar["item2"] = true;
      jsonvar["item3"] = 33.5;
      char msg[JSON.stringify(jsonvar).length()];
      JSON.stringify(jsonvar).toCharArray(msg, JSON.stringify(jsonvar).length());
      Serial.print("\nMensagem enviada: ");
      Serial.println(msg);
      pubsubclient.publish("ei_out", msg);


      delay(1);
    }
  }
}
