package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import domain.utils.ConvertMessageUtil;
import domain.utils.messages.*;

import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@DisplayName("Single-user")
class SingleUser {

    private final static Properties PROPS = new Properties();
    private static int pingTimeMs;
    private static String host;
    private static int port;
    private static int pingTimeMsDeltaAllowed;

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = SingleUser.class.getResourceAsStream("testconfig.properties");
        PROPS.load(in);
        host = PROPS.getProperty("host");
        port = Integer.parseInt(PROPS.getProperty("port"));
        pingTimeMs = Integer.parseInt(PROPS.getProperty("ping_time_ms"));
        pingTimeMsDeltaAllowed = Integer.parseInt(PROPS.getProperty("ping_time_ms_delta_allowed"));
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        s = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new PrintWriter(s.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        s.close();
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class A_login_is_ok {

        @Test
        @Tag("RQ-U100")
        void when_the_username_has_three_characters() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            out.println(ConvertMessageUtil.objectToMessage(new Logon("mym")));
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            LogonResp logonResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals("OK", logonResp.status());
        }

        @Test
        @Tag("RQ-U100")
        void when_the_username_has_14_characters() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            out.println(ConvertMessageUtil.objectToMessage(new Logon("abcdefghijklmn")));
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            LogonResp logonResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals("OK", logonResp.status());
        }

