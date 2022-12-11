package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;
    @Autowired
    NotificationTaskRepository notificationTaskRepository;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);

            String messageText = update.message().text();
            Long chatId = update.message().chat().id();
            String notification = update.message().text();
            String receivedMessage = update.message().text();
            String username = update.message().chat().username();
            String firstName = update.message().chat().firstName();
            String lastName = update.message().chat().lastName();


            if (messageText.startsWith("/start")) {

                sendMessage(chatId,
                        "Привет, я помогу тебе создать напоминание на определенную дату. " +
                                "Введи сообщение в формате дата + напоминание: " +
                                "01.01.2022  20:00 Сделать домашнюю работу");
            } else {

                Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
                Matcher matcher = pattern.matcher(messageText);


                if (matcher.matches()) {
                    String datetime = matcher.group(1);
                    String item = matcher.group(3);
                    LocalDateTime yearOfNotification = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).parse(datetime, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                    LocalDateTime today = LocalDateTime.now();
                    int enteredDate = yearOfNotification.getYear();
                    int currentDate = today.getYear();
                    if(enteredDate < currentDate ) {
                        sendMessage(chatId, "Введите корректную дату");
                    }

                    NotificationTask notificationTask = new NotificationTask(
                            chatId, item, firstName, lastName, yearOfNotification);

                    notificationTaskRepository.save(notificationTask);
                    sendMessage(chatId,"Уведомление принято в обработку");

                } else {
                    sendMessage(chatId, "Введите данные корректно.");
                }

            }
            ;

        });

        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void run() {
        logger.info("Проверка напоминания");
        LocalDateTime yearOfNotification = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<NotificationTask> allByDateTime = notificationTaskRepository.findAllByDateTime(yearOfNotification);

        for (NotificationTask notificationTask : allByDateTime) {
            sendMessage(notificationTask.getChatId(), notificationTask.getNotification());
        }
    }

    public void sendMessage(long chatId, String receivedMessage) {
        logger.info("Отправка сообщения");
        SendMessage message = new SendMessage(chatId, receivedMessage);
        SendResponse response = telegramBot.execute(message);
    }




}
