<!-- ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀ -->

<br/>

<p align="center">
  <a href="https://faceac.ru">
    <img src="https://img.shields.io/badge/%E2%97%86_FaceAC-000?style=for-the-badge&labelColor=000&color=10b981" height="40" />
  </a>
</p>

<p align="center">
  <b>AI-Powered Anticheat for Minecraft</b>
</p>

<p align="center">
  <a href="https://faceac.ru"><img src="https://img.shields.io/badge/Website-faceac.ru-10b981?style=flat-square&labelColor=0a0a0a" /></a>&nbsp;
  <img src="https://img.shields.io/badge/MC-1.16–1.21-10b981?style=flat-square&labelColor=0a0a0a" />&nbsp;
  <img src="https://img.shields.io/badge/Java-17_·_21-10b981?style=flat-square&labelColor=0a0a0a" />&nbsp;
  <img src="https://img.shields.io/badge/Folia-✓-10b981?style=flat-square&labelColor=0a0a0a" />
</p>

<br/>

<!-- ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀ -->

<p align="center">
  <img src="https://raw.githubusercontent.com/catppuccin/catppuccin/main/assets/misc/transparent.png" height="1" width="700" />
</p>

FaceAC — серверный плагин, который в реальном времени анализирует движения игроков нейросетью и автоматически выносит вердикт.
Плагин подключается к **[api.faceac.ru](https://api.faceac.ru)** — облачный inference, ничего не нужно хостить самому.

<br/>

## Быстрый старт

```
1.  Купите тариф → faceac.ru → получите API-ключ в личном кабинете
2.  Скачайте JAR → Releases → положите в plugins/
3.  Запустите сервер → откройте plugins/FaceAC/config.yml
4.  Вставьте api-key → /faceac reload
```

Endpoint уже прописан по умолчанию — менять не нужно.

<br/>

## Команды

```
/faceac alerts                              Вкл / выкл алерты в чат
/faceac prob <ник>                          Вероятность для игрока
/faceac reload                              Перезагрузить конфиг
/faceac start <ник> <CHEAT|LEGIT> "..."     Начать сбор данных
/faceac stop <ник>                          Остановить сбор
```

> Алиасы: `/fac` `/mlsac` `/ml`

<br/>

## Права

```
faceac.admin      Полный доступ (включает всё ниже)     [OP]
faceac.alerts     Получать алерты в чат                  [OP]
faceac.prob       /faceac prob                           [OP]
faceac.reload     Перезагрузка конфига                   [OP]
faceac.collect    Запись данных (start / stop)            [OP]
```

<br/>

## Конфигурация

<details>
<summary>&nbsp;&nbsp;<b>▸ config.yml</b></summary>
<br/>

```yaml
detection:
  enabled: true
  endpoint: "https://api.faceac.ru"
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
```

</details>

<details>
<summary>&nbsp;&nbsp;<b>▸ Модели</b></summary>
<br/>

```yaml
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
```

`only-alert: true` — тестовый режим, наказания не выдаёт.

</details>

<details>
<summary>&nbsp;&nbsp;<b>▸ WorldGuard</b></summary>
<br/>

```yaml
detection:
  worldguard:
    enabled: true
    disabled-regions:
      - "spawn:spawn"         # мир:регион
      - "lobby"               # все миры
```

</details>

<details>
<summary>&nbsp;&nbsp;<b>▸ Folia</b></summary>
<br/>

```yaml
folia:
  enabled: true
  thread-pool-size: 0
  entity-scheduler:
    enabled: true
  region-scheduler:
    enabled: true
```

На Spigot / Paper этот раздел игнорируется.

</details>

<details>
<summary>&nbsp;&nbsp;<b>▸ messages.yml</b></summary>
<br/>

```yaml
prefix: "&bAC &8» &r"
alert-format: "&f{PLAYER} &bfailed &f{CHECK} &7(&fprob &b{PROBABILITY}&7)"
```

Плейсхолдеры: `{PLAYER}` `{CHECK}` `{PROBABILITY}` `{BUFFER}` `{VL}` `{MODEL}`

</details>

<details>
<summary>&nbsp;&nbsp;<b>▸ Кастомные наказания</b></summary>
<br/>

```yaml
penalties:
  actions:
    1: "{BAN} {PLAYER}"
    # или:
    1: "tempban {PLAYER} 1d FaceAC: Killaura ({PROBABILITY})"
```

Префиксы: `{BAN}` `{KICK}` `{CUSTOM_ALERT}` или любая серверная команда.

</details>

<br/>

## Совместимость

```
Spigot              ✓
Paper               ✓
Folia               ✓
kSpigot             ✓
Minecraft 1.16–1.21 ✓
Java 17 / 21        ✓
```

<br/>

---

<p align="center">
  <a href="https://faceac.ru"><img src="https://img.shields.io/badge/faceac.ru-10b981?style=flat-square&labelColor=0a0a0a&label=" /></a>
</p>
