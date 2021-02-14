#include <OneWire.h>
#include <DallasTemperature.h>
#include <LITTLEFS.h>
#include <WiFi.h>
#include "ESPAsyncWebServer.h"
#include <Arduino_JSON.h>
#include <dimmable_light.h>
#include <ESPmDNS.h>
#include "FirebaseESP32.h"


#define PINO_DIM_1    4
#define PINO_ZC     2
#define MAXPOT 255
#define MINPOT  50
#define PINORESET 13
#define CONFIGURATION "/configs.json"
#define DEVICESINFO "/devices.json"


FirebaseData fbdo;
String email;
String senha;
String api_key;
String host;

// Set web server port number to 80
AsyncWebServer  server(80);


short RELE_1 = 16;

volatile float tempPROG = 35.0;
volatile float tempATUAL = 0.0;
volatile float lastTempATUAL = 0.0;
JSONVar configs;
JSONVar devices;

volatile boolean LINHA_1 = true;

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

  if (JSON.typeof(configs) == "undefined") {
    Serial.println("Parsing input failed!");
    return;
  }



  if (!(bool) configs["default"]) {


    api_key = String((const char*)configs["api_key"]);
    email = String((const char*)configs["user_email"]);
    senha = String((const char*)configs["user_senha"]);
    host = String((const char*)configs["host"]);

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

  FirebaseAuth auth;

  // Define the FirebaseConfig data for config data
  FirebaseConfig configur;

  char hostt[host.length() + 1];
  host.toCharArray(hostt, host.length()+1);

  char api_keyy[api_key.length() + 1];
  api_key.toCharArray(api_keyy, api_key.length()+1);

  char emaill[email.length() + 1];
  email.toCharArray(emaill, email.length()+1);

  char senhaa[senha.length() + 1];
  senha.toCharArray(senhaa, senha.length()+1);

  // Assign the project host and api key (required)
  configur.host = hostt;

  configur.api_key = api_keyy;

  // Assign the user sign in credentials
  auth.user.email = emaill;

  auth.user.password = senhaa;

  Serial.print("\napi key: ");
  Serial.println(api_keyy);
  Serial.print("host ");
  Serial.println(hostt);
  Serial.print("email: ");
  Serial.println(emaill);
  Serial.print("senha: ");
  Serial.println(senhaa);

  //Initialize the library with the Firebase authen and config.
  Firebase.begin(&configur, &auth);
  String nodo = "/cliente/" + String((const char *) configs["client_id"]) + "/" + String((const char*) configs["deviceName"]) + "/";


  while (true) {

    if ((millis() - ultimo_millis3) > 5000) { // se ja passou determinado tempo que o botao foi precionado
      ultimo_millis3 = millis();

      FirebaseJson json2;

      json2.set("tempPROG", tempPROG);
      json2.set("tempATUAL", tempATUAL);
      json2.set("LINHA_1", LINHA_1);

      if (Firebase.updateNode(fbdo, nodo, json2)) {


        Serial.println(fbdo.dataPath() + "/" + fbdo.pushName());

      } else {
        Serial.println(fbdo.errorReason());
      }




    }

    delay(1);
  }
}
