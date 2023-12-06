package com.roman.telegramcafebot.services;

import com.roman.telegramcafebot.config.BotConfig;
import com.roman.telegramcafebot.models.Button;
import com.roman.telegramcafebot.models.Coworker;
import com.roman.telegramcafebot.models.Order;
import com.roman.telegramcafebot.models.Reservation;
import com.roman.telegramcafebot.repositories.AdminKeyRepository;
import com.roman.telegramcafebot.repositories.ButtonRepository;
import com.roman.telegramcafebot.repositories.CoworkerRepository;
import com.roman.telegramcafebot.repositories.OrderRepository;
import com.roman.telegramcafebot.utils.*;
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
import java.util.Objects;

@Service
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private KeyboardMarkup keyboardMarkup;

    private AdminKeyRepository adminKeyRepository;
    private final BotConfig botConfig;
    private Reservation reservation;

    private Button button;

    private CoworkerRepository coworkerRepository;

    private ButtonRepository buttonRepository;

    private OrderRepository orderRepository;

    private Order order;
    @Autowired
    public TelegramBot(Reservation reservation, BotConfig botConfig, KeyboardMarkup keyboardMarkup,
                       AdminKeyRepository adminKeyRepository, ButtonRepository buttonRepository,
                       CoworkerRepository coworkerRepository, OrderRepository orderRepository,
                       Order order, Button button){
        this.reservation = reservation;
        this.botConfig = botConfig;
        this.keyboardMarkup = keyboardMarkup;
        this.adminKeyRepository = adminKeyRepository;
        this.buttonRepository = buttonRepository;
        this.coworkerRepository = coworkerRepository;
        this.orderRepository = orderRepository;
        this.order = order;
        this.button = button;
    }

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
                case "/adminoff" -> {
                    Coworker coworker = coworkerRepository.findCoworkerByChatId(String.valueOf(chatId));
                    if(coworker != null){
                        coworker.setActive(false);
                        coworkerRepository.save(coworker);
                    }
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
                sendMessage(getCoworkerChatId(), reservation.toString(), createOneButton("Подтвердить бронь стола", "RESERVATION_CONFIRMED"));
            } else if (messageText.startsWith("/key")) {
                String key = messageText.substring(5);
                String keyFromDB = Objects.requireNonNull(adminKeyRepository.findById(1).orElse(null)).getKey();
                if(key.equals(keyFromDB)){
                    Coworker coworker = new Coworker();
                    coworker.setChatId(String.valueOf(chatId));
                    coworkerRepository.save(coworker);
                    sendMessage(chatId, "Ключ активирован", keyboardMarkup.getKeyboardMarkup("adminmenu"));
                }
            }
            else if (messageText.startsWith("/newitem")){
                String menuItemInfo = messageText.substring(9);
                button.setName(menuItemInfo);
                button.setCallbackData("ADDITEM_BUTTON"+menuItemInfo);
                sendMessage(getCoworkerChatId(), "К какому разделу относиться?",
                        keyboardMarkup.getKeyboardMarkup("adminmenu"));

            }
            else if (messageText.startsWith("/deleteitem")){
                String itemName = messageText.substring(12);
                Button itemForRemoval = buttonRepository.findButtonByNameStartingWith(itemName);
                buttonRepository.delete(itemForRemoval);
            }
            else if (messageText.startsWith("/updateitem")){
                String[] menuItemInfo = messageText.substring(12).split(" ");
                Button itemForUpdate = buttonRepository.findButtonByNameStartingWith(menuItemInfo[0]);
                itemForUpdate.setName(menuItemInfo[0] + " " + menuItemInfo[1]);
                itemForUpdate.setBelongsToMenu(menuItemInfo[2] + "меню");
                itemForUpdate.setCallbackData(menuItemInfo[0].toUpperCase() + " _BUTTON");
                buttonRepository.save(itemForUpdate);
            }
            else if (messageText.equals("/done")){
                sendMessage(getCoworkerChatId(), order.toString());
            }
        }
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if(callbackData.startsWith("ADDITEM_BUTTON")){
                String[] itemInfo = callbackData.substring(14).split(" ");
                sendMessage(chatId, "Добавить в корзину?", keyboardMarkup.getKeyboardMarkup("confirmationmenu", itemInfo[0], itemInfo[1]));
            }
            else if (callbackData.startsWith("ADDTOCART_BUTTON")){
                String[] itemInfo = callbackData.substring(16).split(" ");
                ifOrderTotalPriceNullInitialize();
                order.setTotalPrice(order.getTotalPrice() + Integer.parseInt(itemInfo[1]));
                ifItemStringIsNullInitialize();
                order.setItems(order.getItems() + " " + itemInfo[0]);
                sendMessage(chatId, "Добавлено в корзину", getGoToMenuButton());
            }
            else if (callbackData.startsWith("TYPE")){
                String typeOfMenu = callbackData.substring(4);
                button.setBelongsToMenu(typeOfMenu+"меню");
                buttonRepository.save(button);
                sendMessage(getCoworkerChatId(), "Добавлено успешно", getGoToMenuButton());
            }
            else if (callbackData.startsWith("TABLE_")){
                int tableNumber = Integer.parseInt(callbackData.substring(6));
                reservation.setTable(tableNumber);
                String text = "Вы выбрали стол " + tableNumber + " теперь нужно ввести имя на которое вы хотите" +
                        "забронировать столик. Введите /reservation  далее ваше имя например /reservation Роман";
                sendMessage(chatId, text);
            }

            switch (callbackData) {
                case "RESERVATION_BUTTON" -> {
                    reservation = new Reservation();
                    String text = "Введите номер стола: ";
                    sendMessage(chatId, text, keyboardMarkup.getKeyboardMarkup("tablemenu"));
                }
                case "FOODMENU_BUTTON" ->
                    sendMessage(chatId, "Menu", keyboardMarkup.getKeyboardMarkup("foodmenu"));

                case "RESERVATION_CONFIRMED" -> {
                    String text = "Бронь стола подтверждена";
                    sendMessage(chatId, text);
                }
                case "CREATE_BUTTON" -> {
                    String text = "Введите имя, цену и тип нового продукта, по типу /newitem кофе 70 напиток ";
                    sendMessage(chatId, text);
                }
                case "DRINKS_BUTTON" -> {
                    String text = "drinks";
                    sendMessage(chatId, text, keyboardMarkup.getKeyboardMarkup("напитокменю"));
                }
                case "CROISSANT_BUTTON" -> {
                    sendMessage(chatId, "Croisaants", keyboardMarkup.getKeyboardMarkup("croissantменю"));
                }
                case "ROMANPIZZA_BUTTON" -> {
                    sendMessage(chatId,"ROmanpizza", keyboardMarkup.getKeyboardMarkup("romanpizzaменю"));
                }
                case "PAYMENT_BUTTON" -> {
                    String text = "Для оплаты нужно перевести" + order.getTotalPrice() + " рублей на карту 1234 3456 " +
                            "2345 4556 или на телефон 9485749284 далее нажмите на кнопку олпачено и после этого" +
                            "информация попадет к нашему сотруднику";
                    sendMessage(chatId, text, createOneButton("Подтвердить", "PAYMENTCONFIRMED_BUTTON"));
                }
                case "PAYMENTCONFIRMED_BUTTON" -> {
                    sendMessage(chatId, "Благодарим за покупку");
                    Long coworkerChatId = Long.valueOf(Objects.requireNonNull(coworkerRepository.findById(1).orElse(null)).getChatId());
                    sendMessage(coworkerChatId, "Заказ на сумму " + order.getTotalPrice() + " рублей " + order.toString());
                    saveOrder(order);
                    order = new Order();                }
            }
        }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    private void startCommandReceived(Long chatId, String name) {
        String answer = "Hi, " + name + ", nice to meet you!";
        sendMessage(chatId, answer, keyboardMarkup.getKeyboardMarkup("mainmenu"));
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

    private void sendMessage(Long chatId, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setReplyMarkup(keyboardMarkup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private InlineKeyboardMarkup createOneButton(String text, String callbackDATA){
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

    private Long getCoworkerChatId(){
        return Long.valueOf(Objects.requireNonNull(coworkerRepository.findById(1).orElse(null)).getChatId());
    }
    private void saveOrder(Order order){
        orderRepository.save(order);
    }

    private void ifOrderTotalPriceNullInitialize() {
        if (order.getTotalPrice() == null) {
            order.setTotalPrice(0);
        }
    }
    private void ifItemStringIsNullInitialize() {
        if (order.getItems() == null) {
            order.setItems("");
        }
    }
    private InlineKeyboardMarkup getGoToMenuButton(){
        return createOneButton("Перейти в меню", "FOODMENU_BUTTON");
    }


}
