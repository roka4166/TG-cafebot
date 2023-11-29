package com.roman.telegramcafebot.models;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
@Component
public class FoodMenu {

    private List<ButtonNotDB> createAllButtons (List<MenuItem> menuItems){
        List<ButtonNotDB> allButtonNotDBS = new ArrayList<>();

        for (MenuItem menuItem : menuItems) {
            String itemName = menuItem.getName();
            allButtonNotDBS.add(new ButtonNotDB(itemName, itemName.toUpperCase()));
        }

        return allButtonNotDBS;
    }
    private InlineKeyboardMarkup createInlineKeyboardMarkup(List<ButtonNotDB> allButtonNotDBS){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        int batchSize = 3;
        int totalButtons = allButtonNotDBS.size();
        int numberOfIterations = (int) Math.ceil((double) totalButtons / batchSize);

        for (int j = 0; j < numberOfIterations; j++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int i = j * batchSize; i < Math.min((j + 1) * batchSize, totalButtons); i++) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(allButtonNotDBS.get(i).getText());
                button.setCallbackData(allButtonNotDBS.get(i).getCallbackData());
                row.add(button);
            }
            rowsInLine.add(row);
        }

        keyboardMarkup.setKeyboard(rowsInLine);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getFoodMenuKeyboardMarkup(List<MenuItem> menuItems){
        return createInlineKeyboardMarkup(createAllButtons(menuItems));
    }
}
