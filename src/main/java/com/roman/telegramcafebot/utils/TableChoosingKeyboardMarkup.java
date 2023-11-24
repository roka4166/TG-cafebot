package com.roman.telegramcafebot.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class TableChoosingKeyboardMarkup {

    private List<InlineKeyboardButton> createRowInLine (){
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var tableButton_1 = new InlineKeyboardButton();
        tableButton_1.setText("1");
        tableButton_1.setCallbackData("TABLE_1");

        var tableButton_2 = new InlineKeyboardButton();
        tableButton_2.setText("2");
        tableButton_2.setCallbackData("TABLE_2");

        var tableButton_3 = new InlineKeyboardButton();
        tableButton_3.setText("3");
        tableButton_3.setCallbackData("TABLE_3");

        rowInLine.add(tableButton_1);
        rowInLine.add(tableButton_2);
        rowInLine.add(tableButton_3);

        return rowInLine;
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(List<InlineKeyboardButton> rowInLine){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        rowsInLine.add(rowInLine);
        keyboardMarkup.setKeyboard(rowsInLine);

        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getTableChoosingKeyboardMarkup(){
        return createInlineKeyboardMarkup(createRowInLine());
    }
}
