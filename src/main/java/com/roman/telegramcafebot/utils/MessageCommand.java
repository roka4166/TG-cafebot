package com.roman.telegramcafebot.utils;

public enum MessageCommand {
    COMMAND_START("/start"),
    COMMAND_ADMIN ("/admin"),
    COMMAND_NEW_ITEM ("/newitem"),
    COMMAND_PASSWORD ("/password"),
    COMMAND_ADMIN_ON ("/adminon"),
    COMMAND_ADMIN_OFF ("/adminoff"),
    COMMAND_DELETEME("/deleteme");

    private final String messageCommand;

    MessageCommand(String command) {
        this.messageCommand = command;
    }

    public String getMessageCommand() {
        return messageCommand;
    }

    public static MessageCommand fromMessageText(String commandText) {

        for (MessageCommand command : MessageCommand.values()) {
            if (commandText.startsWith(command.messageCommand) &&
                    (commandText.length() == command.messageCommand.length() || commandText.charAt(command.messageCommand.length()) == ' ')) {
                return command;
            }
        }
        return null;
    }
}
