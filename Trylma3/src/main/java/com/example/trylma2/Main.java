package com.example.trylma2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/trylma2/server.fxml"));
        AnchorPane root = loader.load();
        ApplicationContext context = new ClassPathXmlApplicationContext("Beans.xml");
        JdbcTemplate iteratorTemplate = (JdbcTemplate) context.getBean("iteratorTemplate");
        String sql = "INSERT INTO iterator (game_name) VALUES ('a')";
        iteratorTemplate.update(sql);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Trylma Server");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
