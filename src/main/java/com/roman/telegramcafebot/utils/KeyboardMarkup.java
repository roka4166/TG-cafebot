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
    private List<Button> getButtons (String typeOfMenu, String itemName, String itemPrice){
        List<Button> buttons = buttonRepository.findAllByBelongsToMenu(typeOfMenu);
        for(Button button : buttons){
            if (button.getCallbackData().equals("ADDTOCART")){
                button.setCallbackData("ADDTOCART"+itemName+" "+itemPrice);
            }
        }
        return buttons;
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
    public ReplyKeyboardMarkup getReplyKeyboardMarkup(){
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Оплатить");
        row.add("Корзина");
        rows.add(row);
        replyKeyboardMarkup.setKeyboard(rows);
        return replyKeyboardMarkup;
    }
    public InlineKeyboardMarkup getCartKeyBoardMarkup(List<MenuItem> items){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        int batchSize = 4;
        int totalButtons = items.size();
        int numberOfIterations = (int) Math.ceil((double) totalButtons / batchSize);

        for (int j = 0; j < numberOfIterations; j++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int i = j * batchSize; i < Math.min((j + 1) * batchSize, totalButtons); i++) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(String.valueOf(i+1));
                button.setCallbackData("REMOVEFROMCART"+items.get(i).getId());
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
}
