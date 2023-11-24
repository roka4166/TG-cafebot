package com.roman.telegramcafebot.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
@Component
public class MainMenuKeyboardMarkup {

    private List<InlineKeyboardButton> createRowInLine (){
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton reservationButton = new InlineKeyboardButton();
        reservationButton.setText("Бронь стола");
        reservationButton.setCallbackData("RESERVATION_BUTTON");

        InlineKeyboardButton buyButton = new InlineKeyboardButton();
        buyButton.setText("Купить");
        buyButton.setCallbackData("BUY_BUTTON");
        InlineKeyboardButton deliveryButton = new InlineKeyboardButton();
        deliveryButton.setText("Доставка");
        deliveryButton.setCallbackData("DELIVERY_BUTTON");

        rowInLine.add(reservationButton);
        rowInLine.add(buyButton);
        rowInLine.add(deliveryButton);

        return rowInLine;
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(List<InlineKeyboardButton> rowInLine){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        rowsInLine.add(rowInLine);
        keyboardMarkup.setKeyboard(rowsInLine);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getMainMenuKeyboardMarkup(){
        return createInlineKeyboardMarkup(createRowInLine());
    }
}
