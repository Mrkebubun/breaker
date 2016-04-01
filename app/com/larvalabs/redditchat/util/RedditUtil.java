package com.larvalabs.redditchat.util;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.managers.InboxManager;
import play.Logger;

/**
 * Created by matt on 1/12/16.
 */
public class RedditUtil {

    public static final String CLIENT_ID_BOT = "***REMOVED***";
    public static final String CLIENT_SECRET_BOT = "***REMOVED***";
    public static final String USERNAME_BOT = "breakerappbot";
    public static final String PASSWORD_BOT = "***REMOVED***";

    public static void sendPrivateMessageFromBot(String toUsername, String subject, String content) throws ApiException {
        RedditClient redditClient = getRedditClient();
//        LoggedInAccount me = redditClient.me();

        InboxManager ibm = new InboxManager(redditClient);
        ibm.compose(toUsername, subject, content);

        Logger.info("Private message sent from bot to " + toUsername);

//        System.out.println(me.toString());
    }

    private static RedditClient getRedditClient() throws OAuthException {
        UserAgent myUserAgent = UserAgent.of("web", "com.larvalabs.breaker.bot", "v0.1", "megamatt2000");

        RedditClient redditClient = new RedditClient(myUserAgent);
        Credentials credentials = Credentials.script(USERNAME_BOT, PASSWORD_BOT,
                CLIENT_ID_BOT, CLIENT_SECRET_BOT);
        OAuthData authData = redditClient.getOAuthHelper().easyAuth(credentials);
        redditClient.authenticate(authData);

        return redditClient;
    }

}