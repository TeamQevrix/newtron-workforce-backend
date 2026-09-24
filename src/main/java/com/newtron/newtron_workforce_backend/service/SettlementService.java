package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.entity.Settlement;

public interface SettlementService {

    /**
     * Calculates and persists a settlement record for a given WorkOrder and month.
     *
     * @param workOrderId the ID of the WorkOrder
     * @param month       the settlement month (1-12)
     * @param year        the settlement year
     * @return the saved Settlement entity
     */
    Settlement calculateAndSaveSettlement(Long workOrderId, int month, int year);
}
