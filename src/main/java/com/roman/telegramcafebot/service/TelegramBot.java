package com.roman.telegramcafebot.service;

import com.roman.telegramcafebot.config.BotConfig;
import com.roman.telegramcafebot.utils.MainMenuKeyboardMarkup;
import com.roman.telegramcafebot.utils.Reservation;
import com.roman.telegramcafebot.utils.TableChoosingKeyboardMarkup;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private Reservation reservation;
    @Autowired
    public TelegramBot(Reservation reservation, BotConfig botConfig){
        this.reservation = reservation;
        this.botConfig = botConfig;
    }

    private long coworkerChatId = 12;
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

            switch (messageText) {
                case "/start" -> {
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
                case "/admin" -> {
                    coworkerChatId = chatId;
                    sendMessage(chatId, "Успешено");
                }
            }
            if(messageText.startsWith("/reservation")) {
                reservation.setName(messageText.substring(10));
                String text = "Теперь нужно выбрать время. Что бы это сделать введите" +
                        " /time и время которые на которое вы хотите забронировать столик. Напримет */time 14.30*";
                sendMessage(chatId, text);
            } else if (messageText.startsWith("/time")) {
                reservation.setTime(messageText.substring(5));
                reservation.setCustomerChatId(chatId);
                String text = "Запрос на бронь отправлен нашему сотруднику, вы получите подтверждение как только " +
                        "сотрудник подтвердит бронь";
                sendMessage(chatId, text);
                sendMessage(coworkerChatId, reservation.toString(), createInlineKeyboardMarkup("Подтвердить бронь стола", "RESERVATION_CONFIRMED"));
            }
        }
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "RESERVATION_BUTTON" -> {
                    reservation = new Reservation();
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
                    reservation.setTable(1);
                    String text = "Вы выбрали стол 1, теперь нужно ввести имя на которое вы хотите" +
                            "забронировать столик. Введите /reservation  далее ваше имя например /reservation Роман";
                    sendMessage(chatId, text);
                }
                case "TABLE_2" -> {
                    String text = "Вы выбрали стол 2, теперь нужно ввести имя на которое вы хотите" +
                            "забронировать столик. Введите /reservation  далее ваше имя например /reservation Роман";
                    sendMessage(chatId, text);
                }
                case "TABLE_3" -> {
                    String text = "Вы выбрали стол 3, теперь нужно ввести имя на которое вы хотите" +
                            "забронировать столик. Введите /reservation  далее ваше имя например /reservation Роман";
                    sendMessage(chatId, text);
                }
                case "RESERVATION_CONFIRMED" -> {
                    String text = "Бронь стола подтверждена";
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

    private InlineKeyboardMarkup createInlineKeyboardMarkup(String text, String callbackDATA){
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackDATA);

        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        rowInLine.add(button);
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        rowsInLine.add(rowInLine);
        keyboardMarkup.setKeyboard(rowsInLine);
        return keyboardMarkup;
    }
}
