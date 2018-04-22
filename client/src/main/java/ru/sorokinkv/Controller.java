package ru.sorokinkv;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean authorized;
    private String nick;
    private ObservableList<String> clientsList;

    @FXML
    TextField msgField;

    @FXML
    TextArea mainTextArea;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passField;

    @FXML
    HBox authPanel, msgPanel;

    @FXML
    ListView<String> clientsView;

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
        if (this.authorized) {
            authPanel.setVisible(false);
            authPanel.setManaged(false);
            msgPanel.setVisible(true);
            msgPanel.setManaged(true);
            clientsView.setVisible(true);
            clientsView.setManaged(true);
        } else {
            authPanel.setVisible(true);
            authPanel.setManaged(true);
            msgPanel.setVisible(false);
            msgPanel.setManaged(false);
            clientsView.setVisible(false);
            clientsView.setManaged(false);
            nick = "";
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
        clientsList = FXCollections.observableArrayList();
        clientsView.setItems(clientsList);

    }

    public void sendMsg() {
        try {
            out.writeUTF(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCustomMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/authok ")) {
                                nick = str.split(" ")[1];
                                setAuthorized(true);
                                sendCustomMsg("/history");
                                break;
                            }
                            mainTextArea.appendText(str);
                            mainTextArea.appendText("\n");
                        }
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/")) {
                                if (str.startsWith("/clientslist ")) {
                                    String[] tokens = str.split(" ");
                                    Platform.runLater(() -> {
                                        clientsList.clear();
                                        for (int i = 1; i < tokens.length; i++) {
                                            clientsList.add(tokens[i]);
                                        }
                                    });
                                }
                                continue;
                            }
                            mainTextArea.appendText(str);
                            mainTextArea.appendText("\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        showAlert("Произошло отключение от сервера");
                        setAuthorized(false);
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passField.getText());
            loginField.clear();
            passField.clear();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Невозможно отправить сообщение, проверьте сетевое соединение...");
        }
    }

    public void showAlert(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            alert.showAndWait();
        });
    }

    public void clickClientsList(MouseEvent mouseEvent) {
        if(mouseEvent.getClickCount() == 2) {
            String str = clientsView.getSelectionModel().getSelectedItem();
            msgField.setText("/w " + str + " ");
            msgField.requestFocus();
            msgField.selectEnd();
        }
    }
}
