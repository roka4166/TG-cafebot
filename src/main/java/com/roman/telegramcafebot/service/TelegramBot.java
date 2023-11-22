package com.roman.telegramcafebot.service;

import com.roman.telegramcafebot.config.BotConfig;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig botConfig;

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
            }
        }
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "RESERVATION_BUTTON" -> {
                    String text = "Введите номер стола: ";
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText(text);

                    InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
                    List<InlineKeyboardButton> rowInLine = new ArrayList<>();

                    var tableButton_1 = new InlineKeyboardButton();
                    tableButton_1.setText("1");
                    tableButton_1.setCallbackData("TABLE_1");

                    var tableButton_2 = new InlineKeyboardButton();
                    tableButton_2.setText("2");
                    tableButton_2.setCallbackData("TABLE_2");

                    var tableButton_3 = new InlineKeyboardButton();
                    tableButton_3.setText("3");
                    tableButton_3.setCallbackData("TABLE_3");

                    rowInLine.add(tableButton_1);
                    rowInLine.add(tableButton_2);
                    rowInLine.add(tableButton_3);

                    rowsInLine.add(rowInLine);

                    keyboardMarkup.setKeyboard(rowsInLine);
                    message.setReplyMarkup(keyboardMarkup);

                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                }
                case "BUY_BUTTON" -> {
                    String text = "buy";
                    EditMessageText message = new EditMessageText();
                    message.setChatId(chatId);
                    message.setText(text);
                    message.setMessageId((int) messageId);

                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                }
                case "DELIVERY_BUTTON" -> {
                    String text = "delivery";
                    EditMessageText message = new EditMessageText();
                    message.setChatId(chatId);
                    message.setText(text);
                    message.setMessageId((int) messageId);

                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    private void startCommandReceived(Long chatId, String name) {
        String answer = "Hi, " + name + ", nice to meet you!";
        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(answer);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var reservationButton = new InlineKeyboardButton();
        reservationButton.setText("Бронь стола");
        reservationButton.setCallbackData("RESERVATION_BUTTON");

        var buyButton = new InlineKeyboardButton();
        buyButton.setText("Купить");
        buyButton.setCallbackData("BUY_BUTTON");

        var deliveryButton = new InlineKeyboardButton();
        deliveryButton.setText("Доставка");
        deliveryButton.setCallbackData("DELIVERY_BUTTON");

        rowInLine.add(reservationButton);
        rowInLine.add(buyButton);
        rowInLine.add(deliveryButton);

        rowsInLine.add(rowInLine);

        keyboardMarkup.setKeyboard(rowsInLine);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    private void sendMessage(Long chatId, String textToSend){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
