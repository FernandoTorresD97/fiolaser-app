package br.com.fiolaser.notification;

public interface SmsSender {
  void send(String phone, String message);
}
