#!/binb/bash

echo "Reloading systemd daemon..."
sudo systemctl daemon-reload

echo "Enabling payment-system service to start on boot..."
sudo systemctl enable payment-system

echo "Starting payment-system service..."
sudo systemctl start payment-system

echo "Checking service status..."
sudo systemctl status payment-system