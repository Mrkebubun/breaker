package jobs;

import com.larvalabs.linkunfurl.LinkInfo;
import com.larvalabs.linkunfurl.LinkUnfurl;
import com.larvalabs.redditchat.util.Util;
import models.ChatRoom;
import models.ChatUser;
import models.Message;
import models.WebLink;
import play.Logger;
import play.jobs.Job;

import java.io.IOException;
import java.util.List;

/**
 * Created by matt on 12/23/15.
 */
public class SaveNewMessageJob extends Job<Message> {

    private String uuid;
    private String username;
    private String roomName;
    private String messageText;
    private Message savedMessage;

    public SaveNewMessageJob(String uuid, String username, String roomName, String messageText) {
        this.uuid = uuid;
        this.username = username;
        this.roomName = roomName;
        this.messageText = messageText;
    }

    @Override
    public Message doJobWithResult() throws Exception {
        Logger.info("Saving message to room " + roomName);
        ChatRoom chatRoom = ChatRoom.findByName(roomName);
        ChatUser chatUser = ChatUser.findByUsername(username);
        savedMessage = new Message(uuid, chatUser, chatRoom, messageText);
        savedMessage.unfurlLinks();
        savedMessage.save();

        return savedMessage;
    }

    @Override
    public void after() {
        super.after();

        if (messageText.contains("@")) {
            new NotifyMentionedUsersJob(savedMessage.getId()).now();
        }
    }
}
