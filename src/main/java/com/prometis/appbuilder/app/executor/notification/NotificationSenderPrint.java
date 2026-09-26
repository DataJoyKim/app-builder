package com.prometis.appbuilder.app.executor.notification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotificationSenderPrint {
    public static void printMail(String sendId, String sender, String to, String subject, String content) {

        String print = System.lineSeparator() +
                "=============================================" + System.lineSeparator() +
                "[Notification] " + sendId + System.lineSeparator() +
                "---------------------------------------------" + System.lineSeparator() +
                "> Sender: " + sender + System.lineSeparator() +
                "> To: " + to + System.lineSeparator() +
                "> Subject: " + subject + System.lineSeparator() +
                "> Content: " + System.lineSeparator() + content + System.lineSeparator() +
                "=============================================";

        log.info(print);
    }
}
