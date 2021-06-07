#include <OneWire.h>
#include <DallasTemperature.h>
#include <ArduinoJson.h>
#include <LITTLEFS.h>
#include <WiFi.h>
#include "ESPAsyncWebServer.h"
#include <Arduino_JSON.h>
#include <dimmable_light.h>
#include <ESPmDNS.h>
#include <ESP32httpUpdate.h>
#include <ThreeWire.h>
#include <RtcDS1302.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <NTPClient.h>
#include <WiFiClientSecure.h>
#include <uuid.h>

WiFiUDP udp;
NTPClient ntp(udp, "a.st1.ntp.br", -3 * 3600, 60000);

const char certificado[] =  "-----BEGIN CERTIFICATE-----\n"\
                            "MIIDSjCCAjKgAwIBAgIQRK+wgNajJ7qJMDmGLvhAazANBgkqhkiG9w0BAQUFADA/\n"\
                            "MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT\n"\
                            "DkRTVCBSb290IENBIFgzMB4XDTAwMDkzMDIxMTIxOVoXDTIxMDkzMDE0MDExNVow\n"\
                            "PzEkMCIGA1UEChMbRGlnaXRhbCBTaWduYXR1cmUgVHJ1c3QgQ28uMRcwFQYDVQQD\n"\
                            "Ew5EU1QgUm9vdCBDQSBYMzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n"\
                            "AN+v6ZdQCINXtMxiZfaQguzH0yxrMMpb7NnDfcdAwRgUi+DoM3ZJKuM/IUmTrE4O\n"\
                            "rz5Iy2Xu/NMhD2XSKtkyj4zl93ewEnu1lcCJo6m67XMuegwGMoOifooUMM0RoOEq\n"\
                            "OLl5CjH9UL2AZd+3UWODyOKIYepLYYHsUmu5ouJLGiifSKOeDNoJjj4XLh7dIN9b\n"\
                            "xiqKqy69cK3FCxolkHRyxXtqqzTWMIn/5WgTe1QLyNau7Fqckh49ZLOMxt+/yUFw\n"\
                            "7BZy1SbsOFU5Q9D8/RhcQPGX69Wam40dutolucbY38EVAjqr2m7xPi71XAicPNaD\n"\
                            "aeQQmxkqtilX4+U9m5/wAl0CAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNV\n"\
                            "HQ8BAf8EBAMCAQYwHQYDVR0OBBYEFMSnsaR7LHH62+FLkHX/xBVghYkQMA0GCSqG\n"\
                            "SIb3DQEBBQUAA4IBAQCjGiybFwBcqR7uKGY3Or+Dxz9LwwmglSBd49lZRNI+DT69\n"\
                            "ikugdB/OEIKcdBodfpga3csTS7MgROSR6cz8faXbauX+5v3gTt23ADq1cEmv8uXr\n"\
                            "AvHRAosZy5Q6XkjEGB5YGV8eAlrwDPGxrancWYaLbumR9YbK+rlmM6pZW87ipxZz\n"\
                            "R8srzJmwN0jP41ZL9c8PDHIyh8bwRLtTcm1D9SZImlJnt1ir/md2cXjbDaJWFBM5\n"\
                            "JDGFoqgCWjBH4d1QB7wCCZAA62RjYJsWvIjJEubSfZGL+T0yjWW06XyxV3bqxbYo\n"\
                            "Ob8VZRzI9neWagqNdwvYkQsEjgfbKbYK7p2CNTUQ\n"\
                            "-----END CERTIFICATE-----\n";



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
#define VERSION "1.02"
#define MODEL "casa"
#define NSEMANAS 35
#define SERVIDOR "https://gigabyte.inf.br/"



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
volatile short esp_ativo = 1;
volatile short logado = 0;
volatile boolean saveConfig = false;
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
unsigned long millisWifiTimeout = 0;
unsigned long debounce_delay = 500;

