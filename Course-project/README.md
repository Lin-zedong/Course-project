После настройки среды Docker, выполнить:  
docker compose build app  
docker compose up -d  
  
Домашняя страница пользователя (настройки электронной почты): http://localhost:8080/  
Управление подписками пользователей: http://localhost:8080/subscriptions  
Журнал изменений: http://localhost:8080/events  
Вход администратора: http://localhost:8080/login (admin Admin#123)  
Автоматическое перенаправление после входа в систему: http://localhost:8080/admin/manage  
Проверять свою электронную почту (MailHog): http://localhost:8025
