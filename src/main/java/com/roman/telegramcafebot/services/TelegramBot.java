package com.roman.telegramcafebot.services;

import com.roman.telegramcafebot.config.BotConfig;
import com.roman.telegramcafebot.models.*;
import com.roman.telegramcafebot.repositories.*;
import com.roman.telegramcafebot.utils.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
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

@Service
@Transactional
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private Map<Long, Boolean> expectingNameForReservationMap = new HashMap<>();
    private Map<Long, Boolean> expectingTimeForReservationMap = new HashMap<>();
    private Map<Long, Boolean> expectingTimeForPreorder = new HashMap<>();
    private Map<Long, Boolean> expectingCommentFromCoworker = new HashMap<>();


    private KeyboardMarkup keyboardMarkup;

    private AdminPassowrdRepository adminPasswordRepository;
    private final BotConfig botConfig;

    private CoworkerRepository coworkerRepository;

    private ButtonRepository buttonRepository;

    private OrderRepository orderRepository;

    private MenuItemRepository menuItemRepository;

    private CartRepository cartRepository;

    private ReservationRepository reservationRepository;
    @Autowired
    public TelegramBot(Reservation reservation, BotConfig botConfig, KeyboardMarkup keyboardMarkup,
                       AdminPassowrdRepository adminPasswordRepository, ButtonRepository buttonRepository,
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
            Integer messageId = update.getMessage().getMessageId();


            switch (messageText) {
                case "/start" ->
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());

                case "/adminoff" -> {
                    Coworker coworker = coworkerRepository.findCoworkerByChatId(String.valueOf(chatId));
                    if(coworker != null){
                        coworker.setActive(false);
                        coworkerRepository.save(coworker);
                    }
                }
                case "/adminon" -> {
                    Coworker coworker = coworkerRepository.findCoworkerByChatId(String.valueOf(chatId));
                    if(coworker != null){
                        coworker.setActive(true);
                        coworkerRepository.save(coworker);
                    }
                }
            }
            if(expectingNameForReservationMap.containsKey(chatId) && expectingNameForReservationMap.get(chatId)) {
                Reservation reservation = findReservationByChatId(chatId);
                reservation.setName(messageText);
                reservationRepository.save(reservation);
                sendMessage(chatId, "Теперь введите время");
                expectingNameForReservationMap.put(chatId, false);
                expectingTimeForReservationMap.put(chatId, true);
            }

            else if (expectingTimeForReservationMap.containsKey(chatId) && expectingTimeForReservationMap.get(chatId)) {
                Reservation reservation = findReservationByChatId(chatId);
                reservation.setTime(messageText);
                reservationRepository.save(reservation);
                sendMessage(chatId, getBookingConfirmationTextByChatId(chatId),
                        keyboardMarkup.getKeyboardMarkup(getButtons("bookingconfirmationmenu"), 2));
                expectingTimeForReservationMap.put(chatId, false);
            }

            else if (expectingCommentFromCoworker.containsKey(getCoworkerChatId()) && expectingCommentFromCoworker.get(getCoworkerChatId())) {
                Reservation reservation = findReservationByCommentExpectation();
                reservation.setCoworkerComment(messageText);
                reservationRepository.save(reservation);
                sendMessage(getCoworkerChatId(), "Бронь отклонена, информация об этом отправилась человеку");
                String text = "Бронь откланена. Комментарий от сотрудника: " + reservation.getCoworkerComment();
                sendMessage(reservation.getChatId(), text);
                expectingCommentFromCoworker.put(getCoworkerChatId(), false);
                deleteReservation(reservation);
            }
            if(expectingTimeForPreorder.containsKey(chatId) && expectingTimeForPreorder.get(chatId)) {

                expectingTimeForPreorder.put(chatId, false);
            }

            else if (messageText.startsWith("/password")) {
                String password = messageText.substring(10);
                String passwordFromDB = Objects.requireNonNull(adminPasswordRepository.findById(1).orElse(null)).getKey();
                if(password.equals(passwordFromDB)){
                    Coworker coworker = new Coworker();
                    coworker.setChatId(chatId);
                    coworker.setActive(true);
                    coworkerRepository.save(coworker);
                    sendMessage(chatId, "Ключ активирован");
                }
            }
            else if (messageText.startsWith("/newitem")){
                if(validateCoworker(chatId)){
                    createMenuItem(messageText);
                    sendMessage(getCoworkerChatId(), "К какому разделу относиться?",
                            keyboardMarkup.getKeyboardMarkup(getButtons("adminmenu"), 3));
                }
                else sendMessage(chatId, "Не добавлено в корзину, у вас нет доступа к этой функции");
            }
            else if (messageText.startsWith("/deleteitem")){
                if(validateCoworker(chatId)){
                    deleteMenuItem(messageText);
                    sendMessage(chatId, "Удалено успешно", getGoToMenuButton());
                }
                else sendMessage(chatId, "Не удалено, у вас нет доступа к этой функции");
            }
        }
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.startsWith("TYPE")){
                createButtonFromMenuItem(setMenuItemsMenuBelonging(callbackData));
                sendMessage(getCoworkerChatId(), "Добавлено успешно", getGoToMenuButton());
            }
            else if (callbackData.startsWith("TABLE_")){
                int tableNumber = Integer.parseInt(callbackData.substring(6));
                Reservation reservation = new Reservation();
                reservation.setChatId(chatId);
                reservation.setTable(tableNumber);
                reservationRepository.save(reservation);
                String text = "Вы выбрали стол " + tableNumber + " теперь нужно ввести имя на которое вы хотите" +
                        "забронировать столик.";
                sendMessage(chatId, text);
                expectingNameForReservationMap.put(chatId, true);
            }
            else if (callbackData.startsWith("REMOVEFROMCART")){
                removeFromCart(chatId, callbackData);
                sendMessage(chatId, "Удалено из корзины", getGoToMenuButton());
            }
            else if(callbackData.startsWith("ITEM")){
                sendMessage(chatId, createConfirmationText(findItemById(callbackData)),
                        keyboardMarkup.getKeyboardMarkup(getConfirmationButtons("confirmationmenu", callbackData), 2));
            }
            else if(callbackData.startsWith("ADDTOCART")){
                addToCart(chatId, callbackData);
                sendMessage(chatId,"Добавлено успешно", getGoToMenuButton());
            }
            else if(callbackData.startsWith("RESERVATION_CONFIRMED")){
                Reservation reservation = findReservationById(callbackData);
                String text = "Бронь стола подтверждена";
                sendMessage(reservation.getChatId(), text);
                deleteReservation(reservation);
            }
            else if(callbackData.startsWith("RESERVATION_DECLINED")){
                Reservation reservation = findReservationById(callbackData);
                reservation.setCoworkerComment("expecting");
                reservationRepository.save(reservation);
                String text = "Введите причину отказа брони";
                sendMessage(getCoworkerChatId(), text);
                expectingCommentFromCoworker.put(getCoworkerChatId(), true);
            }

            switch (callbackData) {
                case "START" ->
                    sendMessage(chatId,"Главное меню",
                            keyboardMarkup.getKeyboardMarkup(getButtons("mainmenu"),  1));

                case "RESERVATION" -> {
                    String text = "Введите номер стола: ";
                    sendMessage(chatId, text,
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("tablemenu")), 4));
                }
                case "CONFIRMBOOKING" -> {
                    sendMessage(chatId, "Бронь стола отправлена нашему сотруднику," +
                            "   если этот стол свободен на это время" +
                            "то вы получите подтверждения что стол был забронирован успешно.");
                    sendMessage(getCoworkerChatId(), findReservationByChatId(chatId).toString(),
                            keyboardMarkup.getBookingConfirmationAdminMenu(getButtons("bookingconfirmationadminmenu"),
                                    findReservationByChatId(chatId).getId()));
                }
                case "FOODMENU" ->
                    sendMessage(chatId, "Menu", keyboardMarkup.getKeyboardMarkup(getButtons("foodmenu"), 3));

                case "BREAKFASTS" ->
                    sendMessage(chatId, getMenuText(getItemsByMenuBelonging("breakfastmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("breakfastmenu")), 4));

                case "CROISSANTS" ->
                    sendMessage(chatId, getMenuText(getItemsByMenuBelonging("croissantmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("croissantmenu")), 4));

                case "ROMANPIZZAS" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("romanpizzamenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("romanpizzamenu")), 4));

                case "HOTFOOD" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("hotfoodmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("hotfoodmenu")), 4));

                case "PIESCHUDU" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("pieschudumenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("pieschudumenu")), 1));

                case "SOUPS" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("soupmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("soupmenu")), 4));

                case "SALADS" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("saladmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("saladmenu")), 4));

                case "SANDWICHES" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("sandwichmenu")), keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("sandwichmenu")), 4));

                case "BRUSCHETTAS" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("bruschettamenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("bruschettamenu")), 4));

                case "ADDITION" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("additionmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("additionmenu")), 4));

                case "DRINKS" ->
                    sendMessage(chatId,"Напитки",
                            keyboardMarkup.getKeyboardMarkup(getButtons("drinkmenu"), 2));

                case "DESERTS" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("desertmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("desertmenu")), 2));

                case "BREAD" ->
                    sendMessage(chatId,"Римские пиццы",
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("breadmenu")), 2));

                case "TEA" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("teamenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("teamenu")), 4));

                case "COFFEE" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("coffeemenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("coffemenu")), 4));

                case "CACAO" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("cacaomenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("cacaomenu")),4 ));

                case "SIGNATUREDRINKS" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("signaturedrinksmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("signaturedrinksmenu")), 4));

                case "NOTTEA" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("notteamenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("notteamenu")), 4));

                case "DRINKSADDITION" ->
                    sendMessage(chatId,getMenuText(getItemsByMenuBelonging("drinksadditionmenu")),
                            keyboardMarkup.getKeyboardMarkup(sortButtonsByName(getButtons("drinksadditionmenu")), 4));

                case "GOTOPAYMENT" -> {
                    
                    String text = "Для оплаты нужно перевести " + getTotalPriceOfCartByChatId(chatId) + " рублей на карту 1234 3456 " +
                            "2345 4556 или на телефон 9485749284 далее нажмите на кнопку олпачено и после этого" +
                            "информация попадет к нашему сотруднику";
                    sendMessage(chatId, text, createOneButton("Подтвердить", "PAYMENTCONFIRMED"));
                }
                case "PAYMENTCONFIRMED" -> {
                    Order order = createOrder(chatId);
                    sendMessage(chatId, "Благодарим за покупку");
                    Long coworkerChatId = getCoworkerChatId();
                    sendMessage(coworkerChatId, order.toString());
                }
                case "SHOWCART" -> {
                    if(checkIfCartIsEmpty(chatId)){
                        sendMessage(chatId, "Корзина пуста", getGoToMenuButton());
                    }
                    else {
                        sendMessage(chatId, getMenuTextForCart(getAllMenuItemsFromCart(getAllItemsInCartByChatId(chatId))),
                                keyboardMarkup.getCartKeyBoardMarkup(getAllMenuItemsFromCart(getAllItemsInCartByChatId(chatId))));
                    }
                }
                case "REMOVEALLFROMCART" -> {
                    removeAllFromCart(chatId);
                    sendMessage(chatId, "Корзина очищина", getGoToMenuButton());
                }

            }
        }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    private void startCommandReceived(Long chatId, String name) {
        String answer = "Приветствую, " + name;
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
    private void saveOrder(Order order){
        orderRepository.save(order);
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
        Coworker coworker = coworkerRepository.findCoworkerByChatId(chatId.toString());
        return coworker != null;
    }
    private Long getCoworkerChatId(){
        Coworker coworker = coworkerRepository.findCoworkerByIsActive(true);
        return Long.valueOf(coworker.getChatId());
    }
    private String getMenuText(List<MenuItem> items){
        StringBuilder menuText = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            menuText.append(i+1 + ". " + items.get(i).getName() + " " + items.get(i).getPrice() + " руб." + "\n");
        }
        return menuText.toString();
    }

    private String getMenuTextForCart(List<MenuItem> items){
        StringBuilder menuText = new StringBuilder();
        String cartMenuText = "Что бы удалить предмет из корзины, просто кликните по номеру который соответствует товару" + "\n" + "\n";
        menuText.append(cartMenuText);
        int sum = 0;
        for (int i = 0; i < items.size(); i++) {
            menuText.append(i + 1 + ". " + items.get(i).getName() + " " + items.get(i).getPrice() + " руб." + "\n");
            sum += items.get(i).getPrice();
        }
        String infoAboutSum = "\n" + "Сумма корзины " + sum;
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
        return menuItemRepository.findAllByBelongsToMenu(belongsToMenu);
    }
    private List<Button> getButtons (String typeOfMenu){
        if(typeOfMenu.equals("foodmenu")){

        }
        buttonRepository.findAllByBelongsToMenu(typeOfMenu);
        return buttonRepository.findAllByBelongsToMenu(typeOfMenu);
    }
    private List<Button> getConfirmationButtons (String typeOfMenu, String callbackDataWithId){
        String id = callbackDataWithId.substring(4);
        List<Button> buttons = buttonRepository.findAllByBelongsToMenu(typeOfMenu);
        for (Button button1 : buttons){
            if(button1.getCallbackData().startsWith("ADDTOCART")){
                button1.setCallbackData("ADDTOCART"+id);
            }
        }
        return buttons;
    }
    private String createConfirmationText(MenuItem item){
        return "Вы выбрали " + item.getName() +". Цена " +  item.getPrice();
    }
    private MenuItem findItemById(String id){
        return menuItemRepository.findById(Integer.valueOf(id.substring(4))).orElse(null);
    }
    private void addToCart(Long chatId, String callbackData){
        Integer menuItemId = Integer.valueOf(callbackData.substring(9));
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
            itemsAsString.append(item.getName()).append(" ");
        }
        order.setItems(itemsAsString.toString());
        orderRepository.save(order);
        removeAllFromCart(chatId);
        return order;
    }
    private void deleteMenuItem(String messageText){
        String menuItemName = getItemsName(messageText);
        MenuItem menuItemForRemoval = menuItemRepository.findMenuItemByName(menuItemName);
        int menuItemId = menuItemForRemoval.getId();
        Button buttonForRemoval = buttonRepository.findButtonByCallbackData("ITEM"+menuItemId);
        buttonRepository.delete(buttonForRemoval);
        menuItemRepository.delete(menuItemForRemoval);
    }
    private void createMenuItem(String menuItemInfo){
        String itemInfo = menuItemInfo.trim();
        String menuItemName = getItemsName(itemInfo);
        int menuItemsPrice = Integer.valueOf(getItemsPrice(itemInfo));
        MenuItem menuItem = new MenuItem();
        menuItem.setName(menuItemName);
        menuItem.setPrice(menuItemsPrice);
        menuItem.setBelongsToMenu("unknown");
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
        List<MenuItem> items = menuItemRepository.findAllByBelongsToMenu(menuItem.getBelongsToMenu());
        buttonToAdd.setName(String.valueOf(items.size()));
        buttonToAdd.setBelongsToMenu(menuItem.getBelongsToMenu());
        buttonToAdd.setCallbackData("ITEM"+String.valueOf(menuItem.getId()));
        buttonRepository.save(buttonToAdd);
    }
    private void removeFromCart(Long chatId, String callbackData){
        int menuItemId = Integer.valueOf(callbackData.substring(14));
        cartRepository.deleteByItemsId(menuItemId); ///TODO
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
            totalPrice += menuItemRepository.findById(cart1.getItemsId()).orElse(null).getPrice();
        }
        return totalPrice;
    }
    private boolean checkIfCartIsEmpty(Long chatId){
        List<Cart> cart = getAllItemsInCartByChatId(chatId);
        if(cart.isEmpty()){
            return true;
        }
        return false;
    }
    private void removeAllFromCart(Long chatId){
        cartRepository.deleteAllByChatId(chatId);
    }



    private String getBookingConfirmationTextByChatId(Long chatId){
        Reservation reservaton = findReservationByChatId(chatId);
        return "Подтвердить бронь на имя " + reservaton.getName() + " .На время " + reservaton.getTime() + "?";
    }

    private Reservation findReservationByChatId(Long chatId){
        return reservationRepository.findReservationByChatId(chatId);
    }
    private Reservation findReservationById(String callbackData){
        int id = Integer.valueOf(callbackData.substring(21));
        return reservationRepository.findById(id).orElse(null);
    }
    private Reservation findReservationByCommentExpectation(){
        return reservationRepository.findReservationByCoworkerComment("expecting");
    }
    private void deleteReservation(Reservation reservation){
        reservationRepository.delete(reservation);

    }
}
