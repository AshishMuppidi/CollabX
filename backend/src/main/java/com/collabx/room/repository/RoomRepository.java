package com.collabx.room.repository;

import com.collabx.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    Optional<Room> findByRoomId(String roomId);

    @Modifying
    @Query("UPDATE Room r SET r.code = :code, r.updatedAt = :now WHERE r.roomId = :roomId")
    int updateRoomCode(@Param("roomId") String roomId, @Param("code") String code, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Room r SET r.currentLanguage = :language, r.updatedAt = :now WHERE r.roomId = :roomId")
    int updateRoomLanguage(@Param("roomId") String roomId, @Param("language") String language, @Param("now") Instant now);
}
