package org.example.clouddatastorage.TelegramBot;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import org.example.clouddatastorage.Entity.FileEntity;
import org.example.clouddatastorage.Entity.User;
import org.example.clouddatastorage.Service.FileService;
import org.example.clouddatastorage.Service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


@Component
public class BotCommands extends TelegramLongPollingBot {

    private final UserService userService;
    private final FileService fileService;

    public BotCommands(UserService userService, FileService fileService) {

        this.userService = userService;
        this.fileService = fileService;

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);

        } catch (Exception e) {
            System.out.println("Ошибка регистрации бота");
            e.printStackTrace();
        }

    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            String username = message.getFrom().getUserName();

            if (message.hasText() && fileService.isPendingFileSelection(chatId)) {
                handleFileSelection(chatId, message.getText());

                return;
            }

            // Обработка текстовых сообщений
            if (message.hasText()) {
                String messageText = message.getText();
                handleTextMessage(messageText, chatId, username);
            }

            if (message.hasDocument() || message.hasPhoto() || message.hasVideo()) {
                handleFileMessage(message, chatId, username);
            }
        }
    }

    private void handleTextMessage(String messageText, Long chatId, String username) {

        if (messageText.equals("/start")) {
            try {

                User user = userService.findUserById(chatId);
                if (user == null) {
                    userService.registerUser(chatId, username);
                    System.out.println("Пользователь зарегистрирован: " + username);

                } else {
                    System.out.println("Пользователь уже существует: " + username); // !logger!
                }

            } catch (RuntimeException e) {
                System.out.println("Ошибка при регистрации пользователя: " + e.getMessage());
            }

            sendTextMessage(chatId, "Привет! Вы можете отправить файлы мне на хранение прямо сейчас или скачать файлы командой /download");

        } else if (messageText.equals("/download")) {
            handleDownloadCommand(chatId);
        }

    }

    private void handleDownloadCommand(Long chatId){

        try{

            User user = userService.findUserById(chatId);

            if (user == null){
                sendTextMessage(chatId, "Сначала зарегистрируйтесь с помощью команды /start"); // !enum!
                return;
            }

            List<FileEntity> userFiles = fileService.getFilesByUserId(user.getId());

            if(userFiles.isEmpty()){
                sendTextMessage(chatId, "У вас нет загруженных файлов");
                return;
            }

            StringBuilder messageText = new StringBuilder("Ваши файлы:\n");

            for(int i = 0; i < userFiles.size(); i++){
                messageText.append(i + 1).append(". ").append(userFiles.get(i).getFileName()).append("\n");
            }

            messageText.append("Выберите номер файла для загрузки");
            sendTextMessage(chatId, messageText.toString());

            fileService.savePendingFileSelection(chatId);

        } catch (Exception e){
            System.out.println("Ошибка при обработке команды /download: " + e.getMessage());
            sendTextMessage(chatId, "Произошла ошибка при обработке команды.");
            e.printStackTrace(); // !logger!
        }

    }

    private void handleFileSelection(Long chatId, String messageText) {

        try {

            int fileIndex = Integer.parseInt(messageText) - 1;
            User user = userService.findUserById(chatId);

            if (user == null) { // !Optional<User>.isPresent()!
                sendTextMessage(chatId, "Пользователь не найден.");
                return;
            }

            List<FileEntity> userFiles = fileService.getFilesByUserId(user.getId());

            if (fileIndex < 0 || fileIndex >= userFiles.size()) {
                sendTextMessage(chatId, "Неверный номер файла.");
                return;
            }

            FileEntity selectedFile = userFiles.get(fileIndex);
            sendDocument(chatId, selectedFile.getFilePath(), selectedFile.getFileName());

            fileService.clearPendingFileSelection(chatId);

        } catch (NumberFormatException e) {
            sendTextMessage(chatId, "Введите корректный номер файла.");

        } catch (Exception e) {
            sendTextMessage(chatId, "Произошла ошибка при отправке файла.");
        }

    }

    private void sendDocument(Long chatId, String filePath, String fileName) {

        try {

            InputStream s3FileStream = fileService.getFileFromS3(filePath);

            InputFile inputFile = new InputFile();
            inputFile.setMedia(s3FileStream, fileName);

            SendDocument sendDocumentRequest = new SendDocument();
            sendDocumentRequest.setChatId(chatId.toString());
            sendDocumentRequest.setDocument(inputFile);
            sendDocumentRequest.setCaption("Ваш файл: " + fileName);

            execute(sendDocumentRequest);

            s3FileStream.close();

        } catch (TelegramApiException | IOException e) {
            sendTextMessage(chatId, "Ошибка при отправке файла");
            e.printStackTrace();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void handleFileMessage(Message message, Long chatId, String username) {

        String fileType = ""; // !!!
        String fileName = ""; // !!!
        String fileId = ""; // !!!
        InputStream fileStream = null; // !!!
        ByteArrayOutputStream outputStream = null; // !!!

        try {

            if (message.hasPhoto()) {
                List<PhotoSize> photos = message.getPhoto();
                PhotoSize photoLargest = photos.get(photos.size() - 1);
                fileId = photoLargest.getFileId();
                fileType = "image/jpeg"; // enum
                fileName = fileId + ".jpg";

            } else if (message.hasDocument()) {
                fileId = message.getDocument().getFileId();
                fileType = message.getDocument().getMimeType();
                fileName = message.getDocument().getFileName();

            } else if (message.hasVideo()) {
                fileId = message.getVideo().getFileId();
                fileType = "video/mp4";
                fileName = fileId + ".mp4"; // enum
            }

            File file = execute(new org.telegram.telegrambots.meta.api.methods.GetFile(fileId));
            String filePath = file.getFilePath();

            fileStream = downloadFileAsStream(filePath);
            outputStream = new ByteArrayOutputStream();

            // Буферизируем поток для нормального качества данных
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fileStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] fileData = outputStream.toByteArray();
            int contentLength = fileData.length;

            if (contentLength <= 0) {
                throw new Exception("Не удалось получить длину содержимого файла");
            }

            InputStream finalFileStream = new ByteArrayInputStream(fileData);

            // Загружаем файл в S3
            String s3FilePath = fileService.uploadFileToS3(finalFileStream,
                                                           fileName,
                                                           fileType,
                                                           contentLength,
                                                           username);

            User user = userService.findUserById(chatId);

            if (user == null) { // Optional
                userService.registerUser(chatId, username);
                user = userService.findUserById(chatId);
            }

            // Сохраняем в базе данных
            fileService.saveFileDataBase(user.getId(), fileName, fileType, s3FilePath, user);

            sendTextMessage(chatId, "Файл загружен успешно!");

        } catch (AmazonServiceException e) {
            System.out.println("Ошибка сервиса Amazon: " + e.getMessage());
            e.printStackTrace();

        } catch (SdkClientException e) {
            System.out.println("Ошибка клиента SDK: " + e.getMessage()); // logger
            e.printStackTrace();

        } catch (Exception e) {
            System.out.println("Ошибка при сохранении файла: " + e.getMessage());
            e.printStackTrace(); // вообще это записывается в логи примерно так log.error(message, exception)
            // хочется отличать исключения из бд и передавать в логи контекст исключения, например значения полей объектов
        }

    }


    private void sendTextMessage(Long id, String text) {

        SendMessage message = new SendMessage();
        message.setChatId(id.toString());
        message.setText(text);

        try {
            execute(message);

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getBotToken(){
        return "7290870977:AAFJShzNhSPslBEtsmGKZRffoaUaqK025ok";
    } // это пиздец

    @Override
    public String getBotUsername(){
        return "CloudDataStorage";
    } // это пиздец
}
