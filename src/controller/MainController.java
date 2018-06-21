package controller;

import common.FileOperations;
import common.SVNOperations;
import common.ScreenValidations;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Paint;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import processor.SVNCheckout;
import processor.SVNSearch;

public class MainController implements Initializable {

    @FXML
    TextField svnConnectURL, svnConnectUser, svnOutDestPath, svnOutSpecPath, svnOutSpecWD, svnOutSpecExtensions, svnOutLinkDest, svnSearchDest;
    @FXML
    PasswordField svnConnectPassword;
    @FXML
    ChoiceBox svnOutType, svnOutSpecType, svnOutLinkType, svnOutDepth;
    @FXML
    CheckBox svnSearchCopy;
    @FXML
    Button svnOutButton, svnConnectButton, svnOutSpecButton, svnOutLinkButton, svnSearchButton;
    @FXML
    TextArea svnOutLog, svnOutSpecLog, svnOutLinkPath, svnOutLinkLog, svnSearchList, svnSearchLog;
    @FXML
    Label svnConnectMessage, svnOutDepthLabel;
    @FXML
    Tab tabExportCheckout, tabExportCheckout2, tabExportCheckout3, tabSearch;
    @FXML
    ToggleButton svnOutSpecExtensionsToggle;

    private void clearTextArea(TextArea subject) {
        subject.clear();
    }

    private void setScreenTabs() {
        String buttonText = svnConnectButton.getText();
        if (buttonText.equals("Connect")) {
            tabExportCheckout.setDisable(true);
            tabExportCheckout2.setDisable(true);
            tabExportCheckout3.setDisable(true);
            tabSearch.setDisable(true);
        } else if (buttonText.equals("Disconnect")) {
            tabExportCheckout.setDisable(false);
            tabExportCheckout2.setDisable(false);
            tabExportCheckout3.setDisable(false);
            tabSearch.setDisable(false);
        }
    }

