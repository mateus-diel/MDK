#include <OneWire.h>
#include <DallasTemperature.h>
#include <ArduinoJson.h>
#include <LITTLEFS.h>
#include <WiFi.h>
#include "ESPAsyncWebServer.h"
#include <Arduino_JSON.h>
#include <ESPmDNS.h>
#include "FirebaseESP32.h"
#include <ESP32httpUpdate.h>
#include <ThreeWire.h>
#include <RtcDS1302.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <NTPClient.h>

WiFiUDP udp;
NTPClient ntp(udp, "a.st1.ntp.br", -3 * 3600, 60000);



ThreeWire myWire(21, 19, 18); // IO, SCLK, CE
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

#define PINO_DIM_1    4
#define PINO_ZC     2
#define MAXPOT 252
#define MINPOT  0
#define PINORESET 27
#define CONFIGURATION "/configs.json"
#define DEVICESINFO "/devices.json"
#define DAYSINFO "/days.json"
#define VERSION "1.02_striac"
#define MODEL "casa"
#define NSEMANAS 35



Horarios hrs[NSEMANAS];
SemaphoreHandle_t myMutex;

//Array simbolo grau
byte grau[8] = { B00001100,
                 B00010010,
                 B00010010,
                 B00001100,
                 B00000000,
                 B00000000,
                 B00000000,
                 B00000000,
               };


FirebaseData writeData;
FirebaseData readData;
String email;
String senha;
String api_key;
String host;

AsyncWebServer  server(80);

short RELE_1 = 16;

volatile float tempPROG = 35.0;
volatile float tempATUAL = 0.0;
volatile float lastTempATUAL = 0.0;
volatile boolean rele = false;
volatile bool isUpdate = false;
volatile bool modoViagem = false;
volatile int numError = 0;
volatile bool updateValues = false;
volatile bool automaticMode = false;
JSONVar configs;
JSONVar devices;
JSONVar times;

volatile boolean LINHA_1 = true;

const int oneWireBus = 15;

OneWire oneWire(oneWireBus);

DallasTemperature sensors(&oneWire);


volatile int potencia_1 = 0;
unsigned long ultimo_millis1 = 0;
unsigned long ultimo_millis2 = 0;
unsigned long ultimo_millis3 = 0;
unsigned long millis_lcd = 0;
unsigned long millisWifiTimeout = 0;
unsigned long debounce_delay = 500;

void limpaHorarios() {
  for (int i = 0; i < NSEMANAS; i++) {
    hrs[i].semana = undefined;
  }
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
  Serial.print(datestring);
}

