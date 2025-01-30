# Облачное хранилище на основе Telegram-бота и Yandex.Cloud S3

Это проект облачного хранилища, которое использует Telegram-бота для взаимодействия с пользователями. Пользователи могут загружать и выгружать файлы, такие как видео, текстовые и фото файлы. Все файлы сохраняются в облаке Yandex.Cloud с использованием S3, и каждый пользователь имеет свой уникальный доступ к файлам через Telegram-бота.

## Основные возможности

- **Загрузка файлов**: Пользователь может отправить Telegram-боту файлы (видео, текстовые или изображения), и они будут сохранены в облаке Yandex S3.
- **Хранение файлов по уникальному имени**: Все файлы сохраняются в облаке под уникальным именем пользователя, что позволяет каждому пользователю иметь свой набор данных.
- **Выгрузка файлов**: Пользователь может запросить список всех своих загруженных файлов, и бот выведет список с нумерацией. Для загрузки файла достаточно выбрать номер.
- **Поддержка различных типов файлов**: Бот поддерживает работу с видео, изображениями и текстовыми файлами.


## Стек технологий

Проект использует следующие технологии и зависимости:

- **Spring Boot** — фреймворк для создания веб-приложений:

- **Lombok** — библиотека для упрощения кода Java (автоматическая генерация геттеров, сеттеров, конструкторов и других методов).

- **PostgreSQL** — реляционная база данных, используемая для хранения данных приложения.

- **Spring Data JPA** — для удобной работы с базой данных 

- **Liquibase** — инструмент для управления версиями базы данных и миграциями.

- **Telegram Bots API** — библиотека для создания Telegram-ботов:
  - `telegrambots-spring-boot-starter`: интеграция с Spring Boot для удобной работы с Telegram API.

- **Yandex.Cloud S3 (AWS S3 SDK)** — SDK для работы с хранилищем файлов в Yandex.Cloud, основанном на протоколе AWS S3:
  - `aws-java-sdk-s3`: для взаимодействия с хранилищем S3 (загрузка и выгрузка файлов).