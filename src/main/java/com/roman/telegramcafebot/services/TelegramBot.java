package com.roman.telegramcafebot.services;

import com.roman.telegramcafebot.config.BotConfig;
import com.roman.telegramcafebot.models.*;
import com.roman.telegramcafebot.repositories.*;
import com.roman.telegramcafebot.utils.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private Map<Long, Boolean> expectingNameForReservationMap = new HashMap<>();
    private Map<Long, Boolean> expectingTimeForReservationMap = new HashMap<>();
    private Map<Long, Boolean> expectingTimeForPreorder = new HashMap<>();
    private final Map<Long, Boolean> expectingCommentFromCoworker = new HashMap<>();

    private Map<Long, String> orderTime = new HashMap<>();

    private List<Reservation> reservations = new ArrayList<>();


    private KeyboardMarkup keyboardMarkup;

    private AdminPassowrdRepository adminPasswordRepository;
    private final BotConfig botConfig;

    private CoworkerRepository coworkerRepository;

    private ButtonRepository buttonRepository;

    private OrderRepository orderRepository;

    private MenuItemRepository menuItemRepository;

    private CartRepository cartRepository;




    @Autowired
    public TelegramBot(BotConfig botConfig, KeyboardMarkup keyboardMarkup,
                       AdminPassowrdRepository adminPasswordRepository, ButtonRepository buttonRepository,
                       CoworkerRepository coworkerRepository, OrderRepository orderRepository,
                       MenuItemRepository menuItemRepository, CartRepository cartRepository){
        this.botConfig = botConfig;
        this.keyboardMarkup = keyboardMarkup;
        this.adminPasswordRepository = adminPasswordRepository;
        this.buttonRepository = buttonRepository;
        this.coworkerRepository = coworkerRepository;
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.cartRepository = cartRepository;
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
                handleMessage(update, messageText, chatId);

            }
            else if (update.hasCallbackQuery()) {
                long chatId = update.getCallbackQuery().getMessage().getChatId();
                if(isAnyCoworkerActive()){
                    handleCallbackQuery(update);
                }
                else
                    sendMessage(chatId, "В данный момент бот не активен");
            }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    private void handleMessage(Update update, String messageText, Long chatId){
            MessageCommand messageCommand = MessageCommand.fromMessageText(messageText);
            if(messageCommand!=null){
                switch (messageCommand) {
                    case COMMAND_START -> {
                        removeAllFromCart(chatId);
                        sendMessage(chatId, "Im alive!!");
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    }
                    case COMMAND_ADMIN_OFF -> updateCoworkerActivity(chatId, false);
                    case COMMAND_ADMIN_ON -> updateCoworkerActivity(chatId, true);
                    case COMMAND_SHOWALLSTOPPED -> {
                        if (validateCoworker(chatId)) {
                            sendMessage(chatId, getMenuText(getAllStoppedMenuItems()));
                        }
                    }
                }
                if (messageText.startsWith(MessageCommand.COMMAND_PASSWORD.getMessageCommand())) {
                    String password = messageText.substring(10);
                    String passwordFromDB = Objects.requireNonNull(adminPasswordRepository.findById(1).orElse(null)).getKey();
                    if (password.equals(passwordFromDB)) {
                        Coworker coworker = new Coworker();
                        coworker.setChatId(chatId);
                        coworker.setActive(true);
                        coworkerRepository.save(coworker);
                        sendMessage(chatId, "Ключ активирован");
                    }
                } else if (messageText.startsWith(MessageCommand.COMMAND_ADMIN.getMessageCommand())) {
                    if (validateCoworker(chatId)) {
                        sendMessage(chatId, "Меню", keyboardMarkup.getKeyboardMarkup(getButtons("adminmainmenu"), 3));
                    } else {
                        sendMessage(chatId, "Нет доступа к функции");
                    }
                }
                else if (messageText.startsWith(MessageCommand.COMMAND_NEW_ITEM.getMessageCommand())) {
                    if (validateCoworker(chatId)) {
                        createMenuItem(messageText);
                        sendMessage(getCoworkerChatId(), "К какому разделу относиться?",
                                keyboardMarkup.getKeyboardMarkup(getButtons("adminmenu"), 3));
                    } else sendMessage(chatId, "Не добавлено в корзину, у вас нет доступа к этой функции");
                }
                else if (messageText.startsWith(MessageCommand.COMMAND_DELETE_ITEM.getMessageCommand())) {
                    if (validateCoworker(chatId)) {
                        deleteMenuItem(messageText);
                        sendMessage(chatId, "Удалено успешно", getGoToMenuButton());
                    } else sendMessage(chatId, "Не удалено, у вас нет доступа к этой функции");
                }
            }
                if(expectingNameForReservationMap.containsKey(chatId) && expectingNameForReservationMap.get(chatId)) {
                    Reservation reservation = findReservationByChatId(chatId);
                    reservation.setName(messageText);
                    sendMessage(chatId, "Теперь введите время");
                    expectingNameForReservationMap.put(chatId, false);
                    expectingTimeForReservationMap.put(chatId, true);
                }

                else if (expectingTimeForReservationMap.containsKey(chatId) && expectingTimeForReservationMap.get(chatId)) {
                    Reservation reservation = findReservationByChatId(chatId);
                    reservation.setTime(messageText);
                    sendMessage(chatId, getBookingConfirmationTextByChatId(chatId),
                            keyboardMarkup.getKeyboardMarkup(getButtons("bookingconfirmationmenu"), 2));
                    expectingTimeForReservationMap.put(chatId, false);
                }

                else if (expectingCommentFromCoworker.containsKey(getCoworkerChatId()) && expectingCommentFromCoworker.get(getCoworkerChatId())) {
                    Reservation reservation = findReservationByCommentExpectation();
                    reservation.setCoworkerComment(messageText);
                    sendMessage(getCoworkerChatId(), "Бронь отклонена, информация об этом отправилась клиенту");
                    String text = String.format("Бронь отклонена. Комментарий от сотрудника: %s", reservation.getCoworkerComment());
                    sendMessage(reservation.getChatId(), text);
                    expectingCommentFromCoworker.put(getCoworkerChatId(), false);
                    deleteReservation(reservation);
                }
                else if(expectingTimeForPreorder.containsKey(chatId) && expectingTimeForPreorder.get(chatId)) {
                    orderTime.put(chatId, messageText);
                    sendMessage(chatId,"Подтвердить время" + messageText + " ?", keyboardMarkup.getKeyboardMarkup(getButtons("preordertimemenu"), 2));
                }
        }

    private List<MenuItem> getAllStoppedMenuItems() {
        return menuItemRepository.findAllByIsStoppedTrue();
    }

    private void stopMenuItem(String callbackData) {
        int menuItemId = Integer.parseInt(callbackData.substring(7));
        MenuItem menuItem = menuItemRepository.findById(menuItemId).orElse(null);
        if(menuItem != null){
            String belongsToMenu = menuItem.getBelongsToMenu();
            menuItem.setStopped(true);
            menuItem.setBelongsToMenu("STOPPED");
            menuItemRepository.save(menuItem);
            Button button = buttonRepository.findButtonByCallbackData("ITEM"+menuItem.getId());
            button.setBelongsToMenu("STOPPED"+belongsToMenu);
            button.setCallbackData("STOPPEDITEM"+menuItemId);
            buttonRepository.save(button);
            }
    }
    private boolean unstopMenuItem(String callbackData) {
        int menuItemId = Integer.parseInt(callbackData.substring(11));
        MenuItem menuItem = menuItemRepository.findById(menuItemId).orElse(null);
        if(menuItem != null){
            menuItem.setStopped(false);
            String belongsToMenu = menuItem.getBelongsToMenu().replace("STOPPED", "");
            menuItem.setBelongsToMenu(belongsToMenu);
            menuItemRepository.save(menuItem);

            Button button = buttonRepository.findButtonByCallbackData("STOPPEDITEM"+menuItem.getId());
            button.setBelongsToMenu(belongsToMenu);
            button.setCallbackData("ITEM"+menuItemId);
            buttonRepository.save(button);
            return true;
        }
        return false;
    }

    private void updateCoworkerActivity(Long chatId, boolean active) {
        Coworker coworker = coworkerRepository.findCoworkerByChatId(chatId);
        if (coworker != null) {
            coworker.setActive(active);
            coworkerRepository.save(coworker);
        }
    }

    private void handleCallbackQuery(Update update){
        String callbackData = update.getCallbackQuery().getData();
        CallbackDataCommand callbackDataCommand = CallbackDataCommand.fromCallbackData(callbackData);
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (callbackData.startsWith(CallbackDataCommand.TYPE.getCallbackDataCommand())){
            createButtonFromMenuItem(setMenuItemsMenuBelonging(callbackData));
            sendMessage(getCoworkerChatId(), "Добавлено успешно", getGoToMenuButton());
        }
        else if (callbackData.startsWith(CallbackDataCommand.AMOUNT.getCallbackDataCommand())){
            String amountOfPeople = callbackData.substring(6);
            Reservation reservation = new Reservation();
            reservation.setChatId(chatId);
            reservation.setAmountOfPeople(amountOfPeople);
            reservations.add(reservation);
            String text = String.format("Вы выбрали кол-во человек %s, теперь нужно ввести имя на которое вы хотите " +
                    "забронировать столик.", amountOfPeople);
            sendMessage(chatId, text);
            expectingNameForReservationMap.put(chatId, true);
        }
        else if (callbackData.startsWith(CallbackDataCommand.REMOVEFROMCART.getCallbackDataCommand())){
            removeFromCart(chatId, callbackData);
            sendMessage(chatId, "Удалено из корзины", getGoToMenuButton());
        }
        else if(callbackData.startsWith(CallbackDataCommand.ITEM.getCallbackDataCommand())){
            if(validateCoworker(chatId)){
                sendMessage(chatId, "Что сделать?",
                        keyboardMarkup.getKeyboardMarkup(getAdminItemButtons(callbackData), 2));
                return;
            }
            sendMessage(chatId, createConfirmationText(findItemById(callbackData)),
                    keyboardMarkup.getKeyboardMarkup(getConfirmationButtons(callbackData), 2));
        }
        else if(callbackData.startsWith(CallbackDataCommand.STOPPEDITEM.getCallbackDataCommand())){
            if(unstopMenuItem(callbackData)){
                sendMessage(chatId, "Стоп снят успешно",
                        keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons("adminmainmenu"), 3));
            }
            else {
                sendMessage(chatId, "Что то пошло не так, стоп не был снять",
                        keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons("adminmainmenu"), 3));
            }
        }
        else if(callbackData.startsWith(CallbackDataCommand.ADDTOCART.getCallbackDataCommand())){
            addToCart(chatId, callbackData);
            sendMessage(chatId,"Добавлено успешно", getGoToMenuButton());
        }
        else if(callbackData.startsWith(CallbackDataCommand.RESERVATION_CONFIRMED.getCallbackDataCommand())){
           try {
               Reservation reservation = findReservationById(callbackData);
               String text = "Бронь стола подтверждена";
               sendMessage(getCoworkerChatId(), text);
               sendMessage(reservation.getChatId(), String.format("Бронь стола на имя %s подтверждена. Время %s. Кол-во человек %s",
                        reservation.getName(), reservation.getTime(), reservation.getAmountOfPeople()));
               deleteReservation(reservation);
           }
           catch (Exception e){
               sendMessage(getCoworkerChatId(), "Нет брони");
           }
        }
        else if(callbackData.startsWith(CallbackDataCommand.RESERVATION_DECLINED.getCallbackDataCommand())){
            synchronized (expectingCommentFromCoworker) {
                if (!expectingCommentFromCoworker.containsValue(true)) {
                    Reservation reservation = findReservationById(callbackData);
                    reservation.setCoworkerComment("expecting");
                    String text = "Введите причину отказа брони";
                    sendMessage(getCoworkerChatId(), text);
                    expectingCommentFromCoworker.put(getCoworkerChatId(), true);
                }
            }
        }
        else if (callbackData.startsWith(CallbackDataCommand.PAYMENTCONFIRMEDBYCOWORKER.getCallbackDataCommand())){
            Order order = orderRepository.findById(Integer.valueOf(callbackData.substring(26))).orElse(null);
            assert order != null;
            sendMessage(order.getChatId(), "Оплата была подтверждена работником");
            sendMessage(getCoworkerChatId(), "Оплата подтверждена");
        }
        else if (callbackData.startsWith(CallbackDataCommand.DELETEITEM.getCallbackDataCommand())){
            try {
                deleteMenuItem(callbackData);
                sendMessage(chatId, "Товар удален");
            }
            catch (Exception e){
                sendMessage(chatId, "что то пошло не так, товар не удален");
            }
        }
        else if (callbackData.startsWith(CallbackDataCommand.SETSTOP.getCallbackDataCommand())){
            try{
                stopMenuItem(callbackData);
                sendMessage(chatId, "Стоп на товар поставлен",
                        keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons("foodmenu"), 3));
            }
            catch (Exception e){
                sendMessage(chatId, "Что то пошло не так, стоп не поставлен",
                        keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons("foodmenu"), 3));
            }
        }

        switch (callbackDataCommand) {
            case START ->
                    sendMessage(chatId,"Главное меню",
                            keyboardMarkup.getKeyboardMarkup(getButtons("mainmenu"),  1));

            case RESERVATION -> {
                removeAllReserVationsByChatId(chatId);
                String text = "Введите количество человек: ";
                sendMessage(chatId, text,
                        keyboardMarkup.getKeyboardMarkup((getButtons("amountofpeoplemenu")), 4));
            }
            case CONFIRMBOOKING -> {
                sendMessage(chatId, "Бронь стола отправлена нашему сотруднику," +
                        "   если на это время есть столик, " +
                        "то вы получите подтверждения что стол был забронирован успешно.");
                sendMessage(getCoworkerChatId(), findReservationByChatId(chatId).toString(),
                        keyboardMarkup.getBookingConfirmationAdminMenu(getButtons("bookingconfirmationadminmenu"),
                                reservations.indexOf(findReservationByChatId(chatId))));
            }
            case CONFIRMPREORDERTIME -> {
                String text = String.format("Для оплаты нужно перевести %d рублей на карту 1234 3456 2345 4556" +
                        " или на телефон 9485749284. Далее нажмите на кнопку 'Оплачено'," +
                        " и после этого информация попадет к нашему сотруднику", getTotalPriceOfCartByChatId(chatId));
                sendMessage(chatId, text, createOneButton("Подтвердить", "PAYMENTCONFIRMEDBYCUSTOMER"));
                expectingTimeForPreorder.put(chatId, false);
            }
            case DECLINEPREORDERTIME -> {
                sendMessage(chatId, "Вернуться в меню", getGoToMenuButton());
                expectingTimeForPreorder.put(chatId, false);
            }
            case FOODMENU ->
                    sendMessage(chatId, "Menu", keyboardMarkup.getKeyboardMarkup(getButtons("foodmenu"), 3));

            case BREAKFASTS ->
                    sendMessage(chatId, getMenuText(getItemsByMenuBelonging("breakfastmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("breakfastmenu")), 4));

            case CROISSANTS ->
                    sendMessage(chatId, getMenuText(getItemsByMenuBelonging("croissantmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("croissantmenu")), 4));

            case ROMANPIZZAS ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("romanpizzamenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("romanpizzamenu")), 4));

            case HOTFOOD ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("hotfoodmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("hotfoodmenu")), 4));

            case PIESCHUDU ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("pieschudumenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("pieschudumenu")), 4));

            case SOUPS ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("soupmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("soupmenu")), 4));

            case SALADS ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("saladmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("saladmenu")), 4));

            case SANDWICHES ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("sandwichmenu")), keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("sandwichmenu")), 4));

            case BRUSCHETTAS ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("bruschettamenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("bruschettamenu")), 4));

            case ADDITION ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("additionmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("additionmenu")), 4));

            case DRINKS ->
                    sendMessage(chatId,"Напитки",
                            keyboardMarkup.getKeyboardMarkup(getButtons("drinkmenu"), 2));

            case DESERTS ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("desertmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("desertmenu")), 4));
            case PUFFPASTRY ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("puffpastrymenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("puffpastrymenu")), 4));

            case BREAD ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("breadmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("breadmenu")), 4));

            case TEA ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("teamenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("teamenu")), 4));

            case COFFEE ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("coffemenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("coffemenu")), 4));

            case CACAO ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("cacaomenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("cacaomenu")),4 ));

            case SIGNATUREDRINKS ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("signaturedrinksmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("signaturedrinksmenu")), 4));

            case NOTTEA ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("notteamenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("notteamenu")), 4));

            case DRINKSADDITION ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("drinksadditionmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("drinksadditionmenu")), 4));

            case GOTOPAYMENT -> {
                expectingTimeForPreorder.put(chatId, true);
                sendMessage(chatId,"Введите время в которое вы хотите забрать заказ");
            }
            case PAYMENTCONFIRMEDBYCUSTOMER -> {
                Order order = createOrder(chatId);
                sendMessage(chatId, String.format("Благодарим за покупку. Ваш заказ номер %d был принят." +
                        " Укажите этот номер когда будете забирать заказ.", order.getId()));
                Long coworkerChatId = getCoworkerChatId();
                sendMessage(coworkerChatId, order + ". Нажмите подтвердить что бы подтвердить оплату",
                        createOneButton("Подтвердить оплату", "PAYMENTCONFIRMEDBYCOWORKER"+order.getId()));
            }
            case SHOWCART -> {
                if(checkIfCartIsEmpty(chatId)){
                    sendMessage(chatId, "Корзина пуста", getGoToMenuButton());
                }
                else {
                    sendMessage(chatId, getMenuTextForCart(getAllMenuItemsFromCart(getAllItemsInCartByChatId(chatId))),
                            keyboardMarkup.getCartKeyBoardMarkup(getAllMenuItemsFromCart(getAllItemsInCartByChatId(chatId))));
                }
            }
            case REMOVEALLFROMCART -> {
                removeAllFromCart(chatId);
                sendMessage(chatId, "Корзина очищина", getGoToMenuButton());
            }
            case ADMINFOODMENU -> {
                sendMessage(chatId, "Меню",
                        keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons("foodmenu"), 3));
            }
            case SHOWALLSTOPPED -> {
                showAllStoppedItems(chatId);
            }
        }
    }

    private void showAllStoppedItems(long chatId) {
        List<MenuItem> stoppedItems = menuItemRepository.findAllByIsStoppedTrue();
        sendMessage(chatId, getMenuTextForStoppedItems(stoppedItems),
                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("STOPPED")), 4));
    }


    private void startCommandReceived(Long chatId, String name) {
        String answer = String.format("Приветствую, %s", name);
        sendMessage(chatId, answer, keyboardMarkup.getKeyboardMarkup(getButtons("mainmenu"), 1));
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
    private InlineKeyboardMarkup getGoToMenuButton(){
        return createOneButton("Перейти в меню", "FOODMENU");
    }
    private String getItemsName(String itemInfo){
        Pattern p = Pattern.compile("\"([^\"]*)\"");
        Matcher m = p.matcher(itemInfo);
        m.find();
        return m.group().replace("\"", "");
    }
    private String getItemsPrice(String itemInfo){
        String [] info = itemInfo.split(" ");
        return info[info.length-1];
    }
    private boolean validateCoworker(Long chatId){
        Coworker coworker = coworkerRepository.findCoworkerByChatId(chatId);
        return coworker != null;
    }
    private Long getCoworkerChatId(){
        Coworker coworker = coworkerRepository.findCoworkerByIsActiveTrue();
        if(coworker != null){
            return coworker.getChatId();
        }
        return null;
    }
    private String getMenuText(List<MenuItem> items){
        List<Button> buttons = buttonRepository.findAllByBelongsToMenu(items.get(0).getBelongsToMenu());
        StringBuilder menuText = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            buttons.get(i).setName(String.valueOf(i+1));
            buttons.get(i).setCallbackData("ITEM"+items.get(i).getId());
            menuText.append(i+1).append(". ").append(items.get(i).getName()).append(" ").append(items.get(i).getPrice()).append(" руб.").append("\n");
        }
        return menuText.toString();
    }
    private String getMenuTextForStoppedItems(List<MenuItem> items){
        List<Button> buttons = buttonRepository.findAllByBelongsToMenuStartingWith("STOPPED");
        StringBuilder menuText = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            buttons.get(i).setName(String.valueOf(i+1));
            buttons.get(i).setCallbackData("STOPPEDITEM"+items.get(i).getId());
            menuText.append(i+1).append(". ").append(items.get(i).getName()).append(" ").append(items.get(i).getPrice()).append(" руб.").append("\n");
        }
        return menuText.toString();
    }

    private String getMenuTextForCart(List<MenuItem> items){
        StringBuilder menuText = new StringBuilder();
        String cartMenuText = """
                Что бы удалить предмет из корзины, просто кликните по номеру который соответствует товару

                """;
        menuText.append(cartMenuText);
        int sum = 0;
        for (int i = 0; i < items.size(); i++) {
            menuText.append(i+1).append(". ").append(items.get(i).getName()).append(" ").append(items.get(i).getPrice()).append(" руб.").append("\n");
            sum += items.get(i).getPrice();
        }
        String infoAboutSum = String.format("\nСумма корзины %d", sum);
        menuText.append(infoAboutSum);
        return menuText.toString();
    }
    private List<Button> sortButtonsByName(List<Button> buttonsToSort){
        buttonsToSort.sort((button1, button2) -> {
            int number1 = Integer.parseInt(button1.getName());
            int number2 = Integer.parseInt(button2.getName());
            return Integer.compare(number1, number2);
        });
        return buttonsToSort;
    }
    private List<MenuItem> getItemsByMenuBelonging(String belongsToMenu){
        return menuItemRepository.findAllByBelongsToMenuAndIsStoppedFalse(belongsToMenu);
    }
    private List<Button> getButtons (String typeOfMenu){
        return buttonRepository.findAllByBelongsToMenuStartingWith(typeOfMenu);
    }
    private List<Button> getAdminFoodMenuButtons (String typeOfMenu){
        return buttonRepository.findAllByBelongsToMenu(typeOfMenu);
    }
    private List<Button> getConfirmationButtons (String callbackDataWithId){
        String id = callbackDataWithId.substring(4);
        List<Button> buttons = buttonRepository.findAllByBelongsToMenu("confirmationmenu");
        for (Button button1 : buttons){
            if(button1.getCallbackData().startsWith("ADDTOCART")){
                button1.setCallbackData("ADDTOCART"+id);
            }
        }
        return buttons;
    }
    private List<Button> getAdminItemButtons (String callbackDataWithId){
        String id = callbackDataWithId.substring(4);
        List<Button> buttons = buttonRepository.findAllByBelongsToMenu("adminitemmenu");
        for (Button button1 : buttons){
            String oldCallbackData = button1.getCallbackData();
            if(oldCallbackData.startsWith("DELETEITEM")){
                button1.setCallbackData("DELETEITEM"+id);
            }
            else {
                button1.setCallbackData("SETSTOP"+id);
            }
        }
        return buttons;
    }
    private String createConfirmationText(MenuItem item){
        return String.format("Вы выбрали %s. Цена %d руб.", item.getName(), item.getPrice());
    }
    private MenuItem findItemById(String id){
        return menuItemRepository.findById(Integer.valueOf(id.substring(4))).orElse(null);
    }
    private void addToCart(Long chatId, String callbackData){
        int menuItemId = Integer.parseInt(callbackData.substring(9));
        Cart cart = new Cart();
        cart.setChatId(chatId);
        cart.setItemsId(menuItemId);
        cart.setExpirationDate(LocalDateTime.now().plusMinutes(15));
        cartRepository.save(cart);
    }
    private Order createOrder(Long chatId){
        Order order = new Order();
        List<Cart> allItemsInCart = getAllItemsInCartByChatId(chatId);
        List<MenuItem> menuItems = getAllMenuItemsFromCart(allItemsInCart);
        order.setChatId(chatId);
        order.setTotalPrice(getTotalPriceOfCartByChatId(chatId));
        StringBuilder itemsAsString = new StringBuilder();
        for(MenuItem item : menuItems){
            itemsAsString.append(item.getName()).append(", ");
        }
        order.setItems(itemsAsString.toString());
        order.setTime(findOrderTimeByChatId(chatId));
        orderRepository.save(order);
        orderTime.remove(chatId);
        removeAllFromCart(chatId);
        return order;
    }
    private String findOrderTimeByChatId(Long chatId){
        if(orderTime.containsKey(chatId)){
            return orderTime.get(chatId);
        }
        return null;
    }
    private void deleteMenuItem(String callbackData){
        int menuItemId = Integer.parseInt(callbackData.substring(10));
        MenuItem menuItemForRemoval = menuItemRepository.findById(menuItemId).orElse(null);
        Button buttonForRemoval = buttonRepository.findButtonByCallbackData("ITEM"+menuItemId);
        assert buttonForRemoval != null;
        buttonRepository.delete(buttonForRemoval);
        assert menuItemForRemoval != null;
        menuItemRepository.delete(menuItemForRemoval);
    }
    private void createMenuItem(String menuItemInfo){
        String itemInfo = menuItemInfo.trim();
        String menuItemName = getItemsName(itemInfo);
        int menuItemsPrice = Integer.parseInt(getItemsPrice(itemInfo));
        MenuItem menuItem = new MenuItem();
        menuItem.setName(menuItemName);
        menuItem.setPrice(menuItemsPrice);
        menuItem.setBelongsToMenu("unknown");
        menuItem.setStopped(false);
        menuItemRepository.save(menuItem);
    }

    private MenuItem setMenuItemsMenuBelonging(String belongsToMenu){
        MenuItem menuItem = menuItemRepository.findMenuItemByBelongsToMenu("unknown");
        menuItem.setBelongsToMenu(belongsToMenu.substring(4)+"menu");
        menuItemRepository.save(menuItem);
        return menuItem;
    }
    private void createButtonFromMenuItem(MenuItem menuItem){
        Button buttonToAdd = new Button();
        List<MenuItem> items = menuItemRepository.findAllByBelongsToMenuAndIsStoppedFalse(menuItem.getBelongsToMenu());
        buttonToAdd.setName(String.valueOf(items.size()));
        buttonToAdd.setBelongsToMenu(menuItem.getBelongsToMenu());
        buttonToAdd.setCallbackData("ITEM"+(menuItem.getId()));
        buttonRepository.save(buttonToAdd);
    }
    private void removeFromCart(Long chatId, String callbackData){
        List<Cart> items = cartRepository.findAllByChatId(chatId);
        int menuItemId = Integer.parseInt(callbackData.substring(14));
        for (Cart cart : items){
            if(cart.getItemsId()==menuItemId){
                cartRepository.delete(cart);
            }
        }
    }
    private List<Cart> getAllItemsInCartByChatId(Long chatId){
        return cartRepository.findAllByChatId(chatId);
    }
    private List<MenuItem> getAllMenuItemsFromCart(List<Cart> cart){
        List<MenuItem> menuItems = new ArrayList<>();
        for (Cart cart1 : cart){
            menuItems.add(menuItemRepository.findById(cart1.getItemsId()).orElse(null));
        }
        return menuItems;
    }

    private int getTotalPriceOfCartByChatId(Long chatId){
        int totalPrice = 0;
        List<Cart> cart = getAllItemsInCartByChatId(chatId);
        for (Cart cart1 : cart){
            totalPrice += Objects.requireNonNull(menuItemRepository.findById(cart1.getItemsId()).orElse(null)).getPrice();
        }
        return totalPrice;
    }
    private boolean checkIfCartIsEmpty(Long chatId){
        List<Cart> cart = getAllItemsInCartByChatId(chatId);
        return cart.isEmpty();
    }
    private void removeAllFromCart(Long chatId){
        cartRepository.deleteAllByChatId(chatId);
    }



    private String getBookingConfirmationTextByChatId(Long chatId){
        Reservation reservation = findReservationByChatId(chatId);
        return String.format("Подтвердить бронь на имя %s. На время %s?", reservation.getName(), reservation.getTime());
    }

    private Reservation findReservationByChatId(Long chatId){
        for(Reservation reservation : reservations){
            if(Objects.equals(reservation.getChatId(), chatId)){
                return reservation;
            }
        }
        return null;
    }
    private Reservation findReservationById(String callbackData){
        int id = Integer.parseInt(callbackData.substring(21));
        return reservations.get(id);
    }
    private Reservation findReservationByCommentExpectation(){
        for (Reservation reservation : reservations){
            if(reservation.getCoworkerComment().equals("expecting")){
                return reservation;
            }
        }
        return null;
    }
    private void deleteReservation(Reservation reservation){
        reservations.remove(reservation);
    }
    private void removeAllReserVationsByChatId(Long chatId){
        reservations.removeIf(reservation -> Objects.equals(reservation.getChatId(), chatId));
    }
    private boolean isAnyCoworkerActive(){
        Coworker coworker = coworkerRepository.findCoworkerByIsActiveTrue();
        return coworker != null;
    }
}
