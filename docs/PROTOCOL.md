# EmbedHub Wire Protocol Specification

This document details the command conventions, checksums, framing schemes, and telemetry formats supported by EmbedHub.

---

## 1. D-Pad & Car Driving Commands
Sent as standard single-character ASCII tokens, followed by the selected line ending (`\r\n` or `\n`):

| Command Byte | Description | Action on MCU |
| :---: | :--- | :--- |
| `F` | Forward | Both motors rotate forward |
| `B` | Backward | Both motors rotate backward |
| `L` | Turn Left | Left motor backward/stop, Right motor forward |
| `R` | Turn Right | Left motor forward, Right motor backward/stop |
| `S` | **STOP (Failsafe)** | All motors immediately halt |
| `V0` .. `V9` | Speed Level | Set PWM duty cycle between 0% and 100% |

---

## 2. 20Hz Analog Joystick Protocol
For differential steering and proportional robotics, EmbedHub streams updates at 20Hz:

```text
X:<value>,Y:<value>\n
```
- `<value>` is an integer normalized from `-100` to `+100`.
- Center neutral position is `X:0,Y:0`.
- On touch release, an immediate failsafe `S\n` or `X:0,Y:0\n` is guaranteed.

---

## 3. Telemetry Stream Formats (MCU to EmbedHub)
EmbedHub automatically ingests and visualizes telemetry in any of the following 4 formats:

### Format A: Key-Value Pairs
```text
temp=24.5,humidity=60,voltage=3.82
```
or space/colon delimited:
```text
temp:24.5 rpm:1200
```

### Format B: Structured JSON
```json
{
  "temp": 24.5,
  "rpm": 1200,
  "dist": 35.2,
  "alert": 0
}
```

### Format C: CSV Stream
Send column headers once, followed by comma-separated numerical values:
```text
roll,pitch,yaw
1.24,-0.45,180.20
```

---

## 4. Checksums & Framing
For noisy RF or long serial cables, EmbedHub supports framing and error-checking:

- **XOR Checksum**: 1 byte XOR of all payload bytes.
- **CRC-8**: Polynomial `0x07` (standard SMBus/Dallas).
- **Framing Markers**: Configurable start marker (e.g. `$`) and end marker (e.g. `*` or `#`).
- Example Framed Packet:
  ```text
  $<PAYLOAD><1-BYTE-CHECKSUM>#
  ```
