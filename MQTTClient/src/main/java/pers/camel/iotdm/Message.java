package pers.camel.iotdm;

import lombok.Data;

@Data
public class Message {
    String userID;
    String deviceID;
    String info;
    Long value;
    Boolean alert;
    Double lng;
    Double lat;
    Long time;
}
