
#define ThermistorPin 15
#define adcMax 4095.0
#define Vs 3.3

#define R1 10000.0   // voltage divider resistor value
#define Beta 3950.0 // Beta value
#define To 298.15   // Temperature in Kelvin for 25 degree Celsius
#define Ro 10000.0 // Resistance of Thermistor at 25 degree Celsius


void setup() {
  Serial.begin(115200);
  
}

void loop() {
  double Vout, Rt = 0;
  double T, Tc, Tf = 0;

  double adc = 0;
  for(int i =0; i<5; i++){
    adc += analogRead(ThermistorPin);
    delay(10);
  }

    adc = adc/5;

  Vout = adc * Vs/adcMax;
  Rt = R1 * Vout / (Vs - Vout);
  

  T = 1/(1/To + log(Rt/Ro)/Beta);    // Temperature in Kelvin
  Tc = T - 273.15;   
   
  Serial.println(adc);
  Serial.println(Vout);
  Serial.println(Rt);// Celsius
  if (Tc > 0) Serial.println(Tc);

  delay(2000);
}
