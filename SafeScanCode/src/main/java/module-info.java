module com.imagetotext.imagetotexttest {
    requires javafx.controls;
    requires javafx.fxml;
    requires tess4j;
    requires java.net.http;
    requires org.json;


    opens com.HalalFoodTracker.SafaScan to javafx.fxml;
    exports com.HalalFoodTracker.SafaScan;
}