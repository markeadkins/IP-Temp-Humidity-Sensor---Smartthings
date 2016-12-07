#include <DHT.h>
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>

#define DHTPIN 2
#define DHTTYPE DHT22

const char* ssid = "";
const char* password = "";

DHT dht(DHTPIN, DHTTYPE);
ESP8266WebServer server(80);

float humidity, temp_f;
String webString = "";
unsigned long previousMillis = 0;
const long interval = 2000;

void handleRoot()
{
  gettemperature();
  
  String displayMessage = "Temperature: ";
         displayMessage += String((int)temp_f);
         displayMessage += " - Humidity: ";
         displayMessage += String((int)humidity);
  server.send(200, "text/plain", displayMessage);
}

void handleNotFound(){
  String message = "File Not Found\n\n";
  message += "URI: ";
  message += server.uri();
  message += "\nMethod: ";
  message += (server.method() == HTTP_GET)?"GET":"POST";
  message += "\nArguments: ";
  message += server.args();
  message += "\n";
  for (uint8_t i=0; i<server.args(); i++){
    message += " " + server.argName(i) + ": " + server.arg(i) + "\n";
  }
  server.send(404, "text/plain", message);
}

void setup()
{
  Serial.begin(115200);

  // We start by connecting to a WiFi network
  Serial.println("Connecting to "+String(ssid)); 
  WiFi.begin(ssid, password); 
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print("+");
  }
  Serial.println("");
  Serial.println("WiFi connected. IP address: ");
  Serial.println(WiFi.localIP());  

  server.on("/", handleRoot);
  
  server.on("/temp", []()
  {
    gettemperature();
    webString="Temperature: "+String((int)temp_f)+" F";
    server.send(200, "text/plain", webString);
  });

  server.on("/humidity", []()
  {
    gettemperature();
    webString="Humidity: "+String((int)humidity)+"%";
    server.send(200, "text/plain", webString);
  });

  server.on("/json", []()
  {
    gettemperature();
    
    webString = "{\"object\":{\"temp\":\"";
    webString += String((int)temp_f);
    webString += "\",\"humidity\":\"";
    webString += String((int)humidity);
    webString += "\"}}\"";
    
    server.send(200, "application/json", webString);
  });
  
  server.onNotFound(handleNotFound);
  
  server.begin();
  
  Serial.println("HTTP server started");
  Serial.flush();
}

void loop()
{
  server.handleClient();
}

void gettemperature()
{
  unsigned long currentMillis = millis();

  if (currentMillis - previousMillis >= interval)
  {
    previousMillis = currentMillis;
    humidity = dht.readHumidity();
    temp_f = dht.readTemperature(true);

    if (isnan(humidity) || isnan(temp_f))
    {
      Serial.println("Failed to read from DHT Sensor!");
      return;
    }
  }
}

