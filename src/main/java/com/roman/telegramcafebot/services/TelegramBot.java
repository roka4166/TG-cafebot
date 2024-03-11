package com.roman.telegramcafebot.services;

import com.roman.telegramcafebot.config.BotConfig;
import com.roman.telegramcafebot.models.*;
import com.roman.telegramcafebot.repositories.*;
import com.roman.telegramcafebot.utils.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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


    private KeyboardMarkup keyboardMarkup;

    private AdminPasswordRepository adminPasswordRepository;
    private final BotConfig botConfig;

    private CoworkerRepository coworkerRepository;

    private ButtonRepository buttonRepository;

    private OrderRepository orderRepository;

    private MenuItemRepository menuItemRepository;

    private CartRepository cartRepository;
    private ReservationRepository reservationRepository;




    @Autowired
    public TelegramBot(BotConfig botConfig, KeyboardMarkup keyboardMarkup,
                       AdminPasswordRepository adminPasswordRepository, ButtonRepository buttonRepository,
                       CoworkerRepository coworkerRepository, OrderRepository orderRepository,
                       MenuItemRepository menuItemRepository, CartRepository cartRepository,
                       ReservationRepository reservationRepository){
        this.botConfig = botConfig;
        this.keyboardMarkup = keyboardMarkup;
        this.adminPasswordRepository = adminPasswordRepository;
        this.buttonRepository = buttonRepository;
        this.coworkerRepository = coworkerRepository;
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.cartRepository = cartRepository;
        this.reservationRepository = reservationRepository;
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
                    case COMMAND_START -> startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    case COMMAND_ADMIN_OFF -> updateCoworkerActivity(chatId, false);
                    case COMMAND_ADMIN_ON -> updateCoworkerActivity(chatId, true);
                    case COMMAND_ADMIN -> processAdminCommand(chatId);
                    case COMMAND_DELETEME -> deleteCoworker(chatId);
                }
                if (messageText.startsWith(MessageCommand.COMMAND_PASSWORD.getMessageCommand())) {
                    validatePassword(chatId, messageText);
                }
                else if (messageText.startsWith(MessageCommand.COMMAND_NEW_ITEM.getMessageCommand())) {
                    processNewItemCommand(chatId, messageText);
                }
            }
                if(expectingNameForReservationMap.containsKey(chatId) && expectingNameForReservationMap.get(chatId)) {
                    processExpectingNameForReservation(chatId, messageText);
                }

                else if (expectingTimeForReservationMap.containsKey(chatId) && expectingTimeForReservationMap.get(chatId)) {
                    processExpectingTimeForReservation(chatId, messageText);
                }

                else if (expectingCommentFromCoworker.containsKey(getCoworkerChatId()) && expectingCommentFromCoworker.get(getCoworkerChatId())) {
                    processExpectingCommentFromCoworker(messageText);
                }
                else if(expectingTimeForPreorder.containsKey(chatId) && expectingTimeForPreorder.get(chatId)) {
                    processExpectingTimeForPreorder(chatId, messageText);
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
            createReservationAndAddAmountOfPeople(chatId, callbackData);
            String text = String.format("Вы выбрали кол-во человек %s, теперь нужно ввести имя на которое вы хотите " +
                    "забронировать столик.", amountOfPeople);
            sendMessage(chatId, text);
            expectingNameForReservationMap.put(chatId, true);
        }
        else if (callbackData.startsWith(CallbackDataCommand.REMOVEFROMCART.getCallbackDataCommand())){
            removeFromCart(chatId, callbackData);
            sendMessage(chatId, "Удалено из корзины", getGoToMenuButton());
        }
        else if (callbackData.contains("+") && callbackData.startsWith("ITEM")){
            sendMessage(chatId, createConfirmationTextWithAddition(getListFromMenuItemAndAddition(callbackData)),
                    keyboardMarkup.getKeyboardMarkup(getConfirmationButtons(callbackData), 2));
        }
        else if(callbackData.startsWith(CallbackDataCommand.ITEM.getCallbackDataCommand())){
            if(validateCoworker(chatId)){
                sendMessage(chatId, "Что сделать?",
                        keyboardMarkup.getKeyboardMarkup(getAdminItemButtons(callbackData), 2));
                return;
            }
            else if(isItemStoppedOrDeleted(callbackData)){
                sendMessage(chatId, "Товара больше нет в наличии", getGoToMenuButton());
                return;
            }
            sendMessage(chatId, createConfirmationText(findItemById(callbackData)),
                    keyboardMarkup.getKeyboardMarkup(getConfirmationButtons(callbackData), 2));
        }
        else if(callbackData.startsWith(CallbackDataCommand.ADDITIONITEM.getCallbackDataCommand())){
            if(validateCoworker(chatId)){
                sendMessage(chatId, "Что сделать?",
                        keyboardMarkup.getKeyboardMarkup(getAdminItemButtons(callbackData), 2));
                return;
            }
            else if(isItemStoppedOrDeleted(callbackData)){
                sendMessage(chatId, "Товара больше нет в наличии", getGoToMenuButton());
                return;
            }
            else if(checkIfCartIsEmpty(chatId)){
                sendMessage(chatId, "Сперва добавте блюдо в корзину.", getGoToMenuButton());
            }
            else {
                sendMessage(chatId, getAdditionMenuTextFromCart(getAllItemsInCartByChatId(chatId)),
                        keyboardMarkup.getAddAdditionToCartMenuItemKeyboardMarkup(getAllItemsInCartByChatId(chatId), callbackData));
            }
        }
        else if(callbackData.startsWith(CallbackDataCommand.STOPPEDITEM.getCallbackDataCommand())){
            if(unstopMenuItem(callbackData)){
                sendMessage(chatId, "Стоп снят успешно /admin",
                        keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons(), 3));
            }
            else {
                sendMessage(chatId, "Что то пошло не так, стоп не был снять",
                        keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons(), 3));
            }
        }
        else if(callbackData.startsWith(CallbackDataCommand.ADDTOCART.getCallbackDataCommand())){
            if(callbackData.contains("+")){
                addAdditionToMenuItemInCart(chatId, callbackData);
                return;
            }
            else if(doItemBelongToDrinkAdditionMenu(callbackData)){
                addDrinkAdditionToMenuItemInCart(chatId, callbackData);
                sendMessage(chatId,"Добавлено успешно", getGoToMenuButton());
                return;
            }
            addToCart(chatId, callbackData);
            String menuBelonging = getMenuBelonging(callbackData);
            if(menuBelonging.equals("coffemenu")){
                sendMessage(chatId, "Добавлено успешно! Хотите что то добавить в ваше кофе? \n" +
                                getMenuText(getItemsByMenuBelonging("coffeadditionmenu")),
                        keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("coffeadditionmenu")), 3));
            }
            else if(menuBelonging.equals("teamenu")){
                sendMessage(chatId, "Добавлено успешно! Хотите что то добавить в ваш чай? \n" +
                                getMenuText(getItemsByMenuBelonging("teaadditionmenu")),
                        keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("teaadditionmenu")), 3));
            }
            else
                sendMessage(chatId,"Добавлено успешно", getGoToMenuButton());
        }
        else if(callbackData.startsWith(CallbackDataCommand.RESERVATION_CONFIRMED.getCallbackDataCommand())){
           confirmReservation(chatId, callbackData);
        }
        else if(callbackData.startsWith(CallbackDataCommand.RESERVATION_DECLINED.getCallbackDataCommand())){
            synchronized (expectingCommentFromCoworker) {
                if (!expectingCommentFromCoworker.containsValue(true)) {
                    Reservation reservation = findReservationById(chatId, callbackData);
                    if(reservation.getConfirmedByCoworker() == null){
                        reservation.setCoworkerComment("expecting");
                        sendMessage(getCoworkerChatId(), "Введите причину отказа брони");
                        expectingCommentFromCoworker.put(getCoworkerChatId(), true);
                    }
                    else {
                        sendMessage(getCoworkerChatId(), "Ошибка вы уже изменили статус брони");
                    }
                }
            }
        }
        else if(callbackData.startsWith(CallbackDataCommand.DECLINE_RESERVATION.getCallbackDataCommand())){
            String reservationId = callbackData.substring(callbackData.lastIndexOf("N")+1);
            sendMessage(getCoworkerChatId(), "Подтвердить отказ брони",
                    keyboardMarkup.createTwoButtons("ДА", "RESERVATION_DECLINED"+reservationId, "НЕТ", "RESERVATIONDECISION"+reservationId));
        }
        else if(callbackData.startsWith(CallbackDataCommand.CONFIRM_RESERVATION.getCallbackDataCommand())){
            String reservationId = callbackData.substring(callbackData.lastIndexOf("N")+1);
            sendMessage(getCoworkerChatId(), "Подтвердить бронь",
                    keyboardMarkup.createTwoButtons("ДА", "RESERVATION_CONFIRMED"+reservationId, "НЕТ", "RESERVATIONDECISION"+reservationId));
        }
        else if(callbackData.startsWith(CallbackDataCommand.RESERVATIONDECISION.getCallbackDataCommand())){
            int reservationId = Integer.parseInt(callbackData.substring(callbackData.lastIndexOf("N")+1));
            Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
            if(reservation == null){
                sendMessage(chatId, "Что то пошло не так, бронь не была найдена в базе");
                return;
            }
            sendMessage(getCoworkerChatId(), reservation.toString(),
                    keyboardMarkup.getBookingConfirmationAdminMenu(getButtons("bookingconfirmationadminmenu"),
                            reservation));
        }

        else if (callbackData.startsWith(CallbackDataCommand.PAYMENTCONFIRMEDBYCOWORKER.getCallbackDataCommand())){
            Order order = orderRepository.findById(Integer.valueOf(callbackData.substring(26))).orElse(null);
            if (order == null) {
                sendMessage(chatId, "Ошибка, заказ не был найден в базе");
                return;
            } else if (order.getOrderConfirmed()) {
                sendMessage(getCoworkerChatId(), "Вы уже подтвердили заказ "+order.getId());
            }
            order.setOrderConfirmed(true);
            sendMessage(order.getChatId(), "Оплата была подтверждена работником. /start");
            sendMessage(getCoworkerChatId(), "Оплата заказа"+ order.getId() + "подтверждена");
        }
        else if (callbackData.startsWith(CallbackDataCommand.DELETE.getCallbackDataCommand())){
            try {
                deleteMenuItem(chatId, callbackData);
                sendMessage(chatId, "Товар удален", keyboardMarkup.getKeyboardMarkup(getButtons("adminmainmenu"), 2));
            }
            catch (Exception e){
                sendMessage(chatId, "что то пошло не так, товар не удален");
            }
        }
        else if (callbackData.startsWith(CallbackDataCommand.SETSTOP.getCallbackDataCommand())){
            try{
                stopMenuItem(callbackData);
                sendMessage(chatId, "Стоп на товар поставлен",
                        keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons(), 3));
            }
            catch (Exception e){
                sendMessage(chatId, "Что то пошло не так, стоп не поставлен",
                        keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons(), 3));
            }
        }

        if (callbackDataCommand != null) {
            switch (callbackDataCommand) {
                case START ->
                        sendMessage(chatId,"Главное меню",
                                keyboardMarkup.getKeyboardMarkup(getButtons("mainmenu"),  1));

                case RESERVATION -> {
                    String text = "Введите количество человек: ";
                    sendMessage(chatId, text,
                            keyboardMarkup.getKeyboardMarkup((getButtons("amountofpeoplemenu")), 4));
                }
                case CONFIRMBOOKING -> {
                    confirmBooking(chatId);
                }
                case CONFIRMCOMMENT -> {
                    Reservation reservation = findReservationByCommentExpectation();
                    if(reservation == null){
                        return;
                    }
                    String comment = reservation.getCoworkerComment().replace("expecting", "");
                    reservation.setCoworkerComment(comment);
                    reservation.setConfirmedByCoworker(false);
                    sendMessage(getCoworkerChatId(), "Бронь отклонена, информация об этом отправилась клиенту");
                    String text = String.format("Бронь отклонена. Комментарий от сотрудника: %s", reservation.getCoworkerComment());
                    sendMessage(reservation.getChatId(), text);
                    expectingCommentFromCoworker.put(getCoworkerChatId(), false);
                }
                case DECLINECOMMENT -> {
                    Reservation reservation = findReservationByCommentExpectation();
                    if(reservation == null){
                        return;
                    }
                    sendMessage(getCoworkerChatId(), "Введите причину отказа снова");
                }
                case CONFIRMPREORDERTIME -> {
                    Order order = createOrder(chatId);
                    String text = String.format("Ваш заказ номер %d. Укажите этот номер при оплате. " +
                            "Для оплаты нужно перевести %d рублей на карту 1234 3456 2345 4556" +
                            " или на телефон 9485749284. Далее нажмите на кнопку 'Подтвердить'," +
                            " и после этого информация попадет к нашему сотруднику.", order.getId(), order.getTotalPrice());
                    sendMessage(chatId, text, keyboardMarkup.createOneButton("Подтвердить", "PAYMENTCONFIRMEDBYCUSTOMER"));
                    expectingTimeForPreorder.put(chatId, false);
                }
                case DECLINEPREORDERTIME -> {
                    sendMessage(chatId, "Вернуться в меню", getGoToMenuButton());
                    expectingTimeForPreorder.put(chatId, false);
                }
                case FOODMENU ->{
                    if(validateCoworker(chatId)){
                        sendMessage(chatId, "Menu", keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons(), 3));
                        return;
                    }
                    sendMessage(chatId, "Menu", keyboardMarkup.getKeyboardMarkup(getButtons("foodmenu"), 3));
                }

                case ADMINMAINMENU -> sendMessage(chatId, "Меню", keyboardMarkup.getKeyboardMarkup(getButtons("adminmainmenu"), 3));

                case ADMINCOFFEADDITION -> sendMessage(chatId, getMenuText(getItemsByMenuBelonging("coffeadditionmenu")),
                        keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("coffeadditionmenu")), 3));

                case ADMINTEAADDITION -> sendMessage(chatId, getMenuText(getItemsByMenuBelonging("teaadditionmenu")),
                        keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("teaadditionmenu")), 3));

                case BREAKFASTS ->
                        sendMessage(chatId, getMenuText(getItemsByMenuBelonging("breakfastmenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("breakfastmenu")), 3));

                case CROISSANTS ->
                        sendMessage(chatId, getMenuText(getItemsByMenuBelonging("croissantmenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("croissantmenu")), 3));

                case ROMANPIZZAS ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("romanpizzamenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("romanpizzamenu")), 3));

                case HOTFOOD ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("hotfoodmenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("hotfoodmenu")), 3));

                case PIESCHUDU ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("pieschudumenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("pieschudumenu")), 1));

                case SOUPS ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("soupmenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("soupmenu")), 3));

                case SALADS ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("saladmenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("saladmenu")), 3));

                case SANDWICHES ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("sandwichmenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("sandwichmenu")), 4));

                case BRUSCHETTAS ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("bruschettamenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("bruschettamenu")), 3));

                case ADDITIONMENU ->
                        sendMessage(chatId,getAdditionMenuText(getItemsByMenuBelonging("additionmenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("additionmenu")), 3));

                case DRINKS ->
                        sendMessage(chatId,"Напитки",
                                keyboardMarkup.getKeyboardMarkup(getButtons("drinkmenu"), 2));

                case DESERT ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("desertmenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("desertmenu")), 3));
                case PUFFPASTRY ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("puffpastrymenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("puffpastrymenu")), 3));

                case BREAD ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("breadmenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("breadmenu")), 3));

                case TEA ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("teamenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("teamenu")), 3));

                case COFFEE ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("coffemenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("coffemenu")), 3));

                case CACAO ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("cacaomenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("cacaomenu")),3 ));

                case SIGNATUREDRINKS ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("signaturedrinksmenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("signaturedrinksmenu")), 3));

                case NOTTEA ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("notteamenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("notteamenu")), 3));

                case DRINKSADDITION ->
                        sendMessage(chatId,getMenuText(getItemsByMenuBelonging("drinksadditionmenu")),
                                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("drinksadditionmenu")), 3));

                case GOTOPAYMENT -> {
                    if(checkIfCartIsEmpty(chatId)){
                        sendMessage(chatId, "Ваша корзина пуста", getGoToMenuButton());
                        return;
                    }
                    expectingTimeForPreorder.put(chatId, true);
                    sendMessage(chatId,"Введите время в которое вы хотите забрать заказ");
                }
                case PAYMENTCONFIRMEDBYCUSTOMER -> {
                    Order order = orderRepository.findTopByChatIdOrderByIdDesc(chatId);
                    if(isOrderSentToCoworker(order)){
                        sendMessage(chatId, "Информация о заказе уже была отправлена сотруднику /start");
                        return;
                    }
                    order.setOrderSentToCoworker(true);
                    sendMessage(chatId, String.format("Благодарим за покупку. Ваш заказ номер %d был принят." +
                            " Укажите этот номер когда будете забирать заказ. /start", order.getId()));
                    Long coworkerChatId = getCoworkerChatId();
                    sendMessage(coworkerChatId, String.format("Заказ номер: %d на сумму %d руб. Содержит %s " +
                                    "Нажмите подтвердить что бы подтвердить оплату",
                                    order.getId(), order.getTotalPrice(), order.getItems()),
                                    keyboardMarkup.createOneButton("Подтвердить оплату", "PAYMENTCONFIRMEDBYCOWORKER"+order.getId()));
                }
                case SHOWCART -> {
                    if(checkIfCartIsEmpty(chatId)){
                        sendMessage(chatId, "Корзина пуста", getGoToMenuButton());
                    }
                    else {
                        sendMessage(chatId, getMenuTextForCart(getAllItemsInCartByChatId(chatId)),
                                keyboardMarkup.getCartKeyBoardMarkup(getAllItemsInCartByChatId(chatId)));
                    }
                }
                case REMOVEALLFROMCART -> {
                    removeAllFromCart(chatId);
                    sendMessage(chatId, "Корзина очищина", getGoToMenuButton());
                }
                case ADMINFOODMENU -> sendMessage(chatId, "Меню",
                        keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons(), 3));
                case SHOWALLSTOPPED ->
                        showAllStoppedItems(chatId);
            }
        }
    }

    private void stopMenuItem(String callbackData) {
        int indexOfCharBeforeItemId = callbackData.indexOf("M");
        String originalCallbackData = callbackData.substring(7);
        int menuItemId = Integer.parseInt(callbackData.substring(indexOfCharBeforeItemId+1));
        MenuItem menuItem = menuItemRepository.findById(menuItemId).orElse(null);
        if(menuItem != null){
            String belongsToMenu = menuItem.getBelongsToMenu();
            menuItem.setStopped(true);
            menuItem.setBelongsToMenu("STOPPED"+menuItem.getBelongsToMenu());
            menuItemRepository.save(menuItem);
            Button button = buttonRepository.findButtonByCallbackData(originalCallbackData);
            button.setBelongsToMenu("STOPPED"+belongsToMenu);
            button.setCallbackData("STOPPED"+originalCallbackData);
            buttonRepository.save(button);
        }
    }
    private void processNewItemCommand(Long chatId, String messageText){
        if (validateCoworker(chatId)) {
            try {
                createMenuItem(messageText);
                sendMessage(getCoworkerChatId(), "К какому разделу относиться?",
                        keyboardMarkup.getKeyboardMarkup(getButtons("adminmenu"), 3));
            }
            catch (Exception e) {
                sendMessage(chatId,"Ошибка! Неправильный формат ввода");
            }
        } else sendMessage(chatId, "Не добавлено в корзину, у вас нет доступа к этой функции");
    }
    private void processExpectingNameForReservation(Long chatId, String messageText){
        Reservation reservation = findReservationByChatId(chatId);
        if (reservation == null) {
            sendMessage(chatId, "Что то пошло нет так");
            return;
        }
        reservation.setName(messageText);
        sendMessage(chatId, "Теперь введите время");
        expectingNameForReservationMap.put(chatId, false);
        expectingTimeForReservationMap.put(chatId, true);
    }
    private void processExpectingTimeForReservation(Long chatId, String messageText){
        Reservation reservation = findReservationByChatId(chatId);
        if (reservation == null) {
            sendMessage(chatId, "Ошибка, что то пошло нет так");
            return;
        }
        reservation.setTime(messageText);
        sendMessage(chatId, getBookingConfirmationTextByChatId(chatId),
                keyboardMarkup.getKeyboardMarkup(getButtons("bookingconfirmationmenu"), 2));
        expectingTimeForReservationMap.put(chatId, false);
    }
    private void processExpectingCommentFromCoworker(String messageText){
        Reservation reservation = findReservationByCommentExpectation();
        reservation.setCoworkerComment("expecting"+messageText);
        sendMessage(getCoworkerChatId(), "Подтвердить следующий текст?: "+messageText,
                keyboardMarkup.createTwoButtons("ДА", "CONFIRMCOMMENT", "НЕТ", "DECLINECOMMENT"));
    }
    private void processExpectingTimeForPreorder(Long chatId, String messageText){
        orderTime.put(chatId, messageText);
        sendMessage(chatId,"Подтвердить время " + messageText + " ?", keyboardMarkup.getKeyboardMarkup(getButtons("preordertimemenu"), 2));
    }
    private boolean unstopMenuItem(String callbackData) {
        int indexOfCharBeforeItemId = callbackData.indexOf("M");
        String originalCallbackData = callbackData.substring(6);
        int menuItemId = Integer.parseInt(callbackData.substring(indexOfCharBeforeItemId+1));
        MenuItem menuItem = menuItemRepository.findById(menuItemId).orElse(null);
        if(menuItem != null){
            menuItem.setStopped(false);
            String belongsToMenu = menuItem.getBelongsToMenu().replace("STOPPED", "");
            menuItem.setBelongsToMenu(belongsToMenu);
            menuItemRepository.save(menuItem);

            Button button = buttonRepository.findButtonByCallbackData("STOPPEDITEM"+menuItem.getId());
            button.setBelongsToMenu(belongsToMenu);
            button.setCallbackData(originalCallbackData);
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
            if(active){
                sendMessage(chatId, "Уведомления включены");
            }
            else {
                sendMessage(chatId, "Уведомления отключены");
            }
        }
    }

    private void showAllStoppedItems(long chatId) {
        List<MenuItem> stoppedItems = menuItemRepository.findAllByIsStoppedTrue();
        if(stoppedItems.isEmpty()){
            sendMessage(chatId, "Товаров в стопе нет");
            return;
        }
        sendMessage(chatId, getMenuTextForStoppedItems(stoppedItems),
                keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("STOPPED")), 3));
    }
    private void processAdminCommand(Long chatId){
        if (validateCoworker(chatId)) {
            sendMessage(chatId, "Меню", keyboardMarkup.getKeyboardMarkup(getButtons("adminmainmenu"), 3));
        } else {
            sendMessage(chatId, "Нет доступа к функции");
        }
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
    private InlineKeyboardMarkup getGoToMenuButton(){
        return keyboardMarkup.createOneButton("Перейти в меню", "FOODMENU");
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
        Coworker coworker = coworkerRepository.findTopByIsActiveTrue();
        if(coworker != null){
            return coworker.getChatId();
        }
        return null;
    }
    private void confirmBooking(Long chatId){
        Reservation reservation = findReservationByChatId(chatId);
        if(reservation.getConfirmedByCoworker()){
            sendMessage(chatId, "Вы уже отправили информацию о броне стола");
            return;
        }
        sendMessage(chatId, "Бронь стола отправлена нашему сотруднику," +
                "   если на это время есть столик, " +
                "то вы получите подтверждения что стол был забронирован успешно.");
        sendMessage(getCoworkerChatId(), reservation.toString(),
                keyboardMarkup.getBookingConfirmationAdminMenu(getButtons("bookingconfirmationadminmenu"),
                        findReservationByChatId(chatId)));
    }
    private String getMenuText(List<MenuItem> items){
        List<Button> buttons = buttonRepository.findAllByBelongsToMenu(items.get(0).getBelongsToMenu());
        StringBuilder menuText = new StringBuilder();
        int i = 0;
        for (Button button : buttons) {
            if(button.getCallbackData().equals("FOODMENU")){
                continue;
            }
            button.setName(String.valueOf(i+1));
            button.setCallbackData("ITEM"+items.get(i).getId());
            menuText.append(i+1).append(". ").append(items.get(i).getName()).append(" ").append(items.get(i).getPrice()).append(" руб.").append("\n");
            i += 1;
        }
        return menuText.toString();
    }
    private String getAdditionMenuText(List<MenuItem> items){
        List<Button> buttons = buttonRepository.findAllByBelongsToMenu(items.get(0).getBelongsToMenu());
        StringBuilder menuText = new StringBuilder();
        int i = 0;
        for (Button button : buttons) {
            if(button.getCallbackData().equals("FOODMENU")){
                continue;
            }
            button.setName(String.valueOf(i+1));
            button.setCallbackData("ADDITIONITEM"+items.get(i).getId());
            menuText.append(i+1).append(". ").append(items.get(i).getName()).append(" ").append(items.get(i).getPrice()).append(" руб.").append("\n");
            i += 1;
        }
        return menuText.toString();
    }
    @Scheduled(fixedRate = 1800000)
    void cleanCartIfExpired(){
        List<Cart> allItemsInAllCarts = cartRepository.findAll();
        if(allItemsInAllCarts.isEmpty()){
            return;
        }
        LocalDateTime time = LocalDateTime.now();
        for (Cart itemInCart : allItemsInAllCarts){
            if (itemInCart.getExpirationDate().isBefore(time)){
                cartRepository.delete(itemInCart);
            }
        }
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
    private List<MenuItem> getListFromMenuItemAndAddition(String callbackData){
        String[] menuItemAndAdditions = callbackData.replace("ITEM", "").split("\\+");
        List<MenuItem> items = new ArrayList<>();
        for (String menuItemId : menuItemAndAdditions){
            items.add(menuItemRepository.findById(Integer.valueOf(menuItemId)).orElse(null));
        }
        return items;
    }

    private String getMenuTextForCart(List<Cart> itemsInCart){
        StringBuilder menuText = new StringBuilder();
        String cartMenuText = """
                Что бы удалить предмет из корзины, просто кликните по номеру который соответствует товару

                """;
        menuText.append(cartMenuText);
        int sum = 0;
        for (int i = 0; i < itemsInCart.size(); i++) {
            menuText.append(i+1).append(". ").append(itemsInCart.get(i).getItemsName()).append(" ")
                    .append((itemsInCart.get(i).getPrice())).append(" руб. ")
                    .append("\n");
            sum += (itemsInCart.get(i).getPrice());
        }
        String infoAboutSum = String.format("\nСумма корзины %d", sum);
        menuText.append(infoAboutSum);
        return menuText.toString();
    }
    private String getAdditionMenuTextFromCart(List<Cart> itemsInCart){
        StringBuilder menuText = new StringBuilder();
        String cartMenuText = """
                Выберите блюдо куда вы хотите добавить добавку.

                """;
        menuText.append(cartMenuText);
        for (int i = 0; i < itemsInCart.size(); i++) {
            menuText.append(i+1).append(". ").append(itemsInCart.get(i).getItemsName())
                    .append("\n");

        }
        return menuText.toString();
    }
    private List<Button> sortButtonsByName(List<Button> buttonsToSort){
        buttonsToSort.sort(Comparator.comparing(button -> {
            try {
                return Integer.parseInt(button.getName());
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }));
        return buttonsToSort;
    }
    private List<MenuItem> getItemsByMenuBelonging(String belongsToMenu){
        return menuItemRepository.findAllByBelongsToMenuAndIsStoppedFalse(belongsToMenu);
    }
    private List<Button> getButtons (String typeOfMenu){
        return buttonRepository.findAllByBelongsToMenuStartingWith(typeOfMenu);
    }
    private List<Button> getAdminFoodMenuButtons (){
        List<Button> adminButtons = new ArrayList<>();
        adminButtons.addAll(buttonRepository.findAllByBelongsToMenu("foodmenu"));
        adminButtons.addAll(buttonRepository.findAllByBelongsToMenu("drinksadditionmenu"));
        adminButtons.add(buttonRepository.findButtonByCallbackData("ADMINMAINMENU"));
        return adminButtons;
    }
    private List<Button> getConfirmationButtons (String callbackData){
        List<Button> buttons = buttonRepository.findAllByBelongsToMenu("confirmationmenu");
        for (Button button1 : buttons){
            if(button1.getCallbackData().startsWith("ADDTOCART")){
                button1.setCallbackData("ADDTOCART"+callbackData);
            }
        }
        return buttons;
    }
    private void validatePassword (Long chatId, String messageText){
        String password = messageText.substring(10);
        AdminPassword value = adminPasswordRepository.findTopByPassword(password);
        if (value!=null) {
            Coworker coworker = new Coworker();
            coworker.setChatId(chatId);
            coworker.setActive(true);
            coworkerRepository.save(coworker);
            sendMessage(chatId, "Бот активен");
        }
        else {
            sendMessage(chatId, "Пароль неверный");
        }
    }

    private void createReservationAndAddAmountOfPeople(Long chatId, String amountOfPeople){
        Reservation reservation = new Reservation();
        reservation.setChatId(chatId);
        reservation.setAmountOfPeople(amountOfPeople.substring(6));
        reservation.setConfirmedByCoworker(false);
        reservationRepository.save(reservation);
    }

    private boolean isAnyCoworkerActive(){
        coworkerRepository.findTopByIsActiveTrue();
        return coworkerRepository.findTopByIsActiveTrue() != null;
    }

    private List<Button> getAdminItemButtons (String callbackDataWithId){
        List<Button> buttons = buttonRepository.findAllByBelongsToMenu("adminitemmenu");
        for (Button button1 : buttons){
            String oldCallbackData = button1.getCallbackData();
            if(oldCallbackData.startsWith("DELETE")){
                button1.setCallbackData("DELETE"+callbackDataWithId);
            }
            else {
                button1.setCallbackData("SETSTOP"+callbackDataWithId);
            }
        }
        return buttons;
    }
    private String createConfirmationText(MenuItem item){
        return String.format("Вы выбрали %s. Цена %d руб.", item.getName(), item.getPrice());
    }
    private String createConfirmationTextWithAddition(List<MenuItem> menuItems){
        StringBuilder text = new StringBuilder("Вы выбрали ( ");
        int totalPrice = 0;
        for (int i = 0; i < menuItems.size(); i++) {
            if(i == menuItems.size()-1){
                text.append(menuItems.get(i).getName()).append(" )");
                totalPrice += menuItems.get(i).getPrice();
                continue;
            }
            totalPrice += menuItems.get(i).getPrice();
            text.append(menuItems.get(i).getName()).append(" + ");
        }
        text.append(". Цена ").append(totalPrice).append(" руб. ");
        return text.toString();
    }
    private MenuItem findItemById(String callbackData){
        return menuItemRepository.findById(Integer.valueOf(callbackData.substring(4))).orElse(null);
    }
    private void confirmReservation(Long chatId, String callbackData){
        try {
            Reservation reservation = findReservationById(chatId, callbackData);
            if(reservation.getConfirmedByCoworker() == null){
                reservation.setConfirmedByCoworker(true);
                String confirmationText = String.format("Бронь стола на имя %s подтверждена. Время %s. Кол-во человек %s",
                        reservation.getName(), reservation.getTime(), reservation.getAmountOfPeople());
                sendMessage(getCoworkerChatId(), confirmationText);
                sendMessage(reservation.getChatId(), confirmationText);
            }
            else{
                sendMessage(getCoworkerChatId(), "Ошибка: вы уже изменили статус брони");
            }
        }
        catch (Exception e){
            sendMessage(getCoworkerChatId(), "Нет брони");
        }
    }
    private void addToCart(Long chatId, String callbackData){
        String menuItemId = callbackData.substring(9);
        MenuItem item = findItemById(menuItemId);
        Cart cart = new Cart();
        cart.setChatId(chatId);
        cart.setItemsId(menuItemId);
        cart.setExpirationDate(LocalDateTime.now().plusHours(20));
        cart.setPrice(item.getPrice());
        cart.setItemsName(item.getName());
        cart.setBelongsToMenu(item.getBelongsToMenu());
        cartRepository.save(cart);
    }
    private void addAdditionToMenuItemInCart(Long chatId, String callbackData){
        String itemIdInCart = callbackData.replace("ADDTOCART", "");
        int index = itemIdInCart.lastIndexOf("+");
        String itemsIdinCart = itemIdInCart.substring(0,index);
        int additionId = Integer.parseInt(itemIdInCart.substring(index+1));
        Cart itemInCart = cartRepository.findTopByChatIdAndItemsIdOrderByIdDesc(chatId, itemsIdinCart);
        if(itemInCart == null){
            sendMessage(chatId, "Ошибка! Добавка уже добавлена к блюду," +
                    " если хотите добавить второй раз, перейдите в меню и добавьте еще раз", getGoToMenuButton());
            return;
        }
        MenuItem addition = menuItemRepository.findById(additionId).orElse(null);
        if (addition == null) {
            sendMessage(chatId, "Что то пошло не так, не получилось добавить добавку");
            return;
        }
        itemInCart.setPrice(itemInCart.getPrice()+addition.getPrice());
        itemInCart.setItemsName(itemInCart.getItemsName() + "+" + addition.getName());
        itemInCart.setItemsId(itemInCart.getItemsId()+"+"+addition.getId());
        sendMessage(chatId,"Добавлено успешно", getGoToMenuButton());
    }
    private void addDrinkAdditionToMenuItemInCart(Long chatId, String callbackData) {
        int itemsId = Integer.parseInt(callbackData.substring(13));
        MenuItem item = menuItemRepository.findById(itemsId).orElse(null);
        List<Cart> itemsInCart = cartRepository.findAllByChatIdOrderByIdDesc(chatId);

        for (Cart itemInCart : itemsInCart) {
            if (item != null && doesDrinkAdditionBelongToMenuItemInCart(itemInCart, item)) {
                cartRepository.findById(itemInCart.getId());
                itemInCart.setPrice(itemInCart.getPrice() + item.getPrice());
                itemInCart.setItemsName(itemInCart.getItemsName() + "+" + item.getName());
                itemInCart.setItemsId(itemInCart.getItemsId() + "+" + item.getId());
                cartRepository.save(itemInCart);
                return;
            }
        }
    }
    private boolean doesDrinkAdditionBelongToMenuItemInCart(Cart itemInCart, MenuItem drinkAddition){
        String menuBelongingForCartItem = itemInCart.getBelongsToMenu().substring(0,2);
        String menuBelongingForAddition = drinkAddition.getBelongsToMenu().substring(0,2);
        return menuBelongingForAddition.equals(menuBelongingForCartItem);
    }
    private boolean doItemBelongToDrinkAdditionMenu(String callbackData){
        Button button = buttonRepository.findButtonByCallbackData(callbackData.substring(9));
        return button.getBelongsToMenu().equals("coffeadditionmenu") || button.getBelongsToMenu().equals("teaadditionmenu");
    }
    private boolean isOrderSentToCoworker(Order order){
        return order.getOrderSentToCoworker();
    }
    private Order createOrder(Long chatId){
        Order order = new Order();
        order.setChatId(chatId);
        order.setTotalPrice(getTotalPriceOfCartByChatId(chatId));
        List<Cart> itemsInCart = getAllItemsInCartByChatId(chatId);
        String itemsAsString = getOrderString(itemsInCart);
        order.setItems(itemsAsString);
        order.setTime(findOrderTimeByChatId(chatId));
        order.setOrderSentToCoworker(false);
        orderRepository.save(order);
        orderTime.remove(chatId);
        removeAllFromCart(chatId);
        return order;
    }
    private String getOrderString(List<Cart> itemsInCart){
        StringBuilder itemsAsString = new StringBuilder();
        for (int i = 0; i < itemsInCart.size(); i++) {
            if(i == itemsInCart.size()-1){
                itemsAsString.append(itemsInCart.get(i).getItemsName()).append(". ");
                continue;
            }
            itemsAsString.append(itemsInCart.get(i).getItemsName()).append(", ");
        }

        return itemsAsString.toString();
    }
    private String findOrderTimeByChatId(Long chatId){
        if(orderTime.containsKey(chatId)){
            return orderTime.get(chatId);
        }
        return null;
    }
    private boolean isItemStoppedOrDeleted(String callbackData){
        int indexOfCharacterBeforeItemId = callbackData.indexOf("M");
        int itemId = Integer.parseInt(callbackData.substring(indexOfCharacterBeforeItemId+1));
        MenuItem item = menuItemRepository.findById(itemId).orElse(null);
        if(item == null){
            return true;
        }
        return item.getStopped();
    }
    private void deleteMenuItem(Long chatId, String callbackData){
        int indexOfCharBeforeItemId = callbackData.indexOf("M");
        int menuItemId = Integer.parseInt(callbackData.substring(indexOfCharBeforeItemId+1));
        String buttonCallbackdata = callbackData.substring(6);
        MenuItem menuItemForRemoval = menuItemRepository.findById(menuItemId).orElse(null);
        Button buttonForRemoval = buttonRepository.findButtonByCallbackData(buttonCallbackdata);
        buttonRepository.delete(buttonForRemoval);
        if (menuItemForRemoval == null) {
            sendMessage(chatId, "Ошибка, товар не удален");
            return;
        }
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
        String itemId = callbackData.substring(14);
        List<Cart> items = cartRepository.findByChatIdAndAndItemsId(chatId, itemId);
        if(items.size() >= 2){
            cartRepository.delete(items.get(0));
            return;
        }
        cartRepository.deleteByChatIdAndItemsId(chatId, itemId);
    }
    private List<Cart> getAllItemsInCartByChatId(Long chatId){
        return cartRepository.findAllByChatId(chatId);
    }

    private int getTotalPriceOfCartByChatId(Long chatId){
        int totalPrice = 0;
        List<Cart> cart = getAllItemsInCartByChatId(chatId);
        for (Cart cart1 : cart){
            totalPrice += cart1.getPrice();
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
        if (reservation == null) {
            sendMessage(chatId, "Что то пошло не так");
            return "";
        }
        return String.format("Подтвердить бронь на имя %s. На время %s?", reservation.getName(), reservation.getTime());
    }

    private Reservation findReservationByChatId(Long chatId){
        Reservation reservation = reservationRepository.findTopByChatIdOrderByIdDesc(chatId);
        if(reservation == null){
            sendMessage(chatId, "Ошибка! Бронь не была найдена");
            return null;
        }
        return reservation;
    }
    private Reservation findReservationById(Long chatId, String callbackData){
        int id = Integer.parseInt(callbackData.substring(callbackData.lastIndexOf("D")+1));
        Reservation reservation = reservationRepository.findById(id).orElse(null);
        if(reservation == null){
            sendMessage(chatId, "Что то пошло не так. Бронь не была найдена в базе");
            return null;
        }
        return reservation;
    }
    private Reservation findReservationByCommentExpectation(){
        Reservation reservation = reservationRepository.findReservationByCoworkerCommentContaining("expecting");
        if(reservation == null){
            sendMessage(getCoworkerChatId(), "Нет брони которая ожидает причину отказа");
            return null;
        }
        return reservation;
    }

    private String getMenuBelonging(String callbackData){
        int itemId = Integer.parseInt(callbackData.substring(13));
        MenuItem item = menuItemRepository.findById(itemId).orElse(null);
        if (item == null) {
            System.out.println("Could not find menu item by id");
            return "";
        }
        return item.getBelongsToMenu();
    }
    private void deleteCoworker(Long chatId){
        coworkerRepository.delete(coworkerRepository.findCoworkerByChatId(chatId));
        sendMessage(chatId, "Вы были удалены как админ");
    }
}





