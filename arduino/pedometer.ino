#include <Wire.h>
#include <SparkFun_MMA8452Q.h>

MMA8452Q accel;
const double Z_MAX = 1.4;
const double Z_MIN = 0.8;
const int WALK_TH = 500;

int walk_count = 0;
double z_now = 0;
bool is_up = false;
int walk_time_span = 0;


void setup() {
  Serial.begin(9600);
  Serial.println("MMA8452Q Basic Reading Code!");
  Wire.begin();
  if (accel.begin() == false) {
    Serial.println("Not Connected. Please check connections and read the hookup guide.");
    while (1);
  }
}

void loop() {
  // check if you walk or not
  if (accel.available()) {
    z_now = accel.getCalculatedZ(); 
    if (z_now >= Z_MAX) {
      is_up = true;
      walk_time_span = 0;
    }
    if (is_up && z_now < Z_MIN && walk_time_span < WALK_TH) {
      walk_count += 1;
      is_up = false;
      //Serial.print("walk_count: ");
      //Serial.println(walk_count);
    }
    if (walk_time_span >= WALK_TH) {
      is_up = false;
    }
    //Serial.print("z_now: ");
    //Serial.println(z_now);
  }
  delay(10);
  walk_time_span += 10;
}

