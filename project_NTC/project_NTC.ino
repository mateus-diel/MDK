#include <LITTLEFS.h>
#include <WiFi.h>
#include "ESPAsyncWebServer.h"
#include <Arduino_JSON.h>
#include <dimmable_light.h>
#include <ESPmDNS.h>
#include "FirebaseESP32.h"
#include <ESP32httpUpdate.h>
#include <ThreeWire.h>
#include <RtcDS1302.h>

//NTC
#define ThermistorPin 32
#define adcMax 4095.0
#define Vs 3.3
#define R1 10000.0   // voltage divider resistor value
#define Beta 3950.0 // Beta value
#define To 298.15   // Temperature in Kelvin for 25 degree Celsius
#define Ro 10000.0 // Resistance of Thermistor at 25 degree Celsius

#define PINO_DIM_1    4
#define PINO_ZC     2
#define MAXPOT 252
#define MINPOT  0
#define PINORESET 13
#define CONFIGURATION "/configs.json"
#define DEVICESINFO "/devices.json"
#define DAYSINFO "/days.json"
#define VERSION "1.01"
#define MODEL "casa"
#define NSEMANAS 35


ThreeWire myWire(21, 22, 23); // IO, SCLK, CE
RtcDS1302<ThreeWire> Rtc(myWire);

String childPath[2] = {"/programacoes", "/info"};
size_t childPathSize = 2;

typedef struct
{
  String semana;
  double temp;
  String liga;
  String desliga;
} Horarios;

Horarios hrs[NSEMANAS];
SemaphoreHandle_t myMutex;


FirebaseData writeData;
FirebaseData readData;
String email;
String senha;
String api_key;
String host;

AsyncWebServer  server(80);

short RELE_1 = 16;

volatile double tempPROG = 35.0;
volatile double tempATUAL = 0.0;
volatile double lastTempATUAL = 0.0;
volatile boolean rele = false;
volatile bool isUpdate = false;
volatile int numError = 0;
volatile bool updateValues = false;
volatile bool automaticMode = false;
JSONVar configs;
JSONVar devices;
JSONVar times;
volatile boolean LINHA_1 = true;

volatile int potencia_1 = 0;
unsigned long ultimo_millis1 = 0;
unsigned long ultimo_millis2 = 0;
unsigned long ultimo_millis3 = 0;
unsigned long debounce_delay = 500;

void limpaHorarios() {
  for (int i = 0; i < NSEMANAS; i++) {
    hrs[i].semana = undefined;
  }
}

void streamCallback(MultiPathStreamData stream)
{
  Serial.println();
  Serial.println("Stream Data1 available...");
  Serial.println("path: " + stream.dataPath);
  Serial.println("valuie: " + stream.value);

  size_t numChild = sizeof(childPath) / sizeof(childPath[0]);

  for (size_t i = 0; i < numChild; i++)
  {
    if (stream.get(childPath[i]))
    {
      Serial.println("path: " + stream.dataPath + ", type: " + stream.type + ", value: " + stream.value);
      if (stream.dataPath.indexOf(childPath[1]) > -1 && stream.type.indexOf("json") > -1) {
        JSONVar infos = JSON.parse(stream.value);
        if (infos.hasOwnProperty("update")) {
          isUpdate = (bool) infos["update"];
        }
        if (infos.hasOwnProperty("LINHA_1")) {
          LINHA_1 = (bool) infos["LINHA_1"];
        }
        if (infos.hasOwnProperty("auto")) {
          automaticMode = (bool) infos["auto"];
        }
        if (infos.hasOwnProperty("tempPROG")) {
          tempPROG = (double) infos["tempPROG"];
        }
        updateValues = true;
      } else if (stream.dataPath.indexOf(childPath[0]) > -1 && stream.type.indexOf("json") > -1) {
        int pos = 0;
        limpaHorarios();

        JSONVar root = JSON.parse(stream.value);
        JSONVar days = root.keys();
        for (int i = 0; i < days.length(); i++) {
          JSONVar value = root[days[i]];
          JSONVar Keys = value.keys();
          for (int z = 0; z < Keys.length(); z++) {
            JSONVar val = value[Keys[z]];
            /*Serial.println();
              Serial.print(Keys[z]);
              Serial.print(": ");
              Serial.println(val);
              Serial.println(days[i]);
              Serial.println(val["liga"]);
              Serial.println(val["desliga"]);
              Serial.println(val["tempPROG"]);*/
            xSemaphoreTake(myMutex, portMAX_DELAY);
            hrs[pos].semana = String((const char*) days[i]);
            hrs[pos].temp = String((const char*) val["tempPROG"]).toDouble();
            hrs[pos].liga = String((const char*) val["liga"]);
            hrs[pos].desliga = String((const char*) val["desliga"]);
            xSemaphoreGive(myMutex);
            pos++;
          }
        }
      }
    }
  }
}

