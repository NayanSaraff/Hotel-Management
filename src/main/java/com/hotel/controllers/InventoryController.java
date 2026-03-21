package com.hotel.controllers;

import com.hotel.dao.InventoryDAO;
import com.hotel.model.InventoryItem;
import com.hotel.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for Inventory Management module.
 */
public class InventoryController {

    @FXML private TableView<InventoryItem>              inventoryTable;
    @FXML private TableColumn<InventoryItem,String>     colName;
    @FXML private TableColumn<InventoryItem,String>     colCategory;
    @FXML private TableColumn<InventoryItem,Integer>    colQty;
    @FXML private TableColumn<InventoryItem,Integer>    colThreshold;
    @FXML private TableColumn<InventoryItem,String>     colUnit;
    @FXML private TableColumn<InventoryItem,Double>     colPrice;
    @FXML private TableColumn<InventoryItem,String>     colSupplier;
    @FXML private TableColumn<InventoryItem,String>     colStatus;

    @FXML private VBox              formPane;
    @FXML private TextField         itemNameField;
    @FXML private ComboBox<String>  categoryCombo;
    @FXML private TextField         qtyField;
    @FXML private TextField         thresholdField;
    @FXML private TextField         unitField;
    @FXML private TextField         priceField;
    @FXML private TextField         supplierField;
    @FXML private Label             formTitleLabel;

    @FXML private TextField         searchField;
    @FXML private Button            btnLowStock;

    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private InventoryItem selectedItem = null;
    private boolean isEditing = false;

    @FXML
    public void initialize() {
        setupColumns();
        categoryCombo.setItems(FXCollections.observableArrayList(
                "LINEN","TOILETRIES","FOOD_BEVERAGE","CLEANING","ELECTRONICS","FURNITURE","STATIONERY","OTHER"));
        loadItems();
        showForm(false);

        inventoryTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> selectedItem = n);
        searchField.textProperty().addListener((obs, o, n) -> filterItems(n));
    }

    private void setupColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory().name()));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantityAvailable"));
        colThreshold.setCellValueFactory(new PropertyValueFactory<>("minimumThreshold"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().isLowStock() ? "LOW STOCK" : "OK"));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("LOW STOCK".equals(item)
                        ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;"
                        : "-fx-text-fill: #27ae60;");
            }
        });

        // Highlight low-stock rows
        inventoryTable.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null && item.isLowStock())
                    setStyle("-fx-background-color: #fff3cd;");
                else setStyle("");
            }
        });

        inventoryTable.getColumns().forEach(col -> col.setResizable(false));
inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadItems() {
        inventoryTable.setItems(FXCollections.observableArrayList(inventoryDAO.findAll()));
    }

    private void filterItems(String keyword) {
        if (keyword == null || keyword.isBlank()) { loadItems(); return; }
        List<InventoryItem> filtered = inventoryDAO.findAll().stream()
                .filter(i -> i.getItemName().toLowerCase().contains(keyword.toLowerCase())
                        || i.getCategory().name().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
        inventoryTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML private void handleShowLowStock() {
        inventoryTable.setItems(FXCollections.observableArrayList(inventoryDAO.findLowStock()));
    }

    @FXML private void handleAdd() {
        selectedItem = null; isEditing = false;
        clearForm(); formTitleLabel.setText("Add Inventory Item");
        showForm(true);
    }

    @FXML private void handleEdit() {
        selectedItem = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) { AlertUtil.showWarning("Select Item", "Please select an item."); return; }
        isEditing = true;
        populateForm(selectedItem);
        formTitleLabel.setText("Edit – " + selectedItem.getItemName());
        showForm(true);
    }

    @FXML private void handleSave() {
        try {
            InventoryItem item = buildItemFromForm();
            if (isEditing) {
                item.setItemId(selectedItem.getItemId());
                inventoryDAO.update(item);
                AlertUtil.showInfo("Updated", "Item updated.");
            } else {
                int id = inventoryDAO.save(item);
                if (id < 0) throw new Exception("Failed to save item.");
                AlertUtil.showInfo("Added", "Item added to inventory.");
            }
            loadItems(); showForm(false);
        } catch (Exception e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }

    @FXML private void handleDelete() {
        if (selectedItem == null) return;
        if (AlertUtil.showConfirm("Delete", "Delete item: " + selectedItem.getItemName() + "?")) {
            inventoryDAO.delete(selectedItem.getItemId());
            loadItems(); showForm(false);
        }
    }

    @FXML private void handleRestock() {
        if (selectedItem == null) { AlertUtil.showWarning("Select Item", "Please select an item."); return; }
        TextInputDialog d = new TextInputDialog("10");
        d.setTitle("Restock"); d.setHeaderText(selectedItem.getItemName());
        d.setContentText("Enter quantity to add:");
        d.showAndWait().ifPresent(input -> {
            try {
                int qty = Integer.parseInt(input.trim());
                inventoryDAO.adjustQuantity(selectedItem.getItemId(), qty);
                AlertUtil.showInfo("Restocked", "Stock updated.");
                loadItems();
            } catch (NumberFormatException e) {
                AlertUtil.showError("Error", "Invalid quantity.");
            }
        });
    }

    @FXML private void handleCancel() { showForm(false); }

    private InventoryItem buildItemFromForm() {
        InventoryItem item = new InventoryItem();
        item.setItemName(itemNameField.getText().trim());
        item.setCategory(InventoryItem.Category.valueOf(categoryCombo.getValue()));
        item.setQuantityAvailable(Integer.parseInt(qtyField.getText().trim()));
        item.setMinimumThreshold(Integer.parseInt(thresholdField.getText().trim()));
        item.setUnit(unitField.getText().trim());
        item.setUnitPrice(Double.parseDouble(priceField.getText().trim()));
        item.setSupplier(supplierField.getText().trim());
        item.setLastRestocked(LocalDate.now());
        return item;
    }

    private void populateForm(InventoryItem i) {
        itemNameField.setText(i.getItemName());
        categoryCombo.setValue(i.getCategory().name());
        qtyField.setText(String.valueOf(i.getQuantityAvailable()));
        thresholdField.setText(String.valueOf(i.getMinimumThreshold()));
        unitField.setText(i.getUnit());
        priceField.setText(String.valueOf(i.getUnitPrice()));
        supplierField.setText(i.getSupplier());
    }

    private void clearForm() {
        itemNameField.clear(); qtyField.clear(); thresholdField.clear();
        unitField.clear(); priceField.clear(); supplierField.clear();
        categoryCombo.getSelectionModel().clearSelection();
    }

    private void showForm(boolean show) { formPane.setVisible(show); formPane.setManaged(show); }
}
