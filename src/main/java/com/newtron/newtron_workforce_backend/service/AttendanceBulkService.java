package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.dto.AttendanceBulkResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface AttendanceBulkService {
    AttendanceBulkResponseDto importAttendance(MultipartFile file, User currentUser);
}
