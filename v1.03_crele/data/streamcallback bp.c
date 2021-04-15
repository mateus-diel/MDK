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