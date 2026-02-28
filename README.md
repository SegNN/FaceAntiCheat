<div align="center">
  <a href="https://faceac.ru">
    <img src="https://img.shields.io/badge/%E2%97%86_FaceAC-000?style=for-the-badge&labelColor=000&color=10b981" height="40" alt="FaceAC" />
  </a>

  <h3>AI-Powered Anticheat for Minecraft</h3>

  <p>
    <a href="https://faceac.ru"><img src="https://img.shields.io/badge/Website-faceac.ru-10b981?style=flat-square&labelColor=0a0a0a" alt="Website" /></a>
    <img src="https://img.shields.io/badge/MC-1.16_–_1.21-10b981?style=flat-square&labelColor=0a0a0a" alt="Minecraft Versions" />
    <img src="https://img.shields.io/badge/Java-17_·_21-10b981?style=flat-square&labelColor=0a0a0a" alt="Java" />
    <img src="https://img.shields.io/badge/Folia-Supported-10b981?style=flat-square&labelColor=0a0a0a" alt="Folia" />
  </p>
</div>

---

**FaceAC** — серверный плагин, который в реальном времени анализирует движения игроков с помощью нейросети и автоматически выносит вердикт. Плагин подключается к облачному inference **[api.faceac.ru](https://api.faceac.ru)** — ничего не нужно хостить на своей стороне.

## 🚀 Быстрый старт

> [!IMPORTANT]
> Endpoint уже прописан по умолчанию — менять его в конфиге не нужно.

1. Купите тариф на сайте [faceac.ru](https://faceac.ru) и получите **API-ключ** в личном кабинете.
2. Скачайте JAR-файл из раздела **Releases** и поместите его в папку `plugins/`.
3. Запустите сервер, затем откройте сгенерированный файл `plugins/FaceAC/config.yml`.
4. Вставьте ваш api-key и пропишите в консоли `/faceac reload`.

## 💻 Команды и 🔐 Права

> **Алиасы команд:** `/fac` | `/mlsac` | `/ml`

| Команда | Описание | Право | Доступ |
|---|---|---|---|
| `/faceac alerts` | Вкл / выкл алерты в чат | `faceac.alerts` | OP |
| `/faceac prob <ник>` | Показать вероятность читов для игрока | `faceac.prob` | OP |
| `/faceac start <ник> <CHEAT\|LEGIT> "..."` | Начать сбор данных | `faceac.collect` | OP |
| `/faceac stop <ник>` | Остановить сбор данных | `faceac.collect` | OP |
| `/faceac reload` | Перезагрузить конфиг | `faceac.reload` | OP |
| *Все команды* | Полный доступ ко всему функционалу | `faceac.admin` | OP |

## ⚙️ Конфигурация

Разверните нужный блок, чтобы посмотреть примеры настройки.

<details>
<summary><b>1. Основной конфиг (config.yml)</b></summary>

```yaml
detection:
  enabled: true
  endpoint: "[https://api.faceac.ru](https://api.faceac.ru)"
  api-key: "face_xxxxxxxxxxxxxxxx"      # ← ваш ключ с faceac.ru
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
</details>

<details>
<summary><b>2. Настройка Моделей</b></summary>

Установка only-alert: true включает тестовый режим (наказания не выдаются).

YAML
detection:
  models:
    fast:
      name: "Fast-A1"
      only-alert: false       # выносит наказание
    pro:
      name: "Pro-A1"
      only-alert: true        # только алерт
    ultra:
      name: "Ultra-A1"
      only-alert: true
    experimental:
      name: "Experimental"
      only-alert: true
</details>

<details>
<summary><b>3. Интеграция с WorldGuard</b></summary>

YAML
detection:
  worldguard:
    enabled: true
    disabled-regions:
      - "spawn:spawn"         # мир:регион
      - "lobby"               # работает для всех миров
</details>

<details>
<summary><b>4. Настройки Folia</b></summary>

[!NOTE]
На ядрах Spigot / Paper этот раздел полностью игнорируется.

YAML
folia:
  enabled: true
  thread-pool-size: 0
  entity-scheduler:
    enabled: true
  region-scheduler:
    enabled: true
</details>

<details>
<summary><b>5. Сообщения (messages.yml)</b></summary>

Доступные плейсхолдеры: {PLAYER}, {CHECK}, {PROBABILITY}, {BUFFER}, {VL}, {MODEL}

YAML
prefix: "&bAC &8» &r"
alert-format: "&f{PLAYER} &bfailed &f{CHECK} &7(&fprob &b{PROBABILITY}&7)"
</details>

<details>
<summary><b>6. Кастомные наказания</b></summary>

Доступные префиксы: {BAN}, {KICK}, {CUSTOM_ALERT} или любая серверная команда.

YAML
penalties:
  actions:
    1: "{BAN} {PLAYER}"
    # или альтернативный вариант:
    1: "tempban {PLAYER} 1d FaceAC: Killaura ({PROBABILITY})"
</details>

🔌 Совместимость
✅ Spigot

✅ Paper

✅ Folia

✅ kSpigot

✅ Minecraft 1.16 — 1.21

✅ Java 17 / 21

<div align="center">
<a href="https://faceac.ru"><img src="https://img.shields.io/badge/faceac.ru-10b981?style=flat-square&labelColor=0a0a0a&label=" alt="Website Link" /></a>
</div>
