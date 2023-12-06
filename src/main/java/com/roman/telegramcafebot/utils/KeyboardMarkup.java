package com.roman.telegramcafebot.utils;

import com.roman.telegramcafebot.models.Button;
import com.roman.telegramcafebot.repositories.ButtonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
@Component
public class KeyboardMarkup {

    private ButtonRepository buttonRepository;
    @Autowired
    public KeyboardMarkup(ButtonRepository buttonRepository) {
        this.buttonRepository = buttonRepository;
    }

    private List<Button> getButtons (String typeOfMenu){
        return buttonRepository.findAllByBelongsToMenu(typeOfMenu);
    }
    private List<Button> getButtons (String typeOfMenu, String itemName, String itemPrice){
        List<Button> buttons = buttonRepository.findAllByBelongsToMenu(typeOfMenu);
        for(Button button : buttons){
            if (button.getCallbackData().equals("ADDTOCART_BUTTON")){
                button.setCallbackData("ADDTOCART_BUTTON"+itemName+" "+itemPrice);
            }
        }
        return buttons;
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(List<Button> buttons){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        int batchSize = 3;
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

    public InlineKeyboardMarkup getKeyboardMarkup(String typeOfMenu){
        return createInlineKeyboardMarkup(getButtons(typeOfMenu));
    }
    public InlineKeyboardMarkup getKeyboardMarkup(String typeOfMenu, String itemName, String itemPrice){
        return createInlineKeyboardMarkup(getButtons(typeOfMenu, itemName, itemPrice));
    }
}
