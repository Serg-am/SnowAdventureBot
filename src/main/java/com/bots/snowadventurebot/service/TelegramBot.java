package com.bots.snowadventurebot.service;

import com.bots.snowadventurebot.config.BotConfig;
import com.bots.snowadventurebot.model.User;
import com.bots.snowadventurebot.model.UserRepository;
import com.bots.snowadventurebot.utils.RegionEntity;
import com.bots.snowadventurebot.utils.ResortEntity;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Service
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    private final ResortService resortService;
    private final RegionService regionService;
    private final BotConfig config;
    private int regionId = 0;
    private boolean rideSwitch = false;
    static final String HELP_TEXT = "Бот который показывает какие горнолыжные курорты существуют и где они находятся";
    static final String TEMP_ANSWER = "Извините... мой создатель еще не научил меня что делать с этой командой";
    static final String ERROR_TEXT = "Error occurred: ";

    public TelegramBot(ResortService resortService, RegionService regionService, BotConfig config) {
        this.resortService = resortService;
        this.regionService = regionService;
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Давайте начнем"));
        listOfCommands.add(new BotCommand("/ride", "Посмотрим курорты "));
        listOfCommands.add(new BotCommand("/weather", "Погода на склонах"));
        listOfCommands.add(new BotCommand("/help", "Помощь"));
        try{
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken(){
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if(messageText.contains("/send") && config.getOwnerId() == chatId) {
                String textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                Iterable<User> users = userRepository.findAll();
                for (User user: users){
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else {
                switch (messageText) {
                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/ride":
                        rideRegion(chatId);
                        break;
                    case "/weather":
                        prepareAndSendMessage(chatId, TEMP_ANSWER);
                        break;
                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;
                    default:
                        prepareAndSendMessage(chatId, TEMP_ANSWER);
                }
            }
        } else if(update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if(rideSwitch && Integer.parseInt(callBackData) > 0) {
                rideResort(chatId, Integer.parseInt(callBackData));
                return;
            }
            String text = returnTextDb(callBackData);

            executeEditMessageText(text, chatId, messageId);

        }
    }

    private String returnTextDb(String callBackData) {
        int id = Integer.parseInt(callBackData);
        String result;
        ResortEntity resort = resortService.getByResortId(id);
        result = resort.getResortName() + "\n\n"
                + resort.getResortDescription() + "\n"
                + "Контактный телефон: " + resort.getResortTelephone() + "\n"
                + "Сайт: " + resort.getResortWebSite();

        return result;
    }


    private void rideRegion(long chatId) {
        rideSwitch = true;
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выбери локацию:");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        for (RegionEntity entity : regionService.getAll()) {
            List<InlineKeyboardButton> rowInLine = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(entity.getRegionName());
            button.setCallbackData(String.valueOf(entity.getRegionId()));
            rowInLine.add(button);
            rowsInLine.add(rowInLine);
        }

        markupInLine.setKeyboard(rowsInLine);

        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }


    private void rideResort(long chatId, int regionId){
        rideSwitch = false;
        System.out.println(regionId);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выбери курорт:");


        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        for (ResortEntity entity : resortService.getAll(regionId)) {
            List<InlineKeyboardButton> rowInLine = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(entity.getResortName());
            button.setCallbackData(String.valueOf(entity.getResortId()));
            rowInLine.add(button);

            rowsInLine.add(rowInLine);
        }
        markupInLine.setKeyboard(rowsInLine);

        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void registerUser(Message message) {
        if(userRepository.findById(message.getChatId()).isEmpty()) {
            Long chatId = message.getChatId();
            Chat chat = message.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String firstName){
        String answer = EmojiParser.parseToUnicode("Привет, " + firstName + ", рад что ты присоединился к нам! =)" + ":snowboarder:");

        prepareAndSendMessage(chatId, answer);
    }

    //Пока не используем, использовать когда нужны будут всплывающие кнопки
    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        message.setReplyMarkup(replayKeyBoard());

        executeMessage(message);
    }

    public ReplyKeyboardMarkup replayKeyBoard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("temp1");
        row.add("temp2");

        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("temp3");
        row.add("temp4");
        row.add("temp5");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        return keyboardMarkup;
    }

    private void executeEditMessageText(String text, long chatId, long messageId){
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId((int) messageId);

        try{
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message) {
        try{
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        executeMessage(message);
    }
}
