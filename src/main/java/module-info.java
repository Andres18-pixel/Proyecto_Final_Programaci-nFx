module com.tuempresa.ezencuestafx {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.tuempresa.ezencuestafx to javafx.fxml;
    opens com.tuempresa.ezencuestafx.controller to javafx.fxml;

    exports com.tuempresa.ezencuestafx;
    exports com.tuempresa.ezencuestafx.controller;
    exports com.tuempresa.ezencuestafx.model;
}