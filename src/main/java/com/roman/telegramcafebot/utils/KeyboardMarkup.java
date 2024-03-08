package com.roman.telegramcafebot.utils;

import com.roman.telegramcafebot.models.Button;
import com.roman.telegramcafebot.models.Cart;
import com.roman.telegramcafebot.models.MenuItem;
import com.roman.telegramcafebot.repositories.ButtonRepository;
import com.roman.telegramcafebot.repositories.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
@Component
public class KeyboardMarkup {

    private ButtonRepository buttonRepository;
    @Autowired
    public KeyboardMarkup(ButtonRepository buttonRepository) {
        this.buttonRepository = buttonRepository;
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(List<Button> buttons, int rowsPerLine){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        int batchSize = rowsPerLine;
        int totalButtons = buttons.size();
        int numberOfIterations = (int) Math.ceil((double) totalButtons / batchSize);

        for (int j = 0; j < numberOfIterations; j++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int i = j * batchSize; i < Math.min((j + 1) * batchSize, totalButtons); i++) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(buttons.get(i).getName());
                button.setCallbackData(buttons.get(i).getCallbackData());
                row.add(button);
            }
            rowsInLine.add(row);
        }

        keyboardMarkup.setKeyboard(rowsInLine);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getKeyboardMarkup(List<Button> buttons, int rowsPerLine){
        return createInlineKeyboardMarkup(buttons, rowsPerLine);
    }
    public InlineKeyboardMarkup getCartKeyBoardMarkup(List<Cart> itemsInCart){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        int batchSize = 4;
        int totalButtons = itemsInCart.size();
        int numberOfIterations = (int) Math.ceil((double) totalButtons / batchSize);

        for (int j = 0; j < numberOfIterations; j++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int i = j * batchSize; i < Math.min((j + 1) * batchSize, totalButtons); i++) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(String.valueOf(i+1));
                button.setCallbackData("REMOVEFROMCART"+itemsInCart.get(i).getItemsId());
                row.add(button);
            }
            rowsInLine.add(row);
        }

        InlineKeyboardButton removeAllButton = new InlineKeyboardButton();
        InlineKeyboardButton goToPaymentButton = new InlineKeyboardButton();
        InlineKeyboardButton backToMenuButton = new InlineKeyboardButton();

        removeAllButton.setText("Очистить корзину");
        goToPaymentButton.setText("К оплате");
        backToMenuButton.setText("Назад в меню");

        removeAllButton.setCallbackData("REMOVEALLFROMCART");
        goToPaymentButton.setCallbackData("GOTOPAYMENT");
        backToMenuButton.setCallbackData("FOODMENU");

        List<InlineKeyboardButton> row = new ArrayList<>();

        row.add(removeAllButton);
        row.add(goToPaymentButton);
        row.add(backToMenuButton);

        rowsInLine.add(row);
        keyboardMarkup.setKeyboard(rowsInLine);
        return keyboardMarkup;
    }
    public InlineKeyboardMarkup getAddAdditionToCartMenuItemKeyboardMarkup(List<Cart> itemsInCart, String callbackData){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        int additionItemId = Integer.valueOf(callbackData.substring(12));

        int batchSize = 4;
        int totalButtons = itemsInCart.size();
        int numberOfIterations = (int) Math.ceil((double) totalButtons / batchSize);

        for (int j = 0; j < numberOfIterations; j++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int i = j * batchSize; i < Math.min((j + 1) * batchSize, totalButtons); i++) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(String.valueOf(i+1));
                button.setCallbackData(itemsInCart.get(i).getItemsId()+ "+" + additionItemId);
                row.add(button);
            }
            rowsInLine.add(row);
        }
        InlineKeyboardButton backToMenuButton = new InlineKeyboardButton();

        backToMenuButton.setText("Назад в меню");
        backToMenuButton.setCallbackData("FOODMENU");

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(backToMenuButton);

        rowsInLine.add(row);
        keyboardMarkup.setKeyboard(rowsInLine);
        return keyboardMarkup;
    }
    public InlineKeyboardMarkup getBookingConfirmationAdminMenu(List<Button> buttons, int reservationId){
        for(Button button : buttons){
            if (button.getCallbackData().startsWith("RESERVATION_CONFIRMED")){
                button.setCallbackData("RESERVATION_CONFIRMED"+reservationId);
            }
            else if (button.getCallbackData().startsWith("RESERVATION_DECLINED")){
                button.setCallbackData("RESERVATION_DECLINED"+" " + reservationId);
            }
        }
        return createInlineKeyboardMarkup(buttons, 2);
    }
}
