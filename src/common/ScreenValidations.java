package common;

import java.io.File;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ScreenValidations {

    public static void filePathTextField(TextField subject, String fieldName, boolean shouldExist) throws Exception {
        String value = subject.getText().trim();

        // Mandatory Check
        if (value.isEmpty()) {
            throw new Exception(fieldName + " is blank!");
        }

        // File Related Checks
        File f = new File(value);
        if (shouldExist && !f.exists()) {
            throw new Exception(fieldName + " does not exist!");
        } else if (!shouldExist && f.exists()) {
            throw new Exception(fieldName + " already exists!");
        }

        if (f.exists() && !f.isFile()) {
            throw new Exception(fieldName + " does not look like a \"File\" path!");
        }
    }

    public static void directoryPathTextField(TextField subject, String fieldName, boolean shouldExist) throws Exception {
        String value = subject.getText().trim();

        // Mandatory Check
        if (value.isEmpty()) {
            throw new Exception(fieldName + " is blank!");
        }

        // File Related Checks
        File f = new File(value);
        if (shouldExist && !f.exists()) {
            throw new Exception(fieldName + " does not exist!");
        } else if (!shouldExist && f.exists()) {
            throw new Exception(fieldName + " already exists!");
        }

        if (f.exists() && !f.isDirectory()) {
            throw new Exception(fieldName + " does not look like a \"Directory\" path!");
        }
    }

    public static void regularTextField(TextField subject, String fieldName, boolean mandatory) throws Exception {
        String value = subject.getText().trim();

        // Mandatory Check
        if (mandatory && value.isEmpty()) {
            throw new Exception(fieldName + " is blank!");
        }
    }

    public static void regularTextArea(TextArea subject, String fieldName, boolean mandatory) throws Exception {
        String value = subject.getText().trim();

        // Mandatory Check
        if (mandatory && value.isEmpty()) {
            throw new Exception(fieldName + " is blank!");
        }
    }
}