    @FXML
    private void svnConnectButtonClick() {
        try {

            // Getting screen inputs
            String repoUrl = svnConnectURL.getText().trim();
            String username = svnConnectUser.getText().trim();
            String password = svnConnectPassword.getText().trim();
            String buttonText = svnConnectButton.getText();

            // Validation for SVN Link
            try {
                ScreenValidations.regularTextField(svnConnectURL, "SVN Url", true);
            } catch (Exception e) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Error");
                a.setHeaderText("SVN Repository Link Not Specified!");
                a.setContentText("Please enter the correct repository URL to continue.");
                a.show();
                throw new Exception();
            }

            // Background task of connecting to server            
            Service<Boolean> connectService = new Service<Boolean>() {
                @Override
                protected Task<Boolean> createTask() {
                    return new Task<Boolean>() {
                        @Override
                        protected Boolean call() {
                            boolean result = false;
                            try {
                                result = SVNOperations.session(repoUrl, username, password);
                            } catch (MalformedURLException mue) {
                                Platform.runLater(() -> {
                                    Alert a = new Alert(Alert.AlertType.ERROR);
                                    a.setTitle("Error");
                                    a.setHeaderText("Wrong URL Provided!");
                                    a.setContentText("Please check the URL, and try again (Example URL: http://www.sample-domain.com)");
                                    a.show();
                                });
                            } catch (SVNException se) {
                                Platform.runLater(() -> {
                                    Alert a = new Alert(Alert.AlertType.ERROR);
                                    a.setTitle("Error");
                                    a.setHeaderText("Connection Failed!");
                                    a.setContentText(SVNOperations.getSVNErrorMessage(se));
                                    a.show();
                                });
                            }
                            return result;
                        }
                    };
                }
            };

            // Background task of disconnecting from server
            Service<Void> disconnectService = new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>() {
                        @Override
                        protected Void call() {
                            try {
                                SVNOperations.destroySession();
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    Alert a = new Alert(Alert.AlertType.ERROR);
                                    a.setTitle("Error");
                                    a.setHeaderText("Failed To Disconnect!");
                                    a.setContentText("Disconnection failed due to an unknown reason. Please re-open the application.");
                                    a.show();
                                });
                            }
                            return null;
                        }
                    };
                }
            };

            // Background task to start populating the file tree under the repository, once connected
            Service<Boolean> listingService = new Service<Boolean>() {
                @Override
                protected Task<Boolean> createTask() {
                    return new Task<Boolean>() {
                        @Override
                        protected Boolean call() {
                            try {
                                SVNOperations.listFiles("");
                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        }
                    };
                }
            };

            connectService.setOnRunning(e -> {
                svnConnectURL.setDisable(true);
                svnConnectUser.setDisable(true);
                svnConnectPassword.setDisable(true);
                svnConnectButton.setDisable(true);
                svnConnectMessage.setTextFill(Paint.valueOf("BLACK"));
                svnConnectMessage.setText("Connecting...");
            });

            connectService.setOnSucceeded(e -> {
                svnConnectURL.setDisable(false);
                svnConnectUser.setDisable(false);
                svnConnectPassword.setDisable(false);
                svnConnectButton.setDisable(false);
                if (connectService.getValue()) {
                    svnConnectMessage.setTextFill(Paint.valueOf("GREEN"));
                    svnConnectMessage.setText("Connected to " + SVNOperations.getRepositoryPath());
                    svnConnectURL.setDisable(true);
                    svnConnectUser.setDisable(true);
                    svnConnectPassword.setDisable(true);
                    svnConnectButton.setText("Disconnect");
                    setScreenTabs();
                    listingService.start();
                } else {
                    svnConnectMessage.setTextFill(Paint.valueOf("BLACK"));
                    svnConnectMessage.setText("Enter Credentials To Connect To SVN Repository");
                }
            });

            disconnectService.setOnSucceeded(e -> {
                svnConnectURL.setDisable(false);
                svnConnectUser.setDisable(false);
                svnConnectPassword.setDisable(false);
                svnConnectButton.setDisable(false);
                svnConnectMessage.setTextFill(Paint.valueOf("BLACK"));
                svnConnectMessage.setText("Enter Credentials To Connect To SVN Repository");
                svnConnectButton.setText("Connect");
                setScreenTabs();
            });

            listingService.setOnRunning(e -> {
                svnSearchList.setDisable(true);
                svnSearchButton.setDisable(true);
                svnSearchLog.setText("Please Wait While The System Builds Up The File Index For Searching");
                svnSearchLog.appendText(FileOperations.NEWLINE);
                svnSearchLog.appendText("This Typically Takes 5-10 Minutes, Depending On The Repository Size!");
                svnSearchCopy.setDisable(true);
                svnSearchDest.setDisable(true);
            });

            listingService.setOnSucceeded(e -> {
                if (listingService.getValue()) {
                    svnSearchList.setDisable(false);
                    svnSearchButton.setDisable(false);
                    svnSearchCopy.setDisable(false);
                    svnSearchDest.setDisable(false);
                    clearTextArea(svnSearchLog);
                } else {
                    svnSearchLog.setText("Failed To Construct File Index For The Repository!");
                }
            });

            // Connect or Disconnect based on Button text
            if (buttonText.equals("Connect")) {
                connectService.start();
            } else if (buttonText.equals("Disconnect")) {
                disconnectService.start();
            }
        } catch (Exception e) {
        }
    }

    @FXML
    private void svnOutButtonClick() {
        clearTextArea(svnOutLog);
        try {
            // Getting screen inputs            
            String destPath = svnOutDestPath.getText().trim();
            String type = svnOutType.getValue().toString();
            String depth = svnOutDepth.getValue().toString();

            // Validations for Input
            try {
                ScreenValidations.directoryPathTextField(svnOutDestPath, "Destination Path", false);
            } catch (Exception e) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Error");
                a.setHeaderText("Initialization Failed!");
                a.setContentText(e.getMessage());
                a.show();
                throw e;
            }

            // Starting background task of correcting files
            Service<Void> service = new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>() {
                        @Override
                        protected Void call() {
                            try {
                                SVNCheckout checkout = new SVNCheckout(svnOutLog);
                                checkout.doCheckout(type, destPath, SVNDepth.fromString(depth.toLowerCase()));
                                Platform.runLater(() -> {
                                    svnOutLog.appendText(FileOperations.NEWLINE + FileOperations.NEWLINE + "Done Processing All Files!" + FileOperations.NEWLINE);
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    svnOutLog.appendText("Failed to perform operation!" + FileOperations.NEWLINE + e.getMessage() + FileOperations.NEWLINE);
                                });
                            }
                            return null;
                        }
                    };
                }
            };

            service.setOnRunning(e -> {
                svnOutButton.setText("Processing");
                svnOutButton.setDisable(true);
                svnOutDestPath.setDisable(true);
                svnOutType.setDisable(true);
                svnOutDepth.setDisable(true);
            });

            service.setOnSucceeded(e -> {
                svnOutButton.setDisable(false);
                svnOutButton.setText(svnOutType.getValue().toString());
                svnOutDestPath.setDisable(false);
                svnOutType.setDisable(false);
                svnOutDepth.setDisable(false);
            });
            service.start();
        } catch (Exception e) {
        }
    }

    @FXML
    private void svnOutSpecButtonClick() {
        clearTextArea(svnOutSpecLog);
        try {
            // Getting screen inputs          
            String path = svnOutSpecPath.getText().trim();
            String PWD = svnOutSpecWD.getText().trim();
            String type = svnOutSpecType.getValue().toString();
            String extensionFilters[] = svnOutSpecExtensions.getText().trim().split(",");
            boolean extensionInclude = svnOutSpecExtensionsToggle.isSelected();

            // Validations for input
            try {
                ScreenValidations.directoryPathTextField(svnOutSpecPath, "Source Path", true);
                ScreenValidations.directoryPathTextField(svnOutSpecWD, "Destination Path", false);
            } catch (Exception e) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Error");
                a.setHeaderText("Initialization Failed!");
                a.setContentText(e.getMessage());
                a.show();
                throw e;
            }

            // Starting background task
            Service<Void> service = new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>() {
                        @Override
                        protected Void call() {
                            try {
                                SVNCheckout checkoutSpec = new SVNCheckout(svnOutSpecLog);
                                checkoutSpec.doCheckout(type, PWD, path, extensionFilters, extensionInclude);
                                Platform.runLater(() -> {
                                    svnOutSpecLog.appendText(FileOperations.NEWLINE + FileOperations.NEWLINE + "Done Processing All Files!" + FileOperations.NEWLINE);
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    svnOutSpecLog.appendText("Failed to perform operation!" + FileOperations.NEWLINE + e.getMessage() + FileOperations.NEWLINE);
                                });
                            }
                            return null;
                        }
                    };
                }
            };

            service.setOnRunning(e -> {
                svnOutSpecButton.setText("Processing");
                svnOutSpecButton.setDisable(true);
                svnOutSpecPath.setDisable(true);
                svnOutSpecExtensions.setDisable(true);
                svnOutSpecWD.setDisable(true);
                svnOutSpecType.setDisable(true);
            });

            service.setOnSucceeded(e -> {
                svnOutSpecButton.setText("Process Files");
                svnOutSpecButton.setDisable(false);
                svnOutSpecPath.setDisable(false);
                svnOutSpecExtensions.setDisable(false);
                svnOutSpecWD.setDisable(false);
                svnOutSpecType.setDisable(false);
            });
            service.start();
        } catch (Exception e) {
        }
    }

    @FXML
    private void svnOutLinkButtonClick() {
        clearTextArea(svnOutLinkLog);
        try {
            // Getting screen inputs          
            String[] fileLinks = svnOutLinkPath.getText().trim().split("\\R+");
            String destinationPath = svnOutLinkDest.getText().trim();
            String type = svnOutLinkType.getValue().toString();

            // Validations for input
            try {
                ScreenValidations.directoryPathTextField(svnOutLinkDest, "Destination Path", false);
                ScreenValidations.regularTextArea(svnOutLinkPath, "Source File List", true);
            } catch (Exception e) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Error");
                a.setHeaderText("Initialization Failed!");
                a.setContentText(e.getMessage());
                a.show();
                throw e;
            }

            // Starting background task
            Service<Void> service = new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>() {
                        @Override
                        protected Void call() {
                            try {
                                SVNCheckout checkoutSpec = new SVNCheckout(svnOutSpecLog);
                                checkoutSpec.doCheckout(type, destinationPath, fileLinks);
                                Platform.runLater(() -> {
                                    svnOutLinkLog.appendText(FileOperations.NEWLINE + FileOperations.NEWLINE + "Done Processing All Files!" + FileOperations.NEWLINE);
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    svnOutLinkLog.appendText("Failed to perform operation!" + FileOperations.NEWLINE + e.getMessage() + FileOperations.NEWLINE);
                                });
                            }
                            return null;
                        }
                    };
                }
            };

            service.setOnRunning(e -> {
                svnOutLinkButton.setText("Processing");
                svnOutLinkButton.setDisable(true);
                svnOutLinkPath.setDisable(true);
                svnOutLinkDest.setDisable(true);
                svnOutLinkType.setDisable(true);
            });

            service.setOnSucceeded(e -> {
                svnOutLinkButton.setText(svnOutLinkType.getValue().toString());
                svnOutLinkButton.setDisable(false);
                svnOutLinkPath.setDisable(false);
                svnOutLinkDest.setDisable(false);
                svnOutLinkType.setDisable(false);
            });
            service.start();
        } catch (Exception e) {
        }
    }

    @FXML
    private void svnSearchButtonClick() {
        clearTextArea(svnSearchLog);
        try {
            // Getting screen inputs          
            String[] files = svnSearchList.getText().trim().split("\\R+");
            boolean copyFiles = svnSearchCopy.isSelected();
            String copyPath = svnSearchDest.getText().trim();

            // Validations for input
            try {
                ScreenValidations.regularTextArea(svnSearchList, "Search List", true);
                ScreenValidations.directoryPathTextField(svnSearchDest, "Destination Path", false);
            } catch (Exception e) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Error");
                a.setHeaderText("Initialization Failed!");
                a.setContentText(e.getMessage());
                a.show();
                throw e;
            }

            // Starting background task
            Service<Void> service = new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>() {
                        @Override
                        protected Void call() {
                            try {
                                SVNSearch search = new SVNSearch(svnSearchLog);
                                search.execute(copyPath, files, copyFiles);
                                Platform.runLater(() -> {
                                    svnSearchLog.appendText(FileOperations.NEWLINE + FileOperations.NEWLINE + "Done Processing All Files!" + FileOperations.NEWLINE);
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    svnSearchLog.appendText("Failed to perform operation!" + FileOperations.NEWLINE + e.getMessage() + FileOperations.NEWLINE);
                                });
                            }
                            return null;
                        }
                    };
                }
            };

            service.setOnRunning(e -> {
                svnSearchList.setDisable(true);
                svnSearchButton.setDisable(true);
                svnSearchDest.setDisable(true);
                svnSearchCopy.setDisable(true);
            });

            service.setOnSucceeded(e -> {
                svnSearchList.setDisable(false);
                svnSearchButton.setDisable(false);
                svnSearchDest.setDisable(false);
                svnSearchCopy.setDisable(false);
            });

            service.start();
        } catch (Exception e) {
        }
    }

    private void setExtensionFilter(TextField subject1, ToggleButton subject2) {
        if (subject1.getText().trim().isEmpty()) {
            subject2.setText("No Filters!");
            subject2.setStyle("-fx-background-color: #D3D3D3;");
            subject2.setSelected(true);
            subject2.setDisable(true);
        } else {
            subject2.setDisable(false);
            setExtensionFilterParameters(subject2);
        }
    }

    private void setExtensionFilterParameters(ToggleButton subject) {
        if (subject.isSelected()) {
            subject.setText("Include");
            subject.setStyle("-fx-background-color: #5cb85c;");
        } else {
            subject.setText("Exclude");
            subject.setStyle("-fx-background-color: #d9534f;");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // "Export/Checkout All" Initialization
        svnOutType.setItems(FXCollections.observableArrayList("Checkout", "Export"));
        svnOutType.setValue("Checkout");
        svnOutDepth.setItems(FXCollections.observableArrayList("Empty", "Files", "Immediates", "Infinity"));
        svnOutDepth.setValue("Infinity");
        svnOutType.setOnAction((event) -> {
            svnOutButton.setText(svnOutType.getValue().toString());
            svnOutDepthLabel.setText(svnOutType.getValue().toString() + " Depth");
        });

        // "Export/Checkout With Reference" Initialization
        svnOutSpecType.setItems(FXCollections.observableArrayList("Checkout", "Export"));
        svnOutSpecType.setValue("Checkout");
        svnOutSpecType.setOnAction((event) -> {
            svnOutSpecButton.setText(svnOutSpecType.getValue().toString());
        });
        svnOutSpecExtensions.setOnKeyReleased(event -> {
            setExtensionFilter(svnOutSpecExtensions, svnOutSpecExtensionsToggle);
        });
        svnOutSpecExtensionsToggle.setOnAction(event -> {
            setExtensionFilterParameters(svnOutSpecExtensionsToggle);
        });

        // "Export/Checkout With Links" Initialization
        svnOutLinkType.setItems(FXCollections.observableArrayList("Checkout", "Export"));
        svnOutLinkType.setValue("Checkout");
        svnOutLinkType.setOnAction((event) -> {
            svnOutLinkButton.setText(svnOutLinkType.getValue().toString());
        });
    }
}
