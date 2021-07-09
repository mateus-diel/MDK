#include <OneWire.h>
#include <DallasTemperature.h>
#include <WiFi.h>
#include <ThreeWire.h>
#include <RtcDS1302.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <NTPClient.h>
#include <dimmable_light.h>

#define countof(a) (sizeof(a) / sizeof(a[0]))
#define RELE_1 16
#define PINO_DIM_1 4
#define PINO_ZC 2

boolean teste = true;

ThreeWire myWire(21, 19, 18); // IO, SCLK, CE
RtcDS1302<ThreeWire> Rtc(myWire);

WiFiUDP udp;
NTPClient ntp(udp, "a.st1.ntp.br", -3 * 3600, 60000);
const int oneWireBus = 15;
OneWire oneWire(oneWireBus);
DallasTemperature sensors(&oneWire);
unsigned long millis_lcd = 0;
unsigned long millis_dim = 0;


void setup() {
  Serial.begin(115200);
  while (!Serial);
  Serial.println("ESP INICIANDO");
  Rtc.Begin();
  if (Rtc.GetIsWriteProtected()) {
    Serial.println("RTC está protegido contra gravação, habilitando gravação agora");
    Rtc.SetIsWriteProtected(false);
  }

  if (!Rtc.GetIsRunning()) {
    Serial.println("RTC não está sendo executado, iniciando agora");
    Rtc.SetIsRunning(true);
  }
  xTaskCreatePinnedToCore( taskConn, "taskConn",  10000,  NULL,  2,  NULL,  1);
  delay(500);
  xTaskCreatePinnedToCore( taskDim, "taskDim", 10000, NULL, 1, NULL, 0);
  delay(500);
}

void loop() {
  vTaskDelete(NULL);
}

String printDateTime(const RtcDateTime& dt) {
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
  return datestring;
}

String hora(const RtcDateTime& dt) {
  char hr[20];
  snprintf_P(hr,
             countof(hr),
             PSTR("%02u:%02u:%02u"),
             dt.Hour(),
             dt.Minute(),
             dt.Second() );
  return hr;
}

void taskDim( void * pvParameters ) {
  DimmableLight DIMMER_1(PINO_DIM_1);
  DimmableLight::setSyncPin(PINO_ZC);
  DimmableLight::begin();
  DIMMER_1.setBrightness(map(0, 0, 100, 0, 250));
  sensors.begin();

  while (true) {
    sensors.requestTemperatures();
    delay(1000);
    float t = 0.0;
    t = sensors.getTempCByIndex(0);

    if (t < -100 || t > 84) {
      Serial.print("Erro sensor, temp lida: ");
      Serial.println(t);
    } else {
      Serial.print("Temp lida: ");
      Serial.println(t);
    }
    for (int i = 0; i < 5; i++) {
      if (i % 2 == 0) {
        digitalWrite(RELE_1, HIGH);
        Serial.println("Rele Ligado!");
      } else {
        digitalWrite(RELE_1, LOW);
        Serial.println("Rele Desligado!");
      }
      delay(1000);
    }
    digitalWrite(RELE_1, LOW);
    Serial.println("Rele Desligado!\nIrei aumentar o valor do TRIAC!");
    for (int i = 0; i < 100; i++) {
      DIMMER_1.setBrightness(map(i, 0, 100, 0, 250));
      delay(100);
    }
    Serial.println("Irei Diminuir o valor do TRIAC!");
    for (int i = 100; i > 0; i--) {
      DIMMER_1.setBrightness(map(i, 0, 100, 0, 250));
      delay(100);
    }
    delay(1);
  }
}


void taskConn( void * pvParameters ) {
  while (true) {
    if (teste) {
      RtcDateTime date = Rtc.GetDateTime();
      printDateTime(date);
      delay(1000);
    } else {
      WiFi.mode(WIFI_MODE_STA);
      delay(1000);
      WiFi.begin("GIGA_ASSISTENCIA", "giga2020a"); //trocar pelo nome e senha da wifi
      while (WiFi.status() != WL_CONNECTED) {
        delay(1000);
        Serial.println("Conectando WiFi...");
      }
      ntp.begin();
      delay(500);
      Serial.println("Irei ajustar o horário do RTC...");
      boolean b = false;
      while (b != true) {
        b =  ntp.forceUpdate();
        RtcDateTime timee = ntp.getEpochTime();
        Rtc.SetDateTime(timee - 946684800);
        Serial.println("\nHorario atualizado pela WEB!");
      }
      while (true) {
        if ((millis() - millis_lcd) > 1000) { //minutos*60*1000
          millis_lcd = millis();
          Wire.begin(22, 23);
          LiquidCrystal_I2C lcd(0x27, 16, 2);
          lcd.begin();
          lcd.backlight();
          RtcDateTime date = Rtc.GetDateTime();
          printDateTime(date);
          lcd.clear();
          lcd.setCursor(0, 0);
          lcd.print("Hr: ");
          lcd.print(hora(date));
        }
        delay(1);
      }
    }
  }
}
