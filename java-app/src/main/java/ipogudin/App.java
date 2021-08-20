package ipogudin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//curl -vvv -X POST "http://localhost:8080/crawler" -H "Content-Type: application/json" -d '{"urls": {"ya": "https://ya.ru"}, "childrenLevel": 1, "childrenPattern": ".*yandex\\.ru.*"}'
@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

}
