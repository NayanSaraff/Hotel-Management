package com.hotel.service;

import com.hotel.dao.RoomDAO;
import com.hotel.model.Room;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Room management.
 */
public class RoomService {

    private final RoomDAO roomDAO = new RoomDAO();

    public List<Room> getAllRooms()              { return roomDAO.findAll(); }
    public Optional<Room> getRoomById(int id)   { return roomDAO.findById(id); }

    public int addRoom(Room room) {
        validateRoom(room);
        return roomDAO.save(room);
    }

    public boolean updateRoom(Room room) {
        validateRoom(room);
        return roomDAO.update(room);
    }

    public boolean deleteRoom(int roomId) {
        return roomDAO.delete(roomId);
    }

    public boolean updateRoomStatus(int roomId, Room.Status status) {
        return roomDAO.updateStatus(roomId, status);
    }

    public int countByStatus(Room.Status status) {
        return roomDAO.countByStatus(status);
    }

    private void validateRoom(Room room) {
        if (room.getRoomNumber() == null || room.getRoomNumber().isBlank())
            throw new IllegalArgumentException("Room number is required.");
        if (room.getPricePerNight() <= 0)
            throw new IllegalArgumentException("Price per night must be positive.");
        if (room.getCapacity() <= 0)
            throw new IllegalArgumentException("Room capacity must be positive.");
    }
}
