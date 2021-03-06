package com.larvalabs.redditchat;

import com.larvalabs.redditchat.dataobj.JsonChatRoom;
import com.larvalabs.redditchat.dataobj.JsonUser;
import com.larvalabs.redditchat.realtime.ChatRoomStream;
import models.ChatRoom;
import models.ChatUser;
import models.ChatUserRoomJoin;
import models.Message;
import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.mvc.Http;

import java.util.List;

/**
 * Created by matt on 1/18/16.
 */
public class ChatCommands {

    public static final String COMMAND_CHAR = "/";

    public enum CommandType {
        KICK("kick", true, false, true),
        BAN("ban", true, false, true),
        UNBAN("unban", true, false, false),
        SITEBAN("siteban", false, true, true);

        String commandString;
        boolean modOnly;
        boolean adminOnly;
        boolean closeClientSocket;

        CommandType(String commandString, boolean modOnly, boolean adminOnly, boolean closeClientSocket) {
            this.commandString = commandString;
            this.modOnly = modOnly;
            this.adminOnly = adminOnly;
            this.closeClientSocket = closeClientSocket;
        }

        public String getCommandString() {
            return commandString;
        }

        public boolean canExecute(ChatUser user, ChatRoom room) {
            if (adminOnly) {
                return user.isAdmin();
            } else if (modOnly) {
                return room.isModerator(user);
            }
            return true;
        }

        public static CommandType getType(String message) {
            for (CommandType commandType : values()) {
                if (message.toLowerCase().startsWith(COMMAND_CHAR + commandType.getCommandString())) {
                    return commandType;
                }
            }
            return null;
        }

        public boolean shouldCloseClientSocket() {
            return closeClientSocket;
        }
    }

    public static class Command {
        public CommandType type;
        public String username;

        public Command(CommandType type, String username) {
            this.type = type;
            this.username = username;
        }
    }

    public static boolean isCommand(String message) {
        if (!StringUtils.isEmpty(message)) {
            if (message.trim().startsWith(COMMAND_CHAR)) {
                return true;
            }
        }
        return false;
    }

    private static String getUsername(String message) {
        if (!StringUtils.isEmpty(message)) {
            String[] split = message.split(" ");
            if (split.length > 1) {
                String username = split[1];
                username = username.replace("@", "");
                return username;
            }
        }
        return null;
    }

    public static Command getCommand(String message) {
        if (!StringUtils.isEmpty(message)) {
            message = message.trim();
            if (message.startsWith(COMMAND_CHAR)) {
                CommandType type = CommandType.getType(message);
                if (type != null) {
                    String username = getUsername(message);
                    if (username != null) {
                        return new Command(type, username);
                    }
                }
            }
        }
        return null;
    }

    public static void execCommand(ChatUser executingUser, ChatRoom room, String message,
                                   ChatRoomStream stream, Http.Outbound socket, ChatUser systemUser) throws NotEnoughPermissionsException, CommandNotRecognizedException {
        JsonChatRoom jsonChatRoom = JsonChatRoom.from(room);
        JsonUser systemJsonUser = JsonUser.fromUser(systemUser, true);
        if (!isCommand(message)) {
            Logger.debug("Error processing message, notifying user.");
            socket.send(new ChatRoomStream.ServerMessage(jsonChatRoom, executingUser.getUsername(), systemJsonUser, "Error processing command.").toJson());
            return;
        }

        Command command = getCommand(message);
        if (command == null) {
            throw new CommandNotRecognizedException();
        }

        // Reload objects since they might be detached if coming from websocket
        executingUser = ChatUser.findByUsername(executingUser.username);
        room = ChatRoom.findByName(room.name);

        // Check has permission
        if (!command.type.canExecute(executingUser, room)) {
            Logger.info(executingUser.username + " can't execute " + command.type + " in room " + room.name);
            throw new NotEnoughPermissionsException();
        }

        if (command.type == CommandType.BAN || command.type == CommandType.UNBAN) {
            ChatUser user = ChatUser.findByUsername(command.username);
            if (user != null) {
                // Need to load new copies of the objects to be able to save via JPA
                ChatUser commandTargetUser = ChatUser.findByUsername(command.username);
                if (command.type == CommandType.BAN) {
                    room.getBannedUsers().add(commandTargetUser);

                    List<Message> messagesByUser = room.getMessagesByUser(commandTargetUser, 100);
                    if (messagesByUser != null) {
                        Logger.info("Marking " + messagesByUser.size() + " messages as deleted for banned user " + commandTargetUser.getUsername());
                        for (Message messageToDel : messagesByUser) {
                            messageToDel.setDeleted(true);
                            messageToDel.save();
                        }
                    }
                } else {
                    room.getBannedUsers().remove(commandTargetUser);
                }
                room.save();

                ChatUserRoomJoin join = ChatUserRoomJoin.findByUserAndRoom(user, room);
                if (join != null) {
                    join.delete();
                }

                if (command.type == CommandType.BAN) {
                    socket.send(new ChatRoomStream.ServerMessage(jsonChatRoom, executingUser.getUsername(), systemJsonUser, "User " + command.username + " has been banned.").toJson());
                } else {
                    socket.send(new ChatRoomStream.ServerMessage(jsonChatRoom, executingUser.getUsername(), systemJsonUser, "User " + command.username + " is now unbanned.").toJson());
                }
                stream.publishEvent(new ChatRoomStream.ServerCommand(jsonChatRoom, command));
            } else {
                socket.send(new ChatRoomStream.ServerMessage(jsonChatRoom, executingUser.getUsername(), systemJsonUser, "User " + command.username + " was not found.").toJson());
            }
        } else if (command.type == CommandType.SITEBAN) {
            ChatUser user = ChatUser.findByUsername(command.username);
            if (user != null) {
                user.setShadowBan(true);
                user.save();
                user.deleteAllMessages();
                socket.send(new ChatRoomStream.ServerMessage(jsonChatRoom, executingUser.getUsername(), systemJsonUser, "User " + command.username + " has been banned.").toJson());
                stream.publishEvent(new ChatRoomStream.ServerCommand(jsonChatRoom, command));
            } else {
                socket.send(new ChatRoomStream.ServerMessage(jsonChatRoom, executingUser.getUsername(), systemJsonUser, "User " + command.username + " was not found.").toJson());
            }
        } else if (command.type == CommandType.KICK) {
            socket.send(new ChatRoomStream.ServerMessage(jsonChatRoom, executingUser.getUsername(), systemJsonUser, "User " + command.username + " kicked.").toJson());
            stream.publishEvent(new ChatRoomStream.ServerCommand(jsonChatRoom, command));
        }
    }

    public static class CommandNotRecognizedException extends Exception {
    }

    public static class NotEnoughPermissionsException extends Exception {
    }
}
