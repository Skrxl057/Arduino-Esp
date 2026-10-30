# Contributing to EmbedHub

We welcome contributions from embedded makers, Android developers, and robotics enthusiasts!

## Development Guidelines
1. Fork the repository and create your feature branch:
   ```bash
   git checkout -b feature/my-new-transport
   ```
2. Adhere to Kotlin Official Style Guidelines and Material 3 design tokens.
3. Make sure all hardware links implement the `Link` interface cleanly.
4. Add unit tests in `app/src/test/` for new parsers or protocol helpers.
5. Verify tests and build pass:
   ```bash
   ./gradlew test
   ./gradlew assembleDebug
   ```
6. Submit a Pull Request describing your changes, target microcontrollers tested, and relevant logs.
