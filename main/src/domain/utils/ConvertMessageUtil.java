package domain.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.utils.messages.*;

import java.util.HashMap;
import java.util.Map;

public class ConvertMessageUtil {

    private final static ObjectMapper mapper = new ObjectMapper();
    private final static Map<Class<?>, String> objToNameMapping = new HashMap<>();
    static {
        objToNameMapping.put(Logon.class, "LOGON");
        objToNameMapping.put(LogonResp.class, "LOGON_RESP");
        objToNameMapping.put(BroadcastReq.class, "BROADCAST_REQ");
        objToNameMapping.put(BroadcastResp.class, "BROADCAST_RESP");
        objToNameMapping.put(Broadcast.class, "BROADCAST");
        objToNameMapping.put(Joined.class, "JOINED");
        objToNameMapping.put(Left.class, "LEFT");
        objToNameMapping.put(ParseError.class, "PARSE_ERROR");
        objToNameMapping.put(Pong.class, "PONG");
        objToNameMapping.put(PongError.class, "PONG_ERROR");
        objToNameMapping.put(Hi.class, "HI");
        objToNameMapping.put(Ping.class, "PING");
        objToNameMapping.put(Hangup.class, "HANGUP");
        objToNameMapping.put(ByeResp.class, "BYE_RESP");
        objToNameMapping.put(Bye.class, "BYE");
        objToNameMapping.put(ListReq.class, "LIST");
        objToNameMapping.put(ListResp.class, "LIST_RESP");
        objToNameMapping.put(PrivateReq.class, "PRIVATE_REQ");
        objToNameMapping.put(PrivateResp.class, "PRIVATE_RESP");
        objToNameMapping.put(Private.class, "PRIVATE");
        objToNameMapping.put(CoinTossReq.class, "COIN_TOSS_REQ");
        objToNameMapping.put(CoinTossResp.class, "COIN_TOSS_RESP");
        objToNameMapping.put(CoinTossStart.class, "COIN_TOSS_START");
        objToNameMapping.put(CoinTossChoice.class, "COIN_TOSS_CHOICE");
        objToNameMapping.put(CoinTossLose.class, "COIN_TOSS_LOSE");
        objToNameMapping.put(CoinTossWin.class, "COIN_TOSS_WIN");
        objToNameMapping.put(CoinTossResult.class, "COIN_TOSS_RESULT");
        objToNameMapping.put(Heads.class, "HEADS");
        objToNameMapping.put(Tails.class, "TAILS");
        objToNameMapping.put(TransferReq.class, "TRANSFER_REQ");
        objToNameMapping.put(TransferResp.class, "TRANSFER_RESP");
        objToNameMapping.put(TransferAccept.class, "TRANSFER_ACCEPT");
        objToNameMapping.put(TransferReject.class, "TRANSFER_REJECT");
        objToNameMapping.put(TransferFile.class, "TRANSFER_FILE");
        objToNameMapping.put(TransferDone.class, "TRANSFER_DONE");
        objToNameMapping.put(DownloadAsk.class, "DOWNLOAD_ASK");
        objToNameMapping.put(DownloadAccept.class, "DOWNLOAD_ACCEPT");
        objToNameMapping.put(DownloadReject.class, "DOWNLOAD_REJECT");
        objToNameMapping.put(DownloadAcceptResp.class, "DOWNLOAD_ACCEPT_RESP");
        objToNameMapping.put(DownloadRejectResp.class, "DOWNLOAD_REJECT_RESP");
        objToNameMapping.put(DownloadDone.class, "DOWNLOAD_DONE");
        objToNameMapping.put(UnknownCommand.class, "UNKNOWN_COMMAND");
    }

    public static String objectToMessage(Message message) throws JsonProcessingException {
        Class<?> clazz = message.getClass();
        String header = objToNameMapping.get(clazz);
        if (header == null) {
            throw new RuntimeException("Cannot convert this class to a message");
        }
        String body = mapper.writeValueAsString(message);
        return header + " " + body;
    }

    public static <T> T messageToObject(String message) throws JsonProcessingException {
        String[] parts = message.split(" ", 2);
        if (parts.length > 2 || parts.length == 0) {
            throw new RuntimeException("Invalid message");
        }
        String header = parts[0];
        String body = "{}";
        if (parts.length == 2) {
            body = parts[1];
        }
        Class<?> clazz = getClass(header);
        Message obj = (Message) mapper.readValue(body, clazz);
        return (T) clazz.cast(obj);
    }

    private static Class<?> getClass(String header) {
        return objToNameMapping.entrySet().stream()
                .filter(e -> e.getValue().equals(header))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find class belonging to header " + header));
    }
}