void limpaHorarios() {
  for (int i = 0; i < NSEMANAS; i++) {
    hrs[i].semana = undefined;
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
  Serial.print("\n Dia da semana: ");
  Serial.println(String(date.DayOfWeek()));
  printDateTime(date);
  Serial.println();
  return (String(date.DayOfWeek()));
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
  Serial.print("\n\n\nCONTEUDO LIDO: ");
  Serial.println(content + "\n\n\n");
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
    Serial.print("- write failed for: ");
    Serial.println(path);
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
    email = String((const char*)configs["user_email"]);
    senha = String((const char*)configs["user_senha"]);
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

  DimmableLight DIMMER_1(PINO_DIM_1);
  DimmableLight::setSyncPin(PINO_ZC);
  DimmableLight::begin();


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
      DimmableLight::pauseStop();
      delay(200);
      vTaskDelete(NULL);
    }

    sensors.requestTemperatures();
    tempATUAL = sensors.getTempCByIndex(0);
    delay(10);
    while (tempATUAL < - 150) {
      sensors.requestTemperatures();
      tempATUAL = sensors.getTempCByIndex(0);
      numError++;
      delay(10);
    }

    if (automaticMode) {
      xSemaphoreTake(myMutex, portMAX_DELAY);
      for (int i = 0; i < NSEMANAS; i++) {
        if (hrs[i].semana.length() > 0) {
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


    if ((millis() - ultimo_millis2) > debounce_delay) {
      ultimo_millis2 = millis();
      Serial.print(tempATUAL);
      devices["sensor1"] = tempATUAL;
      devices["linha_1"] = LINHA_1;
      devices["tempPROG"] = tempPROG;
      Serial.println("ºC");
      Serial.print("Potencia -> " + String(potencia_1) + " :");
      Serial.println(DIMMER_1.getBrightness());
      Serial.print("date: ");
      printDateTime(Rtc.GetDateTime());
      Serial.println();
    }

    if ((millis() - ultimo_millis1) > debounce_delay + 1800000 || saveConfig) {
      ultimo_millis1 = millis();
      devices["linha_1"] = LINHA_1;
      devices["tempPROG_1"] = tempPROG;
      DimmableLight::pauseStop();
      delay(10);
      writeFile(JSON.stringify(devices), DEVICESINFO);
      delay(10);
      writeFile(JSON.stringify(configs), CONFIGURATION);
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
      saveConfig = false;
      DIMMER_1.setBrightness(10);
      DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
    }


    if (tempATUAL < tempPROG - 0.5 && LINHA_1) {
      potencia_1 = 50;
      DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
      ligaRELE(RELE_1);
      //Serial.println("rele ligado");
      rele = true;
    } else if (LINHA_1) {
      rele = false;
      desligaRELE(RELE_1);
      if (tempATUAL != lastTempATUAL) {
        //Serial.println("Temp diff");
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
      //Serial.println("rele desligado");
    } else {
      rele = false;
      desligaRELE(RELE_1);
      potencia_1 = 0;
      DIMMER_1.setBrightness( map(potencia_1, 0, 100, MINPOT, MAXPOT));
      //Serial.println("desliga rele e lampada");
    }

    delay(5000);
  }
}

boolean login(String endereco, String email, String senha, String uuid) {
  WiFiClientSecure client;
  client.setCACert(certificado);
  //WiFiClient *client = new WiFiClient;
  HTTPClient https;
  Serial.println("Vou me conectar");
  boolean ret = false;
  if (https.begin(client, SERVIDOR + endereco)) {
    https.addHeader("Content-Type", "application/json");
    Serial.println("resposta");
    JSONVar credenciais;
    credenciais["email"] = email;
    credenciais["senha"] = senha;
    credenciais["uuid_cliente"] = uuid;
    int httpCode = https.POST(JSON.stringify(credenciais));
    if (httpCode == 200) {
      String res = https.getString();
      Serial.println(res);
      JSONVar jso =  JSON.parse(res);
      if (jso.hasOwnProperty("code") && jso.hasOwnProperty("esp_ativo")) {
        if (((int) jso["code"]) == 200) {
          Serial.println("Loguei com sucesso!");
          esp_ativo = String((const char*) jso["esp_ativo"]).toInt();
          ret = true;
        }
      }
    } else {
      Serial.println("Erro HTTP Codigo: " + String(httpCode));
    }
  }
  return ret;
}

boolean intToBoolean(int a) {
  boolean ret = false;
  if (a == 1) {
    ret = true;
  }
  return ret;
}

boolean isValidNumber (String str) {
  for (byte i = 0; i < str.length (); i ++) {
    if (!isDigit(str.charAt (i))) return false;
  }
  return true;
}

boolean api(String endereco, JSONVar dados) {
  WiFiClientSecure client;
  client.setCACert(certificado);
  //WiFiClient *client = new WiFiClient;
  HTTPClient https;
  boolean ret = false;
  if (https.begin(client, SERVIDOR + endereco)) {
    https.addHeader("Content-Type", "application/json");
    Serial.println("resposta");
    int httpCode = https.POST(JSON.stringify(dados));
    if (httpCode == 200) {
      String res = https.getString();
      Serial.println(res);
      JSONVar jso =  JSON.parse(res);
      if (jso.hasOwnProperty("code")) {
        if (((int) jso["code"]) == 200) {
          ret = true;
        } else if (((int) jso["code"]) == 201) {
          ret = true;
          JSONVar dados =  jso["dispositivo"];
          automaticMode = intToBoolean(String((const char*) dados["auto_esp_ler"]).toInt());
          if (!automaticMode) {
            tempPROG = String((const char*) dados["temp_prog_esp_ler"]).toDouble();
          }
          modoViagem = intToBoolean(String((const char*) dados["modo_viagem_esp_ler"]).toInt());
          LINHA_1 = intToBoolean(String((const char*) dados["status_esp_ler"]).toInt());
          JSONVar k = jso.keys();
          byte pos = 0;
          for (int i = 0; i < k.length(); i++) {
            if (isValidNumber(String((const char*)k[i]))) {
              JSONVar prog = jso[k[i]];
              if (prog.hasOwnProperty("dia_semana")) {
                Serial.println(prog);
                Serial.println(String((const char*) prog["temp_prog"]).toDouble());
                xSemaphoreTake(myMutex, portMAX_DELAY);
                hrs[pos].semana = String((const char*) prog["dia_semana"]);
                hrs[pos].temp = String((const char*) prog["temp_prog"]).toDouble();
                hrs[pos].liga = String((const char*) prog["liga"]);
                hrs[pos].desliga = String((const char*) prog["desliga"]);
                Serial.print("\n\n");
                Serial.print("semana:" );
                Serial.println(hrs[pos].semana);
                Serial.print("liha:" );
                Serial.println(hrs[pos].liga);
                Serial.print("desliga:" );
                Serial.println(hrs[pos].desliga);
                Serial.print("temp:" );
                Serial.println(hrs[pos].temp);
                Serial.println("\n\n");
                xSemaphoreGive(myMutex);
                pos++;
              }
            }
          }
        }
      }
    } else {
      Serial.println("Erro HTTP Codigo: " + String(httpCode));
    }
  }
  return ret;
}

void taskConn( void * pvParameters ) {
  short isReset = 0;
  Wire.begin(22, 23);
  LiquidCrystal_I2C lcd(0x27, 16, 2);
  lcd.begin();
  lcd.backlight();
  WiFiClient client;

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
    //Serial.println((const char*) configs["password"]);
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

  if (String((const char*)configs["uuid_dispositivo"]).indexOf("null") > -1) {
    Serial.println("Irei me registrar.");
    JSONVar registro;
    registro["nome"] = String((const char*)configs["deviceName"]);
    registro["uuid_cliente"] = String((const char*)configs["uuid_cliente"]);
    registro["uuid_dispositivo"] = StringUUIDGen();
    while (!api("api/dispositivo/registrar", registro)) {
      delay(10000);
    }
    configs["uuid_dispositivo"] = String((const char*)registro["uuid_dispositivo"]);
    saveConfig = true;
  }

  while (logado == 0) {
    Serial.println("Estou logando...");
    if (login("api/login/esp", String((const char*)configs["user_email"]), String((const char*)configs["user_senha"]), String((const char*)configs["uuid_cliente"]))) {
      logado = 1;
    }
    delay(5000);
  }

  if (esp_ativo == 0) {
    Serial.println("ESP nao esta licenciado.");
    delay(60 * 60 * 1000);//minutos*60*1000
    ESP.restart();
  }

  JSONVar boot;
  boot["uuid_dispositivo"] = String((const char*)configs["uuid_dispositivo"]);
  boot["versao"] = VERSION;
  while (!api("api/dispositivo/boot", boot)) {
    Serial.println("Vou dar boot");
    delay(10000);
  }



  while (true) {
    if ((millis() - ultimo_millis3) > 15 * 60 * 1000) { //minutos*60*1000
      ultimo_millis3 = millis();
      if (ntp.forceUpdate()) {
        RtcDateTime timee = ntp.getEpochTime();
        Rtc.SetDateTime(timee - 946684800);
        Serial.println("\nHorario atualizado pela WEB!");
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


    if ((millis() - ultimo_millis3) > 10000 || updateValues) {
      ultimo_millis3 = millis();
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
        lcd.print("T. Real:");
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

      if (WiFi.status() == WL_CONNECTED) {
        JSONVar dados;
        dados["temp_atual"] = tempATUAL; //(int) random(0, 40));
        dados["status"] = LINHA_1;
        dados["potencia"] = potencia_1;
        dados["erro_leitua"] = numError;
        dados["sinal"] = String(WiFi.RSSI());
        dados["uuid"] = String((const char*)configs["uuid_dispositivo"]);
        dados["temp_prog"] = tempPROG;
        dados["auto"] = automaticMode;
        dados["modo_viagem"] = modoViagem;

        if (api("api/dispositivo/set_get", dados)) {
          xSemaphoreTake(myMutex, portMAX_DELAY);
          millisWifiTimeout = millis();
          xSemaphoreGive(myMutex);
        } else {
          Serial.println("Erro ao enviar dados");
        }

        if (isUpdate) {
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
