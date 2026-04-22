package domain.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import domain.utils.messages.Message;

import java.io.PrintWriter;

import static domain.utils.ConvertMessageUtil.objectToMessage;

public class MessageSender {
    protected PrintWriter printWriter;

    public MessageSender(PrintWriter printWriter) {
        this.printWriter = printWriter;
    }

    public void sendMessage(Message message) {
//        System.out.printf("Sending %s%n", message);
        try {
            printWriter.println(objectToMessage(message));
            printWriter.flush();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
