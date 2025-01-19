package org.example.clouddatastorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CloudDataStorageApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudDataStorageApplication.class, args);
	}

} // вообще хотелось бы побольше абстракций, например можно сначала определять интерфейсы или абстрактные классы
  // а потом писать конкретные реализации
