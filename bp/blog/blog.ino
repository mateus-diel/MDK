#include <OneWire.h>
#include <DallasTemperature.h>
#include <WiFi.h>
const int oneWireBus = 15;
OneWire oneWire(oneWireBus);
DallasTemperature sensors(&oneWire);

void setup() {
  Serial.begin(115200);
  while (!Serial);
  Serial.println("ESP INICIANDO");
  xTaskCreatePinnedToCore( taskConn, "taskConn",  10000,  NULL,  2,  NULL,  1);
  delay(500);
  xTaskCreatePinnedToCore( taskDim, "taskDim", 10000, NULL, 1, NULL, 0);
  delay(500);
}

void loop() {
  vTaskDelete(NULL);
}

void taskDim( void * pvParameters ) {
  while (true) {
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
    }
    Serial.println(t);
    if (t > 30.0) {
      //se a temperatura for tal então faça tal
    }
    delay(1);
  }
}


void taskConn( void * pvParameters ) {
  while (true) {
    WiFi.mode(WIFI_MODE_STA);
    delay(1000);

//se eu colocar o while(true){} aqui e "travar" o codigo antes da wifi iniciar nao obtenho erros nas leituras...
    
    WiFi.begin("GIGA_ASSISTENCIA", "giga2020a");
    Serial.println("conectei");
    while (true) {
      delay(1);
    }
    //resto do codigo responsável por ler banco de dados
    delay(1);
  }
}
