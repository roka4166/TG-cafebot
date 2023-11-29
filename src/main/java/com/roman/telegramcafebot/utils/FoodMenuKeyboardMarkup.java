package com.roman.telegramcafebot.utils;

import com.roman.telegramcafebot.models.ButtonNotDB;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
@Component
public class FoodMenuKeyboardMarkup {

    private List<ButtonNotDB> createAllButtons (){
        List<ButtonNotDB> allButtonNotDBS = new ArrayList<>();

        ButtonNotDB breakfastButtonNotDB = new ButtonNotDB("Breakfast", "BREAKFAST_BUTTON");
        ButtonNotDB croissantButtonNotDB = new ButtonNotDB("Croissant", "CROISSANT_BUTTON");
        ButtonNotDB romanPizzaButtonNotDB = new ButtonNotDB("RomanPizza", "ROMANPIZZA_BUTTON");
        ButtonNotDB hotFoodButtonNotDB = new ButtonNotDB("Hot food", "HOTFOOD_BUTTON");
        ButtonNotDB pieButtonNotDB = new ButtonNotDB("Pies *chudu*", "PIE_BUTTON");
        ButtonNotDB soupButtonNotDB = new ButtonNotDB("soup", "SOUP_BUTTON");
        ButtonNotDB saladButtonNotDB = new ButtonNotDB("salad", "SALAD_BUTTON");
        ButtonNotDB sandwichButtonNotDB = new ButtonNotDB("sandwich", "SANDWICH_BUTTON");
        ButtonNotDB bruschettaButtonNotDB = new ButtonNotDB("bruschetta", "BRUSCHETTA_BUTTON");
        ButtonNotDB drinksButtonNotDB = new ButtonNotDB("Drinks", "DRINKS_BUTTON");
        ButtonNotDB breadButtonNotDB = new ButtonNotDB("Bread", "BREAD_BUTTON");
        ButtonNotDB desertButtonNotDB = new ButtonNotDB("Desert", "DESERT_BUTTON");

        allButtonNotDBS.add(breakfastButtonNotDB);
        allButtonNotDBS.add(croissantButtonNotDB);
        allButtonNotDBS.add(romanPizzaButtonNotDB);
        allButtonNotDBS.add(hotFoodButtonNotDB);
        allButtonNotDBS.add(pieButtonNotDB);
        allButtonNotDBS.add(soupButtonNotDB);
        allButtonNotDBS.add(saladButtonNotDB);
        allButtonNotDBS.add(sandwichButtonNotDB);
        allButtonNotDBS.add(drinksButtonNotDB);
        allButtonNotDBS.add(breadButtonNotDB);
        allButtonNotDBS.add(desertButtonNotDB);

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

    public InlineKeyboardMarkup getFoodMenuKeyboardMarkup(){
        return createInlineKeyboardMarkup(createAllButtons());
    }
}
