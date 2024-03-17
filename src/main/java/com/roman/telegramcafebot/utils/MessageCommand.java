package com.roman.telegramcafebot.utils;

public enum MessageCommand {
    COMMAND_START("/start"),
    COMMAND_ADMIN ("/админ"),
    COMMAND_NEW_ITEM ("/добавить"),
    COMMAND_PASSWORD ("/пароль"),
    COMMAND_ADMIN_ON ("/включить"),
    COMMAND_ADMIN_OFF ("/выключить"),
    COMMAND_DELETEME("/удалить"),
    COMMAND_NEWSECTION("/новыйраздел");

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
