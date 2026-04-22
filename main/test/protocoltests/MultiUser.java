package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import domain.utils.constants.Codes;
import domain.utils.constants.StatusCodes;
import org.junit.jupiter.api.*;

import domain.utils.ConvertMessageUtil;
import domain.utils.messages.*;

import java.io.*;
import java.net.Socket;
import java.util.*;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Multi-user")
class MultiUser {

    private final static Properties PROPS = new Properties();

    private Socket socketUser1, socketUser2;
    private BufferedReader inUser1, inUser2;
    private PrintWriter outUser1, outUser2;
    private static String host;
    private static int port;
    private static int pingTimeMsDeltaAllowed;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = MultiUser.class.getResourceAsStream("testconfig.properties");
        PROPS.load(in);
        host = PROPS.getProperty("host");
        port = Integer.parseInt(PROPS.getProperty("port"));
        pingTimeMsDeltaAllowed = Integer.parseInt(PROPS.getProperty("ping_time_ms_delta_allowed"));
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        socketUser1 = new Socket(host, port);
        inUser1 = new BufferedReader(new InputStreamReader(socketUser1.getInputStream()));
        outUser1 = new PrintWriter(socketUser1.getOutputStream(), true);

        socketUser2 = new Socket(host, port);
        inUser2 = new BufferedReader(new InputStreamReader(socketUser2.getInputStream()));
        outUser2 = new PrintWriter(socketUser2.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        socketUser1.close();
        socketUser2.close();
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class A_JOINED_message_is_received_by_other_clients {

        @Test
        @Tag("RQ-U212")
        void when_a_user_connects() throws JsonProcessingException {
            receiveLineWithTimeout(inUser1, "Initial message expected");
            receiveLineWithTimeout(inUser2, "Initial message expected");

            // Connect user1
            outUser1.println(ConvertMessageUtil.objectToMessage(new Logon("user1")));
            outUser1.flush();
            receiveLineWithTimeout(inUser1, "OK expected");

            // Connect user2
            outUser2.println(ConvertMessageUtil.objectToMessage(new Logon("user2")));
            outUser2.flush();
            receiveLineWithTimeout(inUser2, "OK expected");

            //JOINED is received by user1 when user2 connects
            /* This test is expected to fail with the given NodeJS domain.server because the JOINED is not implemented.
             * Make sure the test works when implementing your own domain.server in Java
             */
            String res = receiveLineWithTimeout(inUser1, "JOINED expected");
            Joined joined = ConvertMessageUtil.messageToObject(res);

            assertEquals(new Joined("user2"), joined);
        }
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class A_BROADCAST_is_received_by_all_other_users {

        @Test
        @Tag("RQ-U101")
        void when_a_user_sends_one() throws JsonProcessingException {
            receiveLineWithTimeout(inUser1, "Initial message expected");
            receiveLineWithTimeout(inUser2, "Initial message expected");

            // Connect user1
            outUser1.println(ConvertMessageUtil.objectToMessage(new Logon("user1")));
            outUser1.flush();
            receiveLineWithTimeout(inUser1, "OK expected");

            // Connect user2
            outUser2.println(ConvertMessageUtil.objectToMessage(new Logon("user2")));
            outUser2.flush();
            receiveLineWithTimeout(inUser2, "OK expected");
            /* This test is expected to fail with the given NodeJS domain.server because the JOINED is not implemented.
             * Make sure the test works when implementing your own domain.server in Java
             */
            receiveLineWithTimeout(inUser1, "JOINED expected");

            //send BROADCAST from user 1
            outUser1.println(ConvertMessageUtil.objectToMessage(new BroadcastReq("messagefromuser1")));

            outUser1.flush();
            String fromUser1 = receiveLineWithTimeout(inUser1, "BROADCAST_RESP expected");
            BroadcastResp broadcastResp1 = ConvertMessageUtil.messageToObject(fromUser1);

            assertEquals("OK", broadcastResp1.status());

            String fromUser2 = receiveLineWithTimeout(inUser2, "BROADCAST expected");
            Broadcast broadcast2 = ConvertMessageUtil.messageToObject(fromUser2);

            assertEquals(new Broadcast("user1", "messagefromuser1"), broadcast2);

            //send BROADCAST from user 2
            outUser2.println(ConvertMessageUtil.objectToMessage(new BroadcastReq("messagefromuser2")));
            outUser2.flush();
            fromUser2 = receiveLineWithTimeout(inUser2, "BROADCAST_RESP expected");
            BroadcastResp broadcastResp2 = ConvertMessageUtil.messageToObject(fromUser2);
            assertEquals("OK", broadcastResp2.status());

            fromUser1 = receiveLineWithTimeout(inUser1, "BROADCAST expected");
            Broadcast broadcast1 = ConvertMessageUtil.messageToObject(fromUser1);

            assertEquals(new Broadcast("user2", "messagefromuser2"), broadcast1);
        }

    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class An_ERROR_message_is_received {

        @Test
        @Tag("RQ-U100")
        void when_trying_to_login_with_an_already_logged_in_username() throws JsonProcessingException {
            receiveLineWithTimeout(inUser1, "Initial message expected");
            receiveLineWithTimeout(inUser2, "Initial message expected");

            // Connect user 1
            outUser1.println(ConvertMessageUtil.objectToMessage(new Logon("user1")));
            outUser1.flush();
            System.out.println(receiveLineWithTimeout(inUser1, "OK expected"));

            // Connect using same username
            outUser2.println(ConvertMessageUtil.objectToMessage(new Logon("user1")));
            outUser2.flush();
            String resUser2 = receiveLineWithTimeout(inUser2, "LOGON_RESP expected");
            System.out.println(resUser2);
            LogonResp logonResp = ConvertMessageUtil.messageToObject(resUser2);
            assertEquals(new LogonResp("ERROR", 5000), logonResp);
        }

        @Test
        @Tag("RQ-U201")
        void when_a_logged_in_user_requests_a_private_message_to_a_non_existent_username() throws JsonProcessingException {
            receiveLineWithTimeout(inUser1, "Initial message expected");
            receiveLineWithTimeout(inUser2, "Initial message expected");

            String username1 = "user1";
            String username2 = "user2";
            String message = "test message";

            // Connect user 1
            outUser1.println(ConvertMessageUtil.objectToMessage(new Logon(username1)));
            outUser1.flush();
            System.out.println(receiveLineWithTimeout(inUser1, "OK expected"));

            // Connect user 2
            outUser2.println(ConvertMessageUtil.objectToMessage(new Logon(username2)));
            outUser2.flush();
            System.out.println(receiveLineWithTimeout(inUser2, "OK expected"));

            outUser2.println(ConvertMessageUtil.objectToMessage(new PrivateReq(message, username1 + "abc")));
            outUser2.flush();
            String resUser2 = receiveLineWithTimeout(inUser2, "ERROR expected");
            System.out.println(resUser2);

            PrivateResp privateResp = ConvertMessageUtil.messageToObject(resUser2);

            assertEquals(StatusCodes.ERROR, privateResp.status());
            assertEquals(Codes.NON_EXISTENT_USERNAME_PROVIDED, privateResp.code());
        }

        @Test
        @Tag("RQ-U201")
        void when_a_not_logged_logged_in_user_requests_a_private_message() throws IOException {
            receiveLineWithTimeout(inUser1, "Initial message expected");
            receiveLineWithTimeout(inUser2, "Initial message expected");

            String username1 = "user1";
            String username2 = "user2";
            String message = "test message";

            // Connect user 1
            outUser1.println(ConvertMessageUtil.objectToMessage(new Logon(username1)));
            outUser1.flush();
            System.out.println(receiveLineWithTimeout(inUser1, "OK expected"));

            outUser2.println(ConvertMessageUtil.objectToMessage(new PrivateReq(message, username1)));
            outUser2.flush();
            String resUser2 = receiveLineWithTimeout(inUser2, "ERROR expected");
            System.out.println(resUser2);

            PrivateResp privateResp = ConvertMessageUtil.messageToObject(resUser2);

            assertEquals(StatusCodes.ERROR, privateResp.status());
            assertEquals(Codes.NOT_LOGGED_IN, privateResp.code());

            assertFalse(inUser1.ready(), "Nothing expected");
        }
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class A_LIST_of_users_is_received {

        @Test
        @Tag("RQ-U200")
        void when_a_logged_in_user_requests_one() throws JsonProcessingException {
            receiveLineWithTimeout(inUser1, "Initial message expected");
            receiveLineWithTimeout(inUser2, "Initial message expected");

            String username1 = "user1";
            String username2 = "user2";

            // Connect user 1
            outUser1.println(ConvertMessageUtil.objectToMessage(new Logon(username1)));
            outUser1.flush();
            System.out.println(receiveLineWithTimeout(inUser1, "OK expected"));

            // Connect user 2
            outUser2.println(ConvertMessageUtil.objectToMessage(new Logon(username2)));
            outUser2.flush();
            System.out.println(receiveLineWithTimeout(inUser2, "OK expected"));

            outUser2.println(ConvertMessageUtil.objectToMessage(new ListReq()));
            outUser2.flush();
            String resUser2 = receiveLineWithTimeout(inUser2, "OK expected");
            System.out.println(resUser2);

            ListResp listResp = ConvertMessageUtil.messageToObject(resUser2);

            assertEquals(StatusCodes.OK, listResp.status());
            assertEquals(2, listResp.usernames().size());
            assertEquals(new ArrayList<>(List.of(username1, username2)), listResp.usernames());
        }
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class A_PRIVATE_message_is_received {

        @Test
        @Tag("RQ-U201")
        void when_a_logged_in_user_requests_one_to_be_sent() throws JsonProcessingException {
            receiveLineWithTimeout(inUser1, "Initial message expected");
            receiveLineWithTimeout(inUser2, "Initial message expected");

            String username1 = "user1";
            String username2 = "user2";
            String message = "test message";

            // Connect user 1
            outUser1.println(ConvertMessageUtil.objectToMessage(new Logon(username1)));
            outUser1.flush();
            System.out.println(receiveLineWithTimeout(inUser1, "OK expected"));

            // Connect user 2
            outUser2.println(ConvertMessageUtil.objectToMessage(new Logon(username2)));
            outUser2.flush();
            System.out.println(receiveLineWithTimeout(inUser2, "OK expected"));

            outUser2.println(ConvertMessageUtil.objectToMessage(new PrivateReq(message, username1)));
            outUser2.flush();
            String resUser2 = receiveLineWithTimeout(inUser2, "OK expected");
            System.out.println(resUser2);

            PrivateResp privateResp = ConvertMessageUtil.messageToObject(resUser2);

            assertEquals(StatusCodes.OK, privateResp.status());

            String resUser1Joined = receiveLineWithTimeout(inUser1, "JOINED expected");
            System.out.println(resUser1Joined);

            String resUser1Private = receiveLineWithTimeout(inUser1, "PRIVATE expected");
            System.out.println(resUser1Private);

            Private privateMessage = ConvertMessageUtil.messageToObject(resUser1Private);

            assertEquals(username2, privateMessage.senderUsername());
            assertEquals(message, privateMessage.message());
        }
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class A_coin_toss_game_is_played_naturally_between_2_logged_in_users {

        @Test
        @Tag("RQ-U202")
        void when_a_logged_in_user_starts_a_game_with_another_logged_in_user() throws JsonProcessingException {
            startCoinGameTest("user1", "user2");
        }

        @Test
        @Tag("RQ-U203, RQ-U205")
        void when_game_started_and_both_users_choose_heads_or_tails_then_they_receive_round_result() throws JsonProcessingException {
            String username1 = "user1";
            String username2 = "user2";
            String gameId = startCoinGameTest(username1, username2);
            chooseHeadsOrTailsOnceTest(username1, username2, gameId);
        }

        @Test
        @Tag("RQ-U205, RQ-206")
        void when_game_started_both_users_chose_heads_or_tails_until_one_of_the_users_wins() throws JsonProcessingException {
            String username1 = "user1";
            String username2 = "user2";
            String gameId = startCoinGameTest(username1, username2);
            chooseHeadsOrTailsOnceTest(username1, username2, gameId);

            CoinTossResult coinTossResult;

            do {
                String coinTossChoiceString1 = receiveLineWithTimeout(inUser1, "COIN_TOSS_CHOICE expected");
                String coinTossChoiceString2 = receiveLineWithTimeout(inUser2, "COIN_TOSS_CHOICE expected");
                System.out.println(coinTossChoiceString1);

                assertEquals(new CoinTossChoice(), ConvertMessageUtil.messageToObject(coinTossChoiceString1));
                assertEquals(new CoinTossChoice(), ConvertMessageUtil.messageToObject(coinTossChoiceString2));

                outUser1.println(ConvertMessageUtil.objectToMessage(new Heads(gameId)));
                outUser1.flush();
                outUser2.println(ConvertMessageUtil.objectToMessage(new Tails(gameId)));
                outUser2.flush();

                String coinTossResultString1 = receiveLineWithTimeout(inUser1, "COIN_TOSS_RESULT expected");
                String coinTossResultString2 = receiveLineWithTimeout(inUser2, "COIN_TOSS_RESULT expected");
                System.out.println(coinTossResultString1);

                CoinTossResult coinTossResult1 = ConvertMessageUtil.messageToObject(coinTossResultString1);
                CoinTossResult coinTossResult2 = ConvertMessageUtil.messageToObject(coinTossResultString2);

                assertEquals(coinTossResult1, coinTossResult2);
                coinTossResult = coinTossResult1;
            } while (!coinTossResult.scores().containsValue(3));

            String coinTossWinString;
            String coinTossLoseString;

            if (coinTossResult.scores().get(username1) == 3) {
                coinTossWinString = receiveLineWithTimeout(inUser1, "COIN_TOSS_WIN expected");
                coinTossLoseString = receiveLineWithTimeout(inUser2, "COIN_TOSS_LOSE expected");

            } else {
                coinTossWinString = receiveLineWithTimeout(inUser2, "COIN_TOSS_WIN expected");
                coinTossLoseString = receiveLineWithTimeout(inUser1, "COIN_TOSS_LOSE expected");
            }


            CoinTossWin coinTossWin = ConvertMessageUtil.messageToObject(coinTossWinString);
            CoinTossLose coinTossLose = ConvertMessageUtil.messageToObject(coinTossLoseString);

            assertEquals(new CoinTossWin(), coinTossWin);
            assertEquals(new CoinTossLose(), coinTossLose);
        }

        private String startCoinGameTest(String username1, String username2) throws JsonProcessingException {
            receiveLineWithTimeout(inUser1, "Initial message expected");
            receiveLineWithTimeout(inUser2, "Initial message expected");

            // Connect user 1
            outUser1.println(ConvertMessageUtil.objectToMessage(new Logon(username1)));
            outUser1.flush();
            System.out.println(receiveLineWithTimeout(inUser1, "OK expected"));

            // Connect user 2
            outUser2.println(ConvertMessageUtil.objectToMessage(new Logon(username2)));
            outUser2.flush();
            System.out.println(receiveLineWithTimeout(inUser2, "OK expected"));

            outUser2.println(ConvertMessageUtil.objectToMessage(new CoinTossReq(username1)));
            outUser2.flush();

            String joinedString = receiveLineWithTimeout(inUser1, "COIN_TOSS_RESP expected");
            assertEquals(new Joined(username2), ConvertMessageUtil.messageToObject(joinedString));

            String coinTossRespString = receiveLineWithTimeout(inUser2, "COIN_TOSS_RESP expected");
            CoinTossResp coinTossResp = ConvertMessageUtil.messageToObject(coinTossRespString);

            assertEquals(StatusCodes.OK, coinTossResp.status());

            String resUser1 = receiveLineWithTimeout(inUser1, "COIN_TOSS_START expected");
            String resUser2 = receiveLineWithTimeout(inUser2, "COIN_TOSS_START expected");
            CoinTossStart coinTossStart1 = ConvertMessageUtil.messageToObject(resUser1);
            CoinTossStart coinTossStart2 = ConvertMessageUtil.messageToObject(resUser2);

            assertEquals(username2, coinTossStart1.username());
            assertEquals(username1, coinTossStart2.username());

            return coinTossStart1.gameId();
        }

        private void chooseHeadsOrTailsOnceTest(String username1, String username2, String gameId) throws JsonProcessingException {
            String coinTossChoiceString1 = receiveLineWithTimeout(inUser1, "COIN_TOSS_CHOICE expected");
            String coinTossChoiceString2 = receiveLineWithTimeout(inUser2, "COIN_TOSS_CHOICE expected");
            System.out.println(coinTossChoiceString1);

            assertEquals(new CoinTossChoice(), ConvertMessageUtil.messageToObject(coinTossChoiceString1));
            assertEquals(new CoinTossChoice(), ConvertMessageUtil.messageToObject(coinTossChoiceString2));

            outUser1.println(ConvertMessageUtil.objectToMessage(new Heads(gameId)));
            outUser1.flush();
            outUser2.println(ConvertMessageUtil.objectToMessage(new Tails(gameId)));
            outUser2.flush();

            String coinTossResultString1 = receiveLineWithTimeout(inUser1, "COIN_TOSS_RESULT expected");
            String coinTossResultString2 = receiveLineWithTimeout(inUser2, "COIN_TOSS_RESULT expected");
            System.out.println(coinTossResultString1);

            CoinTossResult coinTossResult1 = ConvertMessageUtil.messageToObject(coinTossResultString1);
            CoinTossResult coinTossResult2 = ConvertMessageUtil.messageToObject(coinTossResultString2);

            // One of these maps should equal
            Map<String, Integer> user1WinRound = new HashMap<>(Map.of(username2, 1, username1, 0));
            Map<String, Integer> user2WinRound = new HashMap<>(Map.of(username2, 0, username1, 1));

            assertEquals(coinTossResult1, coinTossResult2);
            assertTrue(coinTossResult1.scores().equals(user1WinRound) || coinTossResult1.scores().equals(user2WinRound));
        }
    }

    private String receiveLineWithTimeout(BufferedReader reader, String message) {
        return assertTimeoutPreemptively(ofMillis(pingTimeMsDeltaAllowed), reader::readLine, message);
    }

}