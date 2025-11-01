#!/bin/bash

# 서비스가 실행 중인지 확인
if systemctl is-active --quiet payment-system; then
  echo "Stopping payment-system service..."
  sudo systemctl stop payment-system
else
  echo "payment-system service is not running."
fi