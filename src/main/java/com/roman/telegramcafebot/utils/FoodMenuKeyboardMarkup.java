package com.roman.telegramcafebot.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
@Component
public class FoodMenuKeyboardMarkup {

    private List<InlineKeyboardButton> createRowInLine (){
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton breakfastButton = new InlineKeyboardButton();
        breakfastButton.setText("Breakfasts");
        breakfastButton.setCallbackData("BREAKFAST_BUTTON");

        InlineKeyboardButton croissantButton = new InlineKeyboardButton();
        croissantButton.setText("Croissant");
        croissantButton.setCallbackData("CROISSANT_BUTTON");

        InlineKeyboardButton RomanPizzaButton = new InlineKeyboardButton();
        RomanPizzaButton.setText("RomanPizza");
        RomanPizzaButton.setCallbackData("ROMANPIZZA_BUTTON");

        InlineKeyboardButton HotFoodButton = new InlineKeyboardButton();
        HotFoodButton.setText("Hot food");
        HotFoodButton.setCallbackData("HOTFOOD_BUTTON");

        InlineKeyboardButton pieButton = new InlineKeyboardButton();
        pieButton.setText("Pies *chudu*");
        pieButton.setCallbackData("PIE_BUTTON");

        InlineKeyboardButton soupButton = new InlineKeyboardButton();
        soupButton.setText("soup");
        soupButton.setCallbackData("SOUP_BUTTON");

        InlineKeyboardButton saladButton = new InlineKeyboardButton();
        saladButton.setText("salad");
        saladButton.setCallbackData("SALAD_BUTTON");

        InlineKeyboardButton sandwichButton = new InlineKeyboardButton();
        sandwichButton.setText("sandwich");
        sandwichButton.setCallbackData("SANDWICH_BUTTON");

        InlineKeyboardButton bruschettaButton = new InlineKeyboardButton();
        bruschettaButton.setText("bruschetta");
        bruschettaButton.setCallbackData("BRUSCHETTA_BUTTON");

        InlineKeyboardButton drinksButton = new InlineKeyboardButton();
        breakfastButton.setText("Drinks");
        breakfastButton.setCallbackData("DRINKS_BUTTON");

        rowInLine.add(breakfastButton);
        rowInLine.add(croissantButton);
        rowInLine.add(RomanPizzaButton);
        rowInLine.add(HotFoodButton);
        rowInLine.add(pieButton);
        rowInLine.add(soupButton);
        rowInLine.add(saladButton);
        rowInLine.add(sandwichButton);
        rowInLine.add(drinksButton);

        return rowInLine;
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(List<InlineKeyboardButton> allButtons){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        int buttonsPerRow = 3;

        for (int i = 0; i < allButtons.size(); i += buttonsPerRow) {
            int endIndex = Math.min(i + buttonsPerRow, allButtons.size());
            List<InlineKeyboardButton> row = allButtons.subList(i, endIndex);
            rowsInLine.add(new ArrayList<>(row));
        }

        keyboardMarkup.setKeyboard(rowsInLine);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getFoodMenuKeyboardMarkup(){
        return createInlineKeyboardMarkup(createRowInLine());
    }
}
