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
    private Map<Long, Boolean> expectingCommentFromCoworker = new HashMap<>();

    private Map<Long, Integer> expectingEditDescription = new HashMap<>();

    private Map<Long, Boolean> expectingDescriptionFromCoworker = new HashMap<>();

    private List<Map<Long,Boolean>> expectingList = List.of(expectingNameForReservationMap,
            expectingTimeForReservationMap, expectingTimeForPreorder,
            expectingCommentFromCoworker, expectingDescriptionFromCoworker);

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
                ifExpectingTextSetExpectingTextToFalse(chatId, expectingList);
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
                else if (messageText.startsWith(MessageCommand.COMMAND_NEWSECTION.getMessageCommand())) {
                    createNewSection(chatId, messageText);
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
                else if(expectingDescriptionFromCoworker.containsKey(chatId) && expectingDescriptionFromCoworker.get(chatId)) {
                    processDescriptionForMenuItem(chatId, messageText);
                }
                else if(expectingEditDescription.containsKey(chatId) && expectingEditDescription.get(chatId)!=null) {
                    processNewDescription(chatId, expectingEditDescription.get(chatId), messageText);
                }
        }

    private void handleCallbackQuery(Update update){
        String callbackData = update.getCallbackQuery().getData();
        CallbackDataCommand callbackDataCommand = CallbackDataCommand.fromCallbackData(callbackData);
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (callbackData.startsWith(CallbackDataCommand.TYPE.getCallbackDataCommand())){
            createButtonFromMenuItem(chatId, setMenuItemsMenuBelonging(chatId, callbackData));
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
        else if(callbackData.startsWith(CallbackDataCommand.EDITDESCRIPTION.getCallbackDataCommand())){
            editDescription(chatId, callbackData);

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
        else if(callbackData.startsWith(CallbackDataCommand.DELETERESERVATION.getCallbackDataCommand())){
            sendMessage(chatId, "Подтвердить удаление брони",
                    keyboardMarkup.createTwoButtons("ДА", "CONFIRM"+callbackData, "НЕТ", "SHOWRESERVATIONS"));
        }
        else if(callbackData.startsWith(CallbackDataCommand.CONFIRMDELETERESERVATION.getCallbackDataCommand())){
            int reservationId = Integer.parseInt(callbackData.substring(callbackData.lastIndexOf("N")+1));
            Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
            reservationRepository.delete(reservation);
            sendMessage(chatId, "Бронь удалена", keyboardMarkup.createOneButton("В главное меню", "MAINMENU"));
            sendMessage(getCoworkerChatId(),
                            String.format("Бронь на имя %s. Время %s. Кол-во человек %s. Была удалена",
                            reservation.getName(), reservation.getTime(), reservation.getAmountOfPeople()));
        }
        else if(callbackData.startsWith(CallbackDataCommand.SHOWDESCRIPTION.getCallbackDataCommand())){
                showDescription(chatId, callbackData);
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
                                getMenuText(chatId, getItemsByMenuBelonging("coffeadditionmenu")),
                        keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("coffeadditionmenu")), 3));
            }
            else if(menuBelonging.equals("teamenu")){
                sendMessage(chatId, "Добавлено успешно! Хотите что то добавить в ваш чай? \n" +
                                getMenuText(chatId, getItemsByMenuBelonging("teaadditionmenu")),
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

        else if (callbackData.startsWith(CallbackDataCommand.DELETE.getCallbackDataCommand())){
            try {
                deleteMenuItem(chatId, callbackData);
                sendMessage(chatId, "Товар удален", keyboardMarkup.getKeyboardMarkup(getButtons("adminmainmenu"), 2));
            }
            catch (Exception e){
                sendMessage(chatId, "Товар уже был удален");
            }
        }
        else if (callbackData.startsWith(CallbackDataCommand.SETSTOP.getCallbackDataCommand())){
            try{
                stopMenuItem(callbackData);
                sendMessage(chatId, "Стоп на товар поставлен",
                        keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons(), 3));
            }
            catch (Exception e){
                sendMessage(chatId, "Стоп на товар уже был поставлен кем то другим",
                        keyboardMarkup.getKeyboardMarkup(getAdminFoodMenuButtons(), 3));
            }
        }
        else if (callbackData.startsWith(CallbackDataCommand.TAKEAWAY.getCallbackDataCommand())) {
            String takeaway = callbackData.substring(8);
            Order order = createOrder(chatId, takeaway);
            if(order.getTime() == null){
                sendMessage(chatId, "Вы уже отправили информацию о заказе");
                orderRepository.delete(order);
                return;
            }
            String text = String.format("Ваш заказ номер %d был принят. Укажите этот номер когда будете платить за заказ. ",order.getId());
            sendMessage(getCoworkerChatId(),
                    String.format("Заказ номер: %d на сумму %d руб. Время: %s Содержит: %s ",
                            order.getId(), order.getTotalPrice(),order.getTime(), order.getItems() + takeawayStringForCoworkers(order.getTakeaway())));
            sendMessage(chatId, text, getGoToMenuButton());
            expectingTimeForPreorder.put(chatId, false);
        }
        else if (callbackData.startsWith(CallbackDataCommand.SHOWALLITEMSIN.getCallbackDataCommand())) {
            String typeOfMenu = callbackData.replace("SHOWALLITEMSIN", "").toLowerCase();
            if(typeOfMenu.equals("drink")){
                sendMessage(chatId,"Напитки",
                          keyboardMarkup.getKeyboardMarkup(getButtons("drinkmenu"), 2));
                return;
            }
            sendMessage(chatId, getMenuText(chatId ,getItemsByMenuBelonging(typeOfMenu+"menu")),
                    keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons(typeOfMenu+"menu")), 3));
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
                case MAINMENU -> processMainMenuCallbackData(chatId);
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
                case CONFIRMDESCRIPTION -> confirmDescription(chatId);
                case DECLINEDESCRIPTION -> declineDescription(chatId);
                case CONFIRMPREORDERTIME -> {
                    sendMessage(chatId, "В кафе или с собой",
                            keyboardMarkup.createTwoButtons("С собой", "TAKEAWAYTRUE", "В кафе", "TAKEAWAYFALSE"));
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
                    sendMessage(chatId, "Menu", keyboardMarkup.getKeyboardMarkup(sortFoodMenuButtons(getButtons("foodmenu")), 3));
                }
                case SHOWRESERVATIONS -> {
                    processShowReservations(chatId);
                }

                case ADMINMAINMENU -> sendMessage(chatId, "Меню", keyboardMarkup.getKeyboardMarkup(getButtons("adminmainmenu"), 3));

                case ADMINCOFFEADDITION -> sendMessage(chatId, getMenuText(chatId ,getItemsByMenuBelonging("coffeadditionmenu")),
                        keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("coffeadditionmenu")), 3));

                case ADMINTEAADDITION -> sendMessage(chatId, getMenuText(chatId, getItemsByMenuBelonging("teaadditionmenu")),
                        keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("teaadditionmenu")), 3));

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
                    sendMessage(getCoworkerChatId(),
                            String.format("Заказ номер: %d на сумму %d руб. " +takeawayStringForCoworkers(order.getTakeaway())+ ". Содержит %s " +
                                    order.getId(), order.getTotalPrice(), order.getItems()));
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
                createMenuItem(chatId, messageText);
                sendMessage(chatId, "К какому разделу относиться?",
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
    private void processNewDescription(Long chatId, Integer itemId, String messageText){
        MenuItem item = menuItemRepository.findById(itemId).orElse(null);
        item.setDescription(messageText);
        sendMessage(chatId, "Описание изменено");
        expectingEditDescription.put(chatId, null);
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
                if(checkIfTwoCoworkersAreActiveAtTheSameTime()){
                    sendMessageToAllActiveCoworkers();
                }
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
    private void processShowReservations(Long chatId){
        List<Reservation> reservations = reservationRepository.findAllByChatIdAndExpirationDateAfterAndConfirmedByCoworkerTrueOrderById(chatId, LocalDateTime.now());
        if(reservations.isEmpty()){
            sendMessage(chatId, "Нет броней", keyboardMarkup.createOneButton("В главное меню", "MAINMENU"));
            return;
        }
        sendMessage(chatId, getShowReservationText(chatId), keyboardMarkup.getShowReservationsButtons(chatId, reservations));

    }
    private void showDescription(Long chatId, String callbackData){
        int menuItemId = Integer.parseInt(callbackData.substring(callbackData.lastIndexOf("M")+1));
        MenuItem menuItem = menuItemRepository.findById(menuItemId).orElse(null);
        if(menuItem == null){
            sendMessage(chatId, "Товара больше нет в наличии");
            return;
        }
        sendMessage(chatId, menuItem.getDescription(),
                keyboardMarkup.createOneButton("Назад", "ITEM"+menuItemId));
    }
    private String getShowReservationText(Long chatId){
        List<Reservation> reservations = reservationRepository.findAllByChatIdAndExpirationDateAfterAndConfirmedByCoworkerTrueOrderById(chatId, LocalDateTime.now());
        StringBuilder reservationText = new StringBuilder("Что бы удалить бронь просто кликните по номеру который соответсвует брони. \n");
        for (int i = 0; i < reservations.size(); i++) {
            reservationText.append(i+1);
            String reservationInfo = String.format(". Бронь на имя %s. Время %s", reservations.get(i).getName(), reservations.get(i).getTime());
            reservationText.append(reservationInfo).append("\n");
        }
        return reservationText.toString();
    }
    private void processMainMenuCallbackData(Long chatId){
        sendMessage(chatId, "Главное меню", keyboardMarkup.getKeyboardMarkup(getButtons("mainmenu"), 1));
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
        if(reservation.getSentToCoworker()){
            sendMessage(chatId, "Вы уже отправили информацию о броне стола");
            return;
        }
        reservation.setSentToCoworker(true);
        sendMessage(chatId, "Бронь стола отправлена нашему сотруднику," +
                "   если на это время есть столик, " +
                "то вы получите подтверждения что стол был забронирован успешно.");
        sendMessage(getCoworkerChatId(), reservation.toString(),
                keyboardMarkup.getBookingConfirmationAdminMenu(getButtons("bookingconfirmationadminmenu"),
                        findReservationByChatId(chatId)));
    }
    private String getMenuText(Long chatId, List<MenuItem> items){
        if(items.isEmpty()){
            sendMessage(chatId, "Ошибка. Нет товаров в списке");
            return "";
        }
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
    private List<Button> sortFoodMenuButtons (List<Button> buttons){
        List<Button> finalList = new ArrayList<>(buttons);

        Button buttonAddition = null;
        Button buttonCart = null;
        Button buttonGoToPayment = null;

        Iterator<Button> iterator = finalList.iterator();
        while (iterator.hasNext()) {
            Button button = iterator.next();
            if (button.getName().contains("Добавки")) {
                buttonAddition = button;
                iterator.remove();
            } else if (button.getName().contains("Корзина")) {
                buttonCart = button;
                iterator.remove();
            } else if (button.getName().contains("Оплата")) {
                buttonGoToPayment = button;
                iterator.remove();
            }
        }

        if (buttonAddition != null) {
            finalList.add(buttonAddition);
        }
        if (buttonCart != null) {
            finalList.add(buttonCart);
        }
        if (buttonGoToPayment != null) {
            finalList.add(buttonGoToPayment);
        }
        return finalList;
    }

    private void processDescriptionForMenuItem(Long chatId, String messageText){
        MenuItem menuitem = menuItemRepository.findTopByChatIdOrderByIdDesc(chatId);
        menuitem.setDescription(messageText);
        sendMessage(chatId, "Подтвердить описание: "+messageText,
                keyboardMarkup.createTwoButtons("ДА", "CONFIRMDESCRIPTION", "НЕТ", "DECLINEDESCRIPTION"));
    }
    private void confirmDescription(Long chatId){
        sendMessage(chatId, "Товар добавлен в корзину", getGoToMenuButton());
        expectingDescriptionFromCoworker.put(chatId, false);
    }
    private void declineDescription(Long chatId){
        sendMessage(chatId, "Введите описание снова");
    }
    private void editDescription(Long chatId, String callbackData){
        int menuItemId = Integer.parseInt(callbackData.substring(callbackData.lastIndexOf("M")+1));
        MenuItem item = menuItemRepository.findById(menuItemId).orElse(null);
        sendMessage(chatId, "Текущее описание: "+ item.getDescription());
        sendMessage(chatId, "Введите новое описание");
        expectingEditDescription.put(chatId, menuItemId);
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
    @Scheduled(fixedRate = 180000)
    void removeReservationIfExpired(){
        List<Reservation> allReservations = reservationRepository.findAll();
        if(allReservations.isEmpty()){
            return;
        }
        LocalDateTime time = LocalDateTime.now();
        for (Reservation reservation : allReservations){
            if (reservation.getExpirationDate().isBefore(time)){
                reservationRepository.delete(reservation);
            }
        }
    }
    private String takeawayStringForCoworkers(Boolean takeaway){
        return takeaway ? "Забрать с собой" : "В заведении";
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
            else if (button1.getCallbackData().startsWith("SHOWDESCRIPTION")){
                button1.setCallbackData("SHOWDESCRIPTION"+callbackData);
            }
        }
        return buttons;
    }
    private void validatePassword (Long chatId, String messageText){
        String password = messageText.substring(8);
        AdminPassword value = adminPasswordRepository.findTopByPassword(password);
        if (value!=null) {
            Coworker coworker = new Coworker();
            coworker.setChatId(chatId);
            coworker.setActive(true);
            coworkerRepository.save(coworker);
            sendMessage(chatId, "Бот активен");
            if(checkIfTwoCoworkersAreActiveAtTheSameTime()){
                sendMessageToAllActiveCoworkers();
            }
        }
        else {
            sendMessage(chatId, "Пароль неверный");
        }
    }

    private void createReservationAndAddAmountOfPeople(Long chatId, String amountOfPeople){
        Reservation reservation = new Reservation();
        reservation.setChatId(chatId);
        reservation.setAmountOfPeople(amountOfPeople.substring(6));
        reservation.setSentToCoworker(false);
        reservation.setExpirationDate(LocalDateTime.now().plusHours(48));
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
            if(button1.getCallbackData().startsWith("DELETE")){
                button1.setCallbackData("DELETE"+callbackDataWithId);
            }
            else if (button1.getCallbackData().startsWith("SETSTOP")){
                button1.setCallbackData("SETSTOP"+callbackDataWithId);
            }
            else if (button1.getCallbackData().startsWith("EDIT")){
                button1.setCallbackData("EDITDESCRIPTION"+callbackDataWithId);
            }
        }
        return buttons;
    }
    private String createConfirmationText(MenuItem item){
        return String.format("Вы выбрали %s. Цена %d руб.", item.getName(), item.getPrice());
    }

    private void createNewSection(Long chatId, String messageCommand){
        String sectionName = messageCommand.substring(messageCommand.lastIndexOf(" "))
                .replace("\"", "").strip();
        Button button = new Button();
        Button adminButton = new Button();
        button.setName(sectionName);
        button.setBelongsToMenu("foodmenu");
        button.setCallbackData("SHOWALLITEMSIN"+sectionName.toLowerCase());
        adminButton.setName(sectionName);
        adminButton.setCallbackData("TYPE"+sectionName.toLowerCase());
        adminButton.setBelongsToMenu("adminmenu");
        buttonRepository.save(button);
        buttonRepository.save(adminButton);
        sendMessage(chatId, "Раздел добавлен");
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
    private Order createOrder(Long chatId, String takeaway){
        Order order = new Order();
        order.setChatId(chatId);
        order.setTotalPrice(getTotalPriceOfCartByChatId(chatId));
        List<Cart> itemsInCart = getAllItemsInCartByChatId(chatId);
        String itemsAsString = getOrderString(itemsInCart);
        order.setItems(itemsAsString);
        order.setTime(findOrderTimeByChatId(chatId));
        order.setOrderConfirmed(false);
        order.setOrderSentToCoworker(false);
        order.setTakeaway(takeaway.equals("TRUE"));
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
    private void createMenuItem(Long chatId, String menuItemInfo){
        String itemInfo = menuItemInfo.trim();
        String menuItemName = getItemsName(itemInfo);
        int menuItemsPrice = Integer.parseInt(getItemsPrice(itemInfo));
        MenuItem menuItem = new MenuItem();
        menuItem.setName(menuItemName);
        menuItem.setPrice(menuItemsPrice);
        menuItem.setChatId(chatId);
        menuItem.setBelongsToMenu("unknown");
        menuItem.setStopped(false);
        menuItemRepository.save(menuItem);
    }

    private MenuItem setMenuItemsMenuBelonging(Long chatId, String belongsToMenu){
        MenuItem menuItem = menuItemRepository.findTopByBelongsToMenuAndChatIdOrderByIdDesc("unknown", chatId);
        if(menuItem == null){
            sendMessage(chatId, "Уже добавлено в меню");
            return null;
        }
        menuItem.setBelongsToMenu(belongsToMenu.substring(4)+"menu");
        menuItemRepository.save(menuItem);
        return menuItem;
    }
    private void createButtonFromMenuItem(Long chatId, MenuItem menuItem){
        if(menuItem == null){
            return;
        }
        Button buttonToAdd = new Button();
        List<MenuItem> items = menuItemRepository.findAllByBelongsToMenuAndIsStoppedFalse(menuItem.getBelongsToMenu());
        buttonToAdd.setName(String.valueOf(items.size()));
        buttonToAdd.setBelongsToMenu(menuItem.getBelongsToMenu());
        buttonToAdd.setCallbackData("ITEM"+(menuItem.getId()));
        buttonRepository.save(buttonToAdd);
        sendMessage(chatId, "Теперь добавьте описание товара");
        expectingDescriptionFromCoworker.put(chatId, true);

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
        Reservation reservation = reservationRepository.findTopByCoworkerCommentContainingOrderByIdDesc("expecting");
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
    private void ifExpectingTextSetExpectingTextToFalse(Long chatId, List<Map<Long, Boolean>> expectingList){
        for (Map<Long, Boolean> expectingText : expectingList) {
            if (expectingText.containsKey(chatId) && expectingText.get(chatId)) {
                expectingText.put(chatId, false);
            }
        }
        expectingEditDescription.put(chatId, null);
    }
    private boolean checkIfTwoCoworkersAreActiveAtTheSameTime(){
        List<Coworker> coworkers = coworkerRepository.findAllByIsActiveTrue();
        return coworkers.size() > 1;
    }
    private void sendMessageToAllActiveCoworkers(){
        List<Coworker> coworkers = coworkerRepository.findAllByIsActiveTrue();
        for (Coworker coworker : coworkers){
            sendMessage(coworker.getChatId(), "Внимание! В данный" +
                    " момент активны 2 или более сотрудника одновременно. Не " +
                    "забудте что уведомления о заказах и бронировании буду присылаться только одному из вас." +
                    "Что бы отключить уведомления напишите /adminoff");
        }
    }
}