void streamCallback(MultiPathStreamData stream)
{
  /*Serial.println();
    Serial.println("Stream Data1 available...");
    Serial.println("path: " + stream.dataPath);
    Serial.println("valuie: " + stream.value);*/

  size_t numChild = sizeof(childPath) / sizeof(childPath[0]);

  for (size_t i = 0; i < numChild; i++)
  {
    if (stream.get(childPath[i]))
    {
      //Serial.println("path: " + stream.dataPath + ", type: " + stream.type + ", value: " + stream.value);
      if (stream.dataPath.indexOf(childPath[1]) > -1 && stream.type.indexOf("json") > -1) {
        JSONVar infos = JSON.parse(stream.value);
        if (infos.hasOwnProperty("update")) {
          isUpdate = (bool) infos["update"];
        }
        if (infos.hasOwnProperty("modoViagem")) {
          modoViagem = (bool) infos["modoViagem"];
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
    Serial.println();
    return (String(date.DayOfWeek()));*/
  return "";
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
  //Serial.print("CONTEUDO LIDO: ");
  //Serial.println(content);
  return content;
}

boolean writeFile(String message, String path) {
  //Serial.printf("Writing file: %s\r\n", path);

  File file = LITTLEFS.open(path, FILE_WRITE);
  if (!file) {
    Serial.println("- failed to open file for writing");
    return false;
  }
  if (file.print(message)) {
    //Serial.println("- file written");
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

    sensors.begin();
    loadHrs();

    Serial.print("\ntemperatura programada lida: ");
    Serial.println(tempPROG);
  }

  pinMode(RELE_1, OUTPUT);
  digitalWrite(RELE_1, LOW);



  myMutex = xSemaphoreCreateMutex();
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
  if (myMutex != NULL) {
  xTaskCreatePinnedToCore( taskConn, "taskConn",  10000,  NULL,  2,  NULL,  1);
    delay(500);
    xTaskCreatePinnedToCore( taskDim, "taskDim", 10000, NULL, 1, NULL, 0);
    delay(500);
  }
}

void loop() {
  vTaskDelete(NULL);
}

void taskDim( void * pvParameters ) {
  potencia_1 = 0;
  desligaRELE(RELE_1);
  
  while ((bool) configs["default"]) {
    vTaskDelete(NULL);
  }
  
  while (true) {
    unsigned long mil = 0;
    xSemaphoreTake(myMutex, portMAX_DELAY);
    mil = millisWifiTimeout;
    xSemaphoreGive(myMutex);
    if ((millis() - mil) > 300000) {
      Serial.println("Vou reiniciar");
      ESP.restart();
    }

    if (isUpdate) {
      delay(200);
      vTaskDelete(NULL);
    }

    sensors.requestTemperatures();
    delay(800);
    float t = 0.0;
    t = sensors.getTempCByIndex(0);
    
    while (t < - 100 || t > 84) {
      Serial.print("Erro sensor, temp lida: ");
      Serial.println(t);
      sensors.requestTemperatures();
      delay(1000);
      t = sensors.getTempCByIndex(0);
      numError++;
    }
    tempATUAL = t;

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
        LINHA_1 = true;
        tempPROG = 10.0;
      }
      xSemaphoreGive(myMutex);
    }

    if (modoViagem) {
      LINHA_1 = true;
      tempPROG = 10.0;
    }

    /*lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("T. Prog:");
      lcd.setCursor(8, 0);
      lcd.print(tempPROG);
      lcd.setCursor(0, 1);
      lcd.print("T. Obs:");
      lcd.setCursor(7, 1);
      lcd.print(tempATUAL);*/


    if ((millis() - ultimo_millis2) > debounce_delay) {
      ultimo_millis2 = millis();
      Serial.print(tempATUAL);
      devices["sensor1"] = tempATUAL;
      devices["linha_1"] = LINHA_1;
      devices["tempPROG"] = tempPROG;
      Serial.println("ºC");
      Serial.print("Potencia -> " + String(potencia_1) + " :");
      Serial.print("date: ");
      printDateTime(Rtc.GetDateTime());
      Serial.println();
    }

    if ((millis() - ultimo_millis1) > debounce_delay + 1800000) {
      ultimo_millis1 = millis();
      devices["linha_1"] = LINHA_1;
      devices["tempPROG_1"] = tempPROG;
      delay(10);
      writeFile(JSON.stringify(devices), DEVICESINFO);
      delay(10);
      xSemaphoreTake(myMutex, portMAX_DELAY);
      JSONVar saveHrs;
      JSONVar itemHrs;
      for (int i = 0; i < NSEMANAS; i++) {
        if (hrs[i].semana.length() > 1) {
          //Serial.println(hrs[i].semana);
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
     
    }


    if (tempATUAL < tempPROG && LINHA_1) {
      potencia_1 = 50;
      ligaRELE(RELE_1);
      rele = true;
    } else {
      rele = false;
      desligaRELE(RELE_1);
      potencia_1 = 0;
    }

    delay(5000);
  }
}

void taskConn( void * pvParameters ) {
  short isReset = 0;
  Wire.begin(22, 23);
  LiquidCrystal_I2C lcd(0x27, 16, 2);
  lcd.begin();
  lcd.backlight();

 

  if ((bool) configs["default"]) {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Configurar");
    WiFi.mode(WIFI_AP);
    delay(1000);
    Serial.print("macc: ");
    String mac = WiFi.macAddress();
    Serial.println(mac);
    lcd.setCursor(0, 1);
    lcd.print(mac);
    Serial.print("Size of: ");
    Serial.println(sizeof(mac));
    char ssid[mac.length() + 1];
    mac.toCharArray(ssid, mac.length() + 1);
    char pass[8 + 1];
    String((const char*)configs["defaultPassword"]).toCharArray(pass, 8 + 1);
    Serial.println("\nSSID: ");
    Serial.print(ssid);
    //Serial.println("\nsenha: ");
    //Serial.print(pass);

    WiFi.softAP(ssid, pass);
    delay(2000);
    Serial.print("AP IP address: ");
    Serial.println(WiFi.softAPIP());
  } else {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Iniciando...");
    WiFi.mode(WIFI_MODE_STA);
    delay(1000);
    
    WiFi.begin((const char*) configs["ssid"], (const char*) configs["password"]);
    Serial.print("Wifi e senha: ");
    Serial.print((const char*) configs["ssid"]);

    Serial.println((const char*) configs["password"]);
    delay(1000);
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Conectando em:");
    lcd.setCursor(0, 1);
    lcd.print((const char*) configs["ssid"]);
    int wifi = 0;
    while (WiFi.status() != WL_CONNECTED) {
      delay(1000);
      Serial.println("Connecting to WiFi..");
      if (wifi > 180) {
        ESP.restart();
      }
      /*if (digitalRead(PINORESET) == LOW) {
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
      }*/
      wifi++;
    }
    delay(500);
    ntp.begin();
    delay(500);
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Conectado:");
    lcd.setCursor(0, 1);
    lcd.print((const char*) configs["ssid"]);
    if (ntp.forceUpdate()) {
      RtcDateTime timee = ntp.getEpochTime();
      Rtc.SetDateTime(timee - 946684800);
      Serial.println("\nHorario atualizado pela WEB!");
    }
    WiFi.onEvent(WiFiStationDisconnected, SYSTEM_EVENT_STA_DISCONNECTED);
    WiFi.onEvent(WiFiStationDisconnected, SYSTEM_EVENT_STA_LOST_IP);
    //Serial.print("Ip->: ");
    //Serial.println(WiFi.localIP());
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

    //Serial.println(t);
    //Serial.println("Json");
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
      if (jso.hasOwnProperty("auto")) {
        automaticMode = (bool) jso["auto"];
      }

      ok["linha_1"] = LINHA_1;
      ok["tempPROG"] = tempPROG;
      ok["auto"] = automaticMode;
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
    ok["auto"] = automaticMode;
    responseToClient(request, JSON.stringify(ok));
  });

  server.begin();

  if (configs.hasOwnProperty("deviceName")) {
    if (!MDNS.begin((const char*) configs["deviceName"])) {
      Serial.println("Error setting up MDNS responder!");
      delay(1000);
      ESP.restart();
    }
    //Serial.println("MDNS begib successful;");
  } else {
    if (!MDNS.begin("ESP32")) {
      Serial.println("Error setting up MDNS responder!");
      delay(1000);
      ESP.restart();
    }
  }

  Serial.println("mDNS responder started");
  Serial.println("TCP server started");


  MDNS.addService("dimmer", "tcp", 80);
  //Serial.println("HTTP server started");

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

  /*Serial.print("\napi key: ");
    Serial.println(api_keyy);
    Serial.print("host ");
    Serial.println(hostt);
    Serial.print("email: ");
    Serial.println(emaill);
    Serial.print("senha: ");
    Serial.println(senhaa);*/

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





  while (true) {

    if ((millis() - ultimo_millis3) > 15 * 60 * 1000) { //minutos*60*1000
      ultimo_millis3 = millis();
      if (ntp.forceUpdate()) {
        //RtcDateTime timee = ntp.getEpochTime();
        //Rtc.SetDateTime(timee-946684800);
        //Serial.println("\nHorario atualizado pela WEB!");
      }

    }


    /*if (digitalRead(PINORESET) == LOW) {
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
    }*/

    if ((millis() - millis_lcd) > 1000) {
      millis_lcd = millis();
      Wire.begin(22, 23);
      lcd.begin();
      lcd.backlight();
      if (LINHA_1) {
        lcd.clear();
        lcd.createChar(0, grau);
        lcd.setCursor(0, 0);
        lcd.print("T. Prog:");
        lcd.setCursor(8, 0);
        lcd.print(tempPROG);
        lcd.setCursor(14, 0);
        lcd.write((byte)0);
        lcd.setCursor(15, 0);
        lcd.print("C");
        lcd.setCursor(0, 1);
        lcd.print("T. Obs:");
        lcd.setCursor(8, 1);
        lcd.print(tempATUAL);
        lcd.setCursor(14, 1);
        lcd.write((byte)0);
        lcd.setCursor(15, 1);
        lcd.print("C");
      } else {
        lcd.clear();
        lcd.setCursor(0, 0);
        lcd.print("Aquecimento");
        lcd.setCursor(0, 1);
        lcd.print("desligado!");
      }
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
        json2.set("modoViagem", modoViagem);

        if (Firebase.updateNode(writeData, nodo + "/W/", json2)) {
          xSemaphoreTake(myMutex, portMAX_DELAY);
          millisWifiTimeout = millis();
          xSemaphoreGive(myMutex);
        } else {
          Serial.println(writeData.errorReason());
        }
        Firebase.setTimestamp(writeData, nodo + "/W/Timestamp");
        writeData.stopWiFiClient();

        if (isUpdate) {
          delay(8000);
          String url = "http://update.gigabyte.inf.br/update.php?ver=";
          url.concat(VERSION);
          url.concat("&model=");
          url.concat(MODEL);
          //Serial.println(url);
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
