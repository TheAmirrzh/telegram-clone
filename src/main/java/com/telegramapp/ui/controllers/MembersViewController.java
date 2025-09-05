package com.telegramapp.ui.controllers;

import com.telegramapp.dao.impl.ChannelDAOImpl;
import com.telegramapp.dao.impl.GroupDAOImpl;
import com.telegramapp.dao.impl.UserDAOImpl;
import com.telegramapp.model.*;
import com.telegramapp.util.FX;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MembersViewController {

    @FXML private ListView<User> membersListView;
    @FXML private Button promoteButton;
    @FXML private Button removeButton;
    @FXML private Button addMemberButton;
    @FXML private Label titleLabel;

    private User currentUser;
    private Object chatEntity;
    private User selectedMember;
    private boolean isCurrentUserAdmin;
    private List<String> currentMemberIds;

    @FXML
    public void initialize() {
        membersListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newUser) -> {
            selectedMember = newUser;
            updateButtonStates();
        });
    }

    public void loadMembers(User currentUser, Object chatEntity) {
        this.currentUser = currentUser;
        this.chatEntity = chatEntity;
        refreshMemberList();
    }

    private void refreshMemberList() {
        FX.runAsync(() -> {
            try {
                if (chatEntity instanceof Group) {
                    Platform.runLater(() -> titleLabel.setText("Group Members"));
                    Group group = (Group) chatEntity;
                    List<GroupMemberInfo> membersInfo = new GroupDAOImpl().findMembersWithInfo(group.getId());
                    isCurrentUserAdmin = membersInfo.stream()
                            .anyMatch(m -> m.getUserId().equals(currentUser.getId()) && ("ADMIN".equals(m.getRole()) || "CREATOR".equals(m.getRole())));
                    currentMemberIds = membersInfo.stream().map(GroupMemberInfo::getUserId).collect(Collectors.toList());
                    return new UserDAOImpl().findByIds(currentMemberIds);
                } else if (chatEntity instanceof Channel) {
                    Platform.runLater(() -> titleLabel.setText("Channel Subscribers"));
                    Channel channel = (Channel) chatEntity;
                    ChannelDAOImpl channelDAO = new ChannelDAOImpl();
                    List<ChannelSubscriberInfo> subscribersInfo = channelDAO.findSubscribersWithInfo(channel.getId());
                    isCurrentUserAdmin = subscribersInfo.stream()
                            .anyMatch(s -> s.getUserId().equals(currentUser.getId()) && ("OWNER".equals(s.getRole()) || "ADMIN".equals(s.getRole())));

                    // UI Hint: Regular subscribers can't see the full list, but admins can.
                    if (!isCurrentUserAdmin) {
                        return Collections.<User>emptyList();
                    }
                    currentMemberIds = subscribersInfo.stream().map(ChannelSubscriberInfo::getUserId).collect(Collectors.toList());
                    return new UserDAOImpl().findByIds(currentMemberIds);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Collections.<User>emptyList();
        }, (List<User> users) -> {
            if (chatEntity instanceof Channel && !isCurrentUserAdmin) {
                membersListView.getItems().clear();
                membersListView.setPlaceholder(new Label("Only channel admins can see the full subscriber list."));
            } else if (users != null) {
                membersListView.getItems().setAll(users);
                membersListView.setCellFactory(lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(User user, boolean empty) {
                        super.updateItem(user, empty);
                        setText(empty ? null : user.getDisplayName());
                    }
                });
                membersListView.setPlaceholder(new Label("No members found."));
            }
            updateButtonStates();
        }, null);
    }


    private void updateButtonStates() {
        boolean adminControlsVisible = isCurrentUserAdmin && selectedMember != null && !selectedMember.getId().equals(currentUser.getId());
        promoteButton.setVisible(adminControlsVisible);
        removeButton.setVisible(adminControlsVisible);
        addMemberButton.setVisible(isCurrentUserAdmin);
    }

    @FXML
    private void onAddMember() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_member.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(titleLabel.getScene().getWindow());
            dialog.setTitle("Add New Member");
            dialog.setScene(new Scene(loader.load()));
            AddMemberController ctrl = loader.getController();
            ctrl.initData(chatEntity, currentMemberIds);
            dialog.showAndWait();
            refreshMemberList();
        } catch (IOException e) {
            e.printStackTrace();
            FX.showError("Failed to open the Add Member window.");
        }
    }


    @FXML
    private void onPromote() {
        if (!isCurrentUserAdmin || selectedMember == null) return;

        FX.runAsync(() -> {
            try {
                if (chatEntity instanceof Group) {
                    new GroupDAOImpl().updateMemberRole(((Group) chatEntity).getId(), selectedMember.getId(), "ADMIN");
                } else if (chatEntity instanceof Channel) {
                    new ChannelDAOImpl().updateSubscriberRole(((Channel) chatEntity).getId(), selectedMember.getId(), "ADMIN");
                }
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, success -> {
            if (success) {
                refreshMemberList();
            } else {
                FX.showError("Failed to promote user.");
            }
        }, null);
    }

    @FXML
    private void onRemove() {
        if (!isCurrentUserAdmin || selectedMember == null) return;

        FX.runAsync(() -> {
            try {
                if (chatEntity instanceof Group) {
                    new GroupDAOImpl().removeMember(((Group) chatEntity).getId(), selectedMember.getId());
                } else if (chatEntity instanceof Channel) {
                    new ChannelDAOImpl().removeSubscriber(((Channel) chatEntity).getId(), selectedMember.getId());
                }
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, success -> {
            if (success) {
                refreshMemberList();
            } else {
                FX.showError("Failed to remove member.");
            }
        }, null);
    }

    @FXML
    private void onClose() {
        ((Stage) titleLabel.getScene().getWindow()).close();
    }
}