        @Test
        @Tag("RQ-U100")
        void when_the_username_contains_an_underscore() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            out.println(ConvertMessageUtil.objectToMessage(new Logon("test_")));
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            LogonResp logonResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals("OK", logonResp.status());
        }
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class A_login_causes_an_error {
        @Test
        @Tag("RQ-S203")
        void when_the_username_has_two_characters() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            out.println(ConvertMessageUtil.objectToMessage(new Logon("my")));
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            LogonResp logonResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals(new LogonResp("ERROR", 5001), logonResp, "Too short username accepted: " + serverResponse);
        }

        @Test
        @Tag("RQ-S203")
        void when_the_username_has_15_characters() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            out.println(ConvertMessageUtil.objectToMessage(new Logon("abcdefghijklmop")));
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            LogonResp logonResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals(new LogonResp("ERROR", 5001), logonResp, "Too long username accepted: " + serverResponse);
        }

        @ParameterizedTest
        @Tag("RQ-S203")
        @ValueSource(strings = {"&", "*", "$", "%", "@"})
        void when_the_username_contains_an_invalid_character(String character) throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            out.println(ConvertMessageUtil.objectToMessage(new Logon("test" + character)));
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            LogonResp logonResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals(new LogonResp("ERROR", 5001), logonResp, "Wrong character accepted");
        }
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class A_login_and_broadcast_succeeds {

        /*
         * Send the message using multiple ethernet packages
         */
        @Test
        @Tag("RQ-S205")
        void when_multiple_output_flushes_are_used_when_sending_messages() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            out.print("LOGON {\"username\":\"muser123");
            out.flush();
            out.print("yname\"}\r\nBROAD");
            out.flush();
            out.print("CAST_REQ {\"message\":\"a\"}\r\n");
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            LogonResp logonResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals("OK", logonResp.status());
            serverResponse = receiveLineWithTimeout(in, "BROADCAST_RESP expected");
            BroadcastResp broadcastResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals("OK", broadcastResp.status());
        }

        @Test
        @Tag("RQ-S206")
        void when_windows_line_endings_are_used() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            String message = ConvertMessageUtil.objectToMessage(new Logon("myname")) + "\r\n" +
                    ConvertMessageUtil.objectToMessage(new BroadcastReq("a")) + "\r\n";
            out.print(message);
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            LogonResp logonResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals("OK", logonResp.status());

            serverResponse = receiveLineWithTimeout(in, "BROADCAST_RESP expected");
            BroadcastResp broadcastResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals("OK", broadcastResp.status());
        }

        @Test
        @Tag("RQ-S206")
        void when_linux_line_endings_are_used() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            String message = ConvertMessageUtil.objectToMessage(new Logon("myname")) + "\n" +
                    ConvertMessageUtil.objectToMessage(new BroadcastReq("a")) + "\n";
            out.print(message);
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            LogonResp logonResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals("OK", logonResp.status());

            serverResponse = receiveLineWithTimeout(in, "BROADCAST_RESP expected");
            BroadcastResp broadcastResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals("OK", broadcastResp.status());
        }
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class Server_initial_message_is_received {
        @Test
        @Tag("RQ-S207")
        void when_an_initial_connection_to_the_server_is_made() throws JsonProcessingException {
            String firstLine = receiveLineWithTimeout(in, "Hi expected");
            Hi hi = ConvertMessageUtil.messageToObject(firstLine);
            assertEquals(new Hi("1.7.0"), hi);
        }
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class Server_parse_error_message_is_received {

        @Test
        @Tag("RQ-S208")
        void when_invalid_json_is_received() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            out.println("LOGON {\"}");
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "PARSE_ERROR expected");
            ParseError parseError = ConvertMessageUtil.messageToObject(serverResponse);
            assertNotNull(parseError);
        }

        @Test
        @Tag("RQ-U100")
        void when_an_login_with_empty_json_message_is_received() throws JsonProcessingException {
            // doesnt it make more sense to return parse error instead of logon resp?
            receiveLineWithTimeout(in, "Initial message expected");
            out.println("LOGON ");
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "PARSE_ERROR expected");
            ParseError parseError = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals(new ParseError(), parseError);
        }
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class Server_login_error_response_is_received {
        @Test
        @Tag("RQ-U104")
        void when_the_user_logins_twice() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            out.println(ConvertMessageUtil.objectToMessage(new Logon("first")));
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            LogonResp logonResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals("OK", logonResp.status());

            out.println(ConvertMessageUtil.objectToMessage(new Logon("second")));
            out.flush();
            serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            logonResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals(new LogonResp("ERROR", 5002), logonResp);
        }
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class Pong_error_is_returned {
        @Test
        @Tag("RQ-S102")
        void when_a_pong_without_a_ping_request_is_received() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            out.println(ConvertMessageUtil.objectToMessage(new Pong()));
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            PongError pongError = ConvertMessageUtil.messageToObject(serverResponse);
            assertEquals(new PongError(8000), pongError);
        }
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class Server_PING_is_received {

        @Test
        @Tag("RQ-S102")
        void on_the_expected_time(TestReporter testReporter) throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            out.println(ConvertMessageUtil.objectToMessage(new Logon("myname")));
            out.flush();
            receiveLineWithTimeout(in, "Initial message expected");

            //Make sure the test does not hang when no response is received by using assertTimeoutPreemptively
            assertTimeoutPreemptively(ofMillis(pingTimeMs + pingTimeMsDeltaAllowed), () -> {
                Instant start = Instant.now();
                String pingString = in.readLine();
                Instant finish = Instant.now();

                Ping ping = ConvertMessageUtil.messageToObject(pingString);
                assertNotNull(ping);

                // Also make sure the response is not received too early
                long timeElapsed = Duration.between(start, finish).toMillis();
                testReporter.publishEntry("timeElapsed", String.valueOf(timeElapsed));
                assertTrue(timeElapsed > pingTimeMs - pingTimeMsDeltaAllowed);
            });
        }
    }

    @Nested
    @IndicativeSentencesGeneration(separator = " -> ", generator = DisplayNameGenerator.ReplaceUnderscores.class)
    class Server_bye_succeeds {
        @Test
        @Tag("RQ-U102")
        void when_user_is_logged_in() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");
            out.println(ConvertMessageUtil.objectToMessage(new Logon("mym")));
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "LOGON_RESP expected");
            LogonResp logonResp = ConvertMessageUtil.messageToObject(serverResponse);
            System.out.println(logonResp.status());
            assumeTrue(logonResp.status().equals("OK"));

            out.println("BYE");
            out.flush();
            serverResponse = receiveLineWithTimeout(in, "BYE_RESP expected");
            ByeResp byeResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertTrue(byeResp.status().equals("OK"));
        }

        @Test
        @Tag("RQ-U102")
        void when_user_is_not_logged_in() throws JsonProcessingException {
            receiveLineWithTimeout(in, "Initial message expected");

            out.println("BYE");
            out.flush();
            String serverResponse = receiveLineWithTimeout(in, "BYE_RESP expected");
            ByeResp byeResp = ConvertMessageUtil.messageToObject(serverResponse);
            assertTrue(byeResp.status().equals("OK"));
        }
    }


    private String receiveLineWithTimeout(BufferedReader reader, String message) {
        return assertTimeoutPreemptively(ofMillis(pingTimeMsDeltaAllowed), reader::readLine, message);
    }

}