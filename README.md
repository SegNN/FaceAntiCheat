FaceAC — AI-Powered Anticheat for Minecraft
Сайт: https://faceac.ru
Поддержка: Minecraft 1.16–1.21 | Java 17, 21 | Folia

FaceAC — это инновационный серверный плагин, который в реальном времени анализирует движения игроков с помощью нейросети и автоматически выносит вердикт. Плагин подключается к облачному inference api.faceac.ru, поэтому вам не нужно выделять мощности сервера под машинное обучение.

=== БЫСТРЫЙ СТАРТ ===
1. Оформите подписку: Перейдите на faceac.ru и получите API-ключ в личном кабинете.
2. Установите плагин: Скачайте .jar файл из раздела Releases и поместите его в папку plugins/.
3. Запустите сервер: Дождитесь загрузки, чтобы плагин создал конфигурационные файлы.
4. Настройте: Откройте plugins/FaceAC/config.yml, вставьте ваш API-ключ и введите команду /faceac reload.
Примечание: Endpoint (https://api.faceac.ru) уже прописан по умолчанию — менять его не нужно.

=== КОМАНДЫ ===
Алиасы: /fac, /mlsac, /ml
/faceac alerts — Включить / выключить уведомления (алерты) в чат
/faceac prob <ник> — Проверить вероятность использования читов для игрока
/faceac start <ник> <CHEAT|LEGIT> "..." — Начать сбор данных для обучения/анализа
/faceac stop <ник> — Остановить сбор данных
/faceac reload — Перезагрузить конфигурацию плагина

=== ПРАВА (PERMISSIONS) ===
faceac.admin — Полный доступ ко всем функциям плагина (OP)
faceac.alerts — Получение уведомлений о нарушениях в чат (OP)
faceac.prob — Доступ к команде проверки вероятности (OP)
faceac.collect — Доступ к записи данных (OP)
faceac.reload — Доступ к перезагрузке конфига (OP)

=== КОНФИГУРАЦИЯ ===

--- Основной конфиг (config.yml) ---
detection:
  enabled: true
  endpoint: "https://api.faceac.ru"
  api-key: "face_xxxxxxxxxxxxxxxx"      # ← Ваш ключ с faceac.ru
  timeout-ms: 30000
  sample-size: 40
  sample-interval: 10

alerts:
  threshold: 0.75
  chat-threshold: 0.20
  console: false

violation:
  threshold: 40
  reset-value: 20
  multiplier: 100.0
  decay: 0.35

penalties:
  min-probability: 0.01
  animation:
    enabled: true
    duration: 80
  actions: {}

--- Модели нейросети ---
Примечание: only-alert: true означает тестовый режим (наказания не выдаются).
detection:
  models:
    fast:
      name: "Fast-A1"
      only-alert: false
    pro:
      name: "Pro-A1"
      only-alert: true
    ultra:
      name: "Ultra-A1"
      only-alert: true
    experimental:
      name: "Experimental"
      only-alert: true

--- Интеграция с WorldGuard ---
detection:
  worldguard:
    enabled: true
    disabled-regions:
      - "spawn:spawn"         # Формат: мир:регион
      - "lobby"               # Отключить во всех мирах для региона lobby

--- Оптимизация Folia ---
Примечание: На Spigot / Paper этот раздел автоматически игнорируется.
folia:
  enabled: true
  thread-pool-size: 0
  entity-scheduler:
    enabled: true
  region-scheduler:
    enabled: true

--- Локализация (messages.yml) ---
Доступные плейсхолдеры: {PLAYER}, {CHECK}, {PROBABILITY}, {BUFFER}, {VL}, {MODEL}
prefix: "&bAC &8» &r"
alert-format: "&f{PLAYER} &bfailed &f{CHECK} &7(&fprob &b{PROBABILITY}&7)"

--- Кастомные наказания ---
Префиксы плагина: {BAN}, {KICK}, {CUSTOM_ALERT}. Можно использовать любые серверные команды.
penalties:
  actions:
    1: "{BAN} {PLAYER}"
    # Или интеграция со сторонним плагином банов:
    # 1: "tempban {PLAYER} 1d FaceAC: Вредоносное ПО ({PROBABILITY})"

=== СОВМЕСТИМОСТЬ ===
* Ядра: Spigot, Paper, Folia, kSpigot
* Версии Minecraft: 1.16 — 1.21
* Версии Java: 17, 21
