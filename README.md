<p align="center">
  <img src="https://img.shields.io/badge/FaceAC-AI_Anticheat-7c3aed?style=for-the-badge" alt="FaceAC" />
</p>

<h1 align="center">FaceAC</h1>

<p align="center">
  AI-powered anticheat plugin for Minecraft servers<br/>
  <sub>Spigot · Paper · Folia &nbsp;|&nbsp; 1.16 – 1.21 &nbsp;|&nbsp; Java 17 / 21</sub>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/MC-1.16--1.21-brightgreen?style=flat-square" />
  <img src="https://img.shields.io/badge/Java-17_|_21-f89820?style=flat-square" />
  <img src="https://img.shields.io/badge/Folia-supported-blue?style=flat-square" />
</p>

---

### Что это

FaceAC собирает данные движений игрока (yaw, pitch, ускорение, jerk, GCD) и отправляет их на AI-бэкенд для анализа. По результату нейросети плагин автоматически выносит решение — бан, кик, флаг или наблюдение.

---

### Установка

1. Скачайте JAR из [Releases](../../releases) (или соберите сами — см. ниже)
2. Положите в `plugins/`
3. Запустите сервер — сгенерируется `plugins/FaceAC/config.yml`
4. Укажите `endpoint` и `api-key` в конфиге
5. Перезагрузите: `/faceac reload`

---

### Сборка

```bash
./gradlew shadowJar
```

JAR-файлы появятся в `build/libs/` — отдельно под Java 17 и 21.

---

### Команды

| Команда | Описание |
|---------|----------|
| `/faceac alerts` | Включить / выключить алерты в чат |
| `/faceac prob <ник>` | Показать вероятность для игрока |
| `/faceac reload` | Перезагрузить конфиг |
| `/faceac start <ник> <CHEAT\|LEGIT> "комментарий"` | Начать запись данных |
| `/faceac stop <ник>` | Остановить запись |

Алиасы: `/fac`, `/mlsac`, `/ml`

---

### Права (Permissions)

| Право | Что даёт | По умолчанию |
|-------|----------|:------------:|
| `faceac.admin` | Всё (включает дочерние) | OP |
| `faceac.alerts` | Получать алерты в чат | OP |
| `faceac.prob` | Команда `/faceac prob` | OP |
| `faceac.reload` | Перезагрузка конфига | OP |
| `faceac.collect` | Запись данных (start/stop) | OP |

---

### Конфигурация

<details>
<summary><b>config.yml</b> — основные настройки</summary>

```yaml
detection:
  enabled: true
  endpoint: "http://your-server:8000"   # URL бэкенда
  api-key: "your-api-key"               # Ключ из панели
  allow-http: true
  timeout-ms: 30000
  sample-size: 40                        # тиков на сэмпл
  sample-interval: 10                    # интервал между сэмплами

alerts:
  threshold: 0.75          # порог для алертов
  chat-threshold: 0.20     # порог для чат-алертов
  console: false           # логи в консоль

violation:
  threshold: 40            # VL для наказания
  reset-value: 20          # VL после наказания
  multiplier: 100.0        # множитель буфера
  decay: 0.35              # затухание при low-prob

penalties:
  min-probability: 0.01
  animation:
    enabled: true
    duration: 80           # тиков (80 = 4 сек)
  actions: {}              # кастомные команды
```

</details>

<details>
<summary><b>Модели</b></summary>

В `config.yml` раздел `detection.models`:

```yaml
models:
  fast:
    name: "Fast-A1"
    only-alert: false    # карает
  pro:
    name: "Pro-A1"
    only-alert: true     # только алерт
  ultra:
    name: "Ultra-A1"
    only-alert: true
  experimental:
    name: "Experimental"
    only-alert: true
```

`only-alert: true` — модель работает в тестовом режиме, наказания не выдаёт.

</details>

<details>
<summary><b>WorldGuard</b></summary>

```yaml
detection:
  worldguard:
    enabled: true
    disabled-regions:
      - "spawn:spawn"       # мир:регион
      - "lobby"             # регион во всех мирах
```

В отключённых регионах AI-проверка не запускается.

</details>

<details>
<summary><b>Folia</b></summary>

```yaml
folia:
  enabled: true
  thread-pool-size: 0       # 0 = авто
  entity-scheduler:
    enabled: true
  region-scheduler:
    enabled: true
```

На обычных Spigot/Paper серверах этот раздел игнорируется.

</details>

<details>
<summary><b>messages.yml</b></summary>

```yaml
prefix: "&bAC &8» &r"
alert-format: "&f{PLAYER} &bfailed &f{CHECK} &7(&fprob &b{PROBABILITY}&7)"
```

Плейсхолдеры: `{PLAYER}`, `{CHECK}`, `{PROBABILITY}`, `{BUFFER}`, `{VL}`, `{MODEL}`

</details>

---

### Кастомные наказания

В `penalties.actions` можно указать команды по уровню VL:

```yaml
penalties:
  actions:
    1:  "{BAN} {PLAYER}"
    # или кастомная команда:
    1:  "tempban {PLAYER} 1d FaceAC: Killaura ({PROBABILITY})"
```

Префиксы: `{BAN}`, `{KICK}`, `{CUSTOM_ALERT}` или любая серверная команда.

---

### Совместимость

| Платформа | Статус |
|-----------|--------|
| Spigot | ✅ |
| Paper | ✅ |
| Folia | ✅ |
| kSpigot | ✅ |
| Minecraft 1.16 – 1.21 | ✅ |

---

<p align="center"><sub>FaceAC — AI anticheat for Minecraft</sub></p>
