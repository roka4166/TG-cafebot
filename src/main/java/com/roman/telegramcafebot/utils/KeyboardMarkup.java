package com.roman.telegramcafebot.utils;

import com.roman.telegramcafebot.models.Button;
import com.roman.telegramcafebot.models.Cart;
import com.roman.telegramcafebot.models.Reservation;
import com.roman.telegramcafebot.repositories.ButtonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
@Component
public class KeyboardMarkup {

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

        int additionItemId = Integer.parseInt(callbackData.substring(12));

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
    public InlineKeyboardMarkup getBookingConfirmationAdminMenu(List<Button> buttons, Reservation reservation){
        for(Button button : buttons){
            if (button.getCallbackData().startsWith("CONFIRMRESERVATION")){
                button.setCallbackData("CONFIRMRESERVATION"+reservation.getId());
            }
            else if (button.getCallbackData().startsWith("DECLINERESERVATION")){
                button.setCallbackData("DECLINERESERVATION" + reservation.getId());
            }
        }
        return createInlineKeyboardMarkup(buttons, 2);
    }
    public InlineKeyboardMarkup createOneButton(String text, String callbackData){
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);

        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        rowInLine.add(button);
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        rowsInLine.add(rowInLine);
        keyboardMarkup.setKeyboard(rowsInLine);
        return keyboardMarkup;
    }
    public InlineKeyboardMarkup createTwoButtons(String text, String callbackData, String text2, String callbackData2){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        button2.setText(text2);
        button2.setCallbackData(callbackData2);

        rowInLine.add(button);
        rowInLine.add(button2);
        rowsInLine.add(rowInLine);
        keyboardMarkup.setKeyboard(rowsInLine);
        return keyboardMarkup;
    }
}