void streamTimeoutCallback(bool timeout) {
  if (timeout)  {
    Serial.println();
    Serial.println("Stream timeout, resume streaming...");
    Serial.println();
  }
}

boolean estaEntre(String liga, String desliga) {
  if (Rtc.IsDateTimeValid()) {
    RtcDateTime now = Rtc.GetDateTime();
    RtcDateTime On = RtcDateTime(now.Year(), now.Month(), now.Day(), liga.substring(0, liga.indexOf(":")).toInt(), liga.substring(liga.indexOf(":") + 1, liga.length()).toInt(), 0);
    RtcDateTime Off = RtcDateTime(now.Year(), now.Month(), now.Day(), desliga.substring(0, desliga.indexOf(":")).toInt(), desliga.substring(desliga.indexOf(":") + 1, desliga.length()).toInt(), 0);
    if (now > On && now < Off) {
      return true;
    }
  }
  return false;
}
#define countof(a) (sizeof(a) / sizeof(a[0]))
void printDateTime(const RtcDateTime& dt)
{
  char datestring[20];

  snprintf_P(datestring,
             countof(datestring),
             PSTR("%02u/%02u/%04u %02u:%02u:%02u"),
             dt.Month(),
             dt.Day(),
             dt.Year(),
             dt.Hour(),
             dt.Minute(),
             dt.Second() );
  Serial.println(datestring);
}

String diaDaSemana() {
  if (!Rtc.IsDateTimeValid())  {
    return "-1";
  }

  if (Rtc.GetIsWriteProtected())  {
    Serial.println("RTC was write protected, enabling writing now");
    Rtc.SetIsWriteProtected(false);
    return "-1";
  }

  if (!Rtc.GetIsRunning())  {
    Serial.println("RTC was not actively running, starting now");
    Rtc.SetIsRunning(true);
    return "-1";
  }
  RtcDateTime date = Rtc.GetDateTime();
  /*Serial.print("\n Dia da semana: ");
    Serial.println(String(date.DayOfWeek()));
    printDateTime(date);
    Serial.println();*/
  return (String(date.DayOfWeek()));
}

double getTemp() {
  double Vout, Rt = 0;
  double T, Tc, Tf = 0;
  double adc = 0;
  for (int i = 0; i < 5; i++) {
    adc += analogRead(ThermistorPin);
    delay(10);
  }
  adc = adc / 5;
  Vout = adc * Vs / adcMax;
  Rt = R1 * Vout / (Vs - Vout);

  T = 1 / (1 / To + log(Rt / Ro) / Beta); // Temperature in Kelvin
  Tc = T - 273.15;
  Tc = String(Tc).substring(0, 5).toDouble();
  return Tc;
}

void responseToClient (AsyncWebServerRequest *req, String res) {
  AsyncWebServerResponse *response = req->beginResponse(200, "text/plain", res);
  response->addHeader("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS,POST,PUT");
  response->addHeader("Access-Control-Allow-Credentials", "true");
  response->addHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
  response->addHeader("Access-Control-Allow-Origin", "*");
  req->send(response);
}

