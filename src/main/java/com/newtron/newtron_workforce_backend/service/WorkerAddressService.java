package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.dto.WorkerAddressRequest;
import com.newtron.newtron_workforce_backend.dto.WorkerAddressResponse;

public interface WorkerAddressService {
    WorkerAddressResponse getAddress(User currentUser);
    WorkerAddressResponse createAddress(WorkerAddressRequest request, User currentUser);
    WorkerAddressResponse updateAddress(WorkerAddressRequest request, User currentUser);
}
