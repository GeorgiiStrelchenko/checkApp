package com.example.checkApp.service;

import com.example.checkApp.entity.Check;
import com.example.checkApp.model.CheckStatus;

public interface AlertNotifier {

    void notifyStatusChange(Check check, CheckStatus previousStatus, CheckStatus currentStatus);
}