void WiFiStationDisconnected(WiFiEvent_t event, WiFiEventInfo_t info) {
  Serial.println("Disconnected from WiFi access point");
  Serial.print("WiFi lost connection. Reason: ");
  Serial.println(info.disconnected.reason);
  Serial.println("Trying to Reconnect");
  WiFi.begin((const char*) configs["ssid"], (const char*) configs["password"]);
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

void loadHrs() {
  JSONVar saveHrs = JSON.parse(readFile(DAYSINFO));
  JSONVar a = saveHrs.keys();
  int pos = 0;

  for (int i = 0; i < a.length(); i++) {
    JSONVar value = saveHrs[a[i]];
    hrs[pos].semana = String((const char*) value["sem"]);
    hrs[pos].temp = String((const char*) value["temp"]).toDouble();
    hrs[pos].liga = String((const char*) value["liga"]);
    hrs[pos].desliga = String((const char*) value["desliga"]);
    pos++;
  }

}


void setup() {
  Serial.begin(115200);
  while (!Serial);
  Serial.println("ESP INICIADO");
  pinMode(PINORESET, INPUT);

  if (!LITTLEFS.begin(false)) {
    delay(500);
    if (!LITTLEFS.begin(false)) {
      Serial.println("LITTLEFS Mount Failed");
      ESP.restart();
    }
  }

  configs = JSON.parse(readFile(CONFIGURATION));

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

    loadHrs();

    Serial.print("\ntemperatura programada lida: ");
    Serial.println(tempPROG);
  }

  pinMode(RELE_1, OUTPUT);
  digitalWrite(RELE_1, LOW);

  myMutex = xSemaphoreCreateMutex();
  if (myMutex != NULL) {
    xTaskCreatePinnedToCore( taskDim, "taskDim", 10000, NULL, 1, NULL, 0);
    delay(500);
    xTaskCreatePinnedToCore( taskConn, "taskConn",  10000,  NULL,  2,  NULL,  1);
    delay(500);
  }
}

void loop() {
  vTaskDelete(NULL);
}

void taskDim( void * pvParameters ) {

  DimmableLight DIMMER_1(PINO_DIM_1);
  DimmableLight::setSyncPin(PINO_ZC);
  DimmableLight::begin();
  Rtc.Begin();
  if (Rtc.GetIsWriteProtected())
  {
    Serial.println("RTC was write protected, enabling writing now");
    Rtc.SetIsWriteProtected(false);
  }

  if (!Rtc.GetIsRunning())
  {
    Serial.println("RTC was not actively running, starting now");
    Rtc.SetIsRunning(true);
  }

  while ((bool) configs["default"]) {
    vTaskDelete(NULL);
  }

  while (true) {
    if (isUpdate) {
      DimmableLight::pauseStop();
      delay(200);
      vTaskDelete(NULL);
    }

    tempATUAL = getTemp();
    delay(10);
    while (tempATUAL < - 150) {
      tempATUAL = getTemp();
      numError++;
      delay(10);
    }

    if (automaticMode) {
      xSemaphoreTake(myMutex, portMAX_DELAY);
      for (int i = 0; i < NSEMANAS; i++) {
        if (hrs[i].semana.length() > 1) {
          if (hrs[i].semana.indexOf(diaDaSemana()) > -1 && diaDaSemana().toInt() > -1 ) {
            if (estaEntre(hrs[i].liga, hrs[i].desliga)) {
              LINHA_1 = true;
              tempPROG = hrs[i].temp;
              break;
            }
          }
        }
        LINHA_1 = false;
      }
      xSemaphoreGive(myMutex);
    }

    if ((millis() - ultimo_millis2) > debounce_delay) {
      ultimo_millis2 = millis();
      Serial.print(tempATUAL);
      devices["sensor1"] = tempATUAL;
      devices["linha_1"] = LINHA_1;
      devices["tempPROG"] = tempPROG;
      Serial.println("ºC");
      Serial.print("Potencia -> " + String(potencia_1) + " :");
      Serial.println(DIMMER_1.getBrightness());
    }

    if ((millis() - ultimo_millis1) > debounce_delay + 1800000) {
      ultimo_millis1 = millis();
      devices["linha_1"] = LINHA_1;
      devices["tempPROG_1"] = tempPROG;
      DimmableLight::pauseStop();
      delay(10);
      writeFile(JSON.stringify(devices), DEVICESINFO);
      delay(10);
      xSemaphoreTake(myMutex, portMAX_DELAY);
      JSONVar saveHrs;
      JSONVar itemHrs;
      for (int i = 0; i < NSEMANAS; i++) {
        if (hrs[i].semana.length() > 1) {
          Serial.println(hrs[i].semana);
          itemHrs["sem"] = hrs[i].semana;
          itemHrs["temp"] = hrs[i].temp;
          itemHrs["liga"] = hrs[i].liga;
          itemHrs["desliga"] = hrs[i].desliga;
          saveHrs[String(i)] = itemHrs;
        }
      }
      xSemaphoreGive(myMutex);
      writeFile(JSON.stringify(saveHrs), DAYSINFO);
      delay(10);
      saveHrs = undefined;
      itemHrs = undefined;
      DIMMER_1.setBrightness(10);
      DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
    }


    if (tempATUAL < tempPROG - 0.5 && LINHA_1) {
      potencia_1 = 50;
      DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
      ligaRELE(RELE_1);
      rele = true;
    } else if (LINHA_1) {
      rele = false;
      desligaRELE(RELE_1);
      if (tempATUAL != lastTempATUAL) {
        if (tempATUAL < lastTempATUAL && tempATUAL < tempPROG - 0.0) {
          potencia_1 = potencia_1 + 10;
          potencia_1 = constrain(potencia_1, 0, 100);// limita a variavel
          DIMMER_1.setBrightness( (int) map(potencia_1, 0, 100, MINPOT, MAXPOT));
          //Serial.println("aumentando potencia");
        } else if (tempATUAL > tempPROG - 0.1) {
          /*potencia_1 = potencia_1 - 10;
            potencia_1 = constrain(potencia_1, 0, 100);// limita a variavel
            DIMMER_1.setBrightness( (int) map(potencia_1, 0, 100, MINPOT, MAXPOT));
            //Serial.println("baixando potencia");*/
          potencia_1 = 0;
          potencia_1 = constrain(potencia_1, 0, 100);// limita a variavel
          DIMMER_1.setBrightness( (int) map(potencia_1, 0, 100, MINPOT, MAXPOT));
        }
        lastTempATUAL = tempATUAL;
      }
    } else {
      rele = false;
      desligaRELE(RELE_1);
      potencia_1 = 0;
      DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
    }

    delay(5000);
  }
}

