package com.roman.telegramcafebot.utils;

public enum MessageCommand {
    COMMAND_START("/start"),
    COMMAND_SHOWALLSTOPPED ("/showallstopped"),
    COMMAND_ADMIN ("/admin"),
    COMMAND_NEW_ITEM ("/newitem"),
    COMMAND_DELETE_ITEM ("/deleteitem"),
    COMMAND_PASSWORD ("/password"),
    COMMAND_ADMIN_ON ("/adminon"),
    COMMAND_ADMIN_OFF ("/adminoff");

    private final String messageCommand;

    MessageCommand(String command) {
        this.messageCommand = command;
    }

    public String getMessageCommand() {
        return messageCommand;
    }

    public static MessageCommand fromMessageText(String commandText) {
        String[] parts = commandText.split("\\s+", 2);

        for (MessageCommand command : MessageCommand.values()) {
            if (commandText.startsWith(command.messageCommand)) {
                return command;
            }
        }
        return null;
    }
}
