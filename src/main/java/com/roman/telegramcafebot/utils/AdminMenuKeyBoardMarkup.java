package com.roman.telegramcafebot.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
@Component
public class AdminMenuKeyBoardMarkup {

    private List<InlineKeyboardButton> createRowInLine () {
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("Создать товар");
        addButton.setCallbackData("CREATE_BUTTON");

        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText("Изменить товар");
        updateButton.setCallbackData("UPPDATE_BUTTON");

        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("Удалить товар");
        deleteButton.setCallbackData("DELETE_BUTTON");

        rowInLine.add(addButton);
        rowInLine.add(updateButton);
        rowInLine.add(deleteButton);

        return rowInLine;
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(List<InlineKeyboardButton> rowInLine){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        rowsInLine.add(rowInLine);
        keyboardMarkup.setKeyboard(rowsInLine);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getAdminKeyboardMarkup(){
        return createInlineKeyboardMarkup(createRowInLine());
    }
}
