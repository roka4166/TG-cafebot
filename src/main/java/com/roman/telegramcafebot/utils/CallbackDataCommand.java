package com.roman.telegramcafebot.utils;
public enum CallbackDataCommand {
    SETSTOP("SETSTOP"),
    DELETEITEM("DELETEITEM"),
    PAYMENTCONFIRMEDBYCOWORKER("PAYMENTCONFIRMEDBYCOWORKER"),
    RESERVATION_DECLINED("RESERVATION_DECLINED"),
    RESERVATION_CONFIRMED("RESERVATION_CONFIRMED"),
    ADDTOCART("ADDTOCART"),
    STOPPEDITEM("STOPPEDITEM"),
    ITEM("ITEM"),
    REMOVEFROMCART("REMOVEFROMCART"),
    AMOUNT("AMOUNT"),
    TYPE("TYPE"),
    SHOWALLSTOPPED("SHOWALLSTOPPED"),
    ADMINFOODMENU("ADMINFOODMENU"),
    REMOVEALLFROMCART("REMOVEALLFROMCART"),
    SHOWCART("SHOWCART"),
    PAYMENTCONFIRMEDBYCUSTOMER("PAYMENTCONFIRMEDBYCUSTOMER"),
    GOTOPAYMENT("GOTOPAYMENT"),
    DRINKSADDITION("DRINKSADDITION"),
    NOTTEA("NOTTEA"),
    SIGNATUREDRINKS("SIGNATUREDRINKS"),
    CACAO("CACAO"),
    COFFEE("COFFEE"),
    TEA("TEA"),
    BREAD("BREAD"),
    PUFFPASTRY("PUFFPASTRY"),
    DESERTS("DESSERTS"),
    DRINKS("DRINKS"),
    ADDITION("ADDITION"),
    BRUSCHETTAS("BRUSCHETTAS"),
    SANDWICHES("SANDWICHES"),
    SALADS("SALADS"),
    SOUPS("SOUPS"),
    PIESCHUDU("PIESCHUDU"),
    HOTFOOD("HOTFOOD"),
    ROMANPIZZAS("ROMANPIZZAS"),
    CROISSANTS("CROISSANTS"),
    BREAKFASTS("BREAKFASTS"),
    FOODMENU("FOODMENU"),
    DECLINEPREORDERTIME("DECLINEPREORDERTIME"),
    CONFIRMPREORDERTIME("CONFIRMPREORDERTIME"),
    CONFIRMBOOKING("CONFIRMBOOKING"),
    RESERVATION("RESERVATION"),
    START("START");

    private final String callbackDataCommand;

    CallbackDataCommand(String command) {
        this.callbackDataCommand = command;
    }

    public String getCallbackDataCommand() {
        return callbackDataCommand;
    }

    public static CallbackDataCommand fromCallbackData(String commandText) {
        for (CallbackDataCommand command : CallbackDataCommand.values()) {
            if (command.getCallbackDataCommand().equals(commandText)) {
                return command;
            }
        }
        return null;
    }
}