void taskConn( void * pvParameters ) {
  if ((bool) configs["default"]) {
    WiFi.mode(WIFI_AP);
    delay(1000);
    Serial.print("macc: ");
    String mac = WiFi.macAddress();
    Serial.println(mac);
    Serial.print("Size of: ");
    Serial.println(sizeof(mac));
    char ssid[mac.length() + 1];
    mac.toCharArray(ssid, mac.length() + 1);
    char pass[8 + 1];
    String((const char*)configs["defaultPassword"]).toCharArray(pass, 8 + 1);
    Serial.println("\nSSID: ");
    Serial.print(ssid);
    Serial.println("\nsenha: ");
    Serial.print(pass);

    WiFi.softAP(ssid, pass);
    delay(2000);
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
    WiFi.onEvent(WiFiStationDisconnected, SYSTEM_EVENT_STA_DISCONNECTED);
    WiFi.onEvent(WiFiStationDisconnected, SYSTEM_EVENT_STA_LOST_IP);
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
      configs["client_id"] = (const char*) jso["chave"];
      configs["user_email"] = (const char*) jso["email"];
      configs["user_senha"] = (const char*) jso["senha"];

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
      if (jso.hasOwnProperty("upgrade")) {
        isUpdate = true;
      }

      ok["linha_1"] = LINHA_1;
      ok["tempPROG"] = tempPROG;
      ok["request"] = "ok";
    }
    responseToClient(request, JSON.stringify(ok));
  });

  server.on("/get", HTTP_GET, [](AsyncWebServerRequest * request) {
    JSONVar ok;
    ok["request"] = "success";
    ok["sensor1"] = tempATUAL;
    ok["linha_1"] = LINHA_1;
    ok["tempPROG"] = tempPROG;
    responseToClient(request, JSON.stringify(ok));
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
  Serial.println("TCP server started");
  /*
    if ((millis() - ultimo_millis3) > 5000) { // se ja passou determinado tempo que o botao foi precionado
      ultimo_millis3 = millis();
      MDNS.addService("dimmer", "tcp", 80);
    }*/

  MDNS.addService("dimmer", "tcp", 80);
  Serial.println("HTTP server started");

  while ((bool) configs["default"]) {
    delay(1);
  }

  FirebaseAuth auth;
  FirebaseConfig configur;

  char hostt[host.length() + 1];
  host.toCharArray(hostt, host.length() + 1);

  char api_keyy[api_key.length() + 1];
  api_key.toCharArray(api_keyy, api_key.length() + 1);

  char emaill[email.length() + 1];
  email.toCharArray(emaill, email.length() + 1);

  char senhaa[senha.length() + 1];
  senha.toCharArray(senhaa, senha.length() + 1);

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

  Firebase.begin(&configur, &auth);
  String nodo = "/cliente/" + String((const char *) configs["client_id"]) + "/" + String((const char*) configs["deviceName"]) + "/";
  Firebase.setFloatDigits(2);
  Firebase.setDoubleDigits(2);
  FirebaseJson json2, json1;
  json1.set("update", false);
  if (!Firebase.updateNode(readData, nodo + "R/info/", json1)) {
    Serial.println("Erro ao atualizar boolean");
  }
  if (!Firebase.setTimestamp(writeData, nodo + "/W/uptime")) {
    Serial.println("Erro ao gravar a data em nuvem");
  }
  json1.remove("update");
  json1.set("ver", VERSION);
  if (!Firebase.updateNode(writeData, nodo + "/W/", json1)) {
    Serial.println(writeData.errorReason());
  }
  Firebase.reconnectWiFi(true);

  if (!Firebase.beginMultiPathStream(readData, nodo + "R/", childPath, childPathSize))
  {
    Serial.println("------------------------------------");
    Serial.println("Can't begin stream connection...");
    Serial.println("REASON: " + readData.errorReason());
    Serial.println("------------------------------------");
    Serial.println();
  }

  //Set the reserved size of stack memory in bytes for internal stream callback processing RTOS task.
  //8192 is the minimum size.
  Firebase.setMultiPathStreamCallback(readData, streamCallback, streamTimeoutCallback, 8192);
  short isReset = 0;

  while (true) {


    /*if (digitalRead(PINORESET) == LOW) {
      while (digitalRead(PINORESET) == LOW) {
        delay(1000);
        isReset++;
      }
      }*/
    if (isReset > 5) {
      configs["default"] = true;
      writeFile(JSON.stringify(configs), CONFIGURATION);
      Serial.println("RESET BY PIN");
      ESP.restart();
    }


    if ((millis() - ultimo_millis3) > 10000 || updateValues) {
      ultimo_millis3 = millis();

      if (WiFi.status() == WL_CONNECTED) {
        json2.set("tempPROG", tempPROG);
        json2.set("tempATUAL", tempATUAL);//(int) random(0, 40));
        json2.set("LINHA_1", LINHA_1);
        json2.set("potencia", potencia_1);
        json2.set("erro leitura", numError);
        json2.set("auto", automaticMode);
        json2.set("sinal", String(WiFi.RSSI()));

        if (!Firebase.updateNode(writeData, nodo + "/W/", json2)) {
          Serial.println(writeData.errorReason());
        }
        Firebase.setTimestamp(writeData, nodo + "/W/Timestamp");
        writeData.stopWiFiClient();

        if (isUpdate) {
          String url = "http://update.gigabyte.inf.br/update.php?ver=";
          url.concat(VERSION);
          url.concat("&model=");
          url.concat(MODEL);
          Serial.println(url);
          t_httpUpdate_return ret = ESPhttpUpdate.update(url);
          switch (ret) {
            case HTTP_UPDATE_FAILED:
              Serial.printf("HTTP_UPDATE_FAILD Error (%d): %s", ESPhttpUpdate.getLastError(), ESPhttpUpdate.getLastErrorString().c_str());
              ESP.restart();
              break;

            case HTTP_UPDATE_NO_UPDATES:
              Serial.println("HTTP_UPDATE_NO_UPDATES");
              ESP.restart();
              break;

            case HTTP_UPDATE_OK:
              Serial.println("HTTP_UPDATE_OK");
              ESP.restart();
              break;
          }
        }
      } else {
        WiFi.reconnect();
      }
      updateValues = false;
    }
    delay(1);
  }
}