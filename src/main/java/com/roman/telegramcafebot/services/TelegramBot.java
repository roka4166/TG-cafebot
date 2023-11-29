package com.roman.telegramcafebot.services;

import com.roman.telegramcafebot.config.BotConfig;
import com.roman.telegramcafebot.models.FoodMenu;
import com.roman.telegramcafebot.models.MenuItem;
import com.roman.telegramcafebot.models.Reservation;
import com.roman.telegramcafebot.repositories.AdminKeyRepository;
import com.roman.telegramcafebot.repositories.MenuItemRepository;
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

    private MainMenuKeyboardMarkup mainMenuKeyboardMarkup;
    private FoodMenuKeyboardMarkup foodMenuKeyboardMarkup;
    private TableChoosingKeyboardMarkup tableChoosingKeyboardMarkup;

    private AdminMenuKeyBoardMarkup adminMenuKeyBoardMarkup;

    private AdminKeyRepository adminKeyRepository;

    private MenuItemRepository menuItemRepository;
    private final BotConfig botConfig;
    private Reservation reservation;

    private MenuItem menuItem;

    private FoodMenu foodMenu;
    @Autowired
    public TelegramBot(Reservation reservation, BotConfig botConfig, MainMenuKeyboardMarkup mainMenuKeyboardMarkup,
                       FoodMenuKeyboardMarkup foodMenuKeyboardMarkup, TableChoosingKeyboardMarkup tableChoosingKeyboardMarkup,
                       AdminKeyRepository adminKeyRepository, MenuItemRepository menuItemRepository,
                       AdminMenuKeyBoardMarkup adminMenuKeyBoardMarkup, MenuItem menuItem, FoodMenu foodMenu){
        this.reservation = reservation;
        this.botConfig = botConfig;
        this.mainMenuKeyboardMarkup = mainMenuKeyboardMarkup;
        this.foodMenuKeyboardMarkup = foodMenuKeyboardMarkup;
        this.tableChoosingKeyboardMarkup = tableChoosingKeyboardMarkup;
        this.adminKeyRepository = adminKeyRepository;
        this.menuItemRepository = menuItemRepository;
        this.adminMenuKeyBoardMarkup = adminMenuKeyBoardMarkup;
        this.menuItem = menuItem;
        this.foodMenu = foodMenu;
    }

    private long coworkerChatId = 12; //TODO

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
                    sendMessage(chatId, "Введите ключ");
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
            } else if (messageText.startsWith("/key")) {
                String key = messageText.substring(5);
                String keyFromDB = Objects.requireNonNull(adminKeyRepository.findById(1).orElse(null)).getKey();
                if(key.equals(keyFromDB)){
                    sendMessage(chatId, "Ключ активирован", adminMenuKeyBoardMarkup.getAdminKeyboardMarkup());
                }
            }
            else if (messageText.startsWith("/newitem")){
                String[] menuItemInfo = messageText.substring(9).split(" ");
                MenuItem itemToAdd = new MenuItem();
                itemToAdd.setName(menuItemInfo[0]);
                itemToAdd.setPrice(Integer.valueOf(menuItemInfo[1]));
                itemToAdd.setType(menuItemInfo[2]);
                menuItemRepository.save(itemToAdd);
            }
            else if (messageText.startsWith("/deleteitem")){
                String itemName = messageText.substring(12);
                MenuItem itemForRemoval = menuItemRepository.findByName(itemName);
                menuItemRepository.delete(itemForRemoval);
            }
            else if (messageText.startsWith("/updateitem")){
                String[] menuItemInfo = messageText.substring(12).split(" ");
                MenuItem itemForUpdate = menuItemRepository.findByName(menuItemInfo[0]);
                itemForUpdate.setName(menuItemInfo[0]);
                itemForUpdate.setPrice(Integer.valueOf(menuItemInfo[1]));
                itemForUpdate.setType(menuItemInfo[2]);
                menuItemRepository.save(itemForUpdate);
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
                case "BUY_BUTTON" ->
                    sendMessage(chatId, "Menu", foodMenuKeyboardMarkup.getFoodMenuKeyboardMarkup());

                case "DELIVERY_BUTTON" -> {
                    sendMessage(chatId, "Menu", foodMenuKeyboardMarkup.getFoodMenuKeyboardMarkup());
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
                case "CREATE_BUTTON" -> {
                    String text = "Введите имя, цену и тип нового продукта, по типу /newitem кофе 70 напиток ";
                    sendMessage(chatId, text);
                }
                case "DRINKS_BUTTON" -> {
                    String text = "drinks";
                    List<MenuItem> allDrinks = menuItemRepository.findAllByType("напиток");
                    sendMessage(chatId, text, foodMenu.getFoodMenuKeyboardMarkup(allDrinks));
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
