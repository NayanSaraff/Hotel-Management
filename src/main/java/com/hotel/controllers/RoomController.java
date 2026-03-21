package com.hotel.controllers;

import com.hotel.model.Room;
import com.hotel.service.RoomService;
import com.hotel.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Controller for Room Management module.
 */
public class RoomController {

    // ── Table ─────────────────────────────────────────────────────────────
    @FXML
    private TableView<Room> roomTable;
    @FXML
    private TableColumn<Room, String> colRoomNumber;
    @FXML
    private TableColumn<Room, String> colCategory;
    @FXML
    private TableColumn<Room, Integer> colFloor;
    @FXML
    private TableColumn<Room, Integer> colCapacity;
    @FXML
    private TableColumn<Room, Double> colPrice;
    @FXML
    private TableColumn<Room, String> colStatus;
    @FXML
    private TableColumn<Room, String> colBedType;

    // ── Form ──────────────────────────────────────────────────────────────
    @FXML
    private VBox formPane;
    @FXML
    private TextField roomNumberField;
    @FXML
    private ComboBox<String> categoryCombo;
    @FXML
    private TextField floorField;
    @FXML
    private TextField capacityField;
    @FXML
    private TextField priceField;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private ComboBox<String> bedTypeCombo;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private CheckBox acCheckBox;
    @FXML
    private CheckBox wifiCheckBox;
    @FXML
    private CheckBox tvCheckBox;

    // ── Toolbar ───────────────────────────────────────────────────────────
    @FXML
    private TextField searchField;
    @FXML
    private Button addButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Label formTitleLabel;

    private final RoomService roomService = new RoomService();
    private Room selectedRoom = null;
    private boolean isEditing = false;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();
        loadRooms();
        showForm(false);

        roomTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    selectedRoom = newVal;
                    deleteButton.setDisable(newVal == null);
                });

        searchField.textProperty().addListener((obs, old, val) -> filterRooms(val));
    }

    private void setupTableColumns() {
        colRoomNumber.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory().name()));
        colFloor.setCellValueFactory(new PropertyValueFactory<>("floor"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("pricePerNight"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colBedType.setCellValueFactory(new PropertyValueFactory<>("bedType"));

        // Colour-code status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                switch (item) {
                    case "AVAILABLE" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    case "OCCUPIED" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    case "MAINTENANCE" -> setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    case "HOUSEKEEPING" -> setStyle("-fx-text-fill: #8e44ad; -fx-font-weight: bold;");
                    default -> setStyle("");
                }
            }
        });
        roomTable.getColumns().forEach(col -> col.setResizable(false));
        roomTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupComboBoxes() {
        categoryCombo.setItems(FXCollections.observableArrayList("STANDARD", "DELUXE", "SUITE", "PRESIDENTIAL"));
        statusCombo.setItems(FXCollections.observableArrayList("AVAILABLE", "OCCUPIED", "MAINTENANCE", "HOUSEKEEPING"));
        bedTypeCombo.setItems(FXCollections.observableArrayList("SINGLE", "DOUBLE", "QUEEN", "KING", "TWIN"));
    }

    private void loadRooms() {
        List<Room> rooms = roomService.getAllRooms();
        roomTable.setItems(FXCollections.observableArrayList(rooms));
    }

    private void filterRooms(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            loadRooms();
            return;
        }
        List<Room> filtered = roomService.getAllRooms().stream()
                .filter(r -> r.getRoomNumber().toLowerCase().contains(keyword.toLowerCase())
                        || r.getCategory().name().toLowerCase().contains(keyword.toLowerCase())
                        || r.getStatus().name().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
        roomTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleAdd() {
        selectedRoom = null;
        isEditing = false;
        clearForm();
        formTitleLabel.setText("Add New Room");
        showForm(true);
    }

    @FXML
    private void handleEdit() {
        selectedRoom = roomTable.getSelectionModel().getSelectedItem();
        if (selectedRoom == null) {
            AlertUtil.showWarning("Select Room", "Please select a room to edit.");
            return;
        }
        isEditing = true;
        populateForm(selectedRoom);
        formTitleLabel.setText("Edit Room – " + selectedRoom.getRoomNumber());
        showForm(true);
    }

    @FXML
    private void handleSave() {
        try {
            Room room = buildRoomFromForm();
            if (isEditing && selectedRoom != null) {
                room.setRoomId(selectedRoom.getRoomId());
                roomService.updateRoom(room);
                AlertUtil.showInfo("Updated", "Room updated successfully.");
            } else {
                int id = roomService.addRoom(room);
                if (id < 0)
                    throw new Exception("Failed to add room.");
                AlertUtil.showInfo("Added", "Room added successfully.");
            }
            loadRooms();
            showForm(false);
        } catch (Exception e) {
            AlertUtil.showError("Validation Error", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedRoom == null)
            return;
        if (AlertUtil.showConfirm("Delete Room", "Delete room " + selectedRoom.getRoomNumber() + "?")) {
            roomService.deleteRoom(selectedRoom.getRoomId());
            loadRooms();
            showForm(false);
        }
    }

    @FXML
    private void handleCancel() {
        showForm(false);
    }

    private Room buildRoomFromForm() {
        Room r = new Room();
        r.setRoomNumber(roomNumberField.getText().trim());
        r.setCategory(Room.Category.valueOf(categoryCombo.getValue()));
        r.setFloor(Integer.parseInt(floorField.getText().trim()));
        r.setCapacity(Integer.parseInt(capacityField.getText().trim()));
        r.setPricePerNight(Double.parseDouble(priceField.getText().trim()));
        r.setStatus(Room.Status.valueOf(statusCombo.getValue()));
        r.setBedType(bedTypeCombo.getValue());
        r.setDescription(descriptionArea.getText());
        r.setHasAC(acCheckBox.isSelected());
        r.setHasWifi(wifiCheckBox.isSelected());
        r.setHasTV(tvCheckBox.isSelected());
        return r;
    }

    private void populateForm(Room r) {
        roomNumberField.setText(r.getRoomNumber());
        categoryCombo.setValue(r.getCategory().name());
        floorField.setText(String.valueOf(r.getFloor()));
        capacityField.setText(String.valueOf(r.getCapacity()));
        priceField.setText(String.valueOf(r.getPricePerNight()));
        statusCombo.setValue(r.getStatus().name());
        bedTypeCombo.setValue(r.getBedType());
        descriptionArea.setText(r.getDescription());
        acCheckBox.setSelected(r.isHasAC());
        wifiCheckBox.setSelected(r.isHasWifi());
        tvCheckBox.setSelected(r.isHasTV());
    }

    private void clearForm() {
        roomNumberField.clear();
        floorField.clear();
        capacityField.clear();
        priceField.clear();
        descriptionArea.clear();
        categoryCombo.getSelectionModel().clearSelection();
        statusCombo.getSelectionModel().clearSelection();
        bedTypeCombo.getSelectionModel().clearSelection();
        acCheckBox.setSelected(false);
        wifiCheckBox.setSelected(false);
        tvCheckBox.setSelected(false);
    }

    private void showForm(boolean show) {
        formPane.setVisible(show);
        formPane.setManaged(show);
    }
}
