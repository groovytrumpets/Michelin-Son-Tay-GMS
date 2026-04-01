package com.g42.platform.gms.hikvision.mock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mock Hikvision server for development/testing when real device is not available
 */
@Configuration
@ConditionalOnProperty(name = "hikvision.mock.enabled", havingValue = "true")
public class HikvisionMockServer {

    @RestController
    @RequestMapping("/ISAPI/AccessControl/AcsEvent")
    public static class MockHikvisionController {

        @GetMapping
        public String getEvents() {
            // Mock response similar to real Hikvision device
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            
            return """
                <?xml version="1.0" encoding="UTF-8"?>
                <EventNotificationAlert>
                    <ipAddress>127.0.0.1</ipAddress>
                    <portNo>80</portNo>
                    <protocol>HTTP</protocol>
                    <macAddress>00:12:34:56:78:90</macAddress>
                    <channelID>1</channelID>
                    <dateTime>%s</dateTime>
                    <activePostCount>1</activePostCount>
                    <eventType>AccessControllerEvent</eventType>
                    <eventState>active</eventState>
                    <eventDescription>Card Reader Event</eventDescription>
                    <AccessControllerEvent>
                        <employeeNoString>12345</employeeNoString>
                        <cardNo>1234567890</cardNo>
                        <name>Test Employee</name>
                        <userType>normal</userType>
                        <currentVerifyMode>card</currentVerifyMode>
                        <attendanceStatus>checkIn</attendanceStatus>
                        <doorName>Main Entrance</doorName>
                        <doorNo>1</doorNo>
                        <verifyNo>1</verifyNo>
                        <alarmInfoPlate>
                            <result>processed</result>
                            <time>%s</time>
                        </alarmInfoPlate>
                    </AccessControllerEvent>
                </EventNotificationAlert>
                """.formatted(currentTime, currentTime);
        }
    }
}