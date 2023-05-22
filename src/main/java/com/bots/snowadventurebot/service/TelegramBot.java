package com.bots.snowadventurebot.service;

import com.bots.snowadventurebot.config.BotConfig;
import com.bots.snowadventurebot.model.User;
import com.bots.snowadventurebot.repositories.UserRepository;
import com.bots.snowadventurebot.model.RegionEntity;
import com.bots.snowadventurebot.model.ResortEntity;
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
    private boolean rideSwitch = false;
    private boolean weatherSwitch = false;
    private boolean answerRideSwitch = false;
    private boolean answerWeatherSwitch = false;
    private String textCallbackQuery = "Error";
    static final String START_MESSAGE = "Привет, %s, рад тебя видеть! =)\uD83C\uDFC2"
            + "\nДавай я тебе расскажу немного про себя.\nЯ бот\uD83E\uDD2B\uD83E\uDD10\uD83E\uDD23 Теперь когда ты знаешь мою тайну я расскажу вот что:\n" +
            "Есди ты напишешь мне /ride - я покажу в каких областях нашей страны есть крутые горнолыжние курорты.\n" +
            "А если напишешь /weather - покажу какая там погода на ближайшие дни\n" +
            "Также есть /help - там ничего интересного, но на всякий случай там помощь... хотя по моему мнению, если тебе интересно мнение бота, помощь там сомнительная, я ведь и так тебе все расказал\uD83D\uDE09\n" +
            "Хороших тебе покатушек⛷\uD83C\uDFC2⛷\uD83C\uDFC2⛷\uD83C\uDFC2";
    static final String HELP_TEXT = "Я бот который показывает какие горнолыжные курорты существуют. У меня есть ссылки на сайты, и номера телефонов, что бы ты мог подробнее все узнать.\n" +
            "/ride - Посмотреть список областей и какие есть склоны\n" +
            "/weather - Погода на склонах\n" +
            "/help - Ты снова попадешь ко мне в помощь\uD83D\uDE09\n";
    static final String TEMP_ANSWER = "Извини... мой создатель еще не научил меня что делать с этой командой";
    static final String ERROR_TEXT = "Error occurred: ";
    private final OpenWeatherMapJsonParser openWeatherMapJsonParser;

    public TelegramBot(ResortService resortService, RegionService regionService, BotConfig config, OpenWeatherMapJsonParser openWeatherMapJsonParser) {
        this.resortService = resortService;
        this.regionService = regionService;
        this.config = config;
        this.openWeatherMapJsonParser = openWeatherMapJsonParser;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Давай начнем"));
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
        return config.getName();
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

            if(messageText.contains("/send") && config.getOwner() == chatId) {
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
                        rideSwitch = true;
                        rideRegion(chatId);
                        break;
                    case "/weather":
                        weatherSwitch = true;
                        rideRegion(chatId);
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

            if(rideSwitch) {
                executeEditMessageText(returnRegionDb(callBackData) + ":", chatId, messageId);
                rideResort(chatId, Integer.parseInt(callBackData));
                answerRideSwitch = true;
                return;
            } else if (weatherSwitch) {
                executeEditMessageText(returnRegionDb(callBackData) + ":", chatId, messageId);
                rideResort(chatId, Integer.parseInt(callBackData));
                weatherSwitch = false;
                answerWeatherSwitch = true;
                return;
            }
            if(answerRideSwitch){
                answerRideSwitch = false;
                textCallbackQuery = returnTextDb(callBackData);
            } else if (answerWeatherSwitch) {
                answerWeatherSwitch = false;
                textCallbackQuery = openWeatherMapJsonParser.getReadyForecast(returnWeatherRegion(callBackData));
            }

            executeEditMessageText(textCallbackQuery, chatId, messageId);
        }
    }

    private String returnWeatherRegion(String callBackData){
        int id = Integer.parseInt(callBackData);
        ResortEntity resort = resortService.getByResortId(id);
        return resort.getWeatherRegion();

    }
    private String returnTextDb(String callBackData) {
        int id = Integer.parseInt(callBackData);
        String result;
        ResortEntity resort = resortService.getByResortId(id);
        result = resort.getResortName() + "\n\n"
                + resort.getResortDescription() + "\n\n"
                + "Контактный телефон: " + resort.getResortTelephone() + "\n\n"
                + "Сайт: " + resort.getResortWebSite();
        return result;
    }

    private String returnRegionDb(String callBackData) {
        int id = Integer.parseInt(callBackData);
        RegionEntity region = regionService.getByResortId(id);
        return region.getRegionName();
    }


    private void rideRegion(long chatId) {
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
        prepareAndSendMessage(chatId, String.format(START_MESSAGE, firstName));
    }

    //Пока не используем, использовать когда нужны будут всплывающие кнопки
    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        message.setReplyMarkup(replayKeyBoard());

        executeMessage(message);
    }

    //Пока не используем, использовать когда нужны будут всплывающие кнопки
    public ReplyKeyboardMarkup replayKeyBoard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Вернуться назад");
        //row.add("temp2");

        keyboardRows.add(row);

        //row = new KeyboardRow();
        //row.add("temp3");
        //row.add("temp4");
        //row.add("temp5");

        //keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);

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
