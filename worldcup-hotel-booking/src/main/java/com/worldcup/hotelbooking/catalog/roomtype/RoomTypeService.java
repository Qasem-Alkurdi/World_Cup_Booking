package com.worldcup.hotelbooking.catalog.roomtype;

import org.springframework.stereotype.Service;

@Service
public class RoomTypeService {
        private final RoomTypeRepository roomTypeRepository;

        public RoomTypeService(RoomTypeRepository roomTypeRepository) {
            this.roomTypeRepository = roomTypeRepository;
        }

        public RoomType getRoomTypeById(Long id) {
            return roomTypeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Room type not found with id: " + id));
        }
}
