package org.example.clouddatastorage.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.example.clouddatastorage.Entity.FileEntity;
import org.example.clouddatastorage.Entity.User;
import org.example.clouddatastorage.Repository.FileRepository;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileService  {

    private final AmazonS3 s3client;
    private final FileRepository fileRepository;
    private final Map<Long, Boolean> pendingFileSelection = new HashMap<>();
    private final String bucketName = "cloudstoragedatabase";

    // @RequiredArgsConstructor
    public FileService(AmazonS3 s3client, FileRepository fileRepository) {
        this.s3client = s3client;
        this.fileRepository = fileRepository;
    }

    public String uploadFileToS3(InputStream fileStream,
                                 String fileName,
                                 String fileType,
                                 long contentLength,
                                 String username) throws Exception {

        if (!s3client.doesBucketExistV2(bucketName)) {
            throw new Exception("Указанный бакет не существует: " + bucketName);
        }

        try {
            System.out.println("Список бакетов: " + s3client.listBuckets()); // logger

        } catch (Exception e) {
            System.out.println("Ошибка при получении списка бакетов: " + e.getMessage());
        }

        String s3Key = "Uploads/" + username + "/" + fileName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(fileType);
        metadata.setContentLength(contentLength);

        if (fileStream.available() <= 0) {
            throw new Exception("InputStream пуст или недоступен");
        }

        //доступ к файлам
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, s3Key, fileStream, metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead);

        // Загрузка в S3
        try {
            s3client.putObject(putObjectRequest);

        } catch (AmazonS3Exception e) {
            System.err.println("Error uploading file: " + e.getMessage());
        }

        return s3client.getUrl(bucketName, s3Key).toString();
    }

    public void saveFileDataBase(Long userId, String fileName, String fileType, String s3FilePath, User user) {

        // Обрезаем название если слишком большое
        if (fileName.length() > 255) { // сомнительное продуктовое решение
            fileName = fileName.substring(0, 255);
        }

        if (s3FilePath.length() > 255) { // сомнительное продуктовое решение
            s3FilePath = s3FilePath.substring(0, 255);
        }

        FileEntity file = new FileEntity();
        file.setFileName(fileName);
        file.setFormat(fileType);
        file.setFilePath(s3FilePath);
        file.setUser(user);

        fileRepository.save(file);
    }

    public InputStream getFileFromS3(String filePath) throws Exception {

        try {
            String objectKey = filePath.replace("https://storage.yandexcloud.net/cloudstoragedatabase/", ""); // что за пиздец
            String decodedObjectKey = URLDecoder.decode(objectKey, StandardCharsets.UTF_8.toString()); // нужен ли toString()

            S3Object s3Object = s3client.getObject(bucketName, decodedObjectKey);

            return s3Object.getObjectContent();

        } catch (AmazonServiceException e) { // слишком обобщенное исключение, можно разбить на маленькие и избавиться от ифов

            if ("NoSuchKey".equals(e.getErrorCode())) {
                System.out.println("Файл по указанному пути не найден: " + filePath);

            } else {
                System.out.println("Ошибка сервиса Amazon: " + e.getErrorMessage());
            }

            throw new Exception("Ошибка сервиса Amazon при получении файла из S3: " + e.getMessage());

        } catch (SdkClientException e) {
            System.out.println("Ошибка клиента SDK: " + e.getMessage());
            throw new Exception("Ошибка клиента SDK при получении файла из S3: " + e.getMessage());

        } catch (Exception e) {
            System.out.println("Общая ошибка: " + e.getMessage());
            throw new Exception("Ошибка при получении файла из S3: " + e.getMessage());
        }
    }

    //Состояние ожидания
    public void savePendingFileSelection(Long chatId) {
        pendingFileSelection.put(chatId, true);
    }

    //Проверка состояние ожидания
    public boolean isPendingFileSelection(Long chatId) {
        return pendingFileSelection.getOrDefault(chatId, false);
    }

    public void clearPendingFileSelection(Long chatId) {
        pendingFileSelection.remove(chatId);
    }

    public List<FileEntity> getFilesByUserId(Long userId) {
        return fileRepository.findByUserId(userId);
    }
}
