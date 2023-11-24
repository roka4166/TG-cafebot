package com.roman.telegramcafebot.service;

import com.roman.telegramcafebot.config.BotConfig;
import com.roman.telegramcafebot.utils.MainMenuKeyboardMarkup;
import com.roman.telegramcafebot.utils.TableChoosingKeyboardMarkup;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private long adminChatId;
    private final BotConfig botConfig;

    private final MainMenuKeyboardMarkup mainMenuKeyboardMarkup = new MainMenuKeyboardMarkup();
    private final TableChoosingKeyboardMarkup tableChoosingKeyboardMarkup = new TableChoosingKeyboardMarkup();

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
            else-if(messageText.equals("/admin")){
                adminChatId = chatId;
                sendMessage(chatId, "Успешено");
            }
        }
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "RESERVATION_BUTTON" -> {
                    String text = "Введите номер стола: ";
                    sendMessage(chatId, text, tableChoosingKeyboardMarkup.getTableChoosingKeyboardMarkup());
                }
                case "BUY_BUTTON" -> {
                    String text = "buy";


                }
                case "DELIVERY_BUTTON" -> {
                    String text = "delivery";

                }
                case "TABLE_1" -> {
                    String text = "Вы выбрали стол 1, введите имя";
                    sendMessage(chatId, text);
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
        sendMessage(chatId, answer, mainMenuKeyboardMarkup.getMainMenuKeyboardMarkup());
    }

    private void sendMessage(Long chatId, String textToSend, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        sendMessage.setReplyMarkup(keyboardMarkup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(Long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void forwardMessage(Long ToChatId, Long fromChatId, Long messageId){
        ForwardMessage forwardMessage = new ForwardMessage();
    }

}
